package jadex.bdi.examples.marsworld_env.sentry;

import jadex.adapter.base.envsupport.environment.ISpaceObject;
import jadex.adapter.base.envsupport.environment.space2d.Space2D;
import jadex.bdi.runtime.IGoal;
import jadex.bdi.runtime.Plan;


/**
 *  Inform the sentry agent about a new target.
 */
public class AnalyseTargetPlan extends Plan
{
	//-------- constructors --------

	/**
	 *  Create a new plan.
	 */
	public AnalyseTargetPlan()
	{
		getLogger().info("Created: "+this);
	}

	//-------- methods --------

	/**
	 *  The plan body.
	 */
	public void body()
	{
		ISpaceObject target = (ISpaceObject)getParameter("target").getValue();

		// Move to the target.
		IGoal go_target = createGoal("move.move_dest");
		go_target.getParameter("destination").setValue(target.getProperty(Space2D.POSITION));
		dispatchSubgoalAndWait(go_target);

		// Analyse the target.
		ISpaceObject	myself	= (ISpaceObject)getBeliefbase().getBelief("move.myself").getFact();
		SyncResultListener	res	= new SyncResultListener();
		myself.addTask(new AnalyseTargetTask(target, res));
		Number	ore	= (Number)res.waitForResult();
		System.out.println("Analyzed target: "+getAgentName()+", "+ore+" ore found.");
//		if(ore.intValue()>0)
//			callProductionAgent(target, ore.intValue());
	}

	/**
	 *  Sending a location to the Production Agent.
	 *  Therefore it has first to be looked up in the DF.
	 *  @param target
	 * /
	private void callProductionAgent(Target target)
	{
//		System.out.println("Calling some Production Agent...");

		IContextService cs = (IContextService)getScope().getPlatform().getService(IContextService.class);
		ApplicationContext ac = (ApplicationContext)cs.getContexts(getScope().getAgentIdentifier(), ApplicationContext.class)[0];
		AGRSpace agrs = (AGRSpace)ac.getSpace("myagrspace");
		Group group = agrs.getGroup("mymarsteam");
		IAgentIdentifier[]	producers	= group.getAgentsForRole("producer");

		if(producers.length>0)
		{
			int sel = (int)(Math.random()*producers.length); // todo: Select not randomly
//			System.out.println("Found agents: "+producers.length+" selected: "+sel);

			RequestProduction rp = new RequestProduction();
			rp.setTarget(target);
			//Action action = new Action();
			//action.setAction(rp);
			//action.setActor(SJade.convertAIDtoJade(producers[sel].getName()));
			IMessageEvent mevent = createMessageEvent("request_producer");
			mevent.getParameterSet(SFipa.RECEIVERS).addValue(producers[sel]);
			mevent.getParameter(SFipa.CONTENT).setValue(rp);
			sendMessage(mevent);
//			System.out.println("Sentry Agent: sent location to: "+producers[sel].getName());
		}
	}*/
}
