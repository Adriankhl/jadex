package jadex.bdiv3.features.impl;

import jadex.bdiv3.IBDIClassGenerator;
import jadex.bdiv3.annotation.PlanContextCondition;
import jadex.bdiv3.annotation.RawEvent;
import jadex.bdiv3.features.IBDIAgentFeature;
import jadex.bdiv3.features.impl.BDIAgentFeature.GoalsExistCondition;
import jadex.bdiv3.features.impl.BDIAgentFeature.LifecycleStateCondition;
import jadex.bdiv3.features.impl.BDIAgentFeature.PlansExistCondition;
import jadex.bdiv3.model.BDIModel;
import jadex.bdiv3.model.IBDIModel;
import jadex.bdiv3.model.MBelief;
import jadex.bdiv3.model.MCondition;
import jadex.bdiv3.model.MConfiguration;
import jadex.bdiv3.model.MDeliberation;
import jadex.bdiv3.model.MElement;
import jadex.bdiv3.model.MGoal;
import jadex.bdiv3.model.MPlan;
import jadex.bdiv3.model.MTrigger;
import jadex.bdiv3.runtime.ChangeEvent;
import jadex.bdiv3.runtime.impl.APL;
import jadex.bdiv3.runtime.impl.GoalFailureException;
import jadex.bdiv3.runtime.impl.RGoal;
import jadex.bdiv3.runtime.impl.RPlan;
import jadex.bdiv3.runtime.impl.RProcessableElement;
import jadex.bridge.ComponentTerminatedException;
import jadex.bridge.IComponentStep;
import jadex.bridge.IInternalAccess;
import jadex.bridge.component.ComponentCreationInfo;
import jadex.bridge.component.IComponentFeatureFactory;
import jadex.bridge.component.IExecutionFeature;
import jadex.bridge.component.ILifecycleComponentFeature;
import jadex.bridge.component.IPojoComponentFeature;
import jadex.bridge.component.ISubcomponentsFeature;
import jadex.bridge.component.impl.ComponentFeatureFactory;
import jadex.bridge.modelinfo.UnparsedExpression;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.bridge.service.annotation.CheckNotNull;
import jadex.bridge.service.component.IProvidedServicesFeature;
import jadex.bridge.service.component.IRequiredServicesFeature;
import jadex.bridge.service.search.SServiceProvider;
import jadex.bridge.service.types.clock.IClockService;
import jadex.bridge.service.types.clock.ITimedObject;
import jadex.commons.MethodInfo;
import jadex.commons.SReflect;
import jadex.commons.SUtil;
import jadex.commons.Tuple2;
import jadex.commons.future.DelegationResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.commons.future.IResultListener;
import jadex.javaparser.IParsedExpression;
import jadex.javaparser.SJavaParser;
import jadex.javaparser.SimpleValueFetcher;
import jadex.micro.features.impl.MicroLifecycleComponentFeature;
import jadex.rules.eca.ChangeInfo;
import jadex.rules.eca.EventType;
import jadex.rules.eca.IAction;
import jadex.rules.eca.ICondition;
import jadex.rules.eca.IEvent;
import jadex.rules.eca.IRule;
import jadex.rules.eca.MethodCondition;
import jadex.rules.eca.Rule;
import jadex.rules.eca.RuleSystem;
import jadex.rules.eca.annotations.CombinedCondition;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *  Feature that ensures the agent created(), body() and killed() are called on the pojo. 
 */
public class BDILifecycleAgentFeature extends MicroLifecycleComponentFeature implements IInternalBDILifecycleFeature
{
	/** The factory. */
	public static final IComponentFeatureFactory FACTORY = new ComponentFeatureFactory(ILifecycleComponentFeature.class, BDILifecycleAgentFeature.class,
		new Class<?>[]{IRequiredServicesFeature.class, IProvidedServicesFeature.class, ISubcomponentsFeature.class}, null, false);
	
	/** Is the agent inited and allowed to execute rules? */
	protected boolean inited;
	
	/**
	 *  Factory method constructor for instance level.
	 */
	public BDILifecycleAgentFeature(IInternalAccess component, ComponentCreationInfo cinfo)
	{
		super(component, cinfo);
	}
	
	/**
	 *  Execute the functional body of the agent.
	 *  Is only called once.
	 */
	public IFuture<Void> body()
	{
		startBehavior(getComponent());
		inited	= true;
		return super.body();
	}
	
