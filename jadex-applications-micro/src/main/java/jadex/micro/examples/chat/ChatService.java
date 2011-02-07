package jadex.micro.examples.chat;

import jadex.bridge.IInternalAccess;
import jadex.commons.ChangeEvent;
import jadex.commons.IRemoteChangeListener;
import jadex.commons.future.IResultListener;
import jadex.commons.service.BasicService;

import java.util.ArrayList;
import java.util.List;

/**
 *  Chat service implementation.
 */
public class ChatService extends BasicService implements IChatService
{
	//-------- attributes --------
	
	/** The agent. */
	protected IInternalAccess agent;
	
	/** The listeners. */
	protected List listeners;
	
	/** The chat gui. */
	protected ChatPanel chatpanel;
	
	//-------- constructors --------
	
	/**
	 *  Create a new helpline service.
	 */
	public ChatService(IInternalAccess agent)
	{
		super(agent.getServiceProvider().getId(), IChatService.class, null);
		this.agent = agent;
		this.listeners = new ArrayList();
		this.chatpanel = ChatPanel.createGui(agent.getExternalAccess());
	}
	
	//-------- methods --------
	
	/**
	 *  Hear something.
	 *  @param name The name.
	 *  @param text The text.
	 */
	public void hear(String name, String text)
	{
		for(int i=0; i<listeners.size(); i++)
		{
//			System.out.println("listeners: "+listeners);
			final IRemoteChangeListener lis = (IRemoteChangeListener)listeners.get(i);
			lis.changeOccurred(new ChangeEvent(name, null, text))
				.addResultListener(agent.createResultListener(new IResultListener()
			{
				public void resultAvailable(Object result)
				{
				}
				
				public void exceptionOccurred(Exception exception)
				{
//					exception.printStackTrace();
					listeners.remove(lis);
				}
			}));
		}
	}
	
	/**
	 *  Add a local listener.
	 */
	public void addChangeListener(IRemoteChangeListener listener)
	{
		listeners.add(listener);
	}
	
	/**
	 *  Remove a local listener.
	 */
	public void removeChangeListener(IRemoteChangeListener listener)
	{
		listeners.remove(listener);
	}
	
	/**
	 *  Get the string representation.
	 *  @return The string representation.
	 */
	public String toString()
	{
		return "ChatService, "+agent.getComponentIdentifier();
	}
}
