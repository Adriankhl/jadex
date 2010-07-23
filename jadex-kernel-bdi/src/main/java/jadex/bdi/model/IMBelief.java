package jadex.bdi.model;


/**
 *  Interface for belief model.
 */
public interface IMBelief extends IMTypedElement
{
	/**
	 *  Get the clazz.
	 *  @return The clazz. 
	 */
//	public IParsedExpression getFactExpression();
	
	/**
	 *  Test if the belief is used as argument.
	 *  @return True if used as argument. 
	 */
	public boolean isArgument();
	
	/**
	 *  Test if the belief is used as result.
	 *  @return True if used as result. 
	 */
	public boolean isResult();

}
