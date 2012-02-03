package jadex.base.service.message.transport.tcpmtp;

import jadex.base.AbstractComponentAdapter;
import jadex.base.service.message.ManagerSendTask;
import jadex.base.service.message.transport.ITransport;
import jadex.bridge.service.IServiceProvider;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.bridge.service.search.SServiceProvider;
import jadex.bridge.service.types.message.IMessageService;
import jadex.bridge.service.types.threadpool.IThreadPoolService;
import jadex.commons.SUtil;
import jadex.commons.collection.ILRUEntryCleaner;
import jadex.commons.collection.LRU;
import jadex.commons.collection.SCollection;
import jadex.commons.concurrent.Token;
import jadex.commons.future.ExceptionDelegationResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.commons.future.IResultListener;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

/**
 *  The tcp transport for sending messages over
 *  tcp/ip connections. Initiates one receiving
 *  tcp/ip port under the specified settings and
 *  opens outgoing connections for all remote 
 *  platforms on demand.
 *  
 *  For the receiving side a separate listener
 *  thread is necessary as it must be continuously
 *  listened for incoming transmission requests.
 */
public class TCPTransport implements ITransport
{
	//-------- constants --------
	
	/** The schema name. */
	public final static String SCHEMA = "tcp-mtp://";
	
	/** Constant for asynchronous setting. */
	public final static String ASYNCHRONOUS = "asynchronous";
	
	/** The receiving port. */
	public final static String PORT = "port";
	
	/** How long to keep output connections alive (5 min). */
	protected final static int	MAX_KEEPALIVE	= 300000;

	/** 2MB as message buffer */
	protected final static int BUFFER_SIZE	= 1024 * 1024 * 2;
	
	/** Maximum number of outgoing connections */
	protected final static int MAX_CONNECTIONS	= 20;
	
	/** Default port. */
	protected final static int DEFAULT_PORT	= 9876;
	
	//-------- attributes --------
	
	/** The platform. */
	protected IServiceProvider container;
	
	/** The addresses. */
	protected String[] addresses;
	
	/** The port. */
	protected int port;
	
	/** The server socket for receiving messages. */
	protected ServerSocket serversocket;
	
	/** The opened connections for addresses. (aid address -> connection). */
	protected Map<String, Object> connections;
	
	/** Should be received asynchronously? One thread for receiving is
		unavoidable. Async defines if the receival should be done on a
		new thread always or on the one receiver thread. */
	protected boolean async;
	
	/** The logger. */
	protected Logger logger;
	
	/** The cleanup timer. */
	protected Timer	timer;
	
	//-------- constructors --------
	
	/**
	 *  Init the transport.
	 *  @param platform The platform.
	 *  @param settings The settings.
	 */
	public TCPTransport(final IServiceProvider container, int port)
	{
		this(container, port, true);
	}

	
	/**
	 *  Init the transport.
	 *  @param platform The platform.
	 *  @param settings The settings.
	 */
	public TCPTransport(final IServiceProvider container, int port, final boolean async)
	{
		this.logger = Logger.getLogger(AbstractComponentAdapter.getLoggerName(container.getId())+".TCPTransport");
		
		this.container = container;
		this.async = async;
		this.port = port;
		
		// Set up sending side.
		this.connections = SCollection.createLRU(MAX_CONNECTIONS);
		((LRU<String, Object>)this.connections).setCleaner(new ILRUEntryCleaner<String, Object>()
		{
			public void cleanupEldestEntry(Entry<String, Object> eldest)
			{
				Object con = eldest.getValue();
				if(con instanceof TCPOutputConnection)
				{
					((TCPOutputConnection)con).close();
				}
			}
		});
		this.connections = Collections.synchronizedMap(this.connections);
	}

