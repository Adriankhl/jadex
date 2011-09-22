package jadex.base;

import jadex.bridge.IComponentFactory;
import jadex.bridge.IComponentStep;
import jadex.bridge.IExternalAccess;
import jadex.bridge.IInternalAccess;
import jadex.bridge.modelinfo.IModelInfo;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.bridge.service.SServiceProvider;
import jadex.bridge.service.ServiceNotFoundException;
import jadex.bridge.service.component.ComponentFactorySelector;
import jadex.bridge.service.library.ILibraryService;
import jadex.commons.SUtil;
import jadex.commons.future.DelegationResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.xml.annotation.XMLClassname;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import javax.swing.Icon;


/**
 * Standard meta component factory. Uses several sub factories and uses them
 * according to their order and isLoadable() method.
 */
public class SComponentFactory
{
	/**
	 * Load an component model.
	 * @param model The model.
	 * @return The loaded model.
	 */
	public static IFuture<IModelInfo> loadModel(IExternalAccess exta, final String model)
	{
		return exta.scheduleStep(new IComponentStep<IModelInfo>()
		{
			@XMLClassname("loadModel")
			public IFuture<IModelInfo> execute(final IInternalAccess ia)
			{
				final Future ret = new Future();
				
				SServiceProvider.getService(ia.getServiceContainer(), ILibraryService.class, RequiredServiceInfo.SCOPE_PLATFORM)
					.addResultListener(ia.createResultListener(new DelegationResultListener(ret)
				{
					public void customResultAvailable(Object result)
					{
						final ILibraryService ls = (ILibraryService)result;
						
						SServiceProvider.getService(ia.getServiceContainer(), new ComponentFactorySelector(model, null, ls.getClassLoader()))
							.addResultListener(ia.createResultListener(new DelegationResultListener(ret)
						{
							public void customResultAvailable(Object result)
							{
								IComponentFactory fac = (IComponentFactory)result;
								fac.loadModel(model, null, ls.getClassLoader())
									.addResultListener(new DelegationResultListener(ret));
							}
							
							public void exceptionOccurred(Exception exception)
							{
								if(exception instanceof ServiceNotFoundException)
								{
									ret.setResult(null);
								}
								else
								{
									super.exceptionOccurred(exception);
								}
							}
						}));
					}
				}));
				
				return ret;
			}
		});
	}

	/**
	 * Test if a model can be loaded by the factory.
	 * @param model The model.
	 * @return True, if model can be loaded.
	 */
	public static IFuture isLoadable(IExternalAccess exta, final String model)
	{
		Future ret = new Future();
		
		exta.scheduleStep(new IComponentStep<Boolean>()
		{
			@XMLClassname("isLoadable")
			public IFuture<Boolean> execute(final IInternalAccess ia)
			{
				final Future ret = new Future();
				SServiceProvider.getService(ia.getServiceContainer(), ILibraryService.class, RequiredServiceInfo.SCOPE_PLATFORM)
					.addResultListener(ia.createResultListener(new DelegationResultListener(ret)
				{
					public void customResultAvailable(Object result)
					{
						final ILibraryService ls = (ILibraryService)result;
						
						SServiceProvider.getService(ia.getServiceContainer(), new ComponentFactorySelector(model, null, ls.getClassLoader()))
							.addResultListener(ia.createResultListener(new DelegationResultListener(ret)
						{
							public void customResultAvailable(Object result)
							{
								IComponentFactory fac = (IComponentFactory)result;
								fac.isLoadable(model, null, ls.getClassLoader())
									.addResultListener(new DelegationResultListener(ret));
							}
							
							public void exceptionOccurred(Exception exception)
							{
								if(exception instanceof ServiceNotFoundException)
								{
									ret.setResult(Boolean.FALSE);
								}
								else
								{
									super.exceptionOccurred(exception);
								}
							}
						}));
					}
				}));
				return ret;
			}
		}).addResultListener(new DelegationResultListener(ret));

		return ret;
	}
	
