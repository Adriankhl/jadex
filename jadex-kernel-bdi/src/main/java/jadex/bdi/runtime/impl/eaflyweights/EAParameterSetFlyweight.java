package jadex.bdi.runtime.impl.eaflyweights;

import jadex.bdi.model.IMElement;
import jadex.bdi.model.OAVBDIMetaModel;
import jadex.bdi.model.impl.flyweights.MParameterSetFlyweight;
import jadex.bdi.runtime.IEAParameterSet;
import jadex.bdi.runtime.impl.flyweights.ElementFlyweight;
import jadex.bdi.runtime.interpreter.BDIInterpreter;
import jadex.bdi.runtime.interpreter.BeliefRules;
import jadex.bdi.runtime.interpreter.MessageEventRules;
import jadex.bdi.runtime.interpreter.OAVBDIRuntimeModel;
import jadex.bridge.MessageType;
import jadex.bridge.MessageType.ParameterSpecification;
import jadex.commons.Future;
import jadex.commons.IFuture;
import jadex.commons.SReflect;
import jadex.commons.Tuple;
import jadex.rules.state.IOAVState;

import java.lang.reflect.Array;
import java.util.Collection;

/**
 *  Flyweight for a parameter set on instance level.
 */
public class EAParameterSetFlyweight extends ElementFlyweight implements IEAParameterSet
{
	//-------- attributes --------
	
	/** Parameter name. */
	// Used only when handle is null, because no parameter value stored in state, yet.
	protected String	name;
	
	/** Parameter element handle. */
	protected Object	pe;
	
	//-------- constructors --------
	
	/**
	 *  Create a new parameter flyweight.
	 *  @param state	The state.
	 *  @param scope	The scope handle.
	 *  @param handle	The parameter handle (or null if no value yet).
	 *  @param name	The parameter name (used, if no value yet).
	 *  @param pe	The handle for the parameter element to which this parameter belongs.
	 */
	private EAParameterSetFlyweight(IOAVState state, Object scope, Object handle, String name, Object pe)
	{
		super(state, scope, handle);
		this.name	= name;
		
		if(name==null)
			throw new RuntimeException("fixme");
		
		this.pe = pe;
	}
	
	/**
	 *  Get or create a flyweight.
	 *  @return The flyweight.
	 */
	public static EAParameterSetFlyweight getParameterSetFlyweight(IOAVState state, Object scope, Object handle, String name, Object parameterelement)
	{
		BDIInterpreter ip = BDIInterpreter.getInterpreter(state);
		EAParameterSetFlyweight ret = (EAParameterSetFlyweight)ip.getFlyweightCache(IEAParameterSet.class).get(new Tuple(parameterelement, name));
		if(ret==null)
		{
			ret = new EAParameterSetFlyweight(state, scope, handle, name, parameterelement);
			ip.getFlyweightCache(IEAParameterSet.class).put(new Tuple(parameterelement, name), ret);
		}
		return ret;
	}
	
	//-------- IParameter interface --------

