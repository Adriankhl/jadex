package jadex.bdi;

import jadex.bdi.model.OAVAgentModel;
import jadex.bdi.model.OAVBDIMetaModel;
import jadex.bdi.model.OAVCapabilityModel;
import jadex.bdi.model.editable.IMECapability;
import jadex.bdi.model.impl.flyweights.MCapabilityFlyweight;
import jadex.bdi.runtime.interpreter.BDIInterpreter;
import jadex.bdi.runtime.interpreter.OAVBDIRuntimeModel;
import jadex.bridge.IComponentInstance;
import jadex.bridge.IExternalAccess;
import jadex.bridge.IInternalAccess;
import jadex.bridge.IResourceIdentifier;
import jadex.bridge.modelinfo.IModelInfo;
import jadex.bridge.modelinfo.ModelInfo;
import jadex.bridge.service.RequiredServiceBinding;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.bridge.service.annotation.Service;
import jadex.bridge.service.annotation.ServiceComponent;
import jadex.bridge.service.annotation.ServiceShutdown;
import jadex.bridge.service.annotation.ServiceStart;
import jadex.bridge.service.search.SServiceProvider;
import jadex.bridge.service.types.cms.IComponentDescription;
import jadex.bridge.service.types.factory.IComponentAdapter;
import jadex.bridge.service.types.factory.IComponentAdapterFactory;
import jadex.bridge.service.types.factory.IComponentFactory;
import jadex.bridge.service.types.library.ILibraryService;
import jadex.bridge.service.types.library.ILibraryServiceListener;
import jadex.commons.Tuple2;
import jadex.commons.future.DelegationResultListener;
import jadex.commons.future.ExceptionDelegationResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
/* $if !android $ */
import jadex.commons.gui.SGUI;
/* $endif $ */
import jadex.rules.state.IOAVState;
import jadex.rules.state.IOAVStateListener;
import jadex.rules.state.OAVAttributeType;
import jadex.rules.state.OAVObjectType;
import jadex.rules.state.OAVTypeModel;
import jadex.rules.state.javaimpl.OAVStateFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/* $if !android $ */
import javax.swing.Icon;
import javax.swing.UIDefaults;
/* $endif $ */

/**
 *  Factory for creating Jadex V2 BDI agents.
 */
@Service
public class BDIAgentFactory	implements IDynamicBDIFactory, IComponentFactory
{
	//-------- constants --------
	
	/** The BDI agent file type. */
	public static final String	FILETYPE_BDIAGENT	= "BDI Agent";
	
	/** The BDI capability file type. */
	public static final String	FILETYPE_BDICAPABILITY	= "BDI Capability";
	
	/**
	 * The image icons.
	 */
	/* $if !android $ */
	protected static final UIDefaults icons = new UIDefaults(new Object[]
	{
		"bdi_agent",	SGUI.makeIcon(BDIAgentFactory.class, "/jadex/bdi/images/bdi_agent.png"),
		"bdi_capability",	SGUI.makeIcon(BDIAgentFactory.class, "/jadex/bdi/images/bdi_capability.png")
	});
	/* $endif $ */

	//-------- attributes --------
	
	/** The factory properties. */
	protected Map props;
	
	/** The model loader. */
	protected OAVBDIModelLoader loader;
	
	/** The provider. */
	@ServiceComponent
	protected IInternalAccess component;
		
	/** The types of a manually edited agent model. */
	protected Map mtypes;
	
	/** The library service listener */
	protected ILibraryServiceListener libservicelistener;
	
	/** The library service */
	protected ILibraryService libservice;
	
	//-------- constructors --------
	
	/**
	 *  Create a new agent factory.
	 */
	// Constructor used by GPMN factory.
	public BDIAgentFactory(Map props, IInternalAccess component)
	{
		this(props);
		this.component	= component;
	}
	
	/**
	 *  Create a new agent factory.
	 */
	public BDIAgentFactory(Map props)
	{
		this.props = props;
		this.loader	= new OAVBDIModelLoader(props);
		this.mtypes	= Collections.synchronizedMap(new WeakHashMap());
		this.libservicelistener = new ILibraryServiceListener()
		{
			public IFuture<Void> resourceIdentifierRemoved(IResourceIdentifier rid)
			{
				loader.clearModelCache();
				return IFuture.DONE;
			}
			
			public IFuture<Void> resourceIdentifierAdded(IResourceIdentifier rid)
			{
				loader.clearModelCache();
				return IFuture.DONE;
			}
		};
	}
	
