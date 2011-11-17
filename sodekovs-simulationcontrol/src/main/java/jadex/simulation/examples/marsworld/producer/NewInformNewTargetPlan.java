package jadex.simulation.examples.marsworld.producer;

import jadex.base.fipa.SFipa;
import jadex.bdi.runtime.IChangeEvent;
import jadex.bdi.runtime.IMessageEvent;
import jadex.bdi.runtime.Plan;
import jadex.bridge.IExternalAccess;
import jadex.extension.envsupport.environment.ISpaceObject;
import jadex.extension.envsupport.environment.space2d.ContinuousSpace2D;
import jadex.extension.envsupport.math.IVector2;

/**
 *  Inform the sentry agent about a new target.
 */
public class NewInformNewTargetPlan extends Plan
{
	//-------- methods --------

	/**
	 *  The plan body.
	 */
	public void body()
	{
		IChangeEvent	reason	= (IChangeEvent)getReason();
		ISpaceObject	target	= (ISpaceObject)reason.getValue();
	
		
		//to closest sentry
		ContinuousSpace2D space = (ContinuousSpace2D) ((IExternalAccess) getScope().getParent()).getExtension("my2dspace").get(this);
		IVector2 myPos = (IVector2) getBeliefbase().getBelief("myPos").getFact();
		ISpaceObject nearestSentry = space.getNearestObject(myPos, null, "sentry");		
		
		IMessageEvent mevent = createMessageEvent("inform_target");
		mevent.getParameterSet(SFipa.RECEIVERS).addValue(space.getOwner(nearestSentry.getId()).getName());
		mevent.getParameter(SFipa.CONTENT).setValue(target);
		sendMessage(mevent);
	}
}
