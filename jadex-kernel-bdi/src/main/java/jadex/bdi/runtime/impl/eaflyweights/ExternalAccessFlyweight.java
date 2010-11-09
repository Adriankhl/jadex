package jadex.bdi.runtime.impl.eaflyweights;

import jadex.bdi.runtime.IBDIExternalAccess;
import jadex.bdi.runtime.IEAGoal;
import jadex.bdi.runtime.IEAInternalEvent;
import jadex.bdi.runtime.IEAMessageEvent;
import jadex.bdi.runtime.impl.SFlyweightFunctionality;
import jadex.bdi.runtime.impl.WakeupAction;
import jadex.bdi.runtime.impl.flyweights.ElementFlyweight;
import jadex.bdi.runtime.interpreter.GoalLifecycleRules;
import jadex.bdi.runtime.interpreter.InternalEventRules;
import jadex.bdi.runtime.interpreter.InterpreterTimedObject;
import jadex.bdi.runtime.interpreter.MessageEventRules;
import jadex.bdi.runtime.interpreter.OAVBDIRuntimeModel;
import jadex.bdi.runtime.interpreter.PlanRules;
import jadex.bridge.ComponentResultListener;
import jadex.bridge.IModelInfo;
import jadex.commons.Future;
import jadex.commons.IFuture;
import jadex.commons.IResultCommand;
import jadex.commons.SUtil;
import jadex.commons.concurrent.DelegationResultListener;
import jadex.commons.concurrent.IResultListener;
import jadex.rules.state.IOAVState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

/**
 *  External access interface.
 */
public class ExternalAccessFlyweight extends EACapabilityFlyweight implements IBDIExternalAccess
{
	//-------- constructors --------
	
	/**
	 *  Create a new capability flyweight.
	 *  @param state	The state.
	 *  @param scope	The scope handle.
	 *  @param adapter	The adapter.
	 */
	public ExternalAccessFlyweight(IOAVState state, Object scope)
	{
		super(state, scope);
	}

	//-------- goalbase shortcut methods --------

	/**
	 *  Dispatch a new top-level goal.
	 *  @param goal The new goal.
	 *  Note: plan step is interrupted after call.
	 */
	public IFuture dispatchTopLevelGoal(final IEAGoal goal)
	{
		final Future ret = new Future();
		
//		getGoalbase().dispatchTopLevelGoal(goal);
		
		if(getInterpreter().isExternalThread())
		{
			getInterpreter().getAgentAdapter().invokeLater(new Runnable() 
			{
				public void run() 
				{
					GoalLifecycleRules.adoptGoal(getState(), ((ElementFlyweight)goal).getScope(), ((ElementFlyweight)goal).getHandle());
					ret.setResult(null);
				}
			});
		}
		else
		{
			GoalLifecycleRules.adoptGoal(getState(), ((ElementFlyweight)goal).getScope(), ((ElementFlyweight)goal).getHandle());
			ret.setResult(null);
		}
		
		return ret;
	}

	/**
	 *  Create a goal from a template goal.
	 *  To be processed, the goal has to be dispatched as subgoal
	 *  or adopted as top-level goal.
	 *  @param type	The template goal name as specified in the ADF.
	 *  @return The created goal.
	 */
	public IFuture createGoal(final String type)
	{
//		return getGoalbase().createGoal(type);
		
		final Future ret = new Future();
		
		if(getInterpreter().isExternalThread())
		{
			getInterpreter().getAgentAdapter().invokeLater(new Runnable() 
			{
				public void run() 
				{
					ret.setResult(SFlyweightFunctionality.createGoal(getState(), getScope(), true, type));
				}
			});
		}
		else
		{
			ret.setResult(SFlyweightFunctionality.createGoal(getState(), getScope(), true, type));
		}
		
		return ret;
	}

	//-------- eventbase shortcut methods --------

	/**
	 *  Send a message after some delay.
	 *  @param me	The message event.
	 *  @return The filter to wait for an answer.
	 */
	public IFuture	sendMessage(final IEAMessageEvent me)
	{
		IFuture	ret;
		if(getInterpreter().isExternalThread())
		{
			final Future	future	= new Future();
			getInterpreter().getAgentAdapter().invokeLater(new Runnable()
			{
				public void run()
				{
					Object revent = ((ElementFlyweight)me).getHandle();
					MessageEventRules.sendMessage(getState(), getScope(), revent).addResultListener(new DelegationResultListener(future));
				}
			});
			ret	= future;
		}
		else
		{
			Object revent = ((ElementFlyweight)me).getHandle();
			ret	= MessageEventRules.sendMessage(getState(), getScope(), revent);
		}
		
		return ret;
	}

