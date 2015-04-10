package jadex.micro.benchmarks;

import jadex.base.Starter;
import jadex.bridge.BasicComponentIdentifier;
import jadex.bridge.IComponentIdentifier;
import jadex.bridge.IComponentStep;
import jadex.bridge.IExternalAccess;
import jadex.bridge.IInternalAccess;
import jadex.bridge.component.IArgumentsFeature;
import jadex.bridge.component.IExecutionFeature;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.bridge.service.search.SServiceProvider;
import jadex.bridge.service.types.clock.IClockService;
import jadex.bridge.service.types.cms.CreationInfo;
import jadex.bridge.service.types.cms.IComponentManagementService;
import jadex.commons.Tuple;
import jadex.commons.future.DefaultResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.commons.future.ThreadSuspendable;
import jadex.commons.transformation.annotations.Classname;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.AgentBody;
import jadex.micro.annotation.Argument;
import jadex.micro.annotation.Arguments;
import jadex.micro.annotation.Description;
import jadex.micro.features.IMicroLifecycleFeature;

import java.util.HashMap;
import java.util.Map;

/**
 *  Agent creation benchmark. 
 */
@Description("This agents benchmarks agent creation and termination.")
@Arguments(
{
	@Argument(name="max", clazz=Integer.class, defaultvalue="10000", description="Maximum number of agents to create."),
	@Argument(name="nested", clazz=Boolean.class, defaultvalue="Boolean.FALSE", description="If true, each agent is created as a subcomponent of the previous agent.")
})
@Agent
public class AgentCreationAgent 
{
	//-------- attributes --------
	
	/** The agent. */
	@Agent
	protected IInternalAccess agent;
	
	//-------- methods --------
		
	/**
	 *  Execute an agent step.
	 */
	@AgentBody
	public IFuture<Void> executeBody()
	{
//		System.out.println("body");
		
		Map arguments = agent.getComponentFeature(IArgumentsFeature.class).getArguments();	
		if(arguments==null)
			arguments = new HashMap();
		final Map args = arguments;	
		
		if(args.get("num")==null)
		{
			IClockService clock = SServiceProvider.getLocalService(agent, IClockService.class, RequiredServiceInfo.SCOPE_PLATFORM);
			System.gc();
			try
			{
				Thread.sleep(500);
			}
			catch(InterruptedException e){}
			
			Long startmem = Long.valueOf(Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory());
			Long starttime = Long.valueOf(clock.getTime());
			args.put("num", Integer.valueOf(1));
			args.put("startmem", startmem);
			args.put("starttime", starttime);
			
			step1(args);
		}
		else
		{
			step1(args);
		}
		
		return new Future<Void>(); // never kill?!
	}

	/**
	 *  Execute the first step.
	 */
	protected void step1(final Map args)
	{
		final int num = ((Integer)args.get("num")).intValue();
		final int max = ((Integer)args.get("max")).intValue();
		final boolean nested = ((Boolean)args.get("nested")).booleanValue();
		
		System.out.println("Created peer: "+num);
		
		if(num<max)
		{
			args.put("num", Integer.valueOf(num+1));
//			System.out.println("Args: "+num+" "+args);

			IComponentManagementService cms = SServiceProvider.getLocalService(agent, IComponentManagementService.class, RequiredServiceInfo.SCOPE_PLATFORM);
			cms.createComponent(createPeerName(num+1, agent.getComponentIdentifier()), AgentCreationAgent.this.getClass().getName()+".class",
				new CreationInfo(null, args, nested ? agent.getComponentIdentifier() : null, null, null, null, null, null, null, null, null, null, agent.getComponentDescription().getResourceIdentifier()), null);
		}
		else
		{
			IClockService clock = SServiceProvider.getLocalService(agent, IClockService.class, RequiredServiceInfo.SCOPE_PLATFORM);
			final long end = clock.getTime();
			
			System.gc();
			try
			{
				Thread.sleep(500);
			}
			catch(InterruptedException e){}
			final long used = Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();
			
			Long startmem = (Long)args.get("startmem");
			final Long starttime = (Long)args.get("starttime");
			final long omem = (used-startmem.longValue())/1024;
			final double upera = ((long)(1000*(used-startmem.longValue())/max/1024))/1000.0;
			System.out.println("Overall memory usage: "+omem+"kB. Per agent: "+upera+" kB.");
			System.out.println("Last peer created. "+max+" agents started.");
			final double dur = ((double)end-starttime.longValue())/1000.0;
			final double pera = dur/max;
			System.out.println("Needed: "+dur+" secs. Per agent: "+pera+" sec. Corresponds to "+(1/pera)+" agents per sec.");
		
			// Delete prior agents.
//					if(!nested)
//					{
//						deletePeers(max-1, clock.getTime(), dur, pera, omem, upera, max, (IMicroExternalAccess)getExternalAccess(), nested);
//					}
			
			// If nested, use initial component to kill others
//			else
			{
				IComponentManagementService cms = SServiceProvider.getLocalService(agent, IComponentManagementService.class, RequiredServiceInfo.SCOPE_PLATFORM);
				String	initial	= createPeerName(1, agent.getComponentIdentifier());
				IComponentIdentifier	cid	= new BasicComponentIdentifier(initial, agent.getComponentIdentifier().getRoot());
				cms.getExternalAccess(cid).addResultListener(agent.getComponentFeature(IExecutionFeature.class).createResultListener(new DefaultResultListener<IExternalAccess>()
				{
					public void resultAvailable(IExternalAccess exta)
					{
						exta.scheduleStep(new IComponentStep<Void>()
						{
							@Classname("deletePeers")
							public IFuture<Void> execute(final IInternalAccess ia)
							{
								IClockService clock = SServiceProvider.getLocalService(agent, IClockService.class, RequiredServiceInfo.SCOPE_PLATFORM);
								((AgentCreationAgent)ia.getComponentFeature(IMicroLifecycleFeature.class).getPojoAgent()).deletePeers(max, clock.getTime(), dur, pera, omem, upera, max, nested);
								return IFuture.DONE;
							}
						});
					}
				}));
			}
		}
	}