	/**
	 * Test if a model can be loaded by the factory.
	 * @param model The model.
	 * @return True, if model can be loaded.
	 */
	public static IFuture isModelType(IExternalAccess exta, final String model, final Collection allowedtypes)
	{
		Future ret = new Future();
		
		exta.scheduleStep(new IComponentStep<Boolean>()
		{
			@XMLClassname("isModelType")
			public IFuture<Boolean> execute(final IInternalAccess ia)
			{
				final Future ret = new Future();
				SServiceProvider.getService(ia.getServiceContainer(), ILibraryService.class, RequiredServiceInfo.SCOPE_PLATFORM)
					.addResultListener(ia.createResultListener(new DelegationResultListener(ret)
				{
					public void customResultAvailable(Object result)
					{
						final ILibraryService ls = (ILibraryService)result;
						
						SServiceProvider.getServices(ia.getServiceContainer(), IComponentFactory.class, RequiredServiceInfo.SCOPE_PLATFORM)
							.addResultListener(ia.createResultListener(new DelegationResultListener(ret)
						{
							public void customResultAvailable(Object result)
							{
								Collection facs = (Collection)result;
								if(facs.size()==0)
								{
									ret.setResult(Boolean.FALSE);
								}
								else
								{
									checkComponentType(model, (IComponentFactory[])facs.toArray(new IComponentFactory[0]), 0, ia, ls.getClassLoader(), allowedtypes)
										.addResultListener(ia.createResultListener(new DelegationResultListener(ret)));
								}
							}
							
							public void exceptionOccurred(Exception exception)
							{
								if(exception instanceof ServiceNotFoundException)
								{
									ret.setResult(Boolean.FALSE);
								}
								else
								{
									super.exceptionOccurred(exception);
								}
							}
						}));
					}
				}));
				return ret;
			}
		}).addResultListener(new DelegationResultListener(ret));

		return ret;
	}

	/**
	 * 
	 */
	protected static IFuture checkComponentType(final String model, final IComponentFactory[] facts, final int i, 
		final IInternalAccess ia, final ClassLoader cl, final Collection allowedtypes)
	{
		final Future ret = new Future();
		if(i>=facts.length)
		{
			ret.setResult(Boolean.FALSE);
		}
		else
		{
			facts[i].getComponentType(model, null, cl)
				.addResultListener(ia.createResultListener(new DelegationResultListener(ret)
			{
				public void customResultAvailable(Object result)
				{
					if(result!=null)
					{
						ret.setResult(allowedtypes.contains(result));
					}
					else
					{
						checkComponentType(model, facts, i+1, ia, cl, allowedtypes)
							.addResultListener(ia.createResultListener(new DelegationResultListener(ret)));
					}
				}
			}));
		}
		return ret;
	}
	
	/**
	 * Test if a model is startable (e.g. a component).
	 * @param model The model.
	 * @return True, if startable (and should therefore also be loadable).
	 */
	public static IFuture isStartable(IExternalAccess exta, final String model)
	{
		Future ret = new Future();
		
		exta.scheduleStep(new IComponentStep<Boolean>()
		{
			@XMLClassname("isStartable")
			public IFuture<Boolean> execute(final IInternalAccess ia)
			{
				final Future ret = new Future();
				SServiceProvider.getService(ia.getServiceContainer(), ILibraryService.class, RequiredServiceInfo.SCOPE_PLATFORM)
					.addResultListener(ia.createResultListener(new DelegationResultListener(ret)
				{
					public void customResultAvailable(Object result)
					{
						final ILibraryService ls = (ILibraryService)result;
						
						SServiceProvider.getService(ia.getServiceContainer(), new ComponentFactorySelector(model, null, ls.getClassLoader()))
							.addResultListener(ia.createResultListener(new DelegationResultListener(ret)
						{
							public void customResultAvailable(Object result)
							{
								IComponentFactory fac = (IComponentFactory)result;
								fac.isStartable(model, null, ls.getClassLoader())
									.addResultListener(new DelegationResultListener(ret));
							}
							
							public void exceptionOccurred(Exception exception)
							{
								if(exception instanceof ServiceNotFoundException)
								{
									ret.setResult(Boolean.FALSE);
								}
								else
								{
									super.exceptionOccurred(exception);
								}
							}
						}));
					}			
				}));
				return ret;
			}
		}).addResultListener(new DelegationResultListener(ret));

		return ret;
	}