	/**
	 *  Add a value to a parameter set.
	 *  @param value The new value.
	 */
	public IFuture addValue(final Object value)
	{
		final Future ret = new Future();
		
		if(getInterpreter().isExternalThread())
		{
			getInterpreter().getAgentAdapter().invokeLater(new Runnable()
			{
				public void run()
				{
					if(!hasHandle() && getState().containsKey(pe, 
						OAVBDIRuntimeModel.parameterelement_has_parametersets, name))
					{
						setHandle(getState().getAttributeValue(pe, 
							OAVBDIRuntimeModel.parameterelement_has_parametersets, name));
					}
					if(!hasHandle())
					{
						Object mparamelem = getState().getAttributeValue(pe, OAVBDIRuntimeModel.element_has_model);	
						Object mparamset = getState().getAttributeValue(mparamelem, OAVBDIMetaModel.parameterelement_has_parametersets, name);
						setHandle(BeliefRules.createParameterSet(getState(), name, null, resolveClazz(), pe, mparamset, getScope()));
					}
					String	direction 	= resolveDirection();
					if(OAVBDIMetaModel.PARAMETER_DIRECTION_FIXED.equals(direction)
						|| OAVBDIMetaModel.PARAMETER_DIRECTION_IN.equals(direction) && EAParameterFlyweight.inprocess(getState(), pe, getScope())
						|| OAVBDIMetaModel.PARAMETER_DIRECTION_OUT.equals(direction) && !EAParameterFlyweight.inprocess(getState(), pe, getScope()))
						throw new RuntimeException("Write access not allowed to parameter set: "
							+direction+" "+getName());

					BeliefRules.addParameterSetValue(getState(), getHandle(), value);
					ret.setResult(null);
				}
			});
		}
		else
		{
			if(!hasHandle() && getState().containsKey(pe, 
				OAVBDIRuntimeModel.parameterelement_has_parametersets, name))
			{
				setHandle(getState().getAttributeValue(pe, 
					OAVBDIRuntimeModel.parameterelement_has_parametersets, name));
			}
			if(!hasHandle())
			{
				Object mparamelem = getState().getAttributeValue(pe, OAVBDIRuntimeModel.element_has_model);	
				Object mparamset = getState().getAttributeValue(mparamelem, OAVBDIMetaModel.parameterelement_has_parametersets, name);
				setHandle(BeliefRules.createParameterSet(getState(), name, null, resolveClazz(), pe, mparamset, getScope()));
			}
			String	direction 	= resolveDirection();
			if(OAVBDIMetaModel.PARAMETER_DIRECTION_FIXED.equals(direction)
				|| OAVBDIMetaModel.PARAMETER_DIRECTION_IN.equals(direction) && EAParameterFlyweight.inprocess(getState(), pe, getScope())
				|| OAVBDIMetaModel.PARAMETER_DIRECTION_OUT.equals(direction) && !EAParameterFlyweight.inprocess(getState(), pe, getScope()))
				throw new RuntimeException("Write access not allowed to parameter set: "
					+direction+" "+getName());

			getInterpreter().startMonitorConsequences();
			BeliefRules.addParameterSetValue(getState(), getHandle(), value);
			getInterpreter().endMonitorConsequences();
			ret.setResult(null);
		}
		
		return ret;
	}

	/**
	 *  Remove a value to a parameter set.
	 *  @param value The new value.
	 */
	public IFuture removeValue(final Object value)
	{
		final Future ret = new Future();
		
		if(getInterpreter().isExternalThread())
		{
			getInterpreter().getAgentAdapter().invokeLater(new Runnable()
			{
				public void run()
				{
					if(!hasHandle() && getState().containsKey(pe, 
						OAVBDIRuntimeModel.parameterelement_has_parametersets, name))
					{
						setHandle(getState().getAttributeValue(pe, 
							OAVBDIRuntimeModel.parameterelement_has_parametersets, name));
					}
					String	direction 	= resolveDirection();
					if(OAVBDIMetaModel.PARAMETER_DIRECTION_FIXED.equals(direction)
						|| OAVBDIMetaModel.PARAMETER_DIRECTION_IN.equals(direction) && EAParameterFlyweight.inprocess(getState(), pe, getScope())
						|| OAVBDIMetaModel.PARAMETER_DIRECTION_OUT.equals(direction) && !EAParameterFlyweight.inprocess(getState(), pe, getScope()))
						throw new RuntimeException("Write access not allowed to parameter set: "
							+direction+" "+getName());

					if(!hasHandle())
						throw new RuntimeException("Value not contained: "+value);
					BeliefRules.removeParameterSetValue(getState(), getHandle(), value);
					ret.setResult(null);
				}
			});
		}
		else
		{
			if(!hasHandle() && getState().containsKey(pe, 
				OAVBDIRuntimeModel.parameterelement_has_parametersets, name))
			{
				setHandle(getState().getAttributeValue(pe, 
					OAVBDIRuntimeModel.parameterelement_has_parametersets, name));
			}
			String	direction 	= resolveDirection();
			if(OAVBDIMetaModel.PARAMETER_DIRECTION_FIXED.equals(direction)
				|| OAVBDIMetaModel.PARAMETER_DIRECTION_IN.equals(direction) && EAParameterFlyweight.inprocess(getState(), pe, getScope())
				|| OAVBDIMetaModel.PARAMETER_DIRECTION_OUT.equals(direction) && !EAParameterFlyweight.inprocess(getState(), pe, getScope()))
				throw new RuntimeException("Write access not allowed to parameter set: "
					+direction+" "+getName());

			if(!hasHandle())
				throw new RuntimeException("Value not contained: "+value);
			
			getInterpreter().startMonitorConsequences();
			BeliefRules.removeParameterSetValue(getState(), getHandle(), value);
			getInterpreter().endMonitorConsequences();
			ret.setResult(null);
		}
		
		return ret;
	}

