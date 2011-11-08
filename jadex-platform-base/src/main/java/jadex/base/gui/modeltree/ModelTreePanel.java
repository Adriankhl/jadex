package jadex.base.gui.modeltree;

import jadex.base.gui.SwingDefaultResultListener;
import jadex.base.gui.asynctree.AsyncTreeCellRenderer;
import jadex.base.gui.asynctree.ITreeNode;
import jadex.base.gui.filetree.DefaultNodeFactory;
import jadex.base.gui.filetree.DefaultNodeHandler;
import jadex.base.gui.filetree.FileTreePanel;
import jadex.base.gui.filetree.IFileNode;
import jadex.base.gui.filetree.RootNode;
import jadex.base.service.library.LibraryService;
import jadex.bridge.IComponentStep;
import jadex.bridge.IExternalAccess;
import jadex.bridge.IInternalAccess;
import jadex.bridge.IMultiKernelListener;
import jadex.bridge.IResourceIdentifier;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.bridge.service.search.SServiceProvider;
import jadex.bridge.service.types.factory.IMultiKernelNotifierService;
import jadex.bridge.service.types.library.ILibraryService;
import jadex.bridge.service.types.library.ILibraryServiceListener;
import jadex.commons.IRemoteFilter;
import jadex.commons.SUtil;
import jadex.commons.future.DefaultResultListener;
import jadex.commons.future.DelegationResultListener;
import jadex.commons.future.ExceptionDelegationResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.commons.future.IResultListener;
import jadex.commons.gui.PopupBuilder;
import jadex.commons.gui.SGUI;
import jadex.xml.annotation.XMLClassname;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.SwingUtilities;
import javax.swing.UIDefaults;

/**
 *  Tree for component models.
 */
public class ModelTreePanel extends FileTreePanel
{
	//-------- constants --------

	/** The image icons. */
	protected static final UIDefaults icons = new UIDefaults(new Object[]
	{
		"gid", SGUI.makeIcon(ModelTreePanel.class, "/jadex/base/gui/images/overlay_refresh.png")
	});
	
	protected static int LISTENER_COUNTER = 0;
	
	//-------- attributes --------
	
	/** The actions. */
	protected Map actions;
	
	/** Kernel listener */
	protected IMultiKernelListener kernellistener;
	
	/** The library service listener */
	protected ILibraryServiceListener libservicelistener;
	
	/** The local external access. */
	protected IExternalAccess localexta;
	
	/** The root entries. */
	protected Map<URL, IResourceIdentifier>	rootentries;
	
//	/** The jcc. */
//	protected IControlCenter jcc;
	
