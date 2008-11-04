package jadex.rules.rulesystem.rules;

/**
 *  Interface for all operators.
 */
public interface IOperator
{
	//-------- constants --------
	
	/** The equal operator. */
	public static IOperator EQUAL = new Operator.Equal();
	
	/** The not equal operator. */
	public static IOperator NOTEQUAL = new Operator.NotEqual();
	
	/** The less operator. */
	public static IOperator LESS = new Operator.Less();
	
	/** The less or equal operator. */
	public static IOperator LESSOREQUAL = new Operator.LessOrEqual();
	
	/** The greater operator. */
	public static IOperator GREATER = new Operator.Greater();
	
	/** The greater or equal operator. */
	public static IOperator GREATEROREQUAL = new Operator.GreaterOrEqual();
	
	/** The contains operator. */
	public static IOperator CONTAINS = new Operator.Contains();
	
	/** The excludes operator. */
	public static IOperator EXCLUDES = new Operator.Excludes();
	
	//-------- string operators --------
	
	/** The matches operator. */
	public static IOperator MATCHES = new Operator.Matches();
	
	/** The starts with operator. */
	public static IOperator STARTSWITH = new Operator.StartsWith();
	
	//-------- methods --------
	
	/**
	 *  Evaluate two objects with respect to the
	 *  operator semantics.
	 *  @param a The first object.
	 *  @param b The second object.
	 *  @return True, if objects fit wrt. the operator semantics.
	 */
	public boolean evaluate(Object a, Object b);
}
