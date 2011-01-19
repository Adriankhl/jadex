package jadex.commons.service;

import jadex.commons.Future;
import jadex.commons.IFuture;
import jadex.commons.IIntermediateFuture;
import jadex.commons.IntermediateDelegationResultListener;
import jadex.commons.IntermediateFuture;
import jadex.commons.concurrent.DelegationResultListener;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 *  Static helper class for searching services.
 */
public class SServiceProvider
{	
	//-------- constants --------
	
	/** The sequential search manager. */
	public static ISearchManager sequentialmanager = new SequentialSearchManager();
//	public static ISearchManager sequentialmanagerforced = new SequentialSearchManager(true, true, true);

	/** The parallel search manager. */
	public static ISearchManager parallelmanager = new ParallelSearchManager();
//	public static ISearchManager parallelmanagerforced = new ParallelSearchManager(true, true, true);
	
	/** The sequential search manager that searches only upwards. */
	public static ISearchManager upwardsmanager = new SequentialSearchManager(true, false);

	/** The sequential search manager that searches only locally. */
	public static ISearchManager localmanager = new LocalSearchManager();
//	public static ISearchManager localmanagerforced = new LocalSearchManager(true);
	
	/** The visit decider that stops searching after one result has been found. */
//	public static IVisitDecider abortdecider = new DefaultVisitDecider();
//	public static IVisitDecider rabortdecider = new DefaultVisitDecider(true, RequiredServiceInfo.GLOBAL_SCOPE);

	/** The visit decider that never stops. */
	public static IVisitDecider contdecider = new DefaultVisitDecider(false);
	public static IVisitDecider rcontdecider = new DefaultVisitDecider(false, RequiredServiceInfo.SCOPE_GLOBAL);

	public static IResultSelector contanyselector = new AnyResultSelector(false);
	public static IResultSelector abortanyselector = new AnyResultSelector(true);

	public static Map avisitdeciders;
	public static Map visitdeciders;
	
	static
	{
		avisitdeciders = new HashMap();
		avisitdeciders.put(RequiredServiceInfo.SCOPE_LOCAL, new DefaultVisitDecider(true, RequiredServiceInfo.SCOPE_LOCAL));
		avisitdeciders.put(RequiredServiceInfo.SCOPE_COMPONENT, new DefaultVisitDecider(true, RequiredServiceInfo.SCOPE_COMPONENT));
		avisitdeciders.put(RequiredServiceInfo.SCOPE_APPLICATION, new DefaultVisitDecider(true, RequiredServiceInfo.SCOPE_APPLICATION));
		avisitdeciders.put(RequiredServiceInfo.SCOPE_PLATFORM, new DefaultVisitDecider(true, RequiredServiceInfo.SCOPE_PLATFORM));
		avisitdeciders.put(RequiredServiceInfo.SCOPE_GLOBAL, new DefaultVisitDecider(true, RequiredServiceInfo.SCOPE_GLOBAL));
		
		visitdeciders = new HashMap();
		visitdeciders.put(RequiredServiceInfo.SCOPE_LOCAL, new DefaultVisitDecider(false, RequiredServiceInfo.SCOPE_LOCAL));
		visitdeciders.put(RequiredServiceInfo.SCOPE_COMPONENT, new DefaultVisitDecider(false, RequiredServiceInfo.SCOPE_COMPONENT));
		visitdeciders.put(RequiredServiceInfo.SCOPE_APPLICATION, new DefaultVisitDecider(false, RequiredServiceInfo.SCOPE_APPLICATION));
		visitdeciders.put(RequiredServiceInfo.SCOPE_PLATFORM, new DefaultVisitDecider(false, RequiredServiceInfo.SCOPE_PLATFORM));
		visitdeciders.put(RequiredServiceInfo.SCOPE_GLOBAL, new DefaultVisitDecider(false, RequiredServiceInfo.SCOPE_GLOBAL));

	}
	
