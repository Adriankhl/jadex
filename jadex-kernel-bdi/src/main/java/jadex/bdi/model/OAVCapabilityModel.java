package jadex.bdi.model;

import jadex.bdi.runtime.interpreter.AgentRules;
import jadex.bridge.AbstractErrorReportBuilder;
import jadex.bridge.modelinfo.Argument;
import jadex.bridge.modelinfo.ConfigurationInfo;
import jadex.bridge.modelinfo.IArgument;
import jadex.bridge.modelinfo.ModelInfo;
import jadex.bridge.modelinfo.UnparsedExpression;
import jadex.bridge.service.ProvidedServiceImplementation;
import jadex.bridge.service.ProvidedServiceInfo;
import jadex.bridge.service.RequiredServiceBinding;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.commons.ICacheableModel;
import jadex.commons.SReflect;
import jadex.commons.SUtil;
import jadex.commons.Tuple;
import jadex.commons.collection.IndexMap;
import jadex.commons.collection.MultiCollection;
import jadex.commons.collection.SCollection;
import jadex.javaparser.IParsedExpression;
import jadex.rules.rulesystem.IRule;
import jadex.rules.rulesystem.Rulebase;
import jadex.rules.state.IOAVState;
import jadex.xml.StackElement;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *  The capability model contains the OAV capability model in a state
 *  and a type-specific compiled rulebase (matcher functionality).
 */
public class OAVCapabilityModel implements ICacheableModel//, IModelInfo
{
	//-------- attributes --------
	
	/** The state. */
	protected IOAVState state;
	
	/** The agent handle. */
	protected Object handle;
	
	/** The (actual) object types contained in the state. */
	protected Set	types;
	
	/** The filename. */
	protected String filename;
	
	/** The rulebase of the capability (includes type-specific rules, if any). */
	protected Rulebase rulebase;
	
	/** The last modified date. */
	protected long	lastmod;
	
	/** The last checked date (when the file date was last read). */
	protected long	lastcheck;
	
	/** The model info. */
	protected ModelInfo modelinfo;
	
	// todo: use some internal report for collecting error stuff?!
	/** The multi-collection holding the report messages. */
	protected MultiCollection	entries;
	
	/** The documents for external elements (e.g. capabilities). */
	protected Map externals;
	
	//-------- constructors --------
	
	/**
	 *  Create a model.
	 */
	public OAVCapabilityModel(IOAVState state, Object handle, Set types, String filename, long lastmod, MultiCollection entries)
	{
		this.state	= state;
		this.handle	= handle;
		this.types	= types;
		this.filename = filename;
		this.rulebase	= new Rulebase();
		this.lastmod	= lastmod;
		this.entries	= entries;
	}
	
	/**
	 *  Init the model info.
	 */
	public void initModelInfo()
	{
		boolean startable = !this.getClass().equals(OAVCapabilityModel.class);
		
		Collection tmp = handle!=null ? state.getAttributeValues(handle, OAVBDIMetaModel.capability_has_imports) : null;
//		List imp = new ArrayList(tmp!=null? tmp: new ArrayList());
//		imp.add(state.getAttributeValue(handle, OAVBDIMetaModel.capability_has_package)+".*");
		String[] imports = tmp!=null ? (String[])tmp.toArray(new String[0]) : null;
		
		String[] confignames = getConfigurations();
		ConfigurationInfo[] cinfos = null;
		if(confignames.length>0)
		{
			cinfos = new ConfigurationInfo[confignames.length];
			for(int i=0; i<confignames.length; i++)
			{
				cinfos[i] = new ConfigurationInfo(confignames[i]);
			}
		}
		
		this.modelinfo = new ModelInfo(getName(), getPackage(), getDescription(), null, getArguments(), 
			getResults(), startable, filename, getProperties(), getClassLoader(), getRequiredServices(), 
			getProvidedServices(), cinfos, null, imports);
		
		// Build error report.
		getModelInfo().setReport(new AbstractErrorReportBuilder(getModelInfo().getName(), getModelInfo().getFilename(),
			new String[]{"XML", "Capability", "Belief", "Goal", "Plan", "Event"}, entries, getDocuments())
		{
			public boolean isInCategory(Object obj, String category)
			{
				boolean	ret	= false;
				if("XML".equals(category))
				{
					ret	= obj instanceof String;
				}
				else if("Capability".equals(category))
				{
					ret	= state.getType(obj).isSubtype(OAVBDIMetaModel.capabilityref_type);
				}
				else if("Belief".equals(category))
				{
					ret	= state.getType(obj).isSubtype(OAVBDIMetaModel.belief_type)
						|| state.getType(obj).isSubtype(OAVBDIMetaModel.beliefset_type)
						|| state.getType(obj).isSubtype(OAVBDIMetaModel.beliefreference_type)
						|| state.getType(obj).isSubtype(OAVBDIMetaModel.beliefsetreference_type);
				}
				else if("Goal".equals(category))
				{
					ret	= state.getType(obj).isSubtype(OAVBDIMetaModel.goal_type)
						|| state.getType(obj).isSubtype(OAVBDIMetaModel.goalreference_type);
				}
				else if("Plan".equals(category))
				{
					ret	= state.getType(obj).isSubtype(OAVBDIMetaModel.plan_type);
				}
				else if("Event".equals(category))
				{
					ret	= state.getType(obj).isSubtype(OAVBDIMetaModel.event_type)
						|| state.getType(obj).isSubtype(OAVBDIMetaModel.internaleventreference_type)
						|| state.getType(obj).isSubtype(OAVBDIMetaModel.messageeventreference_type);
				}
				return ret;
			}
			
			public Object getPathElementObject(Object element)
			{
				return ((StackElement)element).getObject();
			}
			
			public String getObjectName(Object obj)
			{
				String	name	= null;
				if(state.getType(obj).isSubtype(OAVBDIMetaModel.modelelement_type))
				{
					name	= (String)state.getAttributeValue(obj, OAVBDIMetaModel.modelelement_has_name);
				}
				
				if(name==null && state.getType(obj).isSubtype(OAVBDIMetaModel.elementreference_type))
				{
					name	= (String)state.getAttributeValue(obj, OAVBDIMetaModel.elementreference_has_concrete);
				}
				
				if(name==null && state.getType(obj).isSubtype(OAVBDIMetaModel.expression_type))
				{
					IParsedExpression	exp	=(IParsedExpression)state.getAttributeValue(obj, OAVBDIMetaModel.expression_has_parsed);
					String	text	= (String)state.getAttributeValue(obj, OAVBDIMetaModel.expression_has_text);
					name	= exp!=null ? exp.getExpressionText() : text!=null ? text.trim() : null;
				}
				
				if(name==null)
				{
					name	= ""+obj;
				}
				
				return obj instanceof String ? (String)obj : state.getType(obj).getName().substring(1) + " " + name;
			}
		}.buildErrorReport());
	}
	
