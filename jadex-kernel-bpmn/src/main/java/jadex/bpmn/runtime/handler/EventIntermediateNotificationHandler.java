package jadex.bpmn.runtime.handler;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import jadex.bpmn.model.MActivity;
import jadex.bpmn.model.MParameter;
import jadex.bpmn.runtime.BpmnInterpreter;
import jadex.bpmn.runtime.ProcessThread;

/**
 *  Wait for an external notification (could be a signal or a fired rule).
 *  Makes available a notfier object as "notifier" property.
 */
public class EventIntermediateNotificationHandler extends DefaultActivityHandler
{
	//-------- constants --------
	
	/** The property for the external notifier (system). */
	public static final String	PROPERTY_EXTERNALNOTIFIER = "externalnotifier";

	//-------- methods --------
	
	/**
	 *  Execute an activity.
	 *  @param activity	The activity to execute.
	 *  @param instance	The process instance.
	 *  @param thread	The process thread.
	 */
	public void execute(final MActivity activity, final BpmnInterpreter instance, final ProcessThread thread)
	{
		thread.setWaiting(true);
		
		// Create a shallow copy of properties.
		HashMap props = new HashMap();
		Map params = thread.getActivity().getParameters();
		if(params!=null)
		{
			for(Iterator it=params.values().iterator(); it.hasNext(); )
			{
				MParameter param = (MParameter)it.next();
				props.put(param.getName(), thread.getParameterValue(param.getName()));
			}
		}
		
		IExternalNotifier ext = (IExternalNotifier)thread.getPropertyValue(PROPERTY_EXTERNALNOTIFIER);
		if(ext!=null)
			ext.activateWait(props, new Notifier(this, activity, instance, thread));
		else
			System.out.println("Warning, thread is waiting forever, no external notification system specified.");
		
		System.out.println("Waiting for notification.");
	}
}