	/**
	 *  Start the component behavior.
	 */
	public static void startBehavior(final IInternalAccess component)
	{
//		super.startBehavior();
		
//		final Object agent = microagent instanceof PojoBDIAgent? ((PojoBDIAgent)microagent).getPojoAgent(): microagent;
				
		final IInternalBDIAgentFeature bdif = (IInternalBDIAgentFeature)component.getComponentFeature(IBDIAgentFeature.class);
		final IBDIModel bdimodel = bdif.getBDIModel();
		final RuleSystem rulesystem = bdif.getRuleSystem();
		
		// Init bdi configuration
		String confname = component.getConfiguration();
		if(confname!=null)
		{
			MConfiguration mconf = bdimodel.getCapability().getConfiguration(confname);
			
			if(mconf!=null)
			{
				// only for pojo agents / xml is inited in beliefbase init
				if(bdimodel instanceof BDIModel)
				{
					// Set initial belief values
					List<UnparsedExpression> ibels = mconf.getInitialBeliefs();
					if(ibels!=null)
					{
						for(UnparsedExpression uexp: ibels)
						{
							try
							{
								MBelief mbel = bdimodel.getCapability().getBelief(uexp.getName());
								Object val = SJavaParser.parseExpression(uexp, component.getModel().getAllImports(), component.getClassLoader()).getValue(component.getFetcher());
		//						Field f = mbel.getTarget().getField(getClassLoader());
		//						f.setAccessible(true);
		//						f.set(agent, val);
								mbel.setValue(component, val);
							}
							catch(RuntimeException e)
							{
								throw e;
							}
							catch(Exception e)
							{
								throw new RuntimeException(e);
							}
						}
					}
				}
				
				// Create initial goals
				List<UnparsedExpression> igoals = mconf.getInitialGoals();
				if(igoals!=null)
				{
					for(UnparsedExpression uexp: igoals)
					{
						MGoal mgoal = null;
						Object goal = null;
						Class<?> gcl = null;
						
						// Create goal if expression available
						if(uexp.getValue()!=null && uexp.getValue().length()>0)
						{
							Object o = SJavaParser.parseExpression(uexp, component.getModel().getAllImports(), component.getClassLoader()).getValue(component.getFetcher());
							if(o instanceof Class)
							{
								gcl = (Class<?>)o;
							}
							else
							{
								goal = o;
								gcl = o.getClass();
							}
						}
						
						if(gcl==null && uexp.getClazz()!=null)
						{
							gcl = uexp.getClazz().getType(component.getClassLoader(), component.getModel().getAllImports());
						}
						if(gcl==null)
						{
							// try to fetch via name
							mgoal = bdimodel.getCapability().getGoal(uexp.getName());
							if(mgoal==null && uexp.getName().indexOf(".")==-1)
							{
								// try with package
								mgoal = bdimodel.getCapability().getGoal(component.getModel().getPackage()+"."+uexp.getName());
							}
							if(mgoal!=null)
							{
								gcl = mgoal.getTargetClass(component.getClassLoader());
							}
						}
						if(mgoal==null)
						{
							mgoal = bdimodel.getCapability().getGoal(gcl.getName());
						}
						if(goal==null && gcl!=null)
						{
							try
							{
								Object agent = component.getComponentFeature(IPojoComponentFeature.class).getPojoAgent();
								Class<?> agcl = agent.getClass();
								Constructor<?>[] cons = gcl.getDeclaredConstructors();
								for(Constructor<?> c: cons)
								{
									Class<?>[] params = c.getParameterTypes();
									if(params.length==0)
									{
										// perfect found empty con
										goal = gcl.newInstance();
										break;
									}
									else if(params.length==1 && params[0].equals(agcl))
									{
										// found (first level) inner class constructor
										goal = c.newInstance(new Object[]{agent});
										break;
									}
								}
							}
							catch(RuntimeException e)
							{
								throw e;
							}
							catch(Exception e)
							{
								throw new RuntimeException(e);
							}
						}
						
						if(mgoal==null || (goal==null && gcl!=null))
						{
							throw new RuntimeException("Could not create initial goal: "+uexp);
						}
						
						RGoal rgoal = new RGoal(component, mgoal, goal, (RPlan)null, null);
						RGoal.adoptGoal(rgoal, component);
					}
				}
				
				// Create initial plans
				List<UnparsedExpression> iplans = mconf.getInitialPlans();
				if(iplans!=null)
				{
					for(UnparsedExpression uexp: iplans)
					{
						MPlan mplan = bdimodel.getCapability().getPlan(uexp.getName());
						// todo: allow Java plan constructor calls
	//						Object val = SJavaParser.parseExpression(uexp, model.getModelInfo().getAllImports(), getClassLoader());
					
						RPlan rplan = RPlan.createRPlan(mplan, mplan, null, component, null);
						RPlan.executePlan(rplan, component);
					}
				}
			}
		}
		
		// Observe dynamic beliefs
		List<MBelief> beliefs = bdimodel.getCapability().getBeliefs();
		
		for(final MBelief mbel: beliefs)
		{
			List<EventType> events = new ArrayList<EventType>();
			
			Collection<String> evs = mbel.getEvents();
			Object	cap = null;
			if(evs!=null && !evs.isEmpty())
			{
				Object agent = component.getComponentFeature(IPojoComponentFeature.class).getPojoAgent();
				Object	ocapa	= agent;
				int	i	= mbel.getName().indexOf(MElement.CAPABILITY_SEPARATOR);
				if(i!=-1)
				{
					ocapa	= ((BDIAgentFeature)bdif).getCapabilityObject(mbel.getName().substring(0, mbel.getName().lastIndexOf(MElement.CAPABILITY_SEPARATOR)));
				}
				cap	= ocapa;

				for(String ev: evs)
				{
					BDIAgentFeature.addBeliefEvents(component, events, ev);
				}
			}
			
			Collection<EventType> rawevents = mbel.getRawEvents();
			if(rawevents!=null)
			{
				Collection<EventType> revs = mbel.getRawEvents();
				if(revs!=null)
					events.addAll(revs);
			}
		
			if(!events.isEmpty())
			{
				final Object fcapa = cap;
				Rule<Void> rule = new Rule<Void>(mbel.getName()+"_belief_update", 
					ICondition.TRUE_CONDITION, new IAction<Void>()
				{
					Object oldval = null;
					
					public IFuture<Void> execute(IEvent event, IRule<Void> rule, Object context, Object condresult)
					{
//							System.out.println("belief update: "+event);
						if(mbel.isFieldBelief())
						{
							try
							{
								Method um = fcapa.getClass().getMethod(IBDIClassGenerator.DYNAMIC_BELIEF_UPDATEMETHOD_PREFIX+SUtil.firstToUpperCase(mbel.getName()), new Class[0]);
								um.invoke(fcapa, new Object[0]);
							}
							catch(Exception e)
							{
								e.printStackTrace();
							}
						}
						else
						{
							Object value = mbel.getValue(component);
							// todo: save old value?!
							BDIAgentFeature.createChangeEvent(value, oldval, null, component, mbel.getName());
							oldval = value;
						}
						return IFuture.DONE;
					}
				});
				rule.setEvents(events);
				rulesystem.getRulebase().addRule(rule);
			}
			
			if(mbel.getUpdaterate()>0)
			{
				int	i	= mbel.getName().indexOf(MElement.CAPABILITY_SEPARATOR);
				final String name;
				final Object capa;
				if(i!=-1)
				{
					capa	= ((BDIAgentFeature)bdif).getCapabilityObject(mbel.getName().substring(0, mbel.getName().lastIndexOf(MElement.CAPABILITY_SEPARATOR)));
					name	= mbel.getName().substring(mbel.getName().lastIndexOf(MElement.CAPABILITY_SEPARATOR)+1); 
				}
				else
				{
					Object agent = component.getComponentFeature(IPojoComponentFeature.class).getPojoAgent();
					capa	= agent;
					name	= mbel.getName();
				}

				final IClockService cs = SServiceProvider.getLocalService(component, IClockService.class, RequiredServiceInfo.SCOPE_PLATFORM);
				cs.createTimer(mbel.getUpdaterate(), new ITimedObject()
				{
					ITimedObject	self	= this;
					Object oldval = null;
					
					public void timeEventOccurred(long currenttime)
					{
						try
						{
							component.getComponentFeature(IExecutionFeature.class).scheduleStep(new IComponentStep<Void>()
							{
								public IFuture<Void> execute(IInternalAccess ia)
								{
									try
									{
										// Invoke dynamic update method if field belief
										if(mbel.isFieldBelief())
										{
											Method um = capa.getClass().getMethod(IBDIClassGenerator.DYNAMIC_BELIEF_UPDATEMETHOD_PREFIX+SUtil.firstToUpperCase(name), new Class[0]);
											um.invoke(capa, new Object[0]);
										}
										// Otherwise just call getValue and throw event
										else
										{
											Object value = mbel.getValue(capa, component.getClassLoader());
											BDIAgentFeature.createChangeEvent(value, oldval, null, component, mbel.getName());
											oldval = value;
										}
									}
									catch(Exception e)
									{
										e.printStackTrace();
									}
									
									cs.createTimer(mbel.getUpdaterate(), self);
									return IFuture.DONE;
								}
							});
						}
						catch(ComponentTerminatedException cte)
						{
							
						}
					}
				
					public void exceptionOccurred(Exception exception)
					{
						component.getLogger().severe("Cannot update belief "+mbel.getName()+": "+exception);
					}
				});
			}
		}
		
		// Observe goal types
		List<MGoal> goals = bdimodel.getCapability().getGoals();
		for(final MGoal mgoal: goals)
		{
//			todo: explicit bdi creation rule
//			rulesystem.observeObject(goals.get(i).getTargetClass(getClassLoader()));
		
//			boolean fin = false;
			
			final Class<?> gcl = mgoal.getTargetClass(component.getClassLoader());
//			boolean declarative = false;
//			boolean maintain = false;
			
			List<MCondition> conds = mgoal.getConditions(MGoal.CONDITION_CREATION);
			if(conds!=null)
			{
				for(MCondition cond: conds)
				{
					if(cond.getConstructorTarget()!=null)
					{
						final Constructor<?> c = cond.getConstructorTarget().getConstructor(component.getClassLoader());
						
						Rule<Void> rule = new Rule<Void>(mgoal.getName()+"_goal_create", 
							ICondition.TRUE_CONDITION, new IAction<Void>()
						{
							public IFuture<Void> execute(IEvent event, IRule<Void> rule, Object context, Object condresult)
							{
	//							System.out.println("create: "+context);
								
								Object pojogoal = null;
								try
								{
									boolean ok = true;
									Class<?>[] ptypes = c.getParameterTypes();
									Object[] pvals = new Object[ptypes.length];
									
									Annotation[][] anns = c.getParameterAnnotations();
									int skip = ptypes.length - anns.length;
									
									for(int i=0; i<ptypes.length; i++)
									{
										Object agent = component.getComponentFeature(IPojoComponentFeature.class).getPojoAgent();
										Object	o	= event.getContent();
										if(o!=null && SReflect.isSupertype(ptypes[i], o.getClass()))
										{
											pvals[i] = o;
										}
										else if(o instanceof ChangeInfo<?> && ((ChangeInfo)o).getValue()!=null && SReflect.isSupertype(ptypes[i], ((ChangeInfo)o).getValue().getClass()))
										{
											pvals[i] = ((ChangeInfo)o).getValue();
										}
										else if(SReflect.isSupertype(agent.getClass(), ptypes[i]))
										{
											pvals[i] = agent;
										}
										
										// ignore implicit parameters of inner class constructor
										if(pvals[i]==null && i>=skip)
										{
											for(int j=0; anns!=null && j<anns[i-skip].length; j++)
											{
												if(anns[i-skip][j] instanceof CheckNotNull)
												{
													ok = false;
													break;
												}
											}
										}
									}
									
									if(ok)
									{
										pojogoal = c.newInstance(pvals);
									}
								}
								catch(RuntimeException e)
								{
									throw e;
								}
								catch(Exception e)
								{
									throw new RuntimeException(e);
								}
								
								if(pojogoal!=null && !bdif.getCapability().containsGoal(pojogoal))
								{
									final Object fpojogoal = pojogoal;
									bdif.dispatchTopLevelGoal(pojogoal)
										.addResultListener(new IResultListener<Object>()
									{
										public void resultAvailable(Object result)
										{
											component.getLogger().info("Goal succeeded: "+result);
										}
										
										public void exceptionOccurred(Exception exception)
										{
											component.getLogger().info("Goal failed: "+fpojogoal+" "+exception);
										}
									});
								}
//								else
//								{
//									System.out.println("new goal not adopted, already contained: "+pojogoal);
//								}
							
								return IFuture.DONE;
							}
						});
						rule.setEvents(cond.getEvents());
						rulesystem.getRulebase().addRule(rule);
					}
					else if(cond.getMethodTarget()!=null)
					{
						final Method m = cond.getMethodTarget().getMethod(component.getClassLoader());
						
						Rule<Void> rule = new Rule<Void>(mgoal.getName()+"_goal_create", 
							new MethodCondition(null, m)
						{
							protected Object invokeMethod(IEvent event) throws Exception
							{
								m.setAccessible(true);
								Object[] pvals = BDIAgentFeature.getInjectionValues(m.getParameterTypes(), m.getParameterAnnotations(),
									mgoal, new ChangeEvent(event), null, null, component);
								return pvals!=null? m.invoke(null, pvals): null;
							}
						}, new IAction<Void>()
						{
							public IFuture<Void> execute(IEvent event, IRule<Void> rule, Object context, Object condresult)
							{
		//						System.out.println("create: "+context);
								
								if(condresult!=null)
								{
									if(SReflect.isIterable(condresult))
									{
										for(Iterator<Object> it = SReflect.getIterator(condresult); it.hasNext(); )
										{
											Object pojogoal = it.next();
											bdif.dispatchTopLevelGoal(pojogoal);
										}
									}
									else
									{
										bdif.dispatchTopLevelGoal(condresult);
									}
								}
								else
								{
									Constructor<?>[] cons = gcl.getConstructors();
									Object pojogoal = null;
									boolean ok = false;
									for(Constructor<?> c: cons)
									{
										try
										{
											Object[] vals = BDIAgentFeature.getInjectionValues(c.getParameterTypes(), c.getParameterAnnotations(),
												mgoal, new ChangeEvent(event), null, null, component);
											if(vals!=null)
											{
												pojogoal = c.newInstance(vals);
												bdif.dispatchTopLevelGoal(pojogoal);
												break;
											}
											else
											{
												ok = true;
											}
										}
										catch(Exception e)
										{
										}
									}
									if(pojogoal==null && !ok)
										throw new RuntimeException("Unknown how to create goal: "+gcl);
								}
								return IFuture.DONE;
							}
						});
						rule.setEvents(cond.getEvents());
						rulesystem.getRulebase().addRule(rule);
					}
					else
					{
						Rule<Void> rule = new Rule<Void>(mgoal.getName()+"_goal_create", 
							new EvaluateExpressionCondition(component, cond, null), new IAction<Void>()
						{
							public IFuture<Void> execute(IEvent event, IRule<Void> rule, Object context, Object condresult)
							{
		//						System.out.println("create: "+context);
								
								List<Map<String, Object>> bindings = APL.calculateBindingElements(component, mgoal, null);
								
								if(bindings!=null)
								{
									for(Map<String, Object> binding: bindings)
									{
										RGoal rgoal = new RGoal(component, mgoal, null, (RPlan)null, binding);
										bdif.dispatchTopLevelGoal(rgoal);
									}
								}
								// No binding: generate one candidate.
								else
								{
									RGoal rgoal = new RGoal(component, mgoal, null, (RPlan)null, null);
									bdif.dispatchTopLevelGoal(rgoal);
								}
								
								return IFuture.DONE;
							}
						});
						
						rule.setEvents(cond.getEvents());
						rulesystem.getRulebase().addRule(rule);
					}
				}
			}
			
			conds = mgoal.getConditions(MGoal.CONDITION_DROP);
			if(conds!=null)
			{
				for(final MCondition cond: conds)
				{
					final Method m = cond.getMethodTarget()==null? null: cond.getMethodTarget().getMethod(component.getClassLoader());
					
					Rule<?> rule = new Rule<Void>(mgoal.getName()+"_goal_drop", 
						new GoalsExistCondition(mgoal, bdif.getCapability()), new IAction<Void>()
					{
						public IFuture<Void> execute(IEvent event, IRule<Void> rule, Object context, Object condresult)
						{
							for(final RGoal goal: bdif.getCapability().getGoals(mgoal))
							{
								if(!RGoal.GoalLifecycleState.DROPPING.equals(goal.getLifecycleState())
									 && !RGoal.GoalLifecycleState.DROPPED.equals(goal.getLifecycleState()))
								{
									if(m!=null)
									{
										executeGoalMethod(m, goal, event, component)
											.addResultListener(new IResultListener<Boolean>()
										{
											public void resultAvailable(Boolean result)
											{
												if(result.booleanValue())
												{
	//												System.out.println("Goal dropping triggered: "+goal);
					//								rgoal.setLifecycleState(BDIAgent.this, rgoal.GOALLIFECYCLESTATE_DROPPING);
													if(!goal.isFinished())
													{
														goal.setException(new GoalFailureException("drop condition: "+m.getName()));
//														{
//															public void printStackTrace() 
//															{
//																super.printStackTrace();
//															}
//														});
														goal.setProcessingState(component, RGoal.GoalProcessingState.FAILED);
													}
												}
											}
											
											public void exceptionOccurred(Exception exception)
											{
											}
										});
									}
									else
									{
										if(evaluateCondition(component, cond, SUtil.createHashMap(new String[]{"$goal"}, new Object[]{goal})))
										{
											if(!goal.isFinished())
											{
												goal.setException(new GoalFailureException("drop condition: "+cond.getExpression().getName()));
												goal.setProcessingState(component, RGoal.GoalProcessingState.FAILED);
											}
										}
									}
								}
							}
							
							return IFuture.DONE;
						}
					});
					List<EventType> events = new ArrayList<EventType>(cond.getEvents());
					events.add(new EventType(new String[]{ChangeEvent.GOALADOPTED, mgoal.getName()}));
					rule.setEvents(events);
					rulesystem.getRulebase().addRule(rule);
//						rule.setEvents(cond.getEvents());
//						rulesystem.getRulebase().addRule(rule);
				}
			}
			
			conds = mgoal.getConditions(MGoal.CONDITION_CONTEXT);
			if(conds!=null)
			{
				for(final MCondition cond: conds)
				{
					final Method m = cond.getMethodTarget()==null? null: cond.getMethodTarget().getMethod(component.getClassLoader());
					
					Rule<?> rule = new Rule<Void>(mgoal.getName()+"_goal_suspend", 
						new GoalsExistCondition(mgoal, bdif.getCapability()), new IAction<Void>()
					{
						public IFuture<Void> execute(IEvent event, IRule<Void> rule, Object context, Object condresult)
						{
							for(final RGoal goal: bdif.getCapability().getGoals(mgoal))
							{
								if(!RGoal.GoalLifecycleState.SUSPENDED.equals(goal.getLifecycleState())
								  && !RGoal.GoalLifecycleState.DROPPING.equals(goal.getLifecycleState())
								  && !RGoal.GoalLifecycleState.DROPPED.equals(goal.getLifecycleState()))
								{	
									if(m!=null)
									{
										executeGoalMethod(m, goal, event, component)
											.addResultListener(new IResultListener<Boolean>()
										{
											public void resultAvailable(Boolean result)
											{
												if(!result.booleanValue())
												{
	//												if(goal.getMGoal().getName().indexOf("AchieveCleanup")!=-1)
	//													System.out.println("Goal suspended: "+goal);
													goal.setLifecycleState(component, RGoal.GoalLifecycleState.SUSPENDED);
													goal.setState(RProcessableElement.State.INITIAL);
												}
											}
											
											public void exceptionOccurred(Exception exception)
											{
											}
										});
									}
									else
									{
										if(!evaluateCondition(component, cond, SUtil.createHashMap(new String[]{"$goal"}, new Object[]{goal})))
										{
											goal.setLifecycleState(component, RGoal.GoalLifecycleState.SUSPENDED);
											goal.setState(RProcessableElement.State.INITIAL);
										}
									}
								}
							}
							return IFuture.DONE;
						}
					});
					List<EventType> events = new ArrayList<EventType>(cond.getEvents());
					events.add(new EventType(new String[]{ChangeEvent.GOALADOPTED, mgoal.getName()}));
					rule.setEvents(events);
					rulesystem.getRulebase().addRule(rule);
					
//						rule.setEvents(cond.getEvents());
//						rulesystem.getRulebase().addRule(rule);
					
					rule = new Rule<Void>(mgoal.getName()+"_goal_option", 
						new GoalsExistCondition(mgoal, bdif.getCapability()), new IAction<Void>()
					{
						public IFuture<Void> execute(IEvent event, IRule<Void> rule, Object context, Object condresult)
						{
							for(final RGoal goal: bdif.getCapability().getGoals(mgoal))
							{
								if(RGoal.GoalLifecycleState.SUSPENDED.equals(goal.getLifecycleState()))
								{	
									if(m!=null)
									{
										executeGoalMethod(m, goal, event, component)
											.addResultListener(new IResultListener<Boolean>()
										{
											public void resultAvailable(Boolean result)
											{
												if(result.booleanValue())
												{
	//													if(goal.getMGoal().getName().indexOf("AchieveCleanup")!=-1)
	//														System.out.println("Goal made option: "+goal);
													goal.setLifecycleState(component, RGoal.GoalLifecycleState.OPTION);
	//													setState(ia, PROCESSABLEELEMENT_INITIAL);
												}
											}
											
											public void exceptionOccurred(Exception exception)
											{
											}
										});
									}
									else
									{
										if(evaluateCondition(component, cond, SUtil.createHashMap(new String[]{"$goal"}, new Object[]{goal})))
										{
											goal.setLifecycleState(component, RGoal.GoalLifecycleState.OPTION);
										}
									}
								}
							}
							
							return IFuture.DONE;
						}
					});
					rule.setEvents(events);
					rulesystem.getRulebase().addRule(rule);
					
//						rule.setEvents(cond.getEvents());
//						rulesystem.getRulebase().addRule(rule);
				}
			}
			
			conds = mgoal.getConditions(MGoal.CONDITION_TARGET);
			if(conds!=null)
			{
				for(final MCondition cond: conds)
				{
					final Method m = cond.getMethodTarget()==null? null: cond.getMethodTarget().getMethod(component.getClassLoader());
										
					Rule<?> rule = new Rule<Void>(mgoal.getName()+"_goal_target", 
						new CombinedCondition(new ICondition[]{
							new GoalsExistCondition(mgoal, bdif.getCapability())
		//							, new LifecycleStateCondition(SUtil.createHashSet(new String[]
		//							{
		//								RGoal.GOALLIFECYCLESTATE_ACTIVE,
		//								RGoal.GOALLIFECYCLESTATE_ADOPTED,
		//								RGoal.GOALLIFECYCLESTATE_OPTION,
		//								RGoal.GOALLIFECYCLESTATE_SUSPENDED
		//							}))
						}),
						new IAction<Void>()
					{
						public IFuture<Void> execute(final IEvent event, final IRule<Void> rule, final Object context, Object condresult)
						{
							for(final RGoal goal: bdif.getCapability().getGoals(mgoal))
							{
								if(m!=null)
								{
									executeGoalMethod(m, goal, event, component)
										.addResultListener(new IResultListener<Boolean>()
									{
										public void resultAvailable(Boolean result)
										{
											if(result.booleanValue())
											{
												goal.targetConditionTriggered(component, event, rule, context);
											}
										}
										
										public void exceptionOccurred(Exception exception)
										{
										}
									});
								}
								else
								{
									if(evaluateCondition(component, cond, SUtil.createHashMap(new String[]{"$goal"}, new Object[]{goal})))
									{
										goal.targetConditionTriggered(component, event, rule, context);
									}
								}
							}
						
							return IFuture.DONE;
						}
					});
					List<EventType> events = new ArrayList<EventType>(cond.getEvents());
					events.add(new EventType(new String[]{ChangeEvent.GOALADOPTED, mgoal.getName()}));
					rule.setEvents(events);
					rulesystem.getRulebase().addRule(rule);
				}
			}
			
			conds = mgoal.getConditions(MGoal.CONDITION_RECUR);
			if(conds!=null)
			{
				for(final MCondition cond: conds)
				{
					final Method m = cond.getMethodTarget()==null? null: cond.getMethodTarget().getMethod(component.getClassLoader());
										
					Rule<?> rule = new Rule<Void>(mgoal.getName()+"_goal_recur",
						new GoalsExistCondition(mgoal, bdif.getCapability()), new IAction<Void>()
	//						new CombinedCondition(new ICondition[]{
	//							new LifecycleStateCondition(GOALLIFECYCLESTATE_ACTIVE),
	//							new ProcessingStateCondition(GOALPROCESSINGSTATE_PAUSED),
	//							new MethodCondition(getPojoElement(), m),
	//						}), new IAction<Void>()
					{
						public IFuture<Void> execute(IEvent event, IRule<Void> rule, Object context, Object condresult)
						{
							for(final RGoal goal: bdif.getCapability().getGoals(mgoal))
							{
								if(RGoal.GoalLifecycleState.ACTIVE.equals(goal.getLifecycleState())
									&& RGoal.GoalProcessingState.PAUSED.equals(goal.getProcessingState()))
								{	
									if(m!=null)
									{
										executeGoalMethod(m, goal, event, component)
											.addResultListener(new IResultListener<Boolean>()
										{
											public void resultAvailable(Boolean result)
											{
												if(result.booleanValue())
												{
													goal.setTriedPlans(null);
													goal.setApplicablePlanList(null);
													goal.setProcessingState(component, RGoal.GoalProcessingState.INPROCESS);
												}
											}
											
											public void exceptionOccurred(Exception exception)
											{
											}
										});
									}
									else
									{
										if(evaluateCondition(component, cond, SUtil.createHashMap(new String[]{"$goal"}, new Object[]{goal})))
										{
											goal.setTriedPlans(null);
											goal.setApplicablePlanList(null);
											goal.setProcessingState(component, RGoal.GoalProcessingState.INPROCESS);
										}
									}
								}
							}
							return IFuture.DONE;
						}
					});
					rule.setEvents(cond.getEvents());
					rulesystem.getRulebase().addRule(rule);
				}
			}
			
			conds = mgoal.getConditions(MGoal.CONDITION_MAINTAIN);
			if(conds!=null)
			{
				for(final MCondition cond: conds)
				{
					final Method m = cond.getMethodTarget()==null? null: cond.getMethodTarget().getMethod(component.getClassLoader());
					
					Rule<?> rule = new Rule<Void>(mgoal.getName()+"_goal_maintain", 
						new GoalsExistCondition(mgoal, bdif.getCapability()), new IAction<Void>()
	//						new CombinedCondition(new ICondition[]{
	//							new LifecycleStateCondition(GOALLIFECYCLESTATE_ACTIVE),
	//							new ProcessingStateCondition(GOALPROCESSINGSTATE_IDLE),
	//							new MethodCondition(getPojoElement(), mcond, true),
	//						}), new IAction<Void>()
					{
						public IFuture<Void> execute(IEvent event, IRule<Void> rule, Object context, Object condresult)
						{
							for(final RGoal goal: bdif.getCapability().getGoals(mgoal))
							{
								if(RGoal.GoalLifecycleState.ACTIVE.equals(goal.getLifecycleState())
									&& RGoal.GoalProcessingState.IDLE.equals(goal.getProcessingState()))
								{	
									if(m!=null)
									{
										executeGoalMethod(m, goal, event, component)
											.addResultListener(new IResultListener<Boolean>()
										{
											public void resultAvailable(Boolean result)
											{
												if(!result.booleanValue())
												{
	//													System.out.println("Goal maintain triggered: "+goal);
	//													System.out.println("state was: "+goal.getProcessingState());
													goal.setProcessingState(component, RGoal.GoalProcessingState.INPROCESS);
												}
											}
											
											public void exceptionOccurred(Exception exception)
											{
											}
										});
									}
									else // xml expression
									{
										if(!evaluateCondition(component, cond, SUtil.createHashMap(new String[]{"$goal"}, new Object[]{goal})))
										{
											goal.setProcessingState(component, RGoal.GoalProcessingState.INPROCESS);
										}
									}
								}
							}
							return IFuture.DONE;
						}
					});
					List<EventType> events = new ArrayList<EventType>(cond.getEvents());
					events.add(new EventType(new String[]{ChangeEvent.GOALADOPTED, mgoal.getName()}));
					rule.setEvents(events);
					rulesystem.getRulebase().addRule(rule);
					
					// if has no own target condition
					if(mgoal.getConditions(MGoal.CONDITION_TARGET)==null)
					{
						// if not has own target condition use the maintain cond
						rule = new Rule<Void>(mgoal.getName()+"_goal_target", 
							new GoalsExistCondition(mgoal, bdif.getCapability()), new IAction<Void>()
	//							new MethodCondition(getPojoElement(), mcond), new IAction<Void>()
						{
							public IFuture<Void> execute(final IEvent event, final IRule<Void> rule, final Object context, Object condresult)
							{
								for(final RGoal goal: bdif.getCapability().getGoals(mgoal))
								{
									if(m!=null)
									{
										executeGoalMethod(m, goal, event, component)
											.addResultListener(new IResultListener<Boolean>()
										{
											public void resultAvailable(Boolean result)
											{
												if(result.booleanValue())
												{
													goal.targetConditionTriggered(component, event, rule, context);
												}
											}
											
											public void exceptionOccurred(Exception exception)
											{
											}
										});
									}
									else // xml expression
									{
										if(evaluateCondition(component, cond, SUtil.createHashMap(new String[]{"$goal"}, new Object[]{goal})))
										{
											goal.targetConditionTriggered(component, event, rule, context);
										}
									}
								}
								
								return IFuture.DONE;
							}
						});
						rule.setEvents(cond.getEvents());
						rulesystem.getRulebase().addRule(rule);
					}
				}
			}
		}
		
		// Observe plan types
		List<MPlan> mplans = bdimodel.getCapability().getPlans();
		for(int i=0; i<mplans.size(); i++)
		{
			final MPlan mplan = mplans.get(i);
			
			IAction<Void> createplan = new IAction<Void>()
			{
				public IFuture<Void> execute(IEvent event, IRule<Void> rule, Object context, Object condresult)
				{
					RPlan rplan = RPlan.createRPlan(mplan, mplan, new ChangeEvent(event), component, null);
					RPlan.executePlan(rplan, component);
					return IFuture.DONE;
				}
			};
			
			MTrigger trigger = mplan.getTrigger();
			
			if(trigger!=null)
			{
				List<String> fas = trigger.getFactAddeds();
				if(fas!=null && fas.size()>0)
				{
					Rule<Void> rule = new Rule<Void>("create_plan_factadded_"+mplan.getName(), ICondition.TRUE_CONDITION, createplan);
					for(String fa: fas)
					{
						rule.addEvent(new EventType(new String[]{ChangeEvent.FACTADDED, fa}));
					}
					rulesystem.getRulebase().addRule(rule);
				}
	
				List<String> frs = trigger.getFactRemoveds();
				if(frs!=null && frs.size()>0)
				{
					Rule<Void> rule = new Rule<Void>("create_plan_factremoved_"+mplan.getName(), ICondition.TRUE_CONDITION, createplan);
					for(String fr: frs)
					{
						rule.addEvent(new EventType(new String[]{ChangeEvent.FACTREMOVED, fr}));
					}
					rulesystem.getRulebase().addRule(rule);
				}
				
				List<String> fcs = trigger.getFactChangeds();
				if(fcs!=null && fcs.size()>0)
				{
					Rule<Void> rule = new Rule<Void>("create_plan_factchanged_"+mplan.getName(), ICondition.TRUE_CONDITION, createplan);
					for(String fc: fcs)
					{
						rule.addEvent(new EventType(new String[]{ChangeEvent.FACTCHANGED, fc}));
						rule.addEvent(new EventType(new String[]{ChangeEvent.BELIEFCHANGED, fc}));
					}
					rulesystem.getRulebase().addRule(rule);
				}
				
				List<MGoal> gfs = trigger.getGoalFinisheds();
				if(gfs!=null && gfs.size()>0)
				{
					Rule<Void> rule = new Rule<Void>("create_plan_goalfinished_"+mplan.getName(), ICondition.TRUE_CONDITION, createplan);
					for(MGoal gf: gfs)
					{
						rule.addEvent(new EventType(new String[]{ChangeEvent.GOALDROPPED, gf.getName()}));
					}
					rulesystem.getRulebase().addRule(rule);
				}
				
				final MCondition mcond = trigger.getCondition();
				if(mcond!=null)
				{
					Rule<Void> rule = new Rule<Void>("create_plan_condition_"+mplan.getName(), new ICondition()
					{
						public IFuture<Tuple2<Boolean, Object>> evaluate(IEvent event)
						{
							UnparsedExpression uexp = mcond.getExpression();
							if(uexp.getParsed()==null)
								SJavaParser.parseExpression(uexp, component.getModel().getAllImports(), component.getClassLoader());
							IParsedExpression exp = (IParsedExpression)uexp.getParsed();
							Boolean ret = (Boolean)exp.getValue(component.getFetcher());
							return new Future<Tuple2<Boolean, Object>>(ret!=null && ret.booleanValue()? TRUE: FALSE);
						}
					}, createplan);
					rule.setEvents(mcond.getEvents());
					rulesystem.getRulebase().addRule(rule);
				}
			}
			
			// context condition
			
			final MethodInfo mi = mplan.getBody().getContextConditionMethod(component.getClassLoader());
			if(mi!=null)
			{
				PlanContextCondition pcc = mi.getMethod(component.getClassLoader()).getAnnotation(PlanContextCondition.class);
				String[] evs = pcc.beliefs();
				RawEvent[] rawevs = pcc.rawevents();
				List<EventType> events = new ArrayList<EventType>();
				for(String ev: evs)
				{
					BDIAgentFeature.addBeliefEvents(component, events, ev);
				}
				for(RawEvent rawev: rawevs)
				{
					events.add(BDIAgentFeature.createEventType(rawev));
				}
				
				IAction<Void> abortplans = new IAction<Void>()
				{
					public IFuture<Void> execute(IEvent event, IRule<Void> rule, Object context, Object condresult)
					{
						Collection<RPlan> coll = bdif.getCapability().getPlans(mplan);
						
						for(final RPlan plan: coll)
						{
							invokeBooleanMethod(plan.getBody().getBody(), mi.getMethod(component.getClassLoader()), plan.getModelElement(), event, plan, component)
								.addResultListener(new IResultListener<Boolean>()
							{
								public void resultAvailable(Boolean result)
								{
									if(!result.booleanValue())
									{
										plan.abort();
									}
								}
								
								public void exceptionOccurred(Exception exception)
								{
								}
							});
						}
						return IFuture.DONE;
					}
				};
				
				Rule<Void> rule = new Rule<Void>("plan_context_abort_"+mplan.getName(), 
					new PlansExistCondition(mplan, bdif.getCapability()), abortplans);
				rule.setEvents(events);
				rulesystem.getRulebase().addRule(rule);
			}
		}
		
		// add/rem goal inhibitor rules
		if(!goals.isEmpty())
		{
			boolean	usedelib	= false;
			for(int i=0; !usedelib && i<goals.size(); i++)
			{
				usedelib	= goals.get(i).getDeliberation()!=null;
			}
			
			if(usedelib)
			{
				List<EventType> events = new ArrayList<EventType>();
				events.add(new EventType(new String[]{ChangeEvent.GOALADOPTED, EventType.MATCHALL}));
				Rule<Void> rule = new Rule<Void>("goal_addinitialinhibitors", 
					ICondition.TRUE_CONDITION, new IAction<Void>()
				{
					public IFuture<Void> execute(IEvent event, IRule<Void> rule, Object context, Object condresult)
					{
						// create the complete inhibitorset for a newly adopted goal
						
						RGoal goal = (RGoal)event.getContent();
						for(RGoal other: bdif.getCapability().getGoals())
						{
	//						if(other.getLifecycleState().equals(RGoal.GOALLIFECYCLESTATE_ACTIVE) 
	//							&& other.getProcessingState().equals(RGoal.GOALPROCESSINGSTATE_INPROCESS)
							if(!other.isInhibitedBy(goal) && other.inhibits(goal, component))
							{
								goal.addInhibitor(other, component);
							}
						}
						
						return IFuture.DONE;
					}
				});
				rule.setEvents(events);
				rulesystem.getRulebase().addRule(rule);
				
				events = BDIAgentFeature.getGoalEvents(null);
				rule = new Rule<Void>("goal_addinhibitor", 
					new ICondition()
					{
						public IFuture<Tuple2<Boolean, Object>> evaluate(IEvent event)
						{
	//						if(((RGoal)event.getContent()).getId().indexOf("Battery")!=-1)
	//							System.out.println("maintain");
//								if(getComponentIdentifier().getName().indexOf("Ambu")!=-1)
//									System.out.println("addin");
							
							// return true when other goal is active and inprocess
							boolean ret = false;
							EventType type = event.getType();
							RGoal goal = (RGoal)event.getContent();
							ret = ChangeEvent.GOALACTIVE.equals(type.getType(0)) && RGoal.GoalProcessingState.INPROCESS.equals(goal.getProcessingState())
								|| (ChangeEvent.GOALINPROCESS.equals(type.getType(0)) && RGoal.GoalLifecycleState.ACTIVE.equals(goal.getLifecycleState()));
//								return ret? ICondition.TRUE: ICondition.FALSE;
							return new Future<Tuple2<Boolean,Object>>(ret? ICondition.TRUE: ICondition.FALSE);
						}
					}, new IAction<Void>()
				{
					public IFuture<Void> execute(IEvent event, IRule<Void> rule, Object context, Object condresult)
					{
						RGoal goal = (RGoal)event.getContent();
//							if(goal.getId().indexOf("PerformPatrol")!=-1)
//								System.out.println("addinh: "+goal);
						MDeliberation delib = goal.getMGoal().getDeliberation();
						if(delib!=null)
						{
							Set<MGoal> inhs = delib.getInhibitions();
							if(inhs!=null)
							{
								for(MGoal inh: inhs)
								{
									Collection<RGoal> goals = bdif.getCapability().getGoals(inh);
									for(RGoal other: goals)
									{
	//									if(!other.isInhibitedBy(goal) && goal.inhibits(other, getInternalAccess()))
										if(!goal.isInhibitedBy(other) && goal.inhibits(other, component))
										{
											other.addInhibitor(goal, component);
										}
									}
								}
							}
						}
						return IFuture.DONE;
					}
				});
				rule.setEvents(events);
				rulesystem.getRulebase().addRule(rule);
				
				rule = new Rule<Void>("goal_removeinhibitor", 
					new ICondition()
					{
						public IFuture<Tuple2<Boolean, Object>> evaluate(IEvent event)
						{
//								if(getComponentIdentifier().getName().indexOf("Ambu")!=-1)
//									System.out.println("remin");
							
							// return true when other goal is active and inprocess
							boolean ret = false;
							EventType type = event.getType();
							if(event.getContent() instanceof RGoal)
							{
								RGoal goal = (RGoal)event.getContent();
								ret = ChangeEvent.GOALSUSPENDED.equals(type.getType(0)) || ChangeEvent.GOALOPTION.equals(type.getType(0))
									|| !RGoal.GoalProcessingState.INPROCESS.equals(goal.getProcessingState());
							}
//								return ret? ICondition.TRUE: ICondition.FALSE;
							return new Future<Tuple2<Boolean,Object>>(ret? ICondition.TRUE: ICondition.FALSE);
						}
					}, new IAction<Void>()
				{
					public IFuture<Void> execute(IEvent event, IRule<Void> rule, Object context, Object condresult)
					{
						// Remove inhibitions of this goal 
						RGoal goal = (RGoal)event.getContent();
						MDeliberation delib = goal.getMGoal().getDeliberation();
						if(delib!=null)
						{
							Set<MGoal> inhs = delib.getInhibitions();
							if(inhs!=null)
							{
								for(MGoal inh: inhs)
								{
		//							if(goal.getId().indexOf("AchieveCleanup")!=-1)
		//								System.out.println("reminh: "+goal);
									Collection<RGoal> goals = bdif.getCapability().getGoals(inh);
									for(RGoal other: goals)
									{
										if(goal.equals(other))
											continue;
										
										if(other.isInhibitedBy(goal))
											other.removeInhibitor(goal, component);
									}
								}
							}
							
							// Remove inhibitor from goals of same type if cardinality is used
							if(delib.isCardinalityOne())
							{
								Collection<RGoal> goals = bdif.getCapability().getGoals(goal.getMGoal());
								if(goals!=null)
								{
									for(RGoal other: goals)
									{
										if(goal.equals(other))
											continue;
										
										if(other.isInhibitedBy(goal))
											other.removeInhibitor(goal, component);
									}
								}
							}
						}
					
						return IFuture.DONE;
					}
				});
				rule.setEvents(events);
				rulesystem.getRulebase().addRule(rule);
				
				
				rule = new Rule<Void>("goal_inhibit", 
					new LifecycleStateCondition(RGoal.GoalLifecycleState.ACTIVE), new IAction<Void>()
				{
					public IFuture<Void> execute(IEvent event, IRule<Void> rule, Object context, Object condresult)
					{
						RGoal goal = (RGoal)event.getContent();
	//					System.out.println("optionizing: "+goal+" "+goal.inhibitors);
						goal.setLifecycleState(component, RGoal.GoalLifecycleState.OPTION);
						return IFuture.DONE;
					}
				});
				rule.addEvent(new EventType(new String[]{ChangeEvent.GOALINHIBITED, EventType.MATCHALL}));
				rulesystem.getRulebase().addRule(rule);
			}
			
			Rule<Void> rule = new Rule<Void>("goal_activate", 
				new CombinedCondition(new ICondition[]{
					new LifecycleStateCondition(RGoal.GoalLifecycleState.OPTION),
					new ICondition()
					{
						public IFuture<Tuple2<Boolean, Object>> evaluate(IEvent event)
						{
							RGoal goal = (RGoal)event.getContent();
//								return !goal.isInhibited()? ICondition.TRUE: ICondition.FALSE;
							return new Future<Tuple2<Boolean,Object>>(!goal.isInhibited()? ICondition.TRUE: ICondition.FALSE);
						}
					}
				}), new IAction<Void>()
			{
				public IFuture<Void> execute(IEvent event, IRule<Void> rule, Object context, Object condresult)
				{
					RGoal goal = (RGoal)event.getContent();
//						if(goal.getMGoal().getName().indexOf("AchieveCleanup")!=-1)
//							System.out.println("reactivating: "+goal);
					goal.setLifecycleState(component, RGoal.GoalLifecycleState.ACTIVE);
					return IFuture.DONE;
				}
			});
			rule.addEvent(new EventType(new String[]{ChangeEvent.GOALNOTINHIBITED, EventType.MATCHALL}));
			rule.addEvent(new EventType(new String[]{ChangeEvent.GOALOPTION, EventType.MATCHALL}));
//				rule.setEvents(SUtil.createArrayList(new String[]{ChangeEvent.GOALNOTINHIBITED, ChangeEvent.GOALOPTION}));
			rulesystem.getRulebase().addRule(rule);
		}
		
		// perform init write fields (after injection of bdiagent)
		BDIAgentFeature.performInitWrites(component);
		
		// Start rule system
//			if(getComponentIdentifier().getName().indexOf("Cleaner")!=-1)// && getComponentIdentifier().getName().indexOf("Burner")==-1)
//				getCapability().dumpPlansPeriodically(getInternalAccess());
//			if(getComponentIdentifier().getName().indexOf("Ambulance")!=-1)
//			{
//				getCapability().dumpGoalsPeriodically(getInternalAccess());
//				getCapability().dumpPlansPeriodically(getInternalAccess());
//			}
		
//			}
//			catch(Exception e)
//			{
//				e.printStackTrace();
//			}
		
//			throw new RuntimeException();
	}
	
