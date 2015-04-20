package jadex.micro.features.impl;

import jadex.bridge.IComponentStep;
import jadex.bridge.IInternalAccess;
import jadex.bridge.component.ComponentCreationInfo;
import jadex.bridge.component.IComponentFeatureFactory;
import jadex.bridge.component.IExecutionFeature;
import jadex.bridge.component.IPojoComponentFeature;
import jadex.bridge.component.ISubcomponentsFeature;
import jadex.bridge.component.impl.AbstractComponentFeature;
import jadex.bridge.component.impl.ComponentFeatureFactory;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.bridge.service.component.IRequiredServicesFeature;
import jadex.bridge.service.search.ServiceNotFoundException;
import jadex.commons.FieldInfo;
import jadex.commons.MethodInfo;
import jadex.commons.SReflect;
import jadex.commons.future.CounterResultListener;
import jadex.commons.future.DelegationResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.commons.future.IIntermediateFuture;
import jadex.commons.future.IIntermediateResultListener;
import jadex.commons.future.IResultListener;
import jadex.micro.MicroModel;
import jadex.micro.annotation.AgentService;
import jadex.micro.features.IMicroServiceInjectionFeature;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 *  Inject required services into annotated field values.
 *  Performed after subcomponent creation and provided service initialization.
 */
public class MicroServiceInjectionComponentFeature extends	AbstractComponentFeature	implements IMicroServiceInjectionFeature
{
	//-------- constants ---------
	
	/** The factory. */
	public static final IComponentFeatureFactory FACTORY = new ComponentFeatureFactory(
		IMicroServiceInjectionFeature.class, MicroServiceInjectionComponentFeature.class,
		new Class<?>[]{ISubcomponentsFeature.class, IPojoComponentFeature.class}, null);

	//-------- constructors --------
	
	/**
	 *  Factory method constructor for instance level.
	 */
	public MicroServiceInjectionComponentFeature(IInternalAccess component, ComponentCreationInfo cinfo)
	{
		super(component, cinfo);
	}

