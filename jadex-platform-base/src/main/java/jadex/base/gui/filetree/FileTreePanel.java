package jadex.base.gui.filetree;

import jadex.base.gui.SwingDelegationResultListener;
import jadex.base.gui.asynctree.AsyncTreeCellRenderer;
import jadex.base.gui.asynctree.AsyncTreeModel;
import jadex.base.gui.asynctree.INodeHandler;
import jadex.base.gui.asynctree.ITreeNode;
import jadex.base.gui.asynctree.TreePopupListener;
import jadex.bridge.IComponentStep;
import jadex.bridge.IExternalAccess;
import jadex.bridge.IInternalAccess;
import jadex.commons.IPropertiesProvider;
import jadex.commons.IRemoteFilter;
import jadex.commons.Properties;
import jadex.commons.Property;
import jadex.commons.SUtil;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.commons.gui.IMenuItemConstructor;
import jadex.commons.gui.PopupBuilder;
import jadex.commons.gui.SGUI;
import jadex.commons.gui.TreeExpansionHandler;
import jadex.xml.annotation.XMLClassname;
import jadex.xml.bean.JavaReader;
import jadex.xml.bean.JavaWriter;

import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.ToolTipManager;
import javax.swing.tree.TreePath;

/**
 *  A panel displaying components on the platform as tree.
 */
public class FileTreePanel extends JPanel implements IPropertiesProvider
{
	//-------- attributes --------
	
	/** The remote flag. */
	protected final boolean remote;
	
	/** The keep roots flag (if added top-level nodes should be used instead of nodes from loaded properties). */
	protected final boolean keeproots;
	
	/** The external access. */
	protected final IExternalAccess	exta;
	
	/** The component tree model. */
	protected final AsyncTreeModel	model;
	
	/** The component tree. */
	protected final JTree tree;
		
	/** Tree expansion handler remembers open tree nodes. */
	protected ExpansionHandler expansionhandler;
		
	/** The iconcache. */
	protected DelegationIconCache iconcache;
	
	
	/** The node factory. */
	protected INodeFactory factory;
	
	/** The filter. */
	protected DelegationFilter filefilter;

	/** Popup rightclick. */
	protected PopupBuilder pubuilder;
	
	/** The filter popup. */
	protected IMenuItemConstructor mic;
	
	//-------- constructors --------
	
	/**
	 *  Create a new component tree panel.
	 */
	public FileTreePanel(IExternalAccess exta)
	{
		this(exta, false, false);
	}
	
	/**
	 *  Create a new component tree panel.
	 */
	public FileTreePanel(IExternalAccess exta, boolean remote, boolean keeproots)
	{
		this.setLayout(new BorderLayout());
		
		this.exta	= exta;
		this.remote = remote;
		this.keeproots	= keeproots;
		this.model	= new AsyncTreeModel();
		this.tree	= new JTree(model);
		this.expansionhandler = new ExpansionHandler(tree);
		this.filefilter = new DelegationFilter();
		this.iconcache = new DelegationIconCache();
		this.factory = new DefaultNodeFactory();
		
		tree.setCellRenderer(new AsyncTreeCellRenderer());
		tree.addMouseListener(new TreePopupListener());
		tree.setShowsRootHandles(true);
		tree.setToggleClickCount(0);
		tree.setRootVisible(false);
		tree.setRowHeight(16);
		ToolTipManager.sharedInstance().registerComponent(tree);
		
		this.add(new JScrollPane(tree), BorderLayout.CENTER);
		
		new TreeExpansionHandler(tree);
		RootNode root = new RootNode(model, tree);
		model.setRoot(root);
		tree.expandPath(new TreePath(root));
		
		tree.addMouseListener(new MouseAdapter()
		{
			public void mousePressed(MouseEvent e)
			{
				if(e.isPopupTrigger())
					showPopUp(e.getX(), e.getY());
			}

			public void mouseReleased(MouseEvent e)
			{
				if(e.isPopupTrigger())
					showPopUp(e.getX(), e.getY());
			}
		});
		
		addKeyListener(new KeyListener()
		{
			public void keyTyped(KeyEvent e)
			{
			}
			
			public void keyReleased(KeyEvent e)
			{
				if(KeyEvent.VK_F5==e.getKeyCode())
					((ITreeNode)getModel().getRoot()).refresh(true);
			}
			
			public void keyPressed(KeyEvent e)
			{
			}
		});
	}
	
	//-------- methods --------
	
	/**
	 *  Add a node handler.
	 */
	public void	addNodeHandler(INodeHandler handler)
	{
		model.addNodeHandler(handler);
	}

