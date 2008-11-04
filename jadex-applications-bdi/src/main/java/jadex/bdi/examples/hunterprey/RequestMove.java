/*
 * RequestMove.java Generated by Protege plugin Beanynizer. Changes will be lost!
 */
package jadex.bdi.examples.hunterprey;

import jadex.adapter.base.fipa.IAgentAction;


/**
 *  Java class for concept RequestMove of hunterprey_beans ontology.
 */
public class RequestMove implements IAgentAction
{
	//-------- constants ----------

	/** Predefined value "up" for slot direction. */
	public static String DIRECTION_UP = "up";

	/** Predefined value "down" for slot direction. */
	public static String DIRECTION_DOWN = "down";

	/** Predefined value "left" for slot direction. */
	public static String DIRECTION_LEFT = "left";

	/** Predefined value "right" for slot direction. */
	public static String DIRECTION_RIGHT = "right";

	//-------- attributes ----------

	/** The creature. */
	protected Creature creature;

	/** The movement direction. */
	protected String direction;

	//-------- constructors --------

	/**
	 *  Default Constructor. <br>
	 *  Create a new <code>RequestMove</code>.
	 */
	public RequestMove()
	{
	}

	/**
	 *  Init Constructor. <br>
	 *  Create a new RequestMove.<br>
	 *  Initializes the object with required attributes.
	 * @param creature
	 * @param direction
	 */
	public RequestMove(Creature creature, String direction)
	{
		this();
		setCreature(creature);
		setDirection(direction);
	}

	//-------- accessor methods --------

	/**
	 *  Get the creature of this RequestMove.
	 *  The creature.
	 * @return creature
	 */
	public Creature getCreature()
	{
		return this.creature;
	}

	/**
	 *  Set the creature of this RequestMove.
	 *  The creature.
	 * @param creature the value to be set
	 */
	public void setCreature(Creature creature)
	{
		this.creature = creature;
	}

	/**
	 *  Get the direction of this RequestMove.
	 *  The movement direction.
	 * @return direction
	 */
	public String getDirection()
	{
		return this.direction;
	}

	/**
	 *  Set the direction of this RequestMove.
	 *  The movement direction.
	 * @param direction the value to be set
	 */
	public void setDirection(String direction)
	{
		this.direction = direction;
	}

	//-------- object methods --------

	/**
	 *  Get a string representation of this RequestMove.
	 *  @return The string representation.
	 */
	public String toString()
	{
		return "RequestMove(" + "creature=" + getCreature() + ", direction=" + getDirection() + ")";
	}

}
