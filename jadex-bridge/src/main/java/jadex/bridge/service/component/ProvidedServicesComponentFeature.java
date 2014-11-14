package jadex.bridge.service.component;

import jadex.bridge.ClassInfo;
import jadex.bridge.IInternalAccess;
import jadex.bridge.component.ComponentCreationInfo;
import jadex.bridge.component.impl.AbstractComponentFeature;
import jadex.bridge.modelinfo.ConfigurationInfo;
import jadex.bridge.modelinfo.UnparsedExpression;
import jadex.bridge.sensor.service.IMethodInvocationListener;
import jadex.bridge.service.BasicService;
import jadex.bridge.service.IInternalService;
import jadex.bridge.service.IService;
import jadex.bridge.service.IServiceIdentifier;
import jadex.bridge.service.ProvidedServiceImplementation;
import jadex.bridge.service.ProvidedServiceInfo;
import jadex.bridge.service.PublishInfo;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.bridge.service.annotation.Service;
import jadex.bridge.service.search.SServiceProvider;
import jadex.bridge.service.types.factory.IPlatformComponentAccess;
import jadex.bridge.service.types.library.ILibraryService;
import jadex.bridge.service.types.monitoring.IMonitoringService.PublishEventLevel;
import jadex.commons.MethodInfo;
import jadex.commons.SReflect;
import jadex.commons.future.DelegationResultListener;
import jadex.commons.future.ExceptionDelegationResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.commons.future.IResultListener;
import jadex.javaparser.SJavaParser;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 *  Feature for provided services.
 */
public class ProvidedServicesComponentFeature	extends AbstractComponentFeature	implements IProvidedServicesFeature
{
	//-------- attributes --------
	
	/** The map of platform services. */
	protected Map<Class<?>, Collection<IInternalService>> services;
	
	/** The map of provided service infos. (sid -> method listener) */
	protected Map<IServiceIdentifier, MethodListenerHandler> servicelisteners;
	
	//-------- constructors --------
	
	/**
	 *  Factory method constructor for instance level.
	 */
	public ProvidedServicesComponentFeature(IInternalAccess component, ComponentCreationInfo cinfo)
	{
		super(component, cinfo);
	}
	
	//-------- IComponentFeature interface / instance level --------
	
