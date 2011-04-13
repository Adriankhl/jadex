package jadex.micro.testcases.semiautomatic.compositeservice;

import jadex.bridge.ComponentTerminatedException;
import jadex.bridge.service.IService;
import jadex.bridge.service.component.ServiceInvocationContext;
import jadex.bridge.service.component.interceptors.AbstractApplicableInterceptor;
import jadex.commons.future.DelegationResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.commons.future.IResultListener;
import jadex.micro.MicroAgent;
import jadex.micro.annotation.Description;
import jadex.micro.annotation.ProvidedService;
import jadex.micro.annotation.ProvidedServices;

/**
 *  
 */
@Description("This agent is a minimal calculator.")
@ProvidedServices({
	@ProvidedService(type=IAddService.class, expression="new AddService($component)"),
	@ProvidedService(type=ISubService.class, expression="new SubService($component)")}
)
public class CorruptAdderAgent extends MicroAgent
{
	protected int calls;
	
	/**
	 * 
	 */
	public IFuture agentCreated()
	{
		IService addser = getServiceContainer().getProvidedService(IAddService.class);
		
		getServiceContainer().addInterceptor(new AbstractApplicableInterceptor()
		{
			public IFuture execute(ServiceInvocationContext context)
			{
				final Future ret = new Future();
				try
				{
					if(context.getMethod().equals(IAddService.class.getMethod("add", new Class[]{double.class, double.class})))
					{
						context.setResult(new Future(new ComponentTerminatedException(getComponentIdentifier())));
						System.out.println("hello interceptor");
//						if(calls++>0)
						{
							// Wait till agent has terminated to ensure that its
							// service is not found as result again.
							killAgent().addResultListener(new IResultListener()
							{
								public void resultAvailable(Object result)
								{
									ret.setResult(null);
								}
								public void exceptionOccurred(Exception exception)
								{
									System.out.println("cannot terminate, already terminated");
								}
							});
						}
					}
					else
					{
						context.invoke().addResultListener(new DelegationResultListener(ret));
					}
				}
				catch(Exception e)
				{
//					e.printStackTrace();
				}
				
				return ret;
			}
		}, addser, 0);
		
		return IFuture.DONE;
	}
}

