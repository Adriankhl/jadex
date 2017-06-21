package jadex.platform.service.remote;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import jadex.base.Starter;
import jadex.bridge.ClassInfo;
import jadex.bridge.ComponentIdentifier;
import jadex.bridge.ComponentTerminatedException;
import jadex.bridge.IComponentIdentifier;
import jadex.bridge.IComponentStep;
import jadex.bridge.IExternalAccess;
import jadex.bridge.IInputConnection;
import jadex.bridge.IInternalAccess;
import jadex.bridge.IOutputConnection;
import jadex.bridge.IResourceIdentifier;
import jadex.bridge.ResourceIdentifier;
import jadex.bridge.component.IExecutionFeature;
import jadex.bridge.fipa.SFipa;
import jadex.bridge.service.BasicService;
import jadex.bridge.service.IService;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.bridge.service.annotation.Service;
import jadex.bridge.service.component.BasicServiceInvocationHandler;
import jadex.bridge.service.component.IRequiredServicesFeature;
import jadex.bridge.service.search.SServiceProvider;
import jadex.bridge.service.search.ServiceNotFoundException;
import jadex.bridge.service.types.address.TransportAddressBook;
import jadex.bridge.service.types.cms.IComponentDescription;
import jadex.bridge.service.types.cms.IComponentManagementService;
import jadex.bridge.service.types.library.ILibraryService;
import jadex.bridge.service.types.marshal.IMarshalService;
import jadex.bridge.service.types.message.IMessageService;
import jadex.bridge.service.types.remote.IRemoteServiceManagementService;
import jadex.bridge.service.types.remote.ServiceInputConnectionProxy;
import jadex.bridge.service.types.remote.ServiceOutputConnectionProxy;
import jadex.commons.IAsyncFilter;
import jadex.commons.SUtil;
import jadex.commons.collection.LRU;
import jadex.commons.concurrent.TimeoutException;
import jadex.commons.future.DelegationResultListener;
import jadex.commons.future.ExceptionDelegationResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.commons.future.IFutureCommandResultListener;
import jadex.commons.future.IIntermediateFutureCommandResultListener;
import jadex.commons.future.ISubscriptionIntermediateFuture;
import jadex.commons.future.ITerminableFuture;
import jadex.commons.future.IntermediateFuture;
import jadex.commons.future.TerminableIntermediateDelegationFuture;
import jadex.commons.transformation.annotations.Classname;
import jadex.commons.transformation.traverser.IRootObjectContext;
import jadex.commons.transformation.traverser.ITraverseProcessor;
import jadex.commons.transformation.traverser.Traverser;
import jadex.commons.transformation.traverser.Traverser.MODE;
import jadex.platform.service.message.MessageService;
import jadex.platform.service.message.streams.InputConnection;
import jadex.platform.service.message.streams.OutputConnection;
import jadex.platform.service.remote.commands.AbstractRemoteCommand;
import jadex.platform.service.remote.commands.RemoteFutureTerminationCommand;
import jadex.platform.service.remote.commands.RemoteGetExternalAccessCommand;
import jadex.platform.service.remote.commands.RemoteSearchCommand;
import jadex.xml.reader.AReader;
import jadex.xml.writer.AWriter;


/**
 *  The remote service management service is responsible for 
 *  handling remote service invocations (similar to RMI).
 */
public class RemoteServiceManagementService extends BasicService implements IRemoteServiceManagementService
{
	//-------- constants --------
	
	/** Excluded remote methods (for all methods)
	 *	Excluded methods throw UnsupportedOperationException. */
	public static final String REMOTE_EXCLUDED = "remote_excluded";
	
	/** Uncached remote methods (for methods with no parameters)
	 *	Uncached methods will be invoked on every call. */
	public static final String REMOTE_UNCACHED = "remote_uncached";
	
	/** Synchronous remote methods (for methods with void return value). 
     *	If void methods are declared synchronous they will block the caller until
     *	the method has been executed on the remote side (exception thus can arrive). */
	public static final String REMOTE_SYNCHRONOUS = "remote_synchronous";

	/** Replacement methods to be executed instead of remote method invocation. */
	public static final String REMOTE_METHODREPLACEMENT = "remote_methodreplacement";

	/** Timeout for remote method invocation. */
	public static final String REMOTE_TIMEOUT = "remote_timeout";
	
	//-------- attributes --------
	
	/** The component. */
	protected IExternalAccess component;
	
	/** The map of waiting calls (callid -> future). */
	protected Map<String, WaitingCallInfo> waitingcalls;
	
	/** The map of processing calls (callid -> terniable future). */
	protected Map<String, Object> processingcalls;
	
	/** The map of termination commands without futures (callid -> command). 
	    This can happen whenever a remote invocation command is executed after the terminate arrives. */
	protected LRU<String, List<Runnable>> terminationcommands;

	/** The remote reference module. */
	protected RemoteReferenceModule rrm;
	
	/** The rmi object to xml writer. */
	protected AWriter writer;
	
	/** The rmi xml to object reader. */
	protected AReader reader;
	
	/** The timer. */
	protected Timer	timer;
	
	/** The marshal service. */
	protected IMarshalService marshal;
	
	/** The message service. */
	protected IMessageService msgservice;
	
	/** The rms identifier. */
//	protected ITransportComponentIdentifier rms;
	
	/** The transport addresses. */
	protected TransportAddressBook addresses;
	
