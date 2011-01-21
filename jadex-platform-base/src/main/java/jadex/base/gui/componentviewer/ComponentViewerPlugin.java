package jadex.base.gui.componentviewer;

import jadex.base.gui.componenttree.ComponentTreePanel;
import jadex.base.gui.componenttree.IActiveComponentTreeNode;
import jadex.base.gui.componenttree.IComponentTreeNode;
import jadex.base.gui.componenttree.INodeHandler;
import jadex.base.gui.componenttree.INodeListener;
import jadex.base.gui.componenttree.ServiceNode;
import jadex.base.gui.plugin.AbstractJCCPlugin;
import jadex.bridge.IComponentIdentifier;
import jadex.bridge.IComponentManagementService;
import jadex.bridge.IComponentStep;
import jadex.bridge.IExternalAccess;
import jadex.bridge.IInternalAccess;
import jadex.commons.Properties;
import jadex.commons.SGUI;
import jadex.commons.SReflect;
import jadex.commons.concurrent.SwingDefaultResultListener;
import jadex.commons.gui.CombiIcon;
import jadex.commons.gui.ObjectCardLayout;
import jadex.commons.service.IService;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.UIDefaults;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;


/**
 *  The service viewer allows to introspect details of services.
 */
public class ComponentViewerPlugin extends AbstractJCCPlugin
{
	// -------- constants --------

	/**
	 * The image icons.
	 */
	protected static final UIDefaults	icons	= new UIDefaults(new Object[]{
		"componentviewer", SGUI.makeIcon(ComponentViewerPlugin.class, "/jadex/base/gui/images/configure.png"), 
		"componentviewer_sel", SGUI.makeIcon(ComponentViewerPlugin.class, "/jadex/tools/common/images/configure_sel.png"), 
		"open_viewer", SGUI.makeIcon(ComponentViewerPlugin.class, "/jadex/base/gui/images/new_introspector.png"),
		"close_viewer", SGUI.makeIcon(ComponentViewerPlugin.class, "/jadex/base/gui/images/close_introspector.png"),
		"viewer_empty", SGUI.makeIcon(ComponentViewerPlugin.class, "/jadex/base/gui/images/viewer_empty.png"),
		"overlay_viewable", SGUI.makeIcon(ComponentViewerPlugin.class, "/jadex/base/gui/images/overlay_edit.png"),
		"overlay_viewed", SGUI.makeIcon(ComponentViewerPlugin.class, "/jadex/base/gui/images/overlay_introspected.png"),
		"overlay_notviewed", SGUI.makeIcon(ComponentViewerPlugin.class, "/jadex/base/gui/images/overlay_notintrospected.png")
	});
	
	//-------- attributes --------

	/** The split panel. */
	protected JSplitPane	split;

	/** The agent tree table. */
	protected ComponentTreePanel	comptree;

	/** The detail panel. */
	protected JPanel	detail;

	/** The detail layout. */
	protected ObjectCardLayout	cards;
	
	/** The service viewer panels. */
	protected Map	panels;
	
	/** Loaded properties. */
	protected Properties	props;
	
	/** The active component node viewable state. */
	protected Map viewables;
	
	//-------- constructors --------
	
	/**
	 *  Create a new plugin.
	 */
	public ComponentViewerPlugin()
	{
		this.panels	= new HashMap();
		this.viewables = Collections.synchronizedMap(new HashMap());
	}
	
	//-------- IControlCenterPlugin interface --------
	
	/**
	 *  @return The plugin name 
	 *  @see jadex.tools.common.plugin.IControlCenterPlugin#getName()
	 */
	public String getName()
	{
		return "Component Viewer";
	}

	/**
	 *  @return The icon of plugin
	 *  @see jadex.tools.common.plugin.IControlCenterPlugin#getToolIcon()
	 */
	public Icon getToolIcon(boolean selected)
	{
		return selected? icons.getIcon("componentviewer_sel"): icons.getIcon("componentviewer");
	}