	/**
	 *  Initialize the feature.
	 */
	public IFuture<Void>	init()
	{
		final Future<Void> ret = new Future<Void>();
		
		try
		{
			// Collect provided services from model (name or type -> provided service info)
			ProvidedServiceInfo[] ps = component.getModel().getProvidedServices();
			Map<Object, ProvidedServiceInfo> sermap = new LinkedHashMap<Object, ProvidedServiceInfo>();
			for(int i=0; i<ps.length; i++)
			{
				Object key = ps[i].getName()!=null? ps[i].getName(): ps[i].getType().getType(component.getClassLoader(), component.getModel().getAllImports());
				if(sermap.put(key, ps[i])!=null)
				{
					throw new RuntimeException("Services with same type must have different name.");  // Is catched and set to ret below
				}
			}
			
			// Adapt services to configuration (if any).
			if(component.getConfiguration()!=null)
			{
				ConfigurationInfo cinfo = component.getModel().getConfiguration(component.getConfiguration());
				ProvidedServiceInfo[] cs = cinfo.getProvidedServices();
				for(int i=0; i<cs.length; i++)
				{
					Object key = cs[i].getName()!=null? cs[i].getName(): cs[i].getType().getType(component.getClassLoader(), component.getModel().getAllImports());
					ProvidedServiceInfo psi = (ProvidedServiceInfo)sermap.get(key);
					ProvidedServiceInfo newpsi= new ProvidedServiceInfo(psi.getName(), psi.getType().getType(component.getClassLoader(), component.getModel().getAllImports()), 
						new ProvidedServiceImplementation(cs[i].getImplementation()), 
						cs[i].getScope()!=null? cs[i].getScope(): psi.getScope(),
						cs[i].getPublish()!=null? cs[i].getPublish(): psi.getPublish(), 
						cs[i].getProperties()!=null? cs[i].getProperties() : psi.getProperties());
					sermap.put(key, newpsi);
				}
			}
			
			// Add custom service infos from outside.
			ProvidedServiceInfo[] pinfos = cinfo.getProvidedServiceInfos();
			for(int i=0; pinfos!=null && i<pinfos.length; i++)
			{
				Object key = pinfos[i].getName()!=null? pinfos[i].getName(): pinfos[i].getType().getType(component.getClassLoader(), component.getModel().getAllImports());
				ProvidedServiceInfo psi = (ProvidedServiceInfo)sermap.get(key);
				ProvidedServiceInfo newpsi= new ProvidedServiceInfo(psi.getName(), psi.getType().getType(component.getClassLoader(), component.getModel().getAllImports()), 
					pinfos[i].getImplementation()!=null? new ProvidedServiceImplementation(pinfos[i].getImplementation()): psi.getImplementation(), 
					pinfos[i].getScope()!=null? pinfos[i].getScope(): psi.getScope(),
					pinfos[i].getPublish()!=null? pinfos[i].getPublish(): psi.getPublish(), 
					pinfos[i].getProperties()!=null? pinfos[i].getProperties() : psi.getProperties());
				sermap.put(key, newpsi);
			}
			
			// Instantiate service objects
			for(ProvidedServiceInfo info: sermap.values())
			{
				final ProvidedServiceImplementation	impl = info.getImplementation();
				// Virtual service (e.g. promoted)
				if(impl!=null && impl.getBinding()!=null)
				{
					RequiredServiceInfo rsi = new RequiredServiceInfo(BasicService.generateServiceName(info.getType().getType( 
						component.getClassLoader(), component.getModel().getAllImports()))+":virtual", info.getType().getType(component.getClassLoader(), component.getModel().getAllImports()));
					IServiceIdentifier sid = BasicService.createServiceIdentifier(component.getComponentIdentifier(), 
						rsi.getName(), rsi.getType().getType(component.getClassLoader(), component.getModel().getAllImports()),
						BasicServiceInvocationHandler.class, component.getModel().getResourceIdentifier(), info.getScope());
					final IInternalService service = BasicServiceInvocationHandler.createDelegationProvidedServiceProxy(
						component, sid, rsi, impl.getBinding(), component.getClassLoader(), cinfo.isRealtime());
					
					addService(service, info);
				}
				else
				{
					Object ser = createServiceImplementation(info);
					
					// Implementation may null to disable service in some configurations.
					if(ser!=null)
					{
						UnparsedExpression[] ins = info.getImplementation().getInterceptors();
						IServiceInvocationInterceptor[] ics = null;
						if(ins!=null)
						{
							ics = new IServiceInvocationInterceptor[ins.length];
							for(int i=0; i<ins.length; i++)
							{
								if(ins[i].getValue()!=null && ins[i].getValue().length()>0)
								{
									ics[i] = (IServiceInvocationInterceptor)SJavaParser.evaluateExpression(ins[i].getValue(), component.getModel().getAllImports(), component.getFetcher(), component.getClassLoader());
								}
								else
								{
									ics[i] = (IServiceInvocationInterceptor)ins[i].getClazz().getType(component.getClassLoader(), component.getModel().getAllImports()).newInstance();
								}
							}
						}
						
						final Class<?> type = info.getType().getType(component.getClassLoader(), component.getModel().getAllImports());
						PublishEventLevel elm = component.getComponentDescription().getMonitoring()!=null? component.getComponentDescription().getMonitoring(): null;
//						 todo: remove this? currently the level cannot be turned on due to missing interceptor
						boolean moni = elm!=null? !PublishEventLevel.OFF.equals(elm.getLevel()): false; 
						final IInternalService proxy = BasicServiceInvocationHandler.createProvidedServiceProxy(
							component, ser, info.getName(), type, info.getImplementation().getProxytype(), ics, cinfo.isCopy(), 
							cinfo.isRealtime(), moni, info, info.getScope());
						
						addService(proxy, info);
					}
				}
			}
			
			// Start the services.
			Collection<IInternalService>	allservices	= getAllServices();
			if(!allservices.isEmpty())
			{
				initServices(allservices.iterator()).addResultListener(new DelegationResultListener<Void>(ret));
			}
			else
			{
				ret.setResult(null);
			}
		}
		catch(Exception e)
		{
			ret.setException(e);
		}
		
		return ret;
	}
	
