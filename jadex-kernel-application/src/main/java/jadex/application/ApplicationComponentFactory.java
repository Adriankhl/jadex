package jadex.application;

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
import jadex.bridge.service.types.factory.IComponentFactoryExtensionService;
import jadex.bridge.service.types.library.ILibraryService;
import jadex.bridge.service.types.library.ILibraryServiceListener;
import jadex.commons.Tuple2;
import jadex.commons.future.CollectionResultListener;
import jadex.commons.future.DefaultResultListener;
import jadex.commons.future.DelegationResultListener;
import jadex.commons.future.ExceptionDelegationResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.commons.future.IIntermediateResultListener;
import jadex.component.ComponentInterpreter;
import jadex.kernelbase.CacheableKernelModel;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/* $if !android $ */
import jadex.commons.gui.SGUI;

import javax.swing.Icon;
import javax.swing.UIDefaults;
/* $endif $ */


/**
 *  Factory for default contexts.
 *  No special properties supported, yet.
 */
public class ApplicationComponentFactory extends BasicService implements IComponentFactory
{
	//-------- constants --------
	
	/** The application component file type. */
	public static final String	FILETYPE_APPLICATION = "Application";
	
//	/** The application file extension. */
//	public static final String	FILE_EXTENSION_APPLICATION	= ".application.xml";

	/**
	 * The image icons.
	 */
	/* $if !android $ */
	protected static final UIDefaults icons = new UIDefaults(new Object[]
	{
		"application", SGUI.makeIcon(ApplicationComponentFactory.class, "/jadex/application/images/application.png"),
	});
	/* $endif $ */
	
	//-------- attributes --------
	
	/** The application model loader. */
	protected ApplicationModelLoader loader;
	
	/** The provider. */
	protected IServiceProvider provider;
	
	/** The library service. */
	protected ILibraryService libservice;
	
	/** The library service listener */
	protected ILibraryServiceListener libservicelistener;
	
	//-------- constructors --------
	
	/**
	 *  Create a new application factory for startup.
	 *  @param platform	The platform.
	 *  @param mappings	The XML reader mappings of supported spaces (if any).
	 */
	// This constructor is used by the Starter class and the ADFChecker plugin. 
	public ApplicationComponentFactory(String providerid)
	{
		super(new ComponentIdentifier(providerid), IComponentFactory.class, null);
		
		// Todo: hack!!! make extensions configurable also for reflective constructor (how?)
		String[]	extensions	= new String[]
		{
			"jadex.extension.envsupport.MEnvSpaceType", "getXMLMapping",
			"jadex.extension.agr.AGRExtensionService", "getXMLMapping"
		};
		List<Set<?>>	mappings	= new ArrayList<Set<?>>();
		for(int i=0; i<extensions.length; i+=2)
		{
			try
			{
				Class<?>	clazz	= Class.forName(extensions[i], true, getClass().getClassLoader());
				Method	m	= clazz.getMethod(extensions[i+1], new Class[0]);
				mappings.add((Set<?>)m.invoke(null, new Object[0]));
			}
			catch(Exception e)
			{
//				e.printStackTrace();
			}
			
		}
		
		
		this.loader = new ApplicationModelLoader(mappings.toArray(new Set[0]));
	}
	
	/**
	 *  Create a new application factory.
	 *  @param platform	The platform.
	 *  @param mappings	The XML reader mappings of supported spaces (if any).
	 */
	public ApplicationComponentFactory(IServiceProvider provider)
	{
		super(provider.getId(), IComponentFactory.class, null);
		this.provider = provider;
	}
	
	/**
	 *  Start the service.
	 */
	public IFuture<Void> startService()
	{
		final Future<Void> ret = new Future<Void>();
		super.startService().addResultListener(new DelegationResultListener<Void>(ret)
		{
			public void customResultAvailable(Void result)
			{
				SServiceProvider.getService(provider, ILibraryService.class, RequiredServiceInfo.SCOPE_PLATFORM)
					.addResultListener(new DelegationResultListener(ret)
				{
					public void customResultAvailable(Object result)
					{
						libservice = (ILibraryService)result;
						
						SServiceProvider.getServices(provider, IComponentFactoryExtensionService.class, RequiredServiceInfo.SCOPE_PLATFORM)
							.addResultListener(new DelegationResultListener(ret)
						{
							public void customResultAvailable(Object result)
							{
								Collection fes = (Collection)result;
								
								CollectionResultListener lis = new CollectionResultListener(fes.size(), true, new DefaultResultListener()
								{
									public void resultAvailable(Object result)
									{
										Collection exts = (Collection)result;
										Set[] mappings = (Set[])exts.toArray(new Set[exts.size()]);
										
										loader = new ApplicationModelLoader(mappings);
										
										libservicelistener = new ILibraryServiceListener()
										{
											public IFuture<Void> resourceIdentifierRemoved(IResourceIdentifier rid)
											{
												loader.clearModelCache();
												return IFuture.DONE;
											}
											
											public IFuture resourceIdentifierAdded(IResourceIdentifier rid)
											{
												loader.clearModelCache();
												return IFuture.DONE;
											}
										};
										
										libservice.addLibraryServiceListener(libservicelistener);
										
										ret.setResult(null);
									}
								});
								
								for(Iterator it=fes.iterator(); it.hasNext(); )
								{
									IComponentFactoryExtensionService fex = (IComponentFactoryExtensionService)it.next();
									fex.getExtension(FILETYPE_APPLICATION).addResultListener(lis);
								}
							}	
						});
					}
				});
			}
		});
		
		return ret;
	}
	
