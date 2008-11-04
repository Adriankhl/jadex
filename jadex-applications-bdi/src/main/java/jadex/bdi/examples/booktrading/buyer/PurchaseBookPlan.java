package jadex.bdi.examples.booktrading.buyer;

import jadex.adapter.base.fipa.IDFAgentDescription;
import jadex.bdi.examples.booktrading.common.NegotiationReport;
import jadex.bdi.examples.booktrading.common.Order;
import jadex.bdi.planlib.protocols.NegotiationRecord;
import jadex.bdi.planlib.protocols.ParticipantProposal;
import jadex.bdi.runtime.GoalFailureException;
import jadex.bdi.runtime.IGoal;
import jadex.bdi.runtime.Plan;
import jadex.bridge.IAgentIdentifier;

import java.util.Date;

/**
 * The plan tries to purchase a book.
 */
public class PurchaseBookPlan extends Plan
{
	//-------- methods --------

	/**
	 * The body method is called on the
	 * instatiated plan instance from the scheduler.
	 */
	public void body()
	{
//		System.out.println("PurchaseBookPlan");
		
		// Get order properties and calculate acceptable price.
		Order order = (Order)getParameter("order").getValue();
		double time_span = order.getDeadline().getTime() - order.getStartTime();
		double elapsed_time = getTime() - order.getStartTime();
		double price_span = order.getLimit() - order.getStartPrice();
		int acceptable_price = (int)(price_span * elapsed_time / time_span)
			+ order.getStartPrice();

		// Find available seller agents.
		IGoal df_search = createGoal("df_search");
		df_search.getParameter("description").setValue(getPropertybase().getProperty("service_seller"));
		dispatchSubgoalAndWait(df_search);
		IDFAgentDescription[] result = (IDFAgentDescription[])df_search
			.getParameterSet("result").getValues();
		if(result.length == 0)
		{
			System.out.println("No seller found, purchase failed.");
			fail();
		}
		
		IAgentIdentifier[] sellers = new IAgentIdentifier[result.length];
		for(int i = 0; i < result.length; i++)
			sellers[i] = result[i].getName();
		//System.out.println("found: "+SUtil.arrayToString(sellers));

		// Initiate a call-for-proposal.
		IGoal cnp = createGoal("cnp_initiate");
		cnp.getParameter("cfp").setValue(order.getTitle());
		cnp.getParameter("cfp_info").setValue(new Integer(acceptable_price));
		cnp.getParameterSet("receivers").addValues(sellers);		
		try
		{
			dispatchSubgoalAndWait(cnp);
			
			NegotiationRecord rec = (NegotiationRecord)cnp.getParameterSet("history").getValues()[0];
			generateNegotiationReport(order, rec, acceptable_price);
			
			// If contract-net succeeds, store result in order object.
			order.setState(Order.DONE);
			order.setExecutionPrice((Integer)(cnp.getParameterSet("result").getValues()[0]));
			order.setExecutionDate(new Date(getTime()));
		}
		catch(GoalFailureException e)
		{
			NegotiationRecord rec = (NegotiationRecord)cnp.getParameterSet("history").getValues()[0];
			generateNegotiationReport(order, rec, acceptable_price);
			
			fail();
		}
		//System.out.println("result: "+cnp.getParameter("result").getValue());
	}
	
	/**
	 *  Generate and add a negotiation report.
	 */
	protected void generateNegotiationReport(Order order, NegotiationRecord rec, double acceptable_price)
	{
		String report = "Accepable price: "+acceptable_price+", proposals: ";
		ParticipantProposal[] proposals = rec.getProposals();
		for(int i=0; i<proposals.length; i++)
		{
			report += proposals[i].getProposal()+"-"+proposals[i].getParticipant().getLocalName();
			if(i+1<proposals.length)
				report += ", ";
		}
		NegotiationReport nr = new NegotiationReport(order, report, rec.getStarttime());
		//System.out.println("REPORT of agent: "+getAgentName()+" "+report);
		getBeliefbase().getBeliefSet("negotiation_reports").addFact(nr);
	}
}