	//-------- IAgentModel methods --------
	
	/**
	 *  Get the name.
	 *  @return The name.
	 */
	public String getName()
	{
		String	ret;
		if(handle!=null)
		{
			ret	= (String)state.getAttributeValue(handle, OAVBDIMetaModel.modelelement_has_name);
		}
		
		// For broken XMLs (no handle) try to generate name from filename.
		else
		{
			int idx	= Math.max(filename.lastIndexOf(File.separator), filename.lastIndexOf("/"));
			if(idx!=-1)
			{
				ret	= filename.substring(idx+1);
			}
			else
			{
				ret	= filename;
			}
		}
		return ret;
	}
	
	/**
	 *  Get the package name.
	 *  @return The package name.
	 */
	public String getPackage()
	{
		return handle!=null ? (String)state.getAttributeValue(handle, OAVBDIMetaModel.capability_has_package) : null;
	}
	
	/**
	 *  Get the full model name (package.name)
	 *  @return The full name.
	 * /
	public String getFullName()
	{
		String pkg = getPackage();
		return pkg!=null && pkg.length()>0? pkg+"."+getName(): getName();
	}*/
	
	/**
	 *  Get the model description.
	 *  @return The model description.
	 */
	public String getDescription()
	{
		String ret = handle!=null ? (String)state.getAttributeValue(handle, OAVBDIMetaModel.modelelement_has_description) : null;
		return ret!=null? ret: "No description available."; 
	}
	
	/**
	 *  Get the report.
	 *  @return The report.
	 * /
	public IReport getReport()
	{
		return report;
	}*/
	
	/**
	 *  Get the configurations.
	 *  @return The configuration.
	 */
	public String[] getConfigurations()
	{
		String[] ret = SUtil.EMPTY_STRING_ARRAY;
		
		Collection configs = handle!=null ? state.getAttributeValues(handle, OAVBDIMetaModel.capability_has_configurations) : null;
		if(configs!=null)
		{
			List tmp = new ArrayList();
			String	defname = (String)state.getAttributeValue(handle, OAVBDIMetaModel.capability_has_defaultconfiguration);
			if(defname!=null)
				tmp.add(defname);
			
			for(Iterator it=configs.iterator(); it.hasNext(); )
			{
				String name = (String)state.getAttributeValue(it.next(), OAVBDIMetaModel.modelelement_has_name);
				if(defname==null || !defname.equals(name))
					tmp.add(name);
			}
			
			ret = (String[])tmp.toArray(new String[tmp.size()]);
		}
		
		return ret;
	}
	
	/**
	 *  Get the arguments.
	 *  @return The arguments.
	 */
	public IArgument[] getArguments()
	{
		List ret = new ArrayList();
		
		Collection bels = handle!=null ? state.getAttributeValues(handle, OAVBDIMetaModel.capability_has_beliefs) : null;
		if(bels!=null)
		{
			for(Iterator it=bels.iterator(); it.hasNext(); )
			{
				Object bel = it.next();
				String exported = (String)state.getAttributeValue(bel, 
					OAVBDIMetaModel.referenceableelement_has_exported);
				Boolean argu = (Boolean)state.getAttributeValue(bel, 
					OAVBDIMetaModel.belief_has_argument);
				if(!OAVBDIMetaModel.EXPORTED_FALSE.equals(exported) || Boolean.TRUE.equals(argu))
					ret.add(createArgument(state, handle, bel, false));
			}
		}
		
		Collection belrefs = handle!=null ? state.getAttributeValues(handle, OAVBDIMetaModel.capability_has_beliefrefs) : null;
		if(belrefs!=null)
		{
			for(Iterator it=belrefs.iterator(); it.hasNext(); )
			{
				Object belref = it.next();
				String exported = (String)state.getAttributeValue(belref, 
					OAVBDIMetaModel.referenceableelement_has_exported);
				Boolean argu = (Boolean)state.getAttributeValue(belref, 
					OAVBDIMetaModel.beliefreference_has_argument);
				if(!OAVBDIMetaModel.EXPORTED_FALSE.equals(exported) || Boolean.TRUE.equals(argu))
					ret.add(createArgument(state, handle, belref, false));
			}
		}
		
		Collection belsets = handle!=null ? state.getAttributeValues(handle, OAVBDIMetaModel.capability_has_beliefsets) : null;
		if(belsets!=null)
		{
			for(Iterator it=belsets.iterator(); it.hasNext(); )
			{
				Object belset = it.next();
				String exported = (String)state.getAttributeValue(belset, 
					OAVBDIMetaModel.referenceableelement_has_exported);
				Boolean argu = (Boolean)state.getAttributeValue(belset, 
					OAVBDIMetaModel.beliefset_has_argument);
				if(!OAVBDIMetaModel.EXPORTED_FALSE.equals(exported) || Boolean.TRUE.equals(argu))
					ret.add(createArgument(state, handle, belset, true));
			}
		}
		
		Collection belsetrefs = handle!=null ? state.getAttributeValues(handle, OAVBDIMetaModel.capability_has_beliefsetrefs) : null;
		if(belsetrefs!=null)
		{
			for(Iterator it=belsetrefs.iterator(); it.hasNext(); )
			{
				Object belsetref = it.next();
				String exported = (String)state.getAttributeValue(belsetref, 
					OAVBDIMetaModel.referenceableelement_has_exported);
				Boolean argu = (Boolean)state.getAttributeValue(belsetref, 
					OAVBDIMetaModel.beliefsetreference_has_argument);
				if(!OAVBDIMetaModel.EXPORTED_FALSE.equals(exported) || Boolean.TRUE.equals(argu))
					ret.add(createArgument(state, handle, belsetref, true));
			}
		}
		
		return (IArgument[])ret.toArray(new IArgument[ret.size()]);
	}
	