	/**
	 * Get a default icon for a file type.
	 */
	public static IFuture<Icon> getFileTypeIcon(IExternalAccess exta, final String type)
	{
		Future<Icon> ret = new Future<Icon>();
		
		exta.scheduleStep(new IComponentStep<Icon>()
		{
			@XMLClassname("getFileTypeIcon")
			public IFuture<Icon> execute(final IInternalAccess ia)
			{
				final Future ret = new Future();
				SServiceProvider.getService(ia.getServiceContainer(), new ComponentFactorySelector(type))
					.addResultListener(ia.createResultListener(new DelegationResultListener(ret)
				{
					public void customResultAvailable(Object result)
					{
						/* $if !android $ */
						IComponentFactory fac = (IComponentFactory)result;
						fac.getComponentTypeIcon(type).addResultListener(new DelegationResultListener(ret));
						/* $endif $ */
					}
					
					public void exceptionOccurred(Exception exception)
					{
						if(exception instanceof ServiceNotFoundException)
						{
							ret.setResult(null);
						}
						else
						{
							super.exceptionOccurred(exception);
						}
					}
				}));
				return ret;
			}
		}).addResultListener(new DelegationResultListener(ret));
		
		return ret;
	}

	/**
	 * Get a default icon for a file type.
	 */
	public static IFuture getProperty(IExternalAccess exta, final String type, final String key)
	{
		final Future ret = new Future();
		
		exta.scheduleStep(new IComponentStep<Object>()
		{
			@XMLClassname("getProperty")
			public IFuture<Object> execute(final IInternalAccess ia)
			{
				final Future ret = new Future();
				SServiceProvider.getServices(ia.getServiceContainer(), IComponentFactory.class)
					.addResultListener(new DelegationResultListener(ret)
				{
					public void customResultAvailable(Object result)
					{
						boolean found = false;
						if(result!=null)
						{
							for(Iterator it=((Collection)result).iterator(); it.hasNext() && !found; )
							{
								IComponentFactory fac = (IComponentFactory)it.next();
								if(SUtil.arrayToSet(fac.getComponentTypes()).contains(type))
								{
									Map res = fac.getProperties(type);
									if(res!=null && res.containsKey(key))
									{
										ret.setResult(res.get(key));
										found = true;
									}
								}
							}
							if(!found)
								ret.setResult(null);
						}
						else
						{
							ret.setResult(null);
						}
					}
					
					public void exceptionOccurred(Exception exception)
					{
						if(exception instanceof ServiceNotFoundException)
						{
							ret.setResult(null);
						}
						else
						{
							super.exceptionOccurred(exception);
						}
					}
				});
				return ret;
			}
		}).addResultListener(new DelegationResultListener(ret));
		
		return ret;
	}

	/**
	 * Get the file type of a model.
	 */
	public static IFuture<String> getFileType(IExternalAccess exta, final String model)
	{
		final Future ret = new Future();
		
		exta.scheduleStep(new IComponentStep<String>()
		{
			@XMLClassname("getFileType")
			public IFuture<String> execute(final IInternalAccess ia)
			{
				final Future ret = new Future();
				SServiceProvider.getService(ia.getServiceContainer(), ILibraryService.class, RequiredServiceInfo.SCOPE_PLATFORM)
					.addResultListener(ia.createResultListener(new DelegationResultListener(ret)
				{
					public void customResultAvailable(Object result)
					{
						final ILibraryService ls = (ILibraryService)result;
						
						SServiceProvider.getService(ia.getServiceContainer(), new ComponentFactorySelector(model, null, ls.getClassLoader()))
							.addResultListener(ia.createResultListener(new DelegationResultListener(ret)
						{
							public void customResultAvailable(Object result)
							{
								IComponentFactory fac = (IComponentFactory)result;
								fac.getComponentType(model, null, ls.getClassLoader())
									.addResultListener(new DelegationResultListener(ret));
							}
							
							public void exceptionOccurred(Exception exception)
							{
								if(exception instanceof ServiceNotFoundException)
								{
									ret.setResult(null);
								}
								else
								{
									super.exceptionOccurred(exception);
								}
							}
						}));
					}
				}));
				return ret;
			}
		}).addResultListener(new DelegationResultListener(ret));
		
		return ret;
	}
}
