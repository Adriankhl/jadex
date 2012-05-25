package deco4mas.distributed.mechanism.subscription;

import jadex.bridge.IComponentStep;
import jadex.bridge.IInternalAccess;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.bridge.service.component.BasicServiceInvocationHandler;
import jadex.bridge.service.search.SServiceProvider;
import jadex.commons.future.IFuture;
import jadex.commons.future.IIntermediateFuture;
import jadex.commons.future.ISubscriptionIntermediateFuture;
import jadex.commons.future.IntermediateDefaultResultListener;
import jadex.kernelbase.StatelessAbstractInterpreter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import deco4mas.distributed.coordinate.environment.CoordinationSpace;
import deco4mas.distributed.mechanism.CoordinationInfo;
import deco4mas.distributed.mechanism.CoordinationMechanism;

/**
 * A coordination mechanism based on the {@link ICoordinationSubscriptionService} interface. The mechanism starts a {@link CoordinationSubscriptionService} for the local application which can be
 * subscribed by others. Also it searches for other {@link CoordinationSubscriptionService}s and subscribe to them.
 * 
 * @author Thomas Preisler
 */
public class SubscriptionServiceMechanism extends CoordinationMechanism {

	/** The applications interpreter */
	protected StatelessAbstractInterpreter applicationInterpreter;

	/** The subscribed services */
	protected Map<String, List<String>> subscribedServices;

	/** The started service */
	protected ICoordinationSubscriptionService service;

	/** Time to wait before a new service lookup */
	protected int lookupInterval;

	/** The id of the offered service */
	protected String serviceId;

	/**
	 * Default Constructor.
	 * 
	 * @param space
	 *            the {@link CoordinationSpace}
	 */
	public SubscriptionServiceMechanism(CoordinationSpace space) {
		super(space);

		// TODO Der Cast ist ein Hack bis Lars und Alex die Schnittstellen von Jadex anpassen
		this.applicationInterpreter = (StatelessAbstractInterpreter) space.getApplicationInternalAccess();

		// If it's a distributed application, then it has a contextID.
		HashMap<String, Object> appArgs = (HashMap<String, Object>) this.applicationInterpreter.getArguments();
		this.coordinationContextID = (String) appArgs.get("CoordinationContextID");

		this.serviceId = "CoordinationSubscriptionService@" + this.applicationInterpreter.getComponentIdentifier().getName();
		this.service = new CoordinationSubscriptionService(serviceId);
		this.subscribedServices = new HashMap<String, List<String>>();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see deco4mas.distributed.mechanism.ICoordinationMechanism#start()
	 */
	@Override
	public void start() {
		this.lookupInterval = getMechanismConfiguration().getIntegerProperty("lookupInterval");

		addService(serviceId, ICoordinationSubscriptionService.class, this.service);

		// look for other services
		IComponentStep<Void> subscribeStep = new IComponentStep<Void>() {

			@Override
			public IFuture<Void> execute(IInternalAccess ia) {
				getCoordinationServices().addResultListener(new IntermediateDefaultResultListener<ICoordinationSubscriptionService>() {

					@Override
					public void intermediateResultAvailable(ICoordinationSubscriptionService result) {
						List<String> serviceIds = subscribedServices.get(coordinationContextID);
						if (serviceIds == null) {
							serviceIds = new ArrayList<String>();
							subscribedServices.put(coordinationContextID, serviceIds);
						}

						// only add and subscribe to services you have not found yet
						if (!serviceIds.contains(result.getServiceId())) {
							ISubscriptionIntermediateFuture<CoordinationInfo> subscription = result.subscribe(coordinationContextID);
							serviceIds.add(result.getServiceId());
							subscription.addResultListener(new IntermediateDefaultResultListener<CoordinationInfo>() {

								@Override
								public void intermediateResultAvailable(CoordinationInfo result) {
									space.publishCoordinationEvent(result);
								}
							});
						}
					}
				});

				ia.waitForDelay(lookupInterval, this);

				return IFuture.DONE;
			}
		};

		this.applicationInterpreter.waitForDelay(0, subscribeStep);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see deco4mas.distributed.mechanism.ICoordinationMechanism#perceiveCoordinationEvent(java.lang.Object)
	 */
	@Override
	public void perceiveCoordinationEvent(Object obj) {
		service.publish(coordinationContextID, (CoordinationInfo) obj);
	}

	/**
	 * Returns all found {@link ICoordinationSubscriptionService}s.
	 * 
	 * @return all found {@link ICoordinationSubscriptionService}s
	 */
	private IIntermediateFuture<ICoordinationSubscriptionService> getCoordinationServices() {
		IIntermediateFuture<ICoordinationSubscriptionService> services = SServiceProvider.getServices(this.applicationInterpreter.getServiceProvider(), ICoordinationSubscriptionService.class,
				RequiredServiceInfo.SCOPE_GLOBAL);
		return services;
	}

	/**
	 * Adds the given Service to the application.
	 * 
	 * @param name
	 *            the service name
	 * @param type
	 *            the service type
	 * @param service
	 *            the actual service instance
	 */
	private void addService(String name, Class<?> type, Object service) {
		applicationInterpreter.addService(name, type, BasicServiceInvocationHandler.PROXYTYPE_DECOUPLED, null, service, null);
	}
}