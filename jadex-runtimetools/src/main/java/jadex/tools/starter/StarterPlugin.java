package jadex.tools.starter;

import jadex.adapter.base.SComponentExecutionService;
import jadex.adapter.base.SComponentFactory;
import jadex.bridge.IApplicationContext;
import jadex.bridge.IComponentDescription;
import jadex.bridge.IComponentExecutionService;
import jadex.bridge.IComponentIdentifier;
import jadex.bridge.IComponentListener;
import jadex.bridge.IContext;
import jadex.bridge.IContextService;
import jadex.bridge.ILoadableComponentModel;
import jadex.bridge.IPlatform;
import jadex.commons.ChangeEvent;
import jadex.commons.IChangeListener;
import jadex.commons.Properties;
import jadex.commons.Property;
import jadex.commons.SGUI;
import jadex.commons.SUtil;
import jadex.commons.concurrent.IResultListener;
import jadex.tools.common.AgentTreeTable;
import jadex.tools.common.ApplicationTreeTable;
import jadex.tools.common.IMenuItemConstructor;
import jadex.tools.common.PopupBuilder;
import jadex.tools.common.jtreetable.DefaultTreeTableNode;
import jadex.tools.common.jtreetable.TreeTableNodeType;
import jadex.tools.common.modeltree.FileNode;
import jadex.tools.common.modeltree.IExplorerTreeNode;
import jadex.tools.common.modeltree.ModelExplorer;
import jadex.tools.common.modeltree.ModelExplorerTreeModel;
import jadex.tools.common.plugin.AbstractJCCPlugin;
import jadex.tools.common.plugin.IAgentListListener;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIDefaults;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;

/**
 *  The starter plugin.
 */
public class StarterPlugin extends AbstractJCCPlugin implements  IAgentListListener
{
	//-------- constants --------

	/**
	 * The image icons.
	 */
	protected static final UIDefaults icons = new UIDefaults(new Object[]
	{
		"resume_agent", SGUI.makeIcon(StarterPlugin.class, "/jadex/tools/common/images/new_agent_big.png"),
		"suspend_agent", SGUI.makeIcon(StarterPlugin.class, "/jadex/tools/common/images/new_agent_szzz_big.png"),
		"kill_agent", SGUI.makeIcon(StarterPlugin.class, "/jadex/tools/common/images/new_killagent.png"),
		"kill_platform", SGUI.makeIcon(StarterPlugin.class, "/jadex/tools/common/images/new_killplatform.png"),
		"starter", SGUI.makeIcon(StarterPlugin.class, "/jadex/tools/common/images/new_starter.png"),
		"starter_sel", SGUI.makeIcon(StarterPlugin.class, "/jadex/tools/common/images/new_starter_sel.png"),
		"start_agent",	SGUI.makeIcon(StarterPlugin.class, "/jadex/tools/common/images/start.png"),
		"agent_suspended", SGUI.makeIcon(StarterPlugin.class, "/jadex/tools/common/images/new_agent_szzz.png"),
		"checking_menu",	SGUI.makeIcon(StarterPlugin.class, "/jadex/tools/common/images/new_agent_broken.png")
	});

	/*
	protected static final java.io.FileFilter ADF_FILTER = new FileFilter()
	{
		public boolean accept(File pathname)
		{
			return pathname.isDirectory() || isJadexFilename(pathname.getName());
		}
	};
	
	protected static final java.io.FileFilter AGENT_FILTER = new FileFilter()
	{
		public boolean accept(File pathname)
		{
			return pathname.isDirectory() || isAgentFilename(pathname.getName());
		}
	};
	
	protected static final java.io.FileFilter CAPABILITY_FILTER = new FileFilter()
	{
		public boolean accept(File pathname)
		{
			return pathname.isDirectory() || isCapabilityFilename(pathname.getName());
		}
	};
	
	protected static final java.io.FileFilter JAVAAGENT_FILTER = new FileFilter()
	{
		public boolean accept(File pathname)
		{
			return pathname.isDirectory() || pathname.getName().endsWith("Agent.class");
		}
	};
	*/

