package jadex.base.service.message.transport.httprelaymtp.io;

import jadex.base.service.awareness.discovery.relay.IRelayAwarenessService;
import jadex.base.service.message.ISendTask;
import jadex.base.service.message.transport.ITransport;
import jadex.base.service.message.transport.httprelaymtp.SRelay;
import jadex.bridge.IComponentIdentifier;
import jadex.bridge.IInternalAccess;
import jadex.bridge.service.types.threadpool.IThreadPoolService;
import jadex.commons.IResultCommand;
import jadex.commons.SUtil;
import jadex.commons.Tuple2;
import jadex.commons.future.ExceptionDelegationResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.commons.future.IResultListener;
import jadex.micro.annotation.Binding;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class HttpRelayTransport implements ITransport
{
	//-------- constants --------
	
	/** The alive time for assuming a connection is working. */
	protected static final long	ALIVETIME	= 30000;
	
	/** The maximum number of workers for an address. */
	protected static final int	MAX_WORKERS	= 16;
	
	// HACK!!! Disable all certificate checking (only until we find a more fine-grained solution)
	static
	{
        try
        {
	        TrustManager[] trustAllCerts = new TrustManager[]
	        {
                new X509TrustManager()
                {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers()
                    {
                    	return null;
                    }
                    public void checkClientTrusted( java.security.cert.X509Certificate[] certs, String authType ) { }
                    public void checkServerTrusted( java.security.cert.X509Certificate[] certs, String authType ) { }
                }
	        };
	
	        // Install the all-trusting trust manager
            SSLContext sc = SSLContext.getInstance( "TLS" );
            sc.init( null, trustAllCerts, new java.security.SecureRandom() );
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier()
            {
                public boolean verify(String urlHostName, SSLSession session)
                {
                	return true;
                }
            });
        }
        catch(Exception e)
        {
            //We can not recover from this exception.
            e.printStackTrace();
        }
	}
	
	//-------- attributes --------
	
	/** The component. */
	protected IInternalAccess component;
	
	/** The thread pool. */
	protected IThreadPoolService	threadpool;
	
	/** The relay server address. */
	protected String	address;
	
	/** The receiver process. */
	protected HttpReceiver	receiver;
	
	/** The known addresses (address -> last used date (0 for pinging, negative for dead connections)). */
	protected Map<String, Long>	addresses;
	
	/** The worker count (address -> count). */
	protected Map<String, Integer>	workers;
	
	/** The ready queue per address (tasks to reschedule after ping). */
	protected Map<String, Collection<ISendTask>>	readyqueue;
	
	/** The send queue per address (tasks to send on worker thread). */
	protected Map<String, List<Tuple2<ISendTask, Future<Void>>>>	sendqueue;
	
	//-------- constructors --------
	
	/**
	 *  Create a new relay transport.
	 */
	public HttpRelayTransport(IInternalAccess component, String address)
	{
		this.component	= component;
		this.address	= address;
		this.addresses	= Collections.synchronizedMap(new HashMap<String, Long>());	// Todo: cleanup unused addresses!?
		this.workers	= new HashMap<String, Integer>();
		this.readyqueue	= new HashMap<String, Collection<ISendTask>>();
		this.sendqueue	= new HashMap<String, List<Tuple2<ISendTask, Future<Void>>>>();
		
		boolean	found	= false;
		for(int i=0; !found && i<getServiceSchemas().length; i++)
		{
			found	= address.startsWith(getServiceSchemas()[i]);
		}
		if(!found)
		{
			throw new RuntimeException("Address does not match supported service schemes: "+address+", "+SUtil.arrayToString(getServiceSchemas()));
		}
	}
	
	//-------- methods --------
	
	/**
	 *  Start the transport.
	 */
	public IFuture<Void> start()
	{
		final Future<Void>	ret	= new Future<Void>();
		component.getServiceContainer().searchService(IThreadPoolService.class, Binding.SCOPE_PLATFORM)
			.addResultListener(new ExceptionDelegationResultListener<IThreadPoolService, Void>(ret)
		{
			public void customResultAvailable(IThreadPoolService tps)
			{
				threadpool	= tps;
				// Create the receiver (starts automatically).
				receiver	= new HttpReceiver(HttpRelayTransport.this, component.getExternalAccess(), address.substring(6));	// strip 'relay-' prefix.
				ret.setResult(null);
			}
		});
		return ret;
	}

	/**
	 *  Perform cleanup operations (if any).
	 */
	public IFuture<Void> shutdown()
	{
		// Stop the reciever.
		this.receiver.stop();
		return IFuture.DONE;
	}
	
	/**
	 *  Called from receiver thread, when it connects to a address.
	 */
	protected void	connected(final String address, final boolean dead)
	{
		Long	oldtime	= addresses.get(address);
		// Remove all old entries with start address (e.g. also awareness urls).
		if(!dead)
		{
			String[]	aadrs	= addresses.keySet().toArray(new String[0]);
			for(int i=0; i<aadrs.length; i++)
			{
				if(aadrs[i].startsWith(address))
				{
					addresses.remove(aadrs[i]);
				}
			}
		}
		addresses.put(address, new Long(dead ? -System.currentTimeMillis() : System.currentTimeMillis()));
		
		ISendTask[]	readytasks	= null;
		synchronized(readyqueue)
		{
			Collection<ISendTask>	queue	= readyqueue.get(address);
			if(queue!=null)
			{
				readytasks	= queue.toArray(new ISendTask[queue.size()]);
				readyqueue.remove(address);
			}
		}
		for(int i=0; readytasks!=null && i<readytasks.length; i++)
		{
			internalSendMessage(address, readytasks[i]);
		}
		
		// inform awa when olddead
		boolean	olddead	= oldtime==null || oldtime.longValue()<=0;
		if(dead != olddead)
		{
			// Inform awareness manager (if any).
			component.getServiceContainer().searchService(IRelayAwarenessService.class, Binding.SCOPE_PLATFORM)
				.addResultListener(new IResultListener<IRelayAwarenessService>()
			{
				public void resultAvailable(IRelayAwarenessService ras)
				{
					if(dead)
						ras.disconnected("relay-"+address);
					else
						ras.connected("relay-"+address);
				}
				
				public void exceptionOccurred(Exception exception)
				{
					// No awa service -> ignore awa infos.
				}
			});
		}
	}
	
	/**
	 *  Test if a transport is applicable for the target address.
	 *  
	 *  @return True, if the transport is applicable for the address.
	 */
	public boolean	isApplicable(String address)
	{
		boolean	applicable	= false;
		for(int i=0; !applicable && i<getServiceSchemas().length; i++)
		{
			applicable	= address.startsWith(getServiceSchemas()[i]);
		}
		return applicable;		
	}
	
	/**
	 *  Send a message to the given address.
	 *  This method is called multiple times for the same message, i.e. once for each applicable transport / address pair.
	 *  The transport should asynchronously try to connect to the target address
	 *  (or reuse an existing connection) and afterwards call-back the ready() method on the send task.
	 *  
	 *  The send manager calls the obtained send commands of the transports and makes sure that the message
	 *  gets sent only once (i.e. call send commands sequentially and stop, when a send command finished successfully).
	 *  
	 *  All transports may keep any established connections open for later messages.
	 *  
	 *  @param address The address to send to.
	 *  @param task A task representing the message to send.
	 */
	public void	sendMessage(String address, ISendTask task)
	{
		internalSendMessage(address.substring(6), task);	// strip 'relay-' prefix.
	}
	
	/**
	 * 	Schedule message sending.
	 */
	public void	internalSendMessage(final String address, final ISendTask task)
	{
		final Long	time	= addresses.get(address);
		// Connection available or dead.
		if(time!=null && time.longValue()!=0 && Math.abs(time.longValue())+ALIVETIME>System.currentTimeMillis())
		{
			IResultCommand<IFuture<Void>, Void>	send	= new IResultCommand<IFuture<Void>, Void>()
			{
				public IFuture<Void> execute(Void args)
				{
					IFuture<Void>	ret;
					if(time.longValue()>0)
					{
						// Connection alive.
						ret	= queueDoSendTask(address, task);
					}
					else
					{
						// Connection dead.
						ret	= new Future<Void>(new RuntimeException("No connection to "+address));
					}
					return ret;
				}
			};
			task.ready(send);
		}
		
		// Ping required or already running.
		else
		{
			queueReadySendTask(address, task, time==null || time.longValue()!=0);
		}
	}
	
	/**
	 *  Returns the prefix of this transport
	 *  @return Transport prefix.
	 */
	public String[] getServiceSchemas()
	{
		return SRelay.ADDRESS_SCHEMES;
	}
	
	/**
	 *  Get the addresses of this transport.
	 *  @return An array of strings representing the addresses 
	 *  of this message transport mechanism.
	 */
	public String[] getAddresses()
	{
		return new String[]{address};
	}
	
	/**
	 *  Queue a ready send task for execution after a ping.
	 */
	protected void	queueReadySendTask(final String address, ISendTask task, boolean ping)
	{
		synchronized(readyqueue)
		{
			Collection<ISendTask>	queue	= readyqueue.get(address);
			if(queue==null)
			{
				queue	= new ArrayList<ISendTask>();
				readyqueue.put(address, queue);
			}
			queue.add(task);
		}
		
		if(ping)
		{
			addresses.put(address, new Long(0));
			
			threadpool.execute(new Runnable()
			{
				public void run()
				{
					// Start new server ping.
					try
					{
						URL	url	= new URL(address + (address.endsWith("/") ? "ping" : "/ping"));
						HttpURLConnection	con	= (HttpURLConnection)url.openConnection();
						con.connect();
						int	code	= con.getResponseCode();
						if(code!=HttpURLConnection.HTTP_OK)
							throw new IOException("HTTP code "+code+": "+con.getResponseMessage());
						addresses.put(address, new Long(System.currentTimeMillis()));
					}
					catch(Exception e)
					{
						component.getLogger().info("HTTP relay: No connection to "+address+", "+e);
						addresses.put(address, new Long(-System.currentTimeMillis()));
					}
					
					ISendTask[]	readytasks	= null;
					synchronized(readyqueue)
					{
						Collection<ISendTask>	queue	= readyqueue.get(address);
						if(queue!=null)
						{
							readytasks	= queue.toArray(new ISendTask[queue.size()]);
							readyqueue.remove(address);
						}
					}
					for(int i=0; readytasks!=null && i<readytasks.length; i++)
					{
						internalSendMessage(address, readytasks[i]);
					}
				}
			});
		}
	}

	/**
	 *  Queue a send task for execution on a worker thread.
	 */
	protected IFuture<Void>	queueDoSendTask(final String address, ISendTask task)
	{
		Future<Void>	ret	= new Future<Void>();
		boolean	startworker	= false;
		synchronized(workers)
		{
			List<Tuple2<ISendTask, Future<Void>>>	queue	= sendqueue.get(address);
			if(queue==null)
			{
				queue	= new LinkedList<Tuple2<ISendTask,Future<Void>>>();
				sendqueue.put(address, queue);
			}
			queue.add(new Tuple2<ISendTask, Future<Void>>(task, ret));
			
			Integer	cnt	= workers.get(address);
			if(cnt==null)
			{
				cnt	= new Integer(0);
				workers.put(address, cnt);
			}
			if(cnt.intValue()<MAX_WORKERS)
			{
				workers.put(address, new Integer(cnt.intValue()+1));
				startworker	= true;
//				System.out.println("starting worker: "+workers.get(address));
			}
		}
		
		if(startworker)
		{
			threadpool.execute(new Runnable()
			{
				public void run()
				{
					boolean	again	= true;
					
					while(again)
					{
						ISendTask	task	= null;
						Future<Void>	ret	= null;
						synchronized(workers)
						{
							List<Tuple2<ISendTask, Future<Void>>>	queue	= sendqueue.get(address);
							if(queue!=null)
							{
								Tuple2<ISendTask, Future<Void>>	tup	= queue.remove(0);
								task	= tup.getFirstEntity();
								ret	= tup.getSecondEntity();
								if(queue.isEmpty())
								{
									sendqueue.remove(address);
								}
							}
							else
							{
								again	= false;
								Integer	cnt	= workers.get(address);
								if(cnt.intValue()>1)
								{
									workers.put(address, new Integer(cnt.intValue()-1));
								}
								else
								{
									workers.remove(address);
								}
							}
						}
						
						if(task!=null)
						{
//							System.out.println("using worker");
							try
							{
								// Message service only calls transport.sendMessage() with receivers on same destination
								// so just use first to fetch platform id.
								IComponentIdentifier	targetid	= task.getReceivers()[0].getRoot();
								byte[]	iddata	= targetid.getName().getBytes("UTF-8");
								
								URL	url	= new URL(address);
								HttpURLConnection	con	= (HttpURLConnection)url.openConnection();
			
								con.setRequestMethod("POST");
								con.setDoOutput(true);
								con.setUseCaches(false);
								con.setRequestProperty("Content-Type", "application/octet-stream");
								con.setRequestProperty("Content-Length", ""+(4+iddata.length+4+task.getProlog().length+task.getData().length));
								con.connect();
								
								OutputStream	out	= con.getOutputStream();
								out.write(SUtil.intToBytes(iddata.length));
								out.write(iddata);
								out.write(SUtil.intToBytes(task.getProlog().length+task.getData().length));
								out.write(task.getProlog());
								out.write(task.getData());
								out.flush();
								
								int	code	= con.getResponseCode();
								if(code!=HttpURLConnection.HTTP_OK)
									throw new IOException("HTTP code "+code+": "+con.getResponseMessage());
								addresses.put(address, new Long(System.currentTimeMillis()));
								
								ret.setResult(null);
							}
							catch(Exception e)
							{
								ret.setException(e);
							}
						}
					}
				}
			});
		}
		return ret;
	}
}
