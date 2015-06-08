package jadex.bdiv3x.runtime;

import jadex.bdiv3.annotation.PlanAPI;
import jadex.bdiv3.annotation.PlanAborted;
import jadex.bdiv3.annotation.PlanBody;
import jadex.bdiv3.annotation.PlanCapability;
import jadex.bdiv3.annotation.PlanFailed;
import jadex.bdiv3.annotation.PlanPassed;
import jadex.bdiv3.features.IBDIAgentFeature;
import jadex.bdiv3.features.impl.IInternalBDIAgentFeature;
import jadex.bdiv3.model.MInternalEvent;
import jadex.bdiv3.model.MMessageEvent;
import jadex.bdiv3.runtime.IBeliefListener;
import jadex.bdiv3.runtime.IGoal;
import jadex.bdiv3.runtime.IPlan;
import jadex.bdiv3.runtime.WaitAbstraction;
import jadex.bdiv3.runtime.impl.BeliefAdapter;
import jadex.bdiv3.runtime.impl.PlanFailureException;
import jadex.bdiv3.runtime.impl.RCapability;
import jadex.bdiv3.runtime.impl.RGoal;
import jadex.bdiv3.runtime.impl.RPlan;
import jadex.bridge.IComponentIdentifier;
import jadex.bridge.IExternalAccess;
import jadex.bridge.IInternalAccess;
import jadex.bridge.component.IExecutionFeature;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.bridge.service.search.SServiceProvider;
import jadex.bridge.service.types.clock.IClockService;
import jadex.bridge.service.types.cms.IComponentDescription;
import jadex.commons.SReflect;
import jadex.commons.concurrent.TimeoutException;
import jadex.commons.future.DelegationResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.rules.eca.ChangeInfo;

import java.util.List;
import java.util.logging.Logger;

/**
 *  Dummy class for loading v2 examples using v3x.
 */
@jadex.bdiv3.annotation.Plan
public abstract class Plan
{
	/** The internal access. */
	@PlanCapability
	protected IInternalAccess agent;
	
	/** The rplan. */
	@PlanAPI
	protected RPlan rplan;
	
	/**
	 *  The body method is called on the
	 *  instantiated plan instance from the scheduler.
	 */
	@PlanBody
	public abstract void body();

	/**
	 *  The passed method is called on plan success.
	 */
	@PlanPassed
	public void	passed()
	{
	}

	/**
	 *  The failed method is called on plan failure/abort.
	 */
	@PlanFailed
	public void	failed()
	{
	}

	/**
	 *  The plan was aborted (because of conditional goal
	 *  success or termination from outside).
	 */
	@PlanAborted
	public void aborted()
	{
	}
	
	/**
	 *  Wait for a some time.
	 *  @param duration The duration.
	 */
	public void	waitFor(long timeout)
	{
		agent.getComponentFeature(IExecutionFeature.class).waitForDelay(timeout).get();
	}
	
	/**
	 *  Wait for next tick.
	 */
	public void	waitForTick()
	{
		agent.getComponentFeature(IExecutionFeature.class).waitForTick().get();
	}
	
	/**
	 *  Create a goal from a template goal.
	 *  To be processed, the goal has to be dispatched as subgoal
	 *  or adopted as top-level goal.
	 *  @param type	The template goal name as specified in the ADF.
	 *  @return The created goal.
	 */
	public IGoal createGoal(String type)
	{
		return getGoalbase().createGoal(type);
	}
	
	/**
	 *  Dispatch a new top-level goal.
	 *  @param goal The new goal.
	 */
	public void	dispatchSubgoalAndWait(IGoal goal)
	{
		dispatchSubgoal(goal);
		RGoal rgoal = (RGoal)goal;
		Future<Void> ret = new Future<Void>();
		rgoal.addListener(new DelegationResultListener<Void>(ret));
		ret.get();
	}
	