	/**
	 *  Shutdown the service.
	 *  @param listener The listener.
	 */
	public IFuture<Void> shutdownService()
	{
		libservice.removeLibraryServiceListener(libservicelistener);
//		SServiceProvider.getService(provider, ILibraryService.class, RequiredServiceInfo.SCOPE_PLATFORM).addResultListener(new DefaultResultListener()
//		{
//			public void resultAvailable(Object result)
//			{
//				ILibraryService libService = (ILibraryService) result;
//				libService.removeLibraryServiceListener(libservicelistener);
//			}
//		});
		return super.shutdownService();
	}
	
	//-------- IComponentFactory interface --------
	
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
						ret.setResult(loader.loadApplicationModel(model, imports, cl, 
								new Object[]{rid, getProviderId().getRoot()}).getModelInfo());
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
				ret.setResult(loader.loadApplicationModel(model, imports, cl, 
						new Object[]{rid, getProviderId().getRoot()}).getModelInfo());
			}
			catch(Exception e)
			{
				ret.setException(e);
			}			
		}

		return ret;
	}
	
	/**
	 * Create a component instance.
	 * @param adapter The component adapter.
	 * @param model The component model.
	 * @param config The name of the configuration (or null for default configuration) 
	 * @param arguments The arguments for the component as name/value pairs.
	 * @param parent The parent component (if any).
	 * @return An instance of a component.
	 */
	public IFuture<Tuple2<IComponentInstance, IComponentAdapter>> createComponentInstance(final IComponentDescription desc, final IComponentAdapterFactory factory, 
		final IModelInfo modelinfo, final String config, final Map<String, Object> arguments, final IExternalAccess parent, 
		final RequiredServiceBinding[] bindings, final boolean copy, 
		final IIntermediateResultListener<Tuple2<String, Object>> resultlistener, final Future<Void> init)
	{
		final Future<Tuple2<IComponentInstance, IComponentAdapter>>	ret	= new Future<Tuple2<IComponentInstance, IComponentAdapter>>();
		
		if(libservice!=null)
		{
			libservice.getClassLoader(modelinfo.getResourceIdentifier()).addResultListener(
				new ExceptionDelegationResultListener<ClassLoader, Tuple2<IComponentInstance, IComponentAdapter>>(ret)
			{
				public void customResultAvailable(ClassLoader cl)
				{
					try
					{
						CacheableKernelModel apptype = loader.loadApplicationModel(modelinfo.getFilename(), null, cl, 
							new Object[]{modelinfo.getResourceIdentifier(), getProviderId().getRoot()});
						ComponentInterpreter interpreter = new ComponentInterpreter(desc, apptype.getModelInfo(), config, factory, parent, arguments, bindings, copy, 
							resultlistener, init, cl);
						ret.setResult(new Tuple2<IComponentInstance, IComponentAdapter>(interpreter, interpreter.getComponentAdapter()));
					}
					catch(Exception e)
					{
						ret.setException(e);
					}
				}
			});
		}
		
		// for platform bootstrapping
		else
		{
			try
			{
				ClassLoader cl = getClass().getClassLoader();
				CacheableKernelModel apptype = loader.loadApplicationModel(modelinfo.getFilename(), null, cl, 
					new Object[]{modelinfo.getResourceIdentifier(), getProviderId().getRoot()});
				ComponentInterpreter interpreter = new ComponentInterpreter(desc, apptype.getModelInfo(), config, factory, parent, arguments, bindings, copy, 
					resultlistener, init, cl);
				ret.setResult(new Tuple2<IComponentInstance, IComponentAdapter>(interpreter, interpreter.getComponentAdapter()));
			}
			catch(Exception e)
			{
				ret.setException(e);
			}
		}
		
//		try
//		{
//			// libservice is null for platform bootstrap factory.
//			ClassLoader cl = libservice==null? getClass().getClassLoader(): libservice.getClassLoader(modelinfo.getResourceIdentifier());
//			CacheableKernelModel apptype = loader.loadApplicationModel(modelinfo.getFilename(), null, 
//				libservice==null? getClass().getClassLoader(): libservice.getClassLoader(modelinfo.getResourceIdentifier()), modelinfo.getResourceIdentifier());
//			ComponentInterpreter interpreter = new ComponentInterpreter(desc, apptype.getModelInfo(), config, factory, parent, arguments, bindings, copy, ret, cl);
//			
//			// todo: result listener?
//			// todo: create application context as return value?!
//					
//			return new Future<Tuple2<IComponentInstance, IComponentAdapter>>(new Tuple2<IComponentInstance, IComponentAdapter>(interpreter, interpreter.getComponentAdapter()));
//		}
//		catch(Exception e)
//		{
//			throw new RuntimeException(e);
//		}

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
		return new Future<Boolean>(model.endsWith(ApplicationModelLoader.FILE_EXTENSION_APPLICATION));
	}
	
	/**
	 *  Test if a model is startable (e.g. an component).
	 *  @param model The model (e.g. file name).
	 *  @param The imports (if any).
	 *  @return True, if startable (and loadable).
	 */
	public IFuture<Boolean> isStartable(String model, String[] imports, IResourceIdentifier rid)
	{
		return new Future<Boolean>(model.endsWith(ApplicationModelLoader.FILE_EXTENSION_APPLICATION));
	}

	/**
	 *  Get the names of ADF file types supported by this factory.
	 */
	public String[] getComponentTypes()
	{
		return new String[]{FILETYPE_APPLICATION};
	}

	/**
	 *  Get a default icon for a file type.
	 */
	/* $if !android $ */
	public IFuture<Icon> getComponentTypeIcon(String type)
	{
		return new Future<Icon>(type.equals(FILETYPE_APPLICATION)? icons.getIcon("application"): null);
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
		return new Future<String>(model.toLowerCase().endsWith(ApplicationModelLoader.FILE_EXTENSION_APPLICATION)? FILETYPE_APPLICATION: null);
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
		return FILETYPE_APPLICATION.equals(type)
			? Collections.EMPTY_MAP : null;
	}

	//-------- helper methods --------
	
	/**
	 *  Load an xml Jadex model.
	 *  Creates file name when specified with or without package.
	 *  @param xml The filename | fully qualified classname
	 *  @return The loaded model.
	 * /
	// Todo: fix directory stuff!???
	// Todo: Abstract model loader unifying app/bdi loading
	public static ResourceInfo getResourceInfo(String xml, String suffix, String[] imports, ClassLoader classloader) throws IOException
	{
		if(xml==null)
			throw new IllegalArgumentException("Required ADF name nulls.");
		if(suffix==null && !xml.endsWith(ApplicationModelLoader.FILE_EXTENSION_APPLICATION))
			throw new IllegalArgumentException("Required suffix nulls.");

		if(suffix==null)
			suffix="";
		
		// Try to find directly as absolute path.
		String resstr = xml;
		ResourceInfo ret = SUtil.getResourceInfo0(resstr, classloader);

		if(ret==null || ret.getInputStream()==null)
		{
			// Fully qualified package name? Can also be full package name with empty package ;-)
			//if(xml.indexOf(".")!=-1)
			//{
				resstr	= SUtil.replace(xml, ".", "/") + suffix;
				//System.out.println("Trying: "+resstr);
				ret	= SUtil.getResourceInfo0(resstr, classloader);
			//}

			// Try to find in imports.
			for(int i=0; (ret==null || ret.getInputStream()==null) && imports!=null && i<imports.length; i++)
			{
				// Package import
				if(imports[i].endsWith(".*"))
				{
					resstr = SUtil.replace(imports[i].substring(0,
						imports[i].length()-1), ".", "/") + xml + suffix;
					//System.out.println("Trying: "+resstr);
					ret	= SUtil.getResourceInfo0(resstr, classloader);
				}
				// Direct import
				else if(imports[i].endsWith(xml))
				{
					resstr = SUtil.replace(imports[i], ".", "/") + suffix;
					//System.out.println("Trying: "+resstr);
					ret	= SUtil.getResourceInfo0(resstr, classloader);
				}
			}
		}

		if(ret==null || ret.getInputStream()==null)
			throw new IOException("File "+xml+" not found in imports");//: "+SUtil.arrayToString(imports));

		return ret;
	}	*/	
}
