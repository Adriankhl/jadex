package jadex.bdiv3.runtime.impl;

import jadex.bdiv3.actions.AdoptGoalAction;
import jadex.bdiv3.actions.DropGoalAction;
import jadex.bdiv3.actions.SelectCandidatesAction;
import jadex.bdiv3.features.IBDIAgentFeature;
import jadex.bdiv3.features.impl.BDIAgentFeature;
import jadex.bdiv3.features.impl.IInternalBDIAgentFeature;
import jadex.bdiv3.model.BDIModel;
import jadex.bdiv3.model.MCapability;
import jadex.bdiv3.model.MDeliberation;
import jadex.bdiv3.model.MGoal;
import jadex.bdiv3.model.MParameter;
import jadex.bdiv3.model.MParameter.Direction;
import jadex.bdiv3.model.MPlan;
import jadex.bdiv3.model.MPlanParameter;
import jadex.bdiv3.runtime.ChangeEvent;
import jadex.bdiv3.runtime.IGoal;
import jadex.bdiv3x.runtime.IParameter;
import jadex.bdiv3x.runtime.IParameterSet;
import jadex.bridge.IComponentStep;
import jadex.bridge.IInternalAccess;
import jadex.bridge.component.IExecutionFeature;
import jadex.bridge.component.IMonitoringComponentFeature;
import jadex.bridge.modelinfo.UnparsedExpression;
import jadex.bridge.service.types.monitoring.IMonitoringEvent;
import jadex.bridge.service.types.monitoring.IMonitoringService.PublishEventLevel;
import jadex.bridge.service.types.monitoring.IMonitoringService.PublishTarget;
import jadex.bridge.service.types.monitoring.MonitoringEvent;
import jadex.commons.MethodInfo;
import jadex.commons.future.IFuture;
import jadex.commons.future.IResultListener;
import jadex.javaparser.IParsedExpression;
import jadex.javaparser.SJavaParser;
import jadex.javaparser.SimpleValueFetcher;
import jadex.rules.eca.Event;
import jadex.rules.eca.EventType;
import jadex.rules.eca.IEvent;
import jadex.rules.eca.IRule;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *  Goal instance implementation.
 */
public class RGoal extends RFinishableElement implements IGoal, IInternalPlan
{
	//-------- attributes --------
	
	/** The lifecycle state. */
	protected GoalLifecycleState lifecyclestate;

	/** The processing state. */
	protected GoalProcessingState processingstate;

	/** The parent plan. */
	protected RPlan parentplan;
	protected RGoal parentgoal;
	
	/** The child plan. */
	protected RPlan childplan;
	
	/** The set of inhibitors. */
	protected Set<RGoal> inhibitors;

	//-------- constructors --------
	
	/**
	 *  Create a new rgoal. 
	 */
	public RGoal(IInternalAccess agent, MGoal mgoal, Object goal, RPlan parentplan, Map<String, Object> vals)
	{
		super(mgoal, goal, agent, vals);
		this.parentplan = parentplan;
		this.lifecyclestate = GoalLifecycleState.NEW;
		this.processingstate = GoalProcessingState.IDLE;
//		System.out.println("created: "+mgoal.getName()+" parent: "+parentplan);
	}
	
	/**
	 *  Create a new rgoal. 
	 */
	public RGoal(IInternalAccess agent, MGoal mgoal, Object goal, RGoal parentgoal, Map<String, Object> vals)
	{
		super(mgoal, goal, agent, vals);
		this.parentgoal = parentgoal;
		this.lifecyclestate = GoalLifecycleState.NEW;
		this.processingstate = GoalProcessingState.IDLE;
	}

	//-------- methods --------
	
	/**
	 *  Adopt a goal so that the agent tries pursuing it.
	 */
	public static void adoptGoal(RGoal rgoal, final IInternalAccess ia)
	{
//		BDIAgentInterpreter ip = (BDIAgentInterpreter)((BDIAgent)ia).getInterpreter();
//		ip.scheduleStep(new AdoptGoalAction(rgoal));
		ia.getComponentFeature(IExecutionFeature.class).scheduleStep(new AdoptGoalAction(rgoal))
			.addResultListener(new IResultListener<Void>()
		{
			public void resultAvailable(Void result)
			{
			}
			
			public void exceptionOccurred(Exception exception)
			{
				ia.getLogger().warning("Exception during goal adoption:"+exception);
			}
		});
	}
	
	/**
	 *  Get the parentplan.
	 *  @return The parentplan.
	 */
	public RPlan getParentPlan()
	{
		return parentplan;
	}
	
	/**
	 *  Get the parentgoal.
	 *  @return The parentgoal.
	 */
	public RGoal getParentGoal()
	{
		return parentgoal;
	}
	
	/**
	 *  Get parent (goal or plan).
	 */
	public RElement getParent()
	{
		return parentplan!=null? parentplan: parentgoal;
	}

	/**
	 *  Get the lifecycleState.
	 *  @return The lifecycleState.
	 */
	public GoalLifecycleState getLifecycleState()
	{
		return lifecyclestate;
	}

	/**
	 *  Set the lifecycleState.
	 *  @param lifecycleState The lifecycleState to set.
	 */
	public void setLifecycleState(GoalLifecycleState lifecyclestate)
	{
		this.lifecyclestate = lifecyclestate;
		if(GoalLifecycleState.ADOPTED.equals(lifecyclestate))
		{
			publishToolGoalEvent(IMonitoringEvent.EVENT_TYPE_CREATION);
		}
		else if(GoalLifecycleState.DROPPED.equals(lifecyclestate))
		{
			publishToolGoalEvent(IMonitoringEvent.EVENT_TYPE_DISPOSAL);
		}
		else
		{
			publishToolGoalEvent(IMonitoringEvent.EVENT_TYPE_MODIFICATION);
		}
	}

