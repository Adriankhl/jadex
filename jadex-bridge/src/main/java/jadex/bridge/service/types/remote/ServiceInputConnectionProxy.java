package jadex.bridge.service.types.remote;

import jadex.bridge.IComponentIdentifier;
import jadex.bridge.IInputConnection;
import jadex.bridge.IOutputConnection;
import jadex.commons.future.IFuture;
import jadex.commons.future.IIntermediateFuture;
import jadex.commons.future.ISubscriptionIntermediateFuture;

/**
 * 
 */
public class ServiceInputConnectionProxy implements IInputConnection
{
	/** The original connection. */
	protected ServiceOutputConnection con;
	
	/** The connection id. */
	protected int conid;
	
	/**
	 * 
	 */
	public ServiceInputConnectionProxy()
	{
		// Bean constructor.
	}
	
	/**
	 * 
	 */
	public ServiceInputConnectionProxy(ServiceOutputConnection con)
	{
		this.con = con;
	}
	
	/**
	 * 
	 */
	public void setOutputConnection(IOutputConnection ocon)
	{
		con.setOutputConnection(ocon);
	}
	
	/**
	 *  Get the connectionid.
	 *  @return The connectionid.
	 */
	public int getConnectionId()
	{
		return conid;
	}

	/**
	 *  Set the connectionid.
	 *  @param connectionid The connectionid to set.
	 */
	public void setConnectionId(int conid)
	{
		this.conid = conid;
	}

	/**
	 * 
	 */
	public int read(byte[] buffer)
	{
		throw new UnsupportedOperationException();
	}
	
	/**
	 * 
	 */
	public int read()
	{
		throw new UnsupportedOperationException();
	}
	
	/**
	 *  Get the number of available bytes.
	 *  @return The number of available bytes. 
	 */
	public int available()
	{
		throw new UnsupportedOperationException();
	}
	
	/**
	 * 
	 */
	public void close()
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * 
	 */
	public IComponentIdentifier getInitiator()
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * 
	 */
	public IComponentIdentifier getParticipant()
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * 
	 */
	public ISubscriptionIntermediateFuture<byte[]> aread()
	{
		throw new UnsupportedOperationException();
	}
	
	
//	public IFuture<Byte> areadNext()
//	{
//		throw new UnsupportedOperationException();
//	}
}