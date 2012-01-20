package jadex.base.service.message.transport.httprelaymtp.nio;

import java.nio.channels.SelectionKey;

/**
 *  Handler interface for managing NIO operations.
 */
public interface IHttpRequest
{
	/**
	 *  Write the HTTP request to the NIO connection.
	 *  May be called multiple times, if not all data can be send at once.
	 *  Has to change the interest to OP_READ, once all data is sent.
	 */
	public void handleWrite(SelectionKey key);
	
	/**
	 *  Receive the HTTP response from the NIO connection.
	 *  May be called multiple times, if not all data can be send at once.
	 *  Has to deregister interest in the connection, once required data is received.
	 *  May close the connection or leave it open for reuse if the server supports keepalive.
	 */
	public void	handleRead(SelectionKey key);
}
