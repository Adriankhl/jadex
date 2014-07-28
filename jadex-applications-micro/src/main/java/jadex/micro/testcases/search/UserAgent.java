package jadex.micro.testcases.search;

import jadex.base.Starter;
import jadex.bridge.IComponentIdentifier;
import jadex.bridge.IExternalAccess;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.bridge.service.search.SServiceProvider;
import jadex.bridge.service.types.cms.IComponentManagementService;
import jadex.commons.future.DefaultTuple2ResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.commons.future.ThreadSuspendable;
import jadex.micro.MicroAgent;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.AgentCreated;
import jadex.micro.annotation.Binding;
import jadex.micro.annotation.RequiredService;
import jadex.micro.annotation.RequiredServices;

import java.util.Collection;
import java.util.Map;

@Agent
@RequiredServices({@RequiredService(name = "testService", type = ITestService.class, multiple = true, 
	binding = @Binding(scope = RequiredServiceInfo.SCOPE_PLATFORM)) })
public class UserAgent 
{
    @Agent
    protected MicroAgent agent;
    
    @AgentCreated
    public void init() 
    {
        System.out.println("Agent created");
        IFuture<Collection<ITestService>> fut = agent.getServiceContainer().getRequiredServices("testService");
        Collection<ITestService> sers = fut.get();
        System.out.println("fetched all available services: "+sers.size());
    }
    
    public static void main(String[] args)
	{
    	ThreadSuspendable sus = new ThreadSuspendable();
		IExternalAccess plat = Starter.createPlatform(new String[]{"-gui", "false"}).get(sus);
		IComponentManagementService cms = SServiceProvider.getService(plat.getServiceProvider(), IComponentManagementService.class, RequiredServiceInfo.SCOPE_PLATFORM).get(sus);
		
		final Future<Void> fut = new Future<Void>();
		
		final int max = 2500;
		final int[] cnt = new int[1];
		for(int i=0; i<max; i++)
		{
			cms.createComponent(ProviderAgent.class.getName()+".class", null).addResultListener(new DefaultTuple2ResultListener<IComponentIdentifier, Map<String, Object>>()
			{
				public void firstResultAvailable(IComponentIdentifier result)
				{
					cnt[0]++;
					System.out.println("created: "+result+" "+cnt[0]);
					if(cnt[0]==max)
					{
						fut.setResult(null);
					}
				}
				
				public void secondResultAvailable(Map<String, Object> result)
				{
				}
				
				public void exceptionOccurred(Exception exception)
				{
					fut.setExceptionIfUndone(exception);
				}
			});
		}
		
		cms.createComponent(UserAgent.class.getName()+".class", null).get(sus);
	}
}