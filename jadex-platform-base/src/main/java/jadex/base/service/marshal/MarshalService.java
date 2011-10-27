package jadex.base.service.marshal;

import jadex.bridge.ComponentIdentifier;
import jadex.bridge.IComponentIdentifier;
import jadex.bridge.IExternalAccess;
import jadex.bridge.service.BasicService;
import jadex.bridge.service.annotation.Excluded;
import jadex.bridge.service.annotation.Reference;
import jadex.bridge.service.annotation.Service;
import jadex.bridge.service.annotation.ServiceShutdown;
import jadex.bridge.service.annotation.ServiceStart;
import jadex.bridge.service.component.BasicServiceInvocationHandler;
import jadex.bridge.service.types.marshal.IMarshalService;
import jadex.commons.IChangeListener;
import jadex.commons.ICloneProcessor;
import jadex.commons.IRemotable;
import jadex.commons.IRemoteChangeListener;
import jadex.commons.future.IFuture;
import jadex.commons.future.IIntermediateFuture;
import jadex.commons.future.IIntermediateResultListener;
import jadex.commons.future.IResultListener;

import java.lang.reflect.Proxy;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *  Marshal service implementation.
 */
public class MarshalService extends BasicService implements IMarshalService
{
	//-------- attributes --------
	
	/** The component. */
	protected IExternalAccess	access;
	
	/** The clone processors. */
	protected List<ICloneProcessor> processors;
	
	/** The reference class cache (clazz->boolean (is reference)). */
	protected Map<Class, boolean[]> references;
	
	//-------- constructors --------
	
	/**
	 *  Create marshal service.
	 */
	public MarshalService(IExternalAccess access)
	{
		super(access, IMarshalService.class, null);
		this.access = access;
	}
	
	//-------- methods --------
		
	/**
	 *  Start the service.
	 *  @return A future that is done when the service has completed starting.  
	 */
	@ServiceStart
	public IFuture<Void>	startService()
	{
//		references = Collections.synchronizedMap(new LRU(500));
		references = Collections.synchronizedMap(new HashMap());
		processors = Collections.synchronizedList(new ArrayList<ICloneProcessor>());
		
		boolean[] tt = new boolean[]{true, true};
		references.put(IRemotable.class, tt);
		references.put(IResultListener.class, tt);
		references.put(IIntermediateResultListener.class, tt);
		references.put(IFuture.class, tt);
		references.put(IIntermediateFuture.class, tt);
		references.put(IChangeListener.class, tt);
		references.put(IRemoteChangeListener.class, tt);
		
		boolean[] tf = new boolean[]{true, false};
		references.put(URL.class, tf);
		references.put(InetAddress.class, tf);
		references.put(Inet4Address.class, tf);
		references.put(Inet6Address.class, tf);
		references.put(IComponentIdentifier.class, tf);
		references.put(ComponentIdentifier.class, tf);
		
		// Problem: if micro agent implements a service it cannot
		// be determined if the service or the agent should be transferred.
		// Per default a service is assumed.
		processors.add(new ICloneProcessor()
		{
			public Object process(Object object, List processors)
			{
				return BasicServiceInvocationHandler.getPojoServiceProxy(object);
			}
			
			public boolean isApplicable(Object object)
			{
				return object!=null && !(object instanceof BasicService) 
					&& object.getClass().isAnnotationPresent(Service.class);
			}
		});
		
		return IFuture.DONE;
	}
		
	/**
	 *  Shutdown the service.
	 *  @return A future that is done when the service has completed its shutdown.  
	 */
	@ServiceShutdown
	public IFuture<Void>	shutdownService()
	{
		return IFuture.DONE;
	}
	
	//-------- class reference management --------

	/**
	 *  Test if an object has reference semantics. It is a reference when:
	 *  - it implements IRemotable
	 *  - it is an IService, IExternalAccess or IFuture
	 *  - if the object has used an @Reference annotation at type level
	 *  - has been explicitly set to be reference
	 */
	public boolean isLocalReference(Object object)
	{
		return isReference(object, true);
	}
	
	/**
	 *  Test if an object has reference semantics. It is a reference when:
	 *  - it implements IRemotable
	 *  - it is an IService, IExternalAccess or IFuture
	 *  - if the object has used an @Reference annotation at type level
	 *  - has been explicitly set to be reference
	 */
	public boolean isRemoteReference(Object object)
	{
		return isReference(object, false);
	}
	
	/**
	 *  Register a class with reference values for local and remote.
	 */
	public void setReferenceProperties(Class clazz, boolean localref, boolean remoteref)
	{
		references.put(clazz, new boolean[]{localref, remoteref});
	}
	
	/**
	 *  Test if an object is a remote object.
	 */
	@Excluded
	public boolean isRemoteObject(Object target)
	{
		boolean ret = false;
		if(Proxy.isProxyClass(target.getClass()))
		{
			Object handler = Proxy.getInvocationHandler(target);
			if(handler instanceof BasicServiceInvocationHandler)
			{
				BasicServiceInvocationHandler bsh = (BasicServiceInvocationHandler)handler;
				// Hack! Needed for dynamically bound delegation services of composites (virtual)
				ret = bsh.getDomainService()==null;
				if(!ret)
					return isRemoteObject(bsh.getDomainService());
			}
			else 
			{
				// todo: remove string based remote check! RemoteMethodInvocationHandler is in package jadex.base.service.remote
				ret = Proxy.getInvocationHandler(target).getClass().getName().indexOf("Remote")!=-1;
			}
		}
		return ret;
//		Object target = getObject();
//		if(Proxy.isProxyClass(target.getClass()))
//			System.out.println("blubb "+Proxy.getInvocationHandler(target).getClass().getName());
//		return Proxy.isProxyClass(target.getClass()) && Proxy.getInvocationHandler(target).getClass().getName().indexOf("Remote")!=-1;

	}
	
	//-------- local clone processors --------
	
	/**
	 *  Get the clone processors.
	 */
	public List<ICloneProcessor> getCloneProcessors()
	{
		return processors;
	}
	
	/**
	 *  Add a clone processor.
	 */
	public void addCloneProcessor(@Reference ICloneProcessor proc)
	{
		this.processors.add(proc);
	}
		
	/**
	 *  Remove a clone processor.
	 */
	public void removeCloneProcessor(@Reference ICloneProcessor proc)
	{
		this.processors.remove(proc);
	}

	//-------- remote clone processors --------

//	/**
//	 *  Add a rmi preprocessor.
//	 */
//	public IFuture<Void> addRMIPreProcessor(@Reference IRMIPreprocessor proc);
//		
//	/**
//	 *  Remove a rmi postprocessor.
//	 */
//	public IFuture<Void> removeRMIPreProcessor(@Reference IRMIPreprocessor proc);
//	
//	/**
//	 *  Add a rmi postprocessor.
//	 */
//	public IFuture<Void> addRMIPostProcessor(@Reference IRMIPostprocessor proc);
//		
//	/**
//	 *  Remove a rmi postprocessor.
//	 */
//	public IFuture<Void> removeRMIPostProcessor(@Reference IRMIPostprocessor proc);
//	
//	/**
//	 *  Get the rmi preprocessors.
//	 */
//	public IIntermediateFuture<IRMIPreProcessor> getRMIPreProcessors();
//	
//	/**
//	 *  Get the rmi postprocessors.
//	 */
//	public IIntermediateFuture<IRMIPostProcessor> getRMIPostProcessors();

	/**
	 *  Test if an object has reference semantics. It is a reference when:
	 *  - it implements IRemotable
	 *  - it is an IService, IExternalAccess or IFuture, IIntermediateFuture, 
	 *  	IResultListener, IIntermediateResultListener, IChangeListener, IRemoteChangeListener
	 *  - if the object has used an @Reference annotation at type level
	 */
	public boolean isReference(Object object, boolean local)
	{
		boolean ret = false;
//		boolean ret = object instanceof IRemotable 
//			|| object instanceof IResultListener || object instanceof IIntermediateResultListener
//			|| object instanceof IFuture || object instanceof IIntermediateFuture
//			|| object instanceof IChangeListener || object instanceof IRemoteChangeListener;
////			|| object instanceof IService;// || object instanceof IExternalAccess;
		
		if(!ret && object!=null)
		{
			boolean localret = ret;
			boolean remoteret = ret;
			
			Class cl = object.getClass();
			List todo = new ArrayList();
			todo.add(cl);
			
			boolean[] isref = null;
			while(todo.size()>0 && isref==null)
			{
				Class clazz = (Class)todo.remove(0);
				isref = (boolean[])references.get(clazz);
				if(isref!=null)
				{
					localret = isref[0];
					remoteret = isref[1];
					break;
				}
				else
				{
					Reference ref = (Reference)clazz.getAnnotation(Reference.class);
					if(ref!=null)
					{
						localret = ref.local();
						remoteret = ref.remote();
						break;
					}
					else
					{
						Class superclazz = clazz.getSuperclass();
						if(superclazz!=null && !superclazz.equals(Object.class))
							todo.add(superclazz);
						Class[] interfaces = clazz.getInterfaces();
						for(int i=0; i<interfaces.length; i++)
						{
							todo.add(interfaces[i]);
						}
					}
				}
			}
			references.put(cl, new boolean[]{localret, remoteret});
			ret = local? localret: remoteret;
//			System.out.println("refsize: "+references.size());
		}
		
//		System.out.println("object ref? "+ret+" "+object.getClass()+" "+object);
		
//		boolean test = object instanceof IRemotable 
//			|| object instanceof IResultListener || object instanceof IIntermediateResultListener
//			|| object instanceof IFuture || object instanceof IIntermediateFuture
//			|| object instanceof IChangeListener || object instanceof IRemoteChangeListener;
//		|| object instanceof IService;// || object instanceof IExternalAccess;
		
//		if(ret==false && test!=ret)
//			System.out.println("wrong reference semantics");
		
		return ret;
	}
}
