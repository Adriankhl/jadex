package jadex.base;

import jadex.base.fipa.CMSComponentDescription;
import jadex.bridge.ComponentIdentifier;
import jadex.bridge.IComponentIdentifier;
import jadex.bridge.IComponentInstance;
import jadex.bridge.IExternalAccess;
import jadex.bridge.ILocalResourceIdentifier;
import jadex.bridge.LocalResourceIdentifier;
import jadex.bridge.ResourceIdentifier;
import jadex.bridge.modelinfo.ConfigurationInfo;
import jadex.bridge.modelinfo.IArgument;
import jadex.bridge.modelinfo.IModelInfo;
import jadex.bridge.modelinfo.UnparsedExpression;
import jadex.bridge.service.search.SServiceProvider;
import jadex.bridge.service.types.cms.CreationInfo;
import jadex.bridge.service.types.cms.IComponentManagementService;
import jadex.bridge.service.types.factory.IComponentAdapter;
import jadex.bridge.service.types.factory.IComponentAdapterFactory;
import jadex.bridge.service.types.factory.IComponentFactory;
import jadex.commons.SReflect;
import jadex.commons.SUtil;
import jadex.commons.Tuple2;
import jadex.commons.future.DelegationResultListener;
import jadex.commons.future.ExceptionDelegationResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.FutureHelper;
import jadex.commons.future.IFuture;
import jadex.commons.future.IResultListener;
import jadex.javaparser.SJavaParser;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *  Starter class for  
 */
public class Starter
{
	//-------- constants --------

	/** The fallback platform configuration. */
	public static final String FALLBACK_PLATFORM_CONFIGURATION = "jadex/standalone/Platform.component.xml";

	/** The component factory to be used for platform component. */
	public static final String FALLBACK_COMPONENT_FACTORY = "jadex.component.ComponentComponentFactory";

//	/** The termination timeout. */
//	// Todo: use configuration/argument value if present.
//	public static final long	TERMINATION_TIMEOUT	= 20000;

	
	/** The configuration file. */
	public static final String CONFIGURATION_FILE = "conf";
	
	/** The configuration name. */
	public static final String CONFIGURATION_NAME = "configname";
	
	/** The platform name. */
	public static final String PLATFORM_NAME = "platformname";

	/** The component factory classname. */
	public static final String COMPONENT_FACTORY = "componentfactory";
	
	/** The adapter factory classname. */
	public static final String ADAPTER_FACTORY = "adapterfactory";
	
	/** The autoshutdown flag. */
	public static final String AUTOSHUTDOWN = "autoshutdown";

	/** The welcome flag. */
	public static final String WELCOME = "welcome";

	/** The component flag (for starting an additional component). */
	public static final String COMPONENT = "component";
	
	/** The parameter copy flag. */
	public static final String PARAMETERCOPY = "parametercopy";

	
	/** The reserved platform parameters. */
	public static final Set<String> RESERVED;
	
	static
	{
		RESERVED = new HashSet<String>();
		RESERVED.add(CONFIGURATION_FILE);
		RESERVED.add(CONFIGURATION_NAME);
		RESERVED.add(PLATFORM_NAME);
		RESERVED.add(COMPONENT_FACTORY);
		RESERVED.add(ADAPTER_FACTORY);
		RESERVED.add(AUTOSHUTDOWN);
		RESERVED.add(WELCOME);
		RESERVED.add(COMPONENT);
		RESERVED.add(PARAMETERCOPY);
	}
	
	/** The shutdown in progress flag. */
	protected static boolean	shutdown;
	
	//-------- static methods --------
	
	/**
	 *  Test if shutdown is in progress.
	 */
	public static boolean	isShutdown()
	{
		return shutdown;
	}
	