	/**
	 *  Get the processingState.
	 *  @return The processingState.
	 */
	public GoalProcessingState getProcessingState()
	{
		return processingstate;
	}

	/**
	 *  Set the processingState.
	 *  @param processingState The processingState to set.
	 */
	public void setProcessingState(GoalProcessingState processingstate)
	{
		this.processingstate = processingstate;
		publishToolGoalEvent(IMonitoringEvent.EVENT_TYPE_MODIFICATION);
	}
	
	/**
	 *  Set the processingState.
	 *  @param processingState The processingState to set.
	 */
	public void setProcessingState(IInternalAccess ia, GoalProcessingState processingstate)
	{
		if(getProcessingState().equals(processingstate))
			return;
		
//		if(GoalProcessingState.FAILED.equals(processingstate))
//			System.out.println("failed: "+this);
		
		if(GoalProcessingState.FAILED.equals(getProcessingState())
			|| GoalProcessingState.SUCCEEDED.equals(getProcessingState()))
		{
			throw new RuntimeException("Final proc state cannot be changed: "+getProcessingState()+" "+processingstate);
		}
			
//		if(getId().indexOf("AnalyzeTarget")!=-1)
//			System.out.println("changeprocstate: "+this+" "+processingstate+" "+getProcessingState());
		
//		this.processingstate = processingstate;
	
		// If was inprocess -> now stop processing.
//		System.out.println("changeprocstate: "+this+" "+processingstate+" "+getProcessingState());

		if(!GoalProcessingState.INPROCESS.equals(processingstate))
		{
			// todo: introduce some state for finished?!
//			state.setAttributeValue(rgoal, OAVBDIRuntimeModel.processableelement_has_state, null);
			setState(State.INITIAL);
			
			// Remove finished plans that would otherwise interfere with next goal processing (if any).
//			Collection	fplans	= state.getAttributeValues(rgoal, OAVBDIRuntimeModel.goal_has_finishedplans);
//			if(fplans!=null && !fplans.isEmpty())
//			{
//				Object[]	afplans	= fplans.toArray();
//				for(int i=0; i<afplans.length; i++)
//					state.removeAttributeValue(rgoal, OAVBDIRuntimeModel.goal_has_finishedplans, afplans[i]);
//			}
			
			// Reset event processing.
//			BDIInterpreter ip = BDIInterpreter.getInterpreter(state);
//			if(rgoal.equals(state.getAttributeValue(ip.getAgent(), OAVBDIRuntimeModel.agent_has_eventprocessing)))
//				state.setAttributeValue(ip.getAgent(), OAVBDIRuntimeModel.agent_has_eventprocessing, null);
			
			// Reset APL.
//			state.setAttributeValue(rgoal, OAVBDIRuntimeModel.processableelement_has_apl, null);
			setApplicablePlanList(null);
			
			// Clean tried plans if necessary.
			setTriedPlans(null);
			
//			Collection coll = state.getAttributeValues(rgoal, OAVBDIRuntimeModel.goal_has_triedmplans);
//			if(coll!=null)
//			{
//				Object[]	acoll	= coll.toArray();
//				for(int i=0; i<acoll.length; i++)
//				{
//					state.removeAttributeValue(rgoal, OAVBDIRuntimeModel.goal_has_triedmplans, acoll[i]);
//				}
//			}
			
//			// Remove timers.
//			ITimer retrytimer = (ITimer)state.getAttributeValue(rgoal, OAVBDIRuntimeModel.goal_has_retrytimer);
//			if(retrytimer!=null)
//			{
//				retrytimer.cancel();
//				((InterpreterTimedObject)retrytimer.getTimedObject()).getAction().setValid(false);
//			}
			
			
//			if(!OAVBDIRuntimeModel.GOALPROCESSINGSTATE_PAUSED.equals(newstate))
//			{
//				ITimer recurtimer = (ITimer)state.getAttributeValue(rgoal, OAVBDIRuntimeModel.goal_has_recurtimer);
//				if(recurtimer!=null)
//				{
//					recurtimer.cancel();
//					((InterpreterTimedObject)recurtimer.getTimedObject()).getAction().setValid(false);
//				}
//			}
		}
		
		setProcessingState(processingstate);
//		BDIAgentInterpreter ip = (BDIAgentInterpreter)((BDIAgent)ia).getInterpreter();
		
//		state.setAttributeValue(rgoal, OAVBDIRuntimeModel.goal_has_processingstate, newstate);
		
		// If now is inprocess -> start processing
		if(GoalProcessingState.INPROCESS.equals(processingstate))
		{
//			if(getId().indexOf("AchieveCleanup")!=-1)
//				System.out.println("activating: "+this);
			((IInternalBDIAgentFeature)ia.getComponentFeature(IBDIAgentFeature.class)).getRuleSystem().addEvent(new Event(new EventType(new String[]{ChangeEvent.GOALINPROCESS, getMGoal().getName()}), this));
			publishToolGoalEvent(ChangeEvent.GOALINPROCESS);
			setState(ia, RProcessableElement.State.UNPROCESSED);
		}
		else
		{
			((IInternalBDIAgentFeature)ia.getComponentFeature(IBDIAgentFeature.class)).getRuleSystem().addEvent(new Event(new EventType(new String[]{ChangeEvent.GOALNOTINPROCESS, getMGoal().getName()}), this));
		}
		
		if(GoalProcessingState.SUCCEEDED.equals(processingstate)
			|| GoalProcessingState.FAILED.equals(processingstate))
		{
//			if(getModelElement().getName().indexOf("vision")==-1)
//				System.out.println("sgmndsdgbjk");
			setLifecycleState(ia, GoalLifecycleState.DROPPING);
		}
		
//		System.out.println("exit: "+rgoal+" "+state.getAttributeValue(rgoal, OAVBDIRuntimeModel.processableelement_has_state));
	}
	
