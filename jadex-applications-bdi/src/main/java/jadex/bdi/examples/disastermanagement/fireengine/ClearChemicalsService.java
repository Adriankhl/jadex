package jadex.bdi.examples.disastermanagement.fireengine;

import jadex.application.space.envsupport.environment.ISpaceObject;
import jadex.bdi.examples.disastermanagement.IClearChemicalsService;
import jadex.bdi.runtime.AgentEvent;
import jadex.bdi.runtime.IBDIExternalAccess;
import jadex.bdi.runtime.ICapability;
import jadex.bdi.runtime.IEAGoal;
import jadex.bdi.runtime.IGoal;
import jadex.bdi.runtime.IGoalListener;
import jadex.bridge.IExternalAccess;
import jadex.commons.Future;
import jadex.commons.IFuture;
import jadex.commons.concurrent.DefaultResultListener;
import jadex.commons.concurrent.IResultListener;
import jadex.commons.service.BasicService;

/**
 *   Clear chemicals service.
 */
public class ClearChemicalsService extends BasicService implements IClearChemicalsService
{
	//-------- attributes --------
	
	/** The agent. */
	protected ICapability agent;
	
	//-------- constructors --------
	
	/**
	 *  Create a new service.
	 */
	public ClearChemicalsService(ICapability agent)
	{
		super(agent.getServiceProvider().getId(), IClearChemicalsService.class, null);
		this.agent = agent;
	}
	
	//-------- methods --------
	
	/**
	 *  Clear chemicals.
	 *  @param disaster The disaster.
	 *  @return Future, null when done.
	 */
	public IFuture clearChemicals(final ISpaceObject disaster)
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
				final IGoal exfire = agent.getGoalbase().createGoal("clear_chemicals");
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
			}
		}
		
		return ret;
	}
	
	/**
	 *  Abort clearing chemicals.
	 *  @return Future, null when done.
	 */
	public IFuture abort()
	{
		final Future ret = new Future();
		
		IGoal[] goals = (IGoal[])agent.getGoalbase().getGoals("clear_chemicals");
		for(int i=0; i<goals.length; i++)
		{
//			System.out.println("Dropping: "+goals[i]);
			goals[i].drop();
		}
		ret.setResult(null);
		
		return ret;
	}

	/**
	 *  Get the string representation.
	 *  @return The string representation.
	 */
	public String toString()
	{
		return "ClearChemicalsService, "+agent.getComponentIdentifier();
	}
}
