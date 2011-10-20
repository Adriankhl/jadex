package jadex.tools.deployer;

import jadex.base.gui.SwingDefaultResultListener;
import jadex.base.gui.SwingDelegationResultListener;
import jadex.base.gui.asynctree.INodeHandler;
import jadex.base.gui.asynctree.ITreeNode;
import jadex.base.gui.filetree.FileNode;
import jadex.base.gui.filetree.JarAsDirectory;
import jadex.base.gui.filetree.RemoteFileNode;
import jadex.base.gui.plugin.IControlCenter;
import jadex.base.service.deployment.DeploymentService;
import jadex.bridge.service.types.deployment.FileContent;
import jadex.bridge.service.types.deployment.IDeploymentService;
import jadex.commons.IPropertiesProvider;
import jadex.commons.Properties;
import jadex.commons.Property;
import jadex.commons.future.CounterResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileInputStream;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.tree.TreePath;

/**
 *  Panel for showing a file transfer view composed of two
 *  panels with a file tree.
 */
public class DeployerPanel extends JPanel implements IPropertiesProvider
{
	//-------- attributes --------
	
	/** The control center. */
	protected IControlCenter jcc;
	
	/** The split panel. */
	protected JSplitPane splitpanel;
	
	/** The first panel. */
	protected DeployerServiceSelectorPanel p1;

	/** The second panel. */
	protected DeployerServiceSelectorPanel p2;

	//-------- constructors --------
	
	/**
	 *  Create a new deloyer panel.
	 */
	public DeployerPanel(final IControlCenter jcc)
	{
		this.jcc = jcc;
		
		splitpanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		setLayout(new BorderLayout());
		add(splitpanel, BorderLayout.CENTER);

		// Local view on the left
		DeployerNodeHandler nh1 = new DeployerNodeHandler();
		DeployerNodeHandler nh2 = new DeployerNodeHandler();
		p1 = new DeployerServiceSelectorPanel(jcc.getJCCAccess(), jcc.getJCCAccess(), nh1, "Local Platform");
		p2 = new DeployerServiceSelectorPanel(jcc.getJCCAccess(), jcc.getPlatformAccess(), nh2, "Remote Platform");
		nh1.setFirstPanel(p1);
		nh1.setSecondPanel(p2);
		nh2.setFirstPanel(p2);
		nh2.setSecondPanel(p1);
		
		splitpanel.add(p1);
		splitpanel.add(p2);
		
		splitpanel.setOneTouchExpandable(true);
	
		p1.refreshCombo();
		p2.refreshCombo();
	}
	
	/**
	 *  Get the properties.
	 */
	public IFuture<Properties> getProperties()
	{
		final Future<Properties> ret = new Future<Properties>();

		final Properties props = new Properties();
		props.addProperty(new Property("split_location", ""+splitpanel.getDividerLocation()));
		
		// Only save properties of local panels.
		DeploymentServiceViewerPanel	dsvp1	= (DeploymentServiceViewerPanel)p1.getCurrentPanel();
		DeploymentServiceViewerPanel	dsvp2	= (DeploymentServiceViewerPanel)p2.getCurrentPanel();
		IFuture	p1props	= dsvp1!=null && !dsvp1.getFileTreePanel().isRemote()
			? p1.getProperties() : IFuture.DONE;		
		final IFuture	p2props	= dsvp2!=null && !dsvp2.getFileTreePanel().isRemote()
			? p2.getProperties() : IFuture.DONE;
		
		p1props.addResultListener(new SwingDelegationResultListener(ret)
		{
			public void customResultAvailable(Object result)
			{
				if(result!=null)
					props.addSubproperties("first", (Properties)result);
				p2props.addResultListener(new SwingDelegationResultListener(ret)
				{
					public void customResultAvailable(Object result)
					{
						if(result!=null)
							props.addSubproperties("second", (Properties)result);
						ret.setResult(props);
					}
				});
			}
		});
		
		return ret;
	}
	