	/**
	 *  Wait for a message event.
	 *  @param type The message event type.
	 */
	public IMessageEvent waitForMessageEvent(String type)
	{
		return waitForMessageEvent(type, -1);
	}

	/**
	 *  Wait for a message event.
	 *  @param type The message event type.
	 *  @param timeout The timeout.
	 */
	public IMessageEvent waitForMessageEvent(String type, long timeout)
	{
		final Future<IMessageEvent> ret = new Future<IMessageEvent>();

		IInternalBDIAgentFeature bdif = (IInternalBDIAgentFeature)agent.getComponentFeature(IBDIAgentFeature.class);
		MMessageEvent mevent = bdif.getBDIModel().getCapability().getMessageEvent(type);
		WaitAbstraction wa = new WaitAbstraction();
		wa.addMessageEvent(mevent);

		IMessageEvent res = (IMessageEvent)rplan.getFromWaitqueue(wa);
		if(res!=null)
		{
			return res;
		}
		else
		{
			// todo: add scope name if is in capa
			rplan.setWaitAbstraction(wa);
			
			// todo: timeout
			
	//		final ResumeCommand<IMessageEvent> rescom = getRPlan().new ResumeCommand<IMessageEvent>(ret, false);
	//
	//		if(timeout>-1)
	//		{
	//			IFuture<ITimer> cont = getRPlan().createTimer(timeout, agent, rescom);
	//			cont.addResultListener(new DefaultResultListener<ITimer>()
	//			{
	//				public void resultAvailable(final ITimer timer)
	//				{
	//					if(timer!=null)
	//						rescom.setTimer(timer);
	//				}
	//			});
	//		}
			
	//		rplan.addResumeCommand(rescom);
			
			return ret.get(timeout);
		}
	}
	
	/**
	 *  Wait for an internal event.
	 *  @param type The internal event type.
	 */
	public IInternalEvent waitForInternalEvent(String type)
	{
		return waitForInternalEvent(type, -1);
	}

	/**
	 *  Wait for an internal event.
	 *  @param type The internal event type.
	 *  @param timeout The timeout.
	 */
	public IInternalEvent waitForInternalEvent(String type, long timeout)
	{
		final Future<IInternalEvent> ret = new Future<IInternalEvent>();

		IInternalBDIAgentFeature bdif = (IInternalBDIAgentFeature)agent.getComponentFeature(IBDIAgentFeature.class);
		MInternalEvent mevent = bdif.getBDIModel().getCapability().getInternalEvent(type);
		WaitAbstraction wa = new WaitAbstraction();
		wa.addInternalEvent(mevent);

		IInternalEvent res = (IInternalEvent)rplan.getFromWaitqueue(wa);
		if(res!=null)
		{
			return res;
		}
		else
		{
			// todo: add scope name if is in capa
			rplan.setWaitAbstraction(wa);
			
			// todo: timeout
			
	//		final ResumeCommand<IMessageEvent> rescom = getRPlan().new ResumeCommand<IMessageEvent>(ret, false);
	//
	//		if(timeout>-1)
	//		{
	//			IFuture<ITimer> cont = getRPlan().createTimer(timeout, agent, rescom);
	//			cont.addResultListener(new DefaultResultListener<ITimer>()
	//			{
	//				public void resultAvailable(final ITimer timer)
	//				{
	//					if(timer!=null)
	//						rescom.setTimer(timer);
	//				}
	//			});
	//		}
			
	//		rplan.addResumeCommand(rescom);
			
			return ret.get(timeout);
		}
	}
	
	/**
	 *  Kill this agent.
	 */
	public void	killAgent()
	{
		agent.killComponent();
	}
	
	/**
	 *  Get the logger.
	 *  @return The logger.
	 */
	public Logger getLogger()
	{
		return agent.getLogger();
	}
	
	/**
	 *  Get the beliefbase.
	 *  @return The beliefbase.
	 */
	public IBeliefbase getBeliefbase()
	{
		return getCapability().getBeliefbase();
	}