	/**
	 *  Get the results.
	 *  @return The results.
	 */
	public IArgument[] getResults()
	{
		List ret = new ArrayList();
		
		Collection bels = handle!=null ? state.getAttributeValues(handle, OAVBDIMetaModel.capability_has_beliefs) : null;
		if(bels!=null)
		{
			for(Iterator it=bels.iterator(); it.hasNext(); )
			{
				Object bel = it.next();
				Boolean result = (Boolean)state.getAttributeValue(bel, 
					OAVBDIMetaModel.belief_has_result);
				if(Boolean.TRUE.equals(result))
					ret.add(createArgument(state, handle, bel, false));
			}
		}
		
		Collection belrefs = handle!=null ? state.getAttributeValues(handle, OAVBDIMetaModel.capability_has_beliefrefs) : null;
		if(belrefs!=null)
		{
			for(Iterator it=belrefs.iterator(); it.hasNext(); )
			{
				Object belref = it.next();
				Boolean result = (Boolean)state.getAttributeValue(belref, 
					OAVBDIMetaModel.beliefreference_has_result);
				if(Boolean.TRUE.equals(result))
					ret.add(createArgument(state, handle, belref, false));
			}
		}
		
		Collection belsets = handle!=null ? state.getAttributeValues(handle, OAVBDIMetaModel.capability_has_beliefsets) : null;
		if(belsets!=null)
		{
			for(Iterator it=belsets.iterator(); it.hasNext(); )
			{
				Object belset = it.next();
				Boolean result = (Boolean)state.getAttributeValue(belset, 
					OAVBDIMetaModel.beliefset_has_result);
				if(Boolean.TRUE.equals(result))
					ret.add(createArgument(state, handle, belset, true));
			}
		}
		
		Collection belsetrefs = handle!=null ? state.getAttributeValues(handle, OAVBDIMetaModel.capability_has_beliefsetrefs) : null;
		if(belsetrefs!=null)
		{
			for(Iterator it=belsetrefs.iterator(); it.hasNext(); )
			{
				Object belsetref = it.next();
				Boolean result = (Boolean)state.getAttributeValue(belsetref, 
					OAVBDIMetaModel.beliefsetreference_has_result);
				if(Boolean.TRUE.equals(result))
					ret.add(createArgument(state, handle, belsetref, true));
			}
		}
		
		return (IArgument[])ret.toArray(new IArgument[ret.size()]);
	}
	
	/**
	 *  Is the model startable.
	 *  @return True, if startable.
	 */
	public boolean isStartable()
	{
		return false;
	}
	
	/**
	 *  Get the model type.
	 *  @reeturn The model type (kernel specific).
	 * /
	public String getType()
	{
		// todo: 
		return "v2capability";
	}*/
	
	/**
	 *  Get the filename.
	 *  @return The filename.
	 * /
	public String getFilename()
	{
		return this.filename;
	}*/
	
	/**
	 *  Get the last modified date.
	 *  @return The last modified date.
	 */
	public long getLastModified()
	{
		return this.lastmod;
	}
	
	/**
	 *  Return the class loader corresponding to the model.
	 *  @return The class loader corresponding to the model.
	 */
	public ClassLoader getClassLoader()
	{
		return getState().getTypeModel().getClassLoader();
	}

	//-------- methods --------

	/**
	 *  Get the last check date.
	 */
	public long getLastChecked()
	{
		return this.lastcheck;
	}
	
	/**
	 *  Set the last modified date.
	 *  @return The last modified date.
	 */
	public void	setLastChecked(long lastcheck)
	{
		this.lastcheck	= lastcheck;
	}

	/**
	 *  Get the properties.
	 *  Arbitrary properties that can e.g. be used to
	 *  define kernel-specific settings to configure tools. 
	 *  @return The properties.
	 */
	public Map	getProperties()
	{
		Map ret = new HashMap();
		if(handle!=null)
			addCapabilityProperties(ret, handle);
		return ret;
	}
	
