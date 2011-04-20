package jadex.bdi.runtime.impl.flyweights;

import jadex.bdi.model.IMElement;
import jadex.bdi.model.impl.flyweights.MCapabilityFlyweight;
import jadex.bdi.runtime.IBDIExternalAccess;
import jadex.bdi.runtime.interpreter.OAVBDIRuntimeModel;
import jadex.bridge.IComponentIdentifier;
import jadex.bridge.IComponentStep;
import jadex.bridge.IModelInfo;
import jadex.bridge.service.IServiceProvider;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.bridge.service.SServiceProvider;
import jadex.bridge.service.clock.IClockService;
import jadex.bridge.service.clock.ITimedObject;
import jadex.commons.SUtil;
import jadex.commons.future.DelegationResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.rules.state.IOAVState;

import java.util.Collection;
import java.util.Iterator;
import java.util.StringTokenizer;

/**
 *  External access interface.
 */
public class ExternalAccessFlyweight extends ElementFlyweight implements IBDIExternalAccess
{
	//-------- attributes --------
	
	/** The service provider. */
	protected IServiceProvider provider;
	
	/** The component identifier. */
	protected IComponentIdentifier	cid;
	
	/** The parent component identifier. */
	protected IComponentIdentifier	parent;

	//-------- constructors --------
	
	/**
	 *  Create a new capability flyweight.
	 *  @param state	The state.
	 *  @param scope	The scope handle.
	 *  @param adapter	The adapter.
	 */
	public ExternalAccessFlyweight(IOAVState state, Object scope)
	{
		super(state, scope, scope);
		this.provider = getInterpreter().getServiceProvider();
		this.cid = getInterpreter().getAgentAdapter().getComponentIdentifier();
		this.parent = getInterpreter().getParent().getComponentIdentifier();
	}

	//-------- methods --------
	
	/**
	 *  Get the model of the component.
	 *  @return	The model.
	 */
	public IModelInfo	getModel()
	{
		return getInterpreter().getModel().getModelInfo();
	}

	/**
	 *  Get the parent (if any).
	 *  @return The parent.
	 */
	public IComponentIdentifier getParent()
	{
		return parent;
	}
	
	/**
	 *  Get the id of the component.
	 *  @return	The component id.
	 */
	public IComponentIdentifier	getComponentIdentifier()
	{
		return cid;
	}
	
	/**
	 *  Get the service provider.
	 *  @return The service provider.
	 */
	public IServiceProvider getServiceProvider()
	{
		return provider;
	}
	
	/**
	 *  Schedule a step of the agent.
	 *  May safely be called from external threads.
	 *  @param step	Code to be executed as a step of the agent.
	 *  @return The result of the step.
	 */
	public IFuture scheduleStep(IComponentStep step)
	{
		return getInterpreter().scheduleStep(step, getHandle());
	}
	
	/**
	 *  Schedule an immediate step of the agent.
	 *  May safely be called from external threads.
	 *  @param step	Code to be executed as a step of the agent.
	 *  @return The result of the step.
	 */
	public IFuture scheduleImmediate(IComponentStep step)
	{
		return getInterpreter().scheduleImmediate(step, getHandle());
	}
	
	/**
	 *  Schedule a step of the component.
	 *  May safely be called from external threads.
	 *  @param step	Code to be executed as a step of the component.
	 *  @param delay The delay to wait before step should be done.
	 *  @return The result of the step.
	 */
	public IFuture scheduleStep(final IComponentStep step, final long delay)
	{
		final Future ret = new Future();
		
		SServiceProvider.getService(getInterpreter().getServiceContainer(), IClockService.class, RequiredServiceInfo.SCOPE_PLATFORM)
			.addResultListener(getInterpreter().createResultListener(new DelegationResultListener(ret)
		{
			public void customResultAvailable(Object result)
			{
				IClockService cs = (IClockService)result;
				cs.createTimer(delay, new ITimedObject()
				{
					public void timeEventOccurred(long currenttime)
					{
						scheduleStep(step).addResultListener(new DelegationResultListener(ret));
					}
				});
			}
		}));
		
		return ret;
	}
	
	/**
	 *  Execute some code on the component's thread.
	 *  Unlike scheduleStep(), the action will also be executed
	 *  while the component is suspended.
	 *  @param action	Code to be executed on the component's thread.
	 *  @param delay The delay to wait before step should be done.
	 *  @return The result of the step.
	 */
	public IFuture scheduleImmediate(final IComponentStep step, final long delay)
	{
		final Future ret = new Future();
		
		SServiceProvider.getService(getInterpreter().getServiceContainer(), IClockService.class, RequiredServiceInfo.SCOPE_PLATFORM)
			.addResultListener(getInterpreter().createResultListener(new DelegationResultListener(ret)
		{
			public void customResultAvailable(Object result)
			{
				IClockService cs = (IClockService)result;
				cs.createTimer(delay, new ITimedObject()
				{
					public void timeEventOccurred(long currenttime)
					{
						scheduleImmediate(step).addResultListener(new DelegationResultListener(ret));
					}
				});
			}
		}));
		
		return ret;
	}
	
	//-------- normal --------
	
	/**
	 *  Get the children (if any).
	 *  @return The children.
	 */
	public IFuture getChildren()
	{
		return getInterpreter().getAgentAdapter().getChildrenIdentifiers();
	}
	