	/**
	 *  Get the rplan.
	 *  @return The rplan
	 */
	public IPlan getRPlan()
	{
		return rplan;
	}
	
	/**
	 *  Get the reason this plan was created for.
	 *  @return The reason.
	 */
	public Object getReason()
	{
		return rplan.getReason();
	}
	
	/**
	 *  Get the reason this plan was created for.
	 *  @return The reason.
	 */
	public Object getDispatchedElement()
	{
		return rplan.getDispatchedElement();
	}
	
	/**
	 *  Get a parameter.
	 *  @param name The name.
	 *  @return The parameter.
	 */
	public IParameter getParameter(String name)
	{
		return rplan.getParameter(name);
	}
	
	/**
	 *  Get a parameter.
	 *  @param name The name.
	 *  @return The parameter.
	 */
	public IParameterSet getParameterSet(String name)
	{
		return rplan.getParameterSet(name);
	}
	
	/**
	 *  Get an expression by name.
	 *  @name The expression name.
	 *  @return The expression.
	 */
	public IExpression getExpression(String name)
	{
		return getExpressionbase().getExpression(name);
	}
	
	/**
	 *  Get an expression by name.
	 *  @name The expression name.
	 *  @return The expression.
	 */
	public IExpression createExpression(String exp)
	{
		return getExpressionbase().createExpression(exp);
	}
	
	/**
	 *  Send a message after some delay.
	 *  @param me	The message event.
	 *  @return The filter to wait for an answer.
	 */
	public IFuture<Void> sendMessage(IMessageEvent me)
	{
		return getEventbase().sendMessage(me);
	}
	
	/**
	 *  Let the plan fail.
	 */
	public void fail()
	{
		throw new PlanFailureException();
	}
	
	/**
	 *  Get the capability.
	 *  @return The capability.
	 */
	protected RCapability getCapability()
	{
		return ((IInternalBDIAgentFeature)agent.getComponentFeature(IBDIAgentFeature.class)).getCapability();
	}
	
	//-------- legacy --------
	

	/**
	 *  Let a plan fail.
	 *  @param cause The cause.
	 */
	public void fail(Throwable cause)
	{
		throw new PlanFailureException(null, cause);
	}

	/**
	 *  Let a plan fail.
	 *  @param message The message.
	 *  @param cause The cause.
	 */
	public void fail(String message, Throwable cause)
	{
		throw new PlanFailureException(message, cause);
	}

	/**
	 *  Get the scope.
	 *  @return The scope.
	 */
	public ICapability getScope()
	{
		return new jadex.bdiv3x.runtime.RCapability(agent);
	}
	
//	/**
//	 *  Start an atomic transaction.
//	 *  All possible side-effects (i.e. triggered conditions)
//	 *  of internal changes (e.g. belief changes)
//	 *  will be delayed and evaluated after endAtomic() has been called.
//	 *  @see #endAtomic()
//	 */
//	public void	startAtomic()
//	{
//		throw new UnsupportedOperationException();
//	}
//
//	/**
//	 *  End an atomic transaction.
//	 *  Side-effects (i.e. triggered conditions)
//	 *  of all internal changes (e.g. belief changes)
//	 *  performed after the last call to startAtomic()
//	 *  will now be evaluated and performed.
//	 *  @see #startAtomic()
//	 */
//	public void	endAtomic()
//	{
//		throw new UnsupportedOperationException();
//	}

	/**
	 *  Dispatch a new subgoal.
	 *  @param subgoal The new subgoal.
	 *  Note: plan step is interrupted after call.
	 */
	public void dispatchSubgoal(IGoal subgoal)
	{
		RGoal rgoal = (RGoal)subgoal;
		rgoal.setParent(rplan);
		rplan.addSubgoal(rgoal);
		RGoal.adoptGoal(rgoal, agent);
	}