	/** The local platform. */
	protected String platform;
	
	//-------- constructors --------
	
	/**
	 *  Create a new remote service management service.
	 */
	public RemoteServiceManagementService(IExternalAccess component, 
		ILibraryService libservice, final IMarshalService marshal, final IMessageService msgservice, TransportAddressBook addresses)//, boolean binarymode)
	{
		super(component.getComponentIdentifier(), IRemoteServiceManagementService.class, null);

//		System.out.println("binary: "+binarymode);
		
		this.component = component;
		this.rrm = new RemoteReferenceModule(this, libservice, marshal);
		this.waitingcalls = new HashMap<String, WaitingCallInfo>();
		this.processingcalls = new HashMap<String, Object>();
		this.terminationcommands = new LRU<String, List<Runnable>>(1000);
		this.timer	= new Timer(true);
		this.marshal = marshal;
		this.msgservice = msgservice;
		this.addresses = addresses;
	}
	
//	/**
//	 *  Get service proxies from a remote platform.
//	 *  (called from arbitrary components)
//	 *  @param cid Component id that is used to start the search.
//	 *  @param manager The search manager.
//	 *  @param decider The visit decider.
//	 *  @param selector The result selector.
//	 *  @return Collection or single result (i.e. service proxies). 
//	 */
//	public ITerminableIntermediateFuture<IService> getServiceProxies(final IComponentIdentifier cid, 
//		final ISearchManager manager, final IVisitDecider decider, final IResultSelector selector)
//	{
//		final IComponentIdentifier rrms = new ComponentIdentifier("rms@"+cid.getPlatformName(), cid.getAddresses());
//		final String callid = SUtil.createUniqueId(component.getComponentIdentifier().getName()+".0.getServiceProxies");
//		
//		final TerminableIntermediateDelegationFuture<IService> future = new TerminableIntermediateDelegationFuture<IService>()
//		{
//			public void terminate(final Exception e) 
//			{
//				component.scheduleStep(new IComponentStep<Void>()
//				{
//					// Sends termination command if aborted
//					@Classname("getServiceProxiesTerminate")
//					public IFuture<Void> execute(IInternalAccess ia)
//					{
//						// Set exception for local state (as rms removes waiting call, cannot receive remote result any more)
//						boolean	set	= setExceptionIfUndone(e);
//						
//						// Send message to announce termination to remote
//						if(set)
//						{
//							Future res = new Future();
//		//					res.addResultListener(new IResultListener()
//		//					{
//		//						public void resultAvailable(Object result)
//		//						{
//		//							System.out.println("received result: "+result);
//		//						}
//		//						public void exceptionOccurred(Exception exception)
//		//						{
//		//							System.out.println("received exception: "+exception);
//		//						}
//		//					});
//							final String mycallid = SUtil.createUniqueId(component.getComponentIdentifier().getName()+".ret.getServiceProxies");
//							RemoteFutureTerminationCommand content = new RemoteFutureTerminationCommand(mycallid, callid, e);
//							// Can be invoked directly, because internally redirects to agent thread.
//		//					System.out.println("sending terminate");
//							sendMessage(rrms, null, content, mycallid,  BasicService.getRemoteDefaultTimeout(), res, null, null);
//						}
//						return IFuture.DONE;
//					}
//				});
//			}
//			
//			public void sendBackwardCommand(Object info)
//			{
//				Future<Object> res = new Future<Object>();
//				final String mycallid = SUtil.createUniqueId(component.getComponentIdentifier().getName()+".ret.getServiceProxies");
//				RemoteFutureBackwardCommand content = new RemoteFutureBackwardCommand(mycallid, callid, info);
////				System.out.println("sending backward cmd");
//				sendMessage(rrms, null, content, mycallid, BasicService.getRemoteDefaultTimeout(), res, null, null);
//			}
//			
//			// Called from delegation listeners in RMS -> ignore if already terminated
//			public void setException(Exception exception)
//			{
//				super.setExceptionIfUndone(exception);
//			}
//		};
//		
////		future.addResultListener(new IIntermediateResultListener<IService>()
////		{
////			public void intermediateResultAvailable(IService result)
////			{
////				System.out.println("inm res: "+result+" "+callid);
////			}
////			public void finished()
////			{
////				System.out.println("fini"+" "+callid);
////			}
////			public void resultAvailable(Collection<IService> result)
////			{
////				System.out.println("res: "+result+" "+callid);
////			}
////			public void exceptionOccurred(Exception exception)
////			{
////				System.out.println("ex: "+exception+" "+callid);
////			}
////		});
//		
////		System.out.println("gsp: "+cid);
//		component.scheduleStep(new IComponentStep<Object>()
//		{
//			@Classname("getServiceProxies")
//			public IFuture<Object> execute(IInternalAccess ia)
//			{
//				final Future fut = future;
////				ia.getServiceContainer().searchService(IComponentManagementService.class, RequiredServiceInfo.SCOPE_PLATFORM)
////					.addResultListener(new ExceptionDelegationResultListener<IComponentManagementService, Object>(fut)
////				{
////					public void customResultAvailable(IComponentManagementService cms)
////					{
//						// Hack! create remote rms cid with "rms" assumption.
//						RemoteSearchCommand content = new RemoteSearchCommand(cid, manager, 
//							decider, selector, callid);
//						
////						System.out.println("send to: "+rrms+" "+callid);
//						sendMessage(rrms, cid, content, callid, BasicService.getRemoteDefaultTimeout(), fut, null, null); // todo: non-func
////					}
////				});
//				
////				return fut;
//				return new Future<Object>(null);
//			}
//		});
//		
//		return future;
//	}
	
	
	
//	/**
//	 *  Get a service proxy from a remote component.
//	 *  (called from arbitrary components)
//	 *  @param cid	The remote provider id.
//	 *  @param service	The service type.
//	 *  @param scope	The search scope. 
//	 *  @return The service proxy.
//	 */
//	public <T> IFuture<T> getServiceProxy(final IComponentIdentifier cid, final Class<T> service, String scope)
//	{
////		System.out.println("getServiceProxy start: "+cid+" "+service.getName());
//		final Future<T>	ret	= new Future<T>();
//		getServiceProxies(cid, SServiceProvider.getSearchManager(false, scope), SServiceProvider.getVisitDecider(true, scope), 
//			new TypeResultSelector(service, true)).addResultListener(new ExceptionDelegationResultListener<Collection<IService>, T>(ret)
//		{
//			public void customResultAvailable(Collection<IService> result)
//			{
////				System.out.println("getServiceProxy end: "+cid+" "+service.getName());
//				if(result!=null && !((Collection<?>)result).isEmpty())
//				{
//					Object	o	= ((Collection<?>)result).iterator().next();
//					ret.setResult((T)o);
//				}
//				else
//				{
//					super.exceptionOccurred(new ServiceNotFoundException("No proxy for service found: "+cid+", "+service.getName()));
//				}
//			}
//			
//			public void exceptionOccurred(Exception exception)
//			{
////				System.out.println("getServiceProxy end ex: "+cid+" "+service.getName());
//				super.exceptionOccurred(exception);
//			}
//		});
//		return ret;
//	}
	
