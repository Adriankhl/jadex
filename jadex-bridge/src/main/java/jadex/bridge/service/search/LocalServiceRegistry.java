package jadex.bridge.service.search;

import jadex.bridge.ClassInfo;
import jadex.bridge.IComponentIdentifier;
import jadex.bridge.service.IService;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.bridge.service.types.remote.IProxyAgentService;
import jadex.bridge.service.types.remote.IRemoteServiceManagementService;
import jadex.commons.IFilter;
import jadex.commons.IRemoteFilter;
import jadex.commons.future.CounterResultListener;
import jadex.commons.future.DelegationResultListener;
import jadex.commons.future.ExceptionDelegationResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.commons.future.IResultListener;
import jadex.commons.future.ISubscriptionIntermediateFuture;
import jadex.commons.future.ITerminableIntermediateFuture;
import jadex.commons.future.IntermediateDefaultResultListener;
import jadex.commons.future.IntermediateDelegationResultListener;
import jadex.commons.future.SubscriptionIntermediateFuture;
import jadex.commons.future.TerminableIntermediateFuture;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *  Local service registry. Is used by component service containers
 *  to add/remove service registrations.
 *  
 *  Search fetches services by types and excludes some according to
 *  the scope.
 */
//todo: rename PlatformServiceRegistry
public class LocalServiceRegistry
{
	/** The map of published services sorted by type. */
	protected Map<ClassInfo, Set<IService>> services;
	
	/** The excluded components. */
	protected Set<IComponentIdentifier> excluded;
	
	/**
	 *  Add an excluded component. 
	 *  @param The component identifier.
	 */
	public synchronized void addExcludedComponent(IComponentIdentifier cid)
	{
		if(excluded==null)
		{
			excluded = new HashSet<IComponentIdentifier>();
		}
		excluded.add(cid);
	}
	
	/**
	 *  Remove an excluded component. 
	 *  @param The component identifier.
	 */
	public synchronized void removeExcludedComponent(IComponentIdentifier cid)
	{
		if(excluded!=null)
		{
			excluded.remove(cid);
		}
	}
	
	/**
	 *  Test if a service is included.
	 *  @param ser The service.
	 *  @return True if is included.
	 */
	public synchronized boolean isIncluded(IComponentIdentifier cid, IService ser)
	{
		boolean ret = true;
		if(excluded!=null && excluded.contains(ser.getServiceIdentifier().getProviderId()) && cid!=null)
		{
			IComponentIdentifier target = ser.getServiceIdentifier().getProviderId();
			ret = getDotName(cid).endsWith(getDotName(target));
		}
		return ret;
	}
	
	/**
	 *  Add a service to the registry.
	 *  @param sid The service id.
	 */
	public synchronized void addService(ClassInfo key, IService service)
	{
//		if(service.getServiceIdentifier().getServiceType().getTypeName().indexOf("ITest")!=-1)
//			System.out.println("added: "+service.getServiceIdentifier().getServiceType()+" - "+service.getServiceIdentifier().getProviderId());
		
		if(services==null)
		{
			services = new HashMap<ClassInfo, Set<IService>>();
		}
		
		Set<IService> sers = services.get(key);
		if(sers==null)
		{
			sers = new HashSet<IService>();
			services.put(key, sers);
		}
		
		sers.add(service);
	}
	
	/**
	 *  Remove a service from the registry.
	 *  @param sid The service id.
	 */
	public synchronized void removeService(ClassInfo key, IService service)
	{
//		if(service.getServiceIdentifier().getServiceType().getTypeName().indexOf("ITest")!=-1)
//			System.out.println("removed: "+service.getServiceIdentifier().getServiceType()+" - "+service.getServiceIdentifier().getProviderId());
		
		if(services!=null)
		{
			Set<IService> sers = services.get(key);
			if(sers!=null)
			{
				sers.remove(service);
			}
			else
			{
				System.out.println("Could not remove service from registry: "+key+", "+service.getServiceIdentifier());
			}
		}
		else
		{
			System.out.println("Could not remove service from registry: "+key+", "+service.getServiceIdentifier());
		}
	}
	