	/**
	 *  Main for starting the platform (with meaningful fallbacks)
	 *  @param args The arguments.
	 *  @throws Exception
	 */
	public static void main(String[] args)
	{
		createPlatform(args).addResultListener(new IResultListener<IExternalAccess>()
		{
			public void resultAvailable(final IExternalAccess access)
			{
//				Runtime.getRuntime().addShutdownHook(new Thread()
//				{
//					public void run()
//					{
//						try
//						{
////							System.out.println("killing: "+access.getComponentIdentifier().getPlatformName());
//							shutdown	= true;
//							access.killComponent().get(new ThreadSuspendable(), TERMINATION_TIMEOUT);
////							System.out.println("killed: "+access.getComponentIdentifier().getPlatformName());
//						}
//						catch(ComponentTerminatedException cte)
//						{
//							// Already killed.
//						}
//						catch(Throwable t)
//						{
//							t.printStackTrace();
//						}
//					}
//				});
				
//				// Continuously run garbage collector and finalizers.
//				Timer	gctimer	= new Timer();
//				gctimer.scheduleAtFixedRate(new TimerTask()
//				{
//					public void run()
//					{
//						System.gc();
//						System.runFinalization();
//					}
//				}, 1000, 1000);
				
				
				// Test CTRL-C shutdown behavior.
//				Timer	timer	= new Timer();
//				timer.schedule(new TimerTask()
//				{
//					public void run()
//					{
//						System.out.println(getClass().getName()+": Calling System.exit() for testing.");
//						System.exit(0);
//					}
//				}, 5000);
			}
			public void exceptionOccurred(Exception exception)
			{
				exception.printStackTrace();
				System.exit(-1);
			}
		});
	}
	
