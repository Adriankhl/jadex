package jadex.platform.service.registryv2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import jadex.base.IPlatformConfiguration;
import jadex.base.PlatformConfigurationHandler;
import jadex.base.Starter;
import jadex.bridge.ComponentTerminatedException;
import jadex.bridge.IComponentIdentifier;
import jadex.bridge.IComponentStep;
import jadex.bridge.IInternalAccess;
import jadex.bridge.component.IExecutionFeature;
import jadex.bridge.component.IRemoteExecutionFeature;
import jadex.bridge.component.impl.IInternalRemoteExecutionFeature;
import jadex.bridge.service.IServiceIdentifier;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.bridge.service.annotation.Service;
import jadex.bridge.service.component.IRequiredServicesFeature;
import jadex.bridge.service.search.ServiceEvent;
import jadex.bridge.service.search.ServiceNotFoundException;
import jadex.bridge.service.search.ServiceQuery;
import jadex.bridge.service.search.ServiceRegistry;
import jadex.bridge.service.types.pawareness.IPassiveAwarenessService;
import jadex.bridge.service.types.registryv2.ISearchQueryManagerService;
import jadex.bridge.service.types.registryv2.ISuperpeerService;
import jadex.bridge.service.types.security.ISecurityService;
import jadex.commons.Boolean3;
import jadex.commons.collection.MultiCollection;
import jadex.commons.future.ExceptionDelegationResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.FutureTerminatedException;
import jadex.commons.future.IFuture;
import jadex.commons.future.IIntermediateResultListener;
import jadex.commons.future.IResultListener;
import jadex.commons.future.ISubscriptionIntermediateFuture;
import jadex.commons.future.ITerminableFuture;
import jadex.commons.future.ITerminableIntermediateFuture;
import jadex.commons.future.IntermediateDefaultResultListener;
import jadex.commons.future.SubscriptionIntermediateFuture;
import jadex.commons.future.TerminableFuture;
import jadex.commons.future.TerminableIntermediateFuture;
import jadex.commons.future.TerminationCommand;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.AgentCreated;
import jadex.micro.annotation.AgentKilled;
import jadex.micro.annotation.Binding;

/**
 *  The super peer client agent is responsible for managing connections to super peers for each network.
 */
@Agent(autoprovide=Boolean3.TRUE)
@Service
public class SuperpeerClientAgent	implements ISearchQueryManagerService
{
	//-------- attributes --------
	
	/** The agent. */
	@Agent
	protected IInternalAccess	agent;
	
	/** The managed connections for each network. */
	protected Map<String, NetworkManager>	connections;
	
	//-------- agent life cycle --------
	
	/**
	 *  Find and connect to super peers.
	 */
	@AgentCreated
	protected IFuture<Void>	init()
	{
		Future<Void>	ret	= new Future<>();
		connections	= new LinkedHashMap<>();
		
		ISecurityService	secser	= agent.getFeature(IRequiredServicesFeature.class).searchLocalService(new ServiceQuery<>(ISecurityService.class));
		secser.getNetworkNames().addResultListener(new ExceptionDelegationResultListener<Set<String>, Void>(ret)
		{
			@Override
			public void customResultAvailable(Set<String> networks) throws Exception
			{
				assert agent.getFeature(IExecutionFeature.class).isComponentThread();
				
				for(String network: networks)
				{
					connections.put(network, new NetworkManager(network));
				}
					
				ret.setResult(null);
			}
		});
		
		return ret;
	}
	
	/**
	 *  Close all connections on shutdown.
	 */
	@AgentKilled
	protected void	shutdown()
	{
		for(NetworkManager manager: connections.values())
		{
			manager.stop();
		}
	}
	
	//-------- ISearchQueryManagerService interface --------
	
