package jadex.bridge.service.component;

import jadex.bridge.Cause;
import jadex.bridge.IComponentIdentifier;
import jadex.bridge.ServiceCall;
import jadex.bridge.service.BasicServiceContainer;
import jadex.bridge.service.IServiceIdentifier;
import jadex.bridge.service.component.interceptors.CallAccess;
import jadex.bridge.service.types.factory.IComponentAdapter;
import jadex.commons.SUtil;
import jadex.commons.Tuple2;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.commons.future.IResultListener;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 *  Context for service invocations.
 *  Contains all method call information. 
 */
public class ServiceInvocationContext
{
	//-------- profiling --------
	
	/** Enable call profiling. */
	public static final boolean	PROFILING	= false;
	
	/** Print every 10 seconds. */
	public static final long	PRINT_DELAY	= 10000;
	
	/** Service calls per method, calculated separately per platform. */
	protected static Map<IComponentIdentifier, Map<Method, Integer>>	calls	= PROFILING ? new HashMap<IComponentIdentifier, Map<Method, Integer>>() : null;
	
	static
	{
		if(PROFILING)
		{
			final Timer	timer	= new Timer(true);
			final Runnable	run	= new Runnable()
			{
				public void run()
				{
					IComponentIdentifier[]	platforms;
					synchronized(calls)
					{
						platforms	= calls.keySet().toArray(new IComponentIdentifier[calls.size()]);
					}
					for(IComponentIdentifier platform: platforms)
					{
						Map<Method, Integer>	pcalls;
						synchronized(calls)
						{
							pcalls	= calls.get(platform);
						}
						StringBuffer	out	= new StringBuffer("Calls of platform ").append(platform).append("\n");
						synchronized(pcalls)
						{
							for(Method m: pcalls.keySet())
							{
								out.append("\t").append(pcalls.get(m)).append(":\t")
									.append(m.getDeclaringClass().getSimpleName()).append(".").append(m.getName()).append(SUtil.arrayToString(m.getParameterTypes()))
									.append("\n");
							}
						}
						System.out.println(out);
					}
					
					final Runnable	run	= this;
					timer.schedule(new TimerTask()
					{
						public void run()
						{
							run.run();
						}
					}, PRINT_DELAY);
				}
			};
			
			timer.schedule(new TimerTask()
			{
				public void run()
				{
					run.run();
				}
			}, PRINT_DELAY);
		}
	}
	
	//-------- attributes --------
	
	/** The origin (proxy object). */
	protected Object proxy;
	
	
	/** The object. */
	protected List<Object> object;
	
	/** The method to be called. */
	protected List<Method> method;
	
	/** The invocation arguments. */
	protected List<List<Object>> arguments;
	
	/** The call result. */
	protected List<Object> result;
	

	/** The service interceptors. */
	protected IServiceInvocationInterceptor[] interceptors;

	/** The stack of used interceptors. */
	protected List<Integer> used;
	
	/** The service call. */
	protected ServiceCall	call;
	
	/** The last service call (to be reestablished after call). */
	protected ServiceCall	lastcall;
	
	/** The caller component. */
	protected IComponentIdentifier caller;
	
	/** The caller component adapter. */
	protected IComponentAdapter calleradapter;
	
	/** The platform identifier. */
	protected IComponentIdentifier platform;
	
	/** The flag if local timeouts should be realtime. */
	protected boolean realtime;
	
	/** The creation (root) cause. */
	protected Cause cause;
	
	
	protected IServiceIdentifier sid;
//	public Exception ex;
	
	//-------- constructors --------
	
