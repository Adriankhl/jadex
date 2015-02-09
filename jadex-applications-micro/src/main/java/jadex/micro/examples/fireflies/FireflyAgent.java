package jadex.micro.examples.fireflies;

import jadex.bridge.IComponentStep;
import jadex.bridge.IInternalAccess;
import jadex.bridge.component.IExecutionFeature;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.bridge.service.component.IRequiredServicesFeature;
import jadex.commons.IFilter;
import jadex.commons.future.ExceptionDelegationResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.extension.envsupport.IEnvironmentService;
import jadex.extension.envsupport.environment.IEnvironmentSpace;
import jadex.extension.envsupport.environment.ISpaceAction;
import jadex.extension.envsupport.environment.ISpaceObject;
import jadex.extension.envsupport.environment.space2d.ContinuousSpace2D;
import jadex.extension.envsupport.environment.space2d.Space2D;
import jadex.extension.envsupport.math.IVector2;
import jadex.extension.envsupport.math.Vector1Int;
import jadex.extension.envsupport.math.Vector2Double;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.AgentBody;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 *  The firefly agent.
 */
@Agent
public class FireflyAgent
{
	/** The agent. */
	@Agent
	protected IInternalAccess agent;
	
	//-------- methods --------

//	/**
//	 *  Init method.
//	 */
//	public IFuture agentCreated()
//	{
//		throw new RuntimeException();
////		return super.agentCreated();
//	}
	
	/**
	 *  Execute an agent step.
	 */
	@AgentBody
	public IFuture<Void> executeBody()
	{
		final Future<Void>	ret	= new Future<Void>();
		
		agent.getComponentFeature(IRequiredServicesFeature.class).searchService(IEnvironmentService.class, RequiredServiceInfo.SCOPE_APPLICATION)
			.addResultListener(new ExceptionDelegationResultListener<IEnvironmentService, Void>(ret)
		{
			public void customResultAvailable(IEnvironmentService es)
			{
				es.getSpace().addResultListener(new ExceptionDelegationResultListener<IEnvironmentSpace, Void>(ret)
				{
					public void customResultAvailable(IEnvironmentSpace result)
					{
						final ContinuousSpace2D space = (ContinuousSpace2D)result;
							
						IComponentStep<Void> step = new IComponentStep<Void>()
						{
							public IFuture<Void> execute(IInternalAccess ia)
							{
								if(space==null)
									return IFuture.DONE;
								
								ISpaceObject avatar = space.getAvatar(agent.getComponentDescription());
								IVector2 mypos = (IVector2)avatar.getProperty(Space2D.PROPERTY_POSITION);
								double dir = ((Number)avatar.getProperty("direction")).doubleValue();
								int clock = ((Number)avatar.getProperty("clock")).intValue();
								int threshold = ((Number)avatar.getProperty("threshold")).intValue();
								int window = ((Number)avatar.getProperty("window")).intValue();
								int resetlevel = ((Number)avatar.getProperty("reset_level")).intValue();

								int flashestoreset = ((Number)space.getProperty("flashes_to_reset")).intValue();
								int cyclelength = ((Number)space.getProperty("cycle_length")).intValue();
								
								// move
								// change direction slightly
								double factor = 10;
								double rotchange = Math.random()*Math.PI/factor-Math.PI/2/factor;
								
								double newdir = dir+rotchange;
								if(newdir<0)
									newdir+=Math.PI*2;
								else if(newdir>Math.PI*2)
									newdir-=Math.PI*2;
								
								// convert to vector
								// normally x=cos(dir) and y=sin(dir)
								// here 0 degree is 12 o'clock and the rotation right
								double x = Math.sin(newdir);
								double y = -Math.cos(newdir);
//											double x = Math.sin(newdir);
//											double y = Math.cos(newdir);
								double stepwidth = 0.1;
								IVector2 newdirvec = new Vector2Double(x*stepwidth, y*stepwidth);
								IVector2 newpos = mypos.copy().add(newdirvec);
								
								// Increment clock (internal counter)
								clock++;
								if(clock == cyclelength)
									clock = 0;
								
								if(clock>window && clock>=threshold)
								{
									// Look
									// if count turtles in-radius 1 with [color = yellow] >= flashes-to-reset
								    // [ set clock reset-level ]
								    Set tmp = Collections.EMPTY_SET;
								    
								    tmp = space.getNearObjects((IVector2)avatar.getProperty(
										Space2D.PROPERTY_POSITION), new Vector1Int(1), "firefly", new IFilter()
										{
											public boolean filter(Object obj)
											{
												ISpaceObject fly = (ISpaceObject)obj;
												return ((Boolean)fly.getProperty("flashing")).booleanValue();
											}
										});
									tmp.remove(avatar);
									if(tmp.size()>=flashestoreset)
									{
										clock = resetlevel;
//													System.out.println("Reset: "+avatar.getId());
									}
								}
					
								Map params = new HashMap();
								params.put(ISpaceAction.OBJECT_ID, avatar.getId());
								params.put(MoveAction.PARAMETER_POSITION, newpos);
								params.put(MoveAction.PARAMETER_DIRECTION, Double.valueOf(newdir));
								params.put(MoveAction.PARAMETER_CLOCK, Integer.valueOf(clock));
								space.performSpaceAction("move", params, null);
								
								agent.getComponentFeature(IExecutionFeature.class).waitForTick(this);
								return IFuture.DONE;
							}
							
							public String toString()
							{
								return "firebug.body()";
							}
						};
						
						agent.getComponentFeature(IExecutionFeature.class).waitForTick(step);
					}
				});				
			}
		});
		
		return ret; // never kill!
	}
}
