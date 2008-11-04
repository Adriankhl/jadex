package jadex.bdi.runtime.impl;

import jadex.bdi.interpreter.BDIInterpreter;
import jadex.bdi.interpreter.OAVBDIRuntimeModel;
import jadex.bdi.runtime.IGoal;
import jadex.bdi.runtime.IWaitAbstraction;
import jadex.bdi.runtime.IWaitqueue;
import jadex.commons.Tuple;
import jadex.rules.state.IOAVState;
import jadex.rules.state.OAVObjectType;

import java.util.Collection;
import java.util.Iterator;

/**
 *  Flyweight for a waitqueue.
 */
public class WaitqueueFlyweight extends WaitAbstractionFlyweight implements IWaitqueue
{
	//-------- attributes --------
	
	/** The plan. */
	protected Object	rplan;
	
	//-------- constructors --------
	
	/**
	 *  Create a new element flyweight.
	 *  @param state	The state.
	 *  @param scope	The scope handle.
	 *  @param handle	The element handle.
	 */
	private WaitqueueFlyweight(IOAVState state, Object scope, Object rplan)
	{
		super(state, scope, state.getAttributeValue(rplan, OAVBDIRuntimeModel.plan_has_waitqueuewa));
		// Hack!! Super constructor creates wa, when null. 
		if(state.getAttributeValue(rplan, OAVBDIRuntimeModel.plan_has_waitqueuewa)==null)
			state.setAttributeValue(rplan, OAVBDIRuntimeModel.plan_has_waitqueuewa, getHandle());
		this.rplan	= rplan;
		state.addExternalObjectUsage(rplan, this);
	}
	
	/**
	 *  Get or create a flyweight.
	 *  @return The flyweight.
	 */
	public static WaitqueueFlyweight getWaitqueueFlyweight(IOAVState state, Object scope, Object rplan)
	{
		Tuple	key	= new Tuple(rplan, IWaitqueue.class);
		BDIInterpreter ip = BDIInterpreter.getInterpreter(state);
		WaitqueueFlyweight ret = (WaitqueueFlyweight)ip.getFlyweightCache(IWaitqueue.class).get(key);
		if(ret==null)
		{
			ret = new WaitqueueFlyweight(state, scope, rplan);
			ip.getFlyweightCache(IWaitqueue.class).put(key, ret);
		}
		return ret;
	}
	
	/**
	 *  Actual cleanup code.
	 *  When overriding this method, super.doCleanup() has to be called. 
	 */
	protected void	doCleanup()
	{
		if(rplan!=null)
		{
			getState().removeExternalObjectUsage(rplan, this);
			rplan	= null;
		}
		super.doCleanup();
	}
	
	//-------- waitqueue methods --------
	
	/**
	 *  Get all elements.
	 *  @return The elements.
	 */
	public Object[] getElements()
	{
		if(getInterpreter().isExternalThread())
		{
			AgentInvocation invoc = new AgentInvocation()
			{
				public void run()
				{
					Collection coll = getState().getAttributeValues(rplan, OAVBDIRuntimeModel.plan_has_waitqueueelements);
					if(coll!=null)
					{
						oarray = new Object[coll.size()];
						int i=0;
						for(Iterator it=coll.iterator(); it.hasNext(); i++)
						{
							// todo: wrong scope!
							oarray[i] = getFlyweight(getState(), getScope(), it.next());
						}
					}
					else
					{
						oarray = new Object[0];
					}
				}
			};
			return invoc.oarray;
		}
		else
		{
			Object[] ret;
			Collection coll = getState().getAttributeValues(rplan, OAVBDIRuntimeModel.plan_has_waitqueueelements);
			if(coll!=null)
			{
				ret = new Object[coll.size()];
				int i=0;
				for(Iterator it=coll.iterator(); it.hasNext(); i++)
				{
					// todo: wrong scope!
					ret[i] = getFlyweight(getState(), getScope(), it.next());
				}
			}
			else
			{
				ret = new Object[0];
			}
			return ret;
		}
	}

	/**
	 *  Get the next element.
	 *  @return The next element (or null if none).
	 */
	public Object removeNextElement()
	{
		if(getInterpreter().isExternalThread())
		{
			AgentInvocation invoc = new AgentInvocation()
			{
				public void run()
				{
					Collection coll = getState().getAttributeValues(rplan, OAVBDIRuntimeModel.plan_has_waitqueueelements);
					if(coll!=null && coll.iterator().hasNext())
					{
						Object pe = coll.iterator().next();
						getState().removeAttributeValue(rplan, OAVBDIRuntimeModel.plan_has_waitqueueelements, pe);
						// todo: wrong scope!
						object = getFlyweight(getState(), getScope(), pe);
					}
				}
			};
			return invoc.object;
		}
		else
		{
			Object ret = null;
			Collection coll = getState().getAttributeValues(rplan, OAVBDIRuntimeModel.plan_has_waitqueueelements);
			if(coll!=null && coll.iterator().hasNext())
			{
				Object pe = coll.iterator().next();
				getState().removeAttributeValue(rplan, OAVBDIRuntimeModel.plan_has_waitqueueelements, pe);
				// todo: wrong scope!
				ret = getFlyweight(getState(), getScope(), pe);
			}
			return ret;
		}
	}