	/**
	 *  Start the service.
	 */
	@ServiceStart
	public synchronized IFuture	startService()
	{
		Future	fut	= new Future();
		SServiceProvider.getService(component.getServiceContainer(), ILibraryService.class, RequiredServiceInfo.SCOPE_PLATFORM)
			.addResultListener(component.createResultListener(new DelegationResultListener(fut)
		{
			public void customResultAvailable(Object result)
			{
				libservice = (ILibraryService) result;
				libservice.addLibraryServiceListener(libservicelistener);
				super.customResultAvailable(null);
			}
		}));
		return fut;
	}
	
	/**
	 *  Shutdown the service.
	 *  @param listener The listener.
	 */
	@ServiceShutdown
	public synchronized IFuture	shutdownService()
	{
		Future	fut	= new Future();
		SServiceProvider.getService(component.getServiceContainer(), ILibraryService.class, RequiredServiceInfo.SCOPE_PLATFORM)
			.addResultListener(component.createResultListener(new DelegationResultListener(fut)
		{
			public void customResultAvailable(Object result)
			{
				ILibraryService libService = (ILibraryService) result;
				libService.removeLibraryServiceListener(libservicelistener);
				super.customResultAvailable(null);
			}
		}));
		return fut;
	}
	
	//-------- IAgentFactory interface --------
	
	/**
	 * Create a component instance.
	 * @param adapter The component adapter.
	 * @param model The component model.
	 * @param config The name of the configuration (or null for default configuration) 
	 * @param arguments The arguments for the agent as name/value pairs.
	 * @param parent The parent component (if any).
	 * @return An instance of a component.
	 */
	public IFuture<Tuple2<IComponentInstance, IComponentAdapter>> createComponentInstance(final IComponentDescription desc, final IComponentAdapterFactory factory, final IModelInfo modelinfo, 
		final String config, final Map<String, Object> arguments, final IExternalAccess parent, final RequiredServiceBinding[] bindings, final boolean copy, final Future<Void> init)
	{
		final Future<Tuple2<IComponentInstance, IComponentAdapter>> ret = new Future<Tuple2<IComponentInstance, IComponentAdapter>>();
		
		if(libservice!=null)
		{
			libservice.getClassLoader(modelinfo.getResourceIdentifier()).addResultListener(
				new ExceptionDelegationResultListener<ClassLoader, Tuple2<IComponentInstance, IComponentAdapter>>(ret)
			{
				public void customResultAvailable(ClassLoader cl)
				{
					try
					{
				//		OAVAgentModel amodel = (OAVAgentModel)model;
						OAVAgentModel amodel = (OAVAgentModel)loader.loadModel(modelinfo.getFilename(), null, 
							cl, modelinfo.getResourceIdentifier());
						
						// Create type model for agent instance (e.g. holding dynamically loaded java classes).
						OAVTypeModel tmodel	= new OAVTypeModel(desc.getName().getLocalName()+"_typemodel", amodel.getState().getTypeModel().getClassLoader());
				//		OAVTypeModel tmodel	= new OAVTypeModel(model.getName()+"_typemodel", ((OAVAgentModel)model).getTypeModel().getClassLoader());
						tmodel.addTypeModel(amodel.getState().getTypeModel());
						tmodel.addTypeModel(OAVBDIRuntimeModel.bdi_rt_model);
						IOAVState	state	= OAVStateFactory.createOAVState(tmodel); 
						state.addSubstate(amodel.getState());
						
						BDIInterpreter bdii = new BDIInterpreter(desc, factory, state, amodel, config, arguments, parent, bindings, props, copy, init);
						ret.setResult(new Tuple2<IComponentInstance, IComponentAdapter>(bdii, bdii.getAgentAdapter()));
					}
					catch(Exception e)
					{
						ret.setException(e);
					}
				}
			});
		}
		
		// For platform bootstrapping
		else
		{
			try
			{
				ClassLoader cl = getClass().getClassLoader();
		//		OAVAgentModel amodel = (OAVAgentModel)model;
				OAVAgentModel amodel = (OAVAgentModel)loader.loadModel(modelinfo.getFilename(), null, 
					cl, modelinfo.getResourceIdentifier());
				
				// Create type model for agent instance (e.g. holding dynamically loaded java classes).
				OAVTypeModel tmodel	= new OAVTypeModel(desc.getName().getLocalName()+"_typemodel", amodel.getState().getTypeModel().getClassLoader());
		//		OAVTypeModel tmodel	= new OAVTypeModel(model.getName()+"_typemodel", ((OAVAgentModel)model).getTypeModel().getClassLoader());
				tmodel.addTypeModel(amodel.getState().getTypeModel());
				tmodel.addTypeModel(OAVBDIRuntimeModel.bdi_rt_model);
				IOAVState	state	= OAVStateFactory.createOAVState(tmodel); 
				state.addSubstate(amodel.getState());
				
				BDIInterpreter bdii = new BDIInterpreter(desc, factory, state, amodel, config, arguments, parent, bindings, props, copy, init);
				ret.setResult(new Tuple2<IComponentInstance, IComponentAdapter>(bdii, bdii.getAgentAdapter()));
			}
			catch(Exception e)
			{
				ret.setException(e);
			}
		}
		
		return ret;
	}
	