	/**
	 *  Dispatch an internal event.
	 *  @param event The event.
	 *  Note: plan step is interrupted after call.
	 */
	public IFuture dispatchInternalEvent(final IEAInternalEvent event)
	{
		final Future ret = new Future();
		
//		getEventbase().dispatchInternalEvent(event);
		
		if(getInterpreter().isExternalThread())
		{
			getInterpreter().getAgentAdapter().invokeLater(new Runnable() 
			{
				public void run() 
				{
					InternalEventRules.adoptInternalEvent(getState(), getScope(), ((ElementFlyweight)event).getHandle());
					ret.setResult(null);
				}
			});
		}
		else
		{
			InternalEventRules.adoptInternalEvent(getState(), getScope(), ((ElementFlyweight)event).getHandle());
			ret.setResult(null);
		}
		
		return ret;
	}

	/**
	 *  Create a new message event.
	 *  @return The new message event.
	 */
	public IFuture createMessageEvent(final String type)
	{
//		return getEventbase().createMessageEvent(type);
		
		final Future ret = new Future();
		
		if(getInterpreter().isExternalThread())
		{
			getInterpreter().getAgentAdapter().invokeLater(new Runnable() 
			{
				public void run() 
				{
					ret.setResult(SFlyweightFunctionality.createMessageEvent(getState(), getScope(), type, true));
				}
			});
		}
		else
		{
			ret.setResult(SFlyweightFunctionality.createMessageEvent(getState(), getScope(), type, true));
		}
		
		return ret;
	}

	/**
	 *  Create a new intenal event.
	 *  @return The new intenal event.
	 */
	public IFuture createInternalEvent(final String type)
	{
//		return getEventbase().createInternalEvent(type);
		
		final Future ret = new Future();
		
		if(getInterpreter().isExternalThread())
		{
			getInterpreter().getAgentAdapter().invokeLater(new Runnable() 
			{
				public void run() 
				{
					ret.setResult(SFlyweightFunctionality.createInternalEvent(getState(), getScope(), type, true));
				}
			});
		}
		else
		{
			ret.setResult(SFlyweightFunctionality.createInternalEvent(getState(), getScope(), type, true));
		}
		
		return ret;
	}

	//-------- methods --------

	/**
	 *  Wait for a some time.
	 *  @param duration The duration.
	 */
	public IFuture waitFor(final long duration)
	{
		final Future ret = new Future();
		
		if(!getInterpreter().isPlanThread())
		{
			getInterpreter().getAgentAdapter().invokeLater(new Runnable() 
			{
				public void run() 
				{
					waitForExternalAccessWaitAbstraction(null, duration, ret);
				}
			});
		}
		else
		{
			PlanRules.waitForWaitAbstraction(null, duration, getState(), getScope(), getInterpreter().getCurrentPlan());
			ret.setResult(null);
		}
		
		return ret;
	}
	
	/**
	 *  Wait for a tick.
	 */
	public IFuture waitForTick()
	{
		final Future ret = new Future();
		
		if(!getInterpreter().isPlanThread())
		{
			getInterpreter().getAgentAdapter().invokeLater(new Runnable() 
			{
				public void run() 
				{
					waitForExternalAccessWaitAbstraction(null, PlanRules.TICK_TIMER, ret);
				}
			});
		}
		else
		{
			PlanRules.waitForWaitAbstraction(null, PlanRules.TICK_TIMER, getState(), getScope(), getInterpreter().getCurrentPlan());
			ret.setResult(null);
		}
		
		return ret;
	}

	/**
	 *  Wait for a condition to be satisfied.
	 *  @param condition The condition.
	 * /
	public void	waitForCondition(ICondition condition)
	{
		throw new UnsupportedOperationException();
	}*/

	/**
	 *  Wait for a condition or until the timeout occurs.
	 *  @param condition The condition.
	 *  @param timeout The timeout.
	 * /
	public void waitForCondition(ICondition condition, long timeout)
	{
		throw new UnsupportedOperationException();
	}*/

