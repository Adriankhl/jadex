package jadex.standalone.service;

import jadex.bridge.IComponentInstance;
import jadex.bridge.IExternalAccess;
import jadex.bridge.service.annotation.Service;
import jadex.bridge.service.types.factory.IComponentAdapter;
import jadex.bridge.service.types.factory.IComponentAdapterFactory;
import jadex.bridge.service.types.factory.IComponentFactory;
import jadex.commons.future.DelegationResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.standalone.ComponentAdapterFactory;
import jadex.standalone.StandaloneComponentAdapter;

/**
 *  Standalone implementation of component execution service.
 */
@Service
public class DecoupledComponentManagementService extends jadex.base.service.cms.DecoupledComponentManagementService
{
	//-------- attributes --------
	
	/** The adapter factory. */
	protected ComponentAdapterFactory adapterfactory = new ComponentAdapterFactory();
	
	//-------- constructors --------

	/**
	 *  Create a new component execution service.
	 *  @param exta	The service provider.
	 */
	public DecoupledComponentManagementService(IComponentAdapter root)
	{
		super(root);
	}
	
	/**
	 *  Create a new component execution service.
	 *  @param exta	The service provider.
	 */
	public DecoupledComponentManagementService(IComponentAdapter root, IComponentFactory factory, boolean copy)
	{
		super(root, factory, copy);
	}
	
	/**
	 *  Get the component instance from an adapter.
	 */
	public IComponentInstance getComponentInstance(IComponentAdapter adapter)
	{
		return ((StandaloneComponentAdapter)adapter).getComponentInstance();
	}

	/**
	 *  Get the component adapter factory.
	 */
	public IComponentAdapterFactory getComponentAdapterFactory()
	{
		return adapterfactory;
	}
	
	/**
	 *  Invoke kill on adapter.
	 */
	public IFuture<Void> killComponent(IComponentAdapter adapter)
	{
		Future<Void> ret = new Future<Void>();
		((StandaloneComponentAdapter)adapter).killComponent()
			.addResultListener(new DelegationResultListener<Void>(ret));
		return ret;
	}
	
	/**
	 *  Cancel the execution.
	 */
	public IFuture<Void> cancel(IComponentAdapter adapter)
	{
		Future<Void> ret = new Future<Void>();
		getExecutionService().cancel((StandaloneComponentAdapter)adapter)
			.addResultListener(new DelegationResultListener<Void>(ret));
		return ret;
	}

	/**
	 *  Do a step.
	 */
	public IFuture doStep(IComponentAdapter adapter)
	{
		Future ret = new Future();
		((StandaloneComponentAdapter)adapter).doStep()
			.addResultListener(new DelegationResultListener(ret));
		return ret;
	}
}
