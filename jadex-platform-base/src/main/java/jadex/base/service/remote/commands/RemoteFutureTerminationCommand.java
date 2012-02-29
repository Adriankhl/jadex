package jadex.base.service.remote.commands;

import jadex.base.service.remote.IRemoteCommand;
import jadex.base.service.remote.RemoteServiceManagementService;
import jadex.bridge.service.annotation.Security;
import jadex.commons.future.IIntermediateFuture;
import jadex.commons.future.ITerminableFuture;
import jadex.commons.future.ITerminableIntermediateFuture;
import jadex.commons.future.IntermediateFuture;
import jadex.micro.IMicroExternalAccess;

/**
 *  Command for executing a remote method.
 */
public class RemoteFutureTerminationCommand extends AbstractRemoteCommand
{
	//-------- attributes --------
	
	/** The call identifier. */
	protected String callid;
	
	/** The call identifier to terminate. */
	protected String terminatecallid;
	
	//-------- constructors --------
	
	/**
	 *  Create a new remote method invocation command.
	 */
	public RemoteFutureTerminationCommand()
	{
	}
	
	/**
	 *  Create a new remote method invocation command. 
	 */
	public RemoteFutureTerminationCommand(String callid, String terminatecallid)
	{
		this.callid = callid;
		this.terminatecallid = terminatecallid;
//		System.out.println("rmi on client: "+callid+" "+methodname);
	}
	
	//-------- methods --------
	
	/**
	 *  Get the security level of the request.
	 */
	public String	getSecurityLevel()
	{
		// No security issues here.
		return Security.UNRESTRICTED;
	}

	/**
	 *  Execute the command.
	 *  @param lrms The local remote management service.
	 *  @return An optional result command that will be 
	 *  sent back to the command origin. 
	 */
	public IIntermediateFuture<IRemoteCommand> execute(IMicroExternalAccess component, final RemoteServiceManagementService rsms)
	{
		final IntermediateFuture<IRemoteCommand> ret = new IntermediateFuture<IRemoteCommand>();
		
		// RMS acts as representative of remote caller.
//		System.out.println("callid: "+callid);
//		System.out.println("terminatecallid: "+terminatecallid);
		Object tfut = rsms.getProcessingCall(terminatecallid);
		if(tfut!=null)
		{
			System.out.println("terminating remote future: "+tfut.hashCode());
			if(tfut instanceof ITerminableFuture)
				((ITerminableFuture)tfut).terminate();
			else
				((ITerminableIntermediateFuture)tfut).terminate();
		}
		else
		{
//			System.out.println("remote future not found");
			rsms.addTerminationCommand(terminatecallid, new Runnable()
			{
				public void run()
				{
					Object tfut = rsms.getProcessingCall(terminatecallid);
					if(tfut!=null)
					{
//						System.out.println("terminated future afterwards");
						if(tfut instanceof ITerminableFuture)
							((ITerminableFuture)tfut).terminate();
						else
							((ITerminableIntermediateFuture)tfut).terminate();
					}
				}
			});
		}
		
		ret.addIntermediateResult(new RemoteResultCommand(null, null, callid, false));
		ret.setFinished();
		return ret;
	}

	//-------- getter/setter methods --------

	/**
	 *  Get the callid.
	 *  @return the callid.
	 */
	public String getCallId()
	{
		return callid;
	}

	/**
	 *  Set the call id.
	 *  @param callid The call id to set.
	 */
	public void setCallId(String callid)
	{
//		System.out.println("rmi on server: "+callid);
		this.callid = callid;
	}

	/**
	 *  Get the terminate call id.
	 *  @return the terminate call id.
	 */
	public String getTerminateCallId()
	{
		return terminatecallid;
	}

	/**
	 *  Set the terminate call id.
	 *  @param terminatecallid The terminate call id to set.
	 */
	public void setTerminateCallId(String terminatecallid)
	{
		this.terminatecallid = terminatecallid;
	}
}