	/**
	 *  Search for matching services using available remote information sources and provide first result.
	 *  @param query	The search query.
	 *  @return Future providing the corresponding service or ServiceNotFoundException when not found.
	 */
	public <T> ITerminableFuture<T> searchService(ServiceQuery<T> query)
	{
		TerminableFuture<T>	ret	= new TerminableFuture<>();
		AtomicInteger	track	= new AtomicInteger(1);
		boolean	foundsuperpeer	= false;
		
		// TODO: search all if networks==null???
		for(String networkname: query.getNetworkNames())
		{
			NetworkManager	manager	= connections.get(networkname);
			if(manager!=null)
			{
				if(manager.superpeer!=null)
				{
					foundsuperpeer	= true;
					// Todo: remember searches for termination? -> more efficient to just let searches run out an ignore result?
					track.incrementAndGet();
					IFuture<IServiceIdentifier>	fut	= manager.superpeer.searchService(query);
					fut.addResultListener(new IResultListener<IServiceIdentifier>()
					{
						@Override
						public void exceptionOccurred(Exception exception)
						{
							// Forward exception only of last finished future...
							if(track.decrementAndGet()==0)
							{
								ret.setExceptionIfUndone(exception);
							}
						}
						
						@SuppressWarnings("unchecked")
						@Override
						public void resultAvailable(IServiceIdentifier result)
						{
							// Forward result if first
							ret.setResultIfUndone((T)result);
						}
					});
				}
				
				// else not connected -> ignore
			}
			
			// else ignore unknown network
			
			// TODO: allow selective/hybrid polling fallback for unknown/unconnected networks?			
		}
		
		// polling fallback when no superpeers
		if(!foundsuperpeer)
		{
			searchRemoteServices(query).addResultListener(new IntermediateDefaultResultListener<T>()
			{
				@Override
				public void intermediateResultAvailable(T result)
				{
					// Forward result if first
					ret.setResultIfUndone(result);					
				}
				
				@Override
				public void exceptionOccurred(Exception exception)
				{
					ret.setException(exception);
				}
				
				@Override
				public void finished()
				{
					ret.setExceptionIfUndone(new ServiceNotFoundException(query.toString()));
				}
			});
		}
		
		else if(track.decrementAndGet()==0)
		{
			ret.setExceptionIfUndone(new ServiceNotFoundException(query.toString()));
		}

		return ret;
	}
	
	/**
	 *  Search for all matching services.
	 *  @param query	The search query.
	 *  @return Each service as an intermediate result or a collection of services as final result.
	 */
	public <T>  ITerminableIntermediateFuture<T> searchServices(ServiceQuery<T> query)
	{
		TerminableIntermediateFuture<T>	ret	= new TerminableIntermediateFuture<>();
		AtomicInteger	track	= new AtomicInteger(1);
		boolean	foundsuperpeer	= false;
		
		// TODO: search all if networks==null???
		for(String networkname: query.getNetworkNames())
		{
			NetworkManager	manager	= connections.get(networkname);
			if(manager!=null)
			{
				if(manager.superpeer!=null)
				{
					foundsuperpeer	= true;
					// Todo: remember searches for termination? -> more efficient to just let searches run out an ignore result?
					track.incrementAndGet();
					IFuture<Set<IServiceIdentifier>>	fut	= manager.superpeer.searchServices(query);
					fut.addResultListener(new IResultListener<Set<IServiceIdentifier>>()
					{
						@Override
						public void exceptionOccurred(Exception exception)
						{
							if(track.decrementAndGet()==0)
							{
								ret.setFinishedIfUndone();
							}
						}
						
						@SuppressWarnings("unchecked")
						@Override
						public void resultAvailable(Set<IServiceIdentifier> result)
						{
							if(!ret.isDone())
							{
								for(IServiceIdentifier sid: result)
								{
									ret.addIntermediateResultIfUndone((T)sid);
								}
							}
							
							if(track.decrementAndGet()==0)
							{
								ret.setFinishedIfUndone();
							}
						}
					});
				}
				
				// else not connected -> ignore
			}
			
			// else ignore unknown network
			
			// TODO: allow selective/hybrid polling fallback for unknown/unconnected networks?			
		}
		
		// polling fallback when no superpeers
		if(!foundsuperpeer)
		{
			return searchRemoteServices(query);
		}
		
		else if(track.decrementAndGet()==0)
		{
			ret.setFinishedIfUndone();
		}

		return ret;
	}
	
	/**
	 *  Add a service query.
	 *  Continuously searches for matching services using available remote information sources.
	 *  @param query	The search query.
	 *  @return Future providing the corresponding services as intermediate results.
	 */
	public <T> ISubscriptionIntermediateFuture<T> addQuery(ServiceQuery<T> query)
	{
		// todo: remember and terminate managed queries on shutdown?
		QueryManager<T>	qinfo	= new QueryManager<>(query);
		return qinfo.getReturnFuture();
	}
	
	//-------- helper methods --------
	
