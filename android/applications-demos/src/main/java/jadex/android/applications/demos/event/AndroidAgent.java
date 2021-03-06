package jadex.android.applications.demos.event;

import jadex.bridge.IInternalAccess;
import jadex.bridge.component.IMessageFeature;
import jadex.bridge.fipa.SFipa;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.bridge.service.types.context.IContextService;
import jadex.commons.future.DefaultResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.AgentBody;
import jadex.micro.annotation.AgentFeature;
import jadex.micro.annotation.AgentMessageArrived;
import jadex.micro.annotation.AgentServiceQuery;
import jadex.micro.annotation.AgentServiceSearch;
import jadex.micro.annotation.Description;
import jadex.micro.annotation.RequiredService;
import jadex.micro.annotation.RequiredServices;

import java.util.Map;

import android.util.Log;

/**
 *  Simple example agent that shows messages
 *  when it is started, stopped and when it receives a message. 
 */
@Description("Sample Android Agent.")
@RequiredServices({
		@RequiredService(name="androidcontext", type=IContextService.class)
})
@Agent
public class AndroidAgent
{
	/** This field is injected by jadex. */
	@Agent
	protected IInternalAccess	agent;

	@AgentFeature
	protected IMessageFeature messageFeature;

	@AgentServiceSearch
	protected IContextService androidcontext;

	//-------- methods --------
	
	/**
	 *  Called when the agent is started.
	 */
	@AgentBody
	public IFuture<Void> executeBody()
	{

		showAndroidMessage("This is Agent <<" + agent.getId().getLocalName() + ">> saying hello!");
		return new Future<Void>();
	}


	@AgentMessageArrived
	public void handleMessage(Map<String, Object> msg) {
		if (msg.get(SFipa.CONTENT).equals("ping")) {
			showAndroidMessage(agent.getId().getLocalName()  + ": pong");
		}
	}
	

	/**
	 *  Called when the agent is killed.
	 */
	public IFuture<Void> agentKilled()
	{
		showAndroidMessage("This is Agent <<" + agent.getId().getLocalName() + ">> saying goodbye!");
		return IFuture.DONE;
	}

	//-------- helper methods --------

	/**
	 *	Show a message on the device.  
	 *  @param msg The message to be shown.
	 */
	protected void showAndroidMessage(String msg)
	{
		final ShowToastEvent event = new ShowToastEvent();
		event.setMessage(msg);
		IFuture<Boolean> dispatchUiEvent = androidcontext.dispatchEvent(event);
		dispatchUiEvent.addResultListener(new DefaultResultListener<Boolean>() {

			@Override
			public void resultAvailable(Boolean result) {
				Log.d("Agent", "dispatched: " + result);
			}
		});
	}
}