	/**
	 *  Create the platform.
	 *  @param args The command line arguments.
	 *  @return The external access of the root component.
	 */
	public static IFuture<IExternalAccess> createPlatform(String[] args)
	{
		final Future<IExternalAccess> ret = new Future<IExternalAccess>();
		try
		{
			// Absolute start time (for testing and benchmarking).
			final long starttime = System.currentTimeMillis();
		
			final Map<String, Object> cmdargs = new HashMap<String, Object>();	// Starter arguments (required for instantiation of root component)
			final Map<String, Object> compargs = new HashMap<String, Object>();	// Arguments of root component (platform)
			final List<String> components = new ArrayList<String>();	// Additional components to start
			for(int i=0; args!=null && i<args.length; i+=2)
			{
				String key = args[i].substring(1);
				Object val = args[i+1];
				if(!RESERVED.contains(key))
				{
					try
					{
						val = SJavaParser.evaluateExpression(args[i+1], null);
					}
					catch(Exception e)
					{
						System.out.println("Argument parse exception using as string: "+args[i]+" \""+args[i+1]+"\"");
					}
					compargs.put(key, val);
				}
				
				if(COMPONENT.equals(key))
				{
					components.add((String)val);
				}
				else
				{
					cmdargs.put(key, val);
				}
			}
			
			// Load the platform (component) model.
			final ClassLoader cl = Starter.class.getClassLoader();
			final String configfile = (String)cmdargs.get(CONFIGURATION_FILE)!=null? 
				(String)cmdargs.get(CONFIGURATION_FILE): FALLBACK_PLATFORM_CONFIGURATION;
			String cfclname = (String)cmdargs.get(COMPONENT_FACTORY)!=null? 
				(String)cmdargs.get(COMPONENT_FACTORY): FALLBACK_COMPONENT_FACTORY;
			Class<IComponentFactory> cfclass = SReflect.findClass(cfclname, null, cl);
			// The providerid for this service is not important as it will be thrown away 
			// after loading the first component model.
			final IComponentFactory cfac = cfclass.getConstructor(new Class[]{String.class})
				.newInstance(new Object[]{"rootid"});
			
			compargs.put(COMPONENT_FACTORY, cfac);
			
			// Hack: what to use as rid? should not have dependency to standalone.
//			final ResourceIdentifier rid = new ResourceIdentifier(null, 
//				"net.sourceforge.jadex:jadex-standalone-launch:2.1-SNAPSHOT");
			
//			System.out.println("Using config file: "+configfile);
			
			cfac.loadModel(configfile, null, null)//rid)
				.addResultListener(new ExceptionDelegationResultListener<IModelInfo, IExternalAccess>(ret)
			{
				public void customResultAvailable(final IModelInfo model) 
				{
					if(model.getReport()!=null)
						throw new RuntimeException("Error loading model:\n"+model.getReport().getErrorText());
					
					// Create an instance of the component.
					Object pfname = getArgumentValue(PLATFORM_NAME, model, cmdargs, compargs);
					String	platformname; 
					if(pfname==null)
					{
						try
						{
							String	name	= InetAddress.getLocalHost().getHostName();
							// Replace special characters used in component ids.
							if(name!=null)
							{
								name	= name.replace('.', '$'); // Dot in host name on Mac !?
								name	= name.replace('@', '$'); // Probably not needed, but just to be sure.
							}
							platformname = SUtil.createUniqueId(name, 3);
						}
						catch(UnknownHostException e)
						{
							platformname = SUtil.createUniqueId("platform", 3);
						}
					}
					else
					{
						platformname	= pfname.toString(); 
					}
					
					final IComponentIdentifier cid = new ComponentIdentifier(platformname);
					if(IComponentIdentifier.LOCAL.get()==null)
						IComponentIdentifier.LOCAL.set(cid);
					// Hack!!! Autoshutdown!?

					// Hack: change rid afterwards?!
//					String src = SUtil.getCodeSource(model.getFilename(), model.getPackage());
//					URL url = SUtil.toURL(src);
//					rid.setLocalIdentifier(new LocalResourceIdentifier(cid, url));
//					URL url = model.getClass().getProtectionDomain().getCodeSource().getLocation();
					ResourceIdentifier rid = (ResourceIdentifier)model.getResourceIdentifier();
					ILocalResourceIdentifier lid = rid.getLocalIdentifier();
					rid.setLocalIdentifier(new LocalResourceIdentifier(cid, lid.getUrl()));
					
					cfac.getComponentType(configfile, null, model.getResourceIdentifier())
						.addResultListener(new ExceptionDelegationResultListener<String, IExternalAccess>(ret)
					{
						public void customResultAvailable(String ctype) 
						{
							try
							{
								Boolean autosd = (Boolean)getArgumentValue(AUTOSHUTDOWN, model, cmdargs, compargs);
								final CMSComponentDescription desc = new CMSComponentDescription(cid, ctype, null, null, 
									autosd, model.getFullName(), null, model.getResourceIdentifier());
								
								Object	af = getArgumentValue(ADAPTER_FACTORY, model, cmdargs, compargs);
								if(af==null)
								{
									ret.setException(new RuntimeException("No adapterfactory found."));
								}
								Class<?> afclass = af instanceof Class ? (Class<?>)af : SReflect.findClass(af.toString(), null, cl);
								final IComponentAdapterFactory afac = (IComponentAdapterFactory)afclass.newInstance();
								
								final Future<IComponentInstance>	instancefut	= new Future<IComponentInstance>();
								Future<Void> future = new Future<Void>();
								future.addResultListener(new ExceptionDelegationResultListener<Void, IExternalAccess>(ret)
								{
									public void customResultAvailable(Void result)
									{
										instancefut.addResultListener(new ExceptionDelegationResultListener<IComponentInstance, IExternalAccess>(ret)
										{
											public void customResultAvailable(final IComponentInstance instance)
											{
												startComponents(0, components, instance)
													.addResultListener(new ExceptionDelegationResultListener<Void, IExternalAccess>(ret)
												{
													public void customResultAvailable(Void result)
													{
														if(Boolean.TRUE.equals(getArgumentValue(WELCOME, model, cmdargs, compargs)))
														{
															long startup = System.currentTimeMillis() - starttime;
															// platform.logger.info("Platform startup time: " + startup + " ms.");
															System.out.println(desc.getName()+" platform startup time: " + startup + " ms.");
														}
														ret.setResult(instance.getExternalAccess());
													}
													
													public void exceptionOccurred(Exception exception)
													{
														// On exception in init: kill platform.
														instance.getExternalAccess().killComponent();
														super.exceptionOccurred(exception);
													}
												});
											}
										});
									}
								});
								
								boolean copy = !Boolean.FALSE.equals(getArgumentValue(PARAMETERCOPY, model, cmdargs, compargs));
								// what about platform result listener?!
								cfac.createComponentInstance(desc, afac, model, getConfigurationName(model, cmdargs), compargs, null, null, copy, null, future)
									.addResultListener(new ExceptionDelegationResultListener<Tuple2<IComponentInstance, IComponentAdapter>, IExternalAccess>(ret)
								{
									public void customResultAvailable(Tuple2<IComponentInstance, IComponentAdapter> root)
									{
										instancefut.setResult(root.getFirstEntity());
										IComponentAdapter adapter = root.getSecondEntity();
										
										// Execute init steps of root component on main thread (i.e. platform)
										// until platform is ready to run by itself.
										boolean again = true;
										while(again && !ret.isDone())
										{
											again = afac.executeStep(adapter) || FutureHelper.notifyStackedListeners();
										}
										
										// Start normal execution of root component (i.e. platform) unless an error occurred during init.
										if(!ret.isDone())
										{
//											try
//											{
//												Thread.sleep(300000);
//											}
//											catch(InterruptedException e)
//											{
//												// TODO Auto-generated catch block
//												e.printStackTrace();
//											}
											afac.initialWakeup(adapter);
										}
									}
								});
							}
							catch(Exception e)
							{
								ret.setException(e);
							}
						};
					});

					if(cid.equals(IComponentIdentifier.LOCAL.get()))
						IComponentIdentifier.LOCAL.set(null);
				}
			});
	//		System.out.println("Model: "+model);
		}
		catch(Exception e)
		{
//			e.printStackTrace();
			ret.setException(e);
		}
		
		return ret;
	}
	
