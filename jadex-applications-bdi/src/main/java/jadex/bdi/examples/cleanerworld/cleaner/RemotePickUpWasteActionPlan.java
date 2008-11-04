package jadex.bdi.examples.cleanerworld.cleaner;

import jadex.bdi.examples.cleanerworld.RequestPickUpWaste;
import jadex.bdi.examples.cleanerworld.Waste;


/**
 *  Pick up a piece of waste in the environment.
 */
public class RemotePickUpWasteActionPlan extends RemoteActionPlan
{
	//-------- methods --------

	/**
	 *  The plan body.
	 */
	public void body()
	{
		Waste waste = (Waste)getParameter("waste").getValue();
		RequestPickUpWaste rp = new RequestPickUpWaste();
		rp.setWaste(waste);
		requestAction(rp);
	}
}
