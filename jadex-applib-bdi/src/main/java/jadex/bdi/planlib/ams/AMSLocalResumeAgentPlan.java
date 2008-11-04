package jadex.bdi.planlib.ams;

import jadex.adapter.base.fipa.IAMS;
import jadex.adapter.base.fipa.IAMSAgentDescription;
import jadex.adapter.base.fipa.SFipa;
import jadex.bdi.runtime.Plan;
import jadex.bridge.IAgentIdentifier;

/**
 *  Plan for resuming a Jadex agent on the platform.
 */
public class AMSLocalResumeAgentPlan extends Plan
{
	/**
	 *  Execute a plan.
	 */
	public void body()
	{	
		IAgentIdentifier	aid	= (IAgentIdentifier)getParameter("agentidentifier").getValue();
		
		SyncResultListener lis = new SyncResultListener();
		((IAMS)getScope().getPlatform().getService(IAMS.class, SFipa.AMS_SERVICE)).resumeAgent(aid, lis);
		IAMSAgentDescription desc =  (IAMSAgentDescription)lis.waitForResult();
		
		getParameter("agentdescription").setValue(desc);
	}
	
}