	// Needed for gpmn factory
	/**
	 * Create a component instance.
	 * @param adapter The component adapter.
	 * @param model The component model.
	 * @param config The name of the configuration (or null for default configuration) 
	 * @param arguments The arguments for the agent as name/value pairs.
	 * @param parent The parent component (if any).
	 * @return An instance of a component.
	 */
	public Tuple2<IComponentInstance, IComponentAdapter> createComponentInstance(IComponentDescription desc, IComponentAdapterFactory factory, OAVAgentModel amodel, 
		String config, Map<String, Object> arguments, IExternalAccess parent, RequiredServiceBinding[] bindings, boolean copy, Future<Void> ret)
	{
		// Create type model for agent instance (e.g. holding dynamically loaded java classes).
		OAVTypeModel tmodel	= new OAVTypeModel(desc.getName().getLocalName()+"_typemodel", amodel.getState().getTypeModel().getClassLoader());
//		OAVTypeModel tmodel	= new OAVTypeModel(model.getName()+"_typemodel", ((OAVAgentModel)model).getTypeModel().getClassLoader());
		tmodel.addTypeModel(amodel.getState().getTypeModel());
		tmodel.addTypeModel(OAVBDIRuntimeModel.bdi_rt_model);
		IOAVState	state	= OAVStateFactory.createOAVState(tmodel); 
		state.addSubstate(amodel.getState());
		
		BDIInterpreter bdii = new BDIInterpreter(desc, factory, state, amodel, config, arguments, parent, bindings, props, copy, ret);
		return new Tuple2<IComponentInstance, IComponentAdapter>(bdii, bdii.getAgentAdapter());
	}
	
	/**
	 *  Load a  model.
	 *  @param filename The model (e.g. file name).
	 *  @param The imports (if any).
	 *  @return The loaded model.
	 */
	public IFuture<IModelInfo> loadModel(final String filename, final String[] imports, final IResourceIdentifier rid)
	{
		final Future<IModelInfo> ret = new Future<IModelInfo>();
//		System.out.println("filename: "+filename);
		
		if(libservice!=null)
		{
			libservice.getClassLoader(rid).addResultListener(
				new ExceptionDelegationResultListener<ClassLoader, IModelInfo>(ret)
			{
				public void customResultAvailable(ClassLoader cl)
				{
					try
					{
//						System.out.println("loading bdi: "+filename);
						OAVCapabilityModel loaded = (OAVCapabilityModel)loader.loadModel(filename, imports, cl, rid);
						ret.setResult(loaded.getModelInfo());
					}
					catch(Exception e)
					{
						ret.setException(e);
//						System.err.println(filename);
//						throw e instanceof RuntimeException ? (RuntimeException)e : new RuntimeException(e);
					}
				}
			});
		}
		else
		{
			try
			{
//				System.out.println("loading bdi: "+filename);
				ClassLoader cl = getClass().getClassLoader();
				OAVCapabilityModel loaded = (OAVCapabilityModel)loader.loadModel(filename, imports, cl, rid);
				ret.setResult(loaded.getModelInfo());
			}
			catch(Exception e)
			{
				ret.setException(e);
//				System.err.println(filename);
//				throw e instanceof RuntimeException ? (RuntimeException)e : new RuntimeException(e);
			}
		}
		
		return ret;
		
//		Future<IModelInfo> ret = new Future<IModelInfo>();
//		try
//		{
////			System.out.println("loading bdi: "+filename);
//			OAVCapabilityModel loaded = (OAVCapabilityModel)loader.loadModel(filename, imports, 
//				libservice.getClassLoader(rid), rid);
//			ret.setResult(loaded.getModelInfo());
//		}
//		catch(Exception e)
//		{
//			ret.setException(e);
////			System.err.println(filename);
////			throw e instanceof RuntimeException ? (RuntimeException)e : new RuntimeException(e);
//		}
//		return ret;
	}
	