	/**
	 *  Add the properties of a capability.
	 *  @param props The map to add the properties.
	 *  @param capa The start capability.
	 */
	public void addCapabilityProperties(Map props, Object capa)
	{
		// Properties from loaded model.
		Collection	oprops	= state.getAttributeKeys(capa, OAVBDIMetaModel.capability_has_properties);
		if(oprops!=null)
		{
			for(Iterator it=oprops.iterator(); it.hasNext(); )
			{
				String	key	= (String) it.next();
				Object	mexp	= state.getAttributeValue(capa, OAVBDIMetaModel.capability_has_properties, key);
				Class	clazz	= (Class)state.getAttributeValue(mexp, OAVBDIMetaModel.expression_has_class);
				String	value	= (String)state.getAttributeValue(mexp, OAVBDIMetaModel.expression_has_text);
				String	language	= (String)state.getAttributeValue(mexp, OAVBDIMetaModel.expression_has_language);
				props.put(key, new UnparsedExpression(key, clazz, value, language));
				
//				// Ignore future properties, which are evaluated at component instance startup time.
//				if(clazz==null || !SReflect.isSupertype(IFuture.class, clazz))
//				{
//					IParsedExpression	pex = (IParsedExpression)state.getAttributeValue(mexp, OAVBDIMetaModel.expression_has_parsed);
//					if(pex!=null)
//					{
//						try
//						{
//							Object	value	= pex.getValue(null);
//							props.put(key, value);
//						}
//						catch(Exception e)
//						{
//							Tuple	se;
//							se	= new Tuple(new Object[]{
//								new StackElement(new QName(state.getType(capa).isSubtype(OAVBDIMetaModel.agent_type) ? "agent" : "capability"), capa, null),
//								new StackElement(new QName("properties"), null, null),				
//								new StackElement(new QName("property"), mexp, null)});				
//							addEntry(se, "Error in property: "+e);
//						}
//					}
//				}
			}
		}
		
//		// Merge with subproperties
//		Collection subcaparefs = state.getAttributeValues(capa, OAVBDIMetaModel.capability_has_capabilityrefs);
//		if(subcaparefs!=null)
//		{
//			for(Iterator it=subcaparefs.iterator(); it.hasNext(); )
//			{
//				Object subcaparef = it.next();
//				Object subcapa = state.getAttributeValue(subcaparef, OAVBDIMetaModel.capabilityref_has_capability);
//				addCapabilityProperties(props, subcapa);
//			}
//		}
	}
	
	/**
	 *  Get the state.
	 *  @return The state.
	 */
	public IOAVState getState()
	{
		return state;
	}
	
	/**
	 *  Get the agent state handle.
	 *  @return The state.
	 */
	public Object getHandle()
	{
		return handle;
	}

	/**
	 *  Get the object types contained in the state.
	 *  @return The types.
	 */
	public Set getTypes()
	{
		return types;
	}

	/**
	 *  Get the rulebase.
	 *  The rulebase of the capability includes
	 *  type-specific rules (if any).
	 *  @return The rule base.
	 */
	public Rulebase getRulebase()
	{
		return rulebase;
	}
	
	/**
	 *  Get the string representation.
	 *  @return The string representation.
	 */
	public String toString()
	{
		String name = (String)state.getAttributeValue(handle, OAVBDIMetaModel.modelelement_has_name);
		return "OAVCapabilityModel("+name+")";
	}

	/**
	 *  Copy content from another capability model.
	 * /
	protected void	copyContentFrom(OAVCapabilityModel model)
	{
//		// Todo: use state factory.
//		this.state	= OAVStateFactory.createOAVState(model.getTypeModel());
//		this.handle	= model.getState().cloneObject(model.getHandle(), this.state);
//		this.types	= model.getTypes();
//		this.rulebase	= model.getRulebase();
		
		this.state	= model.getState();
		this.handle	= model.getHandle();
		this.types	= model.getTypes();
		this.typemodel	= model.getTypeModel();
		this.rulebase	= model.getRulebase();
	}*/

	/**
	 *  Add a subcapability.
	 */
	public void addSubcapabilityModel(OAVCapabilityModel cmodel)
	{
		// Add state from subcapability.
		state.addSubstate(cmodel.getState());

		// Add rules from subcapability.
		for(Iterator rules=cmodel.getRulebase().getRules().iterator(); rules.hasNext(); )
		{
			rulebase.addRule((IRule)rules.next());
		}
		// Add types from subcapability.
		types.addAll(cmodel.getTypes());
	}

	/**
	 *  Get the modelinfo.
	 *  @return the modelinfo.
	 */
	public ModelInfo getModelInfo()
	{
		return modelinfo;
	}
	
	//-------- error stuff --------
	
	/**
	 *  Add an entry to the report.
	 *  @param stack	The path to the element to which the entry applies.
	 *  @param message	The problem description. 
	 */
	public void	addEntry(Tuple stack, String message)
	{
		if(entries==null)
			// Use index map to keep insertion order for elements.
			this.entries	= new MultiCollection(new IndexMap().getAsMap(), LinkedHashSet.class);

		if(!entries.getCollection(stack).contains(message))
			entries.put(stack, message);
	}
	
	/**
	 *  Add an external document.
	 *  @param id	The document id as used in anchor tags.
	 *  @param doc	The html text.
	 */
	public void	addDocument(String id, String doc)
	{
		if(externals==null)
			this.externals	= SCollection.createHashMap();
		
		externals.put(id, doc);
	}

	/**
	 *  Get the external documents.
	 */
	public Map	getDocuments()
	{
		return externals;//==null ? Collections.EMPTY_MAP : externals;
	}
	