	/**
	 *  Search for services on remote platforms using the polling fallback and awareness.
	 */
	protected <T> TerminableIntermediateFuture<T> searchRemoteServices(final ServiceQuery<T> query)
	{
		// TODO: termination
		final TerminableIntermediateFuture<T> ret = new TerminableIntermediateFuture<T>();
		
		// Check for awareness service
		Collection<IPassiveAwarenessService>	pawas	= agent.getFeature(IRequiredServicesFeature.class)
			.searchLocalServices(new ServiceQuery<>(IPassiveAwarenessService.class));
		if(!pawas.isEmpty())
		{
			IPassiveAwarenessService	pawa	= pawas.iterator().next();
			// Count awa search + platform searches (+ async filtering, if any).
			final AtomicInteger	cnt	= new AtomicInteger(1);
			
			// Search for other platforms
			pawa.searchPlatforms().addResultListener(new IntermediateDefaultResultListener<IComponentIdentifier>()
			{
				@Override
				public void intermediateResultAvailable(final IComponentIdentifier platform)
				{
//					System.out.println(getComponent() + " searching remote platform: "+platform+", "+query);
					
					// Only (continue to) search remote when future not yet finished or cancelled.
					if(!ret.isDone())
					{
						cnt.incrementAndGet();
						// TODO: use remote registry service
						final IFuture<Collection<T>> remotesearch =  ((IInternalRemoteExecutionFeature)agent.getFeature(IRemoteExecutionFeature.class))
								.executeRemoteSearch(platform, query);
						
//						System.out.println(cid + " searching remote platform3: "+platform+", "+query);
						remotesearch.addResultListener(new IResultListener<Collection<T>>()
						{
							public void resultAvailable(Collection<T> result)
							{
//								System.out.println(getComponent() + " searched remote platform: "+platform+", "+result);
								if(result != null)
								{
									for(Iterator<T> it = result.iterator(); it.hasNext(); )
									{
										T ser = it.next();
										ret.addIntermediateResultIfUndone(ser);
									}
								}
								doFinished();
							}

							public void exceptionOccurred(Exception exception)
							{
//								System.out.println(getComponent() + " searched remote platform: "+platform+", "+exception);
								doFinished();
							}
						});
					}
				}
				
				@Override
				public void finished()
				{
					doFinished();
				}
				
				@Override
				public void exceptionOccurred(Exception exception)
				{
					// ignore exception
					doFinished();
				}
				
				private void doFinished()
				{
					if(cnt.decrementAndGet()==0)
					{
						// Undone, because gets terminated on first result for search one
						ret.setFinishedIfUndone();
					}
				}
			});
		}
		else
		{
			ret.setFinished();
		}
		
		return ret;
	}
	
	//-------- helper classes --------
	
	/**
	 *  Manage the connection to a superpeer for a given network.
	 */
	protected class NetworkManager
	{
		//-------- attributes --------
		
		/** The managed network (i.e. network name). */
		protected String	networkname;
		
		/** The flag to indicate the manager should be active. */
		protected boolean	running;
		
		/** The current query future for available super peers for a given network (only set while searching for the network). */
		protected ISubscriptionIntermediateFuture<ISuperpeerService>	superpeerquery;
	
		/** The current super peer connections for each network (only set when found, i.e. when not searching for the network). */
		protected ISuperpeerService	superpeer;
		
		/** The connection to the super peer. */
		protected ISubscriptionIntermediateFuture<Void>	connection;
		
		/** Query on the local registry used to transmit changes to super peer. */
		protected ISubscriptionIntermediateFuture<ServiceEvent<IServiceIdentifier>> localquery;
		
		/** User queries waiting for a connection to superpeer. */
		Collection<QueryManager<?>>	waitingqueries	= new ArrayList<>();
		
		//-------- constructors --------
		
		/**
		 *  Create and start a network manager.
		 */
		protected NetworkManager(String networkname)
		{
			this.networkname	= networkname;
			startSuperpeerSearch();
		}
		
		//------- helper methods ---------
		
		/**
		 *  Disconnect and stop all activity, if any.
		 */
		protected void	stop()
		{
			running	= false;
			stopSuperpeerSearch();
			stopSuperpeerSubscription();
		}
	