	//-------- methods --------

//	protected static Map	profiling	= new HashMap();
//	
//	static
//	{
//		new Thread(new Runnable()
//		{
//			public void run()
//			{
//				try
//				{
//					Thread.sleep(5000);
//					
//					synchronized(profiling)
//					{
//						System.out.println("--------------------");
//						for(Iterator it=profiling.keySet().iterator(); it.hasNext(); )
//						{
//							Object	key	= it.next();
//							System.out.println(key+":\t"+profiling.get(key));
//						}
//					}
//				}
//				catch(InterruptedException e)
//				{
//					e.printStackTrace();
//				}
//			}
//		}).start();
//	}
	
	/**
	 *  Get one service of a type.
	 *  @param type The class.
	 *  @return The corresponding service.
	 */
	public static IFuture getService(IServiceProvider provider, Class type)
	{
		return getService(provider, type, null);
	}
	
//	/**
//	 *  Get one service of a type.
//	 *  @param type The class.
//	 *  @return The corresponding service.
//	 */
//	public static IFuture getService(IServiceProvider provider, Class type, boolean remote)
//	{
//		return getService(provider, type, false, false);
//	}
	
//	/**
//	 *  Get one service of a type.
//	 *  @param type The class.
//	 *  @return The corresponding service.
//	 */
//	public static IFuture getService(final IServiceProvider provider, final Class type, final boolean remote, final boolean forcedsearch)
//	{
////		synchronized(profiling)
////		{
////			Integer	cnt	= (Integer)profiling.get(type);
////			profiling.put(type, new Integer(cnt!=null ? cnt.intValue()+1 : 1)); 
////		}
//		final Future ret = new Future();
//		
//		// Hack->remove
////		IVisitDecider abortdecider = new DefaultVisitDecider();
////		IVisitDecider rabortdecider = new DefaultVisitDecider(true, false);
//		
//		provider.getServices(forcedsearch? sequentialmanagerforced: sequentialmanager, 
//			remote? getVisitDecider(true, RequiredServiceInfo.GLOBAL_SCOPE): getVisitDecider(true), 
//			new TypeResultSelector(type, true, remote))
//				.addResultListener(new DelegationResultListener(ret)
//		{
//			public void customResultAvailable(Object result)
//			{
////				System.out.println("Search result: "+result);
//				Collection res = (Collection)result;
//				if(res==null || res.size()==0)
//				{
//					getService(provider, type, remote, forcedsearch).addResultListener(new DefaultResultListener()
//					{
//						public void resultAvailable(Object result)
//						{
//							System.out.println("rrr: "+result);
//						}
//					});
//					exceptionOccurred(new ServiceNotFoundException("No matching service found for type: "+type.getName()));
//				}
//				else
//					super.customResultAvailable(res.iterator().next());
//			}
//		});
//		
//		return ret;
//	}
	
	/**
	 *  Get one service of a type.
	 *  @param type The class.
	 *  @return The corresponding service.
	 */
	public static IFuture getService(final IServiceProvider provider, final Class type, final String scope)
	{
//		synchronized(profiling)
//		{
//			Integer	cnt	= (Integer)profiling.get(type);
//			profiling.put(type, new Integer(cnt!=null ? cnt.intValue()+1 : 1)); 
//		}
		final Future ret = new Future();
		
		// Hack->remove
//		IVisitDecider abortdecider = new DefaultVisitDecider();
//		IVisitDecider rabortdecider = new DefaultVisitDecider(true, false);
		
		provider.getServices(getSearchManager(false, scope), getVisitDecider(true, scope), 
			new TypeResultSelector(type, true, RequiredServiceInfo.SCOPE_GLOBAL.equals(scope)))
				.addResultListener(new DelegationResultListener(ret)
		{
			public void customResultAvailable(Object result)
			{
//				System.out.println("Search result: "+result);
				Collection res = (Collection)result;
				if(res==null || res.size()==0)
				{
//					getService(provider, type, scope).addResultListener(new DefaultResultListener()
//					{
//						public void resultAvailable(Object result)
//						{
//							System.out.println("rrr: "+result);
//						}
//					});
					exceptionOccurred(new ServiceNotFoundException("No matching service found for type: "+type.getName()));
				}
				else
					super.customResultAvailable(res.iterator().next());
			}
		});
		
		return ret;
	}
	