	/**
	 *  Create a new context.
	 */
	public ServiceInvocationContext(Object proxy, Method method, 
		IServiceInvocationInterceptor[] interceptors, IComponentIdentifier platform, 
		boolean realtime, IServiceIdentifier sid, Cause crcause)
	{
//		this.ex = new RuntimeException();
		this.sid = sid;
		
		this.platform = platform;
		this.proxy = proxy;
		this.realtime	= realtime;
		this.object = new ArrayList<Object>();
		this.method = new ArrayList<Method>();
		this.arguments = new ArrayList<List<Object>>();
		this.result = new ArrayList<Object>();
		this.cause = crcause;
		
		this.used = new ArrayList<Integer>();
		this.interceptors = interceptors;
		
		this.caller = IComponentIdentifier.LOCAL.get();
		this.calleradapter	= IComponentAdapter.LOCAL.get();
		
		this.lastcall = CallAccess.getCurrentInvocation();
		
		// Is next call defined by user?
		this.call = CallAccess.getNextInvocation(); 
		if(call==null)
		{
	//		Map<String, Object> props = call!=null ? new HashMap<String, Object>(call.getProperties()) : new HashMap<String, Object>();
			Map<String, Object> props = null;
			
			Boolean inh = lastcall!=null? (Boolean)lastcall.getProperty(ServiceCall.INHERIT): null;
			if(inh!=null && inh.booleanValue())
			{
				props = new HashMap<String, Object>(lastcall.getProperties());
				props.remove(ServiceCall.CAUSE); // remove cause as it has to be adapted
			}
			else
			{
				props = new HashMap<String, Object>();
			}
			this.call = CallAccess.createServiceCall(caller, props);
		}
		
		if(!call.getProperties().containsKey(ServiceCall.TIMEOUT))
		{
			call.setProperty(ServiceCall.TIMEOUT, new Long(BasicServiceContainer.getMethodTimeout(proxy.getClass().getInterfaces(), method, isRemoteCall())));			
		}
		if(!call.getProperties().containsKey(ServiceCall.REALTIME))
		{
			call.setProperty(ServiceCall.REALTIME, realtime ? Boolean.TRUE : Boolean.FALSE);
		}
		
		// Init the cause of the next call based on the last one
//		if(method.getName().indexOf("test")!=-1 && lastcall!=null)
//			System.out.println("lastcall: "+lastcall.getCause());

		if(this.call.getCause()==null)
		{
//			String target = SUtil.createUniqueId(caller!=null? caller.getName(): "unknown", 3);
			String target = sid.toString();
			if(lastcall!=null && lastcall.getCause()!=null)
			{
				this.call.setCause(new Cause(lastcall.getCause(), target));
//				if(method.getName().indexOf("test")!=-1 && lastcall!=null)
//					System.out.println("Creating new cause based on: "+lastcall.getCause());
//				this.call.setCause(new Tuple2<String, String>(cause.getSecondEntity(), SUtil.createUniqueId(caller!=null? caller.getName(): "unknown", 3)));
			}
			else
			{
				// Create cause with novel chain id as origin is component itself
				Cause newc = new Cause(cause);
//				newc.setChainId(newc.createUniqueId());
//				newc.setOrigin(cause.getTargetId());
				// This is on receiver side, i.e. must set the caller as origin
				newc.setOrigin(caller!=null? caller.getName(): sid.getProviderId().getName());
				this.call.setCause(new Cause(newc, target));
				
//				if(method.getName().indexOf("createCompo")!=-1)
//					System.out.println("herer: "+cause);
			}
		}
	}
	
	/**
	 *  Create a copied context.
	 */
	public ServiceInvocationContext(ServiceInvocationContext context)
	{
		this.sid = context.sid;
//		this.ex= context.ex;
		
		this.call	= context.call;
		this.lastcall = context.lastcall;
		this.realtime	= context.realtime;
		this.platform = context.platform;
		this.proxy = context.proxy;
		this.object = new ArrayList<Object>(context.object);
		this.method = new ArrayList<Method>(context.method);
		this.arguments = new ArrayList<List<Object>>(context.arguments);
		this.result = new ArrayList<Object>(context.result);
		
		this.used = new ArrayList<Integer>(context.used);
		this.interceptors = context.interceptors;
		
		this.caller = context.caller;
		this.calleradapter = context.calleradapter;
		this.cause = context.cause;
	}
	
	/**
	 *  Clone a service invocation context.
	 */
	public ServiceInvocationContext	clone()
	{
		return new ServiceInvocationContext(this); 
	}

	//-------- methods --------
	
	/**
	 *  Get the proxy.
	 *  @return The proxy.
	 */
	public Object getProxy()
	{
		return proxy;
	}

	/**
	 *  Set the proxy.
	 *  @param proxy The proxy to set.
	 */
	public void setProxy(Object proxy)
	{
		this.proxy = proxy;
	}
	
	/**
	 *  Get the object.
	 *  @return the object.
	 */
	public Object getObject()
	{
		return object.get(used.size()-1);
	}

