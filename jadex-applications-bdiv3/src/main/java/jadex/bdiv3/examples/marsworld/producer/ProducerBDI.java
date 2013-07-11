package jadex.bdiv3.examples.marsworld.producer;

import jadex.bdiv3.annotation.Body;
import jadex.bdiv3.annotation.Deliberation;
import jadex.bdiv3.annotation.Goal;
import jadex.bdiv3.annotation.GoalDropCondition;
import jadex.bdiv3.annotation.Plan;
import jadex.bdiv3.annotation.PlanAPI;
import jadex.bdiv3.annotation.PlanCapability;
import jadex.bdiv3.annotation.PlanReason;
import jadex.bdiv3.annotation.Plans;
import jadex.bdiv3.annotation.Trigger;
import jadex.bdiv3.examples.marsworld.BaseBDI;
import jadex.bdiv3.examples.marsworld.movement.MovementCapability;
import jadex.bdiv3.examples.marsworld.movement.MovementCapability.WalkAround;
import jadex.bdiv3.examples.marsworld.sentry.SentryBDI.AnalyzeTarget;
import jadex.bdiv3.runtime.IPlan;
import jadex.bridge.service.annotation.Reference;
import jadex.bridge.service.annotation.Service;
import jadex.commons.future.IFuture;
import jadex.extension.envsupport.environment.ISpaceObject;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.Implementation;
import jadex.micro.annotation.ProvidedService;
import jadex.micro.annotation.ProvidedServices;

/**
 * 
 */
@Agent
@Service
@ProvidedServices(@ProvidedService(type=IProduceService.class, implementation=@Implementation(expression="$pojoagent")))
@Plans({
	@Plan(trigger=@Trigger(goals=ProducerBDI.ProduceOre.class), body=@Body(ProduceOrePlan.class)),
	@Plan(trigger=@Trigger(factaddeds="movecapa/mytargets"), body=@Body(InformNewTargetPlan.class))
})
public class ProducerBDI extends BaseBDI implements IProduceService
{
	//-------- attributes --------

	@PlanCapability
	protected MovementCapability capa;
	
	@PlanAPI
	protected IPlan rplan;
	
	@PlanReason
	protected AnalyzeTarget goal;
	
	/**
	 * 
	 */
	@Goal(deliberation=@Deliberation(inhibits=WalkAround.class))
	public class ProduceOre
	{
		/** The target. */
		protected ISpaceObject target;

		/**
		 *  Create a new CarryOre. 
		 */
		public ProduceOre(ISpaceObject target)
		{
			this.target = target;
		}
		
		/**
		 * 
		 */
		@GoalDropCondition(events="movecapa.missionend")
		public boolean checkDrop()
		{
			return movecapa.isMissionend();
		}

		/**
		 *  Get the target.
		 *  @return The target.
		 */
		public ISpaceObject getTarget()
		{
			return target;
		}
		
	}
	
	/**
	 * 
	 */
	public IFuture<Void> doProduce(@Reference ISpaceObject target)
	{
		agent.dispatchTopLevelGoal(new ProduceOre(target));
		return IFuture.DONE;
	}
}