	//-------- attributes --------

	/** The starter panel. */
	private StarterPanel spanel;

	/** The panel showing the classpath models. */
	private ModelExplorer mpanel;

	/** The menu item for enabling/disabling agent model checking. */
	private JCheckBoxMenuItem	checkingmenu;
	
	/** The agent instances in a tree. */
	private AgentTreeTable agents;
	
	/** The application instances in a tree. */
	private ApplicationTreeTable applications;

	/** A split panel. */
	private JSplitPane lsplit;

	/** A split panel. */
    private JSplitPane csplit;

	//-------- methods --------
	
	/**
	 *  Get the name.
	 *  @return The name.
	 *  @see jadex.tools.common.plugin.IControlCenterPlugin#getName()
	 */
	public String getName()
	{
		return "Starter";
	}

	/**
	 *  Create tool bar.
	 *  @return The tool bar.
	 */
	public JComponent[] createToolBar()
	{
		JComponent[] ret = new JComponent[6];
		JButton b;

		b = new JButton(mpanel.ADD_PATH);
		b.setBorder(null);
		b.setToolTipText(b.getText());
		b.setText(null);
		b.setEnabled(true);
		ret[0] = b;
		
		b = new JButton(mpanel.REMOVE_PATH);
		b.setBorder(null);
		b.setToolTipText(b.getText());
		b.setText(null);
		b.setEnabled(true);
		ret[1] = b;
		
		b = new JButton(mpanel.REFRESH);
		b.setBorder(null);
		b.setToolTipText(b.getText());
		b.setText(null);
		b.setEnabled(true);
		ret[2] = b;
		
		JSeparator	separator	= new JToolBar.Separator();
		separator.setOrientation(JSeparator.VERTICAL);
		ret[3] = separator;
		
		/*b = new JButton(GENERATE_JADEXDOC);
		b.setBorder(null);
		b.setToolTipText(b.getText());
		b.setText(null);
		b.setEnabled(true);
		bar.add(b);
		
		separator = new JToolBar.Separator();
		separator.setOrientation(JSeparator.VERTICAL);
		bar.add(separator);*/
		
		b = new JButton(KILL_AGENT);
		b.setBorder(null);
		b.setToolTipText(b.getText());
		b.setText(null);
		b.setEnabled(true);
		ret[4] = b;
		
		b = new JButton(KILL_PLATFORM);
		b.setBorder(null);
		b.setToolTipText(b.getText());
		b.setText(null);
		b.setEnabled(true);
		ret[5] = b;

		return ret;
	}
	
	/**
	 *  Create menu bar.
	 *  @return The menu bar.
	 */
	public JMenu[] createMenuBar()
	{
		JMenu[]	menu	= mpanel.createMenuBar();
		this.checkingmenu = new JCheckBoxMenuItem(TOGGLE_CHECKING);
		this.checkingmenu.setSelected(true);	// Default: on
		menu[0].insert(checkingmenu, 1);	// Hack??? Should not assume position.
		return menu;
	}
	
	/**
	 *  Create main panel.
	 *  @return The main panel.
	 */
	public JComponent createView()
	{
		csplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);
		csplit.setOneTouchExpandable(true);

		lsplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true);
		lsplit.setOneTouchExpandable(true);
		lsplit.setResizeWeight(0.7);

		mpanel = new ModelExplorer(getJCC().getServiceContainer(), new StarterNodeFunctionality(this));
