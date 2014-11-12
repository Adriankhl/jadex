package jadex.bridge.component.impl;

import jadex.bridge.IComponentIdentifier;
import jadex.bridge.IComponentStep;
import jadex.bridge.IExternalAccess;
import jadex.bridge.IInternalAccess;
import jadex.bridge.component.ComponentCreationInfo;
import jadex.bridge.component.INFPropertyComponentFeature;
import jadex.bridge.modelinfo.NFPropertyInfo;
import jadex.bridge.nonfunctional.AbstractNFProperty;
import jadex.bridge.nonfunctional.INFMixedPropertyProvider;
import jadex.bridge.nonfunctional.INFProperty;
import jadex.bridge.nonfunctional.INFPropertyProvider;
import jadex.bridge.nonfunctional.NFMethodPropertyProvider;
import jadex.bridge.nonfunctional.NFPropertyProvider;
import jadex.bridge.nonfunctional.annotation.NFProperties;
import jadex.bridge.nonfunctional.annotation.NFProperty;
import jadex.bridge.service.BasicService;
import jadex.bridge.service.IInternalService;
import jadex.bridge.service.IService;
import jadex.bridge.service.IServiceIdentifier;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.bridge.service.component.IProvidedServicesFeature;
import jadex.bridge.service.component.ProvidedServicesComponentFeature;
import jadex.bridge.service.search.SServiceProvider;
import jadex.bridge.service.types.cms.IComponentManagementService;
import jadex.commons.MethodInfo;
import jadex.commons.collection.ILRUEntryCleaner;
import jadex.commons.collection.LRU;
import jadex.commons.future.CounterResultListener;
import jadex.commons.future.DefaultResultListener;
import jadex.commons.future.DelegationResultListener;
import jadex.commons.future.ExceptionDelegationResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.commons.future.IResultListener;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * 
 */
public class NFPropertyComponentFeature extends AbstractComponentFeature implements INFPropertyComponentFeature
{
	//-------- attributes --------
	
	/** The component property provider. */
	protected INFPropertyProvider compprovider;
	
	/** The nf property providers for required services. */
	protected Map<IServiceIdentifier, INFMixedPropertyProvider> proserprops;
	
	/** The nf property providers for required services. */
	protected Map<IServiceIdentifier, INFMixedPropertyProvider> reqserprops;
	
	/** The max number of preserved req service providers. */
	protected int maxreq;
	
	/** The parent provider. */
	protected INFPropertyProvider parent;
	
	//-------- constructors --------
	
	/**
	 *  Create the feature.
	 */
	public NFPropertyComponentFeature(IInternalAccess component, ComponentCreationInfo cinfo)
	{
		super(component, cinfo);
	}
	
	/**
	 *  Initialize the feature.
	 *  Empty implementation that can be overridden.
	 */
	public IFuture<Void>	init()
	{
//		System.out.println("init start: "+getComponent().getComponentIdentifier());
		
		final Future<Void> ret = new Future<Void>();
		
		int cnt = 0;
		LateCounterListener<Void> lis = new LateCounterListener<Void>(new DelegationResultListener<Void>(ret));
		
		// Init nf component props
		List<NFPropertyInfo> nfprops = getComponent().getModel().getNFProperties();
		if(nfprops!=null)
		{
			for(NFPropertyInfo nfprop: nfprops)
			{
				try
				{
					Class<?> clazz = nfprop.getClazz().getType(getComponent().getClassLoader(), getComponent().getModel().getAllImports());
					INFProperty<?, ?> nfp = AbstractNFProperty.createProperty(clazz, getComponent(), null, null);
					cnt++;
					getComponentPropertyProvider().addNFProperty(nfp).addResultListener(lis);
				}
				catch(Exception e)
				{
					getComponent().getLogger().warning("Property creation problem: "+e);
				}
			}
		}
		
		IProvidedServicesFeature psf = getComponent().getComponentFeature(IProvidedServicesFeature.class);
		if(psf!=null)
		{
			Map<Class<?>, Collection<IInternalService>> sers = ((ProvidedServicesComponentFeature)psf).getServices();
			if(sers!=null)
			{
				for(Class<?> type: sers.keySet())
				{
					for(IInternalService ser: sers.get(type))
					{
						cnt++;
						initNFProperties(ser, psf.getProvidedServiceRawImpl(ser.getServiceIdentifier()).getClass()).addResultListener(lis);
					}
				}
			}
		}
		
		lis.setMax(cnt);
		
//		ret.addResultListener(new IResultListener<Void>()
//		{
//			public void resultAvailable(Void result)
//			{
//				System.out.println("init end: "+getComponent().getComponentIdentifier());
//			}
//			
//			public void exceptionOccurred(Exception exception)
//			{
//				System.out.println("init end ex: "+getComponent().getComponentIdentifier());
//			}
//		});
		
		return ret;
	}
	