	/**
	 *  Set the object.
	 *  @param object The object to set.
	 */
	public void setObject(Object object)
	{
		this.object.set(used.size()-1, object);
	}

	/**
	 *  Get the method.
	 *  @return the method.
	 */
	public Method getMethod()
	{
		return (Method)method.get(used.size()-1);
	}

	/**
	 *  Set the method.
	 *  @param method The method to set.
	 */
	public void setMethod(Method method)
	{
		this.method.set(used.size()-1, method);
	}

	/**
	 *  Get the args.
	 *  @return the args.
	 */
	public List<Object> getArguments()
	{
		return arguments.get(used.size()-1);
	}
	
	/**
	 *  Get the args.
	 *  @return the args.
	 */
	public Object[] getArgumentArray()
	{
		List<Object> args = arguments.get(used.size()-1);
		return args!=null? args.toArray(): new Object[0];
	}
	
	/**
	 *  Set the arguments.
	 *  @param args The arguments to set.
	 */
	public void setArguments(List<Object> args)
	{
		this.arguments.set(used.size()-1, args);
	}

	/**
	 *  Get the result.
	 *  @return the result.
	 */
	public Object getResult()
	{
		return result.get(used.size()-1);
	}

	/**
	 *  Set the result.
	 *  @param result The result to set.
	 */
	public void setResult(Object result)
	{
//		if(getMethod().getName().indexOf("subsc")!=-1)
//			System.out.println("gotta");
		this.result.set(used.size()-1, result);
	}

	/**
	 *  Invoke the next interceptor.
	 */
	public IFuture<Void> invoke(Object object, final Method method, List<Object> args)
	{
		final Future<Void> ret = new Future<Void>();
		
//		if(method.getName().equals("testResultReferences"))
//			System.out.println("invoke: "+caller);
		
		push(object, method, args, null);
		
		final IServiceInvocationInterceptor interceptor = getNextInterceptor();

//		if(method.getName().equals("ma1"))
//			System.out.println("ma1: "+used.get(used.size()-1)+" "+interceptor+" "+Thread.currentThread());
		
		if(interceptor!=null)
		{
//			if(method.getName().equals("shutdownService") && sid.toString().indexOf("Context")!=-1 && sid.getProviderId().getParent()==null)
//			if(sid.getProviderId().getParent()==null && method.getName().indexOf("getResults")!=-1)
//				System.out.println("invoke before: "+method.getName()+" "+interceptor);
			interceptor.execute(this).addResultListener(new IResultListener<Void>()
			{
				public void resultAvailable(Void result)
				{
//					if(sid.getProviderId().getParent()==null)// && method.getName().indexOf("getChildren")!=-1)
//						System.out.println("invoke after: "+method.getName()+" "+interceptor);

//					if(method.getName().indexOf("getResults")!=-1)
//						System.out.println("invoke after: "+method.getName()+" "+interceptor+" "+getResult());
					
					pop();
					ret.setResult(null);
				}
				
				public void exceptionOccurred(Exception exception)
				{
//					if(sid.getProviderId().getParent()==null)
//						System.out.println("invoke after: "+method.getName()+" "+interceptor);

//					if(method.getName().equals("isValid"))
//						System.out.println("interceptor(ex): "+interceptor);

					pop();
					ret.setException(exception);
				}
				
				public String toString()
				{
					return "ServiceInvocationContext$1(method="+method.getName()+", result="+result+")";
				}

			});
		}
		else
		{
			System.out.println("No interceptor: "+method.getName());
			ret.setException(new RuntimeException("No interceptor found: "+method.getName()));
		}

		return ret;
	}
	
	/**
	 *  Get the next interceptor.
	 */
	public IServiceInvocationInterceptor getNextInterceptor()
	{
		IServiceInvocationInterceptor ret = null;
		
		if(interceptors!=null)
		{
			int start = used.size()==0? -1: (Integer)used.get(used.size()-1);
			for(int i=start+1; i<interceptors.length; i++)
			{
				// add before to allow isApplicable fetch context values.
				used.add(new Integer(i));
				if(interceptors[i].isApplicable(this))
				{
					ret = interceptors[i];
					break;
				}
				else
				{
					used.remove(new Integer(i));
				}
			}
		}
		
		return ret;
	}
	
