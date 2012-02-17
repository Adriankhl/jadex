package jadex.commons.future;


import java.util.logging.Logger;

/**
 *  The default listener for just printing out result information.
 *  Is used as fallback when no other listener is available.
 */
public abstract class DefaultResultListener<E> implements IResultListener<E>
{
	//-------- attributes --------
	
	/** The logger. */
	private Logger logger;	// Private to prevent accidental use.
	
//	/** The static instance. */
//	private static IResultListener instance;
	
	//-------- constructors --------
	
	/**
	 *  Create a new listener.
	 *  @param logger The logger.
	 */
	public DefaultResultListener()
	{
		this.logger = Logger.getLogger("default-result-listener");
	}
	
	/**
	 *  Create a new listener.
	 *  @param logger The logger.
	 */
	public DefaultResultListener(Logger logger)
	{
		this.logger = logger;
	}
	
//	/**
//	 *  Get the listener instance.
//	 *  @return The listener.
//	 */
//	public static IResultListener getInstance()
//	{
//		// Hack! Implement that logger can be passed
//		if(instance==null)
//		{
//			instance = new DefaultResultListener()
//			{
//				public void resultAvailable(Object result)
//				{
//				}
//			};
//		}
//		return instance;
//	}
	
	//-------- methods --------
	
	/**
	 *  Called when the result is available.
	 *  @param result The result.
	 * /
	public void resultAvailable(Object result)
	{
		//logger.info(""+result);
	}*/
	
	/**
	 *  Called when an exception occurred.
	 *  @param exception The exception.
	 */
	public void exceptionOccurred(Exception exception)
	{
		exception.printStackTrace();
		logger.severe("Exception occurred: "+exception);
	}
}
