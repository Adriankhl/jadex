package jadex.base.service.remote.commands;

import jadex.base.service.remote.IRemoteCommand;
import jadex.base.service.remote.RemoteReferenceModule;
import jadex.bridge.IComponentIdentifier;
import jadex.bridge.IInternalAccess;
import jadex.bridge.service.IServiceIdentifier;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.bridge.service.search.ServiceNotFoundException;
import jadex.bridge.service.types.security.DefaultAuthorizable;
import jadex.bridge.service.types.security.ISecurityService;
import jadex.commons.future.DelegationResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.commons.future.IResultListener;

/**
 *  Default base class for remote commands.
 */
public abstract class AbstractRemoteCommand	extends DefaultAuthorizable	implements IRemoteCommand
{
	/** The receiver (for processing the command in rmipreprocessor, will not be transferred). */
	protected IComponentIdentifier receiver;
	
	//-------- constructors --------
	
	/**
	 *  Bean constructor.
	 */
	public AbstractRemoteCommand()//IComponentIdentifier receiver)
	{
//		this.receiver = receiver;
	}
	
	//-------- methods --------
	
	/**
	 *  Preprocess command and replace if they are remote references.
	 */
	public IFuture<Void>	preprocessCommand(IInternalAccess component, RemoteReferenceModule rrm, final IComponentIdentifier target)
	{
		// Hack needed for rmi preprocessor
		this.receiver = target;
		
		final Future<Void>	ret	= new Future<Void>();
		component.getServiceContainer().searchService(ISecurityService.class, RequiredServiceInfo.SCOPE_PLATFORM)
			.addResultListener(new IResultListener<ISecurityService>()
		{
			public void resultAvailable(ISecurityService sec)
			{
				sec.preprocessRequest(AbstractRemoteCommand.this, target)
					.addResultListener(new DelegationResultListener<Void>(ret));
			}
			
			public void exceptionOccurred(Exception exception)
			{
				if(exception instanceof ServiceNotFoundException)
				{
					ret.setResult(null);
				}
				else
				{
					ret.setException(exception);
				}
			}
		});
		return ret;
	}
	
	/**
	 *  Post-process a received command before execution
	 *  for e.g. setting security level.
	 */
	public IFuture<Void>	postprocessCommand(IInternalAccess component, RemoteReferenceModule rrm, final IComponentIdentifier target)
	{
		return IFuture.DONE;
	}

	/**
	 *  Get the receiver (rms of other side).
	 *  @return the receiver.
	 */
	public IComponentIdentifier getReceiver()
	{
		return receiver;
	}
	
	/**
	 *  Get the sender component (if other than rms).
	 */
	public IComponentIdentifier getSender()
	{
		return null;
	}
	
	/**
	 *  Get the real receiver (other than rms).
	 *  @return the real receiver.
	 */
	public IComponentIdentifier getRealReceiver()
	{
		return null;
	}
}