	/**
	 *  Get the required services.
	 */
	public RequiredServiceInfo[] getRequiredServices()
	{
		return (RequiredServiceInfo[])getRequiredServices(handle).toArray(new RequiredServiceInfo[0]);
	}
	
	/**
	 *  Get the required services.
	 */
	protected List getRequiredServices(Object handle)
	{
		List ret = new ArrayList();
		Collection own = handle!=null ? state.getAttributeValues(handle, OAVBDIMetaModel.capability_has_requiredservices) : null;
		if(own!=null)
		{
			for(Iterator it=own.iterator(); it.hasNext(); )
			{
				Object req = it.next();
				String name = (String)state.getAttributeValue(req, OAVBDIMetaModel.modelelement_has_name);
				Class clazz = (Class)state.getAttributeValue(req, OAVBDIMetaModel.requiredservice_has_class);
				boolean multiple = ((Boolean)state.getAttributeValue(req, OAVBDIMetaModel.requiredservice_has_multiple));
				
				Object binding = state.getAttributeValue(req, OAVBDIMetaModel.requiredservice_has_binding);
				String scope = binding==null? RequiredServiceInfo.SCOPE_APPLICATION: (String)state.getAttributeValue(binding, OAVBDIMetaModel.binding_has_scope);
				String cname = binding==null? null: (String)state.getAttributeValue(binding, OAVBDIMetaModel.binding_has_componentname);
				String ctype = binding==null? null: (String)state.getAttributeValue(binding, OAVBDIMetaModel.binding_has_componenttype);
				boolean dynamic = binding==null? false: ((Boolean)state.getAttributeValue(binding, OAVBDIMetaModel.binding_has_dynamic)).booleanValue();
				boolean create = binding==null? false: ((Boolean)state.getAttributeValue(binding, OAVBDIMetaModel.binding_has_create)).booleanValue();
				boolean recover = binding==null? false: ((Boolean)state.getAttributeValue(binding, OAVBDIMetaModel.binding_has_recover)).booleanValue();
				RequiredServiceBinding bd = new RequiredServiceBinding(name, cname, ctype, dynamic, scope, create, recover);
				ret.add(new RequiredServiceInfo(name, clazz, multiple, bd));
//				ret.add(state.getAttributeValue(it.next(), OAVBDIMetaModel.expression_has_class));
			}
		}
		
		Collection subcapas = handle!=null ? state.getAttributeValues(handle, OAVBDIMetaModel.capability_has_capabilityrefs) : null;
		if(subcapas!=null)
		{
			for(Iterator it=subcapas.iterator(); it.hasNext(); )
			{
				Object subcaparef = it.next();
				Object subcapa  = state.getAttributeValue(subcaparef, OAVBDIMetaModel.capabilityref_has_capability);
				ret.addAll(getRequiredServices(subcapa));
			}
		}
		
		return ret;
	}
	
	/**
	 *  Get the provided services.
	 */
	public ProvidedServiceInfo[] getProvidedServices()
	{
		return (ProvidedServiceInfo[])getProvidedServices(handle).toArray(new ProvidedServiceInfo[0]);
	}
	
	/**
	 *  Get the required services.
	 */
	protected List getProvidedServices(Object handle)
	{
		List ret = new ArrayList();
		Collection own = handle!=null ? state.getAttributeValues(handle, OAVBDIMetaModel.capability_has_providedservices) : null;
		if(own!=null)
		{
			for(Iterator it=own.iterator(); it.hasNext(); )
			{
				Object ob = it.next();
				Class clazz = (Class)state.getAttributeValue(ob, OAVBDIMetaModel.providedservice_has_class);
				String proxytype = (String)state.getAttributeValue(ob, OAVBDIMetaModel.providedservice_has_proxytype);
				Object	mexp	= state.getAttributeValue(ob, OAVBDIMetaModel.providedservice_has_implementation);
				String text = (String)state.getAttributeValue(mexp, OAVBDIMetaModel.expression_has_text);				
				Class impl = (Class)state.getAttributeValue(mexp, OAVBDIMetaModel.expression_has_class);				
				ProvidedServiceInfo psi = new ScopedProvidedServiceInfo(clazz, new ProvidedServiceImplementation(impl, text, proxytype, null), handle); 
				ret.add(psi);
			}
		}
		
		Collection subcapas = handle!=null ? state.getAttributeValues(handle, OAVBDIMetaModel.capability_has_capabilityrefs) : null;
		if(subcapas!=null)
		{
			for(Iterator it=subcapas.iterator(); it.hasNext(); )
			{
				Object subcaparef = it.next();
				Object subcapa  = state.getAttributeValue(subcaparef, OAVBDIMetaModel.capabilityref_has_capability);
				ret.addAll(getProvidedServices(subcapa));
			}
		}
		return ret;
	}
	
	//-------- helpers --------
	
	/**
	 *  Create an argument. 
	 */
	public static IArgument createArgument(IOAVState state, Object capa, Object handle, boolean beliefset)
	{
		String name = (String)state.getAttributeValue(handle, OAVBDIMetaModel.modelelement_has_name);
		String description = (String)state.getAttributeValue(handle, OAVBDIMetaModel.modelelement_has_description);
//		String typename = SReflect.getInnerClassName(beliefset? findBeliefSetType(state, capa, handle)
//			: findBeliefType(state, capa, handle));

		Argument arg = new Argument(name, description, null);
		
		return arg;
	}
	
