package jadex.rules.rulesystem.rete.constraints;

import jadex.commons.SUtil;
import jadex.rules.rulesystem.rete.Tuple;
import jadex.rules.state.IOAVState;
import jadex.rules.state.OAVAttributeType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 *  A constraint evaluator for and-connected constraints.
 */
public class AndConstraintEvaluator implements IConstraintEvaluator
{
	//-------- attributes --------
	
	/** The constraint evaluator. */
	protected final IConstraintEvaluator[] evaluators;
	
	//-------- constructors --------
	
	/**
	 *  Create an AND constraint evaluator.
	 */
	public AndConstraintEvaluator(IConstraintEvaluator[] evaluators)
	{
		this.evaluators	= evaluators;
	}
	
	//-------- methods --------
	
	/**
	 *  Evaluate the constraints given the right object, left tuple 
	 *  (null for alpha nodes) and the state.
	 *  @param right The right input object.
	 *  @param left The left input tuple. 
	 *  @param state The working memory.
	 */
	public boolean evaluate(Object right, Tuple left, IOAVState state)
	{
		boolean ret = true;
		//IConstraintEvaluator[]	evals	= evaluators;
		for(int i=0; ret && evaluators!=null && i<evaluators.length; i++)
			ret = evaluators[i].evaluate(right, left, state);
		return ret;
	}
	
	/**
	 *  Test if a constraint evaluator is affected from a 
	 *  change of a certain attribute.
	 *  @param tupleindex The tuple index.
	 *  @param attr The attribute.
	 *  @return True, if affected.
	 */
	public boolean isAffected(int tupleindex, OAVAttributeType attr)
	{
		boolean ret = false;
		
		for(int i=0; !ret && i<evaluators.length; i++)
		{
			ret = evaluators[i].isAffected(tupleindex, attr);
		}
		return ret;
	}
	
	/**
	 *  Get the set of relevant attribute types.
	 */
	public Set	getRelevantAttributes()
	{
		Set	ret	= new HashSet();
		for(int i=0; i<evaluators.length; i++)
		{
			ret.addAll(evaluators[i].getRelevantAttributes());
		}
		return ret;
	}

	/**
	 *  Get the string representation.
	 *  @return The string representation. 
	 */
	public String toString()
	{
		StringBuffer ret = new StringBuffer(" and ");
		for(int i=0; evaluators!=null && i<evaluators.length; i++)
			ret.append("(").append(evaluators[i]).append(")");
		return ret.toString();
	}

	/**
	 *  Get the constraint evaluators.
	 */
	public IConstraintEvaluator[] getConstraintEvaluators()
	{
		return evaluators;
	}

	/**
	 *  The hash code.
	 */
	public int hashCode()
	{
		// Arrays.hashCode(Object[]): JDK 1.5
		return 31 + SUtil.arrayHashCode(evaluators);
	}

	/**
	 *  Test for equality.
	 */
	public boolean equals(Object obj)
	{
		if(this==obj)
			return true;

		return (obj instanceof AndConstraintEvaluator)
			&& Arrays.equals(evaluators, ((AndConstraintEvaluator)obj).getConstraintEvaluators());
	}
}