	/**
	 *  Add values to a parameter set.
	 */
	public IFuture addValues(final Object[] values)
	{
		final Future ret = new Future();
//		if(values==null)
//			return;
	
		if(getInterpreter().isExternalThread())
		{
			getInterpreter().getAgentAdapter().invokeLater(new Runnable()
			{
				public void run()
				{
					if(!hasHandle() && getState().containsKey(pe, 
							OAVBDIRuntimeModel.parameterelement_has_parametersets, name))
					{
						setHandle(getState().getAttributeValue(pe, 
							OAVBDIRuntimeModel.parameterelement_has_parametersets, name));
					}
					if(!hasHandle())
					{
						Object mparamelem = getState().getAttributeValue(pe, OAVBDIRuntimeModel.element_has_model);	
						Object mparamset = getState().getAttributeValue(mparamelem, OAVBDIMetaModel.parameterelement_has_parametersets, name);
						setHandle(BeliefRules.createParameterSet(getState(), name, null, resolveClazz(), pe, mparamset, getScope()));
					}
					
					String	direction 	= resolveDirection();
					if(OAVBDIMetaModel.PARAMETER_DIRECTION_FIXED.equals(direction)
						|| OAVBDIMetaModel.PARAMETER_DIRECTION_IN.equals(direction) && EAParameterFlyweight.inprocess(getState(), pe, getScope())
						|| OAVBDIMetaModel.PARAMETER_DIRECTION_OUT.equals(direction) && !EAParameterFlyweight.inprocess(getState(), pe, getScope()))
						throw new RuntimeException("Write access not allowed to parameter set: "
							+direction+" "+getName());

					if(values!=null)
					{
						for(int i=0; i<values.length; i++)
							BeliefRules.addParameterSetValue(getState(), getHandle(), values[i]);
					}
					ret.setResult(null);
				}
			});
		}
		else
		{
			if(!hasHandle() && getState().containsKey(pe, 
					OAVBDIRuntimeModel.parameterelement_has_parametersets, name))
			{
				setHandle(getState().getAttributeValue(pe, 
					OAVBDIRuntimeModel.parameterelement_has_parametersets, name));
			}
			if(!hasHandle())
			{
				Object mparamelem = getState().getAttributeValue(pe, OAVBDIRuntimeModel.element_has_model);	
				Object mparamset = getState().getAttributeValue(mparamelem, OAVBDIMetaModel.parameterelement_has_parametersets, name);
				setHandle(BeliefRules.createParameterSet(getState(), name, null, resolveClazz(), pe, mparamset, getScope()));
			}
			String	direction 	= resolveDirection();
			if(OAVBDIMetaModel.PARAMETER_DIRECTION_FIXED.equals(direction)
				|| OAVBDIMetaModel.PARAMETER_DIRECTION_IN.equals(direction) && EAParameterFlyweight.inprocess(getState(), pe, getScope())
				|| OAVBDIMetaModel.PARAMETER_DIRECTION_OUT.equals(direction) && !EAParameterFlyweight.inprocess(getState(), pe, getScope()))
				throw new RuntimeException("Write access not allowed to parameter set: "
					+direction+" "+getName());

			getInterpreter().startMonitorConsequences();
			if(values!=null)
			{
				for(int i=0; i<values.length; i++)
					BeliefRules.addParameterSetValue(getState(), getHandle(), values[i]);
			}
			getInterpreter().endMonitorConsequences();
			ret.setResult(null);
		}
		
		return ret;
	}

