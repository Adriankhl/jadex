package jadex.platform.service.registryv2;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import org.junit.Test;

import jadex.base.IPlatformConfiguration;
import jadex.base.Starter;
import jadex.base.test.util.STest;
import jadex.bridge.IComponentIdentifier;
import jadex.bridge.IExternalAccess;
import jadex.bridge.service.search.ServiceQuery;
import jadex.bridge.service.types.pawareness.IPassiveAwarenessService;
import jadex.commons.security.SSecurity;

/**
 *  Test basic search functionality with broadcast awareness only
 *  (i.e. no super peer).
 */
public class BroadcastAwarenessTest	extends AbstractSearchQueryTest
{
	//-------- constants --------
	
	/** Client configuration for platform used for searching. */
	public static final IPlatformConfiguration	CLIENTCONF;

	/** Plain provider configuration. */
	public static final IPlatformConfiguration	PROCONF;

	/** Fixed custom port for broadcast. */
	public static final int customport	= SSecurity.getSecureRandom().nextInt(Short.MAX_VALUE*2-1024)+1025;

	static
	{
		IPlatformConfiguration	baseconf	= STest.getDefaultTestConfig();
		baseconf.setValue("superpeerclient.awaonly", true);
//		baseconf.setValue("superpeerclient.pollingrate", WAITFACTOR/2); 	// -> 1500 millis.
		baseconf.setValue("passiveawarenessintravm", false);
		baseconf.setValue("passiveawarenessbroadcast", true);
		baseconf.setValue("passiveawarenessbroadcast.port", customport);
		baseconf.setDefaultTimeout(Starter.getScaledDefaultTimeout(null, WAITFACTOR*2));
		baseconf.getExtendedPlatformConfiguration().setDebugFutures(true);

		// Remote only -> no simulation please
		baseconf.getExtendedPlatformConfiguration().setSimul(false);
		baseconf.getExtendedPlatformConfiguration().setSimulation(false);
		
		CLIENTCONF	= baseconf.clone();
		CLIENTCONF.setPlatformName("client_*");
		
		PROCONF	= baseconf.clone();
		PROCONF.addComponent(GlobalProviderAgent.class);
		PROCONF.addComponent(LocalProviderAgent.class);
		PROCONF.setPlatformName("provider_*");
	}
	
	//-------- constructors --------
	
	/**
	 *  Create the test.
	 */
	public BroadcastAwarenessTest()
	{
		super(true, CLIENTCONF, PROCONF, null, null);
	}
	
	//-------- methods --------

	/**
	 *  Test bare awareness results.
	 */
	@Test
	public void	testBareAwareness()
	{
		IExternalAccess	client	= createPlatform(CLIENTCONF);		
		createPlatform(PROCONF);	
		createPlatform(PROCONF);
		
		IPassiveAwarenessService	pawa	= client.searchService(new ServiceQuery<>(IPassiveAwarenessService.class)).get();
		assertTrue("Found broadcast awareness? "+pawa, pawa.toString().toLowerCase().contains("broadcast"));
		
		Collection<IComponentIdentifier>	found	= pawa.searchPlatforms().get();
		assertEquals(found.toString(), 2, found.size());
	}
}
