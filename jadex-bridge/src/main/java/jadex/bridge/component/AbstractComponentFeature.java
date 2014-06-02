package jadex.bridge.component;

import jadex.bridge.IInternalAccess;
import jadex.commons.future.IFuture;

import java.util.Collections;
import java.util.Set;

/**
 *  Feature for provided services.
 */
public abstract class AbstractComponentFeature	implements IComponentFeature
{
	//-------- attributes --------
	
	/** The component. */
	protected IInternalAccess	component;
	
	/** The creation info. */
	protected ComponentCreationInfo	cinfo;
		
	//-------- constructors --------
	
	/**
	 *  Bean constructor for type level.
	 */
	public AbstractComponentFeature()
	{
	}
	
	/**
	 *  Factory method constructor for instance level.
	 */
	protected AbstractComponentFeature(IInternalAccess component, ComponentCreationInfo cinfo)
	{
		this.component	= component;
		this.cinfo	= cinfo;
	}
	
	//-------- IComponentFeature interface / type level --------
	
	/**
	 *  Get the predecessors, i.e. features that should be inited first.
	 */
	public Set<Class<? extends IComponentFeature>>	getPredecessors()
	{
		return Collections.emptySet();
	}
	
	/**
	 *  Get the successors, i.e. features that should be inited after this feature.
	 */
	public Set<Class<? extends IComponentFeature>>	getSuccessors()
	{
		return Collections.emptySet();
	}
	
	/**
	 *  Get the user interface type of the feature.
	 */
	public abstract Class<?>	getType();
	
	/**
	 *  Create an instance of the feature.
	 *  @param access	The access of the component.
	 *  @param info	The creation info.
	 */
	public abstract IComponentFeature	createInstance(IInternalAccess access, ComponentCreationInfo info);
	
	//-------- IComponentFeature interface / instance level --------
	
	/**
	 *  Initialize the feature.
	 *  Empty implementation that can be overridden.
	 */
	public IFuture<Void>	init()
	{
		return IFuture.DONE;
	}
	
	//-------- IValueFetcher interface --------
	
	/**
	 *  Fetch a value via its name.
	 *  Empty default method to be overridden by subclasses. 
	 *  @param name The name.
	 *  @return The value.
	 */
	public Object fetchValue(String name)
	{
		throw new UnsupportedOperationException();
	}
	
	/**
	 *  Fetch a value via its name from an object.
	 *  Empty default method to be overridden by subclasses. 
	 *  @param name The name.
	 *  @param object The object.
	 *  @return The value.
	 */
	public Object fetchValue(String name, Object object)
	{
		throw new UnsupportedOperationException();
	}
}