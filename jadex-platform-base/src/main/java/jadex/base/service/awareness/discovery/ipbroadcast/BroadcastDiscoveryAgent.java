package jadex.base.service.awareness.discovery.ipbroadcast;

import jadex.base.service.awareness.discovery.ConnectionException;
import jadex.base.service.awareness.discovery.DiscoveryAgent;
import jadex.base.service.awareness.discovery.MasterSlaveDiscoveryAgent;
import jadex.base.service.awareness.discovery.ReceiveHandler;
import jadex.base.service.awareness.discovery.SendHandler;
import jadex.bridge.service.types.awareness.AwarenessInfo;
import jadex.commons.SUtil;
import jadex.commons.future.DefaultResultListener;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.AgentArgument;
import jadex.micro.annotation.Argument;
import jadex.micro.annotation.Arguments;
import jadex.micro.annotation.Description;

import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 *  Agent that sends multicasts to locate other Jadex awareness agents.
 */
@Description("This agent looks for other awareness agents in the local net.")
@Arguments(
{
	@Argument(name="port", clazz=int.class, defaultvalue="55670", description="The port used for finding other agents.")
})
@Agent
public class BroadcastDiscoveryAgent extends MasterSlaveDiscoveryAgent
{
	/** The receiver port. */
	@AgentArgument
	protected int port;
		
	/** The socket. */
	protected DatagramSocket socket;
	
	/** The receive buffer. */
	protected byte[] buffer;

	//-------- methods --------
	
	/**
	 *  Create the send handler.
	 */
	public SendHandler createSendHandler()
	{
		return new BroadcastSendHandler(this);
	}
	
	/**
	 *  Create the receive handler.
	 */
	public ReceiveHandler createReceiveHandler()
	{
		return new BroadcastReceiveHandler(this);
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
//		System.out.println("isMaster: "+(getSocket()!=null && this.port==getSocket().getLocalPort()));
		return getSocket()!=null && this.port==getSocket().getLocalPort();
	}
	
	/**
	 *  Create the master id.
	 */
	protected String createMasterId()
	{
		return isMaster()? createMasterId(SUtil.getInetAddress(),
			getSocket().getLocalPort()): null;
	}
	
	/**
	 *  Get the local master id.
	 */
	protected String getMyMasterId()
	{
		return createMasterId(SUtil.getInetAddress(), port);
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
			getSocket();
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
			if(socket!=null)
			{
				socket.close();
				socket = null;
			}
		}
		catch(Exception e)
		{
		}
	}
	
	/**
	 *  Get or create a receiver socket.
	 *  
	 *  Note, this method has to be synchronized.
	 *  Is called from receiver as well as component thread.
	 */
	protected synchronized DatagramSocket getSocket()
	{
		if(!isKilled())
		{
			if(socket==null)
			{
				try
				{
					socket = new DatagramSocket(port);
					socket.setBroadcast(true);
//					System.out.println("local master at: "+SUtil.getInet4Address()+" "+port);
					getMicroAgent().getLogger().info("local master at: "+SUtil.getInetAddress()+" "+port);
				}
				catch(Exception e)
				{
					try
					{
						// In case the receiversocket cannot be opened
						// open another local socket at an arbitrary port
						// and send this port to the master.
						socket = new DatagramSocket();
						socket.setBroadcast(true);
						
						createAwarenessInfo(AwarenessInfo.STATE_ONLINE, createMasterId())
							.addResultListener(agent.createResultListener(new DefaultResultListener<AwarenessInfo>()
						{
							public void resultAvailable(AwarenessInfo info)
							{
								InetAddress address = SUtil.getInetAddress();
//								AwarenessInfo info = createAwarenessInfo(AwarenessInfo.STATE_ONLINE, createMasterId());
//								byte[] data = DiscoveryState.encodeObject(info, getMicroAgent().getModel().getClassLoader());
								byte[] data = DiscoveryAgent.encodeObject(info, getMicroAgent().getClassLoader());
								((BroadcastSendHandler)sender).send(data, address, port);
//								System.out.println("local slave at: "+SUtil.getInet4Address()+" "+socket.getLocalPort());
								getMicroAgent().getLogger().info("local slave at: "+SUtil.getInetAddress()+" "+socket.getLocalPort());
							}
						}));
					}
					catch(Exception e2)
					{
//						e2.printStackTrace();
						getMicroAgent().getLogger().warning("Awareness error when creating broadcase socket: "+e);
						throw new ConnectionException(e2);
					}
				}
			}
		}

		return socket;
	}
}
