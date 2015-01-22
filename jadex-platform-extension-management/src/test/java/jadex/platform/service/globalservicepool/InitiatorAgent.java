package jadex.platform.service.globalservicepool;

import jadex.base.test.TestReport;
import jadex.base.test.Testcase;
import jadex.bridge.IComponentIdentifier;
import jadex.bridge.IExternalAccess;
import jadex.bridge.nonfunctional.annotation.NFRProperty;
import jadex.bridge.sensor.service.LatencyProperty;
import jadex.bridge.service.IService;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.bridge.service.types.cms.IComponentManagementService;
import jadex.commons.Tuple2;
import jadex.commons.collection.ArrayBlockingQueue;
import jadex.commons.future.CounterResultListener;
import jadex.commons.future.DefaultTuple2ResultListener;
import jadex.commons.future.DelegationResultListener;
import jadex.commons.future.ExceptionDelegationResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.commons.future.IFutureCommandResultListener;
import jadex.commons.future.IIntermediateFuture;
import jadex.commons.future.IIntermediateResultListener;
import jadex.commons.future.IResultListener;
import jadex.commons.future.TupleResult;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.Binding;
import jadex.micro.annotation.RequiredService;
import jadex.micro.annotation.RequiredServices;
import jadex.platform.TestAgent;
import jadex.platform.service.servicepool.PoolServiceInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 */
@Agent
@RequiredServices(
{
	@RequiredService(name="cms", type=IComponentManagementService.class, binding=@Binding(scope=RequiredServiceInfo.SCOPE_PLATFORM)),
	@RequiredService(name="ts", type=ITestService.class, binding=@Binding(scope=RequiredServiceInfo.SCOPE_GLOBAL)),
	@RequiredService(name="aser", type=ITestService.class, multiple=true,
		binding=@Binding(scope=RequiredServiceInfo.SCOPE_GLOBAL, dynamic=true),
		nfprops=@NFRProperty(value=LatencyProperty.class, methodname="methodA", methodparametertypes=long.class))
})
public class InitiatorAgent extends TestAgent
{
	/**
	 *  Perform the tests.
	 */
	protected IFuture<Void> performTests(final Testcase tc)
	{
		final Future<Void> ret = new Future<Void>();
		
		testRemote(1).addResultListener(agent.createResultListener(new ExceptionDelegationResultListener<TestReport, Void>(ret)
		{
			public void customResultAvailable(TestReport result)
			{
				tc.addReport(result);
				// hack: do not quit for better testing
//				ret.setResult(null);
			}
		}));
		
		return ret;
	}
	
	/**
	 *  Test local.
	 */
	protected IFuture<TestReport> testLocal(final int testno)
	{
		final Future<TestReport> ret = new Future<TestReport>();
		
		performTest(agent.getComponentIdentifier().getRoot(), testno, true)
			.addResultListener(agent.createResultListener(new DelegationResultListener<TestReport>(ret)
		{
			public void customResultAvailable(final TestReport result)
			{
				ret.setResult(result);
			}
		}));
		
		return ret;
	}
	
	/**
	 *  Test remote.
	 */
	protected IFuture<TestReport> testRemote(final int testno)
	{
		final Future<TestReport> ret = new Future<TestReport>();
		
		final List<IExternalAccess> pls = new ArrayList<IExternalAccess>();
		setupRemotePlatforms(4, 0, pls).addResultListener(new ExceptionDelegationResultListener<Void, TestReport>(ret) 
		{
			public void customResultAvailable(Void result) 
			{
				performTest(pls.get(0).getComponentIdentifier(), testno, false)
					.addResultListener(agent.createResultListener(new DelegationResultListener<TestReport>(ret)));
			}
		});
		
		return ret;
	}
	
	/**
	 *  Perform the test. Consists of the following steps:
	 *  Create provider agent
	 *  Call methods on it
	 */
	protected IFuture<TestReport> performTest(final IComponentIdentifier root, final int testno, final boolean hassectrans)
	{
		final Future<TestReport> ret = new Future<TestReport>();

		final Future<TestReport> res = new Future<TestReport>();
		
		ret.addResultListener(new DelegationResultListener<TestReport>(res)
		{
			public void exceptionOccurred(Exception exception)
			{
				TestReport tr = new TestReport("#"+testno, "Tests if nflatency works.");
				tr.setFailed(exception);
				super.resultAvailable(tr);
			}
		});
		
		final Future<Collection<Tuple2<String, Object>>> resfut = new Future<Collection<Tuple2<String, Object>>>();
		IResultListener<Collection<Tuple2<String, Object>>> reslis = new DelegationResultListener<Collection<Tuple2<String,Object>>>(resfut);

		Map<String, Object> args = new HashMap<String, Object>();
		args.put("serviceinfos", new PoolServiceInfo[]{new PoolServiceInfo(WorkerAgent.class.getName()+".class", ITestService.class)});
		createComponent(GlobalServicePoolAgent.class.getName()+".class", args, null, root, reslis)
			.addResultListener(new ExceptionDelegationResultListener<IComponentIdentifier, TestReport>(ret)
		{
			public void customResultAvailable(final IComponentIdentifier cid) 
			{
				callService(cid, testno, 5000).addResultListener(new DelegationResultListener<TestReport>(ret));
			}
			
			public void exceptionOccurred(Exception exception)
			{
				exception.printStackTrace();
				super.exceptionOccurred(exception);
			}
		});
		
		return res;
	}
	
	/**
	 *  Call the service methods.
	 */
	protected IFuture<TestReport> callService(final IComponentIdentifier cid, int testno, final long to)
	{
		final Future<TestReport> ret = new Future<TestReport>();
		
		final TestReport tr = new TestReport("#"+testno, "Test if returning changed nf props works");
		
//		IFuture<ITestService> fut = agent.getServiceContainer().getService(ITestService.class, cid);
		
		// Add awarenessinfo for remote platform
//		IAwarenessManagementService awa = SServiceProvider.getService(agent.getServiceProvider(), IAwarenessManagementService.class, RequiredServiceInfo.SCOPE_PLATFORM).get();
//		AwarenessInfo info = new AwarenessInfo(cid.getRoot(), AwarenessInfo.STATE_ONLINE, -1, 
//			null, null, null, SReflect.getInnerClassName(this.getClass()));
//		awa.addAwarenessInfo(info).get();
		
		IIntermediateFuture<ITestService> fut = agent.getRequiredServices("aser");
		fut.addResultListener(new IIntermediateResultListener<ITestService>()
		{
			boolean called;
			public void intermediateResultAvailable(ITestService result)
			{
				System.out.println("found: "+((IService)result).getServiceIdentifier());
				if(cid.equals(((IService)result).getServiceIdentifier().getProviderId()))
				{
					called = true;
					callService(result);
				}
			}
			public void finished()
			{
				if(!called)
				{
					tr.setFailed("Service not found");
					ret.setResult(tr);
				}
			}
			public void resultAvailable(Collection<ITestService> result)
			{
				for(ITestService ts: result)
				{
					intermediateResultAvailable(ts);
				}
				finished();
			}
			public void exceptionOccurred(Exception exception)
			{
				ret.setException(exception);
			}
			
			protected void callService(final ITestService ts)
			{
				int cnt = 10;
				
				CounterResultListener<Void> lis = new CounterResultListener<Void>(cnt, new ExceptionDelegationResultListener<Void, TestReport>(ret)
				{
					public void customResultAvailable(Void result) 
					{
						if(tr.getReason()==null)
							tr.setSucceeded(true);
						ret.setResult(tr);
					}
				});
				
				ts.methodA(0).addResultListener(lis);
				
				agent.waitForDelay(1000).get();
				
				for(int i=0; i<cnt-1; i++)
				{
					ts.methodA(i+1).addResultListener(lis);
				}
			}
		});
		
		return ret;
	}
	
	/**
	 *  Hack class that avoids printouts of forward command
	 */
	abstract class Tuple2Listener<T, E> extends DefaultTuple2ResultListener<T, E> implements IFutureCommandResultListener<Collection<TupleResult>>
	{
		public void commandAvailable(Object command)
		{
			// nop, avoids printouts
		}
	}
}
