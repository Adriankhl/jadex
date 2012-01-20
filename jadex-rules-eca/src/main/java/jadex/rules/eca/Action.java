package jadex.rules.eca;

import jadex.commons.IValueFetcher;

import java.lang.reflect.Method;

public class Action implements IAction
{
	/** The object. */
	protected Object object;
	
	/** The method. */
	protected Method method;
	
	/**
	 * 
	 */
	public Action(Object object, Method method)
	{
		this.object = object;
		this.method = method;
	}

	/**
	 * 
	 */
	public void execute(IEvent event, Object context)
	{
		try
		{
			method.setAccessible(true);
			Object result = method.invoke(object, new Object[]{event, context});
		}
		catch(Exception e)
		{
			throw new RuntimeException(e);
		}
	}
}