	/**
	 *  Get one service with id.
	 *  @param type The class.
	 *  @return The corresponding service.
	 */
	public static IFuture getService(IServiceProvider provider, final IServiceIdentifier sid)
	{
//		synchronized(profiling)
//		{
//			Integer	cnt	= (Integer)profiling.get(type);
//			profiling.put(type, new Integer(cnt!=null ? cnt.intValue()+1 : 1)); 
//		}
		final Future ret = new Future();
		
		// Hack->remove
//		IVisitDecider abortdecider = new DefaultVisitDecider();
		
		provider.getServices(getSearchManager(false), getVisitDecider(true), new IdResultSelector(sid))
			.addResultListener(new DelegationResultListener(ret)
		{
			public void customResultAvailable(Object result)
			{
				Collection res = (Collection)result;
				if(res==null || res.size()==0)
					exceptionOccurred(new ServiceNotFoundException("No service found for id: "+sid));
				else
					super.customResultAvailable(res.iterator().next());
			}
		});
		
		return ret;
	}
	
	/**
	 *  Get one service of a type.
	 *  @param type The class.
	 *  @return The corresponding service.
	 */
	public static IFuture getService(IServiceProvider provider, final IResultSelector selector)
	{
//		synchronized(profiling)
//		{
//			Integer	cnt	= (Integer)profiling.get(selector.getCacheKey());
//			profiling.put(selector.getCacheKey(), new Integer(cnt!=null ? cnt.intValue()+1 : 1)); 
//		}
		final Future ret = new Future();
		
		// Hack->remove
//		IVisitDecider abortdecider = new DefaultVisitDecider();
		
		provider.getServices(getSearchManager(false), getVisitDecider(true), selector)
			.addResultListener(new DelegationResultListener(ret)
		{
			public void customResultAvailable(Object result)
			{
				Collection res = (Collection)result;
				if(res==null || res.size()==0)
					exceptionOccurred(new ServiceNotFoundException("No matching service found for: "+selector));
				else
					super.customResultAvailable(res.iterator().next());
			}
		});
		
		return ret;
	}
	
	/**
	 *  Get all services of a type.
	 *  @param type The class.
	 *  @return The corresponding services.
	 */
	public static IIntermediateFuture getServices(IServiceProvider provider, Class type)
	{
		return getServices(provider, type, null);
	}
	
//	/**
//	 *  Get all services of a type.
//	 *  @param type The class.
//	 *  @return The corresponding services.
//	 */
//	public static IIntermediateFuture getServices(IServiceProvider provider, Class type, boolean remote)
//	{
//		return getServices(provider, type, remote, false);
//	}
	
	/**
	 *  Get all services of a type.
	 *  @param type The class.
	 *  @return The corresponding services.
	 */
	public static IIntermediateFuture getServices(IServiceProvider provider, Class type, String scope)
	{
//		synchronized(profiling)
//		{
//			Integer	cnt	= (Integer)profiling.get(type);
//			profiling.put(type, new Integer(cnt!=null ? cnt.intValue()+1 : 1)); 
//		}
		final IntermediateFuture ret = new IntermediateFuture();
		
		// Hack->remove
//		IVisitDecider contdecider = new DefaultVisitDecider(false);
//		IVisitDecider rcontdecider = new DefaultVisitDecider(false, false);
		
		provider.getServices(getSearchManager(true, scope), 
			getVisitDecider(false, scope),
			new TypeResultSelector(type, false, RequiredServiceInfo.SCOPE_GLOBAL.equals(scope)))
				.addResultListener(new IntermediateDelegationResultListener(ret));
//				{
//					public void customResultAvailable(Object source, Object result)
//					{
//						System.out.println(6);
//						super.customResultAvailable(source, result);
//					}
//				});
		
		return ret;
	}
	
