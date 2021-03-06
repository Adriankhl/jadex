package jadex.android.exampleproject.simple;

import jadex.android.exampleproject.MyEvent;
import jadex.bridge.IInternalAccess;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.bridge.service.component.IRequiredServicesFeature;
import jadex.bridge.service.types.context.IContextService;
import jadex.commons.future.IFuture;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.AgentBody;
import jadex.micro.annotation.AgentKilled;
import jadex.micro.annotation.Description;

/**
 *  Simple example agent that shows messages
 *  when it is started or stopped.
 */
@Description("Sample Android Agent.")
@Agent
public class MyAgent
{
	//-------- methods --------
	
	/**
	 *  Called when the agent is started.
	 */
	@AgentBody
	public IFuture<Void> body(IInternalAccess agent)
	{
		showAndroidMessage("This is Agent <<" + agent.getComponentIdentifier().getLocalName() + ">> saying hello!", agent);
		return IFuture.DONE;
	}
	

	/**
	 *  Called when the agent is killed.
	 */
	//@AgentKilled
	@OnEnd
	public IFuture<Void> agentKilled(IInternalAccess agent)
	{
		showAndroidMessage("This is Agent <<" + agent.getComponentIdentifier().getLocalName() + ">> saying goodbye!", agent);
		return IFuture.DONE;
	}

	//-------- helper methods --------

	/**
	 *	Show a message on the device.  
	 *  @param msg The message to be shown.
	 */
	protected void showAndroidMessage(String txt, IInternalAccess agent)
	{
		MyEvent myEvent = new MyEvent();
		myEvent.setMessage(txt);
		
		IContextService	cs	= agent.getFeature(IRequiredServicesFeature.class).searchService(IContextService.class, ServiceScope.PLATFORM).get();
		
		cs.dispatchEvent(myEvent).get();
	}
}
