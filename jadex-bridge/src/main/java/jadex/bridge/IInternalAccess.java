package jadex.bridge;

import jadex.bridge.modelinfo.IModelInfo;
import jadex.bridge.service.types.cms.IComponentDescription;
import jadex.bridge.service.types.monitoring.IMonitoringEvent;
import jadex.bridge.service.types.monitoring.IMonitoringService.PublishEventLevel;
import jadex.bridge.service.types.monitoring.IMonitoringService.PublishTarget;
import jadex.commons.IFilter;
import jadex.commons.IParameterGuesser;
import jadex.commons.IValueFetcher;
import jadex.commons.future.IFuture;
import jadex.commons.future.ISubscriptionIntermediateFuture;

import java.util.Map;
import java.util.logging.Logger;

/**
 *  Common interface for all component types.
 *  Provides the user view of the component, i.e.,
 *  methods the component can call on itself.
 */
public interface IInternalAccess
{
	/**
	 *  Get the model of the component.
	 *  @return	The model.
	 */
	public IModelInfo getModel();

	/**
	 *  Get the configuration.
	 *  @return	The configuration.
	 */
	public String getConfiguration();
	
	/**
	 *  Get the id of the component.
	 *  @return	The component id.
	 */
	public IComponentIdentifier	getComponentIdentifier();
	
	/**
	 *  Get a feature of the component.
	 *  @param feature	The type of the feature.
	 *  @return The feature instance.
	 */
	public <T> T	getComponentFeature(Class<? extends T> type);
	
	/**
	 *  Get the component description.
	 *  @return	The component description.
	 */
	// Todo: hack??? should be internal to CMS!?
	public IComponentDescription	getComponentDescription();
	
	/**
	 *  Get the service provider.
	 *  @return The service provider.
	 */
	// Todo: convenience object? -> fix search!?
//	public IServiceContainer getServiceContainer();
	
	/**
	 *  Kill the component.
	 */
	public IFuture<Map<String, Object>> killComponent();
	
	/**
	 *  Kill the component.
	 *  @param e The failure reason, if any.
	 */
	public IFuture<Map<String, Object>> killComponent(Exception e);
	
//	/**
//	 *  Create a result listener that is executed on the
//	 *  component thread.
//	 */
//	public <T> IResultListener<T> createResultListener(IResultListener<T> listener);
//	
//	/**
//	 *  Create a result listener that is executed on the
//	 *  component thread.
//	 */
//	public <T> IIntermediateResultListener<T> createResultListener(IIntermediateResultListener<T> listener);
	
	/**
	 *  Get the external access.
	 *  @return The external access.
	 */
	public IExternalAccess getExternalAccess();
	
	/**
	 *  Get the logger.
	 *  @return The logger.
	 */
	public Logger getLogger();
	
	/**
	 *  Get the fetcher.
	 *  @return The fetcher.
	 */
	// Todo: move to IPlatformComponent?
	public IValueFetcher getFetcher();
		
	/**
	 *  Get the parameter guesser.
	 *  @return The parameter guesser.
	 */
	// Todo: move to IPlatformComponent?
	public IParameterGuesser getParameterGuesser();
		
//	/**
//	 *  Get the arguments.
//	 *  @return The arguments.
//	 */
//	public Map<String, Object> getArguments();
//	
//	/**
//	 *  Get the component results.
//	 *  @return The results.
//	 */
//	public Map<String, Object> getResults();
	
//	/**
//	 *  Set a result value.
//	 *  @param name The result name.
//	 *  @param value The result value.
//	 */
//	public void setResultValue(String name, Object value);
	
	/**
	 *  Get the class loader of the component.
	 */
	public ClassLoader	getClassLoader();
	
//	/**
//	 *  Get the model name of a component type.
//	 *  @param ctype The component type.
//	 *  @return The model name of this component type.
//	 */
//	public IFuture getFileName(String ctype);
	
//	/**
//	 *  Execute a component step.
//	 */
//	public <T>	IFuture<T> scheduleStep(IComponentStep<T> step);
//	
//	/**
//	 *  Execute an immediate component step,
//	 *  i.e., the step is executed also when the component is currently suspended.
//	 */
//	public <T>	IFuture<T> scheduleImmediate(IComponentStep<T> step);
//	
//	/**
//	 *  Wait for some time and execute a component step afterwards.
//	 */
//	public <T>	IFuture<T> waitForDelay(long delay, IComponentStep<T> step, boolean realtime);
//
//	/**
//	 *  Wait for some time and execute a component step afterwards.
//	 */
//	public <T>	IFuture<T> waitForDelay(long delay, IComponentStep<T> step);
//
//	/**
//	 *  Wait for some time.
//	 */
//	public IFuture<Void> waitForDelay(long delay, boolean realtime);
//	
//	/**
//	 *  Wait for some time.
//	 */
//	public IFuture<Void> waitForDelay(long delay);
//	
//	// todo:?
////	/**
////	 *  Wait for some time and execute a component step afterwards.
////	 */
////	public IFuture waitForImmediate(long delay, IComponentStep step);
//
//	/**
//	 *  Test if current thread is the component thread.
//	 *  @return True if the current thread is the component thread.
//	 */
//	public boolean isComponentThread();
	
//	/**
//	 *  Subscribe to component events.
//	 *  @param filter An optional filter.
//	 *  @param initial True, for receiving the current state.
//	 */
////	@Timeout(Timeout.NONE)
//	public ISubscriptionIntermediateFuture<IMonitoringEvent> subscribeToEvents(IFilter<IMonitoringEvent> filter, boolean initial, PublishEventLevel elm);
//
//	/**
//	 *  Publish a monitoring event. This event is automatically send
//	 *  to the monitoring service of the platform (if any). 
//	 */
//	public IFuture<Void> publishEvent(IMonitoringEvent event, PublishTarget pt);
//	
//	/**
//	 *  Check if event targets exist.
//	 */
//	public boolean hasEventTargets(PublishTarget pt, PublishEventLevel pi);

	
//	/**
//	 *  Get the required service property provider for a service.
//	 */
//	public INFMixedPropertyProvider getRequiredServicePropertyProvider(IServiceIdentifier sid);
//	
//	/**
//	 *  Has the service a property provider.
//	 */
//	public boolean hasRequiredServicePropertyProvider(IServiceIdentifier sid);
	
	/**
	 *  Get the realtime setting.
	 *  @return If is realtime.
	 */
	public boolean isRealtime();
	
	/**
	 *  Get the copy setting.
	 *  @return If is copy.
	 */
	public boolean isCopy();
	
	/**
	 *  Get the children (if any) component identifiers.
	 *  @return The children component identifiers.
	 */
	public IFuture<IComponentIdentifier[]> getChildren(String type);
}