	/**
	 *  Search for services.
	 */
	public synchronized <T> T searchService(Class<T> type, IComponentIdentifier cid, String scope)
	{
		if(RequiredServiceInfo.SCOPE_GLOBAL.equals(scope))
			throw new IllegalArgumentException("For global searches async method searchGlobalService has to be used.");
		
		T ret = null;
		Set<IService> sers = getServices(type);
		if(sers!=null && sers.size()>0 && !RequiredServiceInfo.SCOPE_NONE.equals(scope))
		{
			for(IService ser: sers)
			{
				if(checkSearchScope(cid, ser, scope) && checkPublicationScope(cid, ser))
				{
					ret = (T)ser;
					break;
				}
			}
		}
		
		return ret;
	}
	
	/**
	 *  Search for services.
	 */
	public synchronized <T> Collection<T> searchServices(Class<T> type, IComponentIdentifier cid, String scope)
	{
		if(RequiredServiceInfo.SCOPE_GLOBAL.equals(scope))
			throw new IllegalArgumentException("For global searches async method searchGlobalServices has to be used.");
		
		Set<T> ret = null;
		Set<IService> sers = getServices(type);
		if(sers!=null && sers.size()>0 && !RequiredServiceInfo.SCOPE_NONE.equals(scope))
		{
			ret = new HashSet<T>();
			for(IService ser: sers)
			{
				if(checkSearchScope(cid, ser, scope) && checkPublicationScope(cid, ser))
				{
					ret.add((T)ser);
				}
			}
		}
		
		return ret;
	}
	
	/**
	 *  Search for service.
	 */
	public synchronized <T> T searchService(Class<T> type, IComponentIdentifier cid, String scope, IFilter<T> filter)
	{
		if(RequiredServiceInfo.SCOPE_GLOBAL.equals(scope))
			throw new IllegalArgumentException("For global searches async method searchGlobalService has to be used.");
		
		T ret = null;
		
		Set<T> sers = (Set<T>)getServices(type);
		if(sers!=null && sers.size()>0 && !RequiredServiceInfo.SCOPE_NONE.equals(scope))
		{
			for(T ser: sers)
			{
				if(checkSearchScope(cid, (IService)ser, scope) && checkPublicationScope(cid, (IService)ser))
				{
					try
					{
						if(filter==null || filter.filter(ser))
						{
							ret = ser;
							break;
						}
					}
					catch(Exception e)
					{
						System.out.println("Warning: filter threw exception during search: "+filter);
					}
				}
			}
		}
		
		return ret;
	}
	
	/**
	 *  Search for service.
	 */
	public synchronized <T> Collection<T> searchServices(Class<T> type, IComponentIdentifier cid, String scope, IFilter<T> filter)
	{
		if(RequiredServiceInfo.SCOPE_GLOBAL.equals(scope))
			throw new IllegalArgumentException("For global searches async method searchGlobalService has to be used.");
		
		List<T> ret = new ArrayList<T>();
		
		Set<T> sers = (Set<T>)getServices(type);
		if(sers!=null && sers.size()>0 && !RequiredServiceInfo.SCOPE_NONE.equals(scope))
		{
			for(T ser: sers)
			{
				if(checkSearchScope(cid, (IService)ser, scope) && checkPublicationScope(cid, (IService)ser))
				{
					try
					{
						if(filter==null || filter.filter(ser))
						{
							ret.add(ser);
						}
					}
					catch(Exception e)
					{
						System.out.println("Warning: filter threw exception during search: "+filter);
					}
				}
			}
		}
		
		return ret;
	}
	
	/**
	 *  Search for service.
	 */
	public synchronized <T> IFuture<T> searchService(Class<T> type, IComponentIdentifier cid, String scope, IRemoteFilter<T> filter)
	{
		final Future<T> ret = new Future<T>();
		
		if(RequiredServiceInfo.SCOPE_GLOBAL.equals(scope))
		{
			ret.setException(new IllegalArgumentException("For global searches searchGlobalService has to be used."));
		}
		else
		{
			Set<T> sers = (Set<T>)getServices(type);
			if(sers!=null && sers.size()>0 && !RequiredServiceInfo.SCOPE_NONE.equals(scope))
			{
				// filter checks in loop are possibly performed outside of synchronized block
				Iterator<T> it = new HashSet<T>(sers).iterator();
				searchLoopService(filter, it, cid, scope).addResultListener(new DelegationResultListener<T>(ret));
			}
			else
			{
				ret.setException(new ServiceNotFoundException(type.getName()));
			}
		}
		return ret;
	}
	
