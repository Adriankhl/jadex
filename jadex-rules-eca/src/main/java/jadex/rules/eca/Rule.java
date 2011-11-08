package jadex.rules.eca;

import java.util.List;

/**
 *  Implementation of a rule.
 *  Has a 
 *  - name
 *  - event types it reacts to
 *  - condition, lhs of the rule
 *  - action, rhs of the rule
 */
public class Rule implements IRule
{
	//-------- attributes --------
	
	/** The rule name. */
	protected String name;
	
	/** The event types. */
	protected List<String> events;
	
	/** The condition. */
	protected ICondition condition;
	
	/** The action. */
	protected IAction action;

	//-------- constructors --------

	/**
	 * 
	 */
	public Rule(String name)
	{
		this(name, null, null);
	}
	
	/**
	 * 
	 */
	public Rule(String name, ICondition condition)
	{
		this(name, condition, null);
	}
	
	/**
	 * 
	 */
	public Rule(String name, ICondition condition, IAction action)
	{
		this.name = name;
		this.condition = condition;
		this.action = action;
	}

	//-------- methods --------
	
	/**
	 *  Get the rule name.
	 *  @return The rule name.
	 */
	public String getName()
	{
		return name;
	}
	
	/**
	 *  Get the condition.
	 *  @return The condition.
	 */
	public ICondition getCondition()
	{
		return condition;
	}

	/**
	 *  Set the condition.
	 *  @param condition The condition to set.
	 */
	public void setCondition(ICondition condition)
	{
		this.condition = condition;
	}

	/**
	 *  Get the action.
	 *  @return The action.
	 */
	public IAction getAction()
	{
		return action;
	}

	/**
	 *  Set the action.
	 *  @param action The action to set.
	 */
	public void setAction(IAction action)
	{
		this.action = action;
	}
	
	/**
	 *  Set the event types.
	 *  @param events The event types.
	 */
	public void setEvents(List<String> events)
	{
		this.events = events;
	}
	
	/**
	 *  Get the event types.
	 *  @retur The event types.
	 */
	public List<String> getEvents()
	{
		return events;
	}

}
