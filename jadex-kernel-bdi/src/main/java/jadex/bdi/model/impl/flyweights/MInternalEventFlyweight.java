package jadex.bdi.model.impl.flyweights;

import jadex.bdi.model.IMInternalEvent;
import jadex.rules.state.IOAVState;


public class MInternalEventFlyweight extends MProcessableElementFlyweight implements IMInternalEvent
{
	//-------- constructors --------
	
	/**
	 *  Create a new internal event flyweight.
	 */
	public MInternalEventFlyweight(IOAVState state, Object scope, Object handle)
	{
		super(state, scope, handle);
	}
}