	/**
	 *  Get the tree model.
	 */
	public AsyncTreeModel getModel()
	{
		return model;
	}
	
	/**
	 *  Get the tree.
	 */
	public JTree getTree()
	{
		return tree;
	}
	
	/**
	 *  Get the external access.
	 *  @return the external access.
	 */
	public IExternalAccess getExternalAccess()
	{
		return exta;
	}
	
	/**
	 *  Get the remote.
	 *  @return the remote.
	 */
	public boolean isRemote()
	{
		return remote;
	}

	/**
	 *  Set the file filter.
	 *  @param filefilter The file filter.
	 */
	public void setFileFilter(IRemoteFilter filefilter)
	{
		this.filefilter.setFilter(filefilter);
	}
	
	/**
	 *  Set the menu item constructor.
	 *  @param mic The menu item constructor.
	 */
	public void setMenuItemConstructor(IMenuItemConstructor mic)
	{
		this.mic = mic;
	}
	
	/**
	 *  Get the menu item constructor.
	 *  @return The menu item constructor.
	 */
	public IMenuItemConstructor getMenuItemConstructor()
	{
		return mic;
	}
	
	/**
	 *  Set the popup builder.
	 *  @param pubuilder The popup builder.
	 */
	public void setPopupBuilder(PopupBuilder pubuilder)
	{
		this.pubuilder = pubuilder;
	}

	/**
	 *  Get the popup builder.
	 *  @return The popup builder.
	 */
	public PopupBuilder getPopupBuilder()
	{
		return pubuilder;
	}

	/**
	 *  Set the iconcache.
	 *  @param iconcache The iconcache to set.
	 */
	public void setIconCache(IIconCache iconcache)
	{
		this.iconcache.setIconCache(iconcache);
	}
	
	/**
	 *  Get the node factory.
	 *  @return The node factory.
	 */
	public INodeFactory getFactory()
	{
		return factory;
	}

	/**
	 *  Set the factory.
	 *  @param factory The factory to set.
	 */
	public void setNodeFactory(INodeFactory factory)
	{
		this.factory = factory;
	}

	/**
	 *  Dispose the tree.
	 *  Should be called to remove listeners etc.
	 */
	public void	dispose()
	{
		getModel().dispose();
	}
	
	/**
	 * Show the popup.
	 * @param x The x position.
	 * @param y The y position.
	 */
	protected void showPopUp(int x, int y)
	{
		TreePath sel = tree.getPathForLocation(x, y);
		if(sel==null)
		{
			tree.clearSelection();
//			System.out.println("show");
			if(pubuilder!=null)
			{
				JPopupMenu pop = pubuilder.buildPopupMenu();
				pop.show(tree, x, y);
			}
		}
	}
	
	/**
	 *  Add a top level node.
	 */
	public void	removeTopLevelNode(ITreeNode node)
	{
		RootNode root = (RootNode)getModel().getRoot();
		root.removeChild(node);
		for(int i=0; i<root.getChildCount(); i++)
			model.fireNodeChanged((ITreeNode) root.getCachedChildren().get(i));
	}
	
	/**
	 *  Add a top level node.
	 */
	public void	addTopLevelNode(File file)
	{
		assert !remote;
		
		RootNode root = (RootNode)getModel().getRoot();
		ITreeNode node = factory.createNode(root, model, tree, file, 
			iconcache, filefilter, exta, factory);
		addNode(node);
	}
	
	/**
	 *  Add a top level node.
	 */
	public void addTopLevelNode(FileData file)
	{
		assert remote;
		
		final RootNode root = (RootNode)getModel().getRoot();
		ITreeNode node = factory.createNode(root, model, tree, file, 
			iconcache, filefilter, exta, factory);
		addNode(node);
	}
	
	/**
	 *  Add a root node to the tree panel. 
	 */
	protected void	addNode(ITreeNode node)
	{
		final RootNode root = (RootNode)getModel().getRoot();
		root.addChild(node);
		
		node.refresh(true);
		
		for(int i=0; i<root.getChildCount(); i++)
			model.fireNodeChanged((ITreeNode) root.getCachedChildren().get(i));
		tree.scrollPathToVisible(new TreePath(new Object[]{root, node}));
	}
	
