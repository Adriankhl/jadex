package jadex.platform.service.servicepool;

import jadex.base.test.TestReport;
import jadex.base.test.Testcase;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.bridge.service.search.SServiceProvider;
import jadex.commons.DefaultPoolStrategy;
import jadex.commons.Tuple2;
import jadex.commons.future.CounterResultListener;
import jadex.commons.future.DefaultResultListener;
import jadex.commons.future.DelegationResultListener;
import jadex.commons.future.ExceptionDelegationResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.micro.MicroAgent;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.AgentBody;
import jadex.micro.annotation.Binding;
import jadex.micro.annotation.ComponentType;
import jadex.micro.annotation.ComponentTypes;
import jadex.micro.annotation.CreationInfo;
import jadex.micro.annotation.RequiredService;
import jadex.micro.annotation.RequiredServices;
import jadex.micro.annotation.Result;
import jadex.micro.annotation.Results;

import java.util.Collection;

/**
 *  User agent that first registers services A, B at the service pool (which is created if not present).
 *  Then searches for A, B and invokes the methods.
 *  The calls will be distributed among the instances of the pool.
 */
@Agent
@RequiredServices(
{
	@RequiredService(name="poolser", type=IServicePoolService.class, binding=@Binding(
		scope=RequiredServiceInfo.SCOPE_PLATFORM, create=true, 
		creationinfo=@CreationInfo(type="spa"))),
	@RequiredService(name="aser", type=IAService.class),
	@RequiredService(name="bser", type=IBService.class)
})
@ComponentTypes(@ComponentType(name="spa", filename="jadex.platform.service.servicepool.ServicePoolAgent.class"))
@Results(@Result(name="testresults", clazz=Testcase.class))
public class UserAgent
{
	//-------- attributes --------
	
	@Agent
	protected MicroAgent agent;
	
	//-------- methods --------
	
	/**
	 *  Agent body.
	 */
	@AgentBody
	public IFuture<Void> body()
	{
		final Future<Void> ret = new Future<Void>();
		
		registerServices().addResultListener(new DelegationResultListener<Void>(ret)
		{
			public void customResultAvailable(Void result)
			{
				searchServices().addResultListener(new ExceptionDelegationResultListener<Tuple2<IAService,IBService>, Void>(ret)
				{
					public void customResultAvailable(Tuple2<IAService, IBService> sers)
					{
						useServices(sers.getFirstEntity(), sers.getSecondEntity())
							.addResultListener(new DelegationResultListener<Void>(ret));
					}
				});
			}
		});
			
		return ret;
	}
	
	/**
	 *  Register services at the service pool service.
	 */
	public IFuture<Void> registerServices()
	{
		final Future<Void> ret = new Future<Void>();
		IFuture<IServicePoolService> fut = agent.getRequiredService("poolser");
		fut.addResultListener(new ExceptionDelegationResultListener<IServicePoolService, Void>(ret)
		{
			public void customResultAvailable(final IServicePoolService sps)
			{
				sps.addServiceType(IAService.class, new DefaultPoolStrategy(5, 35000, 10), "jadex.platform.service.servicepool.example.AAgent.class")
					.addResultListener(new DelegationResultListener<Void>(ret)
				{
					public void customResultAvailable(Void result)
					{
						sps.addServiceType(IBService.class, new DefaultPoolStrategy(3, 10), "jadex.platform.service.servicepool.example.BAgent.class")
							.addResultListener(new DelegationResultListener<Void>(ret));
					}	
				});
			}
		});
			
		return ret;
	}
	
	/**
	 *  Search the services.
	 */
	protected IFuture<Tuple2<IAService, IBService>> searchServices()
	{
		final Future<Tuple2<IAService, IBService>> ret = new Future<Tuple2<IAService, IBService>>();
		
		IFuture<IAService> fut = agent.getRequiredService("aser");
		fut.addResultListener(new ExceptionDelegationResultListener<IAService, Tuple2<IAService, IBService>>(ret)
		{
			public void customResultAvailable(final IAService aser)
			{				
				IFuture<IBService> fut = agent.getRequiredService("bser");
				fut.addResultListener(new ExceptionDelegationResultListener<IBService, Tuple2<IAService, IBService>>(ret)
				{
					public void customResultAvailable(final IBService bser)
					{
						ret.setResult(new Tuple2<IAService, IBService>(aser, bser));
					}
				});
			}
		});
		
		return ret;
	}
	
	/**
	 *  Use the services.
	 */
	protected IFuture<Void> useServices(final IAService aser, final IBService bser)
	{
		final Future<Void> ret = new Future<Void>();
		
		final TestReport rep1 = new TestReport("#1", "Test invoking service A ma1");
		final int cnt1 = 100;
		CounterResultListener<String> lis1 = new CounterResultListener<String>(cnt1, new DefaultResultListener<Void>()
		{
			public void resultAvailable(Void result) 
			{
//				System.out.println("called "+cntma1+" times ma1");
				rep1.setSucceeded(true);
			
				final TestReport rep2 = new TestReport("#2", "Test invoking service A ma2");
				final int cnt2 = 10;
				CounterResultListener<Collection<Integer>> lis = new CounterResultListener<Collection<Integer>>(cnt2, new DefaultResultListener<Void>()
				{
					public void resultAvailable(Void result) 
					{
//						System.out.println("called "+cnt+" times ma2");
						rep2.setSucceeded(true);
						
						final TestReport rep3 = new TestReport("#3", "Test if no A services besides proxy can be found");
						// Ensure that only 
						SServiceProvider.getServices(agent.getServiceProvider(), IAService.class)
							.addResultListener(new ExceptionDelegationResultListener<Collection<IAService>, Void>(ret)
						{
							public void customResultAvailable(Collection<IAService> result)
							{
								System.out.println("found: "+result.size());
								if(result.size()==1)
								{
									rep3.setSucceeded(true);
								}
								else
								{
									rep3.setReason("Found more than one A service: "+result.size());
								}
								
								final TestReport rep4 = new TestReport("#4", "Test invoking service B mb1");
								final int cnt4 = 100;
								CounterResultListener<String> lis4 = new CounterResultListener<String>(cnt4, new DefaultResultListener<Void>()
								{
									public void resultAvailable(Void result) 
									{
//										System.out.println("called "+cntma1+" times ma1");
										rep4.setSucceeded(true);
										
										agent.setResultValue("testresults", new Testcase(4, new TestReport[]{rep1, rep2, rep3, rep4}));
										
										ret.setResult(null);
									}
								});
								for(int i=0; i<cnt4; i++)
								{
									bser.mb1("hello "+i).addResultListener(lis4);
								}
							}
						});
					}
				});
				for(int i=0; i<cnt2; i++)
				{
					aser.ma2().addResultListener(lis);
				}
			}
		});
		for(int i=0; i<cnt1; i++)
		{
			aser.ma1("hello "+i).addResultListener(lis1);
		}
		
		return ret;
	}
}