	/**
	 *  Create tool bar.
	 *  @return The tool bar.
	 */
	public JComponent[] createToolBar()
	{
		JButton b1 = new JButton(START_VIEWER);
		b1.setBorder(null);
		b1.setToolTipText(b1.getText());
		b1.setText(null);
		b1.setEnabled(true);

		JButton b2 = new JButton(STOP_VIEWER);
		b2.setBorder(null);
		b2.setToolTipText(b2.getText());
		b2.setText(null);
		b2.setEnabled(true);
		
		return new JComponent[]{b1, b2};
	}
		
	/**
	 *  Create main panel.
	 *  @return The main panel.
	 */
	public JComponent createView()
	{
		this.split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);
		split.setOneTouchExpandable(true);

		comptree = new ComponentTreePanel(getJCC().getExternalAccess());
		comptree.setMinimumSize(new Dimension(0, 0));
		split.add(comptree);

		comptree.getTree().getSelectionModel().addTreeSelectionListener(new TreeSelectionListener()
		{
			public void valueChanged(TreeSelectionEvent e)
			{
				JTree tree = comptree.getTree();
				if(tree.getSelectionPath()!=null)
				{
					IComponentTreeNode node = (IComponentTreeNode)tree.getSelectionPath().getLastPathComponent();
					Object nodeid = node.getId();
					if(nodeid!=null)
					{
						if(cards.getComponent(nodeid)!=null)
						{
							storeCurrentPanelSettings();
							IAbstractViewerPanel panel = (IAbstractViewerPanel)panels.get(nodeid);
							panel.setProperties(props!=null ? props.getSubproperty(panel.getId()) : null);
							cards.show(nodeid);
						}
					}
				}
			}
		});
		
		comptree.addNodeHandler(new INodeHandler()
		{
			public Action[] getPopupActions(IComponentTreeNode[] nodes)
			{
				Action[]	ret	= null;
				
				boolean	allviewable	= true;
				for(int i=0; allviewable && i<nodes.length; i++)
				{
					allviewable	= isNodeViewable(nodes[i]);
				}
				
				if(allviewable)
				{
					boolean	allob	= true;
					for(int i=0; allob && i<nodes.length; i++)
					{
						allob	= cards.getComponent(nodes[i].getId())!=null;
					}
					boolean	allig	= true;
					for(int i=0; allig && i<nodes.length; i++)
					{
						allig	= cards.getComponent(nodes[i].getId())==null;
					}
					
					// Todo: Large icons for popup actions?
					if(allig)
					{
						Icon	base	= nodes[0].getIcon();
						Action	a	= new AbstractAction((String)START_VIEWER.getValue(Action.NAME),
							base!=null ? new CombiIcon(new Icon[]{base, icons.getIcon("overlay_viewed")}) : (Icon)START_VIEWER.getValue(Action.SMALL_ICON))
						{
							public void actionPerformed(ActionEvent e)
							{
								START_VIEWER.actionPerformed(e);
							}
						};
						ret	= new Action[]{a};
					}
					else if(allob)
					{
						Icon	base	= nodes[0].getIcon();
						Action	a	= new AbstractAction((String)STOP_VIEWER.getValue(Action.NAME),
							base!=null ? new CombiIcon(new Icon[]{base, icons.getIcon("overlay_notviewed")}) : (Icon)STOP_VIEWER.getValue(Action.SMALL_ICON))
						{
							public void actionPerformed(ActionEvent e)
							{
								STOP_VIEWER.actionPerformed(e);
							}
						};
						ret	= new Action[]{a};
					}
				}
				
				return ret;
			}
			
			public Icon getOverlay(IComponentTreeNode node)
			{
				Icon ret	= null;
				if(cards.getComponent(node.getId())!=null)
				{
					ret = icons.getIcon("overlay_viewed");
				}
				else if(isNodeViewable(node))
				{
					ret = icons.getIcon("overlay_viewable");					
				}
				return ret;
			}
			
			public Action getDefaultAction(IComponentTreeNode node)
			{
				Action	a	= null;
				if(cards.getComponent(node.getId())!=null)
				{
					a	= STOP_VIEWER;
				}
				else if(isNodeViewable(node))
				{
					a	= START_VIEWER;
				}
				return a;
			}
		});

