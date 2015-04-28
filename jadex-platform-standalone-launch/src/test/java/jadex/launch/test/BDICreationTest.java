package jadex.launch.test;

import jadex.base.Starter;
import jadex.bridge.ComponentTerminatedException;
import jadex.bridge.IComponentIdentifier;
import jadex.bridge.IExternalAccess;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.bridge.service.search.SServiceProvider;
import jadex.bridge.service.types.cms.CreationInfo;
import jadex.bridge.service.types.cms.IComponentManagementService;
import jadex.commons.Tuple2;
import jadex.commons.future.DelegationResultListener;
import jadex.commons.future.ExceptionDelegationResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.ISuspendable;
import jadex.commons.future.ThreadSuspendable;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

/**
 *  Test if the bdi creation test works.
 */
public class BDICreationTest //extends TestCase
{
	/**
	 *  Test bdi v3 creation.
	 */
	@Test
	public void	testBDICreation() throws Exception
	{
		long timeout	= -1;//BasicService.getLocalDefaultTimeout();
		IExternalAccess	platform	= (IExternalAccess)Starter.createPlatform(new String[]{"-platformname", "benchmarks_*",
//			"-kernels", "\"micro\"",
			"-logging", "false",
			"-libpath", "new String[]{\""+new File("../jadex-applications-bdiv3/target/classes").toURI().toURL().toString()+"\"}",
			"-awareness", "false",	// otherwise influences performance measure
			"-gui", "false", "-saveonexit", "false", "-welcome", "false", //"-autoshutdown", "true",
//			"-componentfactory", "jadex.component.ComponentComponentFactory",
//			"-conf", "jadex.standalone.Platform.component.xml",
			"-printpass", "false"}).get(timeout);
		IComponentManagementService cms = (IComponentManagementService)SServiceProvider.getService(platform, IComponentManagementService.class, RequiredServiceInfo.SCOPE_PLATFORM).get(timeout);
		
		Future<Collection<Tuple2<String, Object>>>	fut	= new Future<Collection<Tuple2<String, Object>>>();
		Map<String, Object>	args	= new HashMap<String, Object>();
		args.put("max", Integer.valueOf(1000));
		cms.createComponent(null, "jadex.bdi.benchmarks.AgentCreation.agent.xml", new CreationInfo(args), new DelegationResultListener<Collection<Tuple2<String, Object>>>(fut))
			.addResultListener(new ExceptionDelegationResultListener<IComponentIdentifier, Collection<Tuple2<String, Object>>>(fut)
		{
			public void customResultAvailable(IComponentIdentifier result)
			{
				// Agent created. Kill listener waits for result.
			}
		});
		
		// 2 times timeout should do on all build servers. if test fails, check if platform has become slower ;-)
		Collection<Tuple2<String, Object>>	results	= fut.get(timeout*2);
		
//		// Write values to property files for hudson plot plugin.
//		Collection<Tuple2<String, Object>>	results	= fut.get(sus, timeout);
//		for(Iterator<Tuple2<String, Object>> it=results.iterator(); it.hasNext(); )
//		{
//			Tuple2<String, Object> tup = it.next();
////			String	key	= it.next();
////			Tuple	value	= (Tuple)results.get(key);
//			// Collect benchmark results (name/value tuple)
//			if(tup.getSecondEntity() instanceof Tuple)
//			{
//				try
//				{
////					FileWriter	fw	= new FileWriter(new File("../"+tup.getFirstEntity()+".properties"));
//					File file = new File("../"+tup.getFirstEntity()+".properties");
//					FileOutputStream fileout = new FileOutputStream(file);
//					Properties	props	=	new Properties();
//					props.setProperty("YVALUE", ""+((Tuple)tup.getSecondEntity()).get(0));
//					props.store(fileout, null);
//					fileout.close();
//				}
//				catch(IOException e)
//				{
//					System.out.println("Warning: could not save value: "+e);
//				}
//			}
//		}
		
		try
		{
			platform.killComponent().get(timeout);
		}
		catch(Exception e)
		{
			// Platform autoshutdown already finished.
			if(!(e instanceof ComponentTerminatedException))
			{
				e.printStackTrace();
			}
		}
		
		platform	= null;
		cms	= null;
		fut	= null;
		
//		try
//		{
//			Thread.sleep(3000000);
//		}
//		catch(InterruptedException e)
//		{
//		}
	}
	
	/**
	 *  Main for testing.
	 */
	public static void main(String[] args) throws Exception
	{
		BDICreationTest test = new BDICreationTest();
		test.testBDICreation();
	}
}
