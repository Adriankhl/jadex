package jadex.bdi.testcases.misc;

import jadex.base.test.TestReport;
import jadex.bdi.IDynamicBDIFactory;
import jadex.bdi.model.editable.IMEBelief;
import jadex.bdi.model.editable.IMECapability;
import jadex.bdi.model.editable.IMEConfiguration;
import jadex.bdi.model.editable.IMEPlan;
import jadex.bdi.runtime.Plan;
import jadex.bridge.service.search.SServiceProvider;
import jadex.bridge.service.types.cms.IComponentManagementService;
import jadex.bridge.service.types.library.ILibraryService;
import jadex.commons.future.DelegationResultListener;
import jadex.commons.future.Future;

/**
 *  Create and test a hello world agent.
 */
public class DynamicModelPlan extends Plan
{
	//-------- attributes --------
	
	/** The test report. */
	protected TestReport	tr	= new TestReport("#1", "Test dynamic model creation.");
	
	//-------- methods --------
	
	/**
	 *  Perform the test.
	 */
	public void body()
	{
		IDynamicBDIFactory	fac	= (IDynamicBDIFactory)getServiceContainer().getRequiredService("factory").get(this);
		IMECapability agent = fac.createAgentModel("HelloWorld", "jadex.bdi.examples.helloworld", null, getScope().getClassLoader());
			
		IMEBelief	msgbelief	= agent.createBeliefbase().createBelief("msg");
		msgbelief.createFact("\"Welcome to editable models!\"", null);
			
		IMEPlan helloplan = agent.createPlanbase().createPlan("hello");
		helloplan.createBody("HelloWorldPlan", null);
		IMEConfiguration conf = agent.createConfiguration("default");
		conf.createInitialPlan("hello");
			
		fac.registerAgentModel(agent, "helloagent.agent.xml");
			
		IComponentManagementService cms	= (IComponentManagementService)getServiceContainer().getRequiredService("cms").get(this);

		Future	finished	= new Future();
		cms.createComponent("hw1", "helloagent.agent.xml", null, new DelegationResultListener(finished));
		
		finished.get(this);
		
		tr.setSucceeded(true);
		getBeliefbase().getBeliefSet("testcap.reports").addFact(tr);
	}
	
	/**
	 *  Test failed.
	 */
	public void failed()
	{
		tr.setFailed(""+getException());
		getBeliefbase().getBeliefSet("testcap.reports").addFact(tr);
	}
}