	public IFuture<Void> shutdown()
	{
		Future<Void>	ret	= new Future<Void>();
		
		// Shutdown the services.
		Collection<IInternalService>	allservices	= getAllServices();
		if(!allservices.isEmpty())
		{
			shutdownServices(allservices.iterator()).addResultListener(new DelegationResultListener<Void>(ret));
		}
		else
		{
			ret.setResult(null);
		}
		
		return ret;
	}
	
	/**
	 *  Add a service.
	 *  @param service	The service object.
	 *  @param info	 The service info.
	 */
	public void	addService(IInternalService service, ProvidedServiceInfo info)
	{
		// Find service types
		Class<?>	type	= info.getType().getType(component.getClassLoader(), component.getModel().getAllImports());
		Set<Class<?>> types = new LinkedHashSet<Class<?>>();
		types.add(type);
		for(Class<?> sin: SReflect.getSuperInterfaces(new Class[]{type}))
		{
			if(sin.isAnnotationPresent(Service.class))
			{
				types.add(sin);
			}
		}

		if(services==null)
		{
			services = Collections.synchronizedMap(new LinkedHashMap<Class<?>, Collection<IInternalService>>());
		}

		for(Class<?> servicetype: types)
		{
			Collection<IInternalService> tmp = services.get(servicetype);
			if(tmp==null)
			{
				tmp = Collections.synchronizedList(new ArrayList<IInternalService>());
				services.put(servicetype, tmp);
			}
			tmp.add(service);
			
			// Make all services available immediately, even before start (hack???).
			((IPlatformComponentAccess)component).getServiceRegistry().addService(new ClassInfo(servicetype), service);
		}
	}
	
	/**
	 *  Create a service implementation from description.
	 */
	protected Object createServiceImplementation(ProvidedServiceInfo info)	throws Exception
	{
		Object	ser	= null;
		ProvidedServiceImplementation impl = info.getImplementation();
		if(impl!=null && impl.getValue()!=null)
		{
			// todo: other Class imports, how can be found out?
			try
			{
//				SimpleValueFetcher fetcher = new SimpleValueFetcher(component.getFetcher());
//				fetcher.setValue("$servicename", info.getName());
//				fetcher.setValue("$servicetype", info.getType().getType(component.getClassLoader(), component.getModel().getAllImports()));
//				System.out.println("sertype: "+fetcher.fetchValue("$servicetype")+" "+info.getName());
				ser = SJavaParser.getParsedValue(impl, component.getModel().getAllImports(), component.getFetcher(), component.getClassLoader());
//				System.out.println("added: "+ser+" "+model.getName());
			}
			catch(RuntimeException e)
			{
//				e.printStackTrace();
				throw new RuntimeException("Service creation error: "+info, e);
			}
		}
		else if(impl!=null && impl.getClazz().getType(component.getClassLoader(), component.getModel().getAllImports())!=null)
		{
			ser = impl.getClazz().getType(component.getClassLoader(), component.getModel().getAllImports()).newInstance();
		}
		
		return ser;
	}
	
	/**
	 *  Get all services in a single collection.
	 */
	protected Collection<IInternalService>	getAllServices()
	{
		Collection<IInternalService> allservices;
		if(services!=null && services.size()>0)
		{
			allservices = new LinkedHashSet<IInternalService>();
			for(Iterator<Collection<IInternalService>> it=services.values().iterator(); it.hasNext(); )
			{
				// Service may occur at different positions if added with more than one interface
				Collection<IInternalService> col = it.next();
				for(IInternalService ser: col)
				{
					if(!allservices.contains(ser))
					{
						allservices.add(ser);
					}
				}
			}
		}
		else
		{
			allservices	= Collections.emptySet();
		}
		
		return allservices;
	}
	