	/**
	 * 
	 * @param filter
	 * @param it
	 * @return
	 */
	protected <T> IFuture<T> searchLoopService(final IRemoteFilter<T> filter, final Iterator<T> it, final IComponentIdentifier cid, final String scope)
	{
		final Future<T> ret = new Future<T>();
		
		if(it.hasNext())
		{
			final T ser = it.next();
			if(!checkSearchScope(cid, (IService)ser, scope) || !checkPublicationScope(cid, (IService)ser))
			{
				searchLoopService(filter, it, cid, scope).addResultListener(new DelegationResultListener<T>(ret));
			}
			else
			{
				if(filter==null)
				{
					ret.setResult(ser);
				}
				else
				{
					filter.filter(ser).addResultListener(new IResultListener<Boolean>()
					{
						public void resultAvailable(Boolean result)
						{
							if(result!=null && result.booleanValue())
							{
								ret.setResult(ser);
							}
							else
							{
								searchLoopService(filter, it, cid, scope).addResultListener(new DelegationResultListener<T>(ret));
							}
						}
						
						public void exceptionOccurred(Exception exception)
						{
							searchLoopService(filter, it, cid, scope).addResultListener(new DelegationResultListener<T>(ret));
						}
					});
				}
			}
		}
		else
		{
			ret.setException(new ServiceNotFoundException("No service that fits filter: "+filter));
		}
		
		return ret;
	}
	
	/**
	 *  Search for services.
	 */
	public synchronized <T> ISubscriptionIntermediateFuture<T> searchServices(Class<T> type, IComponentIdentifier cid, String scope, IRemoteFilter<T> filter)
	{
		final SubscriptionIntermediateFuture<T> ret = new SubscriptionIntermediateFuture<T>();
		
		if(RequiredServiceInfo.SCOPE_GLOBAL.equals(scope))
		{
			ret.setException(new IllegalArgumentException("For global searches searchGlobalServices has to be used."));
		}
		else
		{
			if(services!=null)
			{
				Set<T> sers = (Set<T>)services.get(new ClassInfo(type));
				if(sers!=null && sers.size()>0 && !RequiredServiceInfo.SCOPE_NONE.equals(scope))
				{
					// filter checks in loop are possibly performed outside of synchornized block
					Iterator<T> it = new HashSet<T>(sers).iterator();
					searchLoopServices(filter, it, cid, scope).addResultListener(new IntermediateDelegationResultListener<T>(ret));
				}
				else
				{
					ret.setFinished();
				}
			}
			else
			{
				ret.setFinished();
			}
		}
		return ret;
	}
	
	/**
	 * 
	 * @param filter
	 * @param it
	 * @return
	 */
	protected <T> ISubscriptionIntermediateFuture<T> searchLoopServices(final IRemoteFilter<T> filter, final Iterator<T> it, final IComponentIdentifier cid, final String scope)
	{
		final SubscriptionIntermediateFuture<T> ret = new SubscriptionIntermediateFuture<T>();
		
		if(it.hasNext())
		{
			final T ser = it.next();
			if(!checkSearchScope(cid, (IService)ser, scope) || !checkPublicationScope(cid, (IService)ser))
			{
				searchLoopServices(filter, it, cid, scope).addResultListener(new IntermediateDelegationResultListener<T>(ret));
			}
			else
			{
				if(filter==null)
				{
					ret.addIntermediateResult(ser);
					searchLoopServices(filter, it, cid, scope).addResultListener(new IntermediateDelegationResultListener<T>(ret));
				}
				else
				{
					filter.filter(ser).addResultListener(new IResultListener<Boolean>()
					{
						public void resultAvailable(Boolean result)
						{
							if(result!=null && result.booleanValue())
							{
								ret.addIntermediateResult(ser);
							}
							searchLoopServices(filter, it, cid, scope).addResultListener(new IntermediateDelegationResultListener<T>(ret));
						}
						
						public void exceptionOccurred(Exception exception)
						{
							searchLoopServices(filter, it, cid, scope).addResultListener(new IntermediateDelegationResultListener<T>(ret));
						}
					});
				}
			}
		}
		else
		{
			ret.setFinished();
		}
		
		return ret;
	}
	