	/**
	 *  Start the transport.
	 */
	public IFuture<Void> start()
	{
		final Future<Void> ret = new Future<Void>();
		try
		{
			// Set up receiver side.
			// If port==0 -> any free port
			this.serversocket = new ServerSocket(port);
			this.port = serversocket.getLocalPort();

			
			String[]	addresses	= SUtil.getNetworkAddresses();
			this.addresses	= new String[addresses.length];
			for(int i=0; i<addresses.length; i++)
			{
				this.addresses[i]	= getAddress(addresses[i], port);
			}
			
			// Start the receiver thread.
			SServiceProvider.getService(container, IThreadPoolService.class, RequiredServiceInfo.SCOPE_PLATFORM)
				.addResultListener(new ExceptionDelegationResultListener<IThreadPoolService, Void>(ret)
			{
				public void customResultAvailable(final IThreadPoolService tp)
				{
					ret.setResult(null);
					tp.execute(new Runnable()
					{
						List<Object> openincons = Collections.synchronizedList(new ArrayList<Object>());
						public void run()
						{
							//try{serversocket.setSoTimeout(10000);} catch(SocketException e) {}
							while(!serversocket.isClosed())
							{
								try
								{
									final TCPInputConnection con = new TCPInputConnection(serversocket.accept());
									openincons.add(con);
									if(!async)
									{
										TCPTransport.this.deliverMessages(con)
											.addResultListener(new IResultListener<Void>()
										{
											public void resultAvailable(Void result)
											{
												openincons.remove(con);
											}
											
											public void exceptionOccurred(Exception exception)
											{
												openincons.remove(con);
											}
										});
									}
									else
									{
										// Each accepted incoming connection request is handled
										// in a separate thread in async mode.
										tp.execute(new Runnable()
										{
											public void run()
											{
												TCPTransport.this.deliverMessages(con)
													.addResultListener(new IResultListener<Void>()
												{
													public void resultAvailable(Void result)
													{
														openincons.remove(con);
													}
													
													public void exceptionOccurred(Exception exception)
													{
														openincons.remove(con);
													}
												});
											}
										});
									}
								}
								catch(IOException e)
								{
									//logger.warning("TCPTransport receiver connect error: "+e);
									//e.printStackTrace();
								}
							}
							
							TCPInputConnection[] incons = (TCPInputConnection[])openincons.toArray(new TCPInputConnection[0]);
							for(int i=0; i<incons.length; i++)
							{
//								System.out.println("close: "+incons[i]);
								incons[i].close();
							}
//							logger.warning("TCPTransport serversocket closed.");
						}
					});
				}
			});
		}
		catch(Exception e)
		{
			//e.printStackTrace();
			ret.setException(new RuntimeException("Transport initialization error: "+e.getMessage()));
//			throw new RuntimeException("Transport initialization error: "+e.getMessage());
		}
		return ret;
	}
	
	/**
	 *  Perform cleanup operations (if any).
	 */
	public IFuture<Void> shutdown()
	{
		try{this.serversocket.close();}catch(Exception e){}
		connections = null; // Help gc
		return IFuture.DONE;
	}
	
	//-------- methods --------
	
	/**
	 *  Send a message to receivers on the same platform.
	 *  This method is called concurrently for all transports.
	 *  Each transport should try to connect to the target platform
	 *  (or reuse an existing connection) and afterwards acquire the token.
	 *  
	 *  The one transport that acquires the token (i.e. the first connected transport) gets to send the message.
	 *  All other transports ignore the current message and return an exception,
	 *  but may keep any established connections open for later messages.
	 *  
	 *  @param task The message to send.
	 *  @param token The token to be acquired before sending. 
	 *  @return A future indicating successful sending or exception, when the message was not send by this transport.
	 */
	public IFuture<Void>	sendMessage(ManagerSendTask task, Token token)
	{
		IFuture<Void>	ret	= null;
		
		// Fetch all addresses
		Set<String>	addresses	= new LinkedHashSet<String>();
		for(int i=0; i<task.getReceivers().length; i++)
		{
			String[]	raddrs	= task.getReceivers()[i].getAddresses();
			for(int j=0; j<raddrs.length; j++)
			{
				addresses.add(raddrs[j]);
			}			
		}

		// Iterate over all different addresses and try to send
		// to missing and appropriate receivers
		String[] addrs = (String[])addresses.toArray(new String[addresses.size()]);
		for(int i=0; ret==null && i<addrs.length; i++)
		{
			TCPOutputConnection con = getConnection(addrs[i], true);
			if(con!=null)
			{
				if(token.acquire())
				{
					if(con.send(task.getProlog(), task.getData()))
					{
						ret	= IFuture.DONE;
					}
					else
					{
						ret	= new Future<Void>(new RuntimeException("Send failed: "+con));
					}
				}
				else
				{
					ret	= new Future<Void>(new RuntimeException("Not sending."));
				}
			}
		}
		
		if(ret==null)
		{
			ret	= new Future<Void>(new RuntimeException("No working connection."));			
		}
		
		return ret;
	}
	
	/**
	 *  Returns the prefix of this transport
	 *  @return Transport prefix.
	 */
	public String getServiceSchema()
	{
		return SCHEMA;
	}
	
	/**
	 *  Get the adresses of this transport.
	 *  @return An array of strings representing the addresses 
	 *  of this message transport mechanism.
	 */
	public String[] getAddresses()
	{
		return addresses;
	}
	
	//-------- helper methods --------
	