	/**
	 *  Wait for a condition to be satisfied.
	 *  @param condition The condition.
	 * /
	public void	waitForCondition(String condition)
	{
		throw new UnsupportedOperationException();
	}*/

	/**
	 *  Wait for a condition to be satisfied.
	 *  @param condition The condition.
	 * /
	public void	waitForCondition(String condition, long timeout)
	{
		throw new UnsupportedOperationException();
	}*/

	/**
	 *  Wait for an internal event.
	 *  @param type The internal event type.
	 */
	public IFuture waitForInternalEvent(String type)
	{
		return waitForInternalEvent(type, -1);
	}

	/**
	 *  Wait for an internal event.
	 *  @param type The internal event type.
	 *  @param timeout The timeout.
	 */
	public IFuture waitForInternalEvent(final String type, final long timeout)
	{
		final Future ret = new Future();
		
		if(!getInterpreter().isPlanThread())
		{
			getInterpreter().getAgentAdapter().invokeLater(new Runnable() 
			{
				public void run() 
				{
					Object	wa	= getState().createObject(OAVBDIRuntimeModel.waitabstraction_type);
					SFlyweightFunctionality.addInternalEvent(wa, type, getState(), getScope());
					getState().addExternalObjectUsage(wa, ExternalAccessFlyweight.this);
					waitForExternalAccessWaitAbstraction(wa, timeout, ret);
				}
			});
		}
		else
		{
			Object	wa	= getState().createObject(OAVBDIRuntimeModel.waitabstraction_type);
			SFlyweightFunctionality.addInternalEvent(wa, type, getState(), getScope());
			ret.setResult(PlanRules.waitForWaitAbstraction(wa, timeout, getState(), getScope(), getInterpreter().getCurrentPlan()));
		}
		
		return ret;
	}

	/**
	 *  Send a message and wait for the answer.
	 *  @param me The message event.
	 *  @return The result event.
	 */
	public IFuture sendMessageAndWait(IEAMessageEvent me)
	{
		return sendMessageAndWait(me, -1);
	}

	/**
	 *  Send a message and wait for the answer.
	 *  Adds a reply_with entry if not present, for tracking the conversation.
	 *  @param me The message event.
	 *  @param timeout The timeout.
	 *  @return The result event.
	 */
	public IFuture sendMessageAndWait(final IEAMessageEvent me, final long timeout)
	{
		final Future ret = new Future();
		
		if(!getInterpreter().isPlanThread())
		{
			getInterpreter().getAgentAdapter().invokeLater(new Runnable() 
			{
				public void run() 
				{
					Object	wa	= getState().createObject(OAVBDIRuntimeModel.waitabstraction_type);
					SFlyweightFunctionality.addReply(wa, (ElementFlyweight)me, getState(), getScope());
					getState().addExternalObjectUsage(wa, ExternalAccessFlyweight.this);
					sendMessage(me);
					waitForExternalAccessWaitAbstraction(wa, timeout, ret);
				}
			});
		}
		else
		{
			Object	wa	= getState().createObject(OAVBDIRuntimeModel.waitabstraction_type);
			SFlyweightFunctionality.addReply(wa, (ElementFlyweight)me, getState(), getScope());
			Object[] wret = PlanRules.initializeWait(wa, timeout, getState(), getScope(), getInterpreter().getCurrentPlan());
			sendMessage(me);
			PlanRules.doWait(getState(), getInterpreter().getCurrentPlan());
			ret.setResult(PlanRules.afterWait(wa, (boolean[])wret[1], getState(), getScope(), getInterpreter().getCurrentPlan()));
		}
		
		return ret;
	}

	/**
	 *  Wait for a message event.
	 *  @param type The message event type.
	 */
	public IFuture waitForMessageEvent(String type)
	{
		return waitForMessageEvent(type, -1);
	}