		JLabel	emptylabel	= new JLabel("Select vieweable components or services to activate the viewer",
			icons.getIcon("viewer_empty"), JLabel.CENTER);
		emptylabel.setVerticalAlignment(JLabel.CENTER);
		emptylabel.setHorizontalTextPosition(JLabel.CENTER);
		emptylabel.setVerticalTextPosition(JLabel.BOTTOM);
		emptylabel.setFont(emptylabel.getFont().deriveFont(emptylabel.getFont().getSize()*1.3f));

		cards = new ObjectCardLayout();
		detail = new JPanel(cards);
		detail.setMinimumSize(new Dimension(0, 0));
		detail.add(ObjectCardLayout.DEFAULT_COMPONENT, emptylabel);
		split.add(detail);
		//split.setResizeWeight(1.0);
		
		// todo:
//		SHelp.setupHelp(split, getHelpID());

		split.setDividerLocation(150);
		
		// Listener to remove panels, when services vanish!
		comptree.getModel().addNodeListener(new INodeListener()
		{
			public void nodeRemoved(IComponentTreeNode node)
			{
//				System.out.println("node rem: "+node);
				Object nodeid = node.getId();
				if(panels.containsKey(nodeid))
				{
					storeCurrentPanelSettings();
//					System.out.println("removeing: "+nodeid+" "+cards.getComponent(nodeid));
					detail.remove(cards.getComponent(nodeid));
					IAbstractViewerPanel panel = (IAbstractViewerPanel)panels.remove(nodeid);
					panel.shutdown();
					comptree.getModel().fireNodeChanged(node);
				}
			}
			
			public void nodeAdded(IComponentTreeNode node)
			{
			}
		});