	/**
	 *  Execute a goal method.
	 */
	protected static IFuture<Boolean> executeGoalMethod(Method m, RProcessableElement goal, IEvent event, IInternalAccess component)
	{
		return invokeBooleanMethod(goal.getPojoElement(), m, goal.getModelElement(), event, null, component);
	}
	
	/**
	 * 
	 */
	protected static IFuture<Boolean> invokeBooleanMethod(Object pojo, Method m, MElement modelelement, IEvent event, RPlan rplan, IInternalAccess component)
	{
		final Future<Boolean> ret = new Future<Boolean>();
		try
		{
			m.setAccessible(true);
			
			Object[] vals = BDIAgentFeature.getInjectionValues(m.getParameterTypes(), m.getParameterAnnotations(),
				modelelement, event!=null ? new ChangeEvent(event) : null, rplan, null, component);
			if(vals==null)
				System.out.println("Invalid parameter assignment");
			Object app = m.invoke(pojo, vals);
			if(app instanceof Boolean)
			{
				ret.setResult((Boolean)app);
			}
			else if(app instanceof IFuture)
			{
				((IFuture<Boolean>)app).addResultListener(new DelegationResultListener<Boolean>(ret));
			}
		}
		catch(Exception e)
		{
			System.err.println("method: "+m);
			e.printStackTrace();
			ret.setException(e);
		}
		return ret;
	}
	
