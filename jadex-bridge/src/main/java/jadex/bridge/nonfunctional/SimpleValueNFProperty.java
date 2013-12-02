package jadex.bridge.nonfunctional;

import jadex.bridge.IInternalAccess;
import jadex.bridge.sensor.unit.IConvertableUnit;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.commons.future.IResultListener;

/**
 * 
 */
public abstract class SimpleValueNFProperty<T, U> extends AbstractNFProperty<T, U>
{
	/** The current value. */
	protected T value;
	
	/** The component. */
	protected IInternalAccess comp;
	
	/**
	 *  Create a new property.
	 */
	public SimpleValueNFProperty(final IInternalAccess comp, final NFPropertyMetaInfo mi)
	{
		super(mi);
		this.comp = comp;
		
		if(mi.isDynamic() && mi.getUpdateRate()>0)
		{
			setValue(measureValue());
			IResultListener<Void> res = new IResultListener<Void>()
			{
				public void resultAvailable(Void result)
				{
					setValue(measureValue());
					comp.waitForDelay(mi.getUpdateRate(), mi.isRealtime()).addResultListener(this);
				}
				
				public void exceptionOccurred(Exception exception)
				{
				}
			};
			
			comp.waitForDelay(mi.getUpdateRate(), mi.isRealtime()).addResultListener(res);
		}
		else
		{
			setValue(measureValue());
		}
	}

	/**
	 *  Get the value.
	 */
	public IFuture<T> getValue(U unit)
	{
		T ret = value;
		if(unit instanceof IConvertableUnit)
			ret = ((IConvertableUnit<T>)unit).convert(ret);
		return new Future<T>(ret);
	}
	
	/**
	 *  Set the value.
	 *  @param value The value to set.
	 */
	public void setValue(T value)
	{
		this.value = value;
	}
	
	/**
	 *  Measure the value.
	 */
	public abstract T measureValue();

	/**
	 *  Get the component.
	 *  @return The component.
	 */
	public IInternalAccess getComponent()
	{
		return comp;
	}
}