	//-------- constructors --------
	
//	public ModelTreePanel(IControlCenter jcc)
//	{
//		this(jcc.getPlatformAccess(), jcc.getJCCAccess(), 
//			!SUtil.equals(jcc.getPlatformAccess().getComponentIdentifier().getPlatformName(), 
//			jcc.getJCCAccess().getComponentIdentifier().getPlatformName()));
//		this.jcc = jcc;
//	}

	
	/**
	 *  Create a new model tree panel.
	 */
	public ModelTreePanel(final IExternalAccess exta, IExternalAccess localexta, boolean remote)
	{
		super(exta, remote, false);
		this.localexta = localexta;
		actions = new HashMap();
		this.rootentries	= new LinkedHashMap<URL, IResourceIdentifier>();
		
		final ModelFileFilterMenuItemConstructor mic = new ModelFileFilterMenuItemConstructor(getModel(), exta);
		setNodeFactory(new DefaultNodeFactory()
		{
			public IRemoteFilter getFileFilter()
			{
				return new ModelFileFilter(mic, getRootEntries(), exta);
			}
		});
		ModelIconCache ic = new ModelIconCache(exta, getTree());
		setMenuItemConstructor(mic);
		actions.put(CollapseAllAction.getName(), new CollapseAllAction(this));
		actions.put(AddPathAction.getName(), remote ? new AddRemotePathAction(this) : new AddPathAction(this));
		actions.put(RemovePathAction.getName(), new RemovePathAction(this));
		setPopupBuilder(new PopupBuilder(new Object[]{actions.get(AddPathAction.getName()), 
			actions.get(AddRemotePathAction.getName()), mic}));
		setMenuItemConstructor(mic);
		setIconCache(ic);
		DefaultNodeHandler dnh = new DefaultNodeHandler(getTree())
		{
			public Icon getOverlay(ITreeNode node)
			{
				Icon	overlay	= null;
				if(getModel().getRoot().equals(node.getParent()) && node instanceof IFileNode)
				{
					URL	url	= SUtil.toURL(((IFileNode)node).getFilePath());
					IResourceIdentifier	rid	= rootentries.get(url);
					if(rid!=null && rid.getGlobalIdentifier()!=null)
					{
						overlay	= ModelTreePanel.this.icons.getIcon("gid");
					}
				}
				return overlay;
			}
		};
		dnh.addAction(new RemovePathAction(this), null);
		addNodeHandler(dnh);
		
		tree.setCellRenderer(new AsyncTreeCellRenderer()
		{
			protected String getLabel(ITreeNode node)
			{
				String	ret	= null;
				if(getModel().getRoot().equals(node.getParent()) && node instanceof IFileNode)
				{
					URL	url	= SUtil.toURL(((IFileNode)node).getFilePath());
					IResourceIdentifier	rid	= rootentries.get(url);
					ret	= rid!=null && rid.getGlobalIdentifier()!=null
						? rid.getGlobalIdentifier().toString() : null;
					if(ret!=null && ret.indexOf(':')!=-1)
						ret	= ret.substring(ret.indexOf(':')+1);
				}
				
				return ret!=null ? ret : node.toString();
			}
		});
		
		final String lid = exta.getServiceProvider().getId().toString() + localexta.getServiceProvider().getId().toString() + "_" + LISTENER_COUNTER++;
		SServiceProvider.getService(exta.getServiceProvider(), IMultiKernelNotifierService.class, RequiredServiceInfo.SCOPE_PLATFORM)
			.addResultListener(new IResultListener()
		{
			public void resultAvailable(Object result)
			{
				kernellistener = new TreePanelKernelListener(lid, getTree(), ((ModelFileFilterMenuItemConstructor)getMenuItemConstructor()));
				((IMultiKernelNotifierService)result).addKernelListener(kernellistener);
			}
			
			public void exceptionOccurred(Exception exception)
			{
				// Ignore, no multi-kernel
			}
		});
		
		SServiceProvider.getService(exta.getServiceProvider(), ILibraryService.class, RequiredServiceInfo.SCOPE_PLATFORM)
			.addResultListener(new IResultListener()
		{
			public void resultAvailable(Object result)
			{
				ILibraryService ls = (ILibraryService)result;
				libservicelistener = new ILibraryServiceListener()
				{
					public IFuture resourceIdentifierRemoved(final IResourceIdentifier rid)
					{
						SwingUtilities.invokeLater(new Runnable()
						{
							public void run()
							{
								URL url = rid.getLocalIdentifier().getUrl();
								try
								{
									// Comparison of file/urls is hard.
									// Fetches filenames of all root entries in model tree
									// and sends them with filename to remove to component
									// that is local to those filenames and can create and
									// compare them.
									
									final String toremove = url.toURI().toString();
									
									final RootNode root = (RootNode)getModel().getRoot();
									final List children = root.getCachedChildren();
									final List filenames = new ArrayList();
									for(int i=0; i<children.size(); i++)
									{
										IFileNode child = (IFileNode)children.get(i);
										String filename = child.getFilePath();
										filenames.add(filename);
									}
									
									exta.scheduleStep(new IComponentStep<Integer>()
									{
										@XMLClassname("findchild")
										public IFuture<Integer> execute(IInternalAccess ia)
										{
											int ret = LibraryService.indexOfFilename(toremove, filenames);
											return new Future<Integer>(new Integer(ret));
										}
									}).addResultListener(new DefaultResultListener<Integer>()
									{
										public void resultAvailable(Integer result)
										{
											final int res = result.intValue();
											if(res!=-1)
											{
												SwingUtilities.invokeLater(new Runnable()
												{
													public void run()
													{
														removeTopLevelNode((ITreeNode)children.get(res));
													}
												});
											}
										}
									});
								}
								catch(Exception e)
								{
									e.printStackTrace();
								}
							}
						});
						return IFuture.DONE;
					}
					
					public IFuture resourceIdentifierAdded(IResourceIdentifier rid)
					{
						return IFuture.DONE;
					}
				};
				ls.addLibraryServiceListener(libservicelistener);
			}
			
			public void exceptionOccurred(Exception exception)
			{
				// Ignore, no library service
			}
		});
	}
	