	/**
	 *  Get a service proxy from a remote component.
	 *  (called from arbitrary components)
	 *  @param caller	The component that started the search.
	 *  @param cid	The remote provider id.
	 *  @param service	The service type.
	 *  @param scope	The search scope. 
	 *  @return The service proxy.
	 */
	public <T> IFuture<T> getServiceProxy(IComponentIdentifier caller, final IComponentIdentifier cid, final ClassInfo service, String scope, IAsyncFilter<T> filter)
	{
//		System.out.println("getServiceProxy start: "+cid+" "+service.getName());
		final Future<T>	ret	= new Future<T>();
		getServiceProxies(caller, cid, service, scope, false, filter).addResultListener(new ExceptionDelegationResultListener<Collection<T>, T>(ret)
		{
			public void customResultAvailable(Collection<T> result)
			{
//				System.out.println("getServiceProxy end: "+cid+" "+service.getName());
				if(result!=null && !result.isEmpty())
				{
					T o = result.iterator().next();
					ret.setResult(o);
				}
				else
				{
					super.exceptionOccurred(new ServiceNotFoundException("No proxy for service found: "+cid+", "+service.getTypeName()));
				}
			}
			
			public void exceptionOccurred(Exception exception)
			{
//				System.out.println("getServiceProxy end ex: "+cid+" "+service.getName());
				super.exceptionOccurred(exception);
			}
		});
		return ret;
	}
	
	/**
	 *  Get all service proxies from a remote component.
	 *  (called from arbitrary components)
	 *  @param caller	The component that started the search.
	 *  @param cid	The remote provider id.
	 *  @param service	The service type.
	 *  @param scope	The search scope. 
	 *  @return The service proxy.
	 */
	public <T> IFuture<Collection<T>> getServiceProxies(IComponentIdentifier caller, final IComponentIdentifier cid, final ClassInfo service, final String scope, IAsyncFilter<T> filter)
	{
		return getServiceProxies(caller, cid, service, scope, true, filter);
	}
	
	/**
	 *  Get all service proxies from a remote component.
	 *  (called from arbitrary components)
	 *  @param caller	The component that started the search.
	 *  @param cid	The remote provider id.
	 *  @param service	The service type.
	 *  @param scope	The search scope. 
	 *  @return The service proxy.
	 */
//	public <T> IFuture<Collection<T>> getServiceProxies(final IComponentIdentifier caller, final IComponentIdentifier cid, final ClassInfo service, final String scope, final boolean multiple, final IAsyncFilter<T> filter)
//	{
//		final Future<Collection<T>> ret = new Future<Collection<T>>();
//		
//		SServiceProvider.getService(component, ITransportAddressService.class, RequiredServiceInfo.SCOPE_PLATFORM)
//			.addResultListener(new ExceptionDelegationResultListener<ITransportAddressService, Collection<T>>(ret)
//		{
//			public void customResultAvailable(ITransportAddressService tas)
//			{
//				tas.getTransportComponentIdentifier(cid).addResultListener(new ExceptionDelegationResultListener<ITransportComponentIdentifier, Collection<T>>(ret)
//				{
//					public void customResultAvailable(ITransportComponentIdentifier tcid)
//					{
//						getServiceProxies(caller, tcid, service, scope, multiple, filter).addResultListener(new DelegationResultListener<Collection<T>>(ret));
//					}
//				});
//			}
//		});
//		
//		return ret;
//	}
	