//		mpanel.setAction(FileNode.class, new INodeAction()
//		{
//			public void validStateChanged(TreeNode node, boolean valid)
//			{
//				String file1 = ((FileNode)node).getFile().getAbsolutePath();
//				String file2 = spanel.getFilename();
//				//System.out.println(file1+" "+file2);
//				if(file1!=null && file1.equals(file2))
//				{
//					spanel.reloadModel(file1);
//				}
//			}
//		});
		mpanel.setPopupBuilder(new PopupBuilder(new Object[]{new StartAgentMenuItemConstructor(), mpanel.ADD_PATH,
			mpanel.REMOVE_PATH, mpanel.REFRESH}));
		mpanel.addTreeSelectionListener(new TreeSelectionListener()
		{
			public void valueChanged(TreeSelectionEvent e)
			{
				Object	node = mpanel.getLastSelectedPathComponent();
				if(node instanceof FileNode)
				{
					// Models have to be loaded with absolute path.
					// An example to facilitate understanding:
					// root
					//  +-classes1
					//  |  +- MyAgent.agent.xml
					//  +-classes2
					//  |  +- MyAgent.agent.xml

					String model = ((FileNode)node).getRelativePath();
//					if(getJCC().getAgent().getPlatform().getAgentFactory().isLoadable(model))
					if(SComponentFactory.isLoadable(getJCC().getServiceContainer(), model))
					{
						loadModel(model);
					}
//					else if(getJCC().getAgent().getPlatform().getApplicationFactory().isLoadable(model))
//					{
//						loadModel(model);
//					}
				}
			}
		});
		MouseListener ml = new MouseAdapter()
		{
			public void mousePressed(MouseEvent e)
			{
				int row = mpanel.getRowForLocation(e.getX(), e.getY());
				if(row != -1)
				{
					if(e.getClickCount() == 2)
					{
						Object	node = mpanel.getLastSelectedPathComponent();
						if(node instanceof FileNode)
						{
							mpanel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
							String type = ((FileNode)node).getFile().getAbsolutePath();
//							if(getJCC().getAgent().getPlatform().getAgentFactory().isStartable(type))
							if(SComponentFactory.isStartable(getJCC().getServiceContainer(), type))
								createComponent(type, null, null, null);
							mpanel.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
						}
					}
				}
      		}
  		};
  		mpanel.addMouseListener(ml);

		agents = new AgentTreeTable(((IPlatform)getJCC().getServiceContainer()).getName());
		agents.setMinimumSize(new Dimension(0, 0));
		agents.getTreetable().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		
		// Change agent node type to enable suspended icon for agents. 
		agents.addNodeType(new TreeTableNodeType(AgentTreeTable.NODE_AGENT, 
			new Icon[0], new String[]{"name", "address"}, new String[]{"Name", "Address"})
		{
			public Icon selectIcon(Object value)
			{
				Icon	ret;
				IComponentDescription ad = (IComponentDescription)((DefaultTreeTableNode)value).getUserObject();
				if(IComponentDescription.STATE_SUSPENDED.equals(ad.getState()))
				{
					ret = StarterPlugin.icons.getIcon("agent_suspended");
				}
				else
				{
					ret	= AgentTreeTable.icons.getIcon(AgentTreeTable.NODE_AGENT);
				}
				//System.out.println(value+" "+ad.getState());
				return ret;
			}
		});
		agents.getNodeType(AgentTreeTable.NODE_AGENT).addPopupAction(KILL_AGENT);
		agents.getNodeType(AgentTreeTable.NODE_AGENT).addPopupAction(SUSPEND_AGENT);
		agents.getNodeType(AgentTreeTable.NODE_AGENT).addPopupAction(RESUME_AGENT);
		agents.getNodeType(AgentTreeTable.NODE_PLATFORM).addPopupAction(KILL_PLATFORM);
		agents.getTreetable().getSelectionModel().setSelectionInterval(0, 0);
		
		applications = new ApplicationTreeTable(((IPlatform)getJCC().getServiceContainer()).getName());
		applications.setMinimumSize(new Dimension(0, 0));
		applications.getTreetable().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		
		applications.getNodeType(ApplicationTreeTable.NODE_APPLICATION).addPopupAction(KILL_APPLICATION);
		applications.getNodeType(AgentTreeTable.NODE_PLATFORM).addPopupAction(KILL_PLATFORM);
		applications.getTreetable().getSelectionModel().setSelectionInterval(0, 0);
		applications.setColumnWidths(new int[]{200});
		
		IContextService cs = (IContextService)jcc.getServiceContainer().getService(IContextService.class);
		if(cs!=null)
		{
			cs.addContextListener(new IChangeListener()
			{
				public void changeOccurred(final ChangeEvent event)
				{
					if(IContextService.EVENT_TYPE_CONTEXT_CREATED.equals(event.getType()))
					{
						SwingUtilities.invokeLater(new Runnable()
						{
							public void run()
							{
								applications.addApplication((IApplicationContext)event.getValue());
							}
						});
					}
					else if(IContextService.EVENT_TYPE_CONTEXT_DELETED.equals(event.getType()))
					{
						SwingUtilities.invokeLater(new Runnable()
						{
							public void run()
							{
	//							System.out.println("Remove elem: "+event.getValue());
								applications.removeApplication((IApplicationContext)event.getValue());
							}
						});
					}
				}	
			});
		}
		
		JTabbedPane tp = new JTabbedPane();
		tp.addTab("agents", agents);
		tp.addTab("applications", applications);
		
		lsplit.add(new JScrollPane(mpanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
			JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));
		lsplit.add(tp);
		lsplit.setDividerLocation(300);

		csplit.add(lsplit);
		spanel = new StarterPanel(this);
		csplit.add(spanel);
		csplit.setDividerLocation(180);
            			
