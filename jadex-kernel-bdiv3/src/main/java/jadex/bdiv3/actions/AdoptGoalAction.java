package jadex.bdiv3.actions;

import jadex.bdiv3.annotation.Goal;
import jadex.bdiv3.annotation.GoalAPI;
import jadex.bdiv3.annotation.GoalParent;
import jadex.bdiv3.features.IBDIAgentFeature;
import jadex.bdiv3.features.impl.IInternalBDIAgentFeature;
import jadex.bdiv3.runtime.impl.RGoal;
import jadex.bdiv3.runtime.impl.RPlan;
import jadex.bdiv3.runtime.impl.RPlan.PlanLifecycleState;
import jadex.bridge.IConditionalComponentStep;
import jadex.bridge.IInternalAccess;
import jadex.commons.SReflect;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;

import java.lang.reflect.Field;

/**
 * 
 */
public class AdoptGoalAction implements IConditionalComponentStep<Void>
{
	/** The goal. */
	protected RGoal goal;
	
	/** The state. */
	protected PlanLifecycleState state;
	
	/**
	 *  Create a new action.
	 */
	public AdoptGoalAction(RGoal goal)
	{
//		System.out.println("adopting: "+goal.getId()+" "+goal.getPojoElement().getClass().getName());
		this.goal = goal;
		
		// todo: support this also for a parent goal?!
		if(goal.getParent() instanceof RPlan)
		{
			this.state = goal.getParentPlan().getLifecycleState();
		}
	}
	
	/**
	 *  Test if the action is valid.
	 *  @return True, if action is valid.
	 */
	public boolean isValid()
	{
		return (state==null || state.equals(goal.getParentPlan().getLifecycleState())) 
			&& RGoal.GoalLifecycleState.NEW.equals(goal.getLifecycleState());
	}
	
	/**
	 *  Execute the command.
	 *  @param args The argument(s) for the call.
	 *  @return The result of the command.
	 */
	public IFuture<Void> execute(IInternalAccess ia)
	{
		Future<Void> ret = new Future<Void>();
		try
		{
//			BDIAgentInterpreter ip = (BDIAgentInterpreter)((BDIAgent)ia).getInterpreter();
			// todo: observe class and goal itself!
//			goal.observeGoal(ia);
			
			// inject goal elements
			Class<?> cl = goal.getPojoElement().getClass();
			
			while(cl.isAnnotationPresent(Goal.class))
			{
				Field[] fields = cl.getDeclaredFields();
				for(Field f: fields)
				{
					if(f.isAnnotationPresent(GoalAPI.class))
					{
						f.setAccessible(true);
						f.set(goal.getPojoElement(), goal);
					}
					else if(f.isAnnotationPresent(GoalParent.class))
					{
						if(goal.getParent()!=null)
						{
							Object pa = goal.getParent();
							Object pojopa = null;
							if(pa instanceof RPlan)
							{
								pojopa = ((RPlan)pa).getPojoPlan();
							}
							else if(pa instanceof RGoal)
							{
								pojopa = ((RGoal)pa).getPojoElement();
							}	
								
							if(SReflect.isSupertype(f.getType(), pa.getClass()))
							{
								f.setAccessible(true);
								f.set(goal.getPojoElement(), pa);
							}
							else if(pojopa!=null && SReflect.isSupertype(f.getType(), pojopa.getClass()))
							{
								f.setAccessible(true);
								f.set(goal.getPojoElement(), pojopa);
							}
						}
					}
				}
				cl = cl.getSuperclass();
			}
			
			((IInternalBDIAgentFeature)ia.getComponentFeature(IBDIAgentFeature.class)).getCapability().addGoal(goal);
			goal.setLifecycleState(ia, RGoal.GoalLifecycleState.ADOPTED);
			ret.setResult(null);
		}
		catch(Exception e)
		{
			ret.setException(e);
		}
		return ret;
	}
}