	/**
	 *  Remove all values from a parameter set.
	 */
	public IFuture removeValues()
	{
		final Future ret = new Future();
		
		if(getInterpreter().isExternalThread())
		{
			getInterpreter().getAgentAdapter().invokeLater(new Runnable()
			{
				public void run()
				{
					if(!hasHandle() && getState().containsKey(pe, 
						OAVBDIRuntimeModel.parameterelement_has_parametersets, name))
					{
						setHandle(getState().getAttributeValue(pe, 
							OAVBDIRuntimeModel.parameterelement_has_parametersets, name));
					}
					String	direction 	= resolveDirection();
					if(OAVBDIMetaModel.PARAMETER_DIRECTION_FIXED.equals(direction)
						|| OAVBDIMetaModel.PARAMETER_DIRECTION_IN.equals(direction) && EAParameterFlyweight.inprocess(getState(), pe, getScope())
						|| OAVBDIMetaModel.PARAMETER_DIRECTION_OUT.equals(direction) && !EAParameterFlyweight.inprocess(getState(), pe, getScope()))
						throw new RuntimeException("Write access not allowed to parameter set: "
							+direction+" "+getName());

					if(hasHandle())
					{
						Collection vals = getState().getAttributeValues(getHandle(), OAVBDIRuntimeModel.parameterset_has_values);
						if(vals!=null)
						{
							Object[]	avals	= vals.toArray();
							for(int i=0; i<avals.length; i++)
								BeliefRules.removeParameterSetValue(getState(), getHandle(), avals[i]);
						}				
					}
					ret.setResult(null);
				}
			});
		}
		else
		{
			if(!hasHandle() && getState().containsKey(pe, 
				OAVBDIRuntimeModel.parameterelement_has_parametersets, name))
			{
				setHandle(getState().getAttributeValue(pe, 
					OAVBDIRuntimeModel.parameterelement_has_parametersets, name));
			}
			String	direction 	= resolveDirection();
			if(OAVBDIMetaModel.PARAMETER_DIRECTION_FIXED.equals(direction)
				|| OAVBDIMetaModel.PARAMETER_DIRECTION_IN.equals(direction) && EAParameterFlyweight.inprocess(getState(), pe, getScope())
				|| OAVBDIMetaModel.PARAMETER_DIRECTION_OUT.equals(direction) && !EAParameterFlyweight.inprocess(getState(), pe, getScope()))
				throw new RuntimeException("Write access not allowed to parameter set: "
					+direction+" "+getName());

			if(hasHandle())
			{
				Collection vals = getState().getAttributeValues(getHandle(), OAVBDIRuntimeModel.parameterset_has_values);
				if(vals!=null)
				{
					Object[]	avals	= vals.toArray();
					getInterpreter().startMonitorConsequences();
					for(int i=0; i<avals.length; i++)
						BeliefRules.removeParameterSetValue(getState(), getHandle(), avals[i]);
					getInterpreter().endMonitorConsequences();
				}				
			}
			ret.setResult(null);
		}
		
		return ret;
	}

	/**
	 *  Get a value equal to the given object.
	 *  @param oldval The old value.
	 * /
	public Object	getValue(final Object oldval)
	{
		if(getInterpreter().isExternalThread())
		{
			AgentInvocation invoc = new AgentInvocation()
			{
				public void run()
				{
					if(!hasHandle() && getState().containsKey(pe, 
						OAVBDIRuntimeModel.parameterelement_has_parametersets, name))
					{
						setHandle(getState().getAttributeValue(pe, 
							OAVBDIRuntimeModel.parameterelement_has_parametersets, name));
					}
					if(!hasHandle())
					{
						setHandle(getState().createObject(OAVBDIRuntimeModel.parameterset_type));
						getState().setAttributeValue(getHandle(), OAVBDIRuntimeModel.parameterset_has_name, name);
						getState().addAttributeValue(pe, OAVBDIRuntimeModel.parameterelement_has_parametersets, getHandle());
					}
					
					Collection vals = getState().getAttributeValues(getHandle(), OAVBDIRuntimeModel.parameterset_has_values);
					int index = indexOf(oldval);
					
					if(index != -1)
					{
						found	= true;
						newval	= values.get(index);
					}
				}
			};
			return invoc.bool;
		}
		else
		{
			if(!hasHandle() && getState().containsKey(pe, 
				OAVBDIRuntimeModel.parameterelement_has_parametersets, name))
			{
				setHandle(getState().getAttributeValue(pe, 
					OAVBDIRuntimeModel.parameterelement_has_parametersets, name));
			}
			if(!hasHandle())
			{
				setHandle(getState().createObject(OAVBDIRuntimeModel.parameterset_type));
				getState().setAttributeValue(getHandle(), OAVBDIRuntimeModel.parameterset_has_name, name);
				getState().addAttributeValue(pe, OAVBDIRuntimeModel.parameterelement_has_parametersets, getHandle());
			}
			
			Collection vals = getState().getAttributeValues(getHandle(), OAVBDIRuntimeModel.parameterset_has_values);
			return vals.contains(value);
		}
	}*/

