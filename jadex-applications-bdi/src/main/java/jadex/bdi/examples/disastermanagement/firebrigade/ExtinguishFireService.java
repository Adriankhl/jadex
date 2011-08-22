package jadex.bdi.examples.disastermanagement.firebrigade;

import jadex.bdi.examples.disastermanagement.IExtinguishFireService;
import jadex.bdi.runtime.AgentEvent;
import jadex.bdi.runtime.IBDIInternalAccess;
import jadex.bdi.runtime.IGoal;
import jadex.bdi.runtime.IGoalListener;
import jadex.bridge.service.annotation.Service;
import jadex.bridge.service.annotation.ServiceComponent;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.extension.envsupport.environment.ISpaceObject;

/**
 *  Fire extinguish service.
 */
@Service
public class ExtinguishFireService implements IExtinguishFireService
{
	//-------- attributes --------
	
	/** The agent. */
	@ServiceComponent
	protected IBDIInternalAccess agent;

	//-------- methods --------
	
	/**
	 *  Extinguish a fire.
	 *  @param disaster The disaster.
	 */
	public IFuture extinguishFire(final ISpaceObject disaster)
	{
		final Future ret = new Future();
		
		IGoal[] exgoals = (IGoal[])agent.getGoalbase().getGoals("extinguish_fire");
		if(exgoals.length>0)
		{
			ret.setException(new IllegalStateException("Can only handle one order at a time. Use abort() first."));
		}
		else
		{
			IGoal[] goals = (IGoal[])agent.getGoalbase().getGoals("clear_chemicals");
			if(goals.length>0)
			{
				ret.setException(new IllegalStateException("Can only handle one order at a time. Use abort() first."));
			}
			else
			{
				final IGoal exfire = (IGoal)agent.getGoalbase().createGoal("extinguish_fire");
				exfire.getParameter("disaster").setValue(disaster);
				exfire.addGoalListener(new IGoalListener()
				{
					public void goalFinished(AgentEvent ae)
					{
						if(exfire.isSucceeded())
							ret.setResult(null);
						else
							ret.setException(new RuntimeException("Goal failure."));
					}
					
					public void goalAdded(AgentEvent ae)
					{
					}
				});
				agent.getGoalbase().dispatchTopLevelGoal(exfire);
			}
		}
		
		return ret;
	}
	
	/**
	 *  Abort extinguishing fire.
	 *  @return Future, null when done.
	 */
	public IFuture abort()
	{
//		System.out.println("Aborting force: "+agent.getAgentName());
		final Future ret = new Future();
		
		IGoal[] goals = (IGoal[])agent.getGoalbase().getGoals("extinguish_fire");
		for(int i=0; i<goals.length; i++)
		{
//			System.out.println("Dropping: "+goals[i]);
			goals[i].drop();
		}
		ret.setResult(null);
		
//		System.out.println("Aborted force: "+agent.getAgentName());
		return ret;
	}

	/**
	 *  Get the string representation.
	 *  @return The string representation.
	 */
	public String toString()
	{
		return "ExtinguishFireService, "+agent.getComponentIdentifier();
	}
}