	/**
	 *  Create a name for a peer with a given number.
	 */
	protected String createPeerName(int num, IComponentIdentifier cid)
	{
		String	name = cid.getLocalName();
		int	index	= name.indexOf("Peer_#");
		if(index!=-1)
		{
			name	= name.substring(0, index);
		}
		if(num!=1)
		{
			name	+= "Peer_#"+num;
		}
		return name;
	}
	
	/**
	 *  Delete all peers from last-1 to first.
	 *  @param cnt The highest number of the agent to kill.
	 */
	protected void deletePeers(final int cnt, final long killstarttime, final double dur, final double pera,
		final long omem, final double upera, final int max, final boolean nested)
	{
		final String name = createPeerName(cnt, agent.getComponentIdentifier());
//		System.out.println("Destroying peer: "+name);
		IComponentManagementService cms = SServiceProvider.getLocalService(agent, IComponentManagementService.class, RequiredServiceInfo.SCOPE_PLATFORM);
		IComponentIdentifier aid = new BasicComponentIdentifier(name, agent.getComponentIdentifier().getRoot());
		cms.destroyComponent(aid).addResultListener(agent.getComponentFeature(IExecutionFeature.class).createResultListener(new DefaultResultListener<Map<String, Object>>()
		{
			public void resultAvailable(Map<String, Object> results)
			{
				System.out.println("Successfully destroyed peer: "+name);
				
				if(cnt-1>1)
				{
					deletePeers(cnt-1, killstarttime, dur, pera, omem, upera, max, nested);
				}
				else
				{
					killLastPeer(max, killstarttime, dur, pera, omem, upera);
				}
			}
		}));
	}
	
	/**
	 *  Kill the last peer and print out the results.
	 */
	protected void killLastPeer(final int max, final long killstarttime, final double dur, final double pera, 
		final long omem, final double upera)
	{
		IClockService cs = SServiceProvider.getLocalService(agent, IClockService.class, RequiredServiceInfo.SCOPE_PLATFORM);
		long killend = cs.getTime();
		System.out.println("Last peer destroyed. "+(max-1)+" agents killed.");
		double killdur = ((double)killend-killstarttime)/1000.0;
		final double killpera = killdur/(max-1);
		
		Runtime.getRuntime().gc();
		try
		{
			Thread.sleep(500);
		}
		catch(InterruptedException e){}
		long stillused = (Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/1024;
		
		System.out.println("\nCumulated results:");
		System.out.println("Creation needed: "+dur+" secs. Per agent: "+pera+" sec. Corresponds to "+(1/pera)+" agents per sec.");
		System.out.println("Killing needed:  "+killdur+" secs. Per agent: "+killpera+" sec. Corresponds to "+(1/killpera)+" agents per sec.");
		System.out.println("Overall memory usage: "+omem+"kB. Per agent: "+upera+" kB.");
		System.out.println("Still used memory: "+stillused+"kB.");
		
		agent.getComponentFeature(IArgumentsFeature.class).getResults().put("microcreationtime", new Tuple(""+pera, "s"));
		agent.getComponentFeature(IArgumentsFeature.class).getResults().put("microkillingtime", new Tuple(""+killpera, "s"));
		agent.getComponentFeature(IArgumentsFeature.class).getResults().put("micromem", new Tuple(""+upera, "kb"));
		agent.killComponent();
	}
	
	/**
	 *  Main for testing.
	 */
	public static void main(String[] args)
	{
		IExternalAccess ea = Starter.createPlatform(new String[]
		{
//			"-logging", "true",
			"-gui", "false",
			"-extensions", "null",
			"-cli", "false",
//			"-awareness", "false"
		}).get();
		IComponentManagementService cms = SServiceProvider.getService(ea, IComponentManagementService.class, RequiredServiceInfo.SCOPE_PLATFORM).get();
		cms.createComponent(AgentCreationAgent.class.getName()+".class", null).get();
	}	
}