	/**
	 *  Init an argument.
	 */
	public static void initArgument(Argument arg, IOAVState state, Object capa)
	{
		String name = arg.getName();
		
		boolean beliefset = false;
		Object handle = state.getAttributeValue(capa, OAVBDIMetaModel.capability_has_beliefs, name);
		if(handle==null)
		{
			handle = state.getAttributeValue(capa, OAVBDIMetaModel.capability_has_beliefrefs, name);
			if(handle==null)
			{
				beliefset = true;
				handle = state.getAttributeValue(capa, OAVBDIMetaModel.capability_has_beliefsets, name);
				if(handle==null)
				{
					handle = state.getAttributeValue(capa, OAVBDIMetaModel.capability_has_beliefsetrefs, name);
				}
			}
		}
		
		String typename = SReflect.getInnerClassName(beliefset? findBeliefSetType(state, capa, handle)
			: findBeliefType(state, capa, handle));
		arg.setTypename(typename);
		
		Collection configs = (Collection)state.getAttributeValues(capa, OAVBDIMetaModel.capability_has_configurations);
		if(configs!=null)
		{
			Map defvals = new HashMap();
			for(Iterator it=configs.iterator(); it.hasNext(); )
			{
				Object config = it.next();
				String configname = (String)state.getAttributeValue(config, OAVBDIMetaModel.modelelement_has_name);
				Object val = beliefset? findBeliefSetDefaultValue(state, capa, handle, configname, name)
					: findBeliefDefaultValue(state, capa, handle, configname, name);
				defvals.put(configname, val);
			}
			arg.setDefaultValues(defvals);
		}
		else
		{
			Object val =beliefset? findBeliefSetDefaultValue(state, capa, handle, null, name)
				: findBeliefDefaultValue(state, capa, handle, null, name);
			arg.setDefaultValue(val);
		}
	}
	
	/**
	 *  Find the belief/ref type.
	 */
	protected static Class findBeliefType(IOAVState state, Object scope, Object handle)
	{
		Class	ret	= null;
		
		if(OAVBDIMetaModel.belief_type.equals(state.getType(handle)))
		{
			ret	= (Class)state.getAttributeValue(handle, OAVBDIMetaModel.typedelement_has_class);
		}
		else
		{
			String name = (String)state.getAttributeValue(handle, OAVBDIMetaModel.elementreference_has_concrete);
			Object belref;
			int idx = name.indexOf(".");
			if(idx==-1)
			{
				belref = state.getAttributeValue(scope, OAVBDIMetaModel.capability_has_beliefrefs, name);
				name = (String)state.getAttributeValue(belref, OAVBDIMetaModel.elementreference_has_concrete);
			}
			String capaname = name.substring(0, idx);
			String belname = name.substring(idx+1);
			
			Object subcaparef = state.getAttributeValue(scope, OAVBDIMetaModel.capability_has_capabilityrefs, capaname);
			Object subcapa  = state.getAttributeValue(subcaparef, OAVBDIMetaModel.capabilityref_has_capability);
			
			belref = state.getAttributeValue(subcapa, OAVBDIMetaModel.capability_has_beliefs, belname);
			if(belref==null)
				belref = state.getAttributeValue(subcapa, OAVBDIMetaModel.capability_has_beliefrefs, belname);
			
			ret = findBeliefType(state, subcapa, belref);
		}
		
		return ret;
	}
	
