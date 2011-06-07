package jadex.bdi.examples.disastermanagement.ambulance;

import jadex.bdi.examples.disastermanagement.DeliverPatientTask;
import jadex.bdi.examples.disastermanagement.ITreatVictimsService;
import jadex.bdi.runtime.AgentEvent;
import jadex.bdi.runtime.IBDIInternalAccess;
import jadex.bdi.runtime.IGoal;
import jadex.bdi.runtime.IGoalListener;
import jadex.bridge.service.annotation.ServiceComponent;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.extension.envsupport.environment.ISpaceObject;

/**
 *   Treat victims service.
 */
public class TreatVictimsService implements ITreatVictimsService
{
	//-------- attributes --------
	
	/** The agent. */
	@ServiceComponent
	protected IBDIInternalAccess agent;
	
	public TreatVictimsService()
	{
	}
	
	//-------- methods --------
	
	/**
	 *  Treat victims.
	 *  @param disaster The disaster.
	 *  @return Future, null when done.
	 */
	public IFuture treatVictims(final ISpaceObject disaster)
	{
		final Future ret = new Future();
		
		IGoal[] goals = (IGoal[])agent.getGoalbase().getGoals("treat_victims");
		if(goals.length>0)
		{
			ret.setException(new IllegalStateException("Can only handle one order at a time. Use abort() first."));
		}
		else
		{
			final IGoal tv = (IGoal)agent.getGoalbase().createGoal("treat_victims");
			tv.getParameter("disaster").setValue(disaster);
			tv.addGoalListener(new IGoalListener()
			{
				public void goalFinished(AgentEvent ae)
				{
					System.out.println("tv fin: "+agent.getAgentName());
					if(tv.isSucceeded())
						ret.setResult(null);
					else
						ret.setException(tv.getException());
				}
				
				public void goalAdded(AgentEvent ae)
				{
				}
			});
			System.out.println("tv start: "+agent.getAgentName());
			agent.getGoalbase().dispatchTopLevelGoal(tv);
		}
		
		return ret;
	}
	
	/**
	 *  Abort extinguishing fire.
	 *  @return Future, null when done.
	 */
	public IFuture abort()
	{
		final Future ret = new Future();
		
		ISpaceObject myself	= (ISpaceObject)agent.getBeliefbase().getBelief("myself").getFact();
		if(((Boolean)myself.getProperty(DeliverPatientTask.PROPERTY_PATIENT)).booleanValue())
		{
			ret.setException(new IllegalStateException("Can not abort with patient on board."));			
		}
		else
		{
			IGoal[] goals = (IGoal[])agent.getGoalbase().getGoals("treat_victims");
			for(int i=0; i<goals.length; i++)
			{
//				System.out.println("Dropping: "+goals[i]);
				goals[i].drop();
			}
			ret.setResult(null);
		}
		
		return ret;
	}

	/**
	 *  Get the string representation.
	 *  @return The string representation.
	 */
	public String toString()
	{
		return "TreatVictimsService, "+agent.getComponentIdentifier();
	}
}