	/**
	 *  Set the lifecycle state.
	 *  @param processingState The processingState to set.
	 */
	public void setLifecycleState(IInternalAccess ia, GoalLifecycleState lifecyclestate)
	{
		if(lifecyclestate.equals(getLifecycleState()))
			return;
		
//		if(this.toString().indexOf("TreatV")!=-1 && ia.getComponentIdentifier().getName().indexOf("Ambu")!=-1) //&& *///GoalLifecycleState.ACTIVE.equals(getLifecycleState())
//			&& GoalLifecycleState.OPTION.equals(lifecyclestate))
//		{
//			System.out.println("treat goal: "+this);
//			System.out.println("plan to abort: "+childplan+", "+this);
//			agent.getComponentFeature(IBDIAgentFeature.class).getCapability().dumpGoals();
//		}
		
		if(GoalLifecycleState.DROPPED.equals(getLifecycleState()))
			throw new RuntimeException("Final proc state cannot be changed: "+getLifecycleState()+" "+lifecyclestate);
		if(GoalLifecycleState.DROPPING.equals(getLifecycleState()) && !GoalLifecycleState.DROPPED.equals(lifecyclestate))
			throw new RuntimeException("Final proc state cannot be changed: "+getLifecycleState()+" "+lifecyclestate);
		
//		System.out.println("goal state change: "+this.getId()+" "+getLifecycleState()+" "+lifecyclestate);
//		if(RGoal.GOALLIFECYCLESTATE_DROPPING.equals(lifecyclestate) && RGoal.GOALLIFECYCLESTATE_NEW.equals(getLifecycleState()))
//			Thread.dumpStack();
//		if(RGoal.GOALLIFECYCLESTATE_ADOPTED.equals(lifecyclestate) && RGoal.GOALLIFECYCLESTATE_DROPPING.equals(getLifecycleState()))
//			Thread.dumpStack();
//		if(RGoal.GOALLIFECYCLESTATE_DROPPED.equals(getLifecycleState()))
//			Thread.dumpStack();
		
//		if(getId().indexOf("QueryCharging")!=-1 && GOALLIFECYCLESTATE_DROPPING.equals(lifecyclestate))
//			System.out.println("goal state change: "+this.getId()+" "+getLifecycleState()+" "+lifecyclestate);
//		if(getId().indexOf("Battery")!=-1 && GOALLIFECYCLESTATE_DROPPING.equals(lifecyclestate))
//			System.out.println("goal state change: "+this.getId()+" "+getLifecycleState()+" "+lifecyclestate);
//		if(getId().indexOf("AchieveCleanup")!=-1)
//			System.out.println("goal state change: "+this.getId()+" "+getLifecycleState()+" "+lifecyclestate);
//		if(getId().indexOf("Pick")!=-1)
//		if(ia.getExternalAccess().getComponentIdentifier().getLocalName().indexOf("Sentry")!=-1)
//			System.out.println("goal state change: "+this.getId()+" "+getLifecycleState()+" "+lifecyclestate);

//		BDIAgentInterpreter ip = (BDIAgentInterpreter)((BDIAgent)ia).getInterpreter();
		setLifecycleState(lifecyclestate);
		
		if(GoalLifecycleState.ADOPTED.equals(lifecyclestate))
		{
			((IInternalBDIAgentFeature)ia.getComponentFeature(IBDIAgentFeature.class)).getRuleSystem().addEvent(new Event(new EventType(new String[]{ChangeEvent.GOALADOPTED, getMGoal().getName()}), this));
			setLifecycleState(ia, GoalLifecycleState.OPTION);
		}
		else if(GoalLifecycleState.ACTIVE.equals(lifecyclestate))
		{
			((IInternalBDIAgentFeature)ia.getComponentFeature(IBDIAgentFeature.class)).getRuleSystem().addEvent(new Event(new EventType(new String[]{ChangeEvent.GOALACTIVE, getMGoal().getName()}), this));

			// start means-end reasoning
			if(onActivate())
			{
				setProcessingState(ia, GoalProcessingState.INPROCESS);
			}
			else
			{
				setProcessingState(ia, GoalProcessingState.IDLE);
			}
		}
		else if(GoalLifecycleState.OPTION.equals(lifecyclestate))
		{
			// ready to be activated via deliberation
//			if(getId().indexOf("AchieveCleanup")!=-1)
//				System.out.println("option: "+ChangeEvent.GOALOPTION+"."+getId());
			abortPlans();
			setProcessingState(ia, GoalProcessingState.IDLE);
			((IInternalBDIAgentFeature)ia.getComponentFeature(IBDIAgentFeature.class)).getRuleSystem().addEvent(new Event(new EventType(new String[]{ChangeEvent.GOALOPTION, getMGoal().getName()}), this));
		}
		else if(GoalLifecycleState.SUSPENDED.equals(lifecyclestate))
		{
			// goal is suspended (no more plan executions)
//			if(getId().indexOf("PerformLook")==-1)
//				System.out.println("suspending: "+getId());
			
			abortPlans();
			setProcessingState(ia, GoalProcessingState.IDLE);
			((IInternalBDIAgentFeature)ia.getComponentFeature(IBDIAgentFeature.class)).getRuleSystem().addEvent(new Event(new EventType(new String[]{ChangeEvent.GOALSUSPENDED, getMGoal().getName()}), this));
		}
		
		if(GoalLifecycleState.DROPPING.equals(lifecyclestate))
		{
//			if(getId().indexOf("AchieveCleanup")!=-1)
//				System.out.println("dropping achievecleanup");
			
//			if(getId().indexOf("GetVisionAction")==-1)
//				System.out.println("dropping: "+getId());
			
//			System.out.println("dropping: "+getId());
			
//			ip.getRuleSystem().addEvent(new Event(ChangeEvent.GOALDROPPED, this));
			// goal is dropping (no more plan executions)
//			setProcessingState(ia, GOALPROCESSINGSTATE_IDLE);
			abortPlans();
			ia.getExternalAccess().scheduleStep(new DropGoalAction(this));
		}
		else if(GoalLifecycleState.DROPPED.equals(lifecyclestate))
		{
			((IInternalBDIAgentFeature)ia.getComponentFeature(IBDIAgentFeature.class)).getRuleSystem().addEvent(new Event(new EventType(new String[]{ChangeEvent.GOALDROPPED, getMGoal().getName()}), this));

			if(getListeners()!=null)
			{
				if(!isFinished())
				{
					setProcessingState(GoalProcessingState.FAILED);
					setException(new GoalFailureException());
				}
				super.notifyListeners();
			}
		}
	}
	
