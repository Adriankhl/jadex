package jadex.bdi.runtime.impl;


import jadex.bdi.model.IMParameter;
import jadex.bdi.model.IMParameterSet;
import jadex.bdi.model.OAVBDIMetaModel;
import jadex.bdi.runtime.AgentEvent;
import jadex.bdi.runtime.IBDIInternalAccess;
import jadex.bdi.runtime.IGoal;
import jadex.bdi.runtime.IGoalListener;
import jadex.bdi.runtime.IParameter;
import jadex.bdi.runtime.IParameterSet;
import jadex.bridge.service.annotation.Service;
import jadex.commons.SReflect;
import jadex.commons.SUtil;
import jadex.commons.future.Future;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;

/**
 *  
 */
@Service
public class GoalDelegationHandler  implements InvocationHandler
{
	//-------- attributes --------
	
	/** The agent. */
	protected IBDIInternalAccess agent;
	
	/** The goal name. */
	protected String goalname;
	
	//-------- constructors --------
	
	/**
	 *  Create a new service wrapper invocation handler.
	 *  @param agent The internal access of the agent.
	 */
	public GoalDelegationHandler(IBDIInternalAccess agent, String goalname)
	{
		if(agent==null)
			throw new IllegalArgumentException("Agent must not null.");
		if(goalname==null)
			throw new IllegalArgumentException("Goal name must not null.");
		this.agent = agent;
		this.goalname = goalname;
	}
	
	//-------- methods --------
	
	/**
	 *  Called when a wrapper method is invoked.
	 *  Uses the cms to create a new invocation agent and lets this
	 *  agent call the web service. The result is transferred back
	 *  into the result future of the caller.
	 */
	public Object invoke(Object proxy, final Method method, final Object[] args) throws Throwable
	{
		final Future<Object> ret = new Future<Object>();
		
		final IGoal goal = agent.getGoalbase().createGoal(goalname);
		
		Class<?>[] mptypes = method.getParameterTypes();
		
		List<IParameter> params = SUtil.arrayToList(goal.getParameters());
		List<IParameterSet> paramsets = SUtil.arrayToList(goal.getParameterSets());
		
		for(int i=0; i<mptypes.length; i++)
		{
			boolean set = false;
			if(args[i]!=null)
			{
				for(Iterator<IParameter> it = params.iterator(); it.hasNext(); )
				{
					IParameter p = it.next();
					
					IMParameter mp = (IMParameter)p.getModelElement();
					if(OAVBDIMetaModel.PARAMETER_DIRECTION_IN.equals(mp.getDirection()))
					{
						if(SReflect.isSupertype(mp.getClazz(), mptypes[i]))
						{
							p.setValue(args[i]);
							set = true;
							it.remove();
							break;
						}
					}
					else
					{
						it.remove();
					}
				}
				
				if(!set)
				{
					for(Iterator<IParameterSet> it = paramsets.iterator(); it.hasNext(); )
					{
						IParameterSet ps = it.next();
						
						IMParameterSet mps = (IMParameterSet)ps.getModelElement();
						if(OAVBDIMetaModel.PARAMETER_DIRECTION_IN.equals(mps.getDirection()) && mptypes[i].isArray())
						{
							if(SReflect.isSupertype(mps.getClazz(), mptypes[i].getComponentType()))
							{
								ps.addValues((Object[])args[i]);
								set = true;
								it.remove();
								break;
							}
						}
						else
						{
							it.remove();
						}
					}
				}
			}
			
			if(!set)
				throw new RuntimeException("Could not map: "+args[i]);
		}
		
		
//		final Object handle = ((GoalFlyweight)paint).getHandle();
		goal.addGoalListener(new IGoalListener()
		{
			public void goalFinished(AgentEvent ae)
			{
				if(goal.isSucceeded())
				{
					Class<?> rt = SReflect.unwrapGenericType(method.getReturnType());
					Object rval = null;
					
					if(!Void.class.equals(rt))
					{
						boolean set = false;
						
						IParameter[] params = goal.getParameters();
						for(int i=0; i<params.length; i++)
						{
							IMParameter mp = (IMParameter)params[i].getModelElement();
							String dir = ((IMParameter)params[i].getModelElement()).getDirection();
							if((OAVBDIMetaModel.PARAMETER_DIRECTION_INOUT.equals(dir) || OAVBDIMetaModel.PARAMETER_DIRECTION_OUT.equals(dir)) 
								&& SReflect.isSupertype(mp.getClazz(), rt))
							{
								rval = params[i].getValue();
								set = true;
								break;
							}
						}
						
						if(!set)
						{
							IParameterSet[] paramsets = goal.getParameterSets();
							for(int i=0; i<paramsets.length; i++)
							{
								IMParameterSet mps = (IMParameterSet)paramsets[i].getModelElement();
								String dir = ((IMParameter)params[i].getModelElement()).getDirection();
								if((OAVBDIMetaModel.PARAMETER_DIRECTION_INOUT.equals(dir) || OAVBDIMetaModel.PARAMETER_DIRECTION_OUT.equals(dir)) 
									&& SReflect.isSupertype(mps.getClazz(), rt.getComponentType()))
								{
									paramsets[i].addValues((Object[])args[i]);
									set = true;
									break;
								}
							}
						}
						
						if(!set)
							throw new RuntimeException("Could not map result.");
					}
					
					ret.setResult(rval);
				}
				else
				{
					ret.setException(goal.getException()!=null? goal.getException(): new RuntimeException());
				}
			}
			
			public void goalAdded(AgentEvent ae)
			{
			}
		});
		agent.getGoalbase().dispatchTopLevelGoal(goal);
	
		return ret;
	}
}