	/**
	 *  Search for services.
	 */
	public synchronized <T> IFuture<T> searchGlobalService(final Class<T> type, IComponentIdentifier cid, final IRemoteFilter<T> filter)
	{
		final Future<T> ret = new Future<T>();
		
		searchService(type, cid, RequiredServiceInfo.SCOPE_PLATFORM, filter).addResultListener(new IResultListener<T>()
		{
			public void resultAvailable(T result)
			{
				ret.setResult(result);
			}

			public void exceptionOccurred(Exception exception)
			{
				searchRemoteService(type, filter).addResultListener(new DelegationResultListener<T>(ret));
			}
		});
		
		return ret;
	}
	
	/**
	 *  Search for services.
	 */
	public synchronized <T> ITerminableIntermediateFuture<T> searchGlobalServices(Class<T> type, IComponentIdentifier cid, IRemoteFilter<T> filter)
	{
//		System.out.println("Search global services: "+type);
		
		final TerminableIntermediateFuture<T> ret = new TerminableIntermediateFuture<T>();
		
		final CounterResultListener<Void> lis = new CounterResultListener<Void>(2, true, new ExceptionDelegationResultListener<Void, Collection<T>>(ret)
		{
			public void customResultAvailable(Void result)
			{
				ret.setFinished();
			}
		});
		
		searchServices(type, cid, RequiredServiceInfo.SCOPE_PLATFORM, filter).addResultListener(new IntermediateDefaultResultListener<T>()
		{
			public void intermediateResultAvailable(T result)
			{
				ret.addIntermediateResult(result);
			}
			
			public void finished()
			{
				lis.resultAvailable(null);
			}
			
			public void exceptionOccurred(Exception exception)
			{
				lis.resultAvailable(null);
			}
		});
		
		searchRemoteServices(type, filter).addResultListener(new IntermediateDefaultResultListener<T>()
		{
			public void intermediateResultAvailable(T result)
			{
				ret.addIntermediateResult(result);
			}
			
			public void finished()
			{
				lis.resultAvailable(null);
			}
			
			public void exceptionOccurred(Exception exception)
			{
				lis.resultAvailable(null);
			}
		});
		
		return ret;
	}
	
	/**
	 *  Check if service is ok with respect to search scope of caller.
	 */
	protected boolean checkSearchScope(IComponentIdentifier cid, IService ser, String scope)
	{
		boolean ret = false;
		
		if(!isIncluded(cid, ser))
		{
			return ret;
		}
		
		if(scope==null)
		{
			scope = RequiredServiceInfo.SCOPE_APPLICATION;
		}
		
		if(RequiredServiceInfo.SCOPE_PLATFORM.equals(scope) || RequiredServiceInfo.SCOPE_GLOBAL.equals(scope))
		{
			ret = true;
		}
		else if(RequiredServiceInfo.SCOPE_APPLICATION.equals(scope))
		{
			IComponentIdentifier sercid = ser.getServiceIdentifier().getProviderId();
			ret = sercid.getPlatformName().equals(cid.getPlatformName())
				&& getApplicationName(sercid).equals(getApplicationName(cid));
		}
		else if(RequiredServiceInfo.SCOPE_COMPONENT.equals(scope))
		{
			IComponentIdentifier sercid = ser.getServiceIdentifier().getProviderId();
			ret = getDotName(sercid).endsWith(getDotName(cid));
		}
		else if(RequiredServiceInfo.SCOPE_LOCAL.equals(scope))
		{
			// only the component itself
			ret = ser.getServiceIdentifier().getProviderId().equals(cid);
		}
		else if(RequiredServiceInfo.SCOPE_PARENT.equals(scope))
		{
			// check if parent of searcher reaches the service
			IComponentIdentifier sercid = ser.getServiceIdentifier().getProviderId();
			String subname = getSubcomponentName(cid);
			ret = sercid.getName().endsWith(subname);
		}
		else if(RequiredServiceInfo.SCOPE_UPWARDS.equals(scope))
		{
			// Test if service id is part of searcher id, service is upwards from searcher
			IComponentIdentifier sercid = ser.getServiceIdentifier().getProviderId();
			ret = getDotName(cid).endsWith(getDotName(sercid));
			
//			IComponentIdentifier sercid = ser.getServiceIdentifier().getProviderId();
//			String subname = getSubcomponentName(cid);
//			ret = sercid.getName().endsWith(subname);
//			
//			while(cid!=null)
//			{
//				if(sercid.equals(cid))
//				{
//					ret = true;
//					break;
//				}
//				else
//				{
//					cid = cid.getParent();
//				}
//			}
		}
		
		return ret;
	}
	