	/**
	 *  Invoke the next interceptor.
	 */
	public IFuture<Void> invoke()
	{
		return invoke(getObject(), getMethod(), getArguments());
	}

	/**
	 *  Push saves and copies the current set of values.
	 */
	protected void push(Object o, Method m, List<Object> args, Object res)
	{
		// profile on first invoke
		if(PROFILING && method.isEmpty())
		{
//			System.out.println("invoke from "+IComponentIdentifier.LOCAL.get()+": "+m);
			
			IComponentIdentifier	pf	= IComponentIdentifier.LOCAL.get();
			pf	= pf!=null ? pf.getRoot() : platform;
			
			Map<Method, Integer>	pcalls;
			synchronized(calls)
			{
				pcalls	= calls.get(pf);
				if(pcalls==null)
				{
					pcalls	= new HashMap<Method, Integer>();
					calls.put(pf, pcalls);
				}
			}
			synchronized(pcalls)
			{
				Integer	cnt	= pcalls.get(m);
				pcalls.put(m, new Integer(cnt==null ? 0 : cnt.intValue()+1));
			}
		}
		
		object.add(o);
		method.add(m);
		arguments.add(args);
		result.add(res);
	}
	
	/**
	 *  Pop delete the top most set of values.
	 */
	protected void pop()
	{
		// Keep last results
		if(used.size()>1)
		{
			used.remove(used.size()-1);
			object.remove(object.size()-1);
			method.remove(method.size()-1);
			arguments.remove(arguments.size()-1);
			Object res = this.result.remove(this.result.size()-1);
			result.set(result.size()-1, res);
		}
	}
	
	/**
	 *  Test if a call is remote.
	 */
	public boolean isRemoteCall()
	{
		return caller==null? false: !caller.getRoot().equals(platform);
	}
	
//	/**
//	 *  Test if this call is local.
//	 *  @return True, if it is a local call. 
//	 */
//	public boolean isLocalCall()
//	{
//		return !Proxy.isProxyClass(getObject().getClass());
//	}
	
//	/**
//	 *  Test if a call is remote.
//	 *  @param sic The service invocation context.
//	 */
//	public boolean isRemoteCall()
//	{
//		Object target = getObject();
////		if(Proxy.isProxyClass(target.getClass()))
////			System.out.println("blubb "+Proxy.getInvocationHandler(target).getClass().getName());
//		// todo: remove string based remote check! RemoteMethodInvocationHandler is in package jadex.platform.service.remote
//		return Proxy.isProxyClass(target.getClass()) && Proxy.getInvocationHandler(target).getClass().getName().indexOf("Remote")!=-1;
//	}
	
//	/**
//	 * 
//	 */
//	public void copy(ServiceInvocationContext sic)
//	{
//		setObjectStack(sic.getObjectStack());
//		setMethodStack(sic.getMethodStack());
//		setArgumentStack(sic.getArgumentStack());
//		setResultStack(sic.getResultStack());
//		
//	}
	
	/**
	 *  Get the real target object.
	 *  Returns domain service in case of service info.
	 */
	public Object getTargetObject()
	{
		Object ret = getObject();
		if(ret instanceof ServiceInfo)
		{
			ret = ((ServiceInfo)object).getDomainService();
		}
		return ret;
	}
	
	/**
	 *  Get the caller adapter.
	 */
	public IComponentAdapter	getCallerAdapter()
	{
		return this.calleradapter;
	}
	
	/**
	 *  Get the caller.
	 *  @return The caller.
	 */
	public IComponentIdentifier getCaller()
	{
		return caller;
	}

	/**
	 *  String representation.
	 */
	public String toString()
	{
		return "ServiceInvocationContext(method="+method+", caller="+caller+")";
	}

	/**
	 *  Get the service call.
	 *  @return The service call.
	 */
	public ServiceCall	getServiceCall()
	{
		return call;
	}
	
	/**
	 *  Get the last service call.
	 *  @return The last service call.
	 */
	public ServiceCall	getLastServiceCall()
	{
		return lastcall;
	}

	/**
	 *  Set the lastcall. 
	 *  @param lastcall The lastcall to set.
	 */
	public void setCurrentCall(ServiceCall call)
	{
		this.call = call;
	}
}