		/**
		 *  Find a super peer for a given network.
		 *  Query is automatically restarted on failure.
		 *  @param networkname	The network.
		 */
		protected void startSuperpeerSearch()
		{
			// Clean start for new search (e.g. failure recovery).
			stop();
			running	= true;
			
			assert superpeerquery==null;
			assert superpeer==null;
			assert connection==null;
			assert localquery==null;
			
			System.out.println(agent+" searching for super peers for network "+networkname);
			
			// Also finds and adds locally available super peers -> locaL registry only contains local services, (local/remote) super peer manages separate registry
			ISubscriptionIntermediateFuture<ISuperpeerService>	queryfut	= agent.getFeature(IRequiredServicesFeature.class)
				.addQuery(new ServiceQuery<>(ISuperpeerService.class, Binding.SCOPE_GLOBAL).setNetworkNames(networkname));
			superpeerquery	= queryfut;	// Remember current query.
			queryfut.addResultListener(new IntermediateDefaultResultListener<ISuperpeerService>()
			{
				IIntermediateResultListener<ISuperpeerService>	lis	= this;
				
				@Override
				public void intermediateResultAvailable(ISuperpeerService sp)
				{
					if(running)
					{
						System.out.println(agent+" requesting super peer connection for network "+networkname+" from super peer: "+sp);
						ISubscriptionIntermediateFuture<Void>	regfut	= sp.registerClient(networkname);
						regfut.addResultListener(new IIntermediateResultListener<Void>()
						{
							@Override
							public void intermediateResultAvailable(Void result)
							{
								// First command -> connected (shouldn't be any other commands).
								
								// First connected super peer -> remember connection and stop search
								if(running && superpeer==null)
								{
									System.out.println(agent+" accepting super peer connection for network "+networkname+" from super peer: "+sp);
									
									// Stop ongoing search, if any
									stopSuperpeerSearch();
									superpeer	= sp;
									connection	= regfut;
									
									// Activate waiting queries if any.
									for(QueryManager<?> qmanager: waitingqueries)
									{
										qmanager.updateQuery(new String[]{networkname});
									}
									waitingqueries.clear();
									
									// Local query uses registry directly (w/o feature) -> only service identifiers needed and also removed events
									localquery = ServiceRegistry.getRegistry(agent.getIdentifier())
										.addQuery(new ServiceQuery<>((Class<ServiceEvent<IServiceIdentifier>>)null, RequiredServiceInfo.SCOPE_PLATFORM)
											.setNetworkNames(networkname).setReturnType(ServiceEvent.CLASSINFO));
									localquery.addResultListener(new IIntermediateResultListener<ServiceEvent<IServiceIdentifier>>()
									{
										public void resultAvailable(Collection<ServiceEvent<IServiceIdentifier>> result)
										{
											// Should not happen?
											assert false;
										}
										
										public void exceptionOccurred(Exception exception)
										{
											// Should only happen on termination?
											assert exception instanceof FutureTerminatedException : exception;
										}
		
										public void intermediateResultAvailable(final ServiceEvent<IServiceIdentifier> event)
										{
											if (!RequiredServiceInfo.isScopeOnLocalPlatform(event.getService().getScope()))
											{
												agent.scheduleStep(new IComponentStep<Void>()
												{
													public IFuture<Void> execute(IInternalAccess ia)
													{
														try
														{
															regfut.sendBackwardCommand(event);
														}
														catch (Exception e)
														{
															startSuperpeerSearch();
														}
														return IFuture.DONE;
													};
												});
											}
										}
		
										public void finished()
										{
											// Should not happen?
											assert false;
										}
									});
								}
								
								// Stopped or additional connection -> terminate connection. 
								else
								{
									regfut.terminate();
								}
							}	
							
							@Override
							public void resultAvailable(Collection<Void> result)
							{
								checkConnectionRetry(null);
							}
							
							@Override
							public void finished()
							{
								checkConnectionRetry(null);
							}
							
							@Override
							public void exceptionOccurred(Exception exception)
							{
								checkConnectionRetry(exception);
							}
							
							/**
							 *  When some connection finishes or fails -> check if current connection and restart query.
							 */
							protected void	checkConnectionRetry(Exception reason)
							{
								// Connection still current but ended?
								if(running && superpeer==sp)
								{
									// On error -> restart search after e.g. 300 millis (realtime) (very small delay to prevent busy loop on persistent immediate error)
									agent.getFeature(IExecutionFeature.class).waitForDelay(Starter.getScaledRemoteDefaultTimeout(agent.getIdentifier(), 0.01), new IComponentStep<Void>()
									{
										@Override
										public IFuture<Void> execute(IInternalAccess ia)
										{
											// Still no other connection in between?
											if(running && superpeer==sp)
											{
												// Restart connection attempt
												lis.intermediateResultAvailable(sp);
											}
											return IFuture.DONE;
										}
									}, true);
								}
								
								// Connection immediately failed but no other connection -> retry this super peer after some timeout
								if(superpeer==null && !(reason instanceof ComponentTerminatedException))
								{
									agent.getFeature(IExecutionFeature.class).waitForDelay(Starter.getRemoteDefaultTimeout(agent.getIdentifier()), new IComponentStep<Void>()
									{
										@Override
										public IFuture<Void> execute(IInternalAccess ia)
										{
											// Still no connection?
											if(superpeer==null)
											{
												startSuperpeerSearch();
											}
											return IFuture.DONE;
										}
									}, true);
								}
							}
						});
					}
				}
				
				@Override
				public void finished()
				{
					checkQueryRetry(null);
				}
				
				@Override
				public void exceptionOccurred(Exception exception)
				{
					checkQueryRetry(exception);
				}						
				
				/**
				 *  When query finishes or fails -> check if current query and restart query.
				 */
				protected void	checkQueryRetry(Exception reason)
				{
					 assert queryfut.isDone();
					 
					// Search still valid but ended?
					if(running && superpeerquery==queryfut)
					{
						// On error -> restart search after e.g. 3 secs (realtime) (small delay to prevent busy loop on persistent immediate error)
						agent.getFeature(IExecutionFeature.class).waitForDelay(Starter.getScaledRemoteDefaultTimeout(agent.getIdentifier(), 0.1), new IComponentStep<Void>()
						{
							@Override
							public IFuture<Void> execute(IInternalAccess ia)
							{
								// Still no other search started in between?
								if(running && superpeerquery==queryfut)
								{
									startSuperpeerSearch();
								}
								return IFuture.DONE;
							}
						}, true);
					}
				}
			});
		}
	
