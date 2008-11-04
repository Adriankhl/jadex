package jadex.bdi.examples.cleanerworld.cleaner;

import jadex.bdi.examples.cleanerworld.IEnvironment;
import jadex.bdi.examples.cleanerworld.Waste;
import jadex.bdi.runtime.Plan;
import jadex.bdi.runtime.PlanFailureException;


/**
 *  Pick up a piece of waste in the environment.
 */
public class LocalPickUpWasteActionPlan extends	Plan
{
	//-------- methods --------

	/**
	 *  The plan body.
	 */
	public void body()
	{
		IEnvironment	environment	= (IEnvironment)getBeliefbase().getBelief("environment").getFact();
		Waste waste = (Waste)getParameter("waste").getValue();

		boolean	success	= environment.pickUpWaste(waste);

		if(!success)
			throw new PlanFailureException();
	}
}