	/**
	 *  Get one service of a type and only search upwards (parents).
	 *  @param type The class.
	 *  @return The corresponding service.
	 */
	public static IFuture getServiceUpwards(IServiceProvider provider, Class type)
	{
		return getService(provider, type, RequiredServiceInfo.SCOPE_UPWARDS);
	}
	
//	/**
//	 *  Get one service of a type and only search upwards (parents).
//	 *  @param type The class.
//	 *  @return The corresponding service.
//	 */
//	public static IFuture getServiceUpwards(final IServiceProvider provider, final Class type)
//	{
////		synchronized(profiling)
////		{
////			Integer	cnt	= (Integer)profiling.get(type);
////			profiling.put(type, new Integer(cnt!=null ? cnt.intValue()+1 : 1)); 
////		}
//		final Future ret = new Future();
//		
//		// Hack->remove
////		IVisitDecider abortdecider = new DefaultVisitDecider();
//		
//		provider.getServices(upwardsmanager, getVisitDecider(true, RequiredServiceInfo.PLATFORM_SCOPE), new TypeResultSelector(type))
//			.addResultListener(new DelegationResultListener(ret)
//		{
//			public void customResultAvailable(Object result)
//			{
//				Collection res = (Collection)result;
//				if(res==null || res.size()==0)
//				{
////					provider.getServices(upwardsmanager, abortdecider, new TypeResultSelector(type))
////						.addResultListener(new DefaultResultListener()
////					{
////						public void resultAvailable(Object result)
////						{
////							System.out.println("service not found: "+result);
////						}
////					});
//					exceptionOccurred(new ServiceNotFoundException("No matching service found for type: "+type.getName()));
//				}
//				else
//					super.customResultAvailable(res.iterator().next());
//			}
//		});
//		
//		return ret;
//	}
	
//	/**
//	 *  Get the declared service of a type and only search the current provider.
//	 *  @param type The class.
//	 *  @return The corresponding service.
//	 */
//	public static IFuture getDeclaredService(IServiceProvider provider, final Class type)
//	{
////		synchronized(profiling)
////		{
////			Integer	cnt	= (Integer)profiling.get(type);
////			profiling.put(type, new Integer(cnt!=null ? cnt.intValue()+1 : 1)); 
////		}
//		final Future ret = new Future();
//		
//		// Hack->remove
////		IVisitDecider abortdecider = new DefaultVisitDecider();
//		
//		provider.getServices(localmanager, getVisitDecider(true, RequiredServiceInfo.LOCAL_SCOPE), new TypeResultSelector(type))
//			.addResultListener(new DelegationResultListener(ret)
//		{
//			public void customResultAvailable(Object result)
//			{
//				Collection res = (Collection)result;
//				if(res==null || res.size()==0)
//					exceptionOccurred(new ServiceNotFoundException("No matching service found for type: "+type.getName()));
//				else
//					super.customResultAvailable(res.iterator().next());
//			}
//		});
//		
//		return ret;
//	}
	
//	/**
//	 *  Get the declared services of a type and only search the current provider.
//	 *  @param type The class.
//	 *  @return The corresponding services.
//	 */
//	public static IFuture getDeclaredServices(IServiceProvider provider, Class type)
//	{
////		synchronized(profiling)
////		{
////			Integer	cnt	= (Integer)profiling.get(type);
////			profiling.put(type, new Integer(cnt!=null ? cnt.intValue()+1 : 1)); 
////		}
//		final Future ret = new Future();
//		
//		// Hack->remove
////		IVisitDecider abortdecider = new DefaultVisitDecider();
//		
//		provider.getServices(localmanager, getVisitDecider(false, RequiredServiceInfo.LOCAL_SCOPE), new TypeResultSelector(type))
//			.addResultListener(new DelegationResultListener(ret));
//		
//		return ret;
//	}
	
