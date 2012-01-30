package jadex.bpmn;

import jadex.bpmn.model.MBpmnModel;
import jadex.bpmn.runtime.BpmnInterpreter;
import jadex.bridge.ComponentIdentifier;
import jadex.bridge.IComponentInstance;
import jadex.bridge.IExternalAccess;
import jadex.bridge.IResourceIdentifier;
import jadex.bridge.modelinfo.IModelInfo;
import jadex.bridge.service.BasicService;
import jadex.bridge.service.IServiceProvider;
import jadex.bridge.service.RequiredServiceBinding;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.bridge.service.search.SServiceProvider;
import jadex.bridge.service.types.cms.IComponentDescription;
import jadex.bridge.service.types.factory.IComponentAdapter;
import jadex.bridge.service.types.factory.IComponentAdapterFactory;
import jadex.bridge.service.types.factory.IComponentFactory;
import jadex.bridge.service.types.library.ILibraryService;
import jadex.bridge.service.types.library.ILibraryServiceListener;
import jadex.commons.Tuple2;
import jadex.commons.future.DefaultResultListener;
import jadex.commons.future.DelegationResultListener;
import jadex.commons.future.ExceptionDelegationResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.commons.future.IIntermediateResultListener;
/* $if !android $ */
import jadex.commons.gui.SGUI;
/* $endif $ */

import java.util.HashMap;
import java.util.Map;

/* $if !android $ */
import javax.swing.Icon;
import javax.swing.UIDefaults;
/* $endif $ */

/**
 *  Foctory for loading bpmn processes.
 */
public class BpmnFactory extends BasicService implements IComponentFactory
{
	//-------- constants --------
	
	/** The micro agent file type. */
	public static final String	FILETYPE_BPMNPROCESS = "BPMN Process";
	
	/**
	 * The image icons.
	 */
	/* $if !android $ */
	protected static final UIDefaults icons = new UIDefaults(new Object[]
	{
		"bpmn_process",	SGUI.makeIcon(BpmnFactory.class, "/jadex/bpmn/images/bpmn_process.png"),
	});
	/* $endif $ */
	
	//-------- attributes --------
	
	/** The provider. */
	protected IServiceProvider provider;
	
	/** Running process instances */
	protected Map processes;
	
	/** The model loader */
	protected BpmnModelLoader loader;
	
	/** The library service. */
	protected ILibraryService libservice;
	
	/** The library service listener */
	protected ILibraryServiceListener libservicelistener;
	
	/** The properties. */
	protected Map properties;
	
	//-------- constructors --------
	
	/**
	 *  Create a new factory for startup.
	 *  @param platform	The platform.
	 */
	// This constructor is used by the Starter class and the ADFChecker plugin. 
	public BpmnFactory(String providerid)
	{
		super(new ComponentIdentifier(providerid), IComponentFactory.class, null);
		this.loader = new BpmnModelLoader();
	}
	