	/**
	 *  Get all service proxies from a remote component.
	 *  (called from arbitrary components)
	 *  @param caller	The component that started the search.
	 *  @param cid	The remote provider id.
	 *  @param service	The service type.
	 *  @param scope	The search scope. 
	 *  @return The service proxy.
	 */
	public <T> IFuture<Collection<T>> getServiceProxies(final IComponentIdentifier caller, final IComponentIdentifier cid, final ClassInfo service, final String scope, final boolean multiple, final IAsyncFilter<T> filter)
	{
//		final Future<T>	ret	= new Future<T>();
//		
//		getServiceProxies(cid, SServiceProvider.getSearchManager(true, scope),
//			SServiceProvider.getVisitDecider(false, scope), new TypeResultSelector(service, true))
//			.addResultListener(new ExceptionDelegationResultListener<Collection<IService>, T>(ret)
//		{
//			public void customResultAvailable(Collection<IService> result)
//			{
//				ret.setResult((T)result);
//			}
//		});
//		
//		return ret;
		
		final ComponentIdentifier rrms = new ComponentIdentifier("rms@"+cid.getPlatformName());
		final String callid = SUtil.createUniqueId(component.getComponentIdentifier().getName()+".0.getServiceProxies");
		
		final TerminableIntermediateDelegationFuture<T> future = new TerminableIntermediateDelegationFuture<T>()
		{
			public void terminate(final Exception e) 
			{
				component.scheduleStep(new IComponentStep<Void>()
				{
					// Sends termination command if aborted
					@Classname("getServiceProxiesTerminate")
					public IFuture<Void> execute(IInternalAccess ia)
					{
						// Set exception for local state (as rms removes waiting call, cannot receive remote result any more)
						boolean	set	= setExceptionIfUndone(e);
						
						// Send message to announce termination to remote
						if(set)
						{
							Future res = new Future();
		//					res.addResultListener(new IResultListener()
		//					{
		//						public void resultAvailable(Object result)
		//						{
		//							System.out.println("received result: "+result);
		//						}
		//						public void exceptionOccurred(Exception exception)
		//						{
		//							System.out.println("received exception: "+exception);
		//						}
		//					});
							final String mycallid = SUtil.createUniqueId(component.getComponentIdentifier().getName()+".ret.getServiceProxies");
							RemoteFutureTerminationCommand content = new RemoteFutureTerminationCommand(mycallid, callid, e);
							// Can be invoked directly, because internally redirects to agent thread.
		//					System.out.println("sending terminate");
							sendMessage(rrms, null, content, mycallid,  Starter.getRemoteDefaultTimeout(getComponent().getComponentIdentifier()), res, null, null);
						}
						return IFuture.DONE;
					}
				});
			}
			
//			public void sendBackwardCommand(Object info)
//			{
//				Future<Object> res = new Future<Object>();
//				final String mycallid = SUtil.createUniqueId(component.getComponentIdentifier().getName()+".ret.getServiceProxies");
//				RemoteFutureBackwardCommand content = new RemoteFutureBackwardCommand(mycallid, callid, info);
////				System.out.println("sending backward cmd");
//				sendMessage(rrms, null, content, mycallid, Starter.getRemoteDefaultTimeout(getComponent().getComponentIdentifier()), res, null, null);
//			}
			
			// Called from delegation listeners in RMS -> ignore if already terminated
			public void setException(Exception exception)
			{
				super.setExceptionIfUndone(exception);
			}
		};
		
//		future.addResultListener(new IIntermediateResultListener<IService>()
//		{
//			public void intermediateResultAvailable(IService result)
//			{
//				System.out.println("inm res: "+result+" "+callid);
//			}
//			public void finished()
//			{
//				System.out.println("fini"+" "+callid);
//			}
//			public void resultAvailable(Collection<IService> result)
//			{
//				System.out.println("res: "+result+" "+callid);
//			}
//			public void exceptionOccurred(Exception exception)
//			{
//				System.out.println("ex: "+exception+" "+callid);
//			}
//		});
		
//		System.out.println("gsp: "+cid);
		component.scheduleStep(new IComponentStep<Object>()
		{
			@Classname("getServiceProxies")
			public IFuture<Object> execute(IInternalAccess ia)
			{
				Future fut = new IntermediateFuture<T>();
//				ia.getServiceContainer().searchService(IComponentManagementService.class, RequiredServiceInfo.SCOPE_PLATFORM)
//					.addResultListener(new ExceptionDelegationResultListener<IComponentManagementService, Object>(fut)
//				{
//					public void customResultAvailable(IComponentManagementService cms)
//					{
						// Hack! create remote rms cid with "rms" assumption.
						RemoteSearchCommand content = new RemoteSearchCommand(cid, service, true, scope, callid, (IAsyncFilter<IService>)filter, caller);
						
//						System.out.println("send to: "+rrms+" "+callid);
						sendMessage(rrms, null, content, callid, Starter.getRemoteDefaultTimeout(getComponent().getComponentIdentifier()), fut, null, null); // todo: non-func
//					}
//				});
				
				((IntermediateFuture<T>)fut).addResultListener(new DelegationResultListener<Collection<T>>(future)
				{
					@Override
					public void customResultAvailable(Collection<T> result)
					{
						if(result==null)	// Happens when some remote service classes not available locally?
						{
							result	= Collections.emptyList();
						}
						super.customResultAvailable(result);
					}
				});
						
//				return fut;
				return Future.getEmptyFuture();
			}
		});
		
		return future;
	}
	
//	/**
//	 *  Get all declared service proxies from a remote component.
//	 *  (called from arbitrary components)
//	 *  @param cid The remote provider id.
//	 *  @param service The service type.
//	 *  @return The service proxy.
//	 */
//	public IFuture<Collection<IService>> getDeclaredServiceProxies(IComponentIdentifier cid)
//	{
//		return getServiceProxies(cid, SServiceProvider.localmanager, SServiceProvider.contdecider, 
//			new AnyResultSelector(false, false));
//	}
	