	/**
	 *  Kill the component.
	 */
	public IFuture killComponent()
	{
		final Future ret = new Future();
		
		if(getInterpreter().isExternalThread())
		{
			getInterpreter().getAgentAdapter().invokeLater(new Runnable()
			{
				public void run()
				{
					Object cs = getState().getAttributeValue(getInterpreter().getAgent(), OAVBDIRuntimeModel.agent_has_state);
					if(OAVBDIRuntimeModel.AGENTLIFECYCLESTATE_INITING0.equals(cs) 
						|| OAVBDIRuntimeModel.AGENTLIFECYCLESTATE_INITING1.equals(cs)
						|| OAVBDIRuntimeModel.AGENTLIFECYCLESTATE_ALIVE.equals(cs))
					{
						getInterpreter().killAgent().addResultListener(new DelegationResultListener(ret));
					}
				}
			});
		}
		else
		{
			Object cs = getState().getAttributeValue(getInterpreter().getAgent(), OAVBDIRuntimeModel.agent_has_state);
			if(OAVBDIRuntimeModel.AGENTLIFECYCLESTATE_INITING0.equals(cs) 
				|| OAVBDIRuntimeModel.AGENTLIFECYCLESTATE_INITING1.equals(cs)
				|| OAVBDIRuntimeModel.AGENTLIFECYCLESTATE_ALIVE.equals(cs))
			{
				//	System.out.println("set to terminating");
				getInterpreter().startMonitorConsequences();
				getInterpreter().killAgent().addResultListener(new DelegationResultListener(ret));
				getInterpreter().endMonitorConsequences();
			}
		}
		
		return ret;
	}
	
	/**
	 *  Get external access of subcapability.
	 *  @param name The capability name.
	 *  @return The future with external access.
	 */
	public IFuture getExternalAccess(final String name)
	{
		final Future ret = new Future();
		
		if(!getInterpreter().isPlanThread())
		{
			getInterpreter().getAgentAdapter().invokeLater(new Runnable() 
			{
				public void run() 
				{
					StringTokenizer stok = new StringTokenizer(name, ".");
					Object handle = getHandle();
					while(stok.hasMoreTokens())
					{
						String subcapname = stok.nextToken();
						Object subcapref = getState().getAttributeValue(handle, OAVBDIRuntimeModel.capability_has_subcapabilities, subcapname);
						if(subcapref==null)
						{
							ret.setException(new RuntimeException("Capability not found: "+subcapname));
							return;
						}
						handle = getState().getAttributeValue(subcapref, OAVBDIRuntimeModel.capabilityreference_has_capability);
					}
					ret.setResult(new ExternalAccessFlyweight(getState(), handle));
				}
			});
		}
		else
		{
			StringTokenizer stok = new StringTokenizer(name, ".");
			Object handle = getHandle();
			while(stok.hasMoreTokens())
			{
				String subcapname = stok.nextToken();
				Object subcapref = getState().getAttributeValue(handle, OAVBDIRuntimeModel.capability_has_subcapabilities, subcapname);
				if(subcapref==null)
				{
					ret.setException(new RuntimeException("Capability not found: "+subcapname));
					return ret;
				}
				handle = getState().getAttributeValue(subcapref, OAVBDIRuntimeModel.capabilityreference_has_capability);
			}
			ret.setResult(new ExternalAccessFlyweight(getState(), handle));
		}
		
		return ret;
	}
	
	/**
	 *  Get subcapability names.
	 *  @return The future with array of subcapability names.
	 */
	public IFuture getSubcapabilityNames()
	{
		final Future ret = new Future();
		
		if(!getInterpreter().isPlanThread())
		{
			getInterpreter().getAgentAdapter().invokeLater(new Runnable() 
			{
				public void run() 
				{
					String[] res = SUtil.EMPTY_STRING_ARRAY;
					Collection coll = getState().getAttributeValues(getHandle(), OAVBDIRuntimeModel.capability_has_subcapabilities);
					if(coll!=null)
					{
						res = new String[coll.size()];
						int i=0;
						for(Iterator it=coll.iterator(); it.hasNext(); i++)
						{
							Object cref = it.next();
							String name = (String)getState().getAttributeValue(cref, OAVBDIRuntimeModel.capabilityreference_has_name);
							res[i] = name;
						}
					}
					ret.setResult(res);
				}
			});
		}
		else
		{
			String[] res = SUtil.EMPTY_STRING_ARRAY;
			Collection coll = getState().getAttributeValues(getHandle(), OAVBDIRuntimeModel.capability_has_subcapabilities);
			if(coll!=null)
			{
				res = new String[coll.size()];
				int i=0;
				for(Iterator it=coll.iterator(); it.hasNext(); i++)
				{
					Object cref = it.next();
					String name = (String)getState().getAttributeValue(cref, OAVBDIRuntimeModel.capabilityreference_has_name);
					res[i] = name;
				}
			}
			ret.setResult(res);
		}
		
		return ret;
	}
	
	//-------- element interface --------
	
	/**
	 *  Get the model element.
	 *  @return The model element.
	 */
	public IMElement getModelElement()
	{
		if(getInterpreter().isExternalThread())
		{
			AgentInvocation invoc = new AgentInvocation()
			{
				public void run()
				{
					Object me = getState().getAttributeValue(getHandle(), OAVBDIRuntimeModel.element_has_model);
					object = new MCapabilityFlyweight(getState(), me);
				}
			};
			return (IMElement)invoc.object;
		}
		else
		{
			Object me = getState().getAttributeValue(getHandle(), OAVBDIRuntimeModel.element_has_model);
			return new MCapabilityFlyweight(getState(), me);
		}
	}
	
	/**
	 *  Get the children (if any).
	 *  @return The children.
	 */
	public IFuture getChildren(String type)
	{
		return new Future(null);
	}
	
	/**
	 *  Get the model name of a component type.
	 *  @param ctype The component type.
	 *  @return The model name of this component type.
	 */
	public IFuture getFileName(String ctype)
	{
		return new Future(null);
	}
}