	/**
	 *  Wait for a message event.
	 *  @param type The message event type.
	 *  @param timeout The timeout.
	 */
	public IFuture waitForMessageEvent(final String type, final long timeout)
	{
		final Future ret = new Future();
		
		if(!getInterpreter().isPlanThread())
		{
			getInterpreter().getAgentAdapter().invokeLater(new Runnable() 
			{
				public void run() 
				{
					Object	wa	= getState().createObject(OAVBDIRuntimeModel.waitabstraction_type);
					SFlyweightFunctionality.addMessageEvent(wa, type, getState(), getScope());
					getState().addExternalObjectUsage(wa, ExternalAccessFlyweight.this);
					waitForExternalAccessWaitAbstraction(wa, timeout, ret);
				}
			});
		}
		else
		{
			Object	wa	= getState().createObject(OAVBDIRuntimeModel.waitabstraction_type);
			SFlyweightFunctionality.addMessageEvent(wa, type, getState(), getScope());
			ret.setResult(PlanRules.waitForWaitAbstraction(wa, timeout, getState(), getScope(), getInterpreter().getCurrentPlan()));
		}
		
		return ret;
	}

	/**
	 *  Wait for a message.
	 *  @param msgevent The message event.
	 */
	public IFuture waitForReply(IEAMessageEvent msgevent)
	{
		return waitForReply(msgevent);
	}

	/**
	 *  Wait for a message.
	 *  @param msgevent The message event.
	 */
	public IFuture waitForReply(final IEAMessageEvent msgevent, final long timeout)
	{
		final Future ret = new Future();
		
		if(!getInterpreter().isPlanThread())
		{
			getInterpreter().getAgentAdapter().invokeLater(new Runnable() 
			{
				public void run() 
				{
					Object	wa	= getState().createObject(OAVBDIRuntimeModel.waitabstraction_type);
					SFlyweightFunctionality.addReply(wa, (ElementFlyweight)msgevent, getState(), getScope());
					getState().addExternalObjectUsage(wa, ExternalAccessFlyweight.this);
					waitForExternalAccessWaitAbstraction(wa, timeout, ret);
				}
			});
		}
		else
		{
			Object	wa	= getState().createObject(OAVBDIRuntimeModel.waitabstraction_type);
			SFlyweightFunctionality.addReply(wa, (ElementFlyweight)msgevent, getState(), getScope());
			ret.setResult(PlanRules.waitForWaitAbstraction(wa, timeout, getState(), getScope(), getInterpreter().getCurrentPlan()));
		}
		
		return ret;
	}

	/**
	 *  Wait for a goal.
	 *  @param type The goal type.
	 */
	public IFuture waitForGoal(String type)
	{
		return waitForGoal(type, -1);
	}

	/**
	 *  Wait for a goal.
	 *  @param type The goal type.
	 *  @param timeout The timeout.
	 */
	public IFuture waitForGoal(final String type, final long timeout)
	{
		final Future ret = new Future();
		
		if(!getInterpreter().isPlanThread())
		{
			getInterpreter().getAgentAdapter().invokeLater(new Runnable() 
			{
				public void run() 
				{
					Object	wa	= getState().createObject(OAVBDIRuntimeModel.waitabstraction_type);
					SFlyweightFunctionality.addGoal(wa, type, getState(), getScope());
					getState().addExternalObjectUsage(wa, ExternalAccessFlyweight.this);
					waitForExternalAccessWaitAbstraction(wa, timeout, ret);
				}
			});
		}
		else
		{
			Object	wa	= getState().createObject(OAVBDIRuntimeModel.waitabstraction_type);
			SFlyweightFunctionality.addGoal(wa, type, getState(), getScope());
			ret.setResult(PlanRules.waitForWaitAbstraction(wa, timeout, getState(), getScope(), getInterpreter().getCurrentPlan()));
		}
		
		return ret;
	}

	/**
	 *  Wait for a belief (set) fact change.
	 *  @param belief The belief (set) type.
	 *  @return The changed fact value.
	 */
	public IFuture waitForFactChanged(String belief)
	{
		return waitForFactChanged(belief, -1);
	}

	/**
	 *  Wait for a belief (set) fact change.
	 *  @param belief The belief (set) type.
	 *  @param timeout The timeout.
	 *  @return The changed fact.
	 */
	public IFuture waitForFactChanged(final String belief, final long timeout)
	{
		final Future ret = new Future();
		
		if(!getInterpreter().isPlanThread())
		{
			getInterpreter().getAgentAdapter().invokeLater(new Runnable() 
			{
				public void run() 
				{
					Object	wa	= getState().createObject(OAVBDIRuntimeModel.waitabstraction_type);
					SFlyweightFunctionality.addFactChanged(wa, belief, getState(), getScope());
					getState().addExternalObjectUsage(wa, ExternalAccessFlyweight.this);
					waitForExternalAccessWaitAbstraction(wa, timeout, ret);
				}
			});
		}
		else
		{
			Object	wa	= getState().createObject(OAVBDIRuntimeModel.waitabstraction_type);
			SFlyweightFunctionality.addFactChanged(wa, belief, getState(), getScope());
			ret.setResult(PlanRules.waitForWaitAbstraction(wa, timeout, getState(), getScope(), getInterpreter().getCurrentPlan()));
		}
		
		return ret;
	}

