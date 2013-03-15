package jadex.platform.service.monitoring;

import jadex.bridge.service.types.monitoring.IMonitoringService;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.Implementation;
import jadex.micro.annotation.ProvidedService;
import jadex.micro.annotation.ProvidedServices;

/**
 *  Default monitoring agent. 
 */
@Agent
@ProvidedServices(@ProvidedService(type=IMonitoringService.class, implementation=@Implementation(MonitoringService.class)))
public class MonitoringAgent
{
}