	/**
	 *  Get an external access proxy from a remote component.
	 *  (called from arbitrary components)
	 *  @param cid Component target id.
	 *  @return External access of remote component. 
	 */
//	public IFuture<IExternalAccess> getExternalAccessProxy(final IComponentIdentifier cid)
//	{
//		if(cid instanceof ITransportComponentIdentifier)
//		{
//			return getExternalAccessProxy((ITransportComponentIdentifier)cid);
//		}
//		else
//		{
//			final Future<IExternalAccess> ret = new Future<IExternalAccess>();
//			
//			SServiceProvider.getService(component, ITransportAddressService.class, RequiredServiceInfo.SCOPE_PLATFORM)
//				.addResultListener(new ExceptionDelegationResultListener<ITransportAddressService, IExternalAccess>(ret)
//			{
//				public void customResultAvailable(ITransportAddressService tas)
//				{
//					tas.getTransportComponentIdentifier(cid).addResultListener(new ExceptionDelegationResultListener<ITransportComponentIdentifier, IExternalAccess>(ret)
//					{
//						public void customResultAvailable(ITransportComponentIdentifier tcid)
//						{
//							getExternalAccessProxy(tcid).addResultListener(new DelegationResultListener<IExternalAccess>(ret));
//						}
//					});
//				}
//			});
//			
//			return ret;
//		}
//	}
	
	/**
	 *  Get an external access proxy from a remote component.
	 *  (called from arbitrary components)
	 *  @param cid Component target id.
	 *  @return External access of remote component. 
	 */
	public IFuture<IExternalAccess> getExternalAccessProxy(final IComponentIdentifier cid)
	{
		final Future<IExternalAccess> ret = new Future<IExternalAccess>();
		
		component.scheduleStep(new IComponentStep<Object>()
		{
			@Classname("getExternalAccessProxy")
			public IFuture<Object> execute(IInternalAccess ia)
			{
				final Future<Object> fut = new Future<Object>();
//				ia.getServiceContainer().searchService(IComponentManagementService.class, RequiredServiceInfo.SCOPE_PLATFORM)
//					.addResultListener(new ExceptionDelegationResultListener<IComponentManagementService, Object>(fut)
//				{
//					public void customResultAvailable(IComponentManagementService cms)
//					{
						// Hack! create remote rms cid with "rms" assumption.
//						IComponentIdentifier rrms = cms.createComponentIdentifier("rms@"+cid.getPlatformName(), false, cid.getAddresses());
						IComponentIdentifier rrms = new ComponentIdentifier("rms@"+cid.getPlatformName());
						final String callid = SUtil.createUniqueId(component.getComponentIdentifier().getLocalName());
						RemoteGetExternalAccessCommand content = new RemoteGetExternalAccessCommand(cid, callid);
						
						sendMessage(rrms, cid, content, callid, Starter.getRemoteDefaultTimeout(getComponent().getComponentIdentifier()), fut, null, null); // todo: non-func
//					}
//				});
				
				return fut;
			}
		}).addResultListener(new ExceptionDelegationResultListener<Object, IExternalAccess>(ret)
		{
			public void customResultAvailable(Object result)
			{
				if(result!=null)
				{
					ret.setResult((IExternalAccess)result);
				}
				else
				{
					// may happen with incompatible platform versions.
					ret.setException(new RuntimeException("Cannot get external access: "+cid));
				}
			}
		});
		
		return ret;
	}
	
	/**
	 *  Get the component.
	 *  @return the component.
	 */
	public IExternalAccess getComponent()
	{
		return component;
	}
	
	/**
	 *  Get the rms component identifier.
	 *  @return The rms component identifier.
	 */
	public IComponentIdentifier getRMSComponentIdentifier()
	{
		return component.getComponentIdentifier();
	}
	
	/**
	 *  Get the waiting calls.
	 *  @return the waiting calls.
	 * /
	public Map getWaitingCalls()
	{
		return waitingcalls;
	}*/
	
//	/**
//	 *  Get the msg service.
//	 *  @return the msg service.
//	 */
//	public IMessageService getMessageService()
//	{
//		return msgservice;
//	}

	/**
	 *  Add a new waiting call.
	 *  @param callid The callid.
	 *  @param future The future.
	 */
	public void putWaitingCall(String callid, Future<Object> future, TimeoutTimerTask tt, Object context)
	{
		getRemoteReferenceModule().checkThread();
		if(waitingcalls.containsKey(callid))
			throw new RuntimeException("Call id collision: "+callid+" "+waitingcalls.size());
		waitingcalls.put(callid, new WaitingCallInfo(future, tt, context));
	}
	
	/**
	 *  Get a waiting call future.
	 *  @param callid The callid.
	 *  @return The future.
	 */
	public WaitingCallInfo getWaitingCall(String callid)
	{
		getRemoteReferenceModule().checkThread();
		return waitingcalls.get(callid);
	}
	
