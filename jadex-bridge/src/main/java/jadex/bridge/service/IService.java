package jadex.bridge.service;

import jadex.bridge.service.annotation.Reference;
import jadex.commons.IRemotable;
import jadex.commons.future.IFuture;

import java.util.Map;


/**
 *  The interface for platform services.
 */
@Reference
public interface IService //extends IRemotable
{
	//-------- constants --------
	
	/** Empty service array. */
	public static final IService[] EMPTY_SERVICES = new IService[0];
	
	//-------- methods --------

	/**
	 *  Get the service identifier.
	 *  @return The service identifier.
	 */
	public IServiceIdentifier getServiceIdentifier();
	
	/**
	 *  Test if the service is valid.
	 *  @return True, if service can be used.
	 */
	public IFuture<Boolean> isValid();
	
	/**
	 *  Get the map of properties (considered as constant).
	 *  @return The service property map (if any).
	 */
	public Map getPropertyMap();
}