	/**
	 *  Check if service is ok with respect to publication scope.
	 */
	protected boolean checkPublicationScope(IComponentIdentifier cid, IService ser)
	{
		boolean ret = false;
		
		String scope = ser.getServiceIdentifier().getScope()!=null? ser.getServiceIdentifier().getScope(): RequiredServiceInfo.SCOPE_GLOBAL;
		
		if(RequiredServiceInfo.SCOPE_GLOBAL.equals(scope))
		{
			ret = true;
		}
		else if(RequiredServiceInfo.SCOPE_PLATFORM.equals(scope))
		{
			// Test if searcher and service are on same platform
			ret = cid.getPlatformName().equals(ser.getServiceIdentifier().getProviderId().getPlatformName());
		}
		else if(RequiredServiceInfo.SCOPE_APPLICATION.equals(scope))
		{
			// todo: special case platform service with app scope
			IComponentIdentifier sercid = ser.getServiceIdentifier().getProviderId();
			ret = sercid.getPlatformName().equals(cid.getPlatformName())
				&& getApplicationName(sercid).equals(getApplicationName(cid));
		}
		else if(RequiredServiceInfo.SCOPE_COMPONENT.equals(scope))
		{
			IComponentIdentifier sercid = ser.getServiceIdentifier().getProviderId();
			ret = getDotName(cid).endsWith(getDotName(sercid));
		}
		else if(RequiredServiceInfo.SCOPE_LOCAL.equals(scope))
		{
			// only the component itself
			ret = ser.getServiceIdentifier().getProviderId().equals(cid);
		}
		else if(RequiredServiceInfo.SCOPE_PARENT.equals(scope))
		{
			// check if parent of service reaches the searcher
			IComponentIdentifier sercid = ser.getServiceIdentifier().getProviderId();
			String subname = getSubcomponentName(sercid);
			ret = getDotName(cid).endsWith(subname);
		}
		else if(RequiredServiceInfo.SCOPE_UPWARDS.equals(scope))
		{
			// check if searcher is upwards from service (part of name)
			IComponentIdentifier sercid = ser.getServiceIdentifier().getProviderId();
			ret = getDotName(sercid).endsWith(getDotName(cid));
		}
		
		return ret;
	}
	
	/**
	 *  Search for services on remote platforms.
	 *  @param type The type.
	 *  @param scope The scope.
	 */
	protected <T> ITerminableIntermediateFuture<T> searchRemoteServices(final Class<T> type, final IRemoteFilter<T> filter)
	{
		final TerminableIntermediateFuture<T> ret = new TerminableIntermediateFuture<T>();
		// Must not find services twice (e.g. having two proxies for the same platform)
		final Set<T> founds = new HashSet<T>();
		
		if(services!=null)
		{
			Set<IService> sers = services.get(new ClassInfo(IProxyAgentService.class));
			if(sers!=null && sers.size()>0)
			{
				final CounterResultListener<Void> clis = new CounterResultListener<Void>(sers.size(), new ExceptionDelegationResultListener<Void, Collection<T>>(ret)
				{
					public void customResultAvailable(Void result)
					{
						ret.setFinished();
					}
				});
				
				for(IService ser: sers)
				{
					IProxyAgentService ps = (IProxyAgentService)ser;
					
					ps.getRemoteComponentIdentifier().addResultListener(new IResultListener<IComponentIdentifier>()
					{
						public void resultAvailable(IComponentIdentifier rcid)
						{
							IRemoteServiceManagementService rms = getService(IRemoteServiceManagementService.class);	
							IFuture<Collection<T>> rsers = rms.getServiceProxies(rcid, type, RequiredServiceInfo.SCOPE_PLATFORM, filter);
							rsers.addResultListener(new IResultListener<Collection<T>>()
							{
								public void resultAvailable(Collection<T> result)
								{
									for(T t: result)
									{
										if(!founds.contains(t))
										{
											ret.addIntermediateResult(t);
										}
										founds.add(t);
									}
									clis.resultAvailable(null);
								}
								
								public void exceptionOccurred(Exception exception)
								{
									clis.resultAvailable(null);
								}
							});
						}
						
						public void exceptionOccurred(Exception exception)
						{
							clis.resultAvailable(null);
						}
					});
				}
			}
			else
			{
				ret.setFinished();
			}
		}
		else
		{
			ret.setFinished();
		}
		
//		ret.addResultListener(new IntermediateDefaultResultListener<T>()
//		{
//			public void intermediateResultAvailable(T result)
//			{
//				System.out.println("found: "+result);
//			}
//			
//			public void exceptionOccurred(Exception exception)
//			{
//				System.out.println("ex: "+exception);
//			}
//		});
		
		return ret;
	}
	