	//-------- methods --------
	
	/**
	 *  Add a root node to the tree panel. 
	 */
	protected void	addNode(final ITreeNode node)
	{
		if(node instanceof IFileNode && node.getParent().equals(getTree().getModel().getRoot()))
		{
			// Hack!!! add protocol if not present to allow comparison with library service URLs.
			final String filepath = ((IFileNode)node).getFilePath();
			final String filename = filepath.startsWith("file:") || filepath.startsWith("jar:file:")
				? filepath : "file:"+filepath;
			final URL	url	= SUtil.toURL(filepath);
			exta.scheduleStep(new IComponentStep<IResourceIdentifier>()
			{
				@XMLClassname("addurl")
				public IFuture<IResourceIdentifier> execute(IInternalAccess ia)
				{
					final Future<IResourceIdentifier>	ret	= new Future<IResourceIdentifier>();
					IFuture<ILibraryService>	libfut	= SServiceProvider.getService(ia.getServiceContainer(), ILibraryService.class, RequiredServiceInfo.SCOPE_PLATFORM);
					libfut.addResultListener(new ExceptionDelegationResultListener<ILibraryService, IResourceIdentifier>(ret)
					{
						public void customResultAvailable(final ILibraryService ls)
						{
							ls.addURL(url).addResultListener(new DelegationResultListener<IResourceIdentifier>(ret));
						}
					});
					
					return ret;
				}
			}).addResultListener(new SwingDefaultResultListener<IResourceIdentifier>()
			{
				public void customResultAvailable(IResourceIdentifier rid) 
				{
					// Todo: remove entries on remove.
					rootentries.put(url, rid);
					
					ModelTreePanel.super.addNode(node);
				}
				public void customExceptionOccurred(final Exception exception)
				{
					localexta.scheduleStep(new IComponentStep<Void>()
					{
						public IFuture<Void> execute(IInternalAccess ia)
						{
							ia.getLogger().warning(exception.toString());
							return IFuture.DONE;
						}
					});					
				}
			});
		}
		else
		{
			super.addNode(node);
		}
	}
	
	/**
	 *  Get the action.
	 *  @param name The action name.
	 *  @return The action.
	 */
	public Action getAction(String name)
	{
		return (Action)actions.get(name);
	}
	
	/**
	 *  Dispose the panel.
	 */
	public void dispose()
	{
		if(kernellistener!=null)
		{
			SServiceProvider.getService(exta.getServiceProvider(), IMultiKernelNotifierService.class, RequiredServiceInfo.SCOPE_PLATFORM).addResultListener(new IResultListener()
			{
				public void resultAvailable(Object result)
				{
					((IMultiKernelNotifierService)result).removeKernelListener(kernellistener);
				}
				
				public void exceptionOccurred(Exception exception)
				{
					// Ignore, no multi-kernel
				}
			});
		}
		if(libservicelistener!=null)
		{
			SServiceProvider.getService(exta.getServiceProvider(), ILibraryService.class, RequiredServiceInfo.SCOPE_PLATFORM)
				.addResultListener(new IResultListener()
			{
				public void resultAvailable(Object result)
				{
					ILibraryService ls = (ILibraryService)result;
					ls.removeLibraryServiceListener(libservicelistener);
				}
				
				public void exceptionOccurred(Exception exception)
				{
					// Ignore, no library service
				}
			});
		}
		super.dispose();
	}
	
	/**
	 *  Get the root entries of the tree.
	 */
	public Map<URL, IResourceIdentifier>	getRootEntries()
	{
		return rootentries;
	}
}
