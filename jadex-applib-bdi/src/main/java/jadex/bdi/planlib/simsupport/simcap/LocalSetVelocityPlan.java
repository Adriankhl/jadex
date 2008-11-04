package jadex.bdi.planlib.simsupport.simcap;

import jadex.bdi.planlib.simsupport.common.math.IVector2;
import jadex.bdi.planlib.simsupport.environment.ISimulationEngine;
import jadex.bdi.runtime.IBeliefbase;
import jadex.bdi.runtime.Plan;

/** Plan setting the velocity (direction and speed) of the object.
 */
public class LocalSetVelocityPlan extends Plan
{
	/** Sets the new velocity of the object.
	 */
	public void body()
	{
		Integer objectId = (Integer) getParameter("object_id").getValue();
		IBeliefbase b = getBeliefbase();
		ISimulationEngine engine =
			(ISimulationEngine) b.getBelief("local_simulation_engine").getFact();
		IVector2 velocity = (IVector2) getParameter("velocity").getValue();
		engine.getSimulationObject(objectId).setVelocity(velocity);
	}
}