//		jcc.addAgentListListener(this);
		
		// todo: ?! is this ok?
		
		IComponentExecutionService ces = (IComponentExecutionService)jcc.getServiceContainer().getService(IComponentExecutionService.class);
		ces.getComponentDescriptions(new IResultListener()
		{
			public void resultAvailable(Object result)
			{
				IComponentDescription[] res = (IComponentDescription[])result;
				for(int i=0; i<res.length; i++)
					agentBorn(res[i]);
			}
			
			public void exceptionOccurred(Exception exception)
			{
			}
		});
		ces.addComponentListener(new IComponentListener()
		{
			public void componentRemoved(Object desc)
			{
				agentDied((IComponentDescription)desc);
			}
			
			public void componentAdded(Object desc)
			{
				agentBorn((IComponentDescription)desc);
			}
		});

//		SwingUtilities.invokeLater(new Runnable()
//		{
//			public void run()
//			{
//				agents.adjustColumnWidths();
//			}
//		});

		return csplit;
	}
	
	/**
	 * @return the starter icon
	 * @see jadex.tools.common.plugin.IControlCenterPlugin#getToolIcon()
	 */
	public Icon getToolIcon(boolean selected)
	{
		return selected? icons.getIcon("starter_sel"): icons.getIcon("starter");
	}

	/**
	 * Load the properties.
	 */
	public void setProperties(Properties props)
	{
//		System.out.println("Starter set props: "+props);
		
		Properties	mpanelprops	= props.getSubproperty("mpanel");
		if(mpanelprops!=null)
			mpanel.setProperties(mpanelprops);
		Properties	spanelprops	= props.getSubproperty("spanel");
		if(spanelprops!=null)
			spanel.setProperties(spanelprops);

		lsplit.setDividerLocation(props.getIntProperty("leftsplit.location"));
		csplit.setDividerLocation(props.getIntProperty("mainsplit.location"));

		checkingmenu.setSelected(props.getBooleanProperty("checking"));
		
		Properties ps = props.getSubproperty("agents");
		if(ps!=null)
			agents.setProperties(ps);
	}

	/**
	 * Save the properties.
	 * @param props
	 */
	public Properties	getProperties()
	{
		Properties	props	= new Properties();
		
		addSubproperties(props, "mpanel", mpanel.getProperties());
		addSubproperties(props, "spanel", spanel.getProperties());
		
		props.addProperty(new Property("leftsplit.location", ""+lsplit.getDividerLocation()));
		props.addProperty(new Property("mainsplit.location", ""+csplit.getDividerLocation()));
		
		props.addProperty(new Property("checking", ""+checkingmenu.isSelected()));

		addSubproperties(props, "agents", agents.getProperties());
		
		return props;
	}
	
	/**
	 *  Load a model.
	 *  @param model The model name.
	 */
	protected void loadModel(final String model)
	{
		getCpanel().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		getStarterPanel().loadModel(model);
		getCpanel().setCursor(Cursor.getDefaultCursor());
	}

	/**
	 *  Get the central panel.
	 *  @return cpanel The panel.
	 */
	protected Component getCpanel()
	{
		return csplit;
	}

	/**
	 *  Get the starter panel.
	 * @return the starter panel
	 */
	protected StarterPanel getStarterPanel()
	{
		return spanel;
	}
	
	/**
	 *  Get the checking menu.
	 */
	protected JCheckBoxMenuItem getCheckingMenu()
	{
		return checkingmenu;
	}
	
	/**
	 *  Get the model explorer.
	 * @return the model explorer
	 */
	protected ModelExplorer getModelExplorer()
	{
		return mpanel;
	}

	/**
	 * @see jadex.tools.common.plugin.IControlCenterPlugin#reset()
	 */
	public void reset()
	{
		try
		{
			mpanel.reset();
			spanel.reset();
			this.checkingmenu.setSelected(true);	// Default: on
		}
		catch(Exception e)
		{
		}
	}

	/**
	 *  Called when the agent is closed.
	 */
	public void	shutdown()
	{
		mpanel.close();
	}

	/**
	 *  Action for suspending an agent.
	 */
	final AbstractAction SUSPEND_AGENT = new AbstractAction("Suspend agent", icons.getIcon("suspend_agent"))
	{
		public void actionPerformed(ActionEvent e)
		{
			TreePath[] paths = agents.getTreetable().getTree().getSelectionPaths();
			for(int i=0; paths!=null && i<paths.length; i++) 
			{
				DefaultTreeTableNode node = (DefaultTreeTableNode)paths[i].getLastPathComponent();
				if(node!=null && node.getUserObject() instanceof IComponentDescription)
				{
					IComponentExecutionService ces = (IComponentExecutionService)getJCC().getServiceContainer().getService(IComponentExecutionService.class);
					ces.suspendComponent(((IComponentDescription)node.getUserObject()).getName(), new IResultListener()
					{
						public void resultAvailable(Object result)
						{
							getJCC().setStatusText("Suspended agent: " + result);
						}						
						public void exceptionOccurred(Exception exception)
						{
							getJCC().displayError("Problem Suspending Agent", "Agent could not be suspended.", exception);
						}
					});
				}
			}
		}

		public boolean isEnabled()
		{
			TreePath[] paths = agents.getTreetable().getTree().getSelectionPaths();
			boolean ret = paths!=null;
			for(int i=0; ret && paths!=null && i<paths.length; i++) 
			{
				DefaultTreeTableNode node = (DefaultTreeTableNode)paths[i].getLastPathComponent();
				if(node!=null && node.getUserObject() instanceof IComponentDescription)
				{
					ret &= IComponentDescription.STATE_ACTIVE.equals(
						((IComponentDescription)node.getUserObject()).getState());
				}
			}
			return ret;
		}
	};
	
	/**
	 *  Action for resuming an agent.
	 */
	final AbstractAction RESUME_AGENT = new AbstractAction("Resume agent", icons.getIcon("resume_agent"))
	{
		public void actionPerformed(ActionEvent e)
		{
			TreePath[] paths = agents.getTreetable().getTree().getSelectionPaths();
			for(int i=0; paths!=null && i<paths.length; i++)
			{
				DefaultTreeTableNode node = (DefaultTreeTableNode)paths[i].getLastPathComponent();
				if(node!=null && node.getUserObject() instanceof IComponentDescription)
				{
					IComponentExecutionService ces = (IComponentExecutionService)getJCC().getServiceContainer().getService(IComponentExecutionService.class);
					ces.resumeComponent(((IComponentDescription)node.getUserObject()).getName(), new IResultListener()
					{
						public void resultAvailable(Object result)
						{
							getJCC().setStatusText("Resumed agent: " + result);
						}						
						public void exceptionOccurred(Exception exception)
						{
							getJCC().displayError("Problem Resuming Agent", "Agent could not be resumed.", exception);
						}
					});
				}
			}
		}
		
		public boolean isEnabled()
		{
			TreePath[] paths = agents.getTreetable().getTree().getSelectionPaths();
			boolean ret = paths!=null;
			for(int i=0; ret && paths!=null && i<paths.length; i++) 
			{
				DefaultTreeTableNode node = (DefaultTreeTableNode)paths[i].getLastPathComponent();
				if(node!=null && node.getUserObject() instanceof IComponentDescription)
				{
					ret &= IComponentDescription.STATE_SUSPENDED.equals(
						((IComponentDescription)node.getUserObject()).getState());
				}
			}
			return ret;
		}
	};
	
	/**
	 *  Action for killing an agent.
	 */
	final AbstractAction KILL_AGENT = new AbstractAction("Kill agent", icons.getIcon("kill_agent"))
	{
		public void actionPerformed(ActionEvent e)
		{
			TreePath[] paths = agents.getTreetable().getTree().getSelectionPaths();
			for(int i=0; paths!=null && i<paths.length; i++) 
			{
				DefaultTreeTableNode node = (DefaultTreeTableNode)paths[i].getLastPathComponent();
				if(node!=null && node.getUserObject() instanceof IComponentDescription)
				{
					IComponentExecutionService ces = (IComponentExecutionService)getJCC().getServiceContainer().getService(IComponentExecutionService.class);
					ces.destroyComponent(((IComponentDescription)node.getUserObject()).getName(), new IResultListener()
					{
						public void resultAvailable(Object result)
						{
							getJCC().setStatusText("Killed agent: " + result);
						}						
						public void exceptionOccurred(Exception exception)
						{
							getJCC().displayError("Problem Killing Agent", "Agent could not be killed.", exception);
						}
					});
				}
			}
		}
	};

	/**
	 *  Action for killing an application.
	 */
	final AbstractAction KILL_APPLICATION = new AbstractAction("Kill application", icons.getIcon("kill_agent"))
	{
		public void actionPerformed(ActionEvent e)
		{
			IContextService cs = (IContextService)jcc.getServiceContainer().getService(IContextService.class);
			if(cs!=null)
			{
				TreePath[] paths = applications.getTreetable().getTree().getSelectionPaths();
				for(int i=0; paths!=null && i<paths.length; i++) 
				{
					DefaultTreeTableNode node = (DefaultTreeTableNode)paths[i].getLastPathComponent();
					if(node!=null && node.getUserObject() instanceof IContext)
					{
						final IContext context = (IContext)node.getUserObject();
						cs.deleteContext(context, new IResultListener()
						{
							public void exceptionOccurred(Exception exception)
							{
								SwingUtilities.invokeLater(new Runnable()
								{
									public void run()
									{
										String text = SUtil.wrapText("Could not kill application: "+context.getName());
										JOptionPane.showMessageDialog(SGUI.getWindowParent(spanel), text, "Kill Application Problem", JOptionPane.INFORMATION_MESSAGE);
									}
								});
							}
							public void resultAvailable(Object result)
							{
								jcc.setStatusText("Killed application: "+context.getName());
							}
						});
					}
				}
			}
		}
	};
	
	/**
	 *  Action for killing the platform.
	 */
	final AbstractAction KILL_PLATFORM = new AbstractAction("Kill platform", icons.getIcon("kill_platform"))
	{
		public void actionPerformed(ActionEvent e)
		{
//			IComponentExecutionService ces = (IComponentExecutionService)getJCC().getServiceContainer().getService(IComponentExecutionService.class);
//			ces.shutdownPlatform(new IResultListener()
//			{
//				public void resultAvailable(Object result)
//				{
//				}						
//				public void exceptionOccurred(Exception exception)
//				{
//					getJCC().displayError("Platform Shutdown Problem", "Could not kill platform.", exception);
//				}
//			});
			
			getJCC().getServiceContainer().shutdown(new IResultListener()
			{
				public void resultAvailable(Object result)
				{
				}						
				public void exceptionOccurred(Exception exception)
				{
					getJCC().displayError("Platform Shutdown Problem", "Could not kill platform.", exception);
				}
			});
		}
	};

	/**
	 *  Called when an agent has died.
	 *  @param ad The agent description.
	 */
	public void agentDied(final IComponentDescription ad)
	{
		// Update components on awt thread.
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				agents.removeAgent(ad);
			}
		});
	}

	/**
	 *  Called when an agent is born.
	 *  @param ad the agent description.
	 */
	public void agentBorn(final IComponentDescription ad)
	{
		// Update components on awt thread.
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				agents.addAgent(ad);
			}
		});
	}
	
	/**
	 *  Called when an agent changed.
	 *  @param ad the agent description.
	 */
	public void agentChanged(final IComponentDescription ad)
	{
		// Update components on awt thread.
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				agents.updateAgent(ad);
			}
		});
	}

	/** 
	 * @return the help id of the perspective
	 * @see jadex.tools.jcc.AbstractJCCPlugin#getHelpID()
	 */
	public String getHelpID()
	{
		return "tools.starter";
	}

	//-------- starter specific tree actions --------

	/**
	 *  Dynamically create a new menu item structure for starting agents.
	 */
	class StartAgentMenuItemConstructor implements IMenuItemConstructor
	{
		/**
		 *  Get or create a new menu item (struture).
		 *  @return The menu item (structure).
		 */
		public JMenuItem getMenuItem()
		{
			JMenuItem ret = null;

			if(isEnabled())
			{
				IExplorerTreeNode node = (IExplorerTreeNode)mpanel.getLastSelectedPathComponent();
				if(node instanceof FileNode)
				{
					final String type = ((FileNode)node).getFile().getAbsolutePath();
					if(SComponentFactory.isStartable(getJCC().getServiceContainer(), type))//&& ((FileNode)node).isValid())
					{
						try
						{
//							IAgentFactory agentfactory = getJCC().getAgent().getPlatform().getAgentFactory();
							ILoadableComponentModel model = SComponentFactory.loadModel(getJCC().getServiceContainer(), type);
							String[] inistates = model.getConfigurations();
//							IMBDIAgent model = SXML.loadAgentModel(type, null);
//							final IMConfiguration[] inistates = model.getConfigurationbase().getConfigurations();
							
							if(inistates.length>1)
							{
								JMenu re = new JMenu("Start Agent");
								re.setIcon(icons.getIcon("start_agent"));
								for(int i=0; i<inistates.length; i++)
								{
									final String config = inistates[i];
									JMenuItem me = new JMenuItem(config);
									re.add(me);
									me.addActionListener(new ActionListener()
									{
										public void actionPerformed(ActionEvent e)
										{
											createComponent(type, null, config, null);
										}
									});
									me.setToolTipText("Start in configuration: "+config);

								}
								ret = re;
								ret.setToolTipText("Start agent in selectable configuration");
							}
							else
							{
								if(inistates.length==1)
								{
									ret = new JMenuItem("Start Agent ("+inistates[0]+")");
									ret.setToolTipText("Start agent in configuration:"+inistates[0]);
								}
								else
								{
									ret = new JMenuItem("Start Agent");
									ret.setToolTipText("Start agent without explicit initial state");
								}
								ret.setIcon(icons.getIcon("start_agent"));
								ret.addActionListener(new ActionListener()
								{
									public void actionPerformed(ActionEvent e)
									{
										createComponent(type, null, null, null);
									}
								});
							}
						}
						catch(Exception e)
						{
							// NOP
						}
					}
				}
			}

			return ret;
		}

		/**
		 *  Test if action is available in current context.
		 *  @return True, if available.
		 */
		public boolean isEnabled()
		{
			boolean ret = false;
			IExplorerTreeNode node = (IExplorerTreeNode)mpanel.getLastSelectedPathComponent();
			if(node instanceof FileNode)
			{
				String type = ((FileNode)node).getFile().getAbsolutePath();
				// todo: fix me
				if(type.endsWith(".agent.xml"))
//				if(SXML.isAgentFilename(type))
					ret = true;
			}
			return ret;
		}
	}
	
	/**
	 *  The action for changing integrity checking settings.
	 */
	public final AbstractAction TOGGLE_CHECKING = new AbstractAction("Auto check", icons.getIcon("checking_menu"))
	{
		public void actionPerformed(ActionEvent e)
		{
			((ModelExplorerTreeModel)getModelExplorer().getModel())
				.fireTreeStructureChanged(getModelExplorer().getRootNode());
		}
	};
	
	//-------- constants --------

	/** The Jadex agent extension. * /
	public static final String FILE_EXTENSION_AGENT = ".agent.xml";

	/** The Jadex capability extension. * /
	public static final String FILE_EXTENSION_CAPABILITY = ".capability.xml";
	
	/**
	 *  Test if a file is a Jadex file.
	 *  @param filename The filename.
	 *  @return True, if it is a Jadex file.
	 * /
	public static boolean isJadexFilename(String filename)
	{
		return filename!=null && (filename.toLowerCase().endsWith(FILE_EXTENSION_AGENT)
			|| filename.toLowerCase().endsWith(FILE_EXTENSION_CAPABILITY)
			|| filename.toLowerCase().endsWith("agent.class"));
	}

	/**
	 *  Test if a file is an agent file.
	 *  @param filename The filename.
	 *  @return True, if it is an agent file.
	 * /
	public static boolean isAgentFilename(String filename)
	{
		return filename!=null && filename.toLowerCase().endsWith(FILE_EXTENSION_AGENT);
	}

	/**
	 *  Test if a file is a capability file.
	 *  @param filename The filename.
	 *  @return True, if it is a capability file.
	 * /
	public static boolean isCapabilityFilename(String filename)
	{
		return filename!=null && filename.toLowerCase().endsWith(FILE_EXTENSION_CAPABILITY);
	}*/


	/**
	 *  Create a new agent on the platform.
	 *  Any errors will be displayed in a dialog to the user.
	 */
	public void createComponent(String type, String name, String configname, Map arguments)
	{
		SComponentExecutionService.createComponent(getJCC().getServiceContainer(), name, type, configname, arguments, new IResultListener()
		{
			public void resultAvailable(Object result)
			{
				final IComponentIdentifier aid = (IComponentIdentifier)result;
				SComponentExecutionService.startComponent(getJCC().getServiceContainer(), aid, new IResultListener()
				{
					public void resultAvailable(Object result)
					{
						getJCC().setStatusText("Started agent: " + aid.getLocalName());
					}
					
					public void exceptionOccurred(Exception exception)
					{
						getJCC().displayError("Problem Starting Agent", "Agent could not be started.", exception);
					}
				});
			}
			
			public void exceptionOccurred(Exception exception)
			{
				getJCC().displayError("Problem Starting Agent", "Agent could not be started.", exception);
			}
		}, null);
		
			
	}
}