package jadex.bridge.service;

import jadex.bridge.IComponentIdentifier;
import jadex.bridge.IResourceIdentifier;
import jadex.bridge.service.annotation.GuiClass;
import jadex.bridge.service.annotation.GuiClassName;
import jadex.bridge.service.component.BasicServiceInvocationHandler;
import jadex.bridge.service.search.SServiceProvider;
import jadex.bridge.service.types.publish.IPublishService;
import jadex.commons.SReflect;
import jadex.commons.future.DelegationResultListener;
import jadex.commons.future.ExceptionDelegationResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 *  Basic service provide a simple default isValid() implementation
 *  that returns true after start service and false afterwards.
 */
public class BasicService implements IInternalService
{	
	//-------- attributes --------

	/** The id counter. */
	protected static long idcnt;
	
	/** The started state. */
	protected boolean started;
	
	/** The shutdowned state. */
	protected boolean shutdowned;
	
	/** The service id. */
	protected IServiceIdentifier sid;
	
	/** The service properties. */
	protected Map properties;
	
	/** The publish info. */
//	protected PublishInfo publishinfo;
	
	/** The provider id. */
	protected IComponentIdentifier providerid;
	
	/** The type. */
	protected Class type;
	
	//-------- constructors --------

	/**
	 *  Create a new service.
	 */
	public BasicService(IComponentIdentifier providerid, Class type, Map properties)
	{
//		if(!SReflect.isSupertype(type, getClass()))
//			throw new RuntimeException("Service must implement provided interface: "+getClass().getName()+", "+type.getName());
		this.providerid = providerid;
		this.type = type;
//		this.implclazz = implclazz;
		this.properties	= properties;
		
		// todo: move to be able to use the constant
		// jadex.base.gui.componentviewer.IAbstractViewerPanel.PROPERTY_VIEWERCLASS
		Object guiclazz = properties!=null? properties.get("componentviewer.viewerclass"): null;
		
		if(guiclazz==null && type.isAnnotationPresent(GuiClass.class))
		{
			GuiClass gui = (GuiClass)type.getAnnotation(GuiClass.class);
			guiclazz = gui.value();
			if(this.properties==null)
				this.properties = new HashMap();
			this.properties.put("componentviewer.viewerclass", guiclazz);
//			System.out.println("found: "+guiclazz);
		}
		else if(guiclazz==null && type.isAnnotationPresent(GuiClassName.class))
		{
			GuiClassName gui = (GuiClassName)type.getAnnotation(GuiClassName.class);
			guiclazz = gui.value();
			if(this.properties==null)
				this.properties = new HashMap();
			this.properties.put("componentviewer.viewerclass", guiclazz);
//			System.out.println("found: "+guiclazz);
		}
	}
	
	//-------- methods --------
	
	/**
	 *  Test if the service is valid.
	 *  @return True, if service can be used.
	 *  
	 *  todo: why is method synchronized?
	 */
	public synchronized IFuture<Boolean> isValid()
	{
//		if(getServiceIdentifier().getServiceName().indexOf("Add")!=-1)
//			System.out.println("isValid: "+getServiceIdentifier()+": "+(started && !shutdowned));
		return new Future<Boolean>(started && !shutdowned? Boolean.TRUE: Boolean.FALSE);
	}
	
	/**
	 *  Set the service identifier.
	 */
	public void createServiceIdentifier(String name, Class implclazz, IResourceIdentifier rid)
	{
		this.sid = createServiceIdentifier(providerid, name, type, implclazz, rid);
	}
	
	/**
	 *  Get the service id.
	 *  @return The service id.
	 */
	public IServiceIdentifier getServiceIdentifier()
	{
		if(sid==null)
			throw new RuntimeException("No service identifier: "+this);
//			sid = createServiceIdentifier(providerid, name, type, implclazz==null ? getClass() : implclazz);
		return sid;
	}
	
	/**
	 *  Get a service property.
	 *  @return The service property (if any).
	 * /
	public Object getProperty(String name)
	{
		return properties!=null ? properties.get(name) : null; 
	}*/
	
	/**
	 *  Get the providerid.
	 *  @return the providerid.
	 */
	public IComponentIdentifier getProviderId()
	{
		return providerid;
	}

	/**
	 *  Get a service property.
	 *  @return The service property (if any).
	 */
	public Map getPropertyMap()
	{
		return properties!=null? properties: Collections.EMPTY_MAP; 
	}
	
	/**
	 *  Set the properties.
	 *  @param properties The properties to set.
	 */
	public void setPropertyMap(Map properties)
	{
		this.properties = properties;
	}
	
//	/**
//	 *  Get the publish info.
//	 *  @return The publish info.
//	 */
//	public PublishInfo getPublishInfo()
//	{
//		return publishinfo;
//	}
//	
//	/**
//	 *  Set the publishinfo.
//	 *  @param publishinfo The publishinfo to set.
//	 */
//	public void setPublishInfo(PublishInfo publishinfo)
//	{
//		this.publishinfo = publishinfo;
//	}

	/**
	 *  Start the service.
	 *  @return A future that is done when the service has completed starting.  
	 */
	public IFuture<Void>	startService()
	{
//		System.out.println("start: "+this);
		Future<Void> ret = new Future<Void>();
		
		boolean ex = false;
		synchronized(this)
		{
			if(started)
			{
				ex = true;
			}
			else
			{
				started = true;
			}
		}
		
		if(ex)
		{
			ret.setException(new RuntimeException("Already running."));
		}
		else 
		{
			ret.setResult(null);
//			ret.setResult(getServiceIdentifier());
		}
		
		return ret;
	}
	
	/**
	 *  Shutdown the service.
	 *  @return A future that is done when the service has completed its shutdown.  
	 */
	public IFuture<Void>	shutdownService()
	{
		// Deregister pojo->sid mapping in shutdown.
		BasicServiceInvocationHandler.removePojoServiceProxy(sid);
		
//		System.out.println("shutdown service: "+getServiceIdentifier());
		
		final Future<Void> ret = new Future<Void>();
		isValid().addResultListener(new ExceptionDelegationResultListener<Boolean, Void>(ret)
		{
			public void customResultAvailable(Boolean result)
			{
				if(!result.booleanValue())
				{
					ret.setException(new RuntimeException("Not running."));
				}
				else
				{
					shutdowned = true;
					ret.setResult(null);
//					System.out.println("shutdowned service: "+getServiceIdentifier());
				}
			}
		});
		return ret;
	}
	
	/**
	 *  Generate a unique name.
	 *  @param The calling service class.
	 */
	public static String generateServiceName(Class service)
	{
		synchronized(BasicService.class)
		{
			return SReflect.getInnerClassName(service)+"_#"+idcnt++;
		}
	}
	
	/**
	 *  Create a new service identifier.
	 *  @param providerid The provider id.
	 *  @param servicename The service name.
	 *  @return A service identifier.
	 */
	public static IServiceIdentifier createServiceIdentifier(IComponentIdentifier providerid, String servicename, 
		Class servicetype, Class serviceimpl, IResourceIdentifier rid)
	{
		return new ServiceIdentifier(providerid, servicetype, servicename!=null? servicename: generateServiceName(serviceimpl), rid);
	}
	
	/**
	 *  Check if the service is valid.
	 * /
	public IFuture checkValid()
	{
		Future ret = new Future();
		if(!isValid())
			ret.setException(new RuntimeException("Service invalid: "+getServiceIdentifier()));
		else
			ret.setResult(null);
		return ret;
	}*/
	
	/**
	 *  Check if the service is equal. The service is considered equal if the service identifiers match.
	 *  
	 *  @param obj Object of comparison.
	 *  @return True, if the object is a service with a matching service identifier.
	 */
	public boolean equals(Object obj)
	{
		if (obj instanceof IService)
		{
			return getServiceIdentifier().equals(((IService) obj).getServiceIdentifier());
		}
		return false;
	}
}
