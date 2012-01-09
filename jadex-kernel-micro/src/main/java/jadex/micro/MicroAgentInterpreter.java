package jadex.micro;

import jadex.bridge.ComponentChangeEvent;
import jadex.bridge.ComponentTerminatedException;
import jadex.bridge.IComponentChangeEvent;
import jadex.bridge.IComponentIdentifier;
import jadex.bridge.IComponentInstance;
import jadex.bridge.IComponentStep;
import jadex.bridge.IExternalAccess;
import jadex.bridge.IInternalAccess;
import jadex.bridge.IMessageAdapter;
import jadex.bridge.ITransferableStep;
import jadex.bridge.service.IServiceContainer;
import jadex.bridge.service.RequiredServiceBinding;
import jadex.bridge.service.types.clock.ITimer;
import jadex.bridge.service.types.cms.IComponentDescription;
import jadex.bridge.service.types.factory.IComponentAdapter;
import jadex.bridge.service.types.factory.IComponentAdapterFactory;
import jadex.commons.IValueFetcher;
import jadex.commons.SReflect;
import jadex.commons.Tuple2;
import jadex.commons.future.CounterResultListener;
import jadex.commons.future.DelegationResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.commons.future.IResultListener;
import jadex.javaparser.SJavaParser;
import jadex.javaparser.SimpleValueFetcher;
import jadex.kernelbase.AbstractInterpreter;
import jadex.kernelbase.InterpreterFetcher;
import jadex.micro.annotation.Agent;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 *  The micro agent interpreter is the connection between the agent platform 
 *  and a user-written micro agent. 
 */
public class MicroAgentInterpreter extends AbstractInterpreter
{
	/** Constant for step event. */
	public static final String TYPE_STEP = "step";
	
	//-------- attributes --------
	
	/** The micro agent. */
	protected MicroAgent microagent;
	
	/** The scheduled steps of the agent. */
	protected List steps;
	
	/** Flag indicating that no steps may be scheduled any more. */
	protected boolean nosteps;
	
	/** The list of message handlers. */
	protected List messagehandlers;
	
	/** The classloader (hack? should be in model). */
	protected ClassLoader classloader;
	
	//-------- constructors --------
	
	/**
	 *  Create a new agent.
	 *  @param adapter The adapter.
	 *  @param microagent The microagent.
	 */
	public MicroAgentInterpreter(IComponentDescription desc, IComponentAdapterFactory factory, 
		final MicroModel model, Class microclass, final Map args, final String config, 
		final IExternalAccess parent, RequiredServiceBinding[] bindings, boolean copy, final Future<Void> inited)
	{
		super(desc, model.getModelInfo(), config, factory, parent, bindings, copy, inited);
		
		try
		{
			this.classloader = model.getClassloader();
			this.microagent = createAgent(microclass, model);
			final Object agent = microagent instanceof IPojoMicroAgent? ((IPojoMicroAgent)microagent).getPojoAgent(): microagent;

			this.container = createMyServiceContainer(args);
						
			addStep((new Object[]{new IComponentStep<Void>()
			{
				public IFuture<Void> execute(IInternalAccess ia)
				{
					init(model.getModelInfo(), MicroAgentInterpreter.this.config, args)
						.addResultListener(createResultListener(new DelegationResultListener(inited)
					{
						public void customResultAvailable(Object result)
						{
							injectArguments(agent, model).addResultListener(new DelegationResultListener(inited)
							{
								public void customResultAvailable(Object result)
								{
									injectServices(agent, model).addResultListener(new DelegationResultListener(inited)
									{
										public void customResultAvailable(Object result)
										{
											// Call user code init.
											microagent.agentCreated().addResultListener(new DelegationResultListener(inited));
										}
									});
								}
							});
						}
					}));
					
					return IFuture.DONE;
				}
			}, new Future()}));
		}
		catch(Exception e)
		{
			inited.setException(e);
			if(e instanceof RuntimeException)
			{
				throw (RuntimeException)e;
			}
			else
			{
				throw new RuntimeException(e);
			}
		}
	}
	
