package jadex.base.service.awareness.discovery.ipmulticast;

import jadex.base.service.awareness.AwarenessInfo;
import jadex.base.service.awareness.discovery.DiscoveryAgent;
import jadex.base.service.awareness.discovery.DiscoveryState;
import jadex.base.service.awareness.discovery.SendHandler;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 *  Handle sending.
 */
public class MulticastSendHandler extends SendHandler
{
	/**
	 *  Create a new lease time handling object.
	 */
	public MulticastSendHandler(DiscoveryAgent agent)
	{
		super(agent);
	}
	
	/**
	 *  Method to send messages.
	 */
	public void send(AwarenessInfo info)
	{
		try
		{
			byte[] data = DiscoveryState.encodeObject(info, getAgent().getMicroAgent().getClassLoader());
			Object[] ai = getAgent().getAddressInfo();
			send(data, (InetAddress)ai[0], ((Integer)ai[1]).intValue());
//			System.out.println(getComponentIdentifier()+" sent '"+info+"' ("+data.length+" bytes)"+" "+port+" "+address);
		}
		catch(Exception e)
		{
			getAgent().getMicroAgent().getLogger().warning("Could not send awareness message: "+e);
//			e.printStackTrace();
		}
	}
	
	/**
	 *  Send a packet.
	 */
	public boolean send(byte[] data, InetAddress address, int port)
	{
//		System.out.println("sent packet: "+address+" "+port);
		boolean ret = true;
		try
		{
			DatagramPacket p = new DatagramPacket(data, data.length, new InetSocketAddress(address, port));
			getAgent().getSocket().send(p);
		}
		catch(Exception e)
		{
			ret = false;
		}
		return ret;
	}
	
	/**
	 *  Get the agent.
	 */
	protected MulticastDiscoveryAgent getAgent()
	{
		return (MulticastDiscoveryAgent)agent;
	}
}