	/**
	 * 
	 */
	protected void abortPlans()
	{
		if(childplan!=null)
		{
			childplan.abort();
		}
	}
	
	/**
	 * 
	 */
	public MGoal getMGoal()
	{
		return (MGoal)getModelElement();
	}
	
	/**
	 * 
	 */
	public boolean isSucceeded()
	{
		return GoalProcessingState.SUCCEEDED.equals(processingstate);
	}
	
	/**
	 * 
	 */
	public boolean isFailed()
	{
		return GoalProcessingState.FAILED.equals(processingstate);
	}
	
	/**
	 * 
	 */
	public boolean isFinished()
	{
		return isSucceeded() || isFailed();
	}

//	/**
//	 *  Add a rule.
//	 */
//	protected void addRule(IRule<?> rule)
//	{
//		if(rulenames==null)
//			rulenames = new ArrayList<String>();
//		rulenames.add(rule.getName());
//	}
	
//	/**
//	 *  Unobserve a runtime goal.
//	 */
//	public void unobserveGoal(final IInternalAccess ia)
//	{
//		BDIAgentInterpreter ip = (BDIAgentInterpreter)((BDIAgent)ia).getInterpreter();
//			
//		ip.getRuleSystem().unobserveObject(getPojoElement());
//		
//		if(rulenames!=null)
//		{
//			for(String rulename: rulenames)
//			{
//				ip.getRuleSystem().getRulebase().removeRule(rulename);
//			}
//		}
//	}

	/**
	 *  Get the childplan.
	 *  @return The childplan.
	 */
	public RPlan getChildPlan()
	{
		return childplan;
	}

	/**
	 *  Set the childplan.
	 *  @param childplan The childplan to set.
	 */
	public void setChildPlan(RPlan childplan)
	{
		this.childplan = childplan;
	}
	
	/**
	 * 
	 */
	public void addInhibitor(RGoal inhibitor, IInternalAccess ia)
	{		
		if(inhibitors==null)
			inhibitors = new HashSet<RGoal>();
		
		if(inhibitors.add(inhibitor) && inhibitors.size()==1)
		{
//			BDIAgentInterpreter ip = (BDIAgentInterpreter)((BDIAgent)ia).getInterpreter();
			((IInternalBDIAgentFeature)ia.getComponentFeature(IBDIAgentFeature.class)).getRuleSystem().addEvent(new Event(new EventType(new String[]{ChangeEvent.GOALINHIBITED, getMGoal().getName()}), this));
		}
		
//		if(inhibitor.getId().indexOf("AchieveCleanup")!=-1)
//			System.out.println("add inhibit: "+getId()+" "+inhibitor.getId()+" "+inhibitors);
	}
	
	/**
	 * 
	 */
	public void removeInhibitor(RGoal inhibitor, IInternalAccess ia)
	{
//		System.out.println("rem inhibit: "+getId()+" "+inhibitor.getId()+" "+inhibitors);
		
//		if(inhibitor.getId().indexOf("AchieveCleanup")!=-1)
//			System.out.println("kokoko: "+inhibitor);
		
		if(inhibitors!=null)
		{
			if(inhibitors.remove(inhibitor) && inhibitors.size()==0)
			{
//				System.out.println("goal not inhibited: "+this);
//				BDIAgentInterpreter ip = (BDIAgentInterpreter)((BDIAgent)ia).getInterpreter();
				((IInternalBDIAgentFeature)ia.getComponentFeature(IBDIAgentFeature.class)).getRuleSystem().addEvent(new Event(new EventType(new String[]{ChangeEvent.GOALNOTINHIBITED, getMGoal().getName()}), this));
			}
		}
	}
	
	/**
	 * 
	 */
	public boolean isInhibited()
	{
		return inhibitors!=null && !inhibitors.isEmpty();
	}
	
	/**
	 * 
	 */
	public boolean isInhibitedBy(RGoal other)
	{
		return !isFinished() && inhibitors!=null && inhibitors.contains(other);
	}
	
	/**
	 *  Get the inhibitors.
	 *  @return The inhibitors.
	 */
	public Set<RGoal> getInhibitors()
	{
		return inhibitors;
	}

	/**
	 *  Get the hashcode.
	 */
	public int hashCode()
	{
		int ret;
		if(getMGoal().isUnique())
		{
			if(getPojoElement()!=null)
			{
				ret = getPojoElement().hashCode();
			}
			else
			{
				MGoal mgoal	= (MGoal)getModelElement();
				ret = 31 + mgoal.hashCode();
				for(MParameter param: mgoal.getParameters())
				{
					if(mgoal.getExcludes()==null || !mgoal.getExcludes().contains(mgoal.getName()))
					{
						if(!param.isMulti(getAgent().getClassLoader()))
						{
							Object val = getParameter(param.getName()).getValue();
							ret = 31*ret + (val==null? 0: val.hashCode());
						}
						else
						{
							Object[] vals = getParameterSet(param.getName()).getValues();
							ret = 31*ret + (vals==null? 0: Arrays.hashCode(vals));
						}
					}
				}
			}
		}
		else
		{
			ret = super.hashCode();
		}
		return ret;
	}

	/**
	 *  Test if equal to other object.
	 */
	public boolean equals(Object obj)
	{
		boolean ret = false;
		if(obj instanceof RGoal)
		{
			RGoal other = (RGoal)obj;
			if(getMGoal().isUnique())
			{
				if(getPojoElement()!=null)
				{
					ret = getPojoElement().equals(other.getPojoElement());
				}
				else
				{
					ret = isSame(other);
				}
			}
			else
			{
				ret = super.equals(obj);
			}
		}
		return ret;
//		ret = getMGoal().isUnique()? getPojoElement().equals(((RProcessableElement)obj).getPojoElement()): super.equals(obj);
	}

	/** 
	 * 
	 */
	public String toString()
	{
//		return "RGoal(lifecyclestate=" + lifecyclestate + ", processingstate="
//			+ processingstate + ", state=" + state + ", id=" + id + ")";
		return id+" "+getPojoElement();
	}
	
	/**
	 *  Called when a plan is finished.
	 */
	public void planFinished(IInternalAccess ia, IInternalPlan rplan)
	{
		super.planFinished(ia, rplan);
		childplan = null;
		
		if(rplan!=null)
		{
//			if(rplan.getModelElement().getName().indexOf("seen")!=-1)
//				System.out.println("hhhhhhhhhhhhhggg");
			
			// Find parameter mappings for xml agents
			// todo: goal-goal mappings
			if(rplan instanceof RPlan && rplan.isPassed())
			{
				MPlan mplan = (MPlan)((RPlan)rplan).getModelElement();
				Map<String, Object> mappingvals = null;
				if(mplan.getParameters()!=null && mplan.getParameters().size()>0)
				{
					for(MParameter mparam: mplan.getParameters())
					{
						if(MParameter.Direction.OUT.equals(mparam.getDirection()) || MParameter.Direction.INOUT.equals(mparam.getDirection()))
						{
							List<String> mappings = ((MPlanParameter)mparam).getGoalMappings();
							for(String mapping: mappings)
							{
								if(mapping.startsWith(getModelElement().getName()))
								{
									String target = mapping.substring(mapping.indexOf(".")+1);
									if(mappingvals==null)
										mappingvals = new HashMap<String, Object>();
									if(mparam.isMulti(null))
									{
										getParameterSet(target).removeValues();
										Object[] vals = rplan.getParameterSet(mparam.getName()).getValues();
										for(Object val: vals)
										{
											getParameterSet(target).addValue(val);
										}
									}
									else
									{
										getParameter(target).setValue(rplan.getParameter(mparam.getName()).getValue());
									}
									break;
								}
							}
						}
					}
				}
			}
			
//			PlanLifecycleState state = rplan.getLifecycleState();
//			if(state.equals(RPlan.PlanLifecycleState.FAILED))
			if(rplan.isFailed())
			{
				this.setException(rplan.getException());
			}
		}
//		if(rplan!=null)
//			System.out.println("plan finished: "+rplan.getId());
		
//		if(getPojoElement().getClass().getName().indexOf("PatrolPlan")!=-1)
//			System.out.println("pips");
		
		// create reasoning step depending on the processable element type

		// Check procedural success semantics
		if(isProceduralSucceeded())
		{
			setProcessingState(ia, GoalProcessingState.SUCCEEDED);
		}
		
		if(GoalLifecycleState.ACTIVE.equals(getLifecycleState()))
		{
			if(!isSucceeded() && !isFailed())
			{
				// Test if is retry
				if(isRetry() && rplan!=null)
				{
					if(RProcessableElement.State.CANDIDATESSELECTED.equals(getState()))
					{
						if(getMGoal().getRetryDelay()>-1)
						{
							ia.getExternalAccess().scheduleStep(new SelectCandidatesAction(this), getMGoal().getRetryDelay());
						}
						else
						{
							ia.getExternalAccess().scheduleStep(new SelectCandidatesAction(this));
						}
					}
					else if(RProcessableElement.State.NOCANDIDATES.equals(getState()))
					{
						if(getException()==null)
						{
							setException(new GoalFailureException("No candidates."));
						}
						setProcessingState(ia, GoalProcessingState.FAILED);
					}
//					else
//					{
//						System.out.println("??? "+getState());
//					}
				}
				else
				{
					if(isRecur())
					{
						setProcessingState(ia, GoalProcessingState.PAUSED);
						if(getMGoal().getRecurDelay()>-1)
						{
							ia.getExternalAccess().scheduleStep(new IComponentStep<Void>()
							{
								public IFuture<Void> execute(IInternalAccess ia)
								{
									if(RGoal.GoalLifecycleState.ACTIVE.equals(getLifecycleState())
										&& RGoal.GoalProcessingState.PAUSED.equals(getProcessingState()))
									{
										setTriedPlans(null);
										setApplicablePlanList(null);
										setProcessingState(ia, RGoal.GoalProcessingState.INPROCESS);
									}
									return IFuture.DONE;
								}
							}, getMGoal().getRecurDelay());
						}
					}
					else
					{
						if(getException()==null)
						{
							setException(new GoalFailureException("No candidates."));
						}
						setProcessingState(ia, GoalProcessingState.FAILED);
					}
				}
			}
		}
	}
	
//	/**
//	 * 
//	 */
//	public void addGoalListener(IResultListener<Void> listener)
//	{
//		super.addGoalListener(listener);
//		
//		if(isSucceeded())
//		{
//			listener.resultAvailable(null);
//		}
//		else if(isFailed())
//		{
//			listener.exceptionOccurred(exception);
//		}
//		else
//		{
//			listeners.add(listener);
//		}
//	}
	
	//-------- methods that are goal specific --------

	// todo: implement those methods in goal types
	
	/**
	 * 
	 */
	public boolean onActivate()
	{
		return getMGoal().getConditions(MGoal.CONDITION_MAINTAIN)==null; // for perform, achieve, query
	}
	
	/**
	 * 
	 */
	public boolean isRetry()
	{
		return getMGoal().isRetry();
	}
	
