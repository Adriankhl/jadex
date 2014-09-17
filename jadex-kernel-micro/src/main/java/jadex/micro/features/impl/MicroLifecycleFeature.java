package jadex.micro.features.impl;

import jadex.bridge.IExternalAccess;
import jadex.bridge.IInternalAccess;
import jadex.bridge.component.ComponentCreationInfo;
import jadex.bridge.component.IComponentFeature;
import jadex.bridge.component.IExecutionFeature;
import jadex.bridge.component.impl.AbstractComponentFeature;
import jadex.commons.SReflect;
import jadex.commons.Tuple2;
import jadex.commons.future.ExceptionDelegationResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.commons.future.IIntermediateResultListener;
import jadex.commons.future.IResultListener;
import jadex.micro.MicroModel;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.AgentBody;
import jadex.micro.annotation.AgentCreated;
import jadex.micro.annotation.AgentKilled;
import jadex.micro.features.IMicroLifecycleFeature;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 *  Feature that ensures the agent created(), body() and killed() are called on the pojo. 
 */
public class MicroLifecycleFeature extends	AbstractComponentFeature implements IMicroLifecycleFeature
{
	//-------- type level --------
	
	/**
	 *  Bean constructor for type level.
	 */
	public MicroLifecycleFeature()
	{
	}
	
	/**
	 *  Get the user interface type of the feature.
	 */
	public Class<?>	getType()
	{
		return IMicroLifecycleFeature.class;
	}
	
	/**
	 *  Create an instance of the feature.
	 *  @param access	The access of the component.
	 *  @param info	The creation info.
	 */
	public IComponentFeature	createInstance(IInternalAccess access, ComponentCreationInfo info)
	{
		return new MicroLifecycleFeature(access, info);
	}
	
	//-------- instance level --------
	
	/** The pojo agent. */
	protected Object pojoagent;
	
