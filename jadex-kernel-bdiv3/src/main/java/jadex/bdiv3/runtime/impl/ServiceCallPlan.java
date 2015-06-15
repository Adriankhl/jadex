package jadex.bdiv3.runtime.impl;

import jadex.bdiv3.annotation.Plan;
import jadex.bdiv3.annotation.PlanBody;
import jadex.bdiv3.annotation.PlanReason;
import jadex.bdiv3.features.impl.BDIAgentFeature;
import jadex.bridge.IInternalAccess;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.bridge.service.component.IRequiredServicesFeature;
import jadex.commons.SReflect;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.commons.future.IIntermediateFuture;
import jadex.commons.future.IIntermediateResultListener;
import jadex.commons.future.IResultListener;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *  Default plan for realizing a service call.
 *  Finds and calls a service.
 */
@Plan
public class ServiceCallPlan
{
	@PlanReason
	protected Object reason;
	
//	@PlanCapability
	protected IInternalAccess agent;
	
	/** The service name. */
	protected String service;
	
	/** The method. */
	protected String method;
	
	/** The parameter service mapper. */
	protected IServiceParameterMapper<Object> mapper;
	
	/** The plan. */
	protected RPlan rplan;
	
	public static int cnt;
	
	/**
	 * 
	 */
	public ServiceCallPlan(IInternalAccess agent, String service, String method, IServiceParameterMapper<Object> mapper, RPlan rplan)
	{
		System.out.println("created service call plan: "+method);
		this.agent = agent;
		this.service = service;
		this.method = method;
		this.mapper = mapper;
		this.rplan = rplan;
	}
	
	@PlanBody
	public IFuture<Void> body()
	{		
		final int mycnt = cnt++;
		System.out.println("service call made: "+mycnt);
		
		final Future<Void> ret = new Future<Void>();

//		IIntermediateFuture<Object> services = agent.getServiceContainer().getRequiredServices(service);
		IIntermediateFuture<Object> services = agent.getComponentFeature(IRequiredServicesFeature.class).getRequiredServices(service);
		
		services.addResultListener(new IIntermediateResultListener<Object>()
		{
			int opencalls = 0;
			boolean fini = false;
			Exception ex = null;
			public void intermediateResultAvailable(Object proxy)
			{
				try
				{
					opencalls++;
					
					Method tmp;
					if(method==null)
					{
						RequiredServiceInfo rsi = agent.getComponentFeature(IRequiredServicesFeature.class).getRequiredServiceInfo(service);
						Class<?> cl = rsi.getType().getType(agent.getClassLoader());
						tmp = cl.getDeclaredMethods()[0];
					}
					else
					{
						tmp = SReflect.getMethods(proxy.getClass(), method)[0];
					}
					final Method m = tmp;
					Object[] myargs = mapper.createServiceParameters(reason, m, rplan);
//					System.out.println("invoking service, args: "+SUtil.arrayToString(myargs));
					
					// todo: HACK! do we need this call to enhance the parameters?
					List<Object> ar = new ArrayList<Object>();
					if(myargs!=null)
					{
						for(Object myarg: myargs)
							ar.add(myarg);
					}
					Object[] meargs = BDIAgentFeature.getInjectionValues(m.getParameterTypes(), null, null, null, rplan, null, ar, agent);
					
					Object	res	= m.invoke(proxy, meargs);
					
					if(res instanceof IFuture<?>)
					{
						((IFuture<Object>)res).addResultListener(new IResultListener<Object>()
						{
							public void resultAvailable(Object result)
							{
								mapper.handleServiceResult(reason, m, result, rplan);
								opencalls--;
								proceed();
							}

							public void exceptionOccurred(Exception exception)
							{
								ex = exception;
								mapper.handleServiceResult(reason, m, exception, rplan);
								opencalls--;
								proceed();
							}
						});
//						System.out.println("invoked, result: "+resu);
						// todo: set return value on parameter
					}
					else
					{
						mapper.handleServiceResult(reason, m, res, rplan);
						opencalls--;
						proceed();
					}
				}
				catch(Exception e)
				{
					opencalls--;
					proceed();
				}
			}
			
			public void finished()
			{
				fini = true;
				proceed();
			}
			
			public void resultAvailable(Collection<Object> result)
			{
				for(Object res: result)
				{
					intermediateResultAvailable(res);
				}
				finished();
			}
			
			public void exceptionOccurred(Exception exception)
			{
				ret.setException(exception);
			}
			
			protected void proceed()
			{
				if(opencalls==0 && fini)
				{
					System.out.println("service call retured: "+mycnt+" "+ex);

					if(ex==null)
					{
						ret.setResult(null);
					}
					else
					{
						ret.setException(ex);
					}
				}
			}
		});
		
		return ret;
	}
}