	/**
	 * 
	 */
	public boolean isRecur()
	{
		return getMGoal().isRecur();
	}
	
	/**
	 *  Test if a goal has succeeded with respect to its plan execution.
	 */
	public boolean isProceduralSucceeded()
	{
		boolean ret = false;
		
		// todo: perform goals
		if(isProceduralGoal() && getTriedPlans()!=null && !getTriedPlans().isEmpty())
		{
			// OR case
			if(getMGoal().isOrSuccess())
			{
				IInternalPlan rplan = getTriedPlans().get(getTriedPlans().size()-1);
				ret = rplan.isPassed();
			}
			// AND case
			else
			{
				MCapability mcapa = ((IInternalBDIAgentFeature)getAgent().getComponentFeature(IBDIAgentFeature.class)).getBDIModel().getCapability();
				
				String capaname = getMGoal().getCapabilityName();
				if(capaname!=null)
				{
					mcapa = ((BDIModel)((IInternalBDIAgentFeature)getAgent().getComponentFeature(IBDIAgentFeature.class)).getBDIModel()).getCapability(capaname);
				}
				
				// No further candidate? Then is considered as succeeded
				// todo: is it sufficient that one plan has succeeded or all?
				// todo: what to do when rebuild?
				if(getApplicablePlanList().isEmpty())
				{
					List<IInternalPlan> tps = getTriedPlans();
					if(tps!=null && !tps.isEmpty())
					{
						for(IInternalPlan plan: tps)
						{
							if(plan.isPassed())
							{
								ret = true;
								break;
							}
						}
					}
				}
			}
		}
		
		return ret;
	}
	
	/**
	 * 
	 */
	public boolean isProceduralGoal()
	{
		return !getMGoal().isDeclarative();
	}
	
