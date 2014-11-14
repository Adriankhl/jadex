package jadex.micro.testcases.blocking;

import jadex.base.test.TestReport;
import jadex.base.test.Testcase;
import jadex.bridge.IInternalAccess;
import jadex.bridge.component.IArgumentsFeature;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.bridge.service.search.SServiceProvider;
import jadex.bridge.service.types.cms.IComponentManagementService;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.AgentBody;
import jadex.micro.annotation.Result;
import jadex.micro.annotation.Results;

/**
 *  Test threaded access to raw services.
 */
@Agent
@Results(@Result(name="testresults", clazz=Testcase.class))
public class RawBlockingTestAgent
{
	/**
	 *  Execute the agent
	 */
	@AgentBody(keepalive=false)
	public void	execute(final IInternalAccess agent)
	{
		IComponentManagementService	cms	= SServiceProvider.getService(agent,
			IComponentManagementService.class, RequiredServiceInfo.SCOPE_PLATFORM).get();
		
		cms.getComponentDescriptions().get();
		
		agent.getComponentFeature(IArgumentsFeature.class).getResults().put("testresults", new Testcase(1,
			new TestReport[]{new TestReport("#1", "Test blocking wait.", true, null)}));
	}
}
