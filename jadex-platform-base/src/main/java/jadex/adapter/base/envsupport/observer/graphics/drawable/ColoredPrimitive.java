package jadex.adapter.base.envsupport.observer.graphics.drawable;


import java.awt.Color;


public abstract class ColoredPrimitive extends RotatingPrimitive
{
	/** Color of the primitive. */
	protected Color		c_;

	/** OpenGL color cache. */
	protected float[]	oglColor_;
	
	/**
	 * Initializes the drawable.
	 */
	protected ColoredPrimitive()
	{
		super();
		setColor(Color.WHITE);
	}

	/**
	 * Initializes the drawable.
	 * 
	 * @param position position or position-binding
	 * @param rotation rotation or rotation-binding
	 * @param size size or size-binding
	 * @param c the drawable's color
	 */
	protected ColoredPrimitive(Object position, Object rotation, Object size, Color c, DrawCondition drawcondition)
	{
		super(position, rotation, size, drawcondition);
		if (c == null)
			c = Color.WHITE;
		setColor(c);
	}

	/**
	 * Gets the color of the drawable
	 * 
	 * @return color of the drawable
	 */
	public Color getColor()
	{
		return c_;
	}

	/**
	 * Sets a new color for the drawable
	 * 
	 * @param c new color
	 */
	public void setColor(Color c)
	{
		c_ = c;
		oglColor_ = new float[4];
		oglColor_[0] = c_.getRed() / 255.0f;
		oglColor_[1] = c_.getGreen() / 255.0f;
		oglColor_[2] = c_.getBlue() / 255.0f;
		oglColor_[3] = c_.getAlpha() / 255.0f;
	}
}