	/**
	 *  Find the belief/ref value.
	 *  Returns the expression text of the default value.
	 */
	// Todo: other kernels provide object values!? 
	protected static String	findBeliefDefaultValue(IOAVState state, Object mcapa, Object handle, String configname, String elemname)
	{
		String ret = null;
		boolean found = false;
		
		// Search initial value in configurations.
		Object config;
		if(configname==null)
		{
			configname = (String)state.getAttributeValue(mcapa, OAVBDIMetaModel.capability_has_defaultconfiguration);
			config = state.getAttributeValue(mcapa, OAVBDIMetaModel.capability_has_configurations, configname);
		}
		else
		{
			config = state.getAttributeValue(mcapa, OAVBDIMetaModel.capability_has_configurations, configname);
		}
	
		if(config!=null)
		{
			Object[] belres;
			if(OAVBDIMetaModel.beliefreference_type.equals(state.getType(handle)))
			{
				String ref = (String)state.getAttributeValue(handle, OAVBDIMetaModel.elementreference_has_concrete);
				belres = AgentRules.resolveMCapability(ref, OAVBDIMetaModel.belief_type, mcapa, state);
			}
			else
			{
				belres = new Object[]{elemname, mcapa};
			}
			
			Collection inibels = state.getAttributeValues(config, OAVBDIMetaModel.configuration_has_initialbeliefs);
			if(inibels!=null)
			{
				for(Iterator it=inibels.iterator(); it.hasNext(); )
				{
					Object inibel = it.next();
					String ref = (String)state.getAttributeValue(inibel, OAVBDIMetaModel.configbelief_has_ref);
					Object[] inibelres = AgentRules.resolveMCapability(ref, OAVBDIMetaModel.belief_type, mcapa, state);
					
					if(Arrays.equals(inibelres, belres))
					{	
						Object exp = state.getAttributeValue(inibel, OAVBDIMetaModel.belief_has_fact);
						if(exp!=null)
						{
							// todo: evaluate expression?
							IParsedExpression parsedexp = (IParsedExpression)state.getAttributeValue(exp, OAVBDIMetaModel.expression_has_parsed);
							if(parsedexp!=null)
							{
								ret = parsedexp.getExpressionText();
							}
							else
							{
								ret	= (String)state.getAttributeValue(exp, OAVBDIMetaModel.expression_has_text);
								if(ret!=null)
									ret	= ret.trim();
							}
							found = true;
						}
					}
				}
			}
		}
		
		// If not found 
		// a) its a belief -> get default value
		// b) its a ref -> recursively call this method with ref, subcapa and config
		
		if(!found)
		{
			if(OAVBDIMetaModel.belief_type.equals(state.getType(handle)))
			{
				Object exp = state.getAttributeValue(handle, OAVBDIMetaModel.belief_has_fact);
				if(exp!=null)
				{
					// todo: evaluate expression?
					IParsedExpression parsedexp = (IParsedExpression)state.getAttributeValue(exp, OAVBDIMetaModel.expression_has_parsed);
					if(parsedexp!=null)
					{
						ret = parsedexp.getExpressionText();
					}
					else
					{
						ret	= (String)state.getAttributeValue(exp, OAVBDIMetaModel.expression_has_text);
						if(ret!=null)
							ret	= ret.trim();
					}
				}
			}
			else
			{
				String name = (String)state.getAttributeValue(handle, OAVBDIMetaModel.elementreference_has_concrete);
				Object belref;
				int idx = name.indexOf(".");
				if(idx==-1)
				{
					belref = state.getAttributeValue(mcapa, OAVBDIMetaModel.capability_has_beliefrefs, name);
					name = (String)state.getAttributeValue(belref, OAVBDIMetaModel.elementreference_has_concrete);
				}
				String capaname = name.substring(0, idx);
				String belname = name.substring(idx+1);
				
				Object subcaparef = state.getAttributeValue(mcapa, OAVBDIMetaModel.capability_has_capabilityrefs, capaname);
				Object subcapa  = state.getAttributeValue(subcaparef, OAVBDIMetaModel.capabilityref_has_capability);
				
				belref = state.getAttributeValue(subcapa, OAVBDIMetaModel.capability_has_beliefs, belname);
				if(belref==null)
					belref = state.getAttributeValue(subcapa, OAVBDIMetaModel.capability_has_beliefrefs, belname);
				
				String subconfigname = null;
				if(config!=null)
				{
					Collection inicapas = state.getAttributeValues(config, OAVBDIMetaModel.configuration_has_initialcapabilities);
					if(inicapas!=null)
					{
						for(Iterator it=inicapas.iterator(); subconfigname==null && it.hasNext(); )
						{
							Object inicapa = it.next();
							
							if(state.getAttributeValue(inicapa, OAVBDIMetaModel.initialcapability_has_ref).equals(subcaparef))
							{	
								subconfigname = (String)state.getAttributeValue(inicapa, OAVBDIMetaModel.initialcapability_has_configuration);
							}
						}
					}
				}
				
				ret = findBeliefDefaultValue(state, subcapa, belref, subconfigname, belname);
			}
		}
		
		return ret;
	}
	
	/**
	 *  Find the belief/ref type.
	 */
	protected static Class findBeliefSetType(IOAVState state, Object scope, Object handle)
	{
		Class	ret	= null;
		
		if(OAVBDIMetaModel.beliefset_type.equals(state.getType(handle)))
		{
			ret	= (Class)state.getAttributeValue(handle, OAVBDIMetaModel.typedelement_has_class);
		}
		else
		{
			String name = (String)state.getAttributeValue(handle, OAVBDIMetaModel.elementreference_has_concrete);
			Object belref;
			int idx = name.indexOf(".");
			if(idx==-1)
			{
				belref = state.getAttributeValue(scope, OAVBDIMetaModel.capability_has_beliefrefs, name);
				name = (String)state.getAttributeValue(belref, OAVBDIMetaModel.elementreference_has_concrete);
			}
			String capaname = name.substring(0, idx);
			String belname = name.substring(idx+1);
			
			Object subcaparef = state.getAttributeValue(scope, OAVBDIMetaModel.capability_has_capabilityrefs, capaname);
			Object subcapa  = state.getAttributeValue(subcaparef, OAVBDIMetaModel.capabilityref_has_capability);
			
			belref = state.getAttributeValue(subcapa, OAVBDIMetaModel.capability_has_beliefs, belname);
			if(belref==null)
				belref = state.getAttributeValue(subcapa, OAVBDIMetaModel.capability_has_beliefrefs, belname);
			
			ret = findBeliefSetType(state, subcapa, belref);
		}
		
		return ret;
	}
	