	/**
	 *  Wait for a belief set change.
	 *  @param type The belief set type.
	 *  @return The fact that was added.
	 */
	public IFuture waitForFactAdded(String type)
	{
		return waitForFactAdded(type, -1);
	}

	/**
	 *  Wait for a belief set change.
	 *  @param type The belief set type.
	 *  @param timeout The timeout.
	 *  @return The fact that was added.
	 */
	public IFuture waitForFactAdded(final String type, final long timeout)
	{
		final Future ret = new Future();
		
		if(!getInterpreter().isPlanThread())
		{
			getInterpreter().getAgentAdapter().invokeLater(new Runnable() 
			{
				public void run() 
				{
					Object	wa	= getState().createObject(OAVBDIRuntimeModel.waitabstraction_type);
					SFlyweightFunctionality.addFactAdded(wa, type, getState(), getScope());
					getState().addExternalObjectUsage(wa, ExternalAccessFlyweight.this);
					waitForExternalAccessWaitAbstraction(wa, timeout, ret);
				}
			});
		}
		else
		{
			Object	wa	= getState().createObject(OAVBDIRuntimeModel.waitabstraction_type);
			SFlyweightFunctionality.addFactAdded(wa, type, getState(), getScope());
			ret.setResult(PlanRules.waitForWaitAbstraction(wa, timeout, getState(), getScope(), getInterpreter().getCurrentPlan()));
		}
		
		return ret;
	}

	/**
	 *  Wait for a belief set change.
	 *  @param type The belief set type.
	 *  @return The fact that was added.
	 */
	public IFuture waitForFactRemoved(String type)
	{
		return waitForFactRemoved(type, -1);
	}

	/**
	 *  Wait for a belief set change.
	 *  @param type The belief set type.
	 *  @param timeout The timeout.
	 *  @return The fact that was added.
	 */
	public IFuture waitForFactRemoved(final String type, final long timeout)
	{
		final Future ret = new Future();
		
		if(!getInterpreter().isPlanThread())
		{
			getInterpreter().getAgentAdapter().invokeLater(new Runnable() 
			{
				public void run() 
				{
					Object	wa	= getState().createObject(OAVBDIRuntimeModel.waitabstraction_type);
					SFlyweightFunctionality.addFactRemoved(wa, type, getState(), getScope());
					getState().addExternalObjectUsage(wa, ExternalAccessFlyweight.this);
					waitForExternalAccessWaitAbstraction(wa, timeout, ret);
				}
			});
		}
		else
		{
			Object	wa	= getState().createObject(OAVBDIRuntimeModel.waitabstraction_type);
			SFlyweightFunctionality.addFactRemoved(wa, type, getState(), getScope());
			ret.setResult(PlanRules.waitForWaitAbstraction(wa, timeout, getState(), getScope(), getInterpreter().getCurrentPlan()));
		}
		
		return ret;
	}

	/**
	 *  Dispatch a top level goal and wait for the result.
	 *  @param goal The goal.
	 */
	public IFuture dispatchTopLevelGoalAndWait(IEAGoal goal)
	{
		return dispatchTopLevelGoalAndWait(goal, -1);
	}

	/**
	 *  Dispatch a top level goal and wait for the result.
	 *  @param goal The goal.
	 */
	public IFuture dispatchTopLevelGoalAndWait(final IEAGoal goal, final long timeout)
	{
		final Future ret = new Future();
		
		if(!getInterpreter().isPlanThread())
		{
			getInterpreter().getAgentAdapter().invokeLater(new Runnable() 
			{
				public void run() 
				{
					Object	wa	= getState().createObject(OAVBDIRuntimeModel.waitabstraction_type);
					SFlyweightFunctionality.addGoal(wa, (ElementFlyweight)goal, getState(), getScope());
					getState().addExternalObjectUsage(wa, ExternalAccessFlyweight.this);
					dispatchTopLevelGoal(goal);
					waitForExternalAccessWaitAbstraction(wa, timeout, ret);
				}
			});
		}
		else
		{
			Object	wa	= getState().createObject(OAVBDIRuntimeModel.waitabstraction_type);
			SFlyweightFunctionality.addGoal(wa, (ElementFlyweight)goal, getState(), getScope());
			dispatchTopLevelGoal(goal);
			ret.setResult(PlanRules.waitForWaitAbstraction(wa, timeout, getState(), getScope(), getInterpreter().getCurrentPlan()));
		}
		
		return ret;
	}
	