	/**
	 *  Init the services one by one.
	 */
	protected IFuture<Void> initServices(final Iterator<IInternalService> services)
	{
		final Future<Void> ret = new Future<Void>();
		if(services.hasNext())
		{
			final IInternalService	is	= services.next();
			component.getLogger().info("Starting service: "+is.getServiceIdentifier());
			is.setComponentAccess(component).addResultListener(new DelegationResultListener<Void>(ret)
			{
				public void customResultAvailable(Void result)
				{
					is.startService().addResultListener(new IResultListener<Void>()
					{
						public void resultAvailable(Void result)
						{
							component.getLogger().info("Started service: "+is.getServiceIdentifier());
							
							initServices(services).addResultListener(new DelegationResultListener<Void>(ret));
						}
						
						public void exceptionOccurred(Exception exception)
						{
							ret.setException(exception);
						}
					});
				}
			});
		}
		else
		{
			ret.setResult(null);
		}
		return ret;
	}
	
	/**
	 *  Shutdown the services one by one.
	 */
	protected IFuture<Void> shutdownServices(final Iterator<IInternalService> services)
	{
		final Future<Void> ret = new Future<Void>();
		if(services.hasNext())
		{
			final IInternalService	is	= services.next();
			component.getLogger().info("Stopping service: "+is.getServiceIdentifier());
			is.shutdownService().addResultListener(new IResultListener<Void>()
			{
				public void resultAvailable(Void result)
				{
					component.getLogger().info("Stopped service: "+is.getServiceIdentifier());
					
					shutdownServices(services).addResultListener(new DelegationResultListener<Void>(ret));
				}
				
				public void exceptionOccurred(Exception exception)
				{
					ret.setException(exception);
				}
			});
		}
		else
		{
			ret.setResult(null);
		}
		return ret;
	}
	
	//-------- IProvidedServicesFeature interface --------

	/**
	 *  Get provided (declared) service.
	 *  @return The service.
	 */
	public IService getProvidedService(String name)
	{
		IService ret = null;
		if(services!=null)
		{
			for(Iterator<Class<?>> it=services.keySet().iterator(); it.hasNext() && ret==null; )
			{
				Collection<IInternalService> sers = services.get(it.next());
				for(Iterator<IInternalService> it2=sers.iterator(); it2.hasNext() && ret==null; )
				{
					IService ser = it2.next();
					if(ser.getServiceIdentifier().getServiceName().equals(name))
					{
						ret = ser;
					}
				}
			}
		}
		
		return ret;
	}
	
	/**
	 *  Get the raw implementation of the provided service.
	 *  @param clazz The class.
	 *  @return The raw object.
	 */
	public <T> T getProvidedServiceRawImpl(Class<T> clazz)
	{
		T ret = null;
		
		T service = getProvidedService(clazz);
		if(service!=null)
		{
			BasicServiceInvocationHandler handler = (BasicServiceInvocationHandler)Proxy.getInvocationHandler(service);
			ret = clazz.cast(handler.getDomainService());
		}
		
		return ret;
	}

	/**
	 *  Get the provided service implementation object by name.
	 *  
	 *  @param name The service name.
	 *  @return The service.
	 */
	public Object getProvidedServiceRawImpl(String name)
	{
		Object ret = null;
		
		Object service = getProvidedService(name);
		if(service!=null)
		{
			BasicServiceInvocationHandler handler = (BasicServiceInvocationHandler)Proxy.getInvocationHandler(service);
			ret = handler.getDomainService();
		}
		
		return ret;	
	}
	
	/**
	 *  Get the provided service implementation object by name.
	 *  
	 *  @param name The service identifier.
	 *  @return The service.
	 */
	public Object getProvidedServiceRawImpl(IServiceIdentifier sid)
	{
		Object ret = null;
		
		Object[] services = getProvidedServices(sid.getServiceType().getType(getComponent().getClassLoader()));
		if(services!=null)
		{
			IService service = null;
			for(Object ser: services)
			{
				if(((IService)ser).getServiceIdentifier().equals(sid))
				{
					service = (IService)ser;
				}
			}
			if(service!=null)
			{
				if(Proxy.isProxyClass(service.getClass()))
				{
					BasicServiceInvocationHandler handler = (BasicServiceInvocationHandler)Proxy.getInvocationHandler(service);
					ret = handler.getDomainService();
				}
				else
				{
					ret = service;
				}
			}
		}
		
		return ret;	
	}
	