	/**
	 *  Find the beliefset/ref value.
	 */
	protected static String	findBeliefSetDefaultValue(IOAVState state, Object mcapa, Object handle, String configname, String elemname)
	{
		String ret = null;
		boolean found = false;
		
		// Search initial value in configurations.
		Object config;
		if(configname==null)
		{
			config = state.getAttributeValue(mcapa, OAVBDIMetaModel.capability_has_defaultconfiguration);
		}
		else
		{
			config = state.getAttributeValue(mcapa, OAVBDIMetaModel.capability_has_configurations, configname);
		}
	
		if(config!=null)
		{
			Object[] belsetres;
			if(OAVBDIMetaModel.beliefsetreference_type.equals(state.getType(handle)))
			{
				String ref = (String)state.getAttributeValue(handle, OAVBDIMetaModel.elementreference_has_concrete);
				belsetres = AgentRules.resolveMCapability(ref, OAVBDIMetaModel.beliefset_type, mcapa, state);
			}
			else
			{
				belsetres = new Object[]{elemname, mcapa};
			}
			
			Collection inibelsets = state.getAttributeValues(config, OAVBDIMetaModel.configuration_has_initialbeliefsets);
			if(inibelsets!=null)
			{
				for(Iterator it=inibelsets.iterator(); it.hasNext(); )
				{
					Object inibelset = it.next();
					String ref = (String)state.getAttributeValue(inibelset, OAVBDIMetaModel.configbeliefset_has_ref);
					Object[] inibelsetres = AgentRules.resolveMCapability(ref, OAVBDIMetaModel.beliefset_type, mcapa, state);
					
					if(Arrays.equals(inibelsetres, belsetres))
					{	
						Collection vals = state.getAttributeValues(inibelset, OAVBDIMetaModel.beliefset_has_facts);
						if(vals==null)
						{
							Object exp = state.getAttributeValue(inibelset, OAVBDIMetaModel.beliefset_has_factsexpression);
							if(exp!=null)
							{
								// todo: evaluate expression?
								IParsedExpression parsedexp = (IParsedExpression)state.getAttributeValue(exp, OAVBDIMetaModel.expression_has_parsed);
								if(parsedexp!=null)
								{
									ret = parsedexp.getExpressionText();
								}
								else
								{
									ret	= (String)state.getAttributeValue(exp, OAVBDIMetaModel.expression_has_text);
									if(ret!=null)
										ret	= ret.trim();
								}
								found = true;
							}
						}
						else
						{
							List	rets	= new ArrayList();
							for(Iterator vit=vals.iterator(); vit.hasNext(); )
							{
								Object exp = state.getAttributeValue(inibelset, OAVBDIMetaModel.beliefset_has_factsexpression);
								if(exp!=null)
								{
									// todo: evaluate expression?
									IParsedExpression parsedexp = (IParsedExpression)state.getAttributeValue(exp, OAVBDIMetaModel.expression_has_parsed);
									if(parsedexp!=null)
									{
										rets.add(parsedexp.getExpressionText());
									}
									else
									{
										String	text	= (String)state.getAttributeValue(exp, OAVBDIMetaModel.expression_has_text);
										if(text!=null)
											text	= text.trim();
										rets.add(text);
									}
								}
							}
							found = true;
							ret	= rets.toString();
						}
					}
				}
			}
		}
		
		// If not found 
		// a) its a belief -> get default value
		// b) its a ref -> recursively call this method with ref, subcapa and config
		
		if(!found)
		{
			if(OAVBDIMetaModel.beliefset_type.equals(state.getType(handle)))
			{
				Collection vals = state.getAttributeValues(handle, OAVBDIMetaModel.beliefset_has_facts);
				if(vals==null)
				{
					Object exp = state.getAttributeValue(handle, OAVBDIMetaModel.beliefset_has_factsexpression);
					if(exp!=null)
					{
						// todo: evaluate expression?
						IParsedExpression parsedexp = (IParsedExpression)state.getAttributeValue(exp, OAVBDIMetaModel.expression_has_parsed);
						if(parsedexp!=null)
						{
							ret = parsedexp.getExpressionText();
						}
						else
						{
							ret	= (String)state.getAttributeValue(exp, OAVBDIMetaModel.expression_has_text);
							if(ret!=null)
								ret	= ret.trim();
						}
						found = true;
					}
				}
				else
				{
					List	rets	= new ArrayList();
					for(Iterator vit=vals.iterator(); vit.hasNext(); )
					{
						Object exp = state.getAttributeValue(handle, OAVBDIMetaModel.beliefset_has_factsexpression);
						if(exp!=null)
						{
							// todo: evaluate expression?
							IParsedExpression parsedexp = (IParsedExpression)state.getAttributeValue(exp, OAVBDIMetaModel.expression_has_parsed);
							if(parsedexp!=null)
							{
								rets.add(parsedexp.getExpressionText());
							}
							else
							{
								String	text	= (String)state.getAttributeValue(exp, OAVBDIMetaModel.expression_has_text);
								if(text!=null)
									text	= text.trim();
								rets.add(text);
							}
						}
					}
					found = true;
					ret	= rets.toString();
				}
			}
			else
			{
				String name = (String)state.getAttributeValue(handle, OAVBDIMetaModel.elementreference_has_concrete);
				Object belref;
				int idx = name.indexOf(".");
				if(idx==-1)
				{
					belref = state.getAttributeValue(mcapa, OAVBDIMetaModel.capability_has_beliefsetrefs, name);
					name = (String)state.getAttributeValue(belref, OAVBDIMetaModel.elementreference_has_concrete);
				}
				String capaname = name.substring(0, idx);
				String belname = name.substring(idx+1);
				
				Object subcaparef = state.getAttributeValue(mcapa, OAVBDIMetaModel.capability_has_capabilityrefs, capaname);
				Object subcapa  = state.getAttributeValue(subcaparef, OAVBDIMetaModel.capabilityref_has_capability);
				
				belref = state.getAttributeValue(subcapa, OAVBDIMetaModel.capability_has_beliefsets, belname);
				if(belref==null)
					belref = state.getAttributeValue(subcapa, OAVBDIMetaModel.capability_has_beliefsetrefs, belname);
				
				String subconfigname = null;
				if(config!=null)
				{
					Collection inicapas = state.getAttributeValues(config, OAVBDIMetaModel.configuration_has_initialcapabilities);
					if(inicapas!=null)
					{
						for(Iterator it=inicapas.iterator(); subconfigname==null && it.hasNext(); )
						{
							Object inicapa = it.next();
							if(state.getAttributeValue(inicapa, OAVBDIMetaModel.initialcapability_has_ref).equals(subcaparef))
							{	
								subconfigname = (String)state.getAttributeValue(inicapa, OAVBDIMetaModel.initialcapability_has_configuration);
							}
						}
					}
				}
				
				ret = findBeliefSetDefaultValue(state, subcapa, belref, subconfigname, belname);
			}
		}
		
		return ret;
	}
}