	/**
	 *  Remove a waiting call.
	 *  @param callid The callid.
	 *  @return The future.
	 */
	public WaitingCallInfo removeWaitingCall(String callid)
	{
		getRemoteReferenceModule().checkThread();
		return waitingcalls.remove(callid);
	}
	
	/**
	 *  Add a new processing call.
	 *  @param callid The callid.
	 *  @param future The future.
	 */
	public void putProcessingCall(String callid, Object future)
	{
		getRemoteReferenceModule().checkThread();
		processingcalls.put(callid, future);
	}
	
	/**
	 *  Get a processing call future.
	 *  @param callid The callid.
	 *  @return The future.
	 */
	public Object getProcessingCall(String callid)
	{
		getRemoteReferenceModule().checkThread();
		return processingcalls.get(callid);
	}
	
	/**
	 *  Remove a processing call.
	 *  @param callid The callid.
	 */
	public void removeProcessingCall(final String callid)
	{
//		getRemoteReferenceModule().checkThread();
		
		component.scheduleStep(new IComponentStep<Object>()
		{
			@Classname("remProcCall")
			public IFuture<Object> execute(IInternalAccess ia)
			{
				processingcalls.remove(callid);
				return new Future<Object>((Object)null);
			}
		});
		
//		return processingcalls.remove(callid);
	}
	
	/**
	 *  Add a future command.
	 */
	public void addFutureCommand(String callid, Runnable command)
	{
		getRemoteReferenceModule().checkThread();
		
		List<Runnable> coms = terminationcommands.get(callid);
		if(coms==null)
		{
			coms = new ArrayList<Runnable>();
			terminationcommands.put(callid, coms);
		}
		coms.add(command);
	}
	
	/**
	 *  Remove a future command.
	 */
	public List<Runnable> removeFutureCommands(String callid)
	{
		getRemoteReferenceModule().checkThread();
		
		return terminationcommands.remove(callid);
	}
	
	/**
	 *  Get the remote reference module.
	 *  @return the rrm.
	 */
	public RemoteReferenceModule getRemoteReferenceModule()
	{
		return rrm;
	}
	
	/**
	 *  Get the timer.
	 *  @return the timer.
	 */
	public Timer getTimer()
	{
		return timer;
	}

