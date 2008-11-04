package jadex.bdi.examples.hunterprey;


/**
 *  Editable Java class for concept WorldObject of hunterprey ontology.
 */
public class WorldObject
{
	//-------- attributes ----------

	/** The location of the object. */
	protected Location location;

	//-------- constructors --------

	/**
	 *  Create a new WorldObject.
	 */
	public WorldObject()
	{
		// Empty constructor required for JavaBeans (do not remove).
	}

	/**
	 *  Create a new WorldObject.
	 */
	public WorldObject(Location location)
	{
		// Constructor using required slots (change if desired).
		setLocation(location);
	}

	//-------- accessor methods --------

	/**
	 *  Get the location of this WorldObject.
	 *  The location of the object.
	 * @return location
	 */
	public Location getLocation()
	{
		return this.location;
	}

	/**
	 *  Set the location of this WorldObject.
	 *  The location of the object.
	 * @param location the value to be set
	 */
	public void setLocation(Location location)
	{
		this.location = location;
	}

	//-------- custom code --------

	/**
	 *  Get a string representation of this WorldObject.
	 *  @return The string representation.
	 */
	public String toString()
	{
		return "WorldObject(" + "location=" + getLocation() + ")";
	}

	/**
	 *  Test if two worldobjects are equal.
	 */
	public boolean equals(Object o)
	{
		return o.getClass() == this.getClass() && ((WorldObject)o).getLocation().equals(this.getLocation());
	}

	/**
	 *  Get the hash code of the world object.
	 */
	public int hashCode()
	{
		return getClass().hashCode() ^ getLocation().hashCode();
	}
}