	/**
	 *  Get the component property provider.
	 */
	public INFPropertyProvider getComponentPropertyProvider()
	{
		if(compprovider==null)
		{
			this.compprovider = new NFPropertyProvider() 
			{
				public IInternalAccess getInternalAccess() 
				{
					return getComponent();
				}
				
				public IFuture<INFPropertyProvider> getParent()
				{
					final Future<INFPropertyProvider> ret = new Future<INFPropertyProvider>();
					final IComponentIdentifier pacid = getComponent().getComponentIdentifier().getParent();

					if(parent!=null)
					{
						ret.setResult(parent);
					}
					else if(pacid!=null)
					{
						IComponentManagementService cms = SServiceProvider.getLocalService(getComponent(), IComponentManagementService.class, RequiredServiceInfo.SCOPE_PLATFORM);
						cms.getExternalAccess(pacid).addResultListener(new ExceptionDelegationResultListener<IExternalAccess, INFPropertyProvider>(ret) 
						{
							public void customResultAvailable(IExternalAccess exta) 
							{
								exta.scheduleStep(new IComponentStep<INFPropertyProvider>() 
								{
									public IFuture<INFPropertyProvider> execute(IInternalAccess ia) 
									{
										INFPropertyComponentFeature nff = ia.getComponentFeature(INFPropertyComponentFeature.class);
										return new Future<INFPropertyProvider>(nff.getComponentPropertyProvider());
									}
								}).addResultListener(new DelegationResultListener<INFPropertyProvider>(ret)); 
							}
						});
					}
					return ret;
				}
			};
		}
		
		return compprovider;
	}
	
	/**
	 *  Get the required service property provider for a service.
	 */
	public INFMixedPropertyProvider getRequiredServicePropertyProvider(IServiceIdentifier sid)
	{
		INFMixedPropertyProvider ret = null;
		if(reqserprops==null)
		{
			reqserprops = new LRU<IServiceIdentifier, INFMixedPropertyProvider>(maxreq, new ILRUEntryCleaner<IServiceIdentifier, INFMixedPropertyProvider>()
			{
				public void cleanupEldestEntry(Entry<IServiceIdentifier, INFMixedPropertyProvider> eldest)
				{
					eldest.getValue().shutdownNFPropertyProvider().addResultListener(new DefaultResultListener<Void>()
					{
						public void resultAvailable(Void result)
						{
						}
					});
				}
			}); 
		}
		ret = reqserprops.get(sid);
		if(ret==null)
		{
			ret = new NFMethodPropertyProvider() 
			{
				public IInternalAccess getInternalAccess() 
				{
					return getComponent();
				}
				
				// parent of required service property?
				public IFuture<INFPropertyProvider> getParent()
				{
					return new Future<INFPropertyProvider>((INFMixedPropertyProvider)null);
				}
			}; 
			reqserprops.put(sid, ret);
		}
		return ret;
	}
	
	/**
	 *  Has the service a property provider.
	 */
	public boolean hasRequiredServicePropertyProvider(IServiceIdentifier sid)
	{
		return reqserprops!=null? reqserprops.get(sid)!=null: false;
	}
	
	/**
	 *  Get the provided service property provider for a service.
	 */
	public INFMixedPropertyProvider getProvidedServicePropertyProvider(IServiceIdentifier sid)
	{
		INFMixedPropertyProvider ret = null;
		if(proserprops==null)
		{
			proserprops = new HashMap<IServiceIdentifier, INFMixedPropertyProvider>();
		}
		ret = proserprops.get(sid);
		if(ret==null)
		{
			ret = new NFMethodPropertyProvider() 
			{
				public IInternalAccess getInternalAccess() 
				{
					return getComponent();
				}
				
				public IFuture<INFPropertyProvider> getParent()
				{
					return new Future<INFPropertyProvider>(compprovider);
				}
			}; 
			proserprops.put(sid, ret);
		}
		return ret;
	}
	
//	/**
//	 *  Get the provided service property provider for a service.
//	 */
//	public INFMixedPropertyProvider getProvidedServicePropertyProvider(Class<?> iface)
//	{
//	}
	