	/**
	 *  Test if a value is contained in a parameter.
	 *  @param value The value to test.
	 *  @return True, if value is contained.
	 */
	public IFuture containsValue(final Object value)
	{
		final Future ret = new Future();
		
		if(getInterpreter().isExternalThread())
		{
			getInterpreter().getAgentAdapter().invokeLater(new Runnable()
			{
				public void run()
				{
					if(!hasHandle() && getState().containsKey(pe, 
						OAVBDIRuntimeModel.parameterelement_has_parametersets, name))
					{
						setHandle(getState().getAttributeValue(pe, 
							OAVBDIRuntimeModel.parameterelement_has_parametersets, name));
					}
					
					Collection vals	= null;
					Object newval = value;
					if(hasHandle())
					{
//						Class clazz = (Class)getState().getAttributeValue(getHandle(), OAVBDIMetaModel.typedelement_has_class);
						newval = SReflect.convertWrappedValue(value, resolveClazz());
						vals = getState().getAttributeValues(getHandle(), OAVBDIRuntimeModel.parameterset_has_values); 
					}
					boolean bool = vals!=null && vals.contains(newval);
					ret.setResult(bool? Boolean.TRUE: Boolean.FALSE);
				}
			});
		}
		else
		{
			if(!hasHandle() && getState().containsKey(pe, 
				OAVBDIRuntimeModel.parameterelement_has_parametersets, name))
			{
				setHandle(getState().getAttributeValue(pe, 
					OAVBDIRuntimeModel.parameterelement_has_parametersets, name));
			}
			
			Collection vals	= null;
			Object newval = value;
			if(hasHandle())
			{
				//Class clazz = (Class)getState().getAttributeValue(getHandle(), OAVBDIMetaModel.typedelement_has_class);
				newval = SReflect.convertWrappedValue(value, resolveClazz());
				vals = getState().getAttributeValues(getHandle(), OAVBDIRuntimeModel.parameterset_has_values); 
			}
			boolean bool = vals!=null && vals.contains(newval);
			ret.setResult(bool? Boolean.TRUE: Boolean.FALSE);
		}
		
		return ret;
	}

	/**
	 *  Get the values of a parameterset.
	 *  @return The values.
	 */
	public IFuture getValues()
	{
		final Future ret = new Future();
		
		if(getInterpreter().isExternalThread())
		{
			getInterpreter().getAgentAdapter().invokeLater(new Runnable()
			{
				public void run()
				{
					Collection vals = null;
					if(!hasHandle() && getState().containsKey(pe, 
						OAVBDIRuntimeModel.parameterelement_has_parametersets, name))
					{
						setHandle(getState().getAttributeValue(pe, 
							OAVBDIRuntimeModel.parameterelement_has_parametersets, name));
					}
					if(hasHandle())
					{
						vals = getState().getAttributeValues(getHandle(), OAVBDIRuntimeModel.parameterset_has_values);
					}
					
					Object[] oarray = (Object[])Array.newInstance(resolveClazz(), vals!=null ? vals.size() : 0);
					ret.setResult(vals!=null ? vals.toArray(oarray) : oarray);
				}
			});
		}
		else
		{
			Collection vals = null;
			if(!hasHandle() && getState().containsKey(pe, 
				OAVBDIRuntimeModel.parameterelement_has_parametersets, name))
			{
				setHandle(getState().getAttributeValue(pe, 
					OAVBDIRuntimeModel.parameterelement_has_parametersets, name));
			}
			if(hasHandle())
			{
				vals = getState().getAttributeValues(getHandle(), OAVBDIRuntimeModel.parameterset_has_values);
			}
			
			Object[] oarray = (Object[])Array.newInstance(resolveClazz(), vals!=null ? vals.size() : 0);
			ret.setResult(vals!=null ? vals.toArray(oarray) : oarray);
		}
		
		return ret;
	}
	
	/**
	 *  Get the number of values currently
	 *  contained in this set.
	 *  @return The values count.
	 */
	public IFuture size()
	{
		final Future ret = new Future();
		
		if(getInterpreter().isExternalThread())
		{
			getInterpreter().getAgentAdapter().invokeLater(new Runnable()
			{
				public void run()
				{
					int integer = 0;
					if(!hasHandle() && getState().containsKey(pe, 
						OAVBDIRuntimeModel.parameterelement_has_parametersets, name))
					{
						setHandle(getState().getAttributeValue(pe, 
							OAVBDIRuntimeModel.parameterelement_has_parametersets, name));
					}
					if(hasHandle())
					{
						Collection coll = getState().getAttributeValues(getHandle(), OAVBDIRuntimeModel.parameterset_has_values);
						if(coll!=null)
							integer = coll.size();
					}
					
					ret.setResult(new Integer(integer));
				}
			});
		}
		else
		{
			int integer = 0;
			if(!hasHandle() && getState().containsKey(pe, 
				OAVBDIRuntimeModel.parameterelement_has_parametersets, name))
			{
				setHandle(getState().getAttributeValue(pe, 
					OAVBDIRuntimeModel.parameterelement_has_parametersets, name));
			}
			if(hasHandle())
			{
				Collection coll = getState().getAttributeValues(getHandle(), OAVBDIRuntimeModel.parameterset_has_values);
				if(coll!=null)
					integer = coll.size();
			}
			ret.setResult(new Integer(integer));
		}
		
		return ret;
	}
	