	/**
	 *  Test if a model can be loaded by the factory.
	 *  @param model The model (e.g. file name).
	 *  @param The imports (if any).
	 *  @return True, if model can be loaded.
	 */
	public IFuture<Boolean> isLoadable(String model, String[] imports, IResourceIdentifier rid)
	{
//		init();
		boolean loadable = model.toLowerCase().endsWith(".agent.xml") || model.toLowerCase().endsWith(".capability.xml");
		return new Future<Boolean>(loadable? Boolean.TRUE: Boolean.FALSE);
//		return loader.isLoadable(model, null);
//		return model.toLowerCase().endsWith(".agent.xml") || model.toLowerCase().endsWith(".capability.xml");
		
//		boolean ret =  model.indexOf("/bdi/")!=-1 || model.indexOf(".bdi.")!=-1 || model.indexOf("\\bdi\\")!=-1 
//			|| model.indexOf("v2")!=-1 || model.indexOf("V2")!=-1;
	
//		System.out.println(model+" "+ret);
		
//		return ret;
	}
	
	/**
	 *  Test if a model is startable (e.g. an component).
	 *  @param model The model (e.g. file name).
	 *  @param The imports (if any).
	 *  @return True, if startable (and loadable).
	 */
	public IFuture<Boolean> isStartable(String model, String[] imports, IResourceIdentifier rid)
	{
		boolean startable = model!=null && model.toLowerCase().endsWith(".agent.xml");
		return new Future(startable? Boolean.TRUE: Boolean.FALSE);
//		return SXML.isAgentFilename(model);
	}


	/**
	 *  Get the names of ADF file types supported by this factory.
	 */
	public String[] getComponentTypes()
	{
		return new String[]{FILETYPE_BDIAGENT, FILETYPE_BDICAPABILITY};
	}

	/**
	 *  Get a default icon for a file type.
	 */
	/* $if !android $ */
	public IFuture<Icon> getComponentTypeIcon(String type)
	{
		return new Future<Icon>(type.equals(FILETYPE_BDIAGENT) ? icons.getIcon("bdi_agent")
			: type.equals(FILETYPE_BDICAPABILITY) ? icons.getIcon("bdi_capability") : null);
	}
	/* $endif $ */

	/**
	 *  Get the component type of a model.
	 *  @param model The model (e.g. file name).
	 *  @param The imports (if any).
	 */
	public IFuture<String> getComponentType(String model, String[] imports, IResourceIdentifier rid)
	{
		return new Future<String>(model.toLowerCase().endsWith(".agent.xml") ? FILETYPE_BDIAGENT
			: model.toLowerCase().endsWith(".capability.xml") ? FILETYPE_BDICAPABILITY
			: null);
	}
	
	/**
	 *  Get the properties.
	 *  Arbitrary properties that can e.g. be used to
	 *  define kernel-specific settings to configure tools.
	 *  @param type	The component type. 
	 *  @return The properties or null, if the component type is not supported by this factory.
	 */
	public Map	getProperties(String type)
	{
		return FILETYPE_BDIAGENT.equals(type) || FILETYPE_BDICAPABILITY.equals(type)
			? props : null;
	}