	/**
	 *  Get provided (declared) service.
	 *  @param clazz The interface.
	 *  @return The service.
	 */
	public <T> T[] getProvidedServices(Class<T> clazz)
	{
		Collection<IInternalService> coll	= null;
		if(services!=null)
		{
			if(clazz!=null)
			{
				coll = services.get(clazz);
			}
			else
			{
				coll = new HashSet<IInternalService>();
				for(Class<?> cl: services.keySet())
				{
					Collection<IInternalService> sers = services.get(cl);
					coll.addAll(sers);
				}
			}			
		}
		
//		T[] ret	= (T[])Array.newInstance(clazz, coll!=null ? coll.size(): 0);
		return coll==null ? (T[])new Object[0] : coll.toArray((T[])new Object[coll.size()]);
	}
	
	/**
	 *  Get provided (declared) service.
	 *  @param clazz The interface.
	 *  @return The service.
	 */
	public <T> T getProvidedService(Class<T> clazz)
	{
		T[] ret = getProvidedServices(clazz);
		return ret.length>0? ret[0]: null;
	}

	/**
	 *  Get the services.
	 *  @return The services.
	 */
	public Map<Class<?>, Collection<IInternalService>> getServices() 
	{
		return services;
	}

	/**
	 *  Add a service to the platform. 
	 *  If under the same name and type a service was contained,
	 *  the old one is removed and shutdowned.
	 *  @param type The public service interface.
	 *  @param service The service.
	 */
	public void addService(String name, Class<?> type, Object service)
	{
		addService(name, type, BasicServiceInvocationHandler.PROXYTYPE_DECOUPLED, null, service, null);
	}
	
	/**
	 *  Add a service to the platform.
	 *  If under the same name and type a service was contained,
	 *  the old one is removed and shutdowned.
	 *  @param type The public service interface.
	 *  @param service The service.
	 *  @param type The proxy type (@see{BasicServiceInvocationHandler}).
	 */
	public void	addService(String name, Class<?> type, Object service, String proxytype)
	{
		addService(name, type, proxytype, null, service, null);
	}
	
	// todo:
//	/**
//	 *  Add a service to the platform. 
//	 *  If under the same name and type a service was contained,
//	 *  the old one is removed and shutdowned.
//	 *  @param type The public service interface.
//	 *  @param service The service.
//	 */
//	public void addService(String name, Class<?> type, Object service, PublishInfo pi)
//	{
//		addService(name, type, BasicServiceInvocationHandler.PROXYTYPE_DECOUPLED, null, service, pi);
//	}
	
	/**
	 *  Add a service to the platform. 
	 *  If under the same name and type a service was contained,
	 *  the old one is removed and shutdowned.
	 *  @param type The public service interface.
	 *  @param service The service.
	 */
	public void	addService(String name, Class<?> type, Object service, PublishInfo pi)
	{
		ProvidedServiceInfo psi = pi!=null? new ProvidedServiceInfo(null, type, null, null, pi, null): null;
		addService(name, type, BasicServiceInvocationHandler.PROXYTYPE_DECOUPLED, null, service, psi);
	}