		/**
		 *  Stop an ongoing super peer search for the given network (if any).
		 *  @param networkname
		 */
		private void stopSuperpeerSearch()
		{
			if(superpeerquery!=null)
			{
				System.out.println(agent+" stopping search for super peers for network: "+networkname);
				// Remove before terminate to avoid auto-start of new search on error.
				ISubscriptionIntermediateFuture<ISuperpeerService>	tmp	= superpeerquery;
				superpeerquery	=  null;
				tmp.terminate();
			}
		}
		
		private void stopSuperpeerSubscription()
		{
			if(connection!=null)
			{
				System.out.println(agent+" dropping super peer connection for network "+networkname+" from super peer: "+superpeer);
				assert localquery!=null;
				assert !localquery.isDone();
				assert connection!=null;
				assert superpeer!=null;

				localquery.terminate();
				if(!connection.isDone())
					connection.terminate();

				localquery	= null;				
				connection	= null;
				superpeer	= null;
			}
		}

		/**
		 *  When no connection to network -> remember query until connection etsablished. 
		 */
		protected <T>	void	addWaitingQuery(QueryManager<T> qmanager)
		{
			assert superpeer==null : "Should only be called when no connection.";
			waitingqueries.add(qmanager);
		}
	}
	
	/**
	 *  Internal handler for each user query.
	 */
	protected class QueryManager<T>
	{
		//-------- attributes --------
		
		/** The query itself. */
		protected ServiceQuery<T>	query;
		
		/** The return future to the user. */
		protected SubscriptionIntermediateFuture<T> retfut;
		
		/** The map of handled networks by each superpeer. */
		protected MultiCollection<ISuperpeerService, String> networkspersuperpeer;
		
		/** The auxiliary futures as received from superpeers. */
		protected Collection<ITerminableIntermediateFuture<T>> futures;
		
		//-------- constructors --------
		
		/**
		 *  Create a query manager and start query handling.
		 */
		protected QueryManager(ServiceQuery<T> query)
		{
			this.query	= query;
			this.retfut	= new SubscriptionIntermediateFuture<>();
			this.networkspersuperpeer	= new MultiCollection<>();
			this.futures	= new LinkedHashSet<>();
			
			retfut.setTerminationCommand(new TerminationCommand()
			{
				@Override
				public void terminated(Exception reason)
				{
					for(ITerminableFuture<?> fut: futures.toArray(new ITerminableFuture[futures.size()]))
					{
						fut.terminate();
					}
				}
			});
			
			// Start handling
			updateQuery(query.getNetworkNames());
		}
		