	/**
	 *  Write current state into properties.
	 */
	public IFuture getProperties()
	{
		final Future ret = new Future();
		final Properties props = new Properties();
		
		// Save tree properties.
		final TreeProperties	mep	= new TreeProperties();
		final RootNode root = (RootNode)getTree().getModel().getRoot();
		
		final Future	rootdone	= new Future();
		if(!keeproots)
		{
			// Convert path to relative must be done on target platform.
			final String[] paths	= root.getPathEntries();
			exta.scheduleStep(new IComponentStep()
			{
				@XMLClassname("convertPathToRelative")
				public Object execute(IInternalAccess ia)
				{
					for(int i=0; i<paths.length; i++)
						paths[i]	= SUtil.convertPathToRelative(paths[i]);
					return paths;
				}
			}).addResultListener(new SwingDelegationResultListener(rootdone)
			{
				public void customResultAvailable(Object result)
				{
					String[]	paths	= (String[])result;
					mep.setRootPathEntries(paths);
					rootdone.setResult(null);
				}
			});
		}
		else
		{
			rootdone.setResult(null);
		}
		
		rootdone.addResultListener(new SwingDelegationResultListener(ret)
		{
			public void customResultAvailable(Object result) throws Exception
			{
				mep.setSelectedNode(getTree().getSelectionPath()==null ? null
					: NodePath.createNodePath((ITreeNode)getTree().getSelectionPath().getLastPathComponent()));
				List	expanded	= new ArrayList();
				Enumeration exp = getTree().getExpandedDescendants(new TreePath(root));
				if(exp!=null)
				{
					while(exp.hasMoreElements())
					{
						TreePath	path	= (TreePath)exp.nextElement();
						if(path.getLastPathComponent() instanceof IFileNode)
						{
							expanded.add(NodePath.createNodePath((ITreeNode)path.getLastPathComponent()));
						}
					}
				}
				mep.setExpandedNodes((NodePath[])expanded.toArray(new NodePath[expanded.size()]));
				
				// Hack!!! cannot use (local) platform class loader, because has only access to (remote?) target platform.
				String	treesave	= JavaWriter.objectToXML(mep, getClass().getClassLoader());	// Doesn't support inner classes: ModelExplorer$ModelExplorerProperties
				props.addProperty(new Property("tree", treesave));
						
				// Save the state of file filters
				if(mic instanceof IPropertiesProvider)
				{
					((IPropertiesProvider)mic).getProperties()
						.addResultListener(new SwingDelegationResultListener(ret)
					{
						public void customResultAvailable(Object result)
						{
							Properties	filterprops	= (Properties)result;
							props.addSubproperties("mic", filterprops);
							ret.setResult(props);
						}
					});
				}
				else
				{
					ret.setResult(props);
				}
			}
		});
		
		return ret;
	}
	
	/**
	 *  Update tool from given properties.
	 */
	public IFuture setProperties(final Properties props)
	{
		final Future ret = new Future();
		
		// Load root node.
		String	treexml	= props.getStringProperty("tree");
		if(treexml==null)
		{
			ret.setResult(null);
		}
		else
		{
			try
			{
				// Hack!!! cannot use (local) platform class loader, because has only access to (remote?) target platform.
				ClassLoader cl = getClass().getClassLoader();
				final TreeProperties	mep	= (TreeProperties)JavaReader.objectFromXML(treexml, cl); 	// Doesn't support inner classes: ModelExplorer$ModelExplorerProperties
				final RootNode root = (RootNode)getTree().getModel().getRoot();
				
				final Future	rootdone	= new Future();
				if(!keeproots)
				{
					final String[]	entries	= mep.getRootPathEntries();
					if(entries!=null)
					{
						if(!remote)
						{
							for(int i=0; i<entries.length; i++)
							{
								ITreeNode node = factory.createNode(root, model, tree, new File(entries[i]), iconcache, filefilter, exta, factory);
								addNode(node);
							}
							rootdone.setResult(null);
						}
						else
						{
							exta.scheduleStep(new IComponentStep()
							{
								@XMLClassname("createRootEntries")
								public Object execute(IInternalAccess ia)
								{
									FileData[]	ret	= new FileData[entries.length];
									for(int i=0; i<entries.length; i++)
									{
										ret[i]	= new FileData(new File(entries[i]));
									}
									return ret;
								}
							}).addResultListener(new SwingDelegationResultListener(rootdone)
							{
								public void customResultAvailable(Object result) throws Exception
								{
									FileData[]	entries	= (FileData[])result;
									for(int i=0; i<entries.length; i++)
									{
										ITreeNode node = factory.createNode(root, model, tree, entries[i], iconcache, filefilter, exta, factory);
										addNode(node);
									}
									rootdone.setResult(null);
								}
							});
						}
					}
					else
					{
						root.removeAll();
						rootdone.setResult(null);
					}
				}
				else
				{
					rootdone.setResult(null);
				}

				rootdone.addResultListener(new SwingDelegationResultListener(ret)
				{
					public void customResultAvailable(Object result) throws Exception
					{
						// Select the last selected model in the tree.
						if(mep.getSelectedNode()!=null)
							expansionhandler.setSelectedPath(mep.getSelectedNode());

						// Load the expanded tree nodes.
						if(mep.getExpandedNodes()!=null)
							expansionhandler.setExpandedPaths(mep.getExpandedNodes());

						root.refresh(true);
						
						// Load the filter settings
						Properties	filterprops	= props.getSubproperty("mic");
						if(mic instanceof IPropertiesProvider)
						{
							((IPropertiesProvider)mic).setProperties(filterprops)
								.addResultListener(new SwingDelegationResultListener(ret)
							{
								public void customResultAvailable(Object result) 
								{
									ret.setResult(null);
								};
							});
						}
						else
						{
							ret.setResult(null);
						}
					}
				});				
			}
			catch(Exception e)
			{
				ret.setException(e);
				System.err.println("Cannot load project tree: "+e.getClass().getName());
			}
		}
		
		return ret;
	}
	
