package jadex.bpmn;

import jadex.bpmn.model.MBpmnModel;
import jadex.bpmn.runtime.BpmnInterpreter;
import jadex.bridge.IComponentAdapterFactory;
import jadex.bridge.IComponentDescription;
import jadex.bridge.IComponentFactory;
import jadex.bridge.IExternalAccess;
import jadex.bridge.ILoadableComponentModel;
import jadex.commons.Future;
import jadex.commons.IFuture;
import jadex.commons.SGUI;
import jadex.service.BasicService;
import jadex.service.IServiceContainer;

import java.util.HashMap;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.UIDefaults;

/**
 *  Foctory for loading bpmn processes.
 */
public class BpmnFactory extends BasicService implements IComponentFactory
{
	//-------- constants --------
	
	/** The micro agent file type. */
	public static final String	FILETYPE_BPMNPROCESS = "BPMN Process";
	
	/**
	 * The image icons.
	 */
	protected static final UIDefaults icons = new UIDefaults(new Object[]
	{
		"bpmn_process",	SGUI.makeIcon(BpmnFactory.class, "/jadex/bpmn/images/bpmn_process.png"),
	});
	
	//-------- attributes --------
	
	/** The WFMS */
	protected IServiceContainer container;
	
	/** Running process instances */
	protected Map processes;
	
	/** The model loader */
	protected BpmnModelLoader loader;
	
	/** The properties. */
	protected Map properties;
	
	//-------- constructors --------
	
	/**
	 *  Create a new BpmnProcessService.
	 */
	public BpmnFactory(IServiceContainer container, Map properties)
	{
		this.container = container;
		this.processes = new HashMap();
		this.loader = new BpmnModelLoader();
		this.properties	= properties;
	}
	
	//-------- methods --------
	
	/**
	 *  Start the service.
	 * /
	public synchronized IFuture	startService()
	{
		return super.startService();
	}*/
	
	/**
	 *  Shutdown the service.
	 *  @param listener The listener.
	 * /
	public synchronized IFuture	shutdownService()
	{
		return super.shutdownService();
	}*/
	
	/**
	 *  Load a  model.
	 *  @param model The model (e.g. file name).
	 *  @param The imports (if any).
	 *  @return The loaded model.
	 */
	public ILoadableComponentModel loadModel(String model, String[] imports, ClassLoader classloader)
	{
		MBpmnModel ret = null;
//		System.out.println("filename: "+filename);
		try
		{
			ret = loader.loadBpmnModel(model, imports, classloader);
//			ClassLoader	cl = libservice.getClassLoader();
//			ret.setClassloader(cl);
		}
		catch(Exception e)
		{
			throw new RuntimeException(e);
		}
		
		return ret;
	}
	
	/**
	 * Starts a BPMN process
	 * @param name name of the BPMN model
	 * @param stepmode if true, the process will start in step mode
	 * @return instance name
	 * /
	public Object startProcess(String modelname, final Object id, Map arguments, boolean stepmode)
	{
		try
		{
			IModelRepositoryService mr = (IModelRepositoryService) wfms.getService(IModelRepositoryService.class);
//			String path = mr.getProcessModelPath(modelname);
			MBpmnModel model = loader.loadBpmnModel(modelname, mr.getImports());
			
			final BpmnInstance instance = new BpmnInstance(model);
			instance.setWfms(wfms);
			BpmnExecutor executor = new BpmnExecutor(instance, true);
			
			processes.put(id, executor);
			instance.addChangeListener(new IChangeListener()
			{
				public void changeOccurred(ChangeEvent event)
				{
					if (instance.isFinished(null, null))
					{
						synchronized (BpmnFactory.this)
						{
							processes.remove(id);
						}
					}
				}
			});
			
			executor.setStepmode(stepmode);
		}
		catch(Exception e)
		{
			throw new RuntimeException(e);
		}
		return id;
	}*/
	
	/**
	 *  Test if a model can be loaded by the factory.
	 *  @param model The model (e.g. file name).
	 *  @param The imports (if any).
	 *  @return True, if model can be loaded.
	 */
	public boolean isLoadable(String model, String[] imports, ClassLoader classloader)
	{
		return model.endsWith(".bpmn");
	}

	/**
	 *  Test if a model is startable (e.g. an component).
	 *  @param model The model (e.g. file name).
	 *  @param The imports (if any).
	 *  @return True, if startable (and loadable).
	 */
	public boolean isStartable(String model, String[] imports, ClassLoader classloader)
	{
		return model.endsWith(".bpmn");
	}
	
	/**
	 *  Get the names of ADF file types supported by this factory.
	 */
	public String[] getComponentTypes()
	{
		return new String[]{FILETYPE_BPMNPROCESS};
	}

	/**
	 *  Get a default icon for a file type.
	 */
	public Icon getComponentTypeIcon(String type)
	{
		return type.equals(FILETYPE_BPMNPROCESS)? icons.getIcon("bpmn_process") : null;
	}

	/**
	 *  Get the component type of a model.
	 *  @param model The model (e.g. file name).
	 *  @param The imports (if any).
	 */
	public String getComponentType(String model, String[] imports, ClassLoader classloader)
	{
		return model.toLowerCase().endsWith(".bpmn") ? FILETYPE_BPMNPROCESS: null;
	}
	
	/**
	 * Create a component instance.
	 * @param adapter The component adapter.
	 * @param model The component model.
	 * @param config The name of the configuration (or null for default configuration) 
	 * @param arguments The arguments for the agent as name/value pairs.
	 * @param parent The parent component (if any).
	 * @return An instance of a component.
	 */
	public Object[] createComponentInstance(IComponentDescription desc, IComponentAdapterFactory factory, ILoadableComponentModel model, String config, Map arguments, IExternalAccess parent)
	{
		BpmnInterpreter interpreter = new BpmnInterpreter(desc, factory, (MBpmnModel)model, arguments, config, parent, null, null, null);
		return new Object[]{interpreter, interpreter.getComponentAdapter()};
	}
	
	/**
	 *  Get the properties.
	 *  Arbitrary properties that can e.g. be used to
	 *  define kernel-specific settings to configure tools.
	 *  @param type	The component type. 
	 *  @return The properties or null, if the component type is not supported by this factory.
	 */
	public Map	getProperties(String type)
	{
		return FILETYPE_BPMNPROCESS.equals(type)
		? properties : null;
	}
}