	/**
	 *  Create a new BpmnProcessService.
	 */
	public BpmnFactory(IServiceProvider provider, Map properties)
	{
		super(provider.getId(), IComponentFactory.class, null);

		this.provider = provider;
		this.processes = new HashMap();
		this.loader = new BpmnModelLoader();
		this.properties	= properties;
		
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
	public IFuture<Void> startService()
	{
		final Future<Void> ret = new Future<Void>();
		SServiceProvider.getService(provider, ILibraryService.class, RequiredServiceInfo.SCOPE_PLATFORM)
			.addResultListener(new DelegationResultListener(ret)
		{
			public void customResultAvailable(Object result)
			{
				libservice = (ILibraryService)result;
				libservice.addLibraryServiceListener(libservicelistener);
				BpmnFactory.super.startService().addResultListener(new DelegationResultListener(ret));
			}
		});
		return ret;
	}
	
	//-------- methods --------
	
	/**
	 *  Start the service.
	 * /
	public synchronized IFuture	startService()
	{
		return super.startService();
	}*/
	
	/**
	 *  Shutdown the service.
	 *  @param listener The listener.
	 */
	public synchronized IFuture<Void>	shutdownService()
	{
		SServiceProvider.getService(provider, ILibraryService.class, RequiredServiceInfo.SCOPE_PLATFORM).addResultListener(new DefaultResultListener()
		{
			public void resultAvailable(Object result)
			{
				libservice = (ILibraryService)result;
				libservice.removeLibraryServiceListener(libservicelistener);
			}
		});
		return super.shutdownService();
	}
	
	/**
	 *  Load a  model.
	 *  @param model The model (e.g. file name).
	 *  @param The imports (if any).
	 *  @return The loaded model.
	 */
	public IFuture<IModelInfo> loadModel(final String model, final String[] imports, final IResourceIdentifier rid)
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
						MBpmnModel amodel = loader.loadBpmnModel(model, imports, cl, new Object[]{rid, getProviderId().getRoot()});
						ret.setResult(amodel.getModelInfo());
					}
					catch(Exception e)
					{
						ret.setException(e);
					}
				}
			});
		}
		else
		{
			try
			{
				ClassLoader cl = getClass().getClassLoader();
				MBpmnModel amodel = loader.loadBpmnModel(model, imports, cl, new Object[]{rid, getProviderId().getRoot()});
				ret.setResult(amodel.getModelInfo());
			}
			catch(Exception e)
			{
				ret.setException(e);
			}			
		}
		
		return ret;
	}
	
	/**
	 *  Test if a model can be loaded by the factory.
	 *  @param model The model (e.g. file name).
	 *  @param The imports (if any).
	 *  @return True, if model can be loaded.
	 */
	public IFuture<Boolean> isLoadable(String model, String[] imports, IResourceIdentifier rid)
	{
		return new Future<Boolean>(model.endsWith(".bpmn")? Boolean.TRUE: Boolean.FALSE);
	}

	/**
	 *  Test if a model is startable (e.g. an component).
	 *  @param model The model (e.g. file name).
	 *  @param The imports (if any).
	 *  @return True, if startable (and loadable).
	 */
	public IFuture<Boolean> isStartable(String model, String[] imports, IResourceIdentifier rid)
	{
		return new Future<Boolean>(model.endsWith(".bpmn")? Boolean.TRUE: Boolean.FALSE);
	}
	
	/**
	 *  Get the names of ADF file types supported by this factory.
	 */
	public String[] getComponentTypes()
	{
		return new String[]{FILETYPE_BPMNPROCESS};
	}

	/**
	 *  Get a default icon for a file type.
	 */
	/* $if !android $ */
	public IFuture<Icon> getComponentTypeIcon(String type)
	{
		return new Future<Icon>(type.equals(FILETYPE_BPMNPROCESS)? icons.getIcon("bpmn_process") : null);
	}
	/* $else $
	public IFuture<Void> getComponentTypeIcon(String type) {
		return new Future(null);
	}
	$endif $ */

	/**
	 *  Get the component type of a model.
	 *  @param model The model (e.g. file name).
	 *  @param The imports (if any).
	 */
	public IFuture<String> getComponentType(String model, String[] imports, IResourceIdentifier rid)
	{
		return new Future<String>(model.toLowerCase().endsWith(".bpmn") ? FILETYPE_BPMNPROCESS: null);
	}
	
	/**
	 * Create a component instance.
	 * @param adapter The component adapter.
	 * @param model The component model.
	 * @param config The name of the configuration (or null for default configuration) 
	 * @param arguments The arguments for the agent as name/value pairs.
	 * @param parent The parent component (if any).
	 * @return An instance of a component.
	 */
	public IFuture<Tuple2<IComponentInstance, IComponentAdapter>> createComponentInstance(final IComponentDescription desc, final IComponentAdapterFactory factory, 
		final IModelInfo modelinfo, final String config, final Map<String, Object> arguments, final IExternalAccess parent, final RequiredServiceBinding[] bindings, 
		final boolean copy, final IIntermediateResultListener<Tuple2<String, Object>> resultlistener, final Future<Void> inited)
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
						MBpmnModel model = loader.loadBpmnModel(modelinfo.getFilename(), null, cl, new Object[]{modelinfo.getResourceIdentifier(), getProviderId().getRoot()});
						BpmnInterpreter interpreter = new BpmnInterpreter(desc, factory, model, arguments, config, parent, null, null, null, bindings, copy, resultlistener, inited);
						ret.setResult(new Tuple2<IComponentInstance, IComponentAdapter>(interpreter, interpreter.getComponentAdapter()));
					}
					catch(Exception e)
					{
						ret.setException(e);
					}
				}
			});
		}
		else
		{
			try
			{
				ClassLoader cl = getClass().getClassLoader();
				MBpmnModel model = loader.loadBpmnModel(modelinfo.getFilename(), null, cl, new Object[]{modelinfo.getResourceIdentifier(), getProviderId().getRoot()});
				BpmnInterpreter interpreter = new BpmnInterpreter(desc, factory, model, arguments, config, parent, null, null, null, bindings, copy, resultlistener, inited);
				ret.setResult(new Tuple2<IComponentInstance, IComponentAdapter>(interpreter, interpreter.getComponentAdapter()));
			}
			catch(Exception e)
			{
				ret.setException(e);
			}
		}
		
		return ret;
//		try
//		{
//			MBpmnModel model = loader.loadBpmnModel(modelinfo.getFilename(), null, libservice==null? getClass().getClassLoader(): libservice.getClassLoader(modelinfo.getResourceIdentifier()), modelinfo.getResourceIdentifier());
//			BpmnInterpreter interpreter = new BpmnInterpreter(desc, factory, model, arguments, config, parent, null, null, null, bindings, copy, inited);
//			return new Future<Tuple2<IComponentInstance, IComponentAdapter>>(new Tuple2<IComponentInstance, IComponentAdapter>(interpreter, interpreter.getComponentAdapter()));
//		}
//		catch(Exception e)
//		{
//			return new Future<Tuple2<IComponentInstance, IComponentAdapter>>(e);
//		}
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
		return FILETYPE_BPMNPROCESS.equals(type)
		? properties : null;
	}
}