	/**
	 *  Wait for the agent to terminate.
	 * /
	public void waitForAgentTerminating()
	{
		throw new UnsupportedOperationException();
	}*/

	/**
	 *  Wait for the agent to terminate.
	 * /
	public void waitForAgentTerminating(long timeout)
	{
		throw new UnsupportedOperationException();
	}*/
	
	//-------- handling (synchronized with agent) external threads --------
	
	/**
	 *  Wait for a wait abstraction.
	 *  @param waitabstraction.
	 *  @return The dispatched element.
	 */
	protected void waitForExternalAccessWaitAbstraction(Object wa, long timeout, Future future)
	{
		initializeWait(wa, timeout, future);
			
//			doWait();
//			try
//			{
//				ret = afterWait(wa, tmp[0], (WakeupAction)tmp[1]);
//			}
//			finally
//			{
//				if(tmp[2]!=null)
//				{
//					Object[]	observeds	= (Object[])tmp[2];
//					for(int i=0; i<observeds.length; i++)
//						getInterpreter().getEventReificator().removeObservedElement(observeds[i]);
//				}
//			}
//		}
	}
	
	/**
	 *  Initialize the wait abstraction within the agent.
	 *  Must be done before actions (such as dispatchTopLevelGoal) are taken because
	 *  the agent runs asynchronously and could be finished before wait is installed. 
	 */
	protected void initializeWait(final Object wa, final long timeout, final Future future)
	{
		Object ea = getState().createObject(OAVBDIRuntimeModel.externalaccess_type);
		getState().addAttributeValue(getScope(), OAVBDIRuntimeModel.capability_has_externalaccesses, ea);
		
		// Prohibid wakeup execution in a) agent and b) timer until this thread sleeps. 
	
	
		getState().setAttributeValue(ea, OAVBDIRuntimeModel.externalaccess_has_waitabstraction, wa);
	
		List observedobjects = null;
		if(wa!=null)
		{
			Collection	coll	= getState().getAttributeValues(wa, OAVBDIRuntimeModel.waitabstraction_has_factaddeds);
			if(coll!=null)
			{
				if(observedobjects==null)
					observedobjects	= new ArrayList();
				observedobjects.addAll(coll);
			}
			coll	= getState().getAttributeValues(wa, OAVBDIRuntimeModel.waitabstraction_has_factremoveds);
			if(coll!=null)
			{
				if(observedobjects==null)
					observedobjects	= new ArrayList();
				observedobjects.addAll(coll);
			}
			coll	= getState().getAttributeValues(wa, OAVBDIRuntimeModel.waitabstraction_has_factchangeds);
			if(coll!=null)
			{
				if(observedobjects==null)
					observedobjects	= new ArrayList();
				observedobjects.addAll(coll);
			}
		}
		
		WakeupAction wakeup = new WakeupAction(getState(), getScope(), wa, ea, ExternalAccessFlyweight.this, observedobjects, future);
		getState().setAttributeValue(ea, OAVBDIRuntimeModel.externalaccess_has_wakeupaction, wakeup);

		if(timeout>-1)
		{
//					final long start = System.currentTimeMillis(); 
			
//					System.out.println("Timer created: "+start);
			getState().setAttributeValue(ea, OAVBDIRuntimeModel.externalaccess_has_timer,
				getInterpreter().getClockService().createTimer(timeout, new InterpreterTimedObject(getInterpreter(), wakeup)));
		}
		else if(timeout==PlanRules.TICK_TIMER)
		{
			getState().setAttributeValue(ea, OAVBDIRuntimeModel.externalaccess_has_timer,
				getInterpreter().getClockService().createTickTimer(new InterpreterTimedObject(getInterpreter(), wakeup)));
		}				
	}
	