	/**
	 *  Get an argument value from the command line or the model.
	 *  Also puts parsed value into component args to be available at instance.
	 */
	protected static Object getArgumentValue(String name, IModelInfo model, Map<String, Object> cmdargs, Map<String, Object> compargs)
	{
		String	configname	= getConfigurationName(model, cmdargs);
		ConfigurationInfo	config	= configname!=null
			? model.getConfiguration(configname) 
			: model.getConfigurations().length>0 ? model.getConfigurations()[0] : null;
		
		Object val = cmdargs.get(name);
		if(val==null)
		{
			boolean	found	= false;
			if(config!=null)
			{
				UnparsedExpression[]	upes	= config.getArguments();
				for(int i=0; !found && i<upes.length; i++)
				{
					if(name.equals(upes[i].getName()))
					{
						found	= true;
						val	= upes[i];
					}
				}
			}
			if(!found)
			{
				 IArgument	arg	= model.getArgument(name);
				 if(arg!=null)
				 {
					val	= arg.getDefaultValue(); 
				 }
			}
			val	= UnparsedExpression.getParsedValue(val, model.getAllImports(), null, Starter.class.getClassLoader());
//			val	= UnparsedExpression.getParsedValue(val, model.getAllImports(), null, model.getClassLoader());
		}
		else if(val instanceof String)
		{
			// Try to parse value from command line.
			try
			{
				Object newval	= SJavaParser.evaluateExpression((String)val, null);
				if(newval!=null)
				{
					val	= newval;
				}
			}
			catch(RuntimeException e)
			{
			}
		}
		compargs.put(name, val);
		return val;
	}

	/**
	 * Get the configuration name.
	 */
	protected static String	getConfigurationName(IModelInfo model,	Map<String, Object> cmdargs)
	{
		String	configname	= (String)cmdargs.get(CONFIGURATION_NAME);
		if(configname==null)
		{
			Object	val	= null;
			IArgument	arg	= model.getArgument(CONFIGURATION_NAME);
			if(arg!=null)
			{
				val	= arg.getDefaultValue();
			}
			val	= UnparsedExpression.getParsedValue(val, model.getAllImports(), null, Starter.class.getClassLoader());
//			val	= UnparsedExpression.getParsedValue(val, model.getAllImports(), null, model.getClassLoader());
			configname	= val!=null ? val.toString() : null;
		}
		return configname;
	}

	/**
	 *  Loop for starting components.
	 *  @param i Number to start.
	 *  @param components The list of components.
	 *  @param instance The instance.
	 *  @return True, when done.
	 */
	protected static IFuture<Void> startComponents(final int i, final List<String> components, final IComponentInstance instance)
	{
		final Future<Void>	ret	= new Future<Void>();
		
		if(i<components.size())
		{
			SServiceProvider.getServiceUpwards(instance.getServiceContainer(), IComponentManagementService.class)
				.addResultListener(new ExceptionDelegationResultListener<IComponentManagementService, Void>(ret)
			{
				public void customResultAvailable(IComponentManagementService cms)
				{
					String	name	= null;
					String	config	= null;
					String	comp	= (String)components.get(i);
					int	i1	= comp.indexOf(':');
					if(i1!=-1)
					{
						name	= comp.substring(0, i1);
						comp	= comp.substring(i1+1);
					}
					int	i2	= comp.indexOf('(');
					if(i2!=-1)
					{
						if(comp.endsWith(")"))
						{
							config	= comp.substring(i2+1, comp.length()-1);
							comp	= comp.substring(0, i2);
						}
						else
						{
							throw new RuntimeException("Component specification does not match scheme [<name>:]<type>[(<config>)] : "+components.get(i));
						}
					}
					
					cms.createComponent(name, comp, new CreationInfo(config, null), null)
						.addResultListener(new ExceptionDelegationResultListener<IComponentIdentifier, Void>(ret)
					{
						public void customResultAvailable(IComponentIdentifier result)
						{
							startComponents(i+1, components, instance)
								.addResultListener(new DelegationResultListener<Void>(ret));
						}
					});
				}
			});
		}
		else
		{
			ret.setResult(null);
		}
		
		return ret;
	}	
}