	/**
	 *  Get the address of this transport.
	 *  @param hostname The hostname.
	 *  @param port The port.
	 *  @return <scheme>:<hostname>:<port>
	 */
	protected String getAddress(String hostname, int port)
	{
		return getServiceSchema()+hostname+":"+port;
	}
	
	/**
	 *  Get the connection.
	 *  @param address
	 *  @return a connection of this type
	 */
	protected TCPOutputConnection getConnection(String address, boolean create)
	{
		address = address.toLowerCase();
		
		Object ret = connections.get(address);
		if(ret instanceof TCPOutputConnection && ((TCPOutputConnection)ret).isClosed())
		{
			removeConnection(address);
			ret = null;
		}
		
		if(ret instanceof TCPDeadConnection)
		{
			TCPDeadConnection dead = (TCPDeadConnection)ret;
			// Reset connection if connection should be retried.
			if(dead.shouldRetry())
			{
				connections.remove(address);
				ret = null; 
			}
		}
		
		if(ret==null && create)
			ret = createConnection(address);
		if(ret instanceof TCPDeadConnection)
			ret = null;
		
		return (TCPOutputConnection)ret;
	}
	
	/**
	 *  Create a outgoing connection.
	 *  @param address The connection address.
	 *  @return the connection to this address
	 */
	protected TCPOutputConnection createConnection(String address)
	{
		TCPOutputConnection ret = null;
		
		address = address.toLowerCase();
		if(address.startsWith(getServiceSchema()))
		{
			// Parse the address
			// todo: handle V6 ip adresses (0:0:0:0 ...)
			try
			{
				int schemalen = getServiceSchema().length();
				int div = address.indexOf(':', schemalen);
				String hostname;
				int iport;
				if(div>0)
				{
					hostname = address.substring(schemalen, div);
					iport = Integer.parseInt(address.substring(div+1));
				}
				else
				{
					hostname = address.substring(schemalen);
					iport = DEFAULT_PORT;
				}

				// todo: which resource identifier to use for outgoing connections?
				ret = new TCPOutputConnection(InetAddress.getByName(hostname), iport, new Cleaner(address));
				connections.put(address, ret);
			}
			catch(Exception e)
			{ 
				connections.put(address, new TCPDeadConnection());
				
//				logger.warning("Could not create connection: "+e.getMessage());
				//e.printStackTrace();
			}
		}
		
		return ret;
	}
	
	/**
	 *  Remove a cached connection.
	 *  @param address The address.
	 */
	protected void removeConnection(String address)
	{
		address = address.toLowerCase();
		
		Object con = connections.remove(address);
		if(con instanceof TCPOutputConnection)
			((TCPOutputConnection)con).close();
	}
	
	/**
	 *  Deliver messages to local message service
	 *  for dispatching to the components.
	 *  @param con The connection.
	 */
	protected IFuture<Void> deliverMessages(final TCPInputConnection con)
	{
		final Future<Void> ret = new Future<Void>();
		SServiceProvider.getService(container, IMessageService.class, RequiredServiceInfo.SCOPE_PLATFORM)
			.addResultListener(new ExceptionDelegationResultListener<IMessageService, Void>(ret)
		{
			public void customResultAvailable(IMessageService ms)
			{
				try
				{
					for(byte[] msg=con.read(); msg!=null; msg=con.read())
					{
						ms.deliverMessage(msg);
					}
					con.close();
					ret.setResult(null);
				}
				catch(Exception e)
				{
//					logger.warning("TCPTransport receiving error: "+e);
//					e.printStackTrace();
					con.close();
					ret.setException(e);
				}
			}
		});
		
		return ret;
	}
	
	/**
	 *  Class for cleaning output connections after 
	 *  max keep alive time has been reached.
	 */
	protected class Cleaner
	{
		//-------- attributes --------
		
		/** The address of the connection. */
		protected String address;
		
		/** The timer task. */
		protected TimerTask timertask;
		
		//-------- constructors --------
		
		/**
		 *  Cleaner for a specified output connection.
		 *  @param address The address.
		 */
		public Cleaner(String address)
		{
			this.address = address;
		}
		
		//-------- methods --------
		
		/**
		 *  Refresh the timeout.
		 */
		public void refresh()
		{
			if(timer==null)
			{
				timer	= new Timer(true);
			}
			
			if(timertask!=null)
			{
				timertask.cancel();
			}
			timertask	= new TimerTask()
			{
				public void run()
				{
					logger.info("Timeout reached for: "+address);
					removeConnection(address);						
				}
			};
			timer.schedule(timertask, MAX_KEEPALIVE);
		}
		
		/**
		 *  Remove this cleaner.
		 */
		public void remove()
		{
			if(timertask!=null)
			{
				timertask.cancel();
			}
		}
	}
}