		//-------- methods --------
		
		/**
		 *  The return future for the user containing all the collected results from the internal queries.
		 */
		public ISubscriptionIntermediateFuture<T> getReturnFuture()
		{
			return retfut;
		}
		
		//-------- internal methods --------
		
		
		/**
		 *  Add/update query connections to relevant super peers for given networks.
		 */
		protected void	updateQuery(String[] networknames)
		{
			// Remember new superpeers, unless initial invocation with empty multicollection.
			Set<ISuperpeerService>	newsuperpeers	= networkspersuperpeer.isEmpty() ? null : new LinkedHashSet<>();
			
			// Fill multicollection with relevant superpeers for networks
			// TODO: search all if networks==null???
			for(String networkname: networknames)
			{
				NetworkManager	manager	= connections.get(networkname);
				if(manager!=null)
				{
					if(manager.superpeer!=null)
					{
						Collection<String>	col	= networkspersuperpeer.add(manager.superpeer, networkname);
						if(newsuperpeers!=null && col.size()==1)
							newsuperpeers.add(manager.superpeer);
					}
					
					// Not yet connected to superpeer for network -> remember for later
					else
					{
						manager.addWaitingQuery(this);
					}
				}
				
				// else ignore unknown network
			}
			
			// Add queries for each relevant superpeer
			for(ISuperpeerService superpeer: newsuperpeers!=null ? newsuperpeers : networkspersuperpeer.keySet())
			{
				ITerminableIntermediateFuture<T>	fut	= superpeer.addQuery(query);
				futures.add(fut);	// Remember future for later termination
				fut.addResultListener(new IIntermediateResultListener<T>()
				{
					@Override
					public void intermediateResultAvailable(T result)
					{
						// Forward result to user query
						retfut.addIntermediateResult(result);
					}
					
					@Override
					public void exceptionOccurred(Exception exception)
					{
						// Reconnect query on error, if user query still active
						if(!retfut.isDone())
						{
							// Just remove from lists and try again
							futures.remove(fut);
							Collection<String>	failed_networks	= networkspersuperpeer.remove(superpeer);
							updateQuery(failed_networks.toArray(new String[failed_networks.size()]));
						}
					}
					
					@Override
					public void finished()
					{
						// shouldn't happen?
						exceptionOccurred(null);
					}
					
					@Override
					public void resultAvailable(Collection<T> result)
					{
						// shouldn't happen?
						exceptionOccurred(null);
					}
				});
			}
		}
	}
	
	//-------- main for testing --------
	
	/**
	 *  Main for testing.
	 */
	public static void	main(String[] args)
	{
		// Common base configuration
		IPlatformConfiguration	baseconfig	= PlatformConfigurationHandler.getMinimalComm();
		baseconfig.addComponent("jadex.platform.service.pawareness.PassiveAwarenessIntraVMAgent.class");
//		baseconfig.setGui(true);
//		baseconfig.setLogging(true);
		
		// Super peer base configuration
		IPlatformConfiguration	spbaseconfig	= baseconfig.clone();
		spbaseconfig.addComponent(SuperpeerRegistryAgent.class);
		
		IPlatformConfiguration	config;
		
		// Super peer AB
		config	= spbaseconfig.clone();
		config.setPlatformName("SPAB_*");
		config.setNetworkNames("network-a", "network-b");
		config.setNetworkSecrets("secret-a", "secret-b");
		Starter.createPlatform(config, args).get();
		
		// Super peer BC
		config	= spbaseconfig.clone();
		config.setPlatformName("SPBC_*");
		config.setNetworkNames("network-c", "network-b");
		config.setNetworkSecrets("secret-c", "secret-b");
		Starter.createPlatform(config, args).get();

		// Client ABC
		config	= baseconfig.clone();
		config.addComponent(SuperpeerClientAgent.class);
		config.setPlatformName("ClientABC_*");
		config.setNetworkNames("network-a", "network-b", "network-c");
		config.setNetworkSecrets("secret-a", "secret-b", "secret-c");
		Starter.createPlatform(config, args).get();
	}
}