	/**
	 *  Set the properties.
	 */
	public IFuture<Void> setProperties(Properties props)
	{
		Properties firstprops = props.getSubproperty("first");
		if(firstprops!=null)
			p1.setProperties(firstprops);
		Properties secondprops = props.getSubproperty("second");
		if(secondprops!=null)
			p2.setProperties(secondprops);

		splitpanel.setDividerLocation(props.getIntProperty("split_location"));
	
		return IFuture.DONE;
	}
	
	//-------- helper classes --------
	
	/**
	 *  The deployer node handler that combines
	 *  both file trees via commands.
	 */
	class DeployerNodeHandler implements INodeHandler
	{
		//-------- attributes --------
		
		/** The first panel. */
		protected DeployerServiceSelectorPanel first;
		
		/** The second panel. */
		protected DeployerServiceSelectorPanel second;

		AbstractAction copy = new AbstractAction("Copy file")
		{
			public void actionPerformed(ActionEvent e)
			{
				if(first!=null && second!=null)
				{
					final String sel_1 = first.getSelectedPath();
					final String sel_2 = second.getSelectedPath();
					final IDeploymentService ds = second.getDeploymentService();
					
					if(sel_1!=null && sel_2!=null)
					{
						final File source = new File(sel_1);
						
						final int fragmentsize = DeploymentService.FRAGMENT_SIZE;
						final int len = (int)source.length();
						int num = (int)(len/fragmentsize);
						final int last = (int)(len%fragmentsize);
						final int fragments = num + (last>0? 1: 0);
						
						try
						{
							final FileInputStream fis = new FileInputStream(source);
							
							final CounterResultListener lis = new CounterResultListener(fragments, new SwingDefaultResultListener()
							{
								public void customResultAvailable(Object result)
								{
									second.refreshTreePaths(null);
									jcc.setStatusText("Copied: "+sel_1+" to: "+sel_2);
								}
								
								public void customExceptionOccurred(Exception exception)
								{
									jcc.setStatusText("Copy error: "+sel_1+" to: "+sel_2+" exception: "+exception);
								}
							})
							{
								public void intermediateResultAvailable(Object result)
								{
									if(getCnt()<fragments)
									{
										final CounterResultListener clis = this;
										int percent = (int)(((double)getCnt())/fragments*100);
										jcc.setStatusText("Copy "+percent+"% done");
//										System.out.println("Copy "+percent+"% done");
										FileContent frag = FileContent.createFragment(fis, source.getName(), getCnt()==fragments-1? last: fragmentsize, len);
										
										// Note: uses a swing listener to avoid stack overflow when running on one thread
										ds.putFile(frag, sel_2, (String)result).addResultListener(new SwingDefaultResultListener()
										{
											public void customResultAvailable(Object result)
											{
												clis.resultAvailable(result);
											}
											public void customExceptionOccurred(Exception exception)
											{
												clis.exceptionOccurred(exception);
											}
										});
									}
								}
							};
							
					
							
							FileContent frag = FileContent.createFragment(fis, source.getName(), 0==fragments-1? last: fragmentsize, len);
							ds.putFile(frag, sel_2, null).addResultListener(lis);
						}
						catch(Exception ex)
						{
							jcc.setStatusText("Copy error: "+sel_1);
						}
						
//						ds.putFile(new FileContent(source), sel_2)
//							.addResultListener(new SwingDefaultResultListener()
//						{
//							public void customResultAvailable(Object result)
//							{
//								second.refreshTreePaths(null);
//								jcc.setStatusText("Copied: "+sel_1+" to: "+sel_2);
//							}
//							
//							public void customExceptionOccurred(Exception exception)
//							{
//								jcc.setStatusText("Copy error: "+sel_1+" to: "+sel_2+" exception: "+exception);
//							}
//						});
					}
				}
			}
			
			public boolean isEnabled()
			{
				return first!=null && second!=null && first.getSelectedPath()!=null && second.getSelectedPath()!=null;
			}
		};
		
