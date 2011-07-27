package jadex.base.service.awareness.discovery.ipbroadcast;

import jadex.base.service.awareness.discovery.MasterSlaveReceiveHandler;

import java.net.DatagramPacket;

/**
 * 
 */
public class BroadcastReceiveHandler extends MasterSlaveReceiveHandler
{
	/** The receive buffer. */
	protected byte[] buffer;
	
	/**
	 *  Create a new receive handler.
	 */
	public BroadcastReceiveHandler(BroadcastDiscoveryAgent agent)
	{
		super(agent);
	}
	
	/**
	 *  Receive a packet.
	 */
	public Object[] receive()
	{
		Object[] ret = null;
		try
		{

			if(buffer==null)
			{
				// todo: max ip datagram length (is there a better way to determine length?)
				buffer = new byte[8192];
			}

			final DatagramPacket pack = new DatagramPacket(buffer, buffer.length);
			getAgent().getSocket().receive(pack);
			byte[] data = new byte[pack.getLength()];
			System.arraycopy(buffer, 0, data, 0, pack.getLength());
			ret = new Object[]{pack.getAddress(), new Integer(pack.getPort()), data};
		}
		catch(Exception e)
		{
//			getAgent().getMicroAgent().getLogger().warning("Message receival error: "+e);
		}
		
		return ret;
	}
	
	/**
	 *  Get the agent.
	 */
	public BroadcastDiscoveryAgent getAgent()
	{
		return (BroadcastDiscoveryAgent)agent;
	}
}
