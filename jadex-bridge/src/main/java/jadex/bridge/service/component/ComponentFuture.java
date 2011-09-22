package jadex.bridge.service.component;

import jadex.bridge.IComponentAdapter;
import jadex.bridge.IComponentStep;
import jadex.bridge.IExternalAccess;
import jadex.bridge.IInternalAccess;
import jadex.bridge.JadexCloner;
import jadex.bridge.service.SServiceProvider;
import jadex.commons.future.DelegationResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.commons.future.IResultListener;

/**
 *  The component future ensures that future result/exception notifications
 *  are executed on the calling component thread.
 */
public class ComponentFuture<E> extends Future<E>
{
	//-------- attributes --------
	
	/** The adapter. */
	protected IComponentAdapter	adapter;
	
	/** The external acces. */
	protected IExternalAccess	ea;
	
	/** The result copy flag. */
	protected boolean copy;
	
	/** The source. */
//	protected IFuture source;
	
	//-------- constructors --------
	
	/**
	 *  Create a new future.
	 */
	public ComponentFuture(IExternalAccess ea, IComponentAdapter adapter, IFuture source, boolean copy)
	{
		this.ea	= ea;
		this.adapter	= adapter;
		this.copy = copy;
//		this.source = source;
		source.addResultListener(new DelegationResultListener<E>(this));
	}
	
	/**
	 *  Schedule listener notification on component thread. 
	 */
	protected void notifyListener(final IResultListener<E> listener)
	{
		// Hack!!! Notify multiple listeners at once?
		if(adapter.isExternalThread())
		{
			ea.scheduleStep(new IComponentStep<Void>()
			{
				public IFuture<Void> execute(IInternalAccess ia)
				{
					ComponentFuture.super.notifyListener(listener);
					return IFuture.DONE;
				}
			});
		}
		else
		{
			super.notifyListener(listener);
		}
	}
	
	/**
     *  Set the result. 
     *  Listener notifications occur on calling thread of this method.
     *  @param result The result.
     */
    public void	setResult(E result)
    {
		// Copy result if
		// - copy flag is true
		// - and result is not a reference object
    	if(copy && result!=null)
		{
			boolean copy = !SServiceProvider.isLocalReference(result);
			if(copy)
			{
//				System.out.println("copy result: "+result);
				result = (E)JadexCloner.deepCloneObject(result);
			}
		}
		super.setResult(result);
    }
 
//    /**
//     *  Test if done, i.e. result is available.
//     *  @return True, if done.
//     */
//    public synchronized boolean isDone()
//    {
//    	return source.isDone();
//    }
}