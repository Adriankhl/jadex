package jadex.platform.service.message.transport.ssltcpmtp;


import jadex.base.test.TestReport;
import jadex.base.test.Testcase;
import jadex.bridge.IComponentIdentifier;
import jadex.bridge.IExternalAccess;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.commons.Tuple2;
import jadex.commons.future.DelegationResultListener;
import jadex.commons.future.ExceptionDelegationResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.commons.future.IResultListener;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.Binding;
import jadex.micro.annotation.RequiredService;
import jadex.micro.annotation.RequiredServices;

import java.util.Collection;

/**
 *  Test if the @SecureTransmission annotation works.
 *  Uses a service with two method signatures, one unsecured / one secured.
 *  Uses a cross platform setting with / without secure transport.
 *  (Assumes that the host platform has a secure transport).
 *  
 *  Tests if:
 *  a) with a secure transport both methods can be invoked.
 *  b) without the secure cannot! and the unsecure can be invoked.
 */
@Agent
@RequiredServices(
{
	@RequiredService(name="ts", type=ITestService.class, binding=@Binding(scope=RequiredServiceInfo.SCOPE_GLOBAL))
})
public class InitiatorAgent extends TestAgent
{
	/**
	 *  Perform the tests.
	 */
	protected IFuture<Void> performTests(final Testcase tc)
	{
		final Future<Void> ret = new Future<Void>();
		
		testWithSec(1).addResultListener(agent.createResultListener(new ExceptionDelegationResultListener<TestReport, Void>(ret)
		{
			public void customResultAvailable(TestReport result)
			{
				tc.addReport(result);
				testWithoutSec(2).addResultListener(agent.createResultListener(new ExceptionDelegationResultListener<TestReport, Void>(ret)
				{
					public void customResultAvailable(TestReport result)
					{
						tc.addReport(result);
						ret.setResult(null);
					}
				}));
			}
		}));
		
		return ret;
	}
	
	/**
	 *  Test with secure transport.
	 */
	protected IFuture<TestReport> testWithSec(final int testno)
	{
		final Future<TestReport> ret = new Future<TestReport>();
		
		createPlatform(new String[]{"-ssltcptransport", "true"}).addResultListener(agent.createResultListener(
			new ExceptionDelegationResultListener<IExternalAccess, TestReport>(ret)
		{
			public void customResultAvailable(final IExternalAccess platform)
			{
				performTest(platform.getComponentIdentifier(), testno, true)
					.addResultListener(agent.createResultListener(new DelegationResultListener<TestReport>(ret)
				{
					public void customResultAvailable(final TestReport result)
					{
						platform.killComponent();
//							.addResultListener(new ExceptionDelegationResultListener<Map<String, Object>, TestReport>(ret)
//						{
//							public void customResultAvailable(Map<String, Object> v)
//							{
//								ret.setResult(result);
//							}
//						});
						ret.setResult(result);
					}
				}));
			}
		}));
		
		return ret;
	}
	
	/**
	 *  Test without secure transport.
	 */
	protected IFuture<TestReport> testWithoutSec(final int testno)
	{
		final Future<TestReport> ret = new Future<TestReport>();
		
		createPlatform(new String[]{"-ssltcptransport", "false"}).addResultListener(agent.createResultListener(
			new ExceptionDelegationResultListener<IExternalAccess, TestReport>(ret)
		{
			public void customResultAvailable(final IExternalAccess platform)
			{
				performTest(platform.getComponentIdentifier(), testno, false)
					.addResultListener(agent.createResultListener(new DelegationResultListener<TestReport>(ret)
				{
					public void customResultAvailable(final TestReport result)
					{
						platform.killComponent();
//							.addResultListener(new ExceptionDelegationResultListener<Map<String, Object>, TestReport>(ret)
//						{
//							public void customResultAvailable(Map<String, Object> v)
//							{
//								ret.setResult(result);
//							}
//						});
						ret.setResult(result);
					}
				}));
			}
		}));
		
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
				TestReport tr = new TestReport("#"+testno, "Tests if secure transport works.");
				tr.setReason(exception.getMessage());
				super.resultAvailable(tr);
			}
		});
		
		final Future<Collection<Tuple2<String, Object>>> resfut = new Future<Collection<Tuple2<String, Object>>>();
		IResultListener<Collection<Tuple2<String, Object>>> reslis = new DelegationResultListener<Collection<Tuple2<String,Object>>>(resfut);
		
		createComponent("jadex/platform/service/message/transport/ssltcpmtp/ProviderAgent.class", root, reslis)
			.addResultListener(new ExceptionDelegationResultListener<IComponentIdentifier, TestReport>(ret)
		{
			public void customResultAvailable(final IComponentIdentifier cid) 
			{
				callService(cid, hassectrans, testno).addResultListener(new DelegationResultListener<TestReport>(ret));
			}
		});
		
		return res;
	}
	
	/**
	 *  Call the service methods.
	 */
	protected IFuture<TestReport> callService(IComponentIdentifier cid, final boolean hassectrans, int testno)
	{
		final Future<TestReport> ret = new Future<TestReport>();
		
		final TestReport tr = new TestReport("#"+testno, "Test if secure transmission works "+(hassectrans? "with ": "without ")+"secure transport.");
		
		IFuture<ITestService> fut = agent.getServiceContainer().getService(ITestService.class, cid);
		fut.addResultListener(new ExceptionDelegationResultListener<ITestService, TestReport>(ret)
		{
			public void customResultAvailable(final ITestService ts)
			{
				ts.secMethod("sec_arg").addResultListener(new IResultListener<Void>()
				{
					public void resultAvailable(Void result)
					{
						// Must succeed with secure transport.
						proceed(ts, hassectrans);
					}
					
					public void exceptionOccurred(Exception exception)
					{
						// Must not succeed without secure transport.
//						exception.printStackTrace();
						proceed(ts, !hassectrans);
					}
				});
			}
			
			protected void proceed(ITestService ts, final boolean ok)
			{
				ts.unsecMethod("unsec_arg").addResultListener(new IResultListener<Void>()
				{
					public void resultAvailable(Void result)
					{
						if(ok)
						{
							tr.setSucceeded(true);
						}
						else
						{
							tr.setFailed("Sec transport did not work");
						}
						ret.setResult(tr);
					}
					
					public void exceptionOccurred(Exception exception)
					{
						if(ok)
						{
							tr.setFailed("Sec transport worked and normal did not work");
						}
						else
						{
							tr.setFailed("Sec transport and normal did not work");
						}
						ret.setResult(tr);
					}
				});
			}
		});
		return ret;
	}
}