		return split;
	}
		
	final AbstractAction START_VIEWER = new AbstractAction("Open service viewer", icons.getIcon("open_viewer"))
	{
		public void actionPerformed(ActionEvent e)
		{
			TreePath[]	paths	= comptree.getTree().getSelectionPaths();
			for(int i=0; paths!=null && i<paths.length; i++)
			{
				if(isNodeViewable((IComponentTreeNode)paths[i].getLastPathComponent()))
				{
					final Object tmp = paths[i].getLastPathComponent();
					
					if(tmp instanceof ServiceNode)
					{
						final ServiceNode node = (ServiceNode)tmp;
						final IService service = node.getService();

						AbstractJCCPlugin.getClassLoader(((IActiveComponentTreeNode)node.getParent().getParent()).getComponentIdentifier(), getJCC())
							.addResultListener(new SwingDefaultResultListener(comptree)
						{
							public void customResultAvailable(Object result)
							{
								ClassLoader	cl	= (ClassLoader)result;
								
								final Object clid = service.getPropertyMap()!=null? service.getPropertyMap().get(IAbstractViewerPanel.PROPERTY_VIEWERCLASS) : null;
								final Class clazz = clid instanceof Class? (Class)clid: clid instanceof String? SReflect.classForName0((String)clid, cl): null;
								
								if(clid!=null)
								{
									try
									{
										storeCurrentPanelSettings();
										final IServiceViewerPanel	panel = (IServiceViewerPanel)clazz.newInstance();
										panel.init(getJCC(), service).addResultListener(new SwingDefaultResultListener(comptree)
										{
											public void customResultAvailable(Object result)
											{
												Properties	sub	= props!=null ? props.getSubproperty(panel.getId()) : null;
												panel.setProperties(sub);
												JComponent comp = panel.getComponent();
												
												// todo: help 
												//SHelp.setupHelp(comp, getHelpID());
												panels.put(service.getServiceIdentifier(), panel);
												detail.add(comp, service.getServiceIdentifier());
												comptree.getModel().fireNodeChanged(node);
											}
										});
									}
									catch(Exception e)
									{
										e.printStackTrace();
										getJCC().displayError("Error initializing service viewer panel.", "Component viewer panel class: "+clid, e);
									}
								}
							}
						});
					}
					else if(tmp instanceof IActiveComponentTreeNode)
					{
						final IActiveComponentTreeNode node = (IActiveComponentTreeNode)tmp;
						final IComponentIdentifier cid = node.getComponentIdentifier();
						
						getJCC().getExternalAccess().scheduleStep(new IComponentStep()
						{
							public static final String XML_CLASSNAME = "init";
							public Object execute(IInternalAccess ia)
							{
								ia.getRequiredService("cms").addResultListener(new SwingDefaultResultListener(comptree)
								{
									public void customResultAvailable(Object result)
									{
										final IComponentManagementService cms = (IComponentManagementService)result;
										cms.getExternalAccess(cid).addResultListener(new SwingDefaultResultListener(comptree)
										{
											public void customResultAvailable(Object result)
											{
												final IExternalAccess exta = (IExternalAccess)result;
												final Object clid = exta.getModel().getProperties().get(IAbstractViewerPanel.PROPERTY_VIEWERCLASS);
											
												if(clid instanceof String)
												{
													AbstractJCCPlugin.getClassLoader(cid, getJCC()).addResultListener(new SwingDefaultResultListener(comptree)
													{
														public void customResultAvailable(Object result)
														{
															ClassLoader	cl	= (ClassLoader)result;
															Class clazz	= SReflect.classForName0((String)clid, cl);
															createPanel(clazz, exta, node);
														}
													});
												}
												else if(clid instanceof Class)
												{
													createPanel((Class)clid, exta, node);
												}
											}
										});
									}
								});
								return null;
							}
						});
					}
				}
			}
		}
	};
	
	/**
	 * 
	 */
	protected void createPanel(Class clazz, final IExternalAccess exta, final IActiveComponentTreeNode node)
	{
		try
		{
			storeCurrentPanelSettings();
			final IComponentViewerPanel panel = (IComponentViewerPanel)clazz.newInstance();
			panel.init(getJCC(), exta).addResultListener(new SwingDefaultResultListener(comptree)
			{
				public void customResultAvailable(Object result)
				{
					Properties	sub	= props!=null ? props.getSubproperty(panel.getId()) : null;
					panel.setProperties(sub);
					JComponent comp = panel.getComponent();
					// todo: help
					//SHelp.setupHelp(comp, getHelpID());
					panels.put(exta.getComponentIdentifier(), panel);
					detail.add(comp, exta.getComponentIdentifier());
					comptree.getModel().fireNodeChanged(node);
				}
			});
		}
		catch(Exception e)
		{
			e.printStackTrace();
			getJCC().displayError("Error initializing component viewer panel.", "Component viewer panel class: "+clazz, e);
		}
	}

	final AbstractAction STOP_VIEWER = new AbstractAction("Close service viewer", icons.getIcon("close_viewer"))
	{
		public void actionPerformed(ActionEvent e)
		{
			TreePath[]	paths	= comptree.getTree().getSelectionPaths();
			for(int i=0; paths!=null && i<paths.length; i++)
			{
				if(isNodeViewable((IComponentTreeNode)paths[i].getLastPathComponent()))
				{
					storeCurrentPanelSettings();
					final IComponentTreeNode node = (IComponentTreeNode)paths[i].getLastPathComponent();
					Object nodeid = node.getId();
					detail.remove(cards.getComponent(nodeid));
					IAbstractViewerPanel panel = (IAbstractViewerPanel)panels.remove(nodeid);
					panel.shutdown().addResultListener(new SwingDefaultResultListener(comptree)
					{
						public void customResultAvailable(Object result)
						{
							comptree.getModel().fireNodeChanged(node);
						}
					});
				}
			}
		}
	};

	/**
	 * @return the help id of the perspective
	 * @see jadex.tools.common.plugin.AbstractJCCPlugin#getHelpID()
	 */
	public String getHelpID()
	{
		return "tools.componentviewer";
	}
	
	/**
	 *  Test if a node is viewable.
	 *  @param node	The node.
	 *  @return True, if the node is viewable.
	 */
	protected boolean isNodeViewable(final IComponentTreeNode node)
	{
		boolean ret = false;
		if(node instanceof ServiceNode)
		{
			Map	props	= ((ServiceNode)node).getService().getPropertyMap();
			ret = props!=null && props.get(IAbstractViewerPanel.PROPERTY_VIEWERCLASS)!=null;
		}
		else if(node instanceof IActiveComponentTreeNode)
		{
			final IComponentIdentifier cid = ((IActiveComponentTreeNode)node).getComponentIdentifier();
			
			// For proxy components the cid could be null if the remote cid has not yet been retrieved
			// Using a IFuture as return value in not very helpful because this method can't directly
			// return a result, even if known.
			// todo: how to initiate a repaint in case the the cid is null
			if(cid!=null)
			{
				Boolean viewable = (Boolean)viewables.get(cid);
				if(viewable!=null)
				{
					ret = viewable.booleanValue();
				}
				else
				{
					// Unknown -> start search to find out asynchronously
					jcc.getExternalAccess().scheduleStep(new IComponentStep()
					{
						public static final String XML_CLASSNAME = "is-node-viewable"; 
						public Object execute(IInternalAccess ia)
						{
							ia.getRequiredService("cms").addResultListener(new SwingDefaultResultListener(comptree)
							{
								public void customResultAvailable(Object result)
								{
									final IComponentManagementService cms = (IComponentManagementService)result;
									
									cms.getExternalAccess(cid).addResultListener(new SwingDefaultResultListener(comptree)
									{
										public void customResultAvailable(Object result)
										{
											final IExternalAccess exta = (IExternalAccess)result;
											final Object clid = exta.getModel().getProperties().get(IAbstractViewerPanel.PROPERTY_VIEWERCLASS);
											viewables.put(cid, clid==null? Boolean.FALSE: Boolean.TRUE);
			//								System.out.println("node: "+viewables.get(cid));
											node.refresh(false, false);
										}
										
										public void customExceptionOccurred(Exception exception)
										{
											// Happens e.g. when remote classes not locally available.
//											exception.printStackTrace();
										}
									});
								}
							});
							return null;
						}
					});
				}
			}
		}
		return ret;
	}
	
	//-------- loading / saving --------
	
	/**
	 *  Return properties to be saved in project.
	 */
	public Properties getProperties()
	{
		storeCurrentPanelSettings();
		
		return props;
	}
	
	/**
	 *  Set properties loaded from project.
	 */
	public void setProperties(Properties ps)
	{
		this.props	=	ps;
		for(Iterator it=panels.values().iterator(); it.hasNext(); )
		{
			IAbstractViewerPanel	panel	= (IAbstractViewerPanel)it.next();
			Properties	sub	= props!=null ? props.getSubproperty(panel.getId()) : null;
			panel.setProperties(sub);
		}
	}

	
	/**
	 *  Store settings of current panel.
	 */
	protected void storeCurrentPanelSettings()
	{
		Object	old	= cards.getCurrentKey();
		if(old!=null)
		{
			IAbstractViewerPanel	panel	= (IAbstractViewerPanel)panels.get(old);
			if(panel!=null)
			{
				if(props==null)
					props	= new Properties();
				Properties	sub	= panel.getProperties();
				props.removeSubproperties(panel.getId());
				if(sub!=null)
				{
					sub.setType(panel.getId());
					props.addSubproperties(sub);
				}
			}
		}
	}
}
