package jadex.base.service.awareness.discovery.ipscanner;

import jadex.base.service.awareness.AwarenessInfo;
import jadex.base.service.awareness.discovery.DiscoveryState;
import jadex.base.service.awareness.discovery.IDiscoveryService;
import jadex.base.service.awareness.discovery.MasterSlaveDiscoveryAgent;
import jadex.base.service.awareness.discovery.ReceiveHandler;
import jadex.base.service.awareness.discovery.SendHandler;
import jadex.base.service.awareness.discovery.ipbroadcast.BroadcastSendHandler;
import jadex.base.service.awareness.management.IManagementService;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.bridge.service.threadpool.IThreadPoolService;
import jadex.commons.SUtil;
import jadex.micro.annotation.AgentArgument;
import jadex.micro.annotation.Argument;
import jadex.micro.annotation.Arguments;
import jadex.micro.annotation.Binding;
import jadex.micro.annotation.Configuration;
import jadex.micro.annotation.Configurations;
import jadex.micro.annotation.Description;
import jadex.micro.annotation.Implementation;
import jadex.micro.annotation.NameValue;
import jadex.micro.annotation.ProvidedService;
import jadex.micro.annotation.ProvidedServices;
import jadex.micro.annotation.RequiredService;
import jadex.micro.annotation.RequiredServices;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

/**
 *  Agent that sends multicasts to locate other Jadex awareness agents.
 */
@Description("This agent looks for other awareness agents in the local net.")
@Arguments(
{
	@Argument(name="port", clazz=int.class, defaultvalue="55668", description="The port used for finding other agents."),
	@Argument(name="delay", clazz=long.class, defaultvalue="10000", description="The delay between sending awareness infos (in milliseconds)."),
	@Argument(name="fast", clazz=boolean.class, defaultvalue="true", description="Flag for enabling fast startup awareness (pingpong send behavior)."),
	@Argument(name="scanfactor", clazz=long.class, defaultvalue="1", description="The delay between scanning as factor of delay time, e.g. 1=10000, 2=20000."),
	@Argument(name="buffersize", clazz=int.class, defaultvalue="1024*1024", description="The size of the send buffer (determines the number of messages that can be sent at once).")
})
@Configurations(
{
	@Configuration(name="Frequent updates (10s)", arguments=@NameValue(name="delay", value="10000")),
	@Configuration(name="Medium updates (20s)", arguments=@NameValue(name="delay", value="20000")),
	@Configuration(name="Seldom updates (60s)", arguments=@NameValue(name="delay", value="60000"))
})
@ProvidedServices(
	@ProvidedService(type=IDiscoveryService.class, implementation=@Implementation(expression="$component"))
)
@RequiredServices(
{
	@RequiredService(name="threadpool", type=IThreadPoolService.class, binding=@Binding(scope=RequiredServiceInfo.SCOPE_PLATFORM)),
	@RequiredService(name="management", type=IManagementService.class, binding=@Binding(scope=RequiredServiceInfo.SCOPE_PLATFORM))
})
public class ScannerDiscoveryAgent extends MasterSlaveDiscoveryAgent
{
	/** The receiver port. */
	@AgentArgument
	protected int port;
	
	/** The scan delay factor. */
	@AgentArgument
	protected int scanfactor;
	
	/** The buffer size. */
	@AgentArgument
	protected int buffersize;

	/** The socket to receive. */
	protected DatagramChannel channel;
	protected Selector selector;
	

	/**
	 *  Create the send handler.
	 */
	public SendHandler createSendHandler()
	{
		return new ScannerSendHandler(this);
	}
	
	/**
	 *  Create the receive handler.
	 */
	public ReceiveHandler createReceiveHandler()
	{
		return new ScannerReceiveHandler(this);
	}
	
	/**
	 *  Get the scanfactor.
	 *  @return the scanfactor.
	 */
	public int getScanFactor()
	{
		return scanfactor;
	}

	/**
	 *  Get the port.
	 *  @return the port.
	 */
	public int getPort()
	{
		return port;
	}

	/**
	 *  Test if is master.
	 */
	protected boolean isMaster()
	{
		return getChannel()!=null && this.port==getChannel().socket().getLocalPort();
	}
	
	/**
	 *  Create the master id.
	 */
	protected String createMasterId()
	{
		return isMaster()? createMasterId(SUtil.getInet4Address(),
			getChannel().socket().getLocalPort()): null;
	}
	
	/**
	 *  Get the local master id.
	 */
	protected String getMyMasterId()
	{
		return createMasterId(SUtil.getInet4Address(), port);
	}
	
	/**
	 *  Create the master id.
	 */
	protected String createMasterId(InetAddress address, int port)
	{
		return address+":"+port;
	}
	
	/**
	 *  (Re)init receiving.
	 */
	public synchronized void initNetworkRessource()
	{
		try
		{
			terminateNetworkRessource();
			getChannel();
		}
		catch(Exception e)
		{
		}
	}
	
	/**
	 *  Terminate receiving.
	 */
	protected synchronized void terminateNetworkRessource()
	{
		try
		{
			if(channel!=null)
			{
				channel.close();
				channel = null;
			}
		}
		catch(Exception e)
		{
		}
	}
	
	/**
	 *  Get or create a channel.
	 */
	protected synchronized DatagramChannel getChannel()
	{
		if(!isKilled())
		{
			if(channel==null)
			{
				try
				{
					channel = DatagramChannel.open();
					channel.configureBlocking(false);
					channel.socket().bind(new InetSocketAddress(port));
					channel.socket().setSendBufferSize(buffersize);
					// Register blocks when other thread waits in it.
					// Must be synchronized due to selector wakeup freeing other thread.
					synchronized(this)
					{
						if(selector==null)
							selector = Selector.open();
						selector.wakeup();
						channel.register(selector, SelectionKey.OP_READ);
					}
//					System.out.println("local master at: "+SUtil.getInet4Address()+" "+port);
				}
				catch(Exception e)
				{
					try
					{
						// In case the receiversocket cannot be opened
						// open another local socket at an arbitrary port
						// and send this port to the master.
						channel = DatagramChannel.open();
						channel.configureBlocking(false);
						channel.socket().bind(new InetSocketAddress(0));
						channel.socket().setSendBufferSize(buffersize);
						synchronized(this)
						{
							if(selector==null)
								selector = Selector.open();
							selector.wakeup();
							channel.register(selector, SelectionKey.OP_READ);
						}
						InetAddress address = SUtil.getInet4Address();
						AwarenessInfo info = createAwarenessInfo(AwarenessInfo.STATE_OFFLINE, createMasterId());
						byte[] data = DiscoveryState.encodeObject(info, getMicroAgent().getModel().getClassLoader());
						((ScannerSendHandler)sender).send(data, address, port);
						
//						System.out.println("local slave at: "+SUtil.getInet4Address()+" "+channel.socket().getLocalPort());
//						getLogger().warning("Running in local mode: "+e);
					}
					catch(Exception e2)
					{
//						e2.printStackTrace();
						getMicroAgent().getLogger().warning("Channel problem: "+e2);
						throw new RuntimeException(e2);
					}
				}
			}
		}
		
		return channel;
	}
}