	/**
	 *  Factory method constructor for instance level.
	 */
	public MicroLifecycleFeature(IInternalAccess component, ComponentCreationInfo cinfo)
	{
		super(component, cinfo);
		
		try
		{
			// Create the pojo agent
			MicroModel model = (MicroModel)getComponent().getModel().getRawModel();
			this.pojoagent = model.getPojoClass().newInstance();
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
	 *  The pojoagent to set.
	 *  @param pojoagent The pojoagent to set
	 */
	public void setPojoAgent(Object pojoagent)
	{
		this.pojoagent = pojoagent;
	}
	
	/**
	 *  Initialize the feature.
	 *  Empty implementation that can be overridden.
	 */
	public IFuture<Void> init()
	{
		final Future<Void> ret = new Future<Void>();
		invokeMethod(AgentCreated.class, null).addResultListener(
			createResultListener(new ExceptionDelegationResultListener<Tuple2<Method, Object>, Void>(ret)
		{
			public void customResultAvailable(Tuple2<Method, Object> result)
			{
				ret.setResult(null);
			}
		}));
		return ret;
	}
	
	/**
	 *  Execute the functional body of the agent.
	 *  Is only called once.
	 */
	public IFuture<Void> body()
	{
		final Future<Void> ret = new Future<Void>();
		
		invokeMethod(AgentBody.class, null)
			.addResultListener(createResultListener(
				new ExceptionDelegationResultListener<Tuple2<Method, Object>, Void>(ret)
		{
			public void customResultAvailable(Tuple2<Method, Object> res)
			{
				// Only end body if future or void and kill is true 
				boolean kill = false;
				
				Method method = res!=null? res.getFirstEntity(): null;
				
				if(method!=null)
				{
					if(SReflect.isSupertype(IFuture.class, method.getReturnType()))
					{
						kill = true;
					}
					else if(void.class.equals(method.getReturnType()))
					{
						AgentBody ab = method.getAnnotation(AgentBody.class);
						kill = !ab.keepalive();
					}
				}
				else
				{
					Agent ag = getPojoAgent().getClass().getAnnotation(Agent.class);
					kill = !ag.keepalive();
				}
				
				if(kill)
				{
					ret.setResult(null);
				}
			}
		}));
		
		return ret;
	}

	/**
	 *  Called just before the agent is removed from the platform.
	 *  @return The result of the component.
	 */
	public IFuture<Void> shutdown()
	{
		final Future<Void> ret = new Future<Void>();

		invokeMethod(AgentKilled.class, null).addResultListener(
			createResultListener(new ExceptionDelegationResultListener<Tuple2<Method, Object>, Void>(ret)
		{
			public void customResultAvailable(Tuple2<Method, Object> result)
			{
				ret.setResult(null);
			}
		}));
		return ret;
	}
	
	/**
	 *  Invoke double methods.
	 *  The boolean 'firstorig' determines if basicservice method is called first.
	 */
	protected IFuture<Tuple2<Method, Object>> invokeMethod(Class<? extends Annotation> annotation, Object[] args)
	{
		final Future<Tuple2<Method, Object>> ret = new Future<Tuple2<Method, Object>>();
		
		Method[] methods = getPojoAgent().getClass().getMethods();
		boolean found = false;
		
		for(int i=0; i<methods.length && !found; i++)
		{
			final Method method = methods[i];
			if(method.isAnnotationPresent(annotation))
			{
				found = true;
				
				// Try to guess additional parameters as internal or external access.
				if(args==null || method.getParameterTypes().length>args.length)
				{
					Object[]	tmp	= new Object[method.getParameterTypes().length];
					if(args!=null)
					{
						System.arraycopy(args, 0, tmp, 0, args.length);
					}
					for(int j=args==null?0:args.length; j<method.getParameterTypes().length; j++)
					{
						Class<?>	clazz	= method.getParameterTypes()[j];
						if(SReflect.isSupertype(clazz, IInternalAccess.class))
						{
							tmp[j]= getComponent();
						}
						else if(SReflect.isSupertype(clazz, IExternalAccess.class))
						{
							tmp[j]= getComponent().getExternalAccess();
						}
					}
					args	= tmp;
				}
				
				try
				{
					Object res = method.invoke(getPojoAgent(), args);
					if(res instanceof IFuture)
					{
						((IFuture)res).addResultListener(createResultListener(
							new ExceptionDelegationResultListener<Object, Tuple2<Method, Object>>(ret)
						{
							public void customResultAvailable(Object result)
							{
								ret.setResult(new Tuple2<Method, Object>(method, result));
							}
						}
						));
					}
					else
					{
						ret.setResult(new Tuple2<Method, Object>(method, res));
					}
				}
				catch(Exception e)
				{
					e = (Exception)(e instanceof InvocationTargetException && ((InvocationTargetException)e)
						.getTargetException() instanceof Exception? ((InvocationTargetException)e).getTargetException(): e);
					ret.setException(e);
					break;
				}
			}
		}
		
		if(!found)
		{
			// Check if annotation is present and complain that method is not public.
			
			Class<?> clazz = getPojoAgent().getClass();
			
			while(!Object.class.equals(clazz) && !found)
			{
				methods = clazz.getDeclaredMethods();
				
				for(int i=0; i<methods.length && !found; i++)
				{
					if(methods[i].isAnnotationPresent(annotation))
					{
						found = true;
						ret.setException(new RuntimeException("Method must be declared public: "+methods[i]));
						break;
					}
				}
				
				clazz = clazz.getSuperclass();
			}
			
			if(!found)
			{
				ret.setResult(null);
			}
		}
		
		return ret;
	}
	
	/**
	 *  Create a result listener that is executed on the
	 *  component thread.
	 */
	public <T> IResultListener<T> createResultListener(IResultListener<T> listener)
	{
		return getComponent().getComponentFeature(IExecutionFeature.class).createResultListener(listener);
	}
	
	/**
	 *  Create a result listener that is executed on the
	 *  component thread.
	 */
	public <T> IIntermediateResultListener<T> createResultListener(IIntermediateResultListener<T> listener)
	{
		return getComponent().getComponentFeature(IExecutionFeature.class).createResultListener(listener);
	}
}
