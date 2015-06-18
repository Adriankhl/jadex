package jadex.bdiv3x.runtime;

import jadex.bdiv3.features.IBDIAgentFeature;
import jadex.bdiv3.features.impl.IInternalBDIAgentFeature;
import jadex.bdiv3.model.MCapability;
import jadex.bdiv3.runtime.impl.RCapability;
import jadex.bdiv3.runtime.impl.RElement;
import jadex.bridge.IInternalAccess;
import jadex.bridge.modelinfo.UnparsedExpression;
import jadex.commons.IValueFetcher;
import jadex.javaparser.IParsedExpression;
import jadex.javaparser.SJavaParser;
import jadex.javaparser.SimpleValueFetcher;

import java.util.HashMap;
import java.util.Map;

/**
 * 
 */
public class RExpressionbase extends RElement implements IExpressionbase
{
	/** The expressions. */
	protected Map<String, IExpression> expressions;
	
	/**
	 *  Create a new beliefbase.
	 */
	public RExpressionbase(IInternalAccess agent)
	{
		super(null, agent);
	}
	
	/**
	 *  Get a predefined expression. 
	 *  Creates a new instance on every call.
	 *  @param name	The name of an expression defined in the ADF.
	 *  @return The expression object.
	 */
	public IExpression	getExpression(String name)
	{
		if(expressions==null || !expressions.containsKey(name))
		{
			RCapability rcapa = ((IInternalBDIAgentFeature)agent.getComponentFeature(IBDIAgentFeature.class)).getCapability();
			MCapability mcapa = (MCapability)rcapa.getModelElement();
			UnparsedExpression uexp = mcapa.getExpression(name);
			if(uexp==null)
				throw new RuntimeException("Unknown expression: "+name);
			RExpression rexp = new RExpression(uexp, getAgent());
			expressions = new HashMap<String, IExpression>();
			expressions.put(name, rexp);
		}
		return expressions.get(name);
	}

	/**
	 *  Create a precompiled expression.
	 *  @param expression	The expression string.
	 *  @return The precompiled expression.
	 */
	public IExpression	createExpression(String expression)
	{
		return new RExpression(new UnparsedExpression(null, expression), getAgent());
	}

//	/**
//	 *  Create a precompiled expression.
//	 *  @param expression	The expression string.
//	 *  @param paramnames The parameter names.
//	 *  @param paramtypes The parameter types.
//	 *  @return The precompiled expression.
//	 */
//	public IExpression	createExpression(String expression, String[] paramnames, Class[] paramtypes)
//	{
//		return new RExpression(new UnparsedExpression(null, expression));
//	}
	
	/**
	 * 
	 */
	public class RExpression extends RElement implements IExpression
	{
		/** The unparsed expression. */
		protected UnparsedExpression uexp;
		
		/** The fetcher. */
		protected IValueFetcher fetcher;
		
		/**
		 * 
		 */
		public RExpression(UnparsedExpression uexp, IInternalAccess agent)
		{
			this(uexp, null, agent);
		}
		
		/**
		 * 
		 */
		public RExpression(UnparsedExpression uexp, IValueFetcher fetcher, IInternalAccess agent)
		{
			super(null, agent);
			this.uexp = uexp;
			this.fetcher = fetcher==null? RBeliefbase.getFetcher(getAgent(), getModelElement()) : fetcher;
		}
		
		/**
		 *  Evaluate the expression.
		 *  @return	The value of the expression.
		 */
		public Object getValue()
		{
			return getParsedExpression().getValue(fetcher);
		}

		/**
		 *  Execute the query.
		 *  @return the result value of the query.
		 */
		public Object execute()
		{
			return getValue();
		}

		/**
		 *  Execute the query using a local parameter.
		 *  @param name The name of the local parameter.
		 *  @param value The value of the local parameter.
		 *  @return the result value of the query.
		 */
		public Object execute(String name, Object value)
		{
			SimpleValueFetcher fet = new SimpleValueFetcher(fetcher);
			fet.setValue(name, value);
			return getParsedExpression().getValue(fet);
		}

		/**
		 *  Execute the query using local parameters.
		 *  @param names The names of parameters.
		 *  @param values The parameter values.
		 *  @return The return value.
		 */
		public Object execute(String[] names, Object[] values)
		{
			SimpleValueFetcher fet = new SimpleValueFetcher(fetcher);
			for(int i=0; i<names.length; i++)
			{
				fet.setValue(names[i], values[i]);
			}
			return getParsedExpression().getValue(fet);
		}
		
		/**
		 * 
		 */
		public IParsedExpression getParsedExpression()
		{
			return SJavaParser.parseExpression(uexp, agent.getModel().getAllImports(), agent.getClassLoader());
		}
	}
}
