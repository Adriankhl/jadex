package jadex.bdiv3.testcases.goals;

import jadex.base.test.TestReport;
import jadex.base.test.Testcase;
import jadex.bdiv3.BDIAgent;
import jadex.bdiv3.annotation.Goal;
import jadex.bdiv3.annotation.GoalParameter;
import jadex.bdiv3.annotation.GoalResult;
import jadex.bdiv3.annotation.GoalTargetCondition;
import jadex.bdiv3.annotation.Plan;
import jadex.bdiv3.annotation.Trigger;
import jadex.bdiv3.runtime.IPlan;
import jadex.bridge.IComponentStep;
import jadex.bridge.IInternalAccess;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.AgentBody;
import jadex.micro.annotation.Result;
import jadex.micro.annotation.Results;

import java.util.ArrayList;
import java.util.List;

/**
 *  Agent that uses a goal with a goal as plan (direct subgoal).
 */
@Agent
@Results(@Result(name="testresults", clazz=Testcase.class))
public class GoalGoalBDI
{
	/** The bdi agent. */
	@Agent
	protected BDIAgent agent;
		
	/**
	 *  The top level goal.
	 */
	@Goal(excludemode=Goal.ExcludeMode.WhenFailed)
	public class TestGoal1
	{
//		@GoalParameter
//		protected int cnt;
		
		@GoalParameter
		protected List<Double> results = new ArrayList<Double>();
		
		@GoalTargetCondition(parameters="results")
		public boolean checkTarget()
		{
//			return cnt==3;
			return results.size()==3;
		}
		
//		public void inc()
//		{
//			cnt++;
//			System.out.println("cnt is: "+cnt);
//		}
		
		@GoalResult
		protected void resultReceived(Double res)
		{
//			System.out.println("rec: "+res);
			results.add(res);
		}
		
		@GoalResult
		protected List<Double> getResult()
		{
			return results;
		}
	}
	
	/**
	 *  The goal that is used as a plan.
	 */
	@Goal(triggergoals=TestGoal1.class)
//	@GoalCreationCondition(trigger=@Trigger(goals=TestGoal1.class))
	public class TestGoal2
	{
//		@GoalAPI
//		protected IGoal goal;

		@GoalResult
		protected Double res;
		
//		@GoalParent
//		protected TestGoal1 parent;
		
//		public TestGoal2()
//		{
//			System.out.println("created testgoal2");
//		}
				
//		public void inc()
//		{
//			parent.inc();
//		}
	}
	
	@Plan(trigger=@Trigger(goals=TestGoal2.class))
	public Double inc(TestGoal2 g, IPlan plan)
	{
//		g.inc();
		plan.waitFor(200).get();
		return new Double(Math.random());
	}
	
	@AgentBody
	public void body()
	{
		final TestReport tr = new TestReport("#1", "Test if a goal condition can be triggered by a goal parameter.");
		
		agent.waitForDelay(2000, new IComponentStep<Void>()
		{
			public IFuture<Void> execute(IInternalAccess ia)
			{
				if(!tr.isFinished())
				{
					tr.setFailed("Goal did return");
					agent.setResultValue("testresults", new Testcase(1, new TestReport[]{tr}));
				}
				
				agent.killAgent();
				return IFuture.DONE;
			}
		});
		
		Object res = agent.dispatchTopLevelGoal(new TestGoal1()).get();
		System.out.println("Goal success: "+res);
		tr.setSucceeded(true);
		agent.setResultValue("testresults", new Testcase(1, new TestReport[]{tr}));
		agent.killAgent();
	}
}
