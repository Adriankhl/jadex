package jadex.wfms.service;

import jadex.bridge.IComponentIdentifier;
import jadex.bridge.service.IServiceContainer;
import jadex.bridge.service.SServiceProvider;
import jadex.commons.ICommand;
import jadex.commons.future.DelegationResultListener;
import jadex.commons.future.Future;

import java.security.AccessControlException;

public class AccessControlCheck
{
	private Integer[] actions;
	private IComponentIdentifier client;
	
	public AccessControlCheck(IComponentIdentifier client, Integer[] actions)
	{
		this.actions = actions;
		this.client = client;
	}
	
	public AccessControlCheck(IComponentIdentifier client, Integer action)
	{
		this(client, new Integer[] { action });
	}
	
	public void checkAccess(final Future targetFuture, IServiceContainer provider, final ICommand actionCommand)
	{
		SServiceProvider.getService(provider, IAAAService.class)
			.addResultListener(new DelegationResultListener(targetFuture)
		{
			public void customResultAvailable(Object result)
			{
				for (int i = 0; i < actions.length; ++i)
					if (!((IAAAService) result).accessAction(client, actions[i]))
					{
						targetFuture.setException(new AccessControlException("Not allowed: "+client + " " + actions[i]));
						return;
					}
				
				actionCommand.execute(actions);	
			}
		});
	}
}
