package jadex.bridge.nonfunctional.hardconstraints;

import jadex.bridge.component.INFPropertyComponentFeature;
import jadex.bridge.nonfunctional.INFMixedPropertyProvider;
import jadex.bridge.sensor.service.ExecutionTimeProperty;
import jadex.bridge.service.IService;
import jadex.commons.IAsyncFilter;
import jadex.commons.future.DelegationResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.commons.future.IResultListener;

/**
 * 
 */
public abstract class AbstractConstraintFilter<T> implements IAsyncFilter<T>
{
	/** Name of the property being kept constant. */
	protected String propname;
	
	/** The value once it is bound. */
	protected Object value;
	
	/**
	 *  Creates a constant value filter.
	 */
	public AbstractConstraintFilter()
	{
	}
	
	/**
	 *  Creates a constant value filter.
	 */
	public AbstractConstraintFilter(String propname, Object value)
	{
		this.propname = propname;
		this.value = value;
	}
	
	/**
	 *  Test if an object passes the filter.
	 *  @return True, if passes the filter.
	 */
	public final IFuture<Boolean> filter(final T service)
	{
		if (getValue() == null)
		{
			return new Future<Boolean>(true);
		}
		
		final Future<Boolean> ret = new Future<Boolean>();
		INFMixedPropertyProvider prov = ((INFMixedPropertyProvider)((IService)service).getExternalComponentFeature(INFPropertyComponentFeature.class));
//		((IService)service).getNFPropertyValue(propname).addResultListener(new IResultListener<Object>()
		prov.getNFPropertyValue(propname).addResultListener(new IResultListener<Object>()
		{
			public void resultAvailable(Object result)
			{
				doFilter((IService) service, result).addResultListener(new DelegationResultListener<Boolean>(ret));
			}
			
			public void exceptionOccurred(Exception exception)
			{
				ret.setException(exception);
			}
		});
		return ret;
	}
	
	/**
	 *  Test if an object passes the filter.
	 *  @return True, if passes the filter.
	 */
	public abstract IFuture<Boolean> doFilter(IService service, Object value);

	/**
	 *  Gets the valuename.
	 *drag edge areadrag edge area
	 *  @return The valuename.
	 */
	public String getValueName()
	{
		return propname;
	}

	/**
	 *  Sets the valuename.
	 *
	 *  @param valuename The valuename to set.
	 */
	public void setValueName(String valuename)
	{
		this.propname = valuename;
	}

	/**
	 *  Gets the value.
	 *
	 *  @return The value.
	 */
	public Object getValue()
	{
		return value;
	}

	/**
	 *  Sets the value.
	 *
	 *  @param value The value to set.
	 */
	public void setValue(Object value)
	{
		this.value = value;
	}
}