	/**
	 *  Initialize the feature.
	 *  Empty implementation that can be overridden.
	 */
	public IFuture<Void> init()
	{
		final Future<Void> ret = new Future<Void>();
		
		final MicroModel model = (MicroModel)getComponent().getModel().getRawModel();
		final Object agent = getComponent().getComponentFeature(IPojoComponentFeature.class).getPojoAgent();

		// Inject required services
		if(component.getComponentFeature(IRequiredServicesFeature.class)==null)
		{
			ret.setResult(null);
		}
		else
		{
			String[] sernames = model.getServiceInjectionNames();
			
			if(sernames.length>0)
			{
				CounterResultListener<Void> lis = new CounterResultListener<Void>(sernames.length, 
					new DelegationResultListener<Void>(ret));
		
				for(int i=0; i<sernames.length; i++)
				{
					final Object[] infos = model.getServiceInjections(sernames[i]);
					final CounterResultListener<Void> lis2 = new CounterResultListener<Void>(infos.length, lis);
	
					RequiredServiceInfo	info	= model.getModelInfo().getRequiredService(sernames[i]);				
					final IFuture<Object>	sfut;
					if(info!=null && info.isMultiple())
					{
						IFuture	ifut	= component.getComponentFeature(IRequiredServicesFeature.class).getRequiredServices(sernames[i]);
						sfut	= ifut;
					}
					else
					{
						sfut	= component.getComponentFeature(IRequiredServicesFeature.class).getRequiredService(sernames[i]);					
					}
					
					for(int j=0; j<infos.length; j++)
					{
						if(infos[j] instanceof FieldInfo)
						{
							final Field	f	= ((FieldInfo)infos[j]).getField(component.getClassLoader());
							if(SReflect.isSupertype(IFuture.class, f.getType()))
							{
								try
								{
									f.setAccessible(true);
									f.set(agent, sfut);
									lis2.resultAvailable(null);
								}
								catch(Exception e)
								{
									component.getLogger().warning("Field injection failed: "+e);
									lis2.exceptionOccurred(e);
								}	
							}
							else
							{
								sfut.addResultListener(new IResultListener<Object>()
								{
									public void resultAvailable(Object result)
									{
										try
										{
											f.setAccessible(true);
											f.set(agent, result);
											lis2.resultAvailable(null);
										}
										catch(Exception e)
										{
											component.getLogger().warning("Field injection failed: "+e);
											lis2.exceptionOccurred(e);
										}	
									}
									
									public void exceptionOccurred(Exception e)
									{
										if(!(e instanceof ServiceNotFoundException)
											|| f.getAnnotation(AgentService.class).required())
										{
											component.getLogger().warning("Field injection failed: "+e);
											lis2.exceptionOccurred(e);
										}
										else
										{
											if(SReflect.isSupertype(f.getType(), List.class))
											{
												// Call self with empty list as result.
												resultAvailable(Collections.EMPTY_LIST);
											}
											else
											{
												// Don't set any value.
												lis2.resultAvailable(null);
											}
										}
									}
								});
							}
						}
						else if(infos[j] instanceof MethodInfo)
						{
							final Method	m	= SReflect.getMethod(agent.getClass(), ((MethodInfo)infos[j]).getName(), ((MethodInfo)infos[j]).getParameterTypes(component.getClassLoader()));
							if(info.isMultiple())
							{
								lis2.resultAvailable(null);
								IFuture	tfut	= sfut;
								final IIntermediateFuture<Object>	ifut	= (IIntermediateFuture<Object>)tfut;
								
								ifut.addResultListener(new IIntermediateResultListener<Object>()
								{
									public void intermediateResultAvailable(final Object result)
									{
										if(SReflect.isSupertype(m.getParameterTypes()[0], result.getClass()))
										{
											component.getComponentFeature(IExecutionFeature.class).scheduleStep(new IComponentStep<Void>()
											{
												public IFuture<Void> execute(IInternalAccess ia)
												{
													try
													{
														m.setAccessible(true);
														m.invoke(agent, new Object[]{result});
													}
													catch(Throwable t)
													{
														t	= t instanceof InvocationTargetException ? ((InvocationTargetException)t).getTargetException() : t;
														throw t instanceof RuntimeException ? (RuntimeException)t : new RuntimeException(t);
													}
													return IFuture.DONE;
												}
											});
										}
									}
									
									public void resultAvailable(Collection<Object> result)
									{
										finished();
									}
									
									public void finished()
									{
										if(SReflect.isSupertype(m.getParameterTypes()[0], Collection.class))
										{
											component.getComponentFeature(IExecutionFeature.class).scheduleStep(new IComponentStep<Void>()
											{
												public IFuture<Void> execute(IInternalAccess ia)
												{
													try
													{
														m.setAccessible(true);
														m.invoke(agent, new Object[]{ifut.getIntermediateResults()});
													}
													catch(Throwable t)
													{
														t	= t instanceof InvocationTargetException ? ((InvocationTargetException)t).getTargetException() : t;
														throw t instanceof RuntimeException ? (RuntimeException)t : new RuntimeException(t);
													}
													return IFuture.DONE;
												}
											});
										}
									}
									
									public void exceptionOccurred(Exception e)
									{
										if(!(e instanceof ServiceNotFoundException)
											|| m.getAnnotation(AgentService.class).required())
										{
											component.getLogger().warning("Method injection failed: "+e);
										}
										else
										{
											// Call self with empty list as result.
											finished();
										}
									}
								});
	
							}
							else
							{
								sfut.addResultListener(new IResultListener<Object>()
								{
									public void resultAvailable(final Object result)
									{
										component.getComponentFeature(IExecutionFeature.class).scheduleStep(new IComponentStep<Void>()
										{
											public IFuture<Void> execute(IInternalAccess ia)
											{
												try
												{
													m.setAccessible(true);
													m.invoke(agent, new Object[]{result});
													lis2.resultAvailable(null);
												}
												catch(Throwable t)
												{
													t	= t instanceof InvocationTargetException ? ((InvocationTargetException)t).getTargetException() : t;
													lis2.exceptionOccurred(t instanceof RuntimeException ? (RuntimeException)t : new RuntimeException(t));
//													throw t instanceof RuntimeException ? (RuntimeException)t : new RuntimeException(t);
												}
												return IFuture.DONE;
											}
										});
									}
									
									public void exceptionOccurred(Exception e)
									{
										if(!(e instanceof ServiceNotFoundException)
											|| m.getAnnotation(AgentService.class).required())
										{
											component.getLogger().warning("Method service injection failed: "+e);
										}
									}
								});
							}
						}
					}
				}
			}
			else
			{
				ret.setResult(null);
			}
		}
		
		return ret;
	}
}