	//	protected static Map errors = Collections.synchronizedMap(new LRU(200));
//	public Map interestingcalls = new HashMap();
	/**
	 *  Send the request message of a remote method invocation.
	 *  (Can safely be called from any thread).
	 *  The context parameter is stored during the call and is available when the result arrives.
	 */
	public void sendMessage(final IComponentIdentifier receiver, final IComponentIdentifier realrec, final Object content,
		final String callid, final long to, final Future<Object> future, final Map<String, Object> nonfunc, final Object context)
	{
//		if(content instanceof RemoteIntermediateResultCommand)// && ((RemoteSearchCommand)content).getType().getTypeName().equals("jadex.micro.servicecall.IServiceCallService"))
//		{
//			System.out.println("RMS sending: "+System.currentTimeMillis()+", "+content+" "+receiver);
//		}
		try
		{
			component.scheduleStep(new IComponentStep<Void>()
			{
				@Classname("sendMessage")
				public IFuture<Void> execute(final IInternalAccess ia)
				{
					Future<Void>	pre	= new Future<Void>(); 
//					if(content instanceof AbstractRemoteCommand)
//					{
//						((AbstractRemoteCommand)content).preprocessCommand(ia, rrm, receiver)
//							.addResultListener(new DelegationResultListener<Void>(pre));
//					}
//					else
					{
						pre.setResult(null);
					}
					
					pre.addResultListener(ia.getComponentFeature(IExecutionFeature.class).createResultListener(new ExceptionDelegationResultListener<Void, Object>(future)
					{
						public void customResultAvailable(Void v)
						{
//							final long timeout = to<=0? DEFAULT_TIMEOUT: to;
							
							final TimeoutTimerTask tt = to>=0? new TimeoutTimerTask(to, future, callid, receiver, RemoteServiceManagementService.this): null;
//							System.out.println("remote timeout is: "+to);
							putWaitingCall(callid, future, tt, context);
							
							// Remove waiting call when future is done
							if(future instanceof ISubscriptionIntermediateFuture)
							{
								// IResultListener not allowed for subscription future.
								((ISubscriptionIntermediateFuture)future).addQuietListener(ia.getComponentFeature(IExecutionFeature.class).createResultListener(new IIntermediateFutureCommandResultListener<Object>()
								{
									public void intermediateResultAvailable(Object result)
									{
									}
									public void finished()
									{
										removeWaitingCall(callid);
									}
									public void resultAvailable(Collection<Object> result)
									{
										removeWaitingCall(callid);
									}
									public void exceptionOccurred(Exception exception)
									{
										removeWaitingCall(callid);								
										ia.getLogger().info("Remote exception occurred: "+receiver+", "+exception.toString());
									}
									public void commandAvailable(Object command)
									{
										// do nothing here, cannot forward
									}
								}));								
							}
							else
							{
								future.addResultListener(ia.getComponentFeature(IExecutionFeature.class).createResultListener(new IFutureCommandResultListener<Object>()
								{
									public void resultAvailable(Object result)
									{
										removeWaitingCall(callid);
									}
									public void exceptionOccurred(Exception exception)
									{
										removeWaitingCall(callid);								
										ia.getLogger().info("Remote exception occurred: "+receiver+", "+exception.toString());
									}
									public void commandAvailable(Object command)
									{
										// do nothing here, cannot forward
									}
								}));
							}
							
	//						System.out.println("Waitingcalls: "+waitingcalls.size());
							
							final Map<String, Object> msg = new HashMap<String, Object>();
							msg.put(SFipa.SENDER, component.getComponentIdentifier());
							msg.put(SFipa.RECEIVERS, new IComponentIdentifier[]{receiver});
							msg.put(SFipa.CONVERSATION_ID, callid);
							msg.put(SFipa.X_NONFUNCTIONAL, nonfunc);
							
							getResourceIdentifier(ia.getExternalAccess(), ((AbstractRemoteCommand)content).getSender())
								.addResultListener(ia.getComponentFeature(IExecutionFeature.class).createResultListener(new ExceptionDelegationResultListener<IResourceIdentifier, Object>(future)
							{
								public void customResultAvailable(IResourceIdentifier rid)
								{
									if(rid!=null && rid.getGlobalIdentifier()!=null && !ResourceIdentifier.isHashGid(rid) && !ResourceIdentifier.isJadexRid(rid))
									{
//										System.out.println("rid: "+rid+" "+content.getClass()+" "+msg.get(SFipa.SENDER));
										msg.put(SFipa.X_RID, rid);
									}
//									else
//									{
//										System.out.println("no rid: "+content.getClass());
//									}
									
									// todo:
//									msg.put(SFipa.X_RECEIVER, realrec);
									
									ia.getComponentFeature(IRequiredServicesFeature.class).searchService(ILibraryService.class, RequiredServiceInfo.SCOPE_PLATFORM)
										.addResultListener(new ExceptionDelegationResultListener<ILibraryService, Object>(future)
									{
										public void customResultAvailable(final ILibraryService ls)
										{
											ia.getComponentFeature(IRequiredServicesFeature.class).searchService(IMessageService.class, RequiredServiceInfo.SCOPE_PLATFORM)
												.addResultListener(new ExceptionDelegationResultListener<IMessageService, Object>(future)
											{
												public void customResultAvailable(final IMessageService ms)
												{
													// todo: use rid of sender?! (not important as writer does not use classloader, only nuggets)
													ls.getClassLoader(ia.getModel().getResourceIdentifier())
														.addResultListener(ia.getComponentFeature(IExecutionFeature.class).createResultListener(new ExceptionDelegationResultListener<ClassLoader, Object>(future)
													{
														public void customResultAvailable(final ClassLoader cl)
														{
															msg.put(SFipa.CONTENT, content);
															
//															if(content.toString().indexOf("ntermediate")!=-1)
//															{
//																System.out.println("RMS sending: "+System.currentTimeMillis()+", "+content+", "+receiver);
//															}
															ms.sendMessage(msg, SFipa.FIPA_MESSAGE_TYPE, ia.getComponentIdentifier(), ia.getModel().getResourceIdentifier(), realrec, null, null)
																.addResultListener(new ExceptionDelegationResultListener<Void, Object>(future)
															{
																public void customResultAvailable(Void result)
																{
//																	if(content.toString().indexOf("ntermediate")!=-1)
//																	{
//																		System.out.println("RMS sent: "+System.currentTimeMillis()+", "+content+", "+receiver);
//																	}
																	// ok message could be sent.
																	if(to>=0 && timer!=null && !tt.isCancelled())
																	{
																		timer.schedule(tt, to);
																	}
																}
																
																public void exceptionOccurred(Exception exception)
																{
//																	if(content.toString().indexOf("ntermediate")!=-1)
//																	{
//																		System.out.println("msg send ex: "+System.currentTimeMillis()+", "+content+", "+receiver+", "+exception);
//																	}
																	super.exceptionOccurred(exception);
																}
															});
														}
													}));
												}
											});
										}
									});
								}
							}));
						}
					}));
					
					return IFuture.DONE;
				}
			});
		}
		catch(ComponentTerminatedException e)
		{
			future.setException(e);
		}
	}

	/**
	 *  Called when the service is shut down.
	 */
	public IFuture<Void> shutdownService()
	{
		timer.cancel();
		timer = null;
		return super.shutdownService();
	}
	
	/**
	 *  Timeout timer task for sent messages.
	 */
	public static class TimeoutTimerTask extends TimerTask
	{
		protected Future<?> future;
		
		protected String callid;
		
		protected IComponentIdentifier receiver;
		
		protected RemoteServiceManagementService rms;
		
		protected long timeout;
		
		protected boolean	cancelled;
		
		/**
		 *  Create a new timer task.
		 */
		public TimeoutTimerTask(long timeout, Future<?> future, String callid, 
			IComponentIdentifier receiver, RemoteServiceManagementService rms)
		{
			this.timeout = timeout;
			this.future = future;
			this.callid = callid;
			this.receiver = receiver;
			this.rms = rms;
		}
		
		/**
		 *  Create a new timer task.
		 */
		public TimeoutTimerTask(TimeoutTimerTask tt)
		{
			this(tt.timeout, tt.future, tt.callid, tt.receiver, tt.rms);
		}
		
		/**
		 *  Start the timer task.
		 */
		public void start()
		{
			if(rms.getTimer()!=null)
			{
				rms.getTimer().schedule(this, timeout);
			}
		}

