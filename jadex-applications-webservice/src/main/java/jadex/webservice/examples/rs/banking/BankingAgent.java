package jadex.webservice.examples.rs.banking;

import jadex.bridge.service.types.publish.IPublishService;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.Implementation;
import jadex.micro.annotation.NameValue;
import jadex.micro.annotation.ProvidedService;
import jadex.micro.annotation.ProvidedServices;
import jadex.micro.annotation.Publish;

/**
 *  Banking agent that offers a banking service.
 * 
 *  This example shows how a Jadex service can be automatically published
 *  as web service with a synchronous interfaces.
 *  
 *  The @Publish annotation requires the publishing type (e.g. web service),
 *  the id (e.g. a web service url) and the service type, i.e. interface to
 *  be specified.
 */
@Agent
//@Imports({"javax.ws.rs.core.MediaType"})
@ProvidedServices(
{	
	//a) Generate everything (no own implementation)
	@ProvidedService(name="banking1", type=IBankingService.class, implementation=@Implementation(BankingService.class),
		publish=@Publish(publishtype=IPublishService.PUBLISH_RS, publishid="http://localhost:8080/banking1",
		properties=@NameValue(name="formats", value="new javax.ws.rs.core.MediaType[]{javax.ws.rs.core.MediaType.APPLICATION_XML_TYPE, javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE}")))
	
//	// b) Use custom service class (no generation) Note: the publish id here is taken from the implementation class directly
//	@ProvidedService(name="banking2", type=IBankingService.class, implementation=@Implementation(BankingService.class),
//		publish=@Publish(publishtype=IPublishService.PUBLISH_RS, publishid="http://localhost:8080/banking2", mapping=RSBankingService.class, 
//		properties=@NameValue(name="generate", value="false"))),
//
//	// c) Use custom service class (generation of additional methods)
//	@ProvidedService(name="banking3", type=IBankingService.class, implementation=@Implementation(BankingService.class),
//		publish=@Publish(publishtype=IPublishService.PUBLISH_RS, publishid="http://localhost:8080/banking3", mapping=RSBankingService.class)),
//
//	// d) Use annotated interface instead of implementation (generation of interface implementation)
//	@ProvidedService(name="banking3", type=IBankingService.class, implementation=@Implementation(BankingService.class),
//		publish=@Publish(publishtype=IPublishService.PUBLISH_RS, publishid="http://localhost:8080/banking4", mapping=IRSBankingService.class,
//		properties=@NameValue(name="generate", value="true")))
})
public class BankingAgent
{
}