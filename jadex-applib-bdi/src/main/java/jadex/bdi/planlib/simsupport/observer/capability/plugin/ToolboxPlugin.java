package jadex.bdi.planlib.simsupport.observer.capability.plugin;

import jadex.bdi.planlib.simsupport.observer.capability.ObserverCenter;
import jadex.bdi.runtime.IExternalAccess;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class ToolboxPlugin implements IObserverCenterPlugin
{
	/** Plugin name
	 */
	private static final String NAME = "Toolbox";
	
	/** The main panel
	 */
	private JPanel mainPanel_;
	
	/** The toolbar
	 */
	private JToolBar toolBar_;
	
	/** Status descriptor
	 */
	private JLabel statusLabelDesc_;
	
	/** Status label
	 */
	private JLabel statusLabel_;
	
	/** The observer center
	 */
	private ObserverCenter observerCenter_;
	
	public ToolboxPlugin()
	{
		mainPanel_ = new JPanel(new GridBagLayout());
		mainPanel_.setBorder(new TitledBorder(NAME));
		
		mainPanel_.setMinimumSize(new Dimension(50, 50));
		
		JPanel themePanel = new JPanel(new GridBagLayout());
		themePanel.setBorder(new TitledBorder("Tools"));
		toolBar_ = new JToolBar("Tools", JToolBar.VERTICAL);
		JScrollPane toolScrollPane = new JScrollPane(toolBar_);
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.weighty = 1.0;
		c.weightx = 1.0;
		c.anchor = GridBagConstraints.CENTER;
		c.fill = GridBagConstraints.HORIZONTAL;
		themePanel.add(toolScrollPane, c);
		
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.weighty = 1.0;
		c.weightx = 1.0;
		c.anchor = GridBagConstraints.NORTH;
		c.fill = GridBagConstraints.HORIZONTAL;
		mainPanel_.add(themePanel, c);
		
		JPanel statusPanel = new JPanel(new GridLayout(1, 2));
		statusLabelDesc_ = new JLabel();
		statusPanel.add(statusLabelDesc_);
		statusLabel_ = new JLabel();
		statusPanel.add(statusLabel_);
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.weighty = 1.0;
		c.weightx = 1.0;
		c.anchor = GridBagConstraints.SOUTH;
		c.fill = GridBagConstraints.HORIZONTAL;
		mainPanel_.add(statusPanel, c);
	}
	
	/** Starts the plugin
	 *  
	 *  @param the observer center
	 */
	public void start(ObserverCenter main)
	{
	}
	
	/** Stops the plugin
	 *  
	 */
	public void shutdown()
	{
	}
	
	/** Returns the name of the plugin
	 *  
	 *  @return name of the plugin
	 */
	public String getName()
	{
		return NAME;
	}
	
	/** Returns the viewable component of the plugin
	 *  
	 *  @return viewable component of the plugin
	 */
	public Component getView()
	{
		return mainPanel_;
	}
	
	/** Refreshes the display
	 */
	public void refresh()
	{
	}
}