	/**
	 *  Get all declared services of the given provider.
	 *  @return The corresponding services.
	 */
	public static IIntermediateFuture getDeclaredServices(IServiceProvider provider)
	{
//		synchronized(profiling)
//		{
//			Integer	cnt	= (Integer)profiling.get(type);
//			profiling.put(type, new Integer(cnt!=null ? cnt.intValue()+1 : 1)); 
//		}
		final IntermediateFuture ret = new IntermediateFuture();
		
		// Hack->remove
//		IVisitDecider contdecider = new DefaultVisitDecider(false);
		
		provider.getServices(getSearchManager(false, RequiredServiceInfo.SCOPE_LOCAL), contdecider, contanyselector)
			.addResultListener(new IntermediateDelegationResultListener(ret));
		
		return ret;
	}
	
//	/**
//	 *  Get all declared services of the given provider.
//	 *  @return The corresponding services.
//	 */
//	public static IIntermediateFuture getDeclaredServices(IServiceProvider provider, boolean forcedsearch)
//	{
////		synchronized(profiling)
////		{
////			Integer	cnt	= (Integer)profiling.get(type);
////			profiling.put(type, new Integer(cnt!=null ? cnt.intValue()+1 : 1)); 
////		}
//		final IntermediateFuture ret = new IntermediateFuture();
//		
//		// Hack->remove
////		IVisitDecider contdecider = new DefaultVisitDecider(false);
//		
//		provider.getServices(forcedsearch? localmanagerforced: localmanager, 
//			contdecider, contanyselector)
//				.addResultListener(new IntermediateDelegationResultListener(ret));
//		
//		return ret;
//	}
	
//	/**
//	 *  Get the declared service with id and only search the current provider.
//	 *  @param sid The service identifier.
//	 *  @return The corresponding service.
//	 */
//	public static IFuture getDeclaredService(IServiceProvider provider, final IServiceIdentifier sid)
//	{
////		synchronized(profiling)
////		{
////			Integer	cnt	= (Integer)profiling.get(type);
////			profiling.put(type, new Integer(cnt!=null ? cnt.intValue()+1 : 1)); 
////		}
//		final Future ret = new Future();
//		
//		// Hack->remove
////		IVisitDecider abortdecider = new DefaultVisitDecider();
//		
////		provider.getServices(localmanager, abortdecider, new IdResultSelector(sid))
//		provider.getServices(localmanager, getVisitDecider(true, RequiredServiceInfo.LOCAL_SCOPE), new IdResultSelector(sid))
//			.addResultListener(new DelegationResultListener(ret)
//		{
//			public void customResultAvailable(Object result)
//			{
//				Collection res = (Collection)result;
//				if(res==null || res.size()==0)
//					exceptionOccurred(new ServiceNotFoundException("No matching service found for type: "+sid));
//				else
//					super.customResultAvailable(res.iterator().next());
//			}
//		});
//		
//		return ret;
//	}
	
	/**
	 *  Get the fitting visit decider.
	 */
	public static IVisitDecider getVisitDecider(boolean abort)
	{
		return getVisitDecider(abort, null);
	}
	
	/**
	 *  Get the fitting visit decider.
	 */
	public static IVisitDecider getVisitDecider(boolean abort, String scope)
	{
		// Use application scope as default, use platform scope visit decider for upwards search
		scope = scope==null? RequiredServiceInfo.SCOPE_APPLICATION
			: RequiredServiceInfo.SCOPE_UPWARDS.equals(scope) ? RequiredServiceInfo.SCOPE_PLATFORM : scope;
		return (IVisitDecider)(abort? avisitdeciders.get(scope): visitdeciders.get(scope));
	}
	
	/**
	 *  Get the fitting search manager.
	 *  @param multiple	The multiple flag (i.e. one vs. multiple services required)
	 */
	public static ISearchManager	getSearchManager(boolean multiple)
	{
		return getSearchManager(multiple, null);
	}
	
	/**
	 *  Get the fitting search manager.
	 *  @param multiple	The multiple flag (i.e. one vs. multiple services required)
	 *  @param scope	The search scope.
	 */
	public static ISearchManager	getSearchManager(boolean multiple, String scope)
	{
		// Use application scope as default
		scope = scope==null? RequiredServiceInfo.SCOPE_APPLICATION : scope;

		ISearchManager	ret;
		
		if(RequiredServiceInfo.SCOPE_UPWARDS.equals(scope))
		{
			ret	= upwardsmanager;
		}
		else if(RequiredServiceInfo.SCOPE_LOCAL.equals(scope))
		{
			ret	= localmanager;
		}
		else if(multiple)
		{
			ret	= parallelmanager;
		}
		else
		{
			// Todo: use parallel also for single searches?
			ret	= sequentialmanager;
		}
		
		return ret;
	}
}