	/**
	 *  Create a new agent model, which can be manually edited before starting.
	 *  @param name	A type name for the agent model.
	 */
	public IFuture<IMECapability>	createAgentModel(final String name, final String pkg, final String[] imports, final IResourceIdentifier rid)
	{
		final Future<IMECapability>	ret	= new Future<IMECapability>();
		
		component.getServiceContainer().searchService(ILibraryService.class, RequiredServiceInfo.SCOPE_PLATFORM)
			.addResultListener(new ExceptionDelegationResultListener<ILibraryService, IMECapability>(ret)
		{
			public void customResultAvailable(ILibraryService libservice)
			{
				libservice.getClassLoader(rid).addResultListener(new ExceptionDelegationResultListener<ClassLoader, IMECapability>(ret)
				{
					public void customResultAvailable(ClassLoader cl)
					{
						OAVTypeModel	typemodel	= new OAVTypeModel(name+"_typemodel", cl);
						// Requires runtime meta model, because e.g. user conditions can refer to runtime elements (belief, goal, etc.) 
						typemodel.addTypeModel(OAVBDIRuntimeModel.bdi_rt_model);
						IOAVState	state	= OAVStateFactory.createOAVState(typemodel);
						
						final Set	types	= new HashSet();
						IOAVStateListener	listener	= new IOAVStateListener()
						{
							public void objectAdded(Object id, OAVObjectType type, boolean root)
							{
								// Add the type and its supertypes (if not already contained).
								while(type!=null && types.add(type))
									type	= type.getSupertype();
							}
							
							public void objectModified(Object id, OAVObjectType type, OAVAttributeType attr, Object oldvalue, Object newvalue)
							{
							}
							
							public void objectRemoved(Object id, OAVObjectType type)
							{
							}
						};
						state.addStateListener(listener, false);
						
						Object	handle	= state.createRootObject(OAVBDIMetaModel.agent_type);
						state.setAttributeValue(handle, OAVBDIMetaModel.modelelement_has_name, name);
//						state.setAttributeValue(handle, OAVBDIMetaModel.capability_has_package, pkg);
//						if(imports!=null)
//						{
//							for(int i=0; i<imports.length; i++)
//							{
//								state.addAttributeValue(handle, OAVBDIMetaModel.capability_has_imports, imports[i]);
//							}
//						}
						
						mtypes.put(handle, new Object[]{types, listener});
						
						ModelInfo	info	= new ModelInfo();
						info.setName(name);
						info.setPackage(pkg);
						info.setImports(imports);

						ret.setResult(new MCapabilityFlyweight(state, handle, info));
					}
				});
			}
		});
		
		return ret;
	}
	
	/**
	 *  Register a manually edited agent model in the factory.
	 *  @param model	The edited agent model.
	 *  @param filename	The filename for accessing the model.
	 *  @return	The startable agent model.
	 */
	public IFuture<IModelInfo>	registerAgentModel(IMECapability model, String filename)
	{
		Future<IModelInfo>	fut	= new Future<IModelInfo>();
		OAVCapabilityModel	ret;
		MCapabilityFlyweight	fw	= (MCapabilityFlyweight)model;
		IOAVState	state	= fw.getState();
		Object	handle	= fw.getHandle();
		Object[]	types	= (Object[])mtypes.get(handle);
		if(types!=null)
		{
			state.removeStateListener((IOAVStateListener)types[1]);
		}
		
//		Report	report	= new Report();
		if(state.getType(handle).isSubtype(OAVBDIMetaModel.agent_type))
		{
			ret	=  new OAVAgentModel(state, handle, fw.getModelInfo(), 
				(Set)(types!=null ? types[0] : null), System.currentTimeMillis(), null);
		}
		else
		{
			ret	=  new OAVCapabilityModel(state, handle, fw.getModelInfo(), (Set)(types!=null ? types[0] : null),
				System.currentTimeMillis(), null);
		}
		
		try
		{
			loader.createAgentModelEntry(ret, (ModelInfo)ret.getModelInfo());
			((ModelInfo)ret.getModelInfo()).setFilename(filename);
			loader.registerModel(filename, ret);
			fut.setResult(ret.getModelInfo());
		}
		catch(Exception e)
		{
			fut.setException(e);
		}
		
		return fut;
	}
}
