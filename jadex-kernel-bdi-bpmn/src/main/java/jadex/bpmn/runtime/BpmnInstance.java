package jadex.bpmn.runtime;

import jadex.bpmn.model.MActivity;
import jadex.bpmn.model.MBpmnModel;
import jadex.bpmn.model.MParameter;
import jadex.bpmn.runtime.handler.DefaultActivityHandler;
import jadex.bpmn.runtime.handler.EventIntermediateMultipleActivityHandler;
import jadex.bpmn.runtime.handler.GatewayParallelActivityHandler;
import jadex.bpmn.runtime.handler.GatewayXORActivityHandler;
import jadex.bpmn.runtime.handler.SubProcessActivityHandler;
import jadex.bpmn.runtime.handler.TaskActivityHandler;
import jadex.bpmn.runtime.handler.basic.EventIntermediateTimerActivityHandler;
import jadex.bpmn.runtime.handler.basic.UserInteractionActivityHandler;
import jadex.commons.ChangeEvent;
import jadex.commons.IChangeListener;
import jadex.commons.SReflect;
import jadex.javaparser.IParsedExpression;
import jadex.javaparser.IValueFetcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 *  Representation of a running BPMN process.
 */
public class BpmnInstance	implements IProcessInstance
{
	//-------- static part --------
	
	/** The activity execution handlers (activity type -> handler). */
	public static final Map	DEFAULT_HANDLERS;
	
	static
	{
		Map	defhandlers	= new HashMap();
		
		defhandlers.put(MBpmnModel.TASK, new TaskActivityHandler());
		defhandlers.put(MBpmnModel.SUBPROCESS, new SubProcessActivityHandler());
		
		defhandlers.put(MBpmnModel.GATEWAY_PARALLEL, new GatewayParallelActivityHandler());
		defhandlers.put(MBpmnModel.GATEWAY_DATABASED_EXCLUSIVE, new GatewayXORActivityHandler());

		defhandlers.put(MBpmnModel.EVENT_START_EMPTY, new DefaultActivityHandler());
		defhandlers.put(MBpmnModel.EVENT_END_EMPTY, new DefaultActivityHandler());
		defhandlers.put(MBpmnModel.EVENT_INTERMEDIATE_ERROR, new DefaultActivityHandler());
		defhandlers.put(MBpmnModel.EVENT_INTERMEDIATE_RULE, new UserInteractionActivityHandler());
		defhandlers.put(MBpmnModel.EVENT_INTERMEDIATE_TIMER, new EventIntermediateTimerActivityHandler());
		defhandlers.put(MBpmnModel.EVENT_INTERMEDIATE_MULTIPLE, new EventIntermediateMultipleActivityHandler());
		
		DEFAULT_HANDLERS	= Collections.unmodifiableMap(defhandlers);
	}
	
	//-------- attributes --------
	
	/** The activity handlers. */
	protected Map	handlers;

	/** The global value fetcher. */
	protected IValueFetcher	fetcher;

	/** The thread context. */
	protected ThreadContext	context;
	
	/** The change listeners. */
	protected List	listeners;
	
	//-------- constructors --------
	
	/**
	 *  Create a new BPMN process instance using default handler.
	 *  @param model	The BMPN process model.
	 */
	public BpmnInstance(MBpmnModel model)
	{
		this(model, DEFAULT_HANDLERS, null);
	}

	/**
	 *  Create a new BPMN process instance.
	 *  @param model	The BMPN process model.
	 *  @param handlers	The activity handlers.
	 */
	public BpmnInstance(MBpmnModel model, Map handlers, IValueFetcher fetcher)
	{
		this.handlers	= handlers;
		this.fetcher	= fetcher;
		this.context	= new ThreadContext(model);
		
		// Create initial thread(s). 
		List	startevents	= model.getStartActivities();
		for(int i=0; startevents!=null && i<startevents.size(); i++)
		{
			context.addThread(new ProcessThread((MActivity)startevents.get(i), context));
		}
	}
	
	//-------- methods --------
	
	/**
	 *  Get the model of the BPMN process instance.
	 *  @return The model.
	 */
	public MBpmnModel	getModelElement()
	{
		return (MBpmnModel)context.getModelElement();
	}
	
	/**
	 *  Get the thread context.
	 *  @return The thread context.
	 */
	public ThreadContext getThreadContext()
	{
		return context;
	}
	
