package jadex.simulation.analysis.common.data.parameter.statistics;

/**
 * Code was created by the Author within a Simulation Project at the University of Hamburg
 * Maximal Value
 * @author 5Haubeck
 *
 */
public class Max extends AbstractSingleStatistic
{

	/** Number of values */
	private Double n;

	/** Current value (min) */
	private double value;

	/**
	 * Construct a Min
	 */
	public Max()
	{
		n = 0.0;
		value = Double.NaN;
	}

	@Override
	public synchronized void addValue(final double d)
	{
		if (d > value || Double.isNaN(value))
		{
			value = d;
		}
		n++;
	}

	@Override
	public synchronized void clear()
	{
		value = Double.NaN;
		n = 0.0;
	}

	@Override
	public synchronized Double getResult()
	{
		return new Double(value);
	}

	@Override
	public synchronized Double getN()
	{
		return n;
	}
	
	@Override
	public synchronized String toString()
	{
		return "Max(" + n + " , "+ value +")";
	}
}