	/**
	 * 
	 */
	protected MicroAgent createAgent(Class microclass, MicroModel model) throws Exception
	{
		MicroAgent ret = null;
		
		final Object agent = microclass.newInstance();
		if(agent instanceof MicroAgent)
		{
			ret = (MicroAgent)agent;
			ret.init(MicroAgentInterpreter.this);
		}
		else // if pojoagent
		{
			PojoMicroAgent pa = new PojoMicroAgent();
			pa.init(this, agent);
			ret = pa;

			Field[] fields = model.getAgentInjections();
			for(int i=0; i<fields.length; i++)
			{
				if(fields[i].isAnnotationPresent(Agent.class))
				{
					try
					{
						fields[i].setAccessible(true);
						fields[i].set(agent, ret);
					}
					catch(Exception e)
					{
						getLogger().warning("Agent injection failed: "+e);
					}
				}
			}
		}
		return ret;
	}
	
	/**
	 *  Inject the arguments to the annotated fields.
	 */
	protected IFuture<Void> injectArguments(final Object agent, final MicroModel model)
	{
		Future<Void> ret = new Future<Void>();

		String[] names = model.getArgumentInjectionNames();
		if(names.length>0)
		{
			for(int i=0; i<names.length; i++)
			{
				Object val = getArguments().get(names[i]);
				final Tuple2<Field, String>[] infos = model.getArgumentInjections(names[i]);
				
				try
				{
					for(int j=0; j<infos.length; j++)
					{
						Field field = infos[j].getFirstEntity();
						String convert = infos[j].getSecondEntity();
						if(val!=null || !SReflect.isBasicType(field.getType()))
						{
							if(convert!=null)
							{
								SimpleValueFetcher fetcher = new SimpleValueFetcher(getFetcher());
								fetcher.setValue("$value", val);
								val = SJavaParser.evaluateExpression(convert, fetcher);
							}
							field.setAccessible(true);
							field.set(agent, val);
						}
					}
				}
				catch(Exception e)
				{
					getLogger().warning("Field injection failed: "+e);
					ret.setException(e);
				}
			}
		}
		
		if(!ret.isDone())
			ret.setResult(null);
		
		return ret;
	}
	
	/**
	 *  Inject the services to the annotated fields.
	 */
	protected IFuture<Void> injectServices(final Object agent, final MicroModel model)
	{
		Future<Void> ret = new Future<Void>();

		String[] sernames = model.getServiceInjectionNames();
		
		if(sernames.length>0)
		{
			CounterResultListener lis = new CounterResultListener(sernames.length, new DelegationResultListener(ret));
	
			for(int i=0; i<sernames.length; i++)
			{
				final Field[] fields = model.getServiceInjections(sernames[i]);
				final Future fut = new Future();
				fut.addResultListener(lis);
				getServiceContainer().getRequiredService(sernames[i]).addResultListener(new DelegationResultListener(fut)
				{
					public void customResultAvailable(Object result)
					{
						try
						{
							for(int j=0; j<fields.length; j++)
							{
								fields[j].setAccessible(true);
								fields[j].set(agent, result);
							}
							fut.setResult(null);
						}
						catch(Exception e)
						{
							getLogger().warning("Field injection failed: "+e);
							fut.setException(e);
						}	
					}
				});
			}
		}
		else
		{
			ret.setResult(null);
		}
		
		return ret;
	}
	
	/**
	 *  Start the component behavior.
	 */
	public void startBehavior()
	{
//		System.out.println("started: "+getComponentIdentifier());
		scheduleStep(new IComponentStep<Void>()
		{
			public IFuture<Void> execute(IInternalAccess ia)
			{
//				System.out.println("body: "+getComponentAdapter().getComponentIdentifier());
				microagent.executeBody();
				return IFuture.DONE;
			}
			public String toString()
			{
				return "microagent.executeBody()_#"+this.hashCode();
			}
		});
	}
	
