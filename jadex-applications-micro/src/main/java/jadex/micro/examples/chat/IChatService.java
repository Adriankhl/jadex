package jadex.micro.examples.chat;

import jadex.bridge.service.annotation.Security;
import jadex.commons.future.IFuture;


/**
 *  Service can receive chat messages.
 */
@Security(Security.UNRESTRICTED)
public interface IChatService
{
	//-------- constants --------
	
	/** The default user state. */
	public static final String	STATE_IDLE	= "idle";
	
	/** The user typing a message. */
	public static final String	STATE_TYPING	= "typing";
	
	/** The state for a disconnected user. */
	public static final String	STATE_DEAD	= "dead";
	
	//-------- methods --------
	
	/**
	 *  Post a message
	 *  @param text The text message.
	 */
	public IFuture<Void>	message(String text);
	
	/**
	 *  Post a status change.
	 *  @param status The new status.
	 */
	public IFuture<Void>	status(String status);
}
