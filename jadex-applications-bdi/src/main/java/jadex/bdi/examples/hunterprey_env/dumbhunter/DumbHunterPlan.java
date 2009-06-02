package jadex.bdi.examples.hunterprey_env.dumbhunter;

import jadex.adapter.base.envsupport.environment.ISpaceAction;
import jadex.adapter.base.envsupport.environment.ISpaceObject;
import jadex.adapter.base.envsupport.environment.space2d.Grid2D;
import jadex.adapter.base.envsupport.environment.space2d.Space2D;
import jadex.adapter.base.envsupport.math.IVector2;
import jadex.bdi.examples.hunterprey_env.MoveAction;
import jadex.bdi.runtime.Plan;

import java.util.HashMap;
import java.util.Map;

/**
 *  The behavior of the dumb hunter.
 */
public class DumbHunterPlan extends Plan
{
	/**
	 *  Plan body.
	 */
	public void body()
	{
		Grid2D	env	= (Grid2D)getBeliefbase().getBelief("env").getFact();
		ISpaceObject	myself	= (ISpaceObject)getBeliefbase().getBelief("myself").getFact();
		String	lastdir	= null;
		
		while(true)
		{
//			System.out.println("nearest prey for: "+getAgentName()+", "+getBeliefbase().getBelief("nearest_prey").getFact());
			
			// Get current position.
			IVector2	pos	= (IVector2)myself.getProperty(Space2D.POSITION);
			
			ISpaceObject	prey	= (ISpaceObject)getBeliefbase().getBelief("nearest_prey").getFact();
			if(prey!=null && pos.equals(prey.getProperty(Space2D.POSITION)))
			{
				// Perform eat action.
				try
				{
					SyncResultListener srl	= new SyncResultListener();
					Map params = new HashMap();
					params.put(ISpaceAction.ACTOR_ID, getAgentIdentifier());
					params.put(ISpaceAction.OBJECT_ID, prey);
					env.performSpaceAction("eat", params, srl);
					srl.waitForResult();
				}
				catch(RuntimeException e)
				{
//					System.out.println("Eat failed: "+e);
				}
			}

			else
			{
				// Move towards the prey, if any
				if(prey!=null)
				{
					String	newdir	= MoveAction.getDirection(env, pos, (IVector2)prey.getProperty(Space2D.POSITION));
					if(!MoveAction.DIRECTION_NONE.equals(newdir))
					{
						lastdir	= newdir;
					}
					else
					{
						// Prey unreachable.
						getBeliefbase().getBelief("nearest_prey").setFact(null);						
					}
				}
				
				// When no prey, turn 90� with probability 0.25, otherwise continue moving in same direction.
				else if(lastdir==null || Math.random()>0.75)
				{
					if(MoveAction.DIRECTION_LEFT.equals(lastdir) || MoveAction.DIRECTION_RIGHT.equals(lastdir))
					{
						lastdir	= Math.random()>0.5 ? MoveAction.DIRECTION_UP : MoveAction.DIRECTION_DOWN;
					}
					else
					{
						lastdir	= Math.random()>0.5 ? MoveAction.DIRECTION_LEFT : MoveAction.DIRECTION_RIGHT;
					}
				}
				
				// Perform move action.
				try
				{
					SyncResultListener srl	= new SyncResultListener();
					Map params = new HashMap();
					params.put(ISpaceAction.ACTOR_ID, getAgentIdentifier());
					params.put(MoveAction.PARAMETER_DIRECTION, lastdir);
					env.performSpaceAction("move", params, srl);
					srl.waitForResult();
				}
				catch(RuntimeException e)
				{
					// Move failed, forget about prey and turn 90�.
					getBeliefbase().getBelief("nearest_prey").setFact(null);
					
//					System.out.println("Move failed: "+e);
					if(MoveAction.DIRECTION_LEFT.equals(lastdir) || MoveAction.DIRECTION_RIGHT.equals(lastdir))
					{
						lastdir	= Math.random()>0.5 ? MoveAction.DIRECTION_UP : MoveAction.DIRECTION_DOWN;
					}
					else
					{
						lastdir	= Math.random()>0.5 ? MoveAction.DIRECTION_LEFT : MoveAction.DIRECTION_RIGHT;
					}
				}
			}
		}
	}
}