	/**
	 *  Check, if the process has terminated.
	 *  @return True, when the process instance is finished.
	 */
	public boolean isFinished()
	{
		return context.isFinished();
	}

	/**
	 *  Execute one step of the process.
	 */
	// Todo: What about diagrams with multiple pools/lanes etc?
	public void executeStep()
	{
		if(isFinished())
			throw new UnsupportedOperationException("Cannot execute a finished process: "+this);
		
		if(!isReady())
			throw new UnsupportedOperationException("Cannot execute a process with only waiting threads: "+this);
		
		ProcessThread	thread	= context.getExecutableThread();
		
		// Handle parameter passing in edge inscriptions.
		if(thread.getLastEdge()!=null && thread.getLastEdge().getParameterMappings()!=null)
		{
			Map mappings = thread.getLastEdge().getParameterMappings();
			if(mappings!=null)
			{
				IValueFetcher fetcher = new ProcessThreadValueFetcher(thread, false, this.fetcher);
				for(Iterator it2=mappings.keySet().iterator(); it2.hasNext(); )
				{
					String name = (String)it2.next();
					IParsedExpression exp = (IParsedExpression)mappings.get(name);
					Object value = exp.getValue(fetcher);
					thread.setParameterValue(name, value);
				}
			}
		}
		
		// Handle declared parameters with initial values.
		
		// todo: parameter direction / class
		
		List params = thread.getActivity().getParameters();
		if(params!=null)
		{	
			IValueFetcher fetcher = new ProcessThreadValueFetcher(thread, true, this.fetcher);
			for(int i=0; i<params.size(); i++)
			{
				MParameter param = (MParameter)params.get(i);
				if(!thread.hasParameterValue(param.getName()))
					thread.setParameterValue(param.getName(), param.getInitialval()==null? null: param.getInitialval().getValue(fetcher));
			}
		}
		
		
		// Find handler and execute activity.
		IActivityHandler handler = (IActivityHandler)handlers.get(thread.getActivity().getActivityType());
		if(handler==null)
			throw new UnsupportedOperationException("No handler for activity: "+thread);
		handler.execute(thread.getActivity(), this, thread);
	}
	
	/**
	 *  Check if the process is ready, i.e. if at least one process thread can currently execute a step.
	 */
	public boolean	isReady()
	{
		return context.getExecutableThread()!=null;
	}
	
	//-------- listener handling --------
	
	/**
	 *  Add a change listener.
	 *  The listener is informed, whenever the ready state of the process becomes true.
	 */
	public void	addChangeListener(IChangeListener listener)
	{
		if(listeners==null)
			listeners	= new ArrayList();
		
		listeners.add(listener);
	}

	
	/**
	 *  Remove a change listener.
	 */
	public void	removeChangeListener(IChangeListener listener)
	{
		if(listeners!=null)
		{
			listeners.remove(listener);
			if(listeners.isEmpty())
				listeners	= null;
		}
	}
	
	/**
	 *  Wake up the instance.
	 *  Called from activity handlers when external events re-activate waiting process threads.
	 *  Propagated to change listeners.
	 */
	public void	wakeUp()
	{
		if(listeners!=null)
		{
			ChangeEvent	ce	= new ChangeEvent(this, "ready");	
			for(int i=0; i<listeners.size(); i++)
			{
				((IChangeListener)listeners.get(i)).changeOccurred(ce);
			}
		}
	}
	
	/**
	 *  Get the activity handler for an activity.
	 *  @param actvity The activity.
	 *  @return The activity handler.
	 */
	public IActivityHandler getActivityHandler(MActivity activity)
	{
		return (IActivityHandler)handlers.get(activity.getActivityType());
	}

	/**
	 *  Get the string representation.
	 *  @return The string representation.
	 */
	public String toString()
	{
		StringBuffer buf = new StringBuffer();
		buf.append(SReflect.getInnerClassName(this.getClass()));
		buf.append("(name=");
		buf.append(getModelElement().getName());
		buf.append(", context=");
		buf.append(context);
		buf.append(")");
		return buf.toString();
	}

	/**
	 *  Get the global value fetcher.
	 *  @return The value fetcher (if any).
	 */
	public IValueFetcher getValueFetcher()
	{
		return this.fetcher;
	}
}
