package jadex.simulation.analysis.common.data.parameter;

import java.util.LinkedList;
import java.util.List;

import javax.activation.UnsupportedDataTypeException;

import jadex.simulation.analysis.common.data.ADataObject;
import jadex.simulation.analysis.common.data.IADataView;
import jadex.simulation.analysis.common.data.parameter.statistics.ISingleStatistic;
import jadex.simulation.analysis.common.data.parameter.statistics.Max;
import jadex.simulation.analysis.common.data.parameter.statistics.Mean;
import jadex.simulation.analysis.common.data.parameter.statistics.Min;
import jadex.simulation.analysis.common.data.parameter.statistics.Sum;
import jadex.simulation.analysis.common.data.parameter.statistics.Variance;

public class ASummaryParameter extends ABasicParameter implements IAMultiValueParameter
{
	public ASummaryParameter(String name)
	{
		super(name, Double.class, null);
		synchronized (mutex)
		{
			view = new ASummaryParameterView(this);
		}
	}

	/** number of values */
	protected Double n = 0.0;

	/** Minimum statistic */
	private ISingleStatistic min = new Min();

	/** Maximum statistic */
	private ISingleStatistic max = new Max();

	/** Mean statistic */
	private ISingleStatistic mean = new Mean();

	/** Sum statistic */
	private ISingleStatistic sum = new Sum();

	/** Variance statistic */
	private ISingleStatistic variance = new Variance();
	
	private List<Double> values = new LinkedList<Double>();

	@Override
	public void setValue(Object value)
	{
		synchronized (mutex)
		{
			if (value instanceof Double)
			{
				addValue((Double) value);
			}
			else
			{
//				new UnsupportedDataTypeException(value.getClass() + "not supported. Summary only support Doubles");
			}
		}
	}

	@Override
	public Object getValue()
	{
		return getMean();
	}

//	@Override
//	public Class getValueClass()
//	{
//		return Double.class;
//	}

	/*
	 * (non-Javadoc)
	 * @see jadex.simulation.analysis.common.data.parameter.IAMultiValueParameter#addValue(java.lang.Double)
	 */
	@Override
	public void addValue(Double value)
	{
		synchronized (mutex)
		{
			sum.addValue(value);
			min.addValue(value);
			max.addValue(value);
			mean.addValue(value);
			variance.addValue(value);
			values.add(value);
			n++;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see jadex.simulation.analysis.common.data.parameter.IAMultiValueParameter#addValues(java.lang.Double[])
	 */
	@Override
	public void addValues(Double[] values)
	{
		synchronized (mutex)
		{
			for (double d : values)
			{
				addValue(d);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see jadex.simulation.analysis.common.data.parameter.IAMultiValueParameter#getN()
	 */
	@Override
	public Double getN()
	{
		return n;
	}

	/*
	 * (non-Javadoc)
	 * @see jadex.simulation.analysis.common.data.parameter.IAMultiValueParameter#getSum()
	 */
	@Override
	public Double getSum()
	{
		return sum.getResult();
	}

	/*
	 * (non-Javadoc)
	 * @see jadex.simulation.analysis.common.data.parameter.IAMultiValueParameter#getMean()
	 */
	@Override
	public Double getMean()
	{
//		System.out.println(mean);
//		System.out.println(mean.getResult());
		if (mean != null)
		{
			return mean.getResult();
		} else
		{
			return Double.NaN;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see jadex.simulation.analysis.common.data.parameter.IAMultiValueParameter#getStandardDeviation()
	 */
	@Override
	public Double getStandardDeviation()
	{
		synchronized (mutex)
		{
			double stdDev = Double.NaN;
			if (getN() > 0)
			{
				if (getN() > 1)
				{
					stdDev = Math.sqrt(getVariance());
				}
				else
				{
					stdDev = 0.0;
				}
			}
			return stdDev;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see jadex.simulation.analysis.common.data.parameter.IAMultiValueParameter#getVariance()
	 */
	@Override
	public Double getVariance()
	{
		return variance.getResult();
	}

	/*
	 * (non-Javadoc)
	 * @see jadex.simulation.analysis.common.data.parameter.IAMultiValueParameter#getMax()
	 */
	@Override
	public Double getMax()
	{
		return max.getResult();
	}

	/*
	 * (non-Javadoc)
	 * @see jadex.simulation.analysis.common.data.parameter.IAMultiValueParameter#getMin()
	 */
	@Override
	public Double getMin()
	{
		return min.getResult();
	}

	/*
	 * (non-Javadoc)
	 * @see jadex.simulation.analysis.common.data.parameter.IAMultiValueParameter#isEmpty()
	 */
	@Override
	public boolean isEmpty()
	{
		if (n <= 0)
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see jadex.simulation.analysis.common.data.parameter.IAMultiValueParameter#clear()
	 */
	@Override
	public void clear()
	{
		synchronized (mutex)
		{
			this.n = 0.0;
			min.clear();
			max.clear();
			sum.clear();
			mean.clear();
			variance.clear();
		}
	}

	@Override
	public List<Double> getValues()
	{
		return values;
	}
	
	@Override
	public ADataObject clonen()
	{
		ASummaryParameter clone = new ASummaryParameter(name);
		Boolean oValue = onlyValue;
		clone.setEditable(editable);
		clone.setValueEditable(oValue);
		for (int i = 0; i < values.toArray().length; i++)
		{
			clone.addValues((Double[]) values.toArray());
		}

		return clone;
	}
}
