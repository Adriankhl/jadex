package jadex.android.exampleproject.extended.agent;

import jadex.android.exampleproject.MyEvent;
import jadex.bridge.IInternalAccess;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.bridge.service.annotation.Service;
import jadex.bridge.service.types.context.IContextService;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.AgentService;
import jadex.micro.annotation.Binding;
import jadex.micro.annotation.Description;
import jadex.micro.annotation.ProvidedService;
import jadex.micro.annotation.ProvidedServices;
import jadex.micro.annotation.RequiredService;
import jadex.micro.annotation.RequiredServices;

/**
 *  Simple example agent that shows messages
 *  when it is started or stopped. 
 */
@Description("Sample Android Agent.")
@ProvidedServices({
	@ProvidedService(name="agentinterface", type=IAgentInterface.class)
})
@RequiredServices({
	@RequiredService(name="context", type=IContextService.class, binding=@Binding(scope=RequiredServiceInfo.SCOPE_PLATFORM))
})
@Service
@Agent
public class MyAgent	implements IAgentInterface
{
	/** Context service injected at startup. */
	@AgentService
	protected IContextService	context;
	
	//-------- methods --------
	/**
	 *  Called when the agent is started.
	 */
	public IFuture<Void> executeBody()
	{
//		showAndroidMessage("This is Agent <<" + this.getAgentName() + ">> saying hello!");
		return new Future<Void>();
	}
	
	

	/**
	 *  Called when the agent is killed.
	 */
	public IFuture<Void> agentKilled(IInternalAccess agent)
	{
		showAndroidMessage("This is Agent <<" + agent.getComponentIdentifier().getLocalName() + ">> saying goodbye!");
		return IFuture.DONE;
	}

	//-------- helper methods --------

	/**
	 *	Show a message on the device.  
	 *  @param msg The message to be shown.
	 */
	protected void showAndroidMessage(String txt)
	{
		MyEvent myEvent = new MyEvent();
		myEvent.setMessage(txt);
		context.dispatchEvent(myEvent);
	}



	@Override
	public void callAgent(String message)
	{
		showAndroidMessage("I was called with: " + message);
	}
	
}