	/**
	 *  Do the waiting, i.e. set the external caller thread to sleep mode.
	 */
//	protected void doWait()
//	{
//		try
//		{
////			System.out.println("Waiting: "+Thread.currentThread());
//			Thread.currentThread().wait();
//		}
//		catch(InterruptedException ex)
//		{
//		}
////		System.out.println("Resumed: "+Thread.currentThread());
//	}
	
	/**
	 *  Perform the cleanup operations after an external wait.
	 *  Mainly removes the external access object from capability and generates the result.
	 * /
	protected Object afterWait(final Object wa, final Object ea, final WakeupAction wakeup)//, final boolean[] to)
	{
		AgentInvocation invoc = new AgentInvocation()
		{
			public void run()
			{
				if(wakeup.isTimeout())
//				if(to[0])
				{
					if(wa!=null)
					{
						getState().removeAttributeValue(getScope(), OAVBDIRuntimeModel.capability_has_externalaccesses, ea);
						throw new TimeoutException();
					}
				}
		
				Object de = getState().getAttributeValue(ea, OAVBDIRuntimeModel.externalaccess_has_dispatchedelement);
				if(de!=null)
				{
					OAVObjectType type = getState().getType(de);
					if(OAVBDIRuntimeModel.goal_type.equals(type))
					{
						// When goal is not succeeded (or idle for maintaingoals) throw exception.
						if(!OAVBDIRuntimeModel.GOALPROCESSINGSTATE_SUCCEEDED.equals(
							getState().getAttributeValue(de, OAVBDIRuntimeModel.goal_has_processingstate)))
						{
							Object	mgoal	= getState().getAttributeValue(de, OAVBDIRuntimeModel.element_has_model);
							if(!getState().getType(mgoal).isSubtype(OAVBDIMetaModel.maintaingoal_type)
								|| !OAVBDIRuntimeModel.GOALPROCESSINGSTATE_IDLE.equals(
									getState().getAttributeValue(de, OAVBDIRuntimeModel.goal_has_processingstate)))
							{
								getState().removeAttributeValue(getScope(), OAVBDIRuntimeModel.capability_has_externalaccesses, ea);
								throw new GoalFailureException("Goal failed: "+de);
							}
						}
						object = EGoalFlyweight.getGoalFlyweight(getState(), getScope(), de);
					}
					else if(OAVBDIRuntimeModel.internalevent_type.equals(type))
					{
						object = EInternalEventFlyweight.getInternalEventFlyweight(getState(), getScope(), de);
					}
					else if(OAVBDIRuntimeModel.messageevent_type.equals(type))
					{
						object = EMessageEventFlyweight.getMessageEventFlyweight(getState(), getScope(), de);
					}
				}
				getState().removeAttributeValue(getScope(), OAVBDIRuntimeModel.capability_has_externalaccesses, ea);
				if(wa!=null)
					getState().removeExternalObjectUsage(wa, ExternalAccessFlyweight.this);
			}
		};
		
		return invoc.object;
	}*/
	
	
	/**
	 *  Create a new wait abstraction.
	 *  @return The wait abstraction.
	 * /
	public IWaitAbstraction createWaitAbstraction()
	{
		return new WaitAbstractionFlyweight(getState(), getScope(), null);
	}*/
	
	/**
	 *  Get the waitqueue.
	 *  @return The waitqueue.
	 * /
	public IWaitqueue getWaitqueue()
	{
		// Right scope?
		return new WaitqueueFlyweight(getState(), getScope(), getInterpreter().getCurrentPlan());
	}*/

	/**
	 *  Invoke some code on the agent thread.
	 *  This method queues the runnable in the agent
	 *  and immediately return (i.e. probably before
	 *  the runnable has been executed).
	 */
	public void invokeLater(Runnable runnable)
	{
		getInterpreter().getAgentAdapter().invokeLater(runnable);
	}

	/**
	 *  Get the model of the component.
	 */
	public IModelInfo	getModel()
	{
		return getInterpreter().getModel().getModelInfo();
	}
	
	/**
	 *  Get the children (if any).
	 *  @return The children.
	 */
	public IFuture getChildren()
	{
		return getInterpreter().getAgentAdapter().getChildren();
	}
	
	/**
	 *  Kill the component.
	 */
	public IFuture killComponent()
	{
		return killAgent();
	}
	