	/**
	 *  Get the goal result of the pojo element.
	 *  Searches @GoalResult and delivers value.
	 */
	public static Object getGoalResult(RGoal rgoal, ClassLoader cl)
	{
		Object ret = null;
		Object pojo = rgoal.getPojoElement();
		MGoal mgoal = rgoal.getMGoal();
		
		if(pojo!=null)
		{
			ret = pojo;
			Object pac = mgoal.getPojoResultReadAccess(cl);
			if(pac instanceof Field)
			{
				try
				{
					Field f = (Field)pac;
					f.setAccessible(true);
					ret = f.get(pojo);
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
			else if(pac instanceof Method)
			{
				try
				{
					Method m = (Method)pac;
					m.setAccessible(true);
					ret = m.invoke(pojo, new Object[0]);
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
		}
		// xml goals
		else
		{
			Map<String, Object> res = new HashMap<String, Object>(); 
			for(IParameter param: rgoal.getParameters())
			{
				MParameter.Direction dir = ((MParameter)param.getModelElement()).getDirection();
				if(MParameter.Direction.OUT.equals(dir) || MParameter.Direction.INOUT.equals(dir))
				{
					res.put(param.getName(), param.getValue());
				}
			}
			for(IParameterSet paramset: rgoal.getParameterSets())
			{
				MParameter.Direction dir = ((MParameter)paramset.getModelElement()).getDirection();
				if(MParameter.Direction.OUT.equals(dir) || MParameter.Direction.INOUT.equals(dir))
				{
					res.put(paramset.getName(), paramset.getValues());
				}
			}
			ret = res.size()==0? null: res.size()==1? res.values().iterator().next(): res;
		}
		
		return ret;
	}
	
//	/**
//	 *  Get the goal result of the pojo element.
//	 *  Searches @GoalResult and delivers value.
//	 */
//	public static Object getGoalResult(Object pojo, MGoal mgoal, ClassLoader cl)
//	{
//		Object ret = pojo;
//		Object pac = mgoal.getPojoResultReadAccess(cl);
//		if(pac instanceof Field)
//		{
//			try
//			{
//				Field f = (Field)pac;
//				f.setAccessible(true);
//				ret = f.get(pojo);
//			}
//			catch(Exception e)
//			{
//				e.printStackTrace();
//			}
//		}
//		else if(pac instanceof Method)
//		{
//			try
//			{
//				Method m = (Method)pac;
//				m.setAccessible(true);
//				ret = m.invoke(pojo, new Object[0]);
//			}
//			catch(Exception e)
//			{
//				e.printStackTrace();
//			}
//		}
//		return ret;
//	}
	
	/**
	 * 
	 */
	public void drop()
	{
		if(!GoalLifecycleState.NEW.equals(getLifecycleState())
			&& !GoalLifecycleState.DROPPING.equals(getLifecycleState()) 
			&& !GoalLifecycleState.DROPPED.equals(getLifecycleState()))
		{
			setLifecycleState(getAgent(), GoalLifecycleState.DROPPING);
		}
	}
	
	/**
	 * 
	 */
	public void targetConditionTriggered(IInternalAccess ia, IEvent event, IRule<Void> rule, Object context)
	{
//		System.out.println("Goal target triggered: "+RGoal.this);
		if(getMGoal().getConditions(MGoal.CONDITION_MAINTAIN)!=null)
		{
			setProcessingState(ia, GoalProcessingState.IDLE);
		}
		else
		{
			setProcessingState(ia, GoalProcessingState.SUCCEEDED);
		}
	}
	
	/**
	 * 
	 * @param result
	 * @param cl
	 */
	public void setGoalResult(Object result, ClassLoader cl)
	{
		setGoalResult(result, cl, null, null, null);
	}
	
	/**
	 *  Set the goal result from a plan.
	 */
	public void setGoalResult(Object result, ClassLoader cl, ChangeEvent event, RPlan rplan, RProcessableElement rpe)
	{
//		System.out.println("set goal result: "+result);
		
		MGoal mgoal = (MGoal)getModelElement();
		Object wa = mgoal.getPojoResultWriteAccess(cl);
		if(wa instanceof Field)
		{
			try
			{
				Field f = (Field)wa;
				f.setAccessible(true);
				f.set(getPojoElement(), result);
			}
			catch(Exception e)
			{
				throw new RuntimeException(e);
			}
		}
		else if(wa instanceof Method)
		{
			try
			{
				Method m = (Method)wa;
				m.setAccessible(true);
				List<Object> res = new ArrayList<Object>();
				res.add(result);
				Object[] params = BDIAgentFeature.getInjectionValues(m.getParameterTypes(), m.getParameterAnnotations(), 
					rplan!=null? rplan.getModelElement(): rpe.getModelElement(), event, rplan, rpe, res, getAgent());
				if(params==null)
					System.out.println("Invalid parameter assignment");
				m.invoke(getPojoElement(), params);
			}
			catch(Exception e)
			{
				throw new RuntimeException(e);
			}
		}
	}
	
	/**
	 *  Test if this goal inhibits the other.
	 */
	public boolean inhibits(RGoal other, IInternalAccess ia)
	{
		if(this.equals(other))
			return false;
		
		// todo: cardinality
		
		boolean ret = false;
		
		if(getLifecycleState().equals(GoalLifecycleState.ACTIVE) && getProcessingState().equals(GoalProcessingState.INPROCESS))
		{
			MDeliberation delib = getMGoal().getDeliberation();
			if(delib!=null)
			{
				if(delib.isCardinalityOne() && other.getMGoal().equals(getMGoal()))
				{
					ret = true;
				}
				else
				{
//					Set<MGoal> minh = delib.getInhibitions();
					Set<MGoal> minh = delib.getInhibitions(getMCapability());
					MGoal mother = other.getMGoal();
					if(minh!=null && minh.contains(mother))
					{
						ret = true;
						
						// check if instance relation
						Map<String, MethodInfo> dms = delib.getInhibitionMethods();
						if(dms!=null)
						{
							MethodInfo mi = dms.get(mother.getName());
							if(mi!=null)
							{
								Method dm = mi.getMethod(ia.getClassLoader());
								try
								{
									dm.setAccessible(true);
									ret = ((Boolean)dm.invoke(getPojoElement(), new Object[]{other.getPojoElement()})).booleanValue();
								}
								catch(Exception e)
								{
									Throwable	t	= e instanceof InvocationTargetException ? ((InvocationTargetException)e).getTargetException() : e;
									ia.getLogger().severe("Exception in inhibits expression: "+t);
								}
							}
						}
						
						// xml inhibition expressions
						Map<String, UnparsedExpression> uexps = delib.getInhibitionExpressions();
						if(uexps!=null)
						{
							UnparsedExpression uexp = uexps.get(mother.getName());
							if(uexp!=null && uexp.getValue()!=null && uexp.getValue().length()>0)
							{
								SimpleValueFetcher fet = new SimpleValueFetcher(agent.getFetcher());
								fet.setValue("$goal", this);
								fet.setValue("$ref", other);
								
								try
								{
									ret = ((Boolean)SJavaParser.parseExpression(uexp, getAgent().getModel().getAllImports(), getAgent().getClassLoader()).getValue(fet)).booleanValue();
								}
								catch(Exception e)
								{
									Throwable	t	= e instanceof InvocationTargetException ? ((InvocationTargetException)e).getTargetException() : e;
									ia.getLogger().severe("Exception in inhibits expression: "+t);
								}
							}
						}
					}
				}
			}
		}
		
		return ret;
	}
	
//	/**
//	 * 
//	 */
//	class LifecycleStateCondition implements ICondition
//	{
//		/** The allowed states. */
//		protected Set<GoalLifecycleState> states;
//		
//		/** The flag if state is allowed or disallowed. */
//		protected boolean allowed;
//		
//		/**
//		 * 
//		 */
//		public LifecycleStateCondition(GoalLifecycleState state)
//		{
//			this(SUtil.createHashSet(new GoalLifecycleState[]{state}));
//		}
//		
//		/**
//		 * 
//		 */
//		public LifecycleStateCondition(Set<GoalLifecycleState> states)
//		{
//			this(states, true);
//		}
//		
//		/**
//		 * 
//		 */
//		public LifecycleStateCondition(GoalLifecycleState state, boolean allowed)
//		{
//			this(SUtil.createHashSet(new GoalLifecycleState[]{state}), allowed);
//		}
//		
//		/**
//		 * 
//		 */
//		public LifecycleStateCondition(Set<GoalLifecycleState> states, boolean allowed)
//		{
//			this.states = states;
//			this.allowed = allowed;
//		}
//		
//		/**
//		 * 
//		 */
//		public IFuture<Tuple2<Boolean, Object>> evaluate(IEvent event)
//		{
//			Future<Tuple2<Boolean, Object>> ret = new Future<Tuple2<Boolean, Object>>();
//			boolean res = states.contains(getLifecycleState());
//			if(!allowed)
//				res = !res;
//			ret.setResultIfUndone(res? ICondition.TRUE: ICondition.FALSE);
//			return ret;
//		}
//	}
	
//	/**
//	 * 
//	 */
//	class ProcessingStateCondition implements ICondition
//	{
//		/** The allowed states. */
//		protected Set<String> states;
//		
//		/** The flag if state is allowed or disallowed. */
//		protected boolean allowed;
//		
//		/**
//		 * 
//		 */
//		public ProcessingStateCondition(String state)
//		{
//			this(SUtil.createHashSet(new String[]{state}));
//		}
//		
//		/**
//		 * 
//		 */
//		public ProcessingStateCondition(Set<String> states)
//		{
//			this(states, true);
//		}
//		
//		/**
//		 * 
//		 */
//		public ProcessingStateCondition(String state, boolean allowed)
//		{
//			this(SUtil.createHashSet(new String[]{state}), allowed);
//		}
//		
//		/**
//		 * 
//		 */
//		public ProcessingStateCondition(Set<String> states, boolean allowed)
//		{
//			this.states = states;
//			this.allowed = allowed;
//		}
//		
////		/**
////		 * 
////		 */
////		public Tuple2<Boolean, Object> evaluate(IEvent event)
////		{
////			boolean ret = states.contains(getProcessingState());
////			if(!allowed)
////				ret = !ret;
////			return ret? ICondition.TRUE: ICondition.FALSE;
////		}
//		
//		/**
//		 * 
//		 */
//		public IFuture<Tuple2<Boolean, Object>> evaluate(IEvent event)
//		{
//			Future<Tuple2<Boolean, Object>> ret = new Future<Tuple2<Boolean, Object>>();
//			boolean res = states.contains(getProcessingState());
//			if(!allowed)
//				res = !res;
//			ret.setResultIfUndone(res? ICondition.TRUE: ICondition.FALSE);
//			return ret;
//		}
//	}
	
	/**
	 * 
	 */
	public void publishToolGoalEvent(String evtype)
	{
		if(getAgent().getComponentFeature0(IMonitoringComponentFeature.class)!=null 
			&& getAgent().getComponentFeature(IMonitoringComponentFeature.class).hasEventTargets(PublishTarget.TOSUBSCRIBERS, PublishEventLevel.FINE))
		{
			long time = System.currentTimeMillis();//getClockService().getTime();
			MonitoringEvent mev = new MonitoringEvent();
			mev.setSourceIdentifier(getAgent().getComponentIdentifier());
			mev.setTime(time);
			
			GoalInfo info = GoalInfo.createGoalInfo(this);
			mev.setType(evtype+"."+IMonitoringEvent.SOURCE_CATEGORY_GOAL);
//			mev.setProperty("sourcename", element.toString());
			mev.setProperty("sourcetype", info.getType());
			mev.setProperty("details", info);
			mev.setLevel(PublishEventLevel.FINE);
			
			getAgent().getComponentFeature(IMonitoringComponentFeature.class).publishEvent(mev, PublishTarget.TOSUBSCRIBERS);
		}
	}
	
//	/**
//	 * 
//	 */
//	public BDIAgentInterpreter getInterpreter()
//	{
//		return (BDIAgentInterpreter)((BDIAgent)ia).getInterpreter();
//	}
	
//	public static void main(String[] args)
//	{
//		 PrintWriter out = new PrintWriter(System.err); 
//		List<MemoryPoolMXBean> pools = ManagementFactory.getMemoryPoolMXBeans();
//         for (MemoryPoolMXBean pool : pools) {
//           MemoryUsage peak = pool.getPeakUsage();
//           out.printf("Peak %s memory used: %,d%n", pool.getName(), peak.getUsed());
//           out.printf("Peak %s memory reserved: %,d%n", pool.getName(), peak.getCommitted());
//         }
//         out.close();
//	}
	
//	/**
//	 *  Get the parent plan.
//	 *  @return The parent plan.
//	 */
//	public IPlan getParent()
//	{
//		return parentplan;
//	}

	// IInternalPlan extra methods
	
	/**
	 *  Get the candidate.
	 *  @return The candidate.
	 */
	public Object getCandidate()
	{
		return getModelElement();
	}
	
	/**
	 *  Test if plan has passed.
	 */
	public boolean isPassed()
	{
		return isSucceeded();
	}
	
	/**
	 *  Check if the goal is the same as another goal
	 *  with respect to uniqueness settings.
	 *  When two goals are the same this does not mean
	 *  the objects are equal() in the Java sense!
	 */
	public boolean isSame(IGoal goal)
	{
		// Goals are only the same when they are of same type.
		boolean	same	= getModelElement().equals(goal.getModelElement());
		
		if(same)
		{
			// Check parameter correspondence of goal.
			MGoal mgoal	= (MGoal)goal.getModelElement();

			for(MParameter param: mgoal.getParameters())
			{
				if(!param.isMulti(getAgent().getClassLoader()))
				{
					// Compare parameter values.
					// Todo: Catch exceptions on parameter access?
					Object	val1	= this.getParameter(param.getName()).getValue();
					Object	val2	= goal.getParameter(param.getName()).getValue();
					same	= val1==val2 || val1!=null && val1.equals(val2);
				}
				else
				{
					// Compare parameter set values.
					// Todo: Catch exceptions on parameter set access?
					Object[] vals1 = this.getParameterSet(param.getName()).getValues();
					Object[] vals2 = goal.getParameterSet(param.getName()).getValues();
					same = vals1.length==vals2.length;
					for(int j = 0; same && j < vals1.length; j++)
					{
						same = vals1[j] == vals2[j] || vals1[j] != null && vals1[j].equals(vals2[j]);
					}
				}
			}
		}

		return same;
	}
	
	/**
	 *  Test if a querygoal is finished.
	 *  It is finished when all out parameters/sets are filled with a value.
	 */
	public static boolean isQueryGoalFinished(RGoal goal)
	{
		boolean ret = true;
		
		for(IParameter param: goal.getParameters())
		{
			Direction dir = ((MParameter)param.getModelElement()).getDirection();
			if(MParameter.Direction.OUT.equals(dir) || MParameter.Direction.INOUT.equals(dir))
			{
				Object val = param.getValue();
				ret = val!=null;
				if(!ret)
					break;
			}
		}
		
		if(ret)
		{
			for(IParameterSet paramset: goal.getParameterSets())
			{
				Direction dir = ((MParameter)paramset.getModelElement()).getDirection();
				if(MParameter.Direction.OUT.equals(dir) || MParameter.Direction.INOUT.equals(dir))
				{
					Object[] vals = paramset.getValues();
					ret = vals.length>0;
					if(!ret)
						break;
				}
			}
		}
		
//		if(ret)
//			System.out.println("query finished: "+goal);
		
		System.out.println("querygoal: "+ret+" "+goal);
		
		return ret;
	}
}
