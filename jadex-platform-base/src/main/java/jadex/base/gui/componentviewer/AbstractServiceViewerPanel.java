package jadex.base.gui.componentviewer;

import jadex.base.gui.plugin.IControlCenter;
import jadex.bridge.service.IService;
import jadex.commons.Properties;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;

import javax.swing.JComponent;

/**
 *  Simple default viewer panel.
 */
public abstract class AbstractServiceViewerPanel implements IServiceViewerPanel
{
	//-------- attributes --------
	
	/** The jcc. */
	protected IControlCenter jcc;
	
	/** The service. */
	protected IService service;
	
	//-------- methods --------
	
	/**
	 *  Called once to initialize the panel.
	 *  Called on the swing thread.
	 *  @param jcc	The jcc.
	 * 	@param component The component.
	 */
	public IFuture init(IControlCenter jcc, IService service)
	{
		this.jcc = jcc;
		this.service = service;
		return IFuture.DONE;
	}
	
	/**
	 *  Informs the panel that it should stop all its computation
	 */
	public IFuture shutdown()
	{
		return IFuture.DONE;
	}

	/**
	 *  The id used for mapping properties.
	 */
	public String getId()
	{
		return toString();
	}

	/**
	 *  The component to be shown in the gui.
	 *  @return	The component to be displayed.
	 */
	public abstract JComponent getComponent();

	/**
	 *  Advices the the panel to restore its properties from the argument
	 */
	public IFuture setProperties(Properties ps)
	{
		return new Future();
	}

	/**
	 *  Advices the panel provide its setting as properties (if any).
	 *  This is done on project close or save.
	 */
	public IFuture getProperties()
	{
		return new Future(null);
	}

	/**
	 *  Get the jcc.
	 *  @return the jcc.
	 */
	public IControlCenter getJCC()
	{
		return jcc;
	}
	
	/**
	 *  Get the service.
	 */
	public IService getService()
	{
		return service;
	}
}