	/**
	 *  Create a result listener that will be 
	 *  executed on the component thread.
	 *  @param listener The result listener.
	 *  @return A result listener that is called on component thread.
	 */
	public IResultListener createResultListener(IResultListener listener)
	{
		return new ComponentResultListener(listener, adapter);
	}
	
	/**
	 *  Get external access of subcapability.
	 *  @param name The capability name.
	 *  @return The future with external access.
	 */
	public IFuture getExternalAccess(final String name)
	{
		final Future ret = new Future();
		
		if(!getInterpreter().isPlanThread())
		{
			getInterpreter().getAgentAdapter().invokeLater(new Runnable() 
			{
				public void run() 
				{
					StringTokenizer stok = new StringTokenizer(name, ".");
					Object handle = getHandle();
					while(stok.hasMoreTokens())
					{
						String subcapname = stok.nextToken();
						Object subcapref = getState().getAttributeValue(handle, OAVBDIRuntimeModel.capability_has_subcapabilities, subcapname);
						if(subcapref==null)
						{
							ret.setException(new RuntimeException("Capability not found: "+subcapname));
							return;
						}
						handle = getState().getAttributeValue(subcapref, OAVBDIRuntimeModel.capabilityreference_has_capability);
					}
					ret.setResult(new ExternalAccessFlyweight(getState(), handle));
				}
			});
		}
		else
		{
			StringTokenizer stok = new StringTokenizer(name, ".");
			Object handle = getHandle();
			while(stok.hasMoreTokens())
			{
				String subcapname = stok.nextToken();
				Object subcapref = getState().getAttributeValue(handle, OAVBDIRuntimeModel.capability_has_subcapabilities, subcapname);
				if(subcapref==null)
				{
					ret.setException(new RuntimeException("Capability not found: "+subcapname));
					return ret;
				}
				handle = getState().getAttributeValue(subcapref, OAVBDIRuntimeModel.capabilityreference_has_capability);
			}
			ret.setResult(new ExternalAccessFlyweight(getState(), handle));
		}
		
		return ret;
	}
	
	/**
	 *  Get subcapability names.
	 *  @return The future with array of subcapability names.
	 */
	public IFuture getSubcapabilityNames()
	{
		final Future ret = new Future();
		
		if(!getInterpreter().isPlanThread())
		{
			getInterpreter().getAgentAdapter().invokeLater(new Runnable() 
			{
				public void run() 
				{
					String[] res = SUtil.EMPTY_STRING_ARRAY;
					Collection coll = getState().getAttributeValues(getHandle(), OAVBDIRuntimeModel.capability_has_subcapabilities);
					if(coll!=null)
					{
						res = new String[coll.size()];
						int i=0;
						for(Iterator it=coll.iterator(); it.hasNext(); i++)
						{
							Object cref = it.next();
							String name = (String)getState().getAttributeValue(cref, OAVBDIRuntimeModel.capabilityreference_has_name);
							res[i] = name;
						}
					}
					ret.setResult(res);
				}
			});
		}
		else
		{
			String[] res = SUtil.EMPTY_STRING_ARRAY;
			Collection coll = getState().getAttributeValues(getHandle(), OAVBDIRuntimeModel.capability_has_subcapabilities);
			if(coll!=null)
			{
				res = new String[coll.size()];
				int i=0;
				for(Iterator it=coll.iterator(); it.hasNext(); i++)
				{
					Object cref = it.next();
					String name = (String)getState().getAttributeValue(cref, OAVBDIRuntimeModel.capabilityreference_has_name);
					res[i] = name;
				}
			}
			ret.setResult(res);
		}
		
		return ret;
	}
	
	/**
	 *  Schedule a step of the agent.
	 *  May safely be called from external threads.
	 *  @param step	Code to be executed as a step of the agent.
	 *  @return The result of the step.
	 */
	public IFuture scheduleResultStep(final IResultCommand com)
	{
		final Future ret = new Future();
		
		if(adapter.isExternalThread())
		{
			try
			{
				adapter.invokeLater(new Runnable() 
				{
					public void run() 
					{
						ret.setException(new UnsupportedOperationException());
//						application.scheduleStep(com).addResultListener(new DelegationResultListener(ret));
					}
				});
			}
			catch(Exception e)
			{
				ret.setException(e);
			}
		}
		else
		{
			ret.setException(new UnsupportedOperationException());
//			application.scheduleStep(com).addResultListener(new DelegationResultListener(ret));
		}
		
		return ret;
	}
}