		/**
		 *  Run the task.
		 */
		public void run()
		{
			rms.getComponent().scheduleStep(new IComponentStep<Void>()
			{
				public IFuture<Void> execute(IInternalAccess ia)
				{
					if(!future.isDone())
					{
						WaitingCallInfo	wci	= rms.removeWaitingCall(callid);
						if(future instanceof ITerminableFuture)
						{
							((ITerminableFuture<?>)future).terminate(new TimeoutException("No reply received and timeout occurred: "
								+receiver+", "+callid+", "+wci+", "+timeout));						
						}
						else
						{
							future.setExceptionIfUndone(new TimeoutException("No reply received and timeout occurred: "
								+receiver+", "+callid+", "+wci+", "+timeout));
						}
					}
					return IFuture.DONE;
				}
			});
//			System.out.println("timeout triggered: "+msg);
		}
		
		public boolean cancel()
		{
			cancelled	= true;
			return super.cancel();
		}
		
		public boolean	isCancelled()
		{
			return cancelled;
		}
	};

	/**
	 *  Waiting call info.
	 */
	public static class WaitingCallInfo
	{
		final static Object FINISHED = new Object();
		
		/** The future. */
		protected Future<Object> future;
		
		/** The timer task. */
		protected TimeoutTimerTask timertask;
		
		/** The intermediate result cnt. */
		protected int cnt;
		
		/** The results per cnt. */
		protected Map<Integer, Object> results;
		
		/** The context. */
		protected Object context;

		/**
		 *  Create a new info.
		 */
		public WaitingCallInfo(Future<Object> future, TimeoutTimerTask timertask, Object context)
		{
			this.future = future;
			this.timertask = timertask;
			this.context = context;
		}

		/**
		 *  Get the future.
		 *  @return the future.
		 */
		public Future<Object> getFuture()
		{
			return future;
		}

		/**
		 *  Set the future.
		 *  @param future The future to set.
		 */
		public void setFuture(Future<Object> future)
		{
			this.future = future;
		}

		/**
		 *  Get the timertask.
		 *  @return the timertask.
		 */
		public TimeoutTimerTask getTimerTask()
		{
			return timertask;
		}

		/**
		 *  Set the timertask.
		 *  @param timertask The timertask to set.
		 */
		public void setTimerTask(TimeoutTimerTask timertask)
		{
			this.timertask = timertask;
		}
		
		/**
		 *  Get the cnt.
		 *  @return The cnt.
		 */
		public int getCnt()
		{
			return cnt;
		}

		/**
		 *  Set the cnt.
		 *  @param cnt The cnt to set.
		 */
		public void setCnt(int cnt)
		{
			this.cnt = cnt;
		}
		
		/**
		 * 
		 */
		public void addIntermediateResult(Integer num, Object res, boolean fini)
		{
			if(cnt==num)
			{
				IntermediateFuture ifut = (IntermediateFuture)future;
				cnt++;
				if(fini)
				{
					ifut.setFinishedIfUndone();
				}
				else
				{
					ifut.addIntermediateResultIfUndone(res);
				}
				
				if(results!=null)
				{
					while(results.containsKey(Integer.valueOf(cnt)))
					{
						Object nres = results.remove(Integer.valueOf(cnt++));
						if(nres==FINISHED)
						{
							ifut.setFinishedIfUndone();
						}
						else
						{
							ifut.addIntermediateResultIfUndone(nres);
						}
					}
				}
			}
			else
			{
				if(results==null)
				{
					results = new HashMap<Integer, Object>();
				}
				if(fini)
				{
					results.put(num, FINISHED);
				}
				else
				{
					results.put(num, res);
				}
			}
		}
		
		/**
		 * 
		 */
		public void refresh()
		{
			if(timertask!=null)
			{
				timertask.cancel();
				timertask = new TimeoutTimerTask(timertask);
				timertask.start();
			}
		}

		/**
		 *  Get the context.
		 *  @return The context.
		 */
		public Object getContext()
		{
			return context;
		}
	}
	
	/**
	 * 
	 */
	protected static IFuture<IResourceIdentifier> getResourceIdentifier(IExternalAccess provider, final IComponentIdentifier sender)
	{
		final Future<IResourceIdentifier> ret = new Future<IResourceIdentifier>();
//		ret.addResultListener(new IResultListener<IResourceIdentifier>()
//		{
//			public void resultAvailable(IResourceIdentifier result)
//			{
//			}
//			public void exceptionOccurred(Exception exception)
//			{
//				exception.printStackTrace();
//			}
//		});
		
		if(sender!=null)
		{
			SServiceProvider.getService(provider, IComponentManagementService.class, RequiredServiceInfo.SCOPE_PLATFORM)
				.addResultListener(new ExceptionDelegationResultListener<IComponentManagementService, IResourceIdentifier>(ret)
			{
				public void customResultAvailable(IComponentManagementService cms) 
				{
					cms.getComponentDescription(sender).addResultListener(new ExceptionDelegationResultListener<IComponentDescription, IResourceIdentifier>(ret)
					{
						public void customResultAvailable(IComponentDescription result) 
						{
							ret.setResult(result.getResourceIdentifier());
						};
						public void exceptionOccurred(Exception exception)
						{
							// Hack???
							ret.setResult(null);
//							super.exceptionOccurred(exception);
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
	 *  Get the addresses.
	 *  @return The addresses
	 */
	public TransportAddressBook getAddresses()
	{
		return addresses;
	}
}