		AbstractAction del = new AbstractAction("Delete file")
		{
			public void actionPerformed(ActionEvent e)
			{
				if(first!=null)
				{
					final TreePath tp = first.getSelectedTreePath();
					final String sel = first.getSelectedPath();
					if(sel!=null)
					{
						IDeploymentService ds = first.getDeploymentService();
						ds.deleteFile(sel).addResultListener(new SwingDefaultResultListener()
						{
							public void customResultAvailable(Object result)
							{
								first.refreshTreePaths(new TreePath[]{tp.getParentPath()});
								jcc.setStatusText("Deleted: "+sel);
							}
							public void customExceptionOccurred(Exception exception) 
							{
								jcc.setStatusText("Could not delete: "+sel);
							}
						});
					}
				}
			}
			
			public boolean isEnabled()
			{
				boolean ret = false;
				if(first!=null && first.getSelectedTreePath()!=null)
				{
					Object node = first.getSelectedTreePath().getLastPathComponent();
					if(node instanceof FileNode)
					{
						File file = ((FileNode)node).getFile();
						ret = !file.isDirectory() || (file instanceof JarAsDirectory && ((JarAsDirectory)file).isRoot());
					}
					else if(node instanceof RemoteFileNode)
					{
						ret = !((RemoteFileNode)node).getRemoteFile().isDirectory();
					}
				}
				return ret;
			}
		};
		
		AbstractAction rename = new AbstractAction("Rename file")
		{
			public void actionPerformed(ActionEvent e)
			{
				if(first!=null)
				{
					final TreePath tp = first.getSelectedTreePath();
					final String sel = first.getSelectedPath();
					
					if(sel!=null)
					{
						String name = JOptionPane.showInputDialog("New name: ");
						if(name==null || name.length()==0)
						{
							jcc.setStatusText("Cannot rename to empty name.");
						}
						else
						{
							IDeploymentService ds = first.getDeploymentService();
							ds.renameFile(sel, name).addResultListener(new SwingDefaultResultListener()
							{
								public void customResultAvailable(Object result)
								{
									first.refreshTreePaths(new TreePath[]{tp.getParentPath()});
									jcc.setStatusText("Renamed: "+sel);
								}
								public void customExceptionOccurred(Exception exception) 
								{
									jcc.setStatusText("Could not rename: "+sel+" reason: "+exception.getMessage());
								}
							});
						}
					}
				}
			}
			
			public boolean isEnabled()
			{
				return first!=null && first.getSelectedTreePath()!=null;
			}
		};
		
		AbstractAction open = new AbstractAction("Open file")
		{
			public void actionPerformed(ActionEvent e)
			{
				if(first!=null)
				{
					final TreePath tp = first.getSelectedTreePath();
					final String sel = first.getSelectedPath();
					
					if(sel!=null)
					{
						IDeploymentService ds = first.getDeploymentService();
						ds.openFile(sel).addResultListener(new SwingDefaultResultListener()
						{
							public void customResultAvailable(Object result)
							{
								first.refreshTreePaths(new TreePath[]{tp.getParentPath()});
								jcc.setStatusText("Opened: "+sel);
							}
							public void customExceptionOccurred(Exception exception) 
							{
								jcc.setStatusText("Could not open: "+sel+" reason: "+exception.getMessage());
							}
						});
					}
				}
			}
			
			public boolean isEnabled()
			{
				return first!=null && first.getSelectedTreePath()!=null;
			}
		};
	
		//-------- methods --------

		/**
		 *  Get the overlay for a node if any.
		 */
		public Icon	getOverlay(ITreeNode node)
		{
			return null;
		}

		/**
		 *  Get the popup actions available for all of the given nodes, if any.
		 */
		public Action[]	getPopupActions(ITreeNode[] nodes)
		{
			return new Action[]{copy, del, rename, open};
		}

		/**
		 *  Get the default action to be performed after a double click.
		 */
		public Action getDefaultAction(ITreeNode node)
		{
			return null;
		}
		
		/**
		 *  Set the first panel.
		 */
		public void setFirstPanel(DeployerServiceSelectorPanel first)
		{
			 this.first = first;
		}
		
		/**
		 *  Set the first panel.
		 */
		public void setSecondPanel(DeployerServiceSelectorPanel second)
		{
			 this.second = second;
		}
	}
}
