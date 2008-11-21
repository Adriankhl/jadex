package jadex.bdi.planlib.simsupport.common.math;

import java.math.BigDecimal;

public class Vector1Long implements IVector1
{
	private long x_;
	
	/** Creates a new Vector1Long.
	 *  
	 *  @param value vector value
	 */
	public Vector1Long(long value)
	{
		x_ = value;
	}
	
	/** Adds another vector to this vector, adding individual components.
	 *
	 *  @param vector the vector to add to this vector
	 *  @return a reference to the called vector (NOT a copy)
	 */
	public IVector1 add(IVector1 vector)
	{
		x_ += vector.getAsLong();
		return this;
	}
	
	/** Subtracts another vector to this vector, subtracting individual components.
	 *
	 *  @param vector the vector to subtract from this vector
	 *  @return a reference to the called vector (NOT a copy)
	 */
	public IVector1 subtract(IVector1 vector)
	{
		x_ -= vector.getAsLong();
		return this;
	}
	
	/** Performs a multiplication on the vector.
	 *
	 *  @param vector vector
	 *  @return a reference to the called vector (NOT a copy)
	 */
	public IVector1 multiply(IVector1 vector)
	{
		x_ *= vector.getAsLong();
		return this;
	}
	
	/** Sets the vector component to zero.
	 *
	 *  @return a reference to the called vector (NOT a copy)
	 */
	public IVector1 zero()
	{
		x_ = 0;
		return this;
	}
	
	/** Negates the vector by negating its components.
	 *
	 *  @return a reference to the called vector (NOT a copy)
	 */
	public IVector1 negate()
	{
		x_ = -x_;
		return this;
	}
	
	/** Returns the distance to another vector
	 *
	 *  @param vector other vector 
	 *  @return distance
	 */
	public IVector1 getDistance(IVector1 vector)
	{
		long distance = Math.abs(x_) - Math.abs(vector.getAsLong());
		return new Vector1Long(distance);
	}
	
	/** Returns the vector as integer.
	 *
	 *  @return vector as integer
	 */
	public int getAsInteger()
	{
		return (int) x_;
	}
	
	/** Returns the vector as long.
	 *
	 *  @return vector as long
	 */
	public long getAsLong()
	{
		return x_;
	}
	
	/** Returns the vector as float.
	 *
	 *  @return vector as float
	 */
	public float getAsFloat()
	{
		return (float) x_;
	}
	
	/** Returns the vector as double.
	 *
	 *  @return vector as double
	 */
	public double getAsDouble()
	{
		return (double) x_;
	}
	
	/** Returns the vector as BigDecimal.
	 *
	 *  @return vector as BigDecimal
	 */
	public BigDecimal getAsBigDecimal()
	{
		return new BigDecimal(x_);
	}

	/** Makes a copy of the vector without using the complex clone interface.
	 *
	 *  @return copy of the vector
	 */
	public IVector1 copy()
	{
		return new Vector1Long(x_);
	}
	
	/** Generates a deep clone of the vector.
	 *
	 *  @return clone of this vector
	 */
	public Object clone() throws CloneNotSupportedException
	{
		return copy();
	}
	
	/** Compares the vector to an object
	 * 
	 * @param obj the object
	 * @return always returns false unless the object is an IVector2,
	 *         in which case it is equivalent to equals(IVector vector)
	 */
	public boolean equals(Object obj)
	{
		if (obj instanceof IVector1)
		{
			return equals((IVector1) obj);
		}
		return false;
	}
	
	/** Compares the vector to another vector.
	 *  The vectors are equal if the components are equal.
	 * 
	 * @param vector the other vector
	 * @return true if the vectors are equal
	 */
	public boolean equals(IVector1 vector)
	{
		return (x_ == vector.getAsLong());
	}
	
	/** Tests if the vector is less than another vector.
	 * 
	 * @param vector the other vector
	 * @return true if the vector is less than the given vector.
	 */
	public boolean less(IVector1 vector)
	{
		return (x_ < vector.getAsLong());
	}

}
