package jadex.bdi.examples.cleanerworld;

import jadex.application.space.envsupport.environment.IEnvironmentSpace;
import jadex.application.space.envsupport.environment.ISpaceAction;
import jadex.application.space.envsupport.environment.ISpaceObject;
import jadex.application.space.envsupport.environment.space2d.Space2D;
import jadex.application.space.envsupport.math.IVector1;
import jadex.application.space.envsupport.math.IVector2;
import jadex.application.space.envsupport.math.Vector1Double;
import jadex.bridge.IComponentIdentifier;
import jadex.commons.SimplePropertyObject;

import java.util.Map;

/**
 *  Action for dropping a waste.
 */
public class DropWasteAction extends SimplePropertyObject implements ISpaceAction
{
	protected static final IVector1 TOLERANCE = new Vector1Double(0.05);
	
	/**
	 * Performs the action.
	 * @param parameters parameters for the action
	 * @param space the environment space
	 * @return action return value
	 */
	public Object perform(Map parameters, IEnvironmentSpace space)
	{	
		boolean ret = false;
				
		Space2D env = (Space2D)space;
		
		IComponentIdentifier owner = (IComponentIdentifier)parameters.get(ISpaceAction.ACTOR_ID);
		ISpaceObject wastebin = (ISpaceObject)parameters.get(ISpaceAction.OBJECT_ID);
		ISpaceObject waste = (ISpaceObject)parameters.get("waste");
		ISpaceObject avatar = env.getAvatar(owner);

//		if(so.getProperty("garbage")!=null)
//			System.out.println("pickup failed: "+so);
		
		assert avatar.getProperty("waste")!=null: avatar;
		
		if(env.getDistance((IVector2)wastebin.getProperty(Space2D.PROPERTY_POSITION), (IVector2)avatar.getProperty(Space2D.PROPERTY_POSITION)).greater(TOLERANCE))
			throw new RuntimeException("Not near enough to wastebin: "+wastebin+" "+avatar);
			
//		if(Math.random()>0.5)
		{
//			System.out.println("drop: "+waste);
			if(!((Boolean)wastebin.getProperty("full")).booleanValue())
			{
				int wastes = ((Integer)wastebin.getProperty("wastes")).intValue();
				wastebin.setProperty("wastes", new Integer(wastes+1));
				env.destroySpaceObject(waste.getId());
				avatar.setProperty("waste", null);
				ret = true;
			}
			//pcs.firePropertyChange("worldObjects", garb, null);
//				System.out.println("Agent picked up: "+owner+" "+so.getProperty(Space2D.POSITION));
		}

//		System.out.println("pickup waste action "+parameters);

		return ret? Boolean.TRUE: Boolean.FALSE;
	}
}
