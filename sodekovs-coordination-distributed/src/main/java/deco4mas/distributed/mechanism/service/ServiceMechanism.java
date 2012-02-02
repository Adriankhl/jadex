package deco4mas.distributed.mechanism.service;

import jadex.bridge.service.RequiredServiceInfo;
import jadex.bridge.service.component.BasicServiceInvocationHandler;
import jadex.bridge.service.search.SServiceProvider;
import jadex.commons.future.DefaultResultListener;
import jadex.kernelbase.StatelessAbstractInterpreter;

import java.util.Collection;

import deco4mas.distributed.coordinate.environment.CoordinationSpace;
import deco4mas.distributed.mechanism.CoordinationInfo;
import deco4mas.distributed.mechanism.CoordinationMechanism;

/**
 * Jadex Service based Coordination Medium which allows the coordination of distributed Jadex applications by distributing the {@link CoordinationInfo}s via Jadex Services.
 * 
 * @author Thomas Preisler
 */
public class ServiceMechanism extends CoordinationMechanism {

	/** The applications interpreter */
	protected StatelessAbstractInterpreter applicationInterpreter = null;

	/**
	 * Default Constructor.
	 * 
	 * @param space
	 *            the {@link CoordinationSpace}
	 */
	public ServiceMechanism(CoordinationSpace space) {
		super(space);

		// TODO Der Cast ist ein Hack bis Lars und Alex die Schnittstellen von Jadex anpassen
		this.applicationInterpreter = (StatelessAbstractInterpreter) space.getApplicatioInternalAccess();
	}

	@Override
	public void start() {
		String name = "CoordinationService@" + applicationInterpreter.getComponentIdentifier().toString();
		addService(name, ICoordinationService.class, new CoordinationService(space));
	}

	@Override
	public void perceiveCoordinationEvent(Object obj) {
		final CoordinationInfo ci = (CoordinationInfo) obj;

		SServiceProvider.getServices(applicationInterpreter.getServiceProvider(), ICoordinationService.class, RequiredServiceInfo.SCOPE_GLOBAL).addResultListener(
				new DefaultResultListener<Collection<ICoordinationService>>() {

					@Override
					public void resultAvailable(Collection<ICoordinationService> result) {
						for (ICoordinationService service : result) {
							service.publish(ci);
						}
					}
				});
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