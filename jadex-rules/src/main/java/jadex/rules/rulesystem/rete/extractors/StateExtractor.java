package jadex.rules.rulesystem.rete.extractors;

import jadex.rules.rulesystem.rete.Tuple;
import jadex.rules.state.IOAVState;
import jadex.rules.state.OAVAttributeType;

import java.util.Collections;
import java.util.Set;

/**
 *  The state extractor returns the current state as value.
 *  Is the extractor for the reserved variable Variable.STATE ($state). 
 */
public class StateExtractor implements IValueExtractor
{
	//-------- methods --------
	
	/**
	 *  Get the value of an attribute from an object or tuple.
	 * @param left The left input tuple. 
	 * @param right The right input object.
	 * @param state The working memory.
	 */
	public Object getValue(Tuple left, Object right, IOAVState state)
	{
		return state;
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
		return false;
	}
	
	/**
	 *  Get the string representation.
	 *  @return The string representation. 
	 */
	public String toString()
	{
		return "state";
	}

	/**
	 *  Get the set of relevant attribute types.
	 */
	public Set	getRelevantAttributes()
	{
		return Collections.EMPTY_SET;
	}
}