	/**
	 * 
	 */
	public IFuture<Void> initNFProperties(final IInternalService ser, Class<?> impltype)
	{
		final Future<Void> ret = new Future<Void>();
		
		List<Class<?>> classes = new ArrayList<Class<?>>();
		Class<?> superclazz = ser.getServiceIdentifier().getServiceType().getType(getComponent().getClassLoader());
		while(superclazz != null && !Object.class.equals(superclazz))
		{
			classes.add(superclazz);
			superclazz = superclazz.getSuperclass();
		}
						
		superclazz = impltype;
		while(superclazz != null && !BasicService.class.equals(superclazz) && !Object.class.equals(superclazz))
		{
			classes.add(superclazz);
			superclazz = superclazz.getSuperclass();
		}
//				Collections.reverse(classes);
		
		int cnt = 0;
		
		LateCounterListener<Void> lis = new LateCounterListener<Void>(new DelegationResultListener<Void>(ret));
		
		Map<MethodInfo, Method> meths = new HashMap<MethodInfo, Method>();
		for(Class<?> sclazz: classes)
		{
			if(sclazz.isAnnotationPresent(NFProperties.class))
			{
				addNFProperties(sclazz.getAnnotation(NFProperties.class), null, null).addResultListener(lis);
				cnt++;
			}
			
			Method[] methods = sclazz.getMethods();
			for(Method m : methods)
			{
				if(m.isAnnotationPresent(NFProperties.class))
				{
					MethodInfo mis = new MethodInfo(m.getName(), m.getParameterTypes());
					if(!meths.containsKey(mis))
					{
						meths.put(mis, m);
					}
				}
			}
		}
		
		for(MethodInfo key: meths.keySet())
		{
			addNFProperties(meths.get(key).getAnnotation(NFProperties.class), ser, key).addResultListener(lis);
			cnt++;
		}
		
		// Set the number of issued calls
		lis.setMax(cnt);
		
		return ret;
	}
	
	/**
	 *  Add nf properties from a type.
	 */
	public IFuture<Void> addNFProperties(NFProperties nfprops, IService ser, MethodInfo mi)
	{
		Future<Void> ret = new Future<Void>();
		INFMixedPropertyProvider prov = getProvidedServicePropertyProvider(ser.getServiceIdentifier());
		
		CounterResultListener<Void> lis = new CounterResultListener<Void>(nfprops.value().length, new DelegationResultListener<Void>(ret));
		for(NFProperty nfprop : nfprops.value())
		{
			Class<?> clazz = nfprop.value();
			INFProperty<?, ?> prop = AbstractNFProperty.createProperty(clazz, getComponent(), ser, null);
			prov.addNFProperty(prop).addResultListener(lis);
		}
		
		return ret;
	}
	
	/**
	 *  Add nf properties from a type.
	 */
	public IFuture<Void> addNFMethodProperties(NFProperties nfprops, IService ser, MethodInfo mi)
	{
		Future<Void> ret = new Future<Void>();
		
		INFMixedPropertyProvider prov = getProvidedServicePropertyProvider(ser.getServiceIdentifier());
		CounterResultListener<Void> lis = new CounterResultListener<Void>(nfprops.value().length, new DelegationResultListener<Void>(ret));
		for(NFProperty nfprop : nfprops.value())
		{
			Class<?> clazz = ((NFProperty)nfprop).value();
			INFProperty<?, ?> prop = AbstractNFProperty.createProperty(clazz, getComponent(), ser, mi);
			prov.addMethodNFProperty(mi, prop).addResultListener(lis);
		}
		
		return ret;
	}
	
	/**
	 *  Get external feature facade.
	 */
	public <T> T getExternalFacade(Object context)
	{
		T ret = null;
		if(context instanceof IService)
		{
//			IServiceIdentifier sid = (IServiceIdentifier)context;
			ret = (T)getProvidedServicePropertyProvider(((IService)context).getServiceIdentifier());
		}
		else 
		{
			ret = (T)getComponentPropertyProvider();
		}
		
		return ret;
	}
	
//	/**
//	 * 
//	 */
//	public <T> Class<T> getExternalFacadeType(Object context)
//	{
//		Class<T> ret = (Class<T>)INFPropertyComponentFeature.class;
//		if(context instanceof IService)
//		{
//			ret = (Class<T>)INFMixedPropertyProvider.class;
//		}
//		return ret;
//	}
	
	/**
	 *  Counter listener that allows to set the max after usage.
	 */
	public static class LateCounterListener<T> implements IResultListener<T>
	{
		IResultListener<T> delegate;
		int max = -1;
		int cnt = 0;
		
		public LateCounterListener(IResultListener<T> delegate)
		{
			this.delegate = delegate;
		}
		
		public void resultAvailable(T result)
		{
			cnt++;
			check();
		}
		
		public void exceptionOccurred(Exception exception)
		{
			cnt++;
			check();
		}
		
		protected void check()
		{
			if(max>-1 && max==cnt)
			{
				delegate.resultAvailable(null);
			}
		}
		
		public void setMax(int max)
		{
			this.max = max;
			check();
		}
	};
}