	/**
	 *  Get the name.
	 *  @return The name.
	 */
	public String getName()
	{
		return name;
		
//		final Future ret = new Future();
//		
//		if(getInterpreter().isExternalThread())
//		{
//			getInterpreter().getAgentAdapter().invokeLater(new Runnable()
//			{
//				public void run()
//				{
//					ret.setResult(getState().getAttributeValue(getHandle(), OAVBDIRuntimeModel.parameterset_has_name));
//				}
//			});
//		}
//		else
//		{
//			ret.setResult(getState().getAttributeValue(getHandle(), OAVBDIRuntimeModel.parameterset_has_name));
//		}
//		
//		return ret;
	}
	
	//-------- IElement interface --------

	/**
	 *  Get the model element.
	 *  @return The model element.
	 * /
	public IMElement getModelElement()
	{
		if(getInterpreter().isExternalThread())
		{
			AgentInvocation invoc = new AgentInvocation()
			{
				public void run()
				{
					Object	mpe	= getState().getAttributeValue(pe, OAVBDIRuntimeModel.element_has_model);
					Object	mparameterset = getState().getAttributeValue(mpe, OAVBDIMetaModel.parameterelement_has_parametersets, name);
					Object	mscope	= getState().getAttributeValue(getScope(), OAVBDIRuntimeModel.element_has_model);
					object	= new MParameterSetFlyweight(getState(), mscope, mparameterset);
				}
			};
			return (IMElement)invoc.object;
		}
		else
		{
			IMElement	ret	= null;
			Object	mpe	= getState().getAttributeValue(pe, OAVBDIRuntimeModel.element_has_model);
			Object	mparameterset = getState().getAttributeValue(mpe, OAVBDIMetaModel.parameterelement_has_parametersets, name);
			Object	mscope	= getState().getAttributeValue(getScope(), OAVBDIRuntimeModel.element_has_model);
			ret	= new MParameterSetFlyweight(getState(), mscope, mparameterset);
			return ret;
		}
	}*/
	
	/**
	 *  Resolve the parameterset class.
	 */
	protected Class resolveClazz()
	{
		Class clazz = null;
		Object mparamelem = getState().getAttributeValue(pe, OAVBDIRuntimeModel.element_has_model);	
		Object mparamset = getState().getAttributeValue(mparamelem, OAVBDIMetaModel.parameterelement_has_parametersets, name);
		if(mparamset!=null)
		{
			clazz = (Class)getState().getAttributeValue(mparamset, OAVBDIMetaModel.typedelement_has_class);
		}
		else if(getState().getType(mparamelem).isSubtype(OAVBDIMetaModel.messageevent_type))
		{
			MessageType mt = MessageEventRules.getMessageEventType(getState(), mparamelem);
			ParameterSpecification ps = mt.getParameter(name);
			clazz = ps.getClazz();
		}
		if(clazz==null)
			clazz = Object.class;
		
		return clazz;
	}
	
	/**
	 *  Resolve the parameter direction.
	 */
	protected String resolveDirection()
	{
		String direction = null;
		Object mparamelem = getState().getAttributeValue(pe, OAVBDIRuntimeModel.element_has_model);	
		Object mparamset = getState().getAttributeValue(mparamelem, OAVBDIMetaModel.parameterelement_has_parametersets, name);
		if(mparamset!=null)
		{
			direction = (String)getState().getAttributeValue(mparamset, OAVBDIMetaModel.parameterset_has_direction);
		}
		if(direction==null)
			direction = OAVBDIMetaModel.PARAMETER_DIRECTION_IN;
		
		return direction;
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
					Object mscope = getState().getAttributeValue(getScope(), OAVBDIRuntimeModel.element_has_model);
					object = new MParameterSetFlyweight(getState(), mscope, me);
				}
			};
			return (IMElement)invoc.object;
		}
		else
		{
			Object me = getState().getAttributeValue(getHandle(), OAVBDIRuntimeModel.element_has_model);
			Object mscope = getState().getAttributeValue(getScope(), OAVBDIRuntimeModel.element_has_model);
			return new MParameterSetFlyweight(getState(), mscope, me);
		}
	}
}