	/**
	 *  Get the value fetcher.
	 */
	public IValueFetcher getFetcher()
	{
		assert !getComponentAdapter().isExternalThread();
		
		if(fetcher==null)
		{
			SimpleValueFetcher fetcher = new SimpleValueFetcher(super.getFetcher());
			if(microagent instanceof IPojoMicroAgent)
			{
				fetcher.setValue("$pojoagent", ((IPojoMicroAgent)microagent).getPojoAgent());
			}
			this.fetcher = fetcher;
		}
		return fetcher;
	}
	
//	/**
//	 *  Override to set value also in fields.
//	 *  @param name The name.
//	 *  @param value The value.
//	 */
//	public boolean addArgument(String name, Object value)
//	{
//		boolean ret = super.addArgument(name, value);
//	
//		if(ret && value!=null)
//		{
//			Object agent = microagent instanceof PojoMicroAgent? ((PojoMicroAgent)microagent).getPojoAgent(): microagent;
//			
//			boolean found = false;
//			Class microclass = agent.getClass();
//			
//			while(!Object.class.equals(microclass) && !MicroAgent.class.equals(microclass) && !found)
//			{
//				Field[] fields = microclass.getDeclaredFields();
//				for(int i=0; i<fields.length && !found; i++)
//				{
//					if(fields[i].isAnnotationPresent(AgentArgument.class))
//					{
//						AgentArgument aa = (AgentArgument)fields[i].getAnnotation(AgentArgument.class);
//						String aname = aa.value().length()==0? fields[i].getName(): aa.value();
//						if(aname.equals(name))
//						{
//							String ce = aa.convert();
//							if(ce.length()>0)
//							{
//								SimpleValueFetcher fetcher = new SimpleValueFetcher(getFetcher());
//								fetcher.setValue("$value", value);
//								try
//								{
//									value = SJavaParser.evaluateExpression(ce, getModel().getAllImports(), fetcher, getModel().getClassLoader());
//								}
//								catch(Exception e)
//								{
//									getLogger().warning("Argument conversion failed: "+e);
//								}
//							}
//							if(SReflect.isSupertype(fields[i].getType(), value.getClass()))
//							{
//								try
//								{
//									fields[i].setAccessible(true);
//									fields[i].set(agent, value);
//									found = true;
////									System.out.println("set: "+agent+" "+fields[i].getName()+" "+value);
//								}
//								catch(Exception e)
//								{
//									getLogger().warning("Argument injection failed: "+e);
//								}
//							}
//							else
//							{
//								getLogger().warning("Wrong argument type: "+fields[i].getType()+" "+value.getClass());
//							}
//						}
//					}
//				}
//				microclass = microclass.getSuperclass();
//			}
//		}
//		
//		return ret;
//	}
	
	//-------- IKernelAgent interface --------
	
	/**
	 *  Can be called on the agent thread only.
	 * 
	 *  Main method to perform agent execution.
	 *  Whenever this method is called, the agent performs
	 *  one of its scheduled actions.
	 *  The platform can provide different execution models for agents
	 *  (e.g. thread based, or synchronous).
	 *  To avoid idle waiting, the return value can be checked.
	 *  The platform guarantees that executeAction() will not be called in parallel. 
	 *  @return True, when there are more actions waiting to be executed. 
	 */
	public boolean executeStep()
	{
		try
		{
			if(steps!=null && !steps.isEmpty())
			{
				Object[] step = removeStep();
				Future future = (Future)step[1];
				
				// Correct to execute them in try catch?!
				try
				{
					Object res = ((IComponentStep)step[0]).execute(microagent);
					if(res instanceof IFuture)
					{
						((IFuture)res).addResultListener(new DelegationResultListener(future));
					}
					else
					{
						future.setResult(res);
					}
				}
				catch(RuntimeException e)
				{
					future.setExceptionIfUndone(e);
					throw e;
				}
			}
	
			return steps!=null && !steps.isEmpty();
		}
		catch(ComponentTerminatedException ate)
		{
			// Todo: fix microkernel bug.
			ate.printStackTrace();
			return false; 
		}
	}

	/**
	 *  Can be called concurrently (also during executeAction()).
	 *  
	 *  Inform the agent that a message has arrived.
	 *  Can be called concurrently (also during executeAction()).
	 *  @param message The message that arrived.
	 */
	public void messageArrived(final IMessageAdapter message)
	{
//		System.out.println("msgrec: "+getAgentAdapter().getComponentIdentifier()+" "+message);
//		IFuture ret = scheduleStep(new ICommand()
		scheduleStep(new HandleMessageStep(message));
	}

	/**
	 *  Can be called concurrently (also during executeAction()).
	 *   
	 *  Request agent to kill itself.
	 *  The agent might perform arbitrary cleanup activities during which executeAction()
	 *  will still be called as usual.
	 *  Can be called concurrently (also during executeAction()).
	 *  @param listener	When cleanup of the agent is finished, the listener must be notified.
	 */
	public IFuture cleanupComponent()
	{
		final Future ret = new Future();
		
		try
		{
			getAgentAdapter().invokeLater(new Runnable()
			{
				public void run()
				{	
					exitState();
					
					ComponentChangeEvent.dispatchTerminatingEvent(adapter, getCreationTime(), getModel(), getServiceProvider(), componentlisteners, null);
					
					microagent.agentKilled().addResultListener(microagent.createResultListener(new IResultListener()
					{
						public void resultAvailable(Object result)
						{
							nosteps = true;
							exitState();
							
							ComponentChangeEvent.dispatchTerminatedEvent(adapter, getCreationTime(), getModel(), getServiceProvider(), componentlisteners, null);
							
							IComponentIdentifier cid = adapter.getComponentIdentifier();
							ret.setResult(cid);							
						}
						
						public void exceptionOccurred(Exception exception)
						{
							StringWriter	sw	= new StringWriter();
							exception.printStackTrace(new PrintWriter(sw));
							microagent.getLogger().severe("Exception during cleanup: "+sw);
							resultAvailable(null);
						}
					}));
				}
				
				public String toString()
				{
					return "microagent.agentKilled()_#"+this.hashCode();
				}
			});
		}
		catch(Exception e)
		{
			ret.setException(e);
		}
		
		return ret;
	}
	
	/**
	 *  Test if the component's execution is currently at one of the
	 *  given breakpoints. If yes, the component will be suspended by
	 *  the platform.
	 *  @param breakpoints	An array of breakpoints.
	 *  @return True, when some breakpoint is triggered.
	 */
	public boolean isAtBreakpoint(String[] breakpoints)
	{
		return microagent.isAtBreakpoint(breakpoints);
	}
	
	//-------- helpers --------
	
	/**
	 *  Schedule a step of the agent.
	 *  May safely be called from external threads.
	 *  @param step	Code to be executed as a step of the agent.
	 */
	public <T> IFuture<T> scheduleStep(final IComponentStep<T> step)
	{
		final Future<T> ret = new Future<T>();
//		System.out.println("ss: "+getAgentAdapter().getComponentIdentifier()+" "+Thread.currentThread()+" "+step);
		try
		{
			if(adapter.isExternalThread())
			{
				adapter.invokeLater(new Runnable()
				{			
					public void run()
					{
						addStep(new Object[]{step, ret});
					}
					
					public String toString()
					{
						return "invokeLater("+step+")";
					}
				});
			}
			else
			{
				addStep(new Object[]{step, ret});
			}
		}
		catch(Exception e)
		{
			ret.setException(e);
		}
		return ret;
	}

	/**
	 *  Add a new step.
	 */
	protected void addStep(Object[] step)
	{
		if(nosteps)
		{
			((Future)step[1]).setException(new ComponentTerminatedException(getAgentAdapter().getComponentIdentifier()));
		}
		else
		{
			if(steps==null)
				steps	= new ArrayList();
			steps.add(step);
			if(componentlisteners!=null)
			{
				notifyListeners(new ComponentChangeEvent(IComponentChangeEvent.EVENT_TYPE_CREATION, TYPE_STEP, step[0].getClass().getName(), 
					step[0].toString(), microagent.getComponentIdentifier(), getCreationTime(), getStepDetails((IComponentStep)step[0])));
			}
		}
	}
	
	/**
	 *  Add a new step.
	 */
	protected Object[] removeStep()
	{
		assert steps!=null && !steps.isEmpty();
		Object[] ret = (Object[])steps.remove(0);
		if(steps.isEmpty())
			steps	= null;
		if(componentlisteners!=null)
		{
			notifyListeners(new ComponentChangeEvent(IComponentChangeEvent.EVENT_TYPE_DISPOSAL, TYPE_STEP, ret[0].getClass().getName(),
				ret[0].toString(), microagent.getComponentIdentifier(), getCreationTime(), getStepDetails((IComponentStep)ret[0])));
		}
//		notifyListeners(new ChangeEvent(this, "removeStep", new Integer(0)));
		return ret;
	}
	
	/**
	 *  Add an action from external thread.
	 *  The contract of this method is as follows:
	 *  The agent ensures the execution of the external action, otherwise
	 *  the method will throw a agent terminated sexception.
	 *  @param action The action.
	 * /
	public void invokeLater(Runnable action)
	{
		synchronized(ext_entries)
		{
			if(ext_forbidden)
				throw new ComponentTerminatedException("External actions cannot be accepted " +
					"due to terminated agent state: "+this);
			{
				ext_entries.add(action);
			}
		}
		adapter.wakeup();
	}*/
	
	/**
	 *  Invoke some code with agent behaviour synchronized on the agent.
	 *  @param code The code to execute.
	 *  The method will block the externally calling thread until the
	 *  action has been executed on the agent thread.
	 *  If the agent does not accept external actions (because of termination)
	 *  the method will directly fail with a runtime exception.
	 *  Note: 1.4 compliant code.
	 *  Problem: Deadlocks cannot be detected and no exception is thrown.
	 * /
	public void invokeSynchronized(final Runnable code)
	{
		if(isExternalThread())
		{
//			System.err.println("Unsynchronized internal thread.");
//			Thread.dumpStack();

			final boolean[] notified = new boolean[1];
			final RuntimeException[] exception = new RuntimeException[1];
			
			// Add external will throw exception if action execution cannot be done.
//			System.err.println("invokeSynchonized("+code+"): adding");
			getAgentAdapter().invokeLater(new Runnable()
			{
				public void run()
				{
					try
					{
						code.run();
					}
					catch(RuntimeException e)
					{
						exception[0]	= e;
					}
					
					synchronized(notified)
					{
						notified.notify();
						notified[0] = true;
					}
				}
				
				public String toString()
				{
					return code.toString();
				}
			});
			
			try
			{
//				System.err.println("invokeSynchonized("+code+"): waiting");
				synchronized(notified)
				{
					if(!notified[0])
					{
						notified.wait();
					}
				}
//				System.err.println("invokeSynchonized("+code+"): returned");
			}
			catch(InterruptedException e)
			{
				e.printStackTrace();
			}
			if(exception[0]!=null)
				throw exception[0];
		}
		else
		{
			System.err.println("Method called from internal agent thread.");
			Thread.dumpStack();
			code.run();
		}
	}*/
	
	/**
	 *  Get the agent adapter.
	 *  @return The agent adapter.
	 */
	public IComponentAdapter getAgentAdapter()
	{
		return getComponentAdapter();
	}

//	/**
//	 *  Create the service container.
//	 *  @return The service container.
//	 */
//	public IServiceContainer getServiceContainer()
//	{
//		if(container==null)
//		{
//			container = microagent.createServiceContainer();
//		}
//		return container;
//	}

	/**
	 *  Add a message handler.
	 *  @param  The handler.
	 */
	public void addMessageHandler(final IMessageHandler handler)
	{
		if(handler.getFilter()==null)
			throw new RuntimeException("Filter must not null in handler: "+handler);
			
		if(messagehandlers==null)
		{
			messagehandlers = new ArrayList();
		}
		if(handler.getTimeout()>0)
		{
			microagent.waitFor(handler.getTimeout(), new IComponentStep<Void>()
			{
				public IFuture<Void> execute(IInternalAccess ia)
				{
					handler.timeoutOccurred();
					if(handler.isRemove())
					{
						removeMessageHandler(handler);
					}
					return IFuture.DONE;
				}
			});
		}
		messagehandlers.add(handler);
	}
	
	/**
	 *  Remove a message handler.
	 *  @param handler The handler.
	 */
	public void removeMessageHandler(IMessageHandler handler)
	{
		if(messagehandlers!=null)
		{
			messagehandlers.remove(handler);
		}
	}
	
	/**
	 *  Exit the running or end state.
	 *  Cleans up remaining steps and timer entries.
	 */
	protected void exitState()
	{
//		System.out.println("cleanupComponent: "+getAgentAdapter().getComponentIdentifier());
		ComponentTerminatedException ex = new ComponentTerminatedException(getAgentAdapter().getComponentIdentifier());
		while(steps!=null && !steps.isEmpty())
		{
			Object[] step = removeStep();
			Future future = (Future)step[1];
			future.setException(ex);
//			System.out.println("Cleaning obsolete step: "+getAgentAdapter().getComponentIdentifier()+", "+step[0]);
		}
		
		if(microagent.timers!=null)
		{
			for(int i=0; i<microagent.timers.size(); i++)
			{
				ITimer timer = (ITimer)microagent.timers.get(i);
				timer.cancel();
			}
			microagent.timers.clear();
		}
	}