	/**
	 *  Get the inited.
	 *  @return The inited.
	 */
	public boolean isInited()
	{
		return inited;
	}

	// for xml
	
	/**
	 *  Evaluate the condition.
	 *  @return
	 */
	public static boolean evaluateCondition(IInternalAccess agent, MCondition cond, Map<String, Object> vals)
	{
		boolean ret = false;
		
		UnparsedExpression uexp = cond.getExpression();
		if(uexp.getParsed()==null)
			SJavaParser.parseExpression(uexp, agent.getModel().getAllImports(), agent.getClassLoader());
		SimpleValueFetcher fet = new SimpleValueFetcher(agent.getFetcher());
		if(vals!=null)
		{
			for(Map.Entry<String, Object> entry: vals.entrySet())
			{
				fet.setValue(entry.getKey(), entry.getValue());
			}
		}
		Object res = ((IParsedExpression)uexp.getParsed()).getValue(fet);
		if(res instanceof Boolean)
		{
			ret = ((Boolean)res).booleanValue();
		}
		else
		{
			System.out.println("Suspend condition does not evaluate to boolean: "+uexp.getValue());
		}
		
		return ret;
	}
	
	/**
	 *  Condition that tests if an expression evalutes to true.
	 */
	public static class EvaluateExpressionCondition implements ICondition
	{
		protected MCondition cond;
		protected IInternalAccess agent;
		protected Map<String, Object> vals;
		
		public EvaluateExpressionCondition(IInternalAccess agent, MCondition cond, Map<String, Object> vals)
		{
			this.agent = agent;
			this.cond = cond;
			this.vals = vals;
		}
		
		public IFuture<Tuple2<Boolean, Object>> evaluate(IEvent event)
		{
//			vals.put("$event", event);
			boolean res = evaluateCondition(agent, cond, vals);
			return new Future<Tuple2<Boolean,Object>>(res? ICondition.TRUE: ICondition.FALSE);
		}
	}
}