	/**
	 *  Removes a service from the platform (shutdowns also the service).
	 *  @param service The service.
	 */
	public IFuture<Void> removeService(final IServiceIdentifier sid)
	{
		final Future<Void> ret = new Future<Void>();
		
		if(sid==null)
		{
			ret.setException(new IllegalArgumentException("Service identifier nulls."));
			return ret;
		}
			
		getServiceTypes(sid).addResultListener(new ExceptionDelegationResultListener<Collection<Class<?>>, Void>(ret)
		{
			public void customResultAvailable(final Collection<Class<?>> servicetypes)
			{
//				System.out.println("Removing service: " + servicetype);
				synchronized(this)
				{
					IInternalService service = null;
					
					for(Class<?> servicetype: servicetypes)
					{
						Collection<IInternalService> tmp = services!=null? services.get(servicetype): null;
						
						service = null;
						
						if(tmp!=null)
						{
							for(Iterator<IInternalService> it=tmp.iterator(); it.hasNext() && service==null; )
							{
								final IInternalService tst = it.next();
								if(tst.getServiceIdentifier().equals(sid))
								{
									service = tst;
									tmp.remove(service);
								}
							}
							
							// Remove collection if last service
							if(tmp.isEmpty())
							{
								services.remove(servicetype);
							}
						}
						
						if(service==null)
						{
							ret.setException(new IllegalArgumentException("Service not found: "+sid));
							break;
						}
					}
					
					if(service!=null)
					{
						final IInternalService fservice = service;
						// Todo: fix started/terminated!? (i.e. addService() is ignored, when not started!?)
	//					if(!terminated)
	//					{
//							if(sid.toString().indexOf("Context")!=-1)
//									System.out.println("Terminating service: "+sid);
							getComponent().getLogger().info("Terminating service: "+sid);
							
							// Dispose nonfunc properties
							
							// todo: how to shutdown?
							
							ret.setResult(null);
							
//							service.shutdownNFPropertyProvider().addResultListener(new DelegationResultListener<Void>(ret)
//							{
//								public void customResultAvailable(Void result)
//								{
////									if(fservice.getServiceIdentifier().toString().indexOf("ContextSer")!=-1)
////										System.out.println("hierda");
//									
//									fservice.shutdownService().addResultListener(new DelegationResultListener<Void>(ret)
//									{
//										public void customResultAvailable(Void result)
//										{
////											if(id.getParent()==null)// && sid.toString().indexOf("Async")!=-1)
////												System.out.println("Terminated service: "+sid);
//											getLogger().info("Terminated service: "+sid);
//											
//											for(Class<?> key: servicetypes)
//											{
//												getServiceRegistry().removeService(new ClassInfo(key), fservice);
//											}
//											
//											serviceShutdowned(fservice).addResultListener(new DelegationResultListener<Void>(ret));
//										}
//										
//										public void exceptionOccurred(Exception exception)
//										{
//											exception.printStackTrace();
//											super.exceptionOccurred(exception);
//										}
//									});
//								}
//								
//								public void exceptionOccurred(Exception exception)
//								{
//									exception.printStackTrace();
//									super.exceptionOccurred(exception);
//								}
//							});							
	//					}
	//					else
	//					{
	//						ret.setResult(null);
	//					}
					}
				}
			}
		});
		
		return ret;
	}

	/**
	 * 
	 */
	public IFuture<Collection<Class<?>>> getServiceTypes(final IServiceIdentifier sid)
	{
		final Future<Collection<Class<?>>> ret = new Future<Collection<Class<?>>>();
		getServiceType(sid).addResultListener(new ExceptionDelegationResultListener<Class<?>, Collection<Class<?>>>(ret)
		{
			public void customResultAvailable(Class<?> result)
			{
				// todo: cache results
				Set<Class<?>> res = new LinkedHashSet<Class<?>>();
				res.add(result);
				
				Class<?>[] sins = SReflect.getSuperInterfaces(new Class[]{result});
				for(Class<?> sin: sins)
				{
					if(sin.isAnnotationPresent(Service.class))
					{
						res.add(sin);
					}
				}
				
				ret.setResult(res);
			}
		});
		
		return ret;
	}
	
	/**
	 * 
	 */
	public IFuture<Class<?>> getServiceType(final IServiceIdentifier sid)
	{
		final Future<Class<?>> ret = new Future<Class<?>>();
		if(sid.getServiceType().getType(getComponent().getClassLoader(), getComponent().getModel().getAllImports())!=null)
		{
			ret.setResult(sid.getServiceType().getType(getComponent().getClassLoader(), getComponent().getModel().getAllImports())); // todo: only local? remote would cause nullpointer
		}
		else
		{
			ILibraryService ls = SServiceProvider.getLocalService(getComponent(), ILibraryService.class, RequiredServiceInfo.SCOPE_PLATFORM);
			ls.getClassLoader(sid.getResourceIdentifier())
				.addResultListener(new ExceptionDelegationResultListener<ClassLoader, Class<?>>(ret)
			{
				public void customResultAvailable(ClassLoader cl)
				{
					ret.setResult(sid.getServiceType().getType(cl));
				}
			});
		}
		return ret;
	}
	
	
	/**
	 *  Add a service to the component. 
	 *  @param type The service interface.
	 *  @param service The service.
	 *  @param proxytype	The proxy type (@see{BasicServiceInvocationHandler}).
	 */
	public IInternalService addService(final String name, final Class<?> type, final String proxytype, 
		final IServiceInvocationInterceptor[] ics, final Object service, final ProvidedServiceInfo info)
	{
//		System.out.println("addS:"+service);

		PublishEventLevel elm = getComponent().getComponentDescription().getMonitoring()!=null? getComponent().getComponentDescription().getMonitoring(): null;
		// todo: remove this? currently the level cannot be turned on due to missing interceptor
//		boolean moni = elm!=null? !PublishEventLevel.OFF.equals(elm.getLevel()): false; 
		
		boolean moni = elm!=null && !PublishEventLevel.OFF.equals(elm); 
		final IInternalService proxy = BasicServiceInvocationHandler.createProvidedServiceProxy(
			getComponent(), service, name, type, proxytype, ics, getComponent().isCopy(), 
			getComponent().isRealtime(), moni, 
			info, info!=null? info.getScope(): null);
		
		addService(proxy, info);
		
		return proxy;
	}
	
