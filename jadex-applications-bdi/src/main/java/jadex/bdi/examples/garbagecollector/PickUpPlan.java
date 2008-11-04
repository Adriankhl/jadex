package jadex.bdi.examples.garbagecollector;

import jadex.bdi.runtime.Plan;

/**
 *  Try to pickup some piece of garbage.
 */
public class PickUpPlan extends Plan
{
	/**
	 *  The plan body.
	 */
	public void body()
	{
		Environment env = (Environment)getBeliefbase().getBelief("env").getFact();

		//System.out.println("Calling pickup: "+getAgentName()+" "+getRootGoal());
		if(!env.pickup(getAgentName()))
			fail();
	}
}