	/**
	 *  Get the string representation.
	 *  @return The string representation.
	 */
	public String toString()
	{
		StringBuffer buf = new StringBuffer();
		buf.append(SReflect.getInnerClassName(this.getClass()));
		buf.append("(");
		buf.append(rplan);
		buf.append(")");
		return buf.toString();
	}

	/**
	 *  Get the agent name.
	 *  @return The agent name.
	 */
	public String getComponentName()
	{
		return getComponentIdentifier().getLocalName();
	}
	
	/**
	 * Get the agent identifier.
	 * @return The agent identifier.
	 */
	public IComponentIdentifier	getComponentIdentifier()
	{
		return agent.getComponentIdentifier();
	}
	
	/**
	 * Get the agent description.
	 * @return The agent description.
	 */
	public IComponentDescription getComponentDescription()
	{
		return agent.getComponentDescription();
	}

	/**
	 *  Get the uncatched exception that occurred in the body (if any).
	 *  Method should only be called when in failed() method.
	 *  @return The exception.
	 */
	public Exception getException()
	{
		return getRPlan().getException();
	} 

	/**
	 *  Get the goal base.
	 *  @return The goal base.
	 */
	public IGoalbase getGoalbase()
	{
		return getCapability().getGoalbase();
	}

//	/**
//	 *  Get the plan base.
//	 *  @return The plan base.
//	 */
//	public IPlanbase getPlanbase()
//	{
//		throw new UnsupportedOperationException();
//	}

	/**
	 *  Get the event base.
	 *  @return The event base.
	 */
	public IEventbase getEventbase()
	{
		return getCapability().getEventbase();
	}

	/**
	 * Get the expression base.
	 * @return The expression base.
	 */
	public IExpressionbase getExpressionbase()
	{
		return getCapability().getExpressionbase();
	}
	
	/**
	 *  Get the clock.
	 *  @return The clock.
	 */
	public IClockService getClock()
	{
		return SServiceProvider.getLocalService(agent, IClockService.class, RequiredServiceInfo.SCOPE_PLATFORM);
	}

	/**
	 *  Get the current time.
	 *  The time unit depends on the currently running clock implementation.
	 *  For the default system clock, the time value adheres to the time
	 *  representation as used by {@link System#currentTimeMillis()}, i.e.,
	 *  the value of milliseconds passed since 0:00 'o clock, January 1st, 1970, UTC.
	 *  For custom simulation clocks, arbitrary representations can be used.
	 *  @return The current time.
	 */
	public long getTime()
	{
		return getClock().getTime();
	}
	
	/**
	 *  Dispatch a new top-level goal.
	 *  @param goal The new goal.
	 *  Note: plan step is interrupted after call.
	 */
	public void dispatchTopLevelGoal(IGoal goal)
	{
		getGoalbase().dispatchTopLevelGoal(goal);
	}

//	/**
//	 *  Send a message.
//	 *  @param me	The message event.
//	 */
//	public IFuture<Void> sendMessage(IMessageEvent me, byte[] codecids)
//	{	
//		return getEventbase().sendMessage(me, codecids);
//	}

	/**
	 *  Dispatch an internal event.
	 *  @param event The event.
	 *  Note: plan step is interrupted after call.
	 */
	public void dispatchInternalEvent(IInternalEvent event)
	{
		getEventbase().dispatchInternalEvent(event);
	}

	/**
	 *  Create a new message event.
	 *  @return The new message event.
	 */
	public IMessageEvent createMessageEvent(String type)
	{
		return getEventbase().createMessageEvent(type);
	}

	/**
	 *  Create a new intenal event.
	 *  @return The new intenal event.
	 */
	public IInternalEvent createInternalEvent(String type)
	{
		return getEventbase().createInternalEvent(type);
	}

	/**
	 *  Get the scope.
	 *  @return The scope.
	 */
	public IExternalAccess getExternalAccess()
	{
		return agent.getExternalAccess();
	}

//	/**
//	 *  Create a precompiled expression.
//	 *  @param expression	The expression string.
//	 *  @return The precompiled expression.
//	 */
//	public IExpression	createExpression(String expression, String[] paramnames, Class<?>[] paramtypes)
//	{
//		throw new UnsupportedOperationException();
//	}

