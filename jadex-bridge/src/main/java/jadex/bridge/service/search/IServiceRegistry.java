package jadex.bridge.service.search;

import java.util.Set;

import jadex.bridge.IComponentIdentifier;
import jadex.bridge.service.IService;
import jadex.commons.future.IFuture;
import jadex.commons.future.ISubscriptionIntermediateFuture;

/**
 *  Interface for a service registry.
 *  
 *  todo: further cleanup the interface
 */
public interface IServiceRegistry
{
	/**
	 *  Add a service to the registry.
	 *  @param service The service.
	 */
	// write
	public IFuture<Void> addService(IService service);
	
	/**
	 *  Remove a service from the registry.
	 *  @param service The service.
	 */
	// write
	public void removeService(IService service);
	
	/**
	 *  Remove services of a platform from the registry.
	 *  @param platform The platform.
	 */
	// write
	public void removeServices(IComponentIdentifier platform);
	
	/**
	 *  Search for services.
	 */
	public <T> T searchServiceSync(ServiceQuery<T> query);
	
	/**
	 *  Search for services.
	 */
	public <T> Set<T> searchServicesSync(ServiceQuery<T> query);
	
	/**
	 *  Search for services.
	 */
	public <T> IFuture<T> searchServiceAsync(ServiceQuery<T> query);
	
	/**
	 *  Search for services.
	 */
	public <T> ISubscriptionIntermediateFuture<T> searchServicesAsync(ServiceQuery<T> query);
	
	/**
	 *  Search for services.
	 */
//	// read
	@Deprecated
	public <T> T searchService(ServiceQuery<T> query, boolean excluded);
	
	/**
	 *  Add a service query to the registry.
	 *  @param query ServiceQuery.
	 */
	// write
	public <T> ISubscriptionIntermediateFuture<T> addQuery(final ServiceQuery<T> query);
	
	/**
	 *  Remove a service query from the registry.
	 *  @param query ServiceQuery.
	 */
	// write
	public <T> void removeQuery(ServiceQuery<T> query);
	
	/**
	 *  Remove all service queries of a specific component from the registry.
	 *  @param owner The query owner.
	 */
	// write
	public void removeQueries(IComponentIdentifier owner);
	
	/**
	 *  Get queries per type.
	 *  @param type The interface type. If type is null all services are returned.
	 *  @return The queries.
	 */
	// read
//	public <T> Set<ServiceQueryInfo<T>> getQueries(ClassInfo type);
	
	/**
	 *  Add an excluded component. 
	 *  @param The component identifier.
	 */
	// write
	public void addExcludedComponent(IComponentIdentifier cid);
	
	/**
	 *  Remove an excluded component. 
	 *  @param The component identifier.
	 */
	// write
	public IFuture<Void> removeExcludedComponent(IComponentIdentifier cid);
	
	/**
	 *  Test if a service is included.
	 *  @param ser The service.
	 *  @return True if is included.
	 */
	// read
	public boolean isIncluded(IComponentIdentifier cid, IService ser);
}