	/**
	 *  Get the internal access.
	 */
	public IInternalAccess getInternalAccess()
	{
		return microagent;
	}
	
	/**
	 *  Can be called concurrently (also during executeAction()).
	 * 
	 *  Get the external access for this component.
	 *  The specific external access interface is kernel specific
	 *  and has to be casted to its corresponding incarnation.
	 *  @param listener	External access is delivered via result listener.
	 */
	public IExternalAccess getExternalAccess()
	{
		if(access==null)
		{
			synchronized(this)
			{
				if(access==null)
				{
					access	= new ExternalAccess(microagent, this);
				}
			}
		}
		
		return access;
	}
	
	/**
	 *  Create the service container.
	 *  @return The service conainer.
	 */
	public IServiceContainer createServiceContainer()
	{
		// Overridden to ensure that super call does nothing.
		// Container init must be done when microagent has
		// already been created. Otherwise createServiceContainer
		// cannot be delegated.
		return null;
	}
	
	/**
	 *  Create the service container.
	 *  @return The service conainer.
	 */
	public IServiceContainer createMyServiceContainer(Map args)
	{
		IServiceContainer ret = microagent.createServiceContainer(args);
		if(ret==null)
			ret = super.createServiceContainer();
		return ret;
	}
	
	/**
	 *  Step to handle a message.
	 */
	public static class HandleMessageStep implements IComponentStep<Void>
	{
		private final IMessageAdapter message;

		public static final String XML_CLASSNAME = "msg";

		public HandleMessageStep(IMessageAdapter message)
		{
			this.message = message;
		}

		public IFuture<Void> execute(IInternalAccess ia)
		{
			MicroAgent	microagent	= (MicroAgent)ia;
			MicroAgentInterpreter	ip	= microagent.interpreter;
			
			boolean done = false;
			if(ip.messagehandlers!=null)
			{
				for(int i=0; i<ip.messagehandlers.size(); i++)
				{
					IMessageHandler mh = (IMessageHandler)ip.messagehandlers.get(i);
					if(mh.getFilter().filter(message))
					{
						mh.handleMessage(message.getParameterMap(), message.getMessageType());
						if(mh.isRemove())
						{
							ip.messagehandlers.remove(i);
						}
						done = true;
					}
				}
			}
			
			if(!done)
			{
				microagent.messageArrived(Collections.unmodifiableMap(message.getParameterMap()), message.getMessageType());
			}
			return IFuture.DONE;
		}

		public String toString()
		{
			return "microagent.messageArrived()_#"+this.hashCode();
		}
	}
	
	/**
	 *  Get the details of a step.
	 */
	public Object getStepDetails(IComponentStep step)
	{
		Object	ret;
		
		if(step instanceof MicroAgent.ExecuteWaitForStep)
		{
			MicroAgent.ExecuteWaitForStep waitForStep = (MicroAgent.ExecuteWaitForStep) step;
			if(waitForStep.getComponentStep() instanceof ITransferableStep)
			{
				ret = ((ITransferableStep) waitForStep.getComponentStep()).getTransferableObject();
				return ret;
			}
		}
		
		StringBuffer buf = new StringBuffer();

		buf.append("Class = ").append(SReflect.getClassName(step.getClass()));

		Field[] fields = step.getClass().getDeclaredFields();
		for (int i = 0; i < fields.length; i++) {
			String valtext = null;
			try {
				fields[i].setAccessible(true);
				Object val = fields[i].get(step);
				valtext = val == null ? "null" : val.toString();
			} catch (Exception e) {
				valtext = e.getMessage();
			}

			if (valtext != null) {
				buf.append("\n");
				buf.append(fields[i].getName()).append(" = ").append(valtext);
			}
		}

		ret = buf.toString();
			
		return ret;
	}
	
	/**
	 *  Get the class loader of the component.
	 *  The component class loader is required to avoid incompatible class issues,
	 *  when changing the platform class loader while components are running. 
	 *  This may occur e.g. when decoding messages and instantiating parameter values.
	 *  @return	The component class loader. 
	 */
	public ClassLoader getClassLoader()
	{
		return classloader;
	}
}
