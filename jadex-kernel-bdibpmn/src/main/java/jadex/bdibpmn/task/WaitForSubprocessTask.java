package jadex.bdibpmn.task;

import jadex.bpmn.runtime.BpmnInterpreter;
import jadex.bpmn.runtime.ITask;
import jadex.bpmn.runtime.ITaskContext;
import jadex.bpmn.runtime.task.ParameterMetaInfo;
import jadex.bpmn.runtime.task.TaskMetaInfo;
import jadex.commons.Future;
import jadex.commons.IFuture;

/**
 *  Wait for the subprocess result.
 */
public class WaitForSubprocessTask	implements ITask
{
	/**
	 *  Execute the task.
	 */
	public IFuture execute(ITaskContext context, BpmnInterpreter instance)
	{
		// todo: ???
		
		IResultFuture	rf = (IResultFuture)context.getParameterValue("resultfuture");
//		listener.resultAvailable(this, rf.getResults());
		return new Future(rf.getResults());
	}
	
	//-------- static methods --------
	
	/**
	 *  Get the meta information about the agent.
	 */
	public static TaskMetaInfo getMetaInfo()
	{
		String desc = "The wait for subprocess task can be used to wait for an existing subprocess to finish.";
		
		ParameterMetaInfo goalmi = new ParameterMetaInfo(ParameterMetaInfo.DIRECTION_IN, 
			IResultFuture.class, "subprocess", null, "The subprocess parameter identifies the subprocess to be waited for.");

		return new TaskMetaInfo(desc, new ParameterMetaInfo[]{goalmi}); 
	}
}