	/**
	 *  Add a method invocation handler.
	 */
	public void addMethodInvocationListener(IServiceIdentifier sid, MethodInfo mi, IMethodInvocationListener listener)
	{
//		System.out.println("added lis: "+sid+" "+mi+" "+hashCode());
		
		if(servicelisteners==null)
			servicelisteners = new HashMap<IServiceIdentifier, MethodListenerHandler>();
		MethodListenerHandler handler = servicelisteners.get(sid);
		if(handler==null)
		{
			handler = new MethodListenerHandler();
			servicelisteners.put(sid, handler);
		}
		handler.addMethodListener(mi, listener);
	}
	
	/**
	 *  Remove a method invocation handler.
	 */
	public void removeMethodInvocationListener(IServiceIdentifier sid, MethodInfo mi, IMethodInvocationListener listener)
	{
		if(servicelisteners!=null)
		{
			MethodListenerHandler handler = servicelisteners.get(sid);
			if(handler!=null)
			{
				handler.removeMethodListener(mi, listener);
			}
		}
	}
	
	/**
	 *  Notify listeners that a service method has been called.
	 */
	public void notifyMethodListeners(IServiceIdentifier sid, boolean start, Object proxy, final Method method, final Object[] args, Object callid, ServiceInvocationContext context)
	{
		if(servicelisteners!=null)
		{
			MethodListenerHandler handler = servicelisteners.get(sid);
			if(handler!=null)
			{
//				MethodInfo mi = new MethodInfo(method);
				handler.notifyMethodListeners(start, proxy, method, args, callid, context);
			}
		}
	}
	
	/**
	 *  Test if service and method has listeners.
	 */
	public boolean hasMethodListeners(IServiceIdentifier sid, MethodInfo mi)
	{
		boolean ret = false;
		if(servicelisteners!=null)
		{
			MethodListenerHandler handler = servicelisteners.get(sid);
			if(handler!=null)
			{
				ret = handler.hasMethodListeners(sid, mi);
			}
		}
		
		return ret;
	}
	
	/**
	 *  Add a service interceptor.
	 *  @param interceptor The interceptor.
	 *  @param service The service.
	 *  @param pos The position (0=first, -1=last-1, i.e. one before method invocation).
	 */
	public void addInterceptor(IServiceInvocationInterceptor interceptor, Object service, int pos)
	{
		BasicServiceInvocationHandler handler = (BasicServiceInvocationHandler)Proxy.getInvocationHandler(service);
		handler.addServiceInterceptor(interceptor, pos);
	}
	
	/**
	 *  Remove a service interceptor.
	 *  @param interceptor The interceptor.
	 *  @param service The service.
	 */
	public void removeInterceptor(IServiceInvocationInterceptor interceptor, Object service)
	{
		BasicServiceInvocationHandler handler = (BasicServiceInvocationHandler)Proxy.getInvocationHandler(service);
		handler.removeServiceInterceptor(interceptor);
	}
	
	/**
	 *  Get the interceptors of a service.
	 *  @param service The service.
	 *  @return The interceptors.
	 */
	public IServiceInvocationInterceptor[] getInterceptors(Object service)
	{
		BasicServiceInvocationHandler handler = (BasicServiceInvocationHandler)Proxy.getInvocationHandler(service);
		return handler.getInterceptors();
	}
}
