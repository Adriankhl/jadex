package jadex.bdi.planlib.df;

import jadex.adapter.base.fipa.DFRegister;
import jadex.adapter.base.fipa.Done;
import jadex.adapter.base.fipa.IDF;
import jadex.adapter.base.fipa.IDFAgentDescription;
import jadex.adapter.base.fipa.SFipa;
import jadex.bdi.runtime.IGoal;
import jadex.bdi.runtime.Plan;
import jadex.bridge.IAgentIdentifier;

import java.util.Date;

/**
 *  Register on a remote platform.
 */
public class DFRemoteRegisterPlan extends Plan
{
	/**
	 * The body method is called on the
	 * instatiated plan instance from the scheduler.
	 */
	public void body()
	{
//		System.out.println("df register");
		DFRegister re = new DFRegister();
		IDFAgentDescription desc = (IDFAgentDescription)getParameter("description").getValue();
		Number lt = (Number)getParameter("leasetime").getValue();
		// When AID is ommited, enter self. Hack???
		if(desc.getName()==null || lt!=null)
		{
			IDF	dfservice	= (IDF)getScope().getPlatform().getService(IDF.class, SFipa.DF_SERVICE);
			IAgentIdentifier	bid	= desc.getName()!=null ? desc.getName() : getScope().getAgentIdentifier();
			Date	leasetime	= lt==null ? desc.getLeaseTime() : new Date(getTime()+lt.longValue());
			desc	= dfservice.createDFAgentDescription(bid, desc.getServices(), desc.getLanguages(), desc.getOntologies(), desc.getProtocols(), leasetime);
		}
		
		re.setAgentDescription(desc);

		IGoal req = createGoal("rp_initiate");
		req.getParameter("receiver").setValue(getParameter("df").getValue());
		req.getParameter("action").setValue(re);
		dispatchSubgoalAndWait(req);

		getParameter("result").setValue(((DFRegister)((Done)req.getParameter("result").getValue()).getAction()).getResult());
	}
}
