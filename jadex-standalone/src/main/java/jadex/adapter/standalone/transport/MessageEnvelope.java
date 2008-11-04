package jadex.adapter.standalone.transport;

import jadex.bridge.IAgentIdentifier;
import jadex.commons.SReflect;
import jadex.commons.SUtil;

import java.util.Map;

/**
 *  The message envelope holding the native message,
 *  the receivers and the message type.
 */
public class MessageEnvelope //implements IMessageEnvelope
{
	//-------- attributes --------

	/** The message. */
	protected Map message;
	
	/** The receivers. */
	protected IAgentIdentifier[] receivers;
	
	/** The message type. */
	protected String message_type;
	
	//-------- constructors --------

	/**
	 *  Create a new message envelope.
	 *  (bean constructor)
	 */
	public MessageEnvelope()
	{
	}
	
	/**
	 *  Create a new message envelope.
	 */
	public MessageEnvelope(Map message, IAgentIdentifier[] receivers, String message_type)
	{
		this.message = message;
		this.receivers = receivers;
		this.message_type = message_type;
	}

	//-------- methods --------

	/**
	 *  Get native message.
	 *  @return The native message.
	 */
	public Map getMessage()
	{
		return message;
	}
	
	/**
	 *  Set native message.
	 *  @param message The native message.
	 */
	public void setMessage(Map message)
	{
		this.message = message;
	}
	
	/**
	 * Get the receivers.
	 */
	public IAgentIdentifier[] getReceivers()
	{
		return receivers;
	}
	
	/**
	 * Get the receivers.
	 */
	public void setReceivers(IAgentIdentifier[] receivers)
	{
		this.receivers = receivers;
	}

	/**
	 *  Set the type (e.g. "fipa").
	 * @param messagetypename 
	 */
	public void setTypeName(String messagetypename)
	{
		message_type = messagetypename;
	}

	/**
	 *  Get the type (e.g. "fipa").
	 */
	public String getTypeName()
	{
		return message_type;
	}
	
	/**
	 *  Get the string representation.
	 *  @return The string representation.
	 */
	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		sb.append(SReflect.getInnerClassName(this.getClass())+"(");
		//sb.append("sender: "+getSender()+", ");
		sb.append("receivers: "+SUtil.arrayToString(getReceivers())+", ");
		sb.append("message type: "+message_type);
		sb.append("raw values: "+message);
//		sb.append(super.toString());
		sb.append(")");
		return sb.toString();
	}
}
