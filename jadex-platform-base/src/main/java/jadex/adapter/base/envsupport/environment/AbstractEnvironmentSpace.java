package jadex.adapter.base.envsupport.environment;

import jadex.adapter.base.envsupport.environment.agentaction.IActionExecutor;
import jadex.adapter.base.envsupport.environment.agentaction.IAgentAction;
import jadex.adapter.base.envsupport.environment.agentaction.ImmediateExecutor;
import jadex.bridge.IAgentIdentifier;
import jadex.commons.concurrent.IResultListener;
import jadex.adapter.base.envsupport.environment.view.IView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *  
 */
public abstract class AbstractEnvironmentSpace extends PropertyHolder 
											   implements IEnvironmentSpace
{
	//-------- attributes --------
	
	/** Available space actions. */
	protected Map spaceactions;
	
	/** Available views */
	protected Map views;
	
	/** Available agent actions. */
	protected Map agentactions;
	
	/** The environment processes. */
	protected Map processes;

	/** Long/ObjectIDs (keys) and environment objects (values). */
	protected Map spaceobjects;
	
	/** Types of EnvironmentObjects and lists of EnvironmentObjects of that type (typed view). */
	protected Map spaceobjectsbytype;
	
	/** Space object by owner, owner can null (owner view). */
	protected Map spaceobjectsbyowner;
	
	/** Object id counter for new ids. */
	protected AtomicCounter objectidcounter;
	
	/** The environment listeners. */
	protected List listeners;
	
	//-------- constructors --------
	
	/**
	 *  Create an environment space
	 */
	public AbstractEnvironmentSpace()
	{
		this.views = new HashMap();
		this.spaceactions = new HashMap();
		this.agentactions = new HashMap();
		this.processes = new HashMap();
		this.spaceobjects = new HashMap();
		this.spaceobjectsbytype = new HashMap();
		this.spaceobjectsbyowner = new HashMap();
		this.objectidcounter = new AtomicCounter();
	}
	
	//-------- methods --------
	
	/**
	 * Adds a space process.
	 * @param id ID of the space process
	 * @param process new space process
	 */
	public void addSpaceProcess(Object id, ISpaceProcess process)
	{
		synchronized(monitor)
		{
			processes.put(id, process);
			process.start(this);
		}
	}

	/**
	 * Returns a space process.
	 * @param id ID of the space process
	 * @return the space process or null if not found
	 */
	public ISpaceProcess getSpaceProcess(Object id )
	{
		synchronized(monitor)
		{
			return (ISpaceProcess)processes.get(id);
		}
	}

	/**
	 * Removes a space process.
	 * @param id ID of the space process
	 */
	public void removeSpaceProcess(final Object id)
	{
		synchronized(monitor)
		{
			ISpaceProcess process = (ISpaceProcess)processes.remove(id);
			if(process!=null)
				process.shutdown(this);
		}
	}
	
	/** 
	 * Creates an object in this space.
	 * @param type the object's type
	 * @param properties initial properties (may be null)
	 * @param tasks initial task list (may be null)
	 * @param listeners initial listeners (may be null)
	 * @return the object's ID
	 */
	public ISpaceObject createSpaceObject(Object type, Object owner, Map properties, List tasks, List listeners)
	{
		ISpaceObject ret;
		
		synchronized(monitor)
		{
			Object id;
			do
			{
				id = objectidcounter.getNext();
			}
			while(spaceobjects.containsKey(id));
			
			ret = new SpaceObject(id, type, owner, properties, tasks, listeners, monitor);
			spaceobjects.put(id, ret);
			List typeobjects = (List)spaceobjectsbytype.get(ret.getType());
			if(typeobjects == null)
			{
				typeobjects = new ArrayList();
				spaceobjectsbytype.put(ret.getType(), typeobjects);
			}
			typeobjects.add(ret);
			
			if(owner!=null)
			{
				List ownerobjects = (List)spaceobjectsbyowner.get(owner);
				if(ownerobjects == null)
				{
					ownerobjects = new ArrayList();
					spaceobjectsbyowner.put(owner, ownerobjects);
				}
				ownerobjects.add(ret);
			}
		}
		
		if(listeners!=null)
		{
			EnvironmentEvent event = new EnvironmentEvent(EnvironmentEvent.OBJECT_CREATED, this, ret);
			for(int i=0; i<listeners.size(); i++)
			{
				IEnvironmentListener lis = (IEnvironmentListener)listeners.get(i);
				if(lis.isRelevant(event))
					lis.dispatchEnvironmentEvent(event);
			}
		}
		
		return ret;
	}
	
	/** 
	 * Destroys an object in this space.
	 * @param id the object's ID
	 */
	public void destroySpaceObject(final Object id)
	{
		ISpaceObject obj;
		synchronized(monitor)
		{
			obj = (ISpaceObject)spaceobjects.get(id);
			// shutdown and jettison tasks
			obj.clearTasks();

			// remove object
			spaceobjects.remove(id);
			List typeobjs = (List)spaceobjectsbytype.get(obj.getType());
			typeobjs.remove(obj);
			if(typeobjs.size()==0)
				spaceobjectsbytype.remove(obj.getType());
			
			if(obj.getProperty(ISpaceObject.OWNER)!=null)
			{
				List ownedobjs = (List)spaceobjectsbyowner.get(obj.getProperty(ISpaceObject.OWNER));
				ownedobjs.remove(obj);
				if(ownedobjs.size()==0)
					spaceobjectsbyowner.remove(obj.getProperty(ISpaceObject.OWNER));
			}
		}
		
		// signal removal
		// hmm? what about calling destroy on object? could it do sth. else than throwing event?
		ObjectEvent event = new ObjectEvent(ObjectEvent.OBJECT_REMOVED);
		event.setParameter("space_name", getName());
		obj.fireObjectEvent(event);
		
		if(listeners!=null)
		{
			EnvironmentEvent ev = new EnvironmentEvent(EnvironmentEvent.OBJECT_DESTRYOED, this, obj);
			for(int i=0; i<listeners.size(); i++)
			{
				IEnvironmentListener lis = (IEnvironmentListener)listeners.get(i);
				if(lis.isRelevant(ev))
					lis.dispatchEnvironmentEvent(ev);
			}
		}
	}
	
	/**
	 * Returns an object in this space.
	 * @param id the object's ID
	 * @return the object in this space
	 */
	public ISpaceObject getSpaceObject(Object id)
	{
		synchronized(monitor)
		{
			return (ISpaceObject)spaceobjects.get(id);
		}
	}
	
	/**
	 * Get all space object of a specific type.
	 * @param type The space object type.
	 * @return The space objects of the desired type.
	 */
	public ISpaceObject[] getSpaceObjectsByType(Object type)
	{
		List obs = (List)spaceobjectsbytype.get(type);
		return obs==null? new ISpaceObject[0]: (ISpaceObject[])obs.toArray(new ISpaceObject[obs.size()]); 
	}
	
	/**
	 * Adds a space action.
	 * @param actionId the action ID
	 * @param action the action
	 */
	public void addSpaceAction(Object id, ISpaceAction action)
	{
		synchronized(monitor)
		{
			spaceactions.put(id, action);
		}
	}
	
	/**
	 * Removes a space action.
	 * @param id the action ID
	 */
	public void removeSpaceAction(final Object id)
	{
		synchronized(monitor)
		{	
			spaceactions.remove(id);
		}
	}
	
	/**
	 * Performs an environment action.
	 * @param id ID of the action
	 * @param parameters parameters for the action (may be null)
	 * @return return value of the action
	 */
	public Object performSpaceAction(final Object id, final Map parameters)
	{
		synchronized(monitor)
		{
			ISpaceAction action = (ISpaceAction) spaceactions.get(id);
			assert action != null;
			return action.perform(parameters, this);
		}
	}
	
	/**
	 * Adds an agent action.
	 * @param actionId the action ID
	 * @param action the action
	 */
	public void addAgentAction(Object id, IAgentAction action)
	{
		synchronized(monitor)
		{
			agentactions.put(id, action);
		}
	}
	
	/**
	 * Removes an agent action.
	 * @param actionId the action ID
	 */
	public void removeAgentAction(Object id)
	{
		synchronized(monitor)
		{	
			agentactions.remove(id);
		}
	}
	
	/**
	 * Schedules an agent action.
	 * @param id Id of the action
	 * @param parameters parameters for the action (may be null)
	 * @param listener the result listener
	 */
	public void performAgentAction(final Object id, final Map parameters, final IResultListener listener)
	{
		synchronized(monitor)
		{
			IActionExecutor executor = (IActionExecutor)processes.get(IActionExecutor.DEFAULT_EXECUTOR_NAME);
			if(executor ==null)
			{
				executor = new ImmediateExecutor();
				addSpaceProcess(IActionExecutor.DEFAULT_EXECUTOR_NAME, executor);
				System.out.println("No agent action executor defined, using immediate execution as default.");
			}
			executor.getSynchronizer().invokeLater(new Runnable()
			{
				public void run()
				{
					IAgentAction action = (IAgentAction)agentactions.get(id);
					Object ret = action.perform(new HashMap(parameters), AbstractEnvironmentSpace.this);
					listener.resultAvailable(ret);
				}
			});
		}
	}
	
	/**
	 * Returns the space's name.
	 * @return the space's name.
	 */
	public String getName()
	{
		synchronized(monitor)
		{
			return (String)getProperty("name");
		}
	}
	
	/**
	 * Returns the space's name.
	 * @return the space's name.
	 */
	public void setName(final String name)
	{
		synchronized(monitor)
		{
			setProperty("name", name);
		}
	}
	
	/**
	 *  Get the owner of an object.
	 *  @param id The id.
	 *  @return The owner.
	 * /
	public Object getOwner(Object id)
	{
		synchronized(getSynchronizedObject().getMonitor())
		{
			ISpaceObject obj = getSpaceObject(id); 
			if(obj==null)
				throw new RuntimeException("Space object not found: "+id);
			return obj.getProperty(ISpaceObject.OWNER);
		}
	}*/
	
	/**
	 *  Set the owner of an object.
	 *  @param id The object id.
	 *  @param pos The object owner.
	 */
	public void setOwner(Object id, Object owner)
	{
		synchronized(monitor)
		{
			ISpaceObject obj = getSpaceObject(id); 
			if(obj==null)
				throw new RuntimeException("Space object not found: "+id);
			Object oldowner = obj.getProperty(ISpaceObject.OWNER);
			if(oldowner!=null)
			{
				List ownedobjs = (List)spaceobjectsbyowner.get(oldowner);
				ownedobjs.remove(obj);
				if(ownedobjs.size()==0)
					spaceobjectsbyowner.remove(oldowner);
			}
			if(owner!=null)
			{
				List ownedobjs = (List)spaceobjectsbyowner.get(owner);
				if(ownedobjs==null)
				{
					ownedobjs = new ArrayList();
					spaceobjectsbyowner.put(owner, ownedobjs);
				}
				ownedobjs.add(obj);
			}
			obj.setProperty(ISpaceObject.OWNER, owner);
		}
	}
	
	/**
	 *  Get the owned objects.
	 *  @return The owned objects. 
	 */
	public ISpaceObject[] getOwnedObjects(Object owner)
	{
		synchronized(monitor)
		{
			List ownedobjs = (List)spaceobjectsbyowner.get(owner);
			return ownedobjs==null? new ISpaceObject[0]: (ISpaceObject[])ownedobjs.toArray(new ISpaceObject[ownedobjs.size()]);
		}
	}
	
	/**
	 * Adds a view to the space.
	 * @param name name of the view
	 * @param view the view
	 */
	public void addView(String name, IView view)
	{
		synchronized (monitor)
		{
			views.put(name, view);
		}
	}
	
	/**
	 * Removes a view from the space.
	 * @param name name of the view
	 */
	public void removeView(String name)
	{
		synchronized (monitor)
		{
			views.remove(name);
		}
	}
	
	/**
	 * Gets a specific view.
	 * @param name name of the view
	 * @return the view
	 */
	public IView getView(String name)
	{
		synchronized (monitor)
		{
			return (IView) views.get(name);
		}
	}
	
	/**
	 * Get all available views in this space.
	 * @return list of view names
	 */
	public List getViewNames()
	{
		synchronized (monitor)
		{
			return new ArrayList(views.values());
		}
	}
	
	/**
	 *  Add an environment listener.
	 *  @param listener The environment listener. 
	 */
	public void addEnvironmentListener(IEnvironmentListener listener)
	{
		synchronized(monitor)
		{
			if(listeners==null)
				listeners = new ArrayList();
			listeners.add(listener);
		}
	}
	
	/**
	 *  Remove an environment listener.
	 *  @param listener The environment listener. 
	 */
	public void removeEnvironmentListener(IEnvironmentListener listener)
	{
		synchronized(monitor)
		{
			listeners.remove(listener);
			if(listeners.size()==0)
				listeners = null;
		}
	}
	
	//-------- ISpace methods --------
	
	/**
	 *  Called when an agent was added. 
	 */
	public void agentAdded(IAgentIdentifier aid)
	{
	}
	
	/**
	 *  Called when an agent was removed.
	 */
	public void agentRemoved(IAgentIdentifier aid)
	{
		// Remove the owned object too?
	}
	
	/**
	 *  Synchronized counter class
	 */
	private class AtomicCounter
	{
		long count_;
		
		public AtomicCounter()
		{
			count_ = 0;
		}
		
		public synchronized Long getNext()
		{
			return new Long(count_++);
		}
	}
}