	/**
	 *  Remove an element.
	 */
	public void removeElement(final Object element)
	{
		if(getInterpreter().isExternalThread())
		{
			new AgentInvocation()
			{
				public void run()
				{
					getState().removeAttributeValue(rplan, OAVBDIRuntimeModel.plan_has_waitqueueelements, ((ElementFlyweight)element).getHandle());
				}
			};
		}
		else
		{
			getState().removeAttributeValue(rplan, OAVBDIRuntimeModel.plan_has_waitqueueelements, ((ElementFlyweight)element).getHandle());
		}
	}

	/**
	 *  Add a Goal. Overrides method for checking if rgoal is already finished.
	 *  @param goal The goal.
	 */
	public IWaitAbstraction addGoal(final IGoal goal)
	{
		if(getInterpreter().isExternalThread())
		{
			new AgentInvocation()
			{
				public void run()
				{
					Object rgoal  = ((ElementFlyweight)goal).getHandle();
					// Directly add rgoal to waitqueue if already finished.
					if(goal.isFinished())
					{
						getState().addAttributeValue(rplan, OAVBDIRuntimeModel.plan_has_waitqueueelements, rgoal);
					}
					else
					{
						addGoal(getOrCreateWaitAbstraction(), goal, getState(), getScope());
					}
				}
			};
			return this;		
		}
		else
		{
			Object rgoal  = ((ElementFlyweight)goal).getHandle();
			// Directly add rgoal to waitqueue if already finished.
			if(goal.isFinished())
			{
				getState().addAttributeValue(rplan, OAVBDIRuntimeModel.plan_has_waitqueueelements, rgoal);
			}
			else
			{
				addGoal(getOrCreateWaitAbstraction(), goal, getState(), getScope());
			}
			return this;
		}
	}
	
	//-------- other methods --------
	
	/**
	 *  Get the number of events in the waitqueue.
	 *  @return The size of the waitqueue.
	 */
	public int	size()
	{
		if(getInterpreter().isExternalThread())
		{
			AgentInvocation invoc = new AgentInvocation()
			{
				public void run()
				{
					Collection coll = getState().getAttributeValues(rplan, OAVBDIRuntimeModel.plan_has_waitqueueelements);
					integer = coll==null? 0: coll.size();
				}
			};
			return invoc.integer;
		}
		else
		{
			Collection coll = getState().getAttributeValues(rplan, OAVBDIRuntimeModel.plan_has_waitqueueelements);
			return coll==null? 0: coll.size();
		}
	}

	/**
	 *  Test if the waitqueue is empty.
	 *  @return True, if empty.
	 */
	public boolean	isEmpty()
	{
		if(getInterpreter().isExternalThread())
		{
			AgentInvocation invoc = new AgentInvocation()
			{
				public void run()
				{
					Collection coll = getState().getAttributeValues(rplan, OAVBDIRuntimeModel.plan_has_waitqueueelements);
					bool = coll==null? true: coll.isEmpty();
				}
			};
			return invoc.bool;
		}
		else
		{
			Collection coll = getState().getAttributeValues(rplan, OAVBDIRuntimeModel.plan_has_waitqueueelements);
			return coll==null? true: coll.isEmpty();
		}
	}
	
	//-------- helpers --------
	
	/**
	 *  Get flyweight for element.
	 *  @param elem The element.
	 *  @return The flyweight.
	 */
	public static ElementFlyweight getFlyweight(IOAVState state, Object rcapa, Object elem)
	{
		ElementFlyweight ret = null;
		OAVObjectType type = state.getType(elem);
		
		if(type.equals(OAVBDIRuntimeModel.goal_type))
		{
			ret = GoalFlyweight.getGoalFlyweight(state, rcapa, elem);
		}
		else if(type.equals(OAVBDIRuntimeModel.internalevent_type))
		{
			ret = InternalEventFlyweight.getInternalFlyweight(state, rcapa, elem);
		}
		else if(type.equals(OAVBDIRuntimeModel.messageevent_type))
		{
			ret = MessageEventFlyweight.getMessageFlyweight(state, rcapa, elem);
		}
		else if(type.isSubtype(OAVBDIRuntimeModel.changeevent_type))
		{
			String cetype = (String)state.getAttributeValue(elem, OAVBDIRuntimeModel.changeevent_has_type);
			if(OAVBDIRuntimeModel.CHANGEEVENT_GOALDROPPED.equals(cetype))
			{
				ret = GoalFlyweight.getGoalFlyweight(state, rcapa, state.getAttributeValue(elem, OAVBDIRuntimeModel.changeevent_has_element));
			}
			else
			{
				ret = new ChangeEventFlyweight(state, rcapa, elem);
			}
		}
		
		return ret;
	}
	
	/**
	 *  Get or create the waitabstraction.
	 *  @return The waitabstraction.
	 */
	protected Object getWaitAbstraction()
	{
		return getState().getAttributeValue(rplan, OAVBDIRuntimeModel.plan_has_waitqueuewa);
	}
	
	/**
	 *  Create the waitabstraction.
	 *  @return The waitabstraction.
	 */
	protected Object createWaitAbstraction()
	{
		Object wa = super.createWaitAbstraction();
		getState().setAttributeValue(rplan, OAVBDIRuntimeModel.plan_has_waitqueuewa, wa);
		return wa;
	}
	
}