	/**
	 *  Get all parameters.
	 *  @return All parameters.
	 */
	public IParameter[]	getParameters()
	{
		return rplan.getParameters();
	}

	/**
	 *  Get all parameter sets.
	 *  @return All parameter sets.
	 */
	public IParameterSet[]	getParameterSets()
	{
		return rplan.getParameterSets();
	}

	/**
	 *  Has the element a parameter element.
	 *  @param name The name.
	 *  @return True, if it has the parameter.
	 */
	public boolean hasParameter(String name)
	{
		return rplan.hasParameter(name);
	}

	/**
	 *  Has the element a parameter set element.
	 *  @param name The name.
	 *  @return True, if it has the parameter set.
	 */
	public boolean hasParameterSet(String name)
	{
		return rplan.hasParameterSet(name);
	}

	/**
	 *  Get the agent.
	 *  @return The agent
	 */
	public IInternalAccess getAgent()
	{
		return agent;
	}
	
	/**
	 *  Get the waitqueue.
	 */
	public List<Object> getWaitqueue()
	{
		return rplan.getWaitqueue();
	}
	
	/**
	 *  Wait for a fact change of a belief.
	 */
	public void waitForFactChanged(String belname)
	{
		waitForFactChanged(belname, -1);
	}
	
	/**
	 *  Wait for a fact change of a belief.
	 */
	public void waitForFactChanged(String belname, long timeout)
	{
		final Future<Void> ret = new Future<Void>();
		IBDIAgentFeature bdif = agent.getComponentFeature(IBDIAgentFeature.class);
		IBeliefListener<Object> lis = new BeliefAdapter<Object>()
		{
			public void beliefChanged(ChangeInfo<Object> info)
			{
				ret.setResultIfUndone(null);
			}
		};
		bdif.addBeliefListener(belname, lis);
		try
		{
			ret.get(timeout);
		}
		finally
		{
			bdif.removeBeliefListener(belname, lis);
		}
	}
	
	/**
	 *  Wait for a fact added.
	 */
	public Object waitForFactAdded(String belname)
	{
		return waitForFactAdded(belname, -1);
	}
	
	/**
	 *  Wait for a fact added.
	 */
	public Object waitForFactAdded(String belname, long timeout)
	{
		final Future<Object> ret = new Future<Object>();
		IBDIAgentFeature bdif = agent.getComponentFeature(IBDIAgentFeature.class);
		IBeliefListener<Object> lis = new BeliefAdapter<Object>()
		{
			public void factAdded(ChangeInfo<Object> info)
			{
				ret.setResultIfUndone(info.getValue());
			}
		};
		bdif.addBeliefListener(belname, lis);
		try
		{
			return ret.get(timeout);
		}
		finally
		{
			bdif.removeBeliefListener(belname, lis);
		}
	}
	
	/**
	 *  Wait for a fact added.
	 */
	public Object waitForFactRemoved(String belname)
	{
		return waitForFactRemoved(belname, -1);
	}
	
	/**
	 *  Wait for a fact added.
	 */
	public Object waitForFactRemoved(String belname, long timeout)
	{
		final Future<Void> ret = new Future<Void>();
		IBDIAgentFeature bdif = agent.getComponentFeature(IBDIAgentFeature.class);
		IBeliefListener<Object> lis = new BeliefAdapter<Object>()
		{
			public void factRemoved(ChangeInfo<Object> info)
			{
				ret.setResultIfUndone(null);
			}
		};
		bdif.addBeliefListener(belname, lis);
		try
		{
			return ret.get(timeout);
		}
		finally
		{
			bdif.removeBeliefListener(belname, lis);
		}
	}
	 
	/**
	 *  Get the plan interface.
	 */
	public IPlan getPlanElement()
	{
		return rplan;
	}
}