	/**
	 *  Search for services on remote platforms.
	 *  @param type The type.
	 *  @param scope The scope.
	 */
	protected <T> IFuture<T> searchRemoteService(final Class<T> type, final IRemoteFilter<T> filter)
	{
		final Future<T> ret = new Future<T>();
		
		if(services!=null)
		{
			Set<IService> sers = getServices(IProxyAgentService.class);
			if(sers!=null && sers.size()>0)
			{
				final CounterResultListener<Void> clis = new CounterResultListener<Void>(sers.size(), new ExceptionDelegationResultListener<Void, T>(ret)
				{
					public void customResultAvailable(Void result)
					{
						ret.setExceptionIfUndone(new ServiceNotFoundException(type.getName()));
					}
				});
				
				for(IService ser: sers)
				{
					IProxyAgentService ps = (IProxyAgentService)ser;
					
					ps.getRemoteComponentIdentifier().addResultListener(new IResultListener<IComponentIdentifier>()
					{
						public void resultAvailable(IComponentIdentifier rcid)
						{
							IRemoteServiceManagementService rms = getService(IRemoteServiceManagementService.class);	
							IFuture<T> rsers = rms.getServiceProxy(rcid, type, RequiredServiceInfo.SCOPE_PLATFORM, filter);
							rsers.addResultListener(new IResultListener<T>()
							{
								public void resultAvailable(T result)
								{
									ret.setResultIfUndone(result);
									clis.resultAvailable(null);
								}
								
								public void exceptionOccurred(Exception exception)
								{
									clis.resultAvailable(null);
								}
							});
						}
						
						public void exceptionOccurred(Exception exception)
						{
							clis.resultAvailable(null);
						}
					});
				}
			}
			else
			{
				ret.setExceptionIfUndone(new ServiceNotFoundException(type.getName()));
			}
		}
		else
		{
			ret.setExceptionIfUndone(new ServiceNotFoundException(type.getName()));
		}
		
		return ret;
	}
	
	/**
	 *  Get a service per type.
	 *  @param type The interface type.
	 *  @return First matching service or null.
	 */
	protected <T> T getService(Class<T> type)
	{
		Set<T> sers = services==null? null: (Set<T>)services.get(new ClassInfo(type));
		return sers==null || sers.size()==0? null: (T)sers.iterator().next();
	}
	
	/**
	 * 
	 */
	protected Set<IService> getServices(Class<?> type)
	{
		Set<IService> ret = Collections.EMPTY_SET;;
		if(services!=null)
		{
			if(type!=null)
			{
				ret = services.get(new ClassInfo(type));
			}
			else
			{
				ret = new HashSet<IService>();
				for(ClassInfo t: services.keySet())
				{
					ret.addAll(services.get(t));
				}
			}
		}
		return ret;
	}
	
	/**
	 *  Get the application name. Equals the local component name in case it is a child of the platform.
	 *  broadcast@awa.plat1 -> awa
	 *  @return The application name.
	 */
	public static String getApplicationName(IComponentIdentifier cid)
	{
		String ret = cid.getName();
		int idx;
		// If it is a direct subcomponent
		if((idx = ret.lastIndexOf('.')) != -1)
		{
			// cut off platform name
			ret = ret.substring(0, idx);
			// cut off local name 
			if((idx = ret.indexOf('@'))!=-1)
				ret = ret.substring(idx + 1);
			if((idx = ret.indexOf('.'))!=-1)
				ret = ret.substring(idx + 1);
		}
		else
		{
			ret = cid.getLocalName();
		}
		return ret;
	}
	
	/**
	 *  Get the subcomponent name.
	 *  @param cid The component id.
	 *  @return The subcomponent name.
	 */
	public static String getSubcomponentName(IComponentIdentifier cid)
	{
		String ret = cid.getName();
		int idx;
		if((idx = ret.indexOf('@'))!=-1)
			ret = ret.substring(idx + 1);
		return ret;
	}
	
	/**
	 *  
	 */
	public static String getDotName(IComponentIdentifier cid)
	{
		return cid.getParent()==null? cid.getName(): cid.getLocalName()+"."+getSubcomponentName(cid);
	}
}
