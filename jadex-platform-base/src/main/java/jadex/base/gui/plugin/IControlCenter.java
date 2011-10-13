package jadex.base.gui.plugin;

import jadex.base.gui.CMSUpdateHandler;
import jadex.bridge.IExternalAccess;
import jadex.bridge.IResourceIdentifier;

import javax.swing.JComponent;

/**
 *  Control center interface providing useful methods
 *  that may be called from plugins. 
 */
public interface IControlCenter
{
	//-------- platform related methods --------
	
	/**
	 *  Get the external access of the administered platform (potentially remote).
	 *  @return The external access.
	 */
	public IExternalAccess	getPlatformAccess();
		
	//-------- global JCC methods --------
	
	/**
	 *  Get the external access of the JCC component (always local).
	 *  @return The external access.
	 */
	public IExternalAccess	getJCCAccess();
	
	/**
	 *  Get the cms update handler shared by all tools.
	 */
	// Only one per JCC as many plugins listen to many remote platforms, too.
	// So its not useful to have separate handlers for each administered remote platform.
	public CMSUpdateHandler getCMSHandler();
	
	//-------- GUI helper methods --------
	
	/**
	 *  Add a new platform control center
	 *  or switch to tab if already exists.
	 */
	public void	showPlatform(IExternalAccess platformaccess);

	/**
	 *  Set a text to be displayed in the status bar.
	 *  The text will be removed automatically after
	 *  some delay (or replaced by some other text).
	 */
	public void setStatusText(String text);
	
	/**
	 *  Add a component to the status bar.
	 *  @param id	An id for later reference.
	 *  @param comp	An id for later reference.
	 */
	public void	addStatusComponent(Object id, JComponent comp);

	/**
	 *  Remove a previously added component from the status bar.
	 *  @param id	The id used for adding the component.
	 */
	public void	removeStatusComponent(Object id);
	
	/**
	 *  Display an error dialog.
	 * 
	 *  @param errortitle The title to use for an error dialog (required).
	 *  @param errormessage An optional error message displayed before the exception.
	 *  @param exception The exception (if any).
	 */
	public void displayError(final String errortitle, String errormessage, Exception exception);
	
	/**
	 *  Get the resource identifier.
	 */
	public ClassLoader getClassLoader(IResourceIdentifier rid);
}