	/**
	 *  Get selected file paths.
	 */
	public String[] getSelectionPaths()
	{
		String[] ret = null;
		TreePath[] paths = tree.getSelectionPaths();
		if(paths!=null)
		{
			ret = new String[paths.length];
			if(remote)
			{
				for(int i=0; i<paths.length; i++)
				{
					FileData file = ((RemoteFileNode)paths[i].getLastPathComponent()).getRemoteFile();
					ret[i] = file.getPath();
				}
			}
			else
			{
				for(int i=0; i<paths.length; i++)
				{
					File file = ((FileNode)paths[i].getLastPathComponent()).getFile();
					ret[i] = file.getPath();
				}
			}
		}
		else
		{
			ret = new String[0];
		}
		return ret;
	}
	
	
	
	/**
	 *  Main for testing.
	 */
	public static void main(String[] args)
	{
		JFrame f =  new JFrame();
		FileTreePanel ftp = new FileTreePanel(null);
		DefaultFileFilterMenuItemConstructor mic = new DefaultFileFilterMenuItemConstructor(new String[]{".doc"}, ftp.getModel());
		ftp.setPopupBuilder(new PopupBuilder(new Object[]{mic}));
		DefaultFileFilter ff = new DefaultFileFilter(mic);
		ftp.setFileFilter(ff);
		ftp.addNodeHandler(new DefaultNodeHandler(ftp.getTree()));
		ftp.addTopLevelNode(new File("c:/"));
		f.add(new JScrollPane(ftp), BorderLayout.CENTER);
		f.pack();
		f.setLocation(SGUI.calculateMiddlePosition(f));
		f.setVisible(true);
	}
	
	/**
	 *  Delegation filter class.
	 */
	public static class DelegationFilter implements IRemoteFilter
	{
		//-------- attributes --------
		
		/** The delegation filter. */
		protected IRemoteFilter filter;

		//-------- methods --------

		/**
		 *  Test if an object passes the filter.
		 *  @return True, if passes the filter.
		 */
		public IFuture filter(Object obj)
		{
			return filter!=null? filter.filter(obj): new Future(Boolean.TRUE);
		}
		
		/**
		 *  Get the filter.
		 *  @return the filter.
		 */
		public IRemoteFilter getFilter()
		{
			return filter;
		}

		/**
		 *  Set the filter.
		 *  @param filter The filter to set.
		 */
		public void setFilter(IRemoteFilter filter)
		{
			this.filter = filter;
		}
	}
	
	/**
	 *  The delegation icon cache.
	 */
	public static class DelegationIconCache implements IIconCache
	{
		//-------- attributes --------
		
		/** The delegation icon cache. */
		protected IIconCache iconcache;
		
		//-------- methods --------

		/**
		 *  Create a new delegation cache. 
		 */
		public DelegationIconCache()
		{
			this.iconcache = new DefaultIconCache();
		}
		
		/**
		 *  Get an icon.
		 */
		public Icon getIcon(ITreeNode node)
		{
			return iconcache!=null? iconcache.getIcon(node): null;
		}

		/**
		 *  Get the iconcache.
		 *  @return the iconcache.
		 */
		public IIconCache getIconCache()
		{
			return iconcache;
		}

		/**
		 *  Set the iconcache.
		 *  @param iconcache The iconcache to set.
		 */
		public void setIconCache(IIconCache iconcache)
		{
			this.iconcache = iconcache;
		}
	}
}
