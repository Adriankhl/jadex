package jadex.micro.tutorial;

import java.util.Map;

import jadex.bridge.IComponentIdentifier;
import jadex.bridge.IComponentStep;
import jadex.bridge.IInternalAccess;
import jadex.bridge.service.clock.IClockService;
import jadex.commons.future.DefaultResultListener;
import jadex.commons.future.IFuture;
import jadex.micro.MicroAgent;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.AgentArgument;
import jadex.micro.annotation.AgentBody;
import jadex.micro.annotation.Argument;
import jadex.micro.annotation.Arguments;
import jadex.micro.annotation.Binding;
import jadex.micro.annotation.Description;
import jadex.micro.annotation.Implementation;
import jadex.micro.annotation.ProvidedService;
import jadex.micro.annotation.ProvidedServices;
import jadex.micro.annotation.RequiredService;
import jadex.micro.annotation.RequiredServices;

/**
 *  Chat micro agent with a . 
 */
@Description("This agent provides a basic chat service.")
@Agent
@ProvidedServices(@ProvidedService(type=IChatService.class, 
	implementation=@Implementation(value=ChatServiceD5.class)))
@RequiredServices({
	@RequiredService(name="clockservice", type=IClockService.class, 
		binding=@Binding(scope=Binding.SCOPE_PLATFORM)),
	@RequiredService(name="chatservices", type=IChatService.class, multiple=true,
		binding=@Binding(dynamic=true, scope=Binding.SCOPE_GLOBAL)),
	@RequiredService(name="regservice", type=IRegistryServiceE3.class)
})
@Arguments(@Argument(name="nickname", clazz=String.class, defaultvalue="\"Willi\""))
public class ChatE3Agent
{
	/** The agent. */
	@Agent
	protected MicroAgent agent;
	
	/** The nickname. */
	@AgentArgument
	protected String nickname;
	
	/**
	 *  Execute the functional body of the agent.
	 *  Is only called once.
	 */
	@AgentBody
	public void executeBody()
	{
		IFuture<IRegistryServiceE3>	regservice	= agent.getServiceContainer().getRequiredService("regservice");
		regservice.addResultListener(new DefaultResultListener<IRegistryServiceE3>()
		{
			public void resultAvailable(final IRegistryServiceE3 rs)
			{
				rs.register(agent.getComponentIdentifier(), nickname);
				
				agent.waitFor(10000, new IComponentStep()
				{
					public Object execute(IInternalAccess ia)
					{
						rs.getChatters().addResultListener(new DefaultResultListener<Map<String, IComponentIdentifier>>()
						{
							public void resultAvailable(Map<String, IComponentIdentifier> result)
							{
								System.out.println("The current chatters: "+result);
							}
						});
						return null;
					}
				});
			}
		});
	}
}