package jadex.bpmnbdi.task;

import jadex.application.space.envsupport.environment.IEnvironmentSpace;
import jadex.bpmn.runtime.BpmnInterpreter;
import jadex.bpmn.runtime.ITask;
import jadex.bpmn.runtime.ITaskContext;
import jadex.bpmn.runtime.task.ParameterMetaInfo;
import jadex.bpmn.runtime.task.TaskMetaInfo;
import jadex.commons.concurrent.IResultListener;

import java.util.Map;

/**
 *  Create a task for a space object.
 */
public class CreateSpaceObjectTaskTask	implements	ITask
{
	/**
	 *  Execute the task.
	 */
	public void execute(ITaskContext context, BpmnInterpreter instance, IResultListener listener)
	{
		try
		{
			String type	= (String)context.getParameterValue("type");
			IEnvironmentSpace	space	= (IEnvironmentSpace)context.getParameterValue("space");
			Object	objectid	= context.getParameterValue("objectid");
			Map	properties	= context.hasParameterValue("properties")
				? (Map)context.getParameterValue("properties") : null;
			
			Object	taskid	= space.createObjectTask(type, properties, objectid);
			
			if(context.hasParameterValue("taskid"))
				context.setParameterValue("taskid", taskid);
			
			boolean	wait	= context.hasParameterValue("wait")
				? ((Boolean)context.getParameterValue("wait")).booleanValue() : true;
			if(wait)
			{
				space.addTaskListener(taskid, objectid, listener);
			}
			else
			{
				listener.resultAvailable(this, null);
			}
		}
		catch(Exception e)
		{
			listener.exceptionOccurred(this, e);
		}
	}
	
	//-------- static methods --------
	
	/**
	 *  Get the meta information about the agent.
	 */
	public static TaskMetaInfo getMetaInfo()
	{
		String desc = "The create space object task task can be used to create a space object task in an" +
			"EnvSupport environment.";
		
		ParameterMetaInfo typemi = new ParameterMetaInfo(ParameterMetaInfo.DIRECTION_IN, 
			String.class, "type", null, "The type parameter identifies the space object task type.");
		ParameterMetaInfo spacemi = new ParameterMetaInfo(ParameterMetaInfo.DIRECTION_IN, 
			IEnvironmentSpace.class, "space", null, "The space parameter defines the space.");
		ParameterMetaInfo objectid = new ParameterMetaInfo(ParameterMetaInfo.DIRECTION_IN, 
			Object.class, "objectid", null, "The objectid parameter for identifying the space object.");
		ParameterMetaInfo propsmi = new ParameterMetaInfo(ParameterMetaInfo.DIRECTION_IN, 
			Map.class, "properties", null, "The properties parameter to specify a map of properties for the task.");
		ParameterMetaInfo taskidmi = new ParameterMetaInfo(ParameterMetaInfo.DIRECTION_OUT, 
			Object.class, "taskid", null, "The taskid parameter for the return value, i.e. the id of the created task.");
		ParameterMetaInfo waitmi = new ParameterMetaInfo(ParameterMetaInfo.DIRECTION_IN, 
			boolean.class, "wait", null, "The wait parameter to wait for the task.");

		return new TaskMetaInfo(desc, new ParameterMetaInfo[]{typemi, spacemi, objectid, propsmi, taskidmi, waitmi}); 
	}
}
