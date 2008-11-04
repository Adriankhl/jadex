package jadex.rules.rulesystem.rete.extractors;

import jadex.rules.rulesystem.rete.Tuple;
import jadex.rules.rulesystem.rete.nodes.VirtualFact;
import jadex.rules.state.IOAVState;
import jadex.rules.state.OAVAttributeType;
import jadex.rules.state.OAVJavaAttributeType;

import java.lang.reflect.Method;

/**
 *  Extractor for fetching a Java value from a rete tuple.
 */
public class JavaTupleExtractor extends TupleExtractor
{
	//-------- constructors --------
	
	/**
	 *  Create a new extractor.
	 */
	public JavaTupleExtractor(int tupleindex, OAVAttributeType attr)
	{
		super(tupleindex, attr);
	}
	
	//-------- methods --------
	
	/**
	 *  Get the value of an attribute from an object or tuple.
	 * @param left The left input tuple. 
	 * @param right The right input object.
	 * @param state The working memory.
	 */
	public Object getValue(Tuple left, Object right, IOAVState state)
	{
		// Fetch the object from the tuple

		// a) attr == null -> use object
		// b) attr !=null -> use object.getXYZ()
		
		Object object = left.getObject(tupleindex);
		
		if(object instanceof VirtualFact)
			object = ((VirtualFact)object).getObject();
				
		if(attr!=null)
		{
			Method rm = ((OAVJavaAttributeType)attr).getPropertyDescriptor()
				.getReadMethod();
			if(rm==null)
				throw new RuntimeException("No attribute accessor found: "+attr);
			try
			{
				object = rm.invoke(object, new Object[0]);
			}
			catch(Exception e)
			{
				throw new RuntimeException(e);
			}
		}
		
		return object;
	}
	
	/**
	 *  Get the string representation.
	 *  @return The string representation. 
	 */
	public String toString()
	{
		return "[java]"+"["+tupleindex+"]"+"."+(attr==null? "object": attr.getName());
	}
}
