package jadex.platform.service.parallelizer;

import jadex.bridge.service.RequiredServiceInfo;
import jadex.micro.MicroAgent;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.Argument;
import jadex.micro.annotation.Arguments;
import jadex.micro.annotation.Binding;
import jadex.micro.annotation.ComponentType;
import jadex.micro.annotation.ComponentTypes;
import jadex.micro.annotation.CreationInfo;
import jadex.micro.annotation.RequiredService;
import jadex.micro.annotation.RequiredServices;
import jadex.platform.service.servicepool.IServicePoolService;

@Agent
@Arguments(
{
	@Argument(name="workermodel", clazz=String.class, description="The worker agent model."),
	@Argument(name="servicetype", clazz=Class.class, description="The service interface."),
	@Argument(name="serviceimpl", clazz=Class.class, description="The service implementation.")
})
@RequiredServices(
{
	@RequiredService(name="poolser", type=IServicePoolService.class, binding=@Binding(
		scope=RequiredServiceInfo.SCOPE_PLATFORM, create=true, 
		creationinfo=@CreationInfo(type="spa"))),
})
@ComponentTypes(@ComponentType(name="spa", filename="jadex.platform.service.servicepool.ServicePoolAgent.class"))
public class ParallelizerAgent
{
	//-------- attributes --------
	
	@Agent
	protected MicroAgent agent;

//	/**
//	 * 
//	 */
//	public 
}
