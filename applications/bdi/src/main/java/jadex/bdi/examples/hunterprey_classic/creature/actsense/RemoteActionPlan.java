package jadex.bdi.examples.hunterprey_classic.creature.actsense;

import jadex.bdi.examples.hunterprey_classic.environment.IEnvironmentService;
import jadex.bdiv3.runtime.IGoal;
import jadex.bdiv3.runtime.impl.GoalFailureException;
import jadex.bdiv3.runtime.impl.PlanFailureException;
import jadex.bdiv3x.runtime.Plan;
import jadex.bridge.IComponentIdentifier;
import jadex.bridge.service.IService;


/**
 *  Offers a methods for interacting with the hunterprey environment.
 */
public abstract class RemoteActionPlan extends Plan
{
	/**
	 *  Request an action from the environment.
	 *  @param action The action.
	 *  @return The finished goal.
	 *  @throws GoalFailureException	when the request goal fails.
	 */
	public IGoal requestAction(Object action)
		throws GoalFailureException
	{
		// Search and store the environment agent.
		IComponentIdentifier	env	= searchEnvironmentAgent();

		IGoal rg = createGoal("rp_initiate");
		rg.getParameter("receiver").setValue(env);
		rg.getParameter("action").setValue(action);
//		rg.getParameter("ontology").setValue(HunterPreyOntology.ONTOLOGY_NAME);
		//rg.getParameter("language").setValue(SFipa.JADEX_XML);

		dispatchSubgoalAndWait(rg);
		
		return rg;
	}

	/**
	 *  When the plan has failed, assume that environment is down.
	 *  Remove fact to enable new search for environment.
	 */
	public void failed()
	{
		// Received a timeout. Probably the environment agent has died.
		
		getBeliefbase().getBelief("environmentagent").setFact(null);
	}
	
	//--------- helper methods --------
	
	/**
	 *  Search the environent agent and store its AID in the beliefbase.
	 */
	protected IComponentIdentifier searchEnvironmentAgent()
	{
		IComponentIdentifier res = (IComponentIdentifier)getBeliefbase().getBelief("environmentagent").getFact();

		if(res==null)
		{
			try
			{
				IEnvironmentService es = getAgent().getLocalService(IEnvironmentService.class);
//				System.out.println("env agent: "+((IService)es).getId().getProviderId());
				res = ((IService)es).getServiceId().getProviderId();
				getBeliefbase().getBelief("environmentagent").setFact(res);
//				System.out.println("env set: "+res);
			}
			catch(Exception e)
			{
				throw new PlanFailureException();
			}
			
//			IDF df = (IDF)getAgent().getFeature(IRequiredServicesFeature.class).searchService(new ServiceQuery<>( IDF.class, RequiredServiceInfo.SCOPE_PLATFORM)).get();
//			IDFServiceDescription sd = new DFServiceDescription(null, "hunter-prey environment", null);
//			IDFComponentDescription ad = new DFComponentDescription(null, sd);
//			IDFComponentDescription[] tas = df.search(ad, null).get();
//
//			if(tas.length!=0)
//			{
//				// Found.
//				res	= tas[0].getName();
//				getBeliefbase().getBelief("environmentagent").setFact(res);
//				if(tas.length>1)
//					getLogger().warning("More than environment agent found.");
//			}
//			else
//			{
//				// Not found.
//				throw new PlanFailureException();
//			}
		}

		return res;
	}

}
