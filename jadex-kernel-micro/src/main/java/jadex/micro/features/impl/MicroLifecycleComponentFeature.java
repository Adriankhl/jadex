package jadex.micro.features.impl;

import jadex.bridge.IInternalAccess;
import jadex.bridge.component.ComponentCreationInfo;
import jadex.bridge.component.IArgumentsFeature;
import jadex.bridge.component.IComponentFeatureFactory;
import jadex.bridge.component.ISubcomponentsFeature;
import jadex.bridge.component.impl.AbstractComponentFeature;
import jadex.bridge.component.impl.ComponentFeatureFactory;
import jadex.bridge.service.component.IProvidedServicesFeature;
import jadex.bridge.service.component.IRequiredServicesFeature;
import jadex.commons.FieldInfo;
import jadex.commons.IParameterGuesser;
import jadex.commons.IValueFetcher;
import jadex.commons.MethodInfo;
import jadex.commons.SimpleParameterGuesser;
import jadex.commons.Tuple3;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.commons.future.IResultListener;
import jadex.javaparser.SJavaParser;
import jadex.javaparser.SimpleValueFetcher;
import jadex.micro.IPojoMicroAgent;
import jadex.micro.MicroModel;
import jadex.micro.annotation.AgentBody;
import jadex.micro.annotation.AgentCreated;
import jadex.micro.annotation.AgentKilled;
import jadex.micro.features.IMicroLifecycleFeature;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;

/**
 *  Feature that ensures the agent created(), body() and killed() are called on the pojo. 
 */
public class MicroLifecycleComponentFeature extends	AbstractComponentFeature implements IMicroLifecycleFeature, IValueFetcher
{
	//-------- constants --------
	
	/** The factory. */
	public static final IComponentFeatureFactory FACTORY = new ComponentFeatureFactory(IMicroLifecycleFeature.class, MicroLifecycleComponentFeature.class,
		new Class<?>[]{IRequiredServicesFeature.class, IProvidedServicesFeature.class, ISubcomponentsFeature.class}, null);
	
	//-------- attributes --------
	
	/** The pojo agent. */
	protected Object pojoagent;
	
	/** The parameter guesser (cached for speed). */
	protected IParameterGuesser	guesser; 
	
	//-------- constructors --------
	
	/**
	 *  Factory method constructor for instance level.
	 */
	public MicroLifecycleComponentFeature(IInternalAccess component, ComponentCreationInfo cinfo)
	{
		super(component, cinfo);
		
		try
		{
			// Create the pojo agent
			MicroModel model = (MicroModel)getComponent().getModel().getRawModel();
			this.pojoagent = model.getPojoClass().getType(model.getClassloader()).newInstance();
		}
		catch(Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 *  Get the pojoagent.
	 *  @return The pojoagent
	 */
	public Object getPojoAgent()
	{
		return pojoagent;
	}

	/**
	 *  Initialize the feature.
	 *  Empty implementation that can be overridden.
	 */
	public IFuture<Void> init()
	{
		return invokeMethod(getComponent(), AgentCreated.class, null);
	}
	
	/**
	 *  Execute the functional body of the agent.
	 *  Is only called once.
	 */
	public IFuture<Void> body()
	{
		return invokeMethod(getComponent(), AgentBody.class, null);
	}

	/**
	 *  Called just before the agent is removed from the platform.
	 *  @return The result of the component.
	 */
	public IFuture<Void> shutdown()
	{
		final Future<Void> ret = new Future<Void>();
		invokeMethod(getComponent(), AgentKilled.class, null).addResultListener(new IResultListener<Void>()
		{
			public void resultAvailable(Void result)
			{
				proceed(null);
			}
			
			public void exceptionOccurred(Exception exception)
			{
				proceed(exception);
			}
			
			protected void proceed(Exception e)
			{
				try
				{
					MicroModel micromodel = (MicroModel)getComponent().getModel().getRawModel();
					Object agent = getPojoAgent();
					
					for(String name: micromodel.getResultInjectionNames())
					{
						Tuple3<FieldInfo, String, String> inj = micromodel.getResultInjection(name);
						Field field = inj.getFirstEntity().getField(getComponent().getClassLoader());
						String convback = inj.getThirdEntity();
						
						field.setAccessible(true);
						Object val = field.get(agent);
						
						if(convback!=null)
						{
							SimpleValueFetcher fetcher = new SimpleValueFetcher(getComponent().getFetcher());
							fetcher.setValue("$value", val);
							val = SJavaParser.evaluateExpression(convback, getComponent().getModel().getAllImports(), fetcher, getComponent().getClassLoader());
						}
						
						getComponent().getComponentFeature(IArgumentsFeature.class).getResults().put(name, val);
					}
				}
				catch(Exception e2)
				{
					ret.setException(e2);
//					throw new RuntimeException(e2);
				}
				
				if(!ret.isDone())
				{
					if(e!=null)
					{
						ret.setException(e);
					}
					else
					{
						ret.setResult(null);
					}
				}
			}
		});
		
		return ret;
	}
	
	/**
	 *  The feature can inject parameters for expression evaluation
	 *  by providing an optional value fetcher. The fetch order is the reverse
	 *  init order, i.e., later features can override values from earlier features.
	 */
	public IValueFetcher	getValueFetcher()
	{
		return this;
	}

	/**
	 *  Add $pojoagent to fetcher.
	 */
	public Object fetchValue(String name)
	{
		if("$pojoagent".equals(name))
		{
			return getPojoAgent();
		}
		else
		{
			throw new RuntimeException("Value not found: "+name);
		}
	}
	
	/**
	 *  The feature can add objects for field or method injections
	 *  by providing an optional parameter guesser. The selection order is the reverse
	 *  init order, i.e., later features can override values from earlier features.
	 */
	public IParameterGuesser	getParameterGuesser()
	{
		if(guesser==null)
		{
			guesser	= new SimpleParameterGuesser(Collections.singleton(pojoagent));
		}
		return guesser;
	}
	
	//-------- helper methods --------
	
	/**
	 *  Invoke an agent method by injecting required arguments.
	 */
	public static IFuture<Void> invokeMethod(IInternalAccess component, Class<? extends Annotation> ann, Object[] args)
	{
		IFuture<Void> ret;
		
		MicroModel	model = (MicroModel)component.getModel().getRawModel();
		MethodInfo	mi	= model.getAgentMethod(ann);
		if(mi!=null)
		{
			Method	method	= mi.getMethod(component.getClassLoader());
			
			// Try to guess parameters from given args or component internals.
			IParameterGuesser	guesser	= args!=null ? new SimpleParameterGuesser(component.getParameterGuesser(), Arrays.asList(args)) : component.getParameterGuesser();
			Object[]	iargs	= new Object[method.getParameterTypes().length];
			for(int i=0; i<method.getParameterTypes().length; i++)
			{
				iargs[i]	= guesser.guessParameter(method.getParameterTypes()[i], false);
			}
			
			try
			{
				Object res = method.invoke(component.getComponentFeature(IMicroLifecycleFeature.class).getPojoAgent(), iargs);
				if(res instanceof IFuture)
				{
					ret	= (IFuture<Void>)res;
				}
				else
				{
					ret	= IFuture.DONE;
				}
			}
			catch(Exception e)
			{
				e = (Exception)(e instanceof InvocationTargetException && ((InvocationTargetException)e)
					.getTargetException() instanceof Exception? ((InvocationTargetException)e).getTargetException(): e);
				ret	= new Future<Void>(e);
			}
		}
		else
		{
			ret	= IFuture.DONE;
		}
		
		return ret;
	}
}
