package jadex.base.gui.componenttree;

import jadex.base.gui.asynctree.AbstractSwingTreeNode;
import jadex.base.gui.asynctree.AsyncSwingTreeModel;
import jadex.base.gui.asynctree.ISwingTreeNode;
import jadex.bridge.IComponentIdentifier;
import jadex.bridge.IExternalAccess;
import jadex.bridge.nonfunctional.INFPropertyMetaInfo;
import jadex.bridge.service.IService;
import jadex.bridge.service.IServiceIdentifier;
import jadex.bridge.service.ProvidedServiceInfo;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.bridge.service.search.SServiceProvider;
import jadex.bridge.service.types.library.ILibraryService;
import jadex.commons.MethodInfo;
import jadex.commons.SReflect;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.commons.future.IResultListener;
import jadex.commons.gui.SGUI;
import jadex.commons.gui.future.SwingDefaultResultListener;
import jadex.commons.gui.future.SwingResultListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.UIDefaults;

/**
 *  Node object representing a service.
 */
public class ProvidedServiceInfoNode	extends AbstractSwingTreeNode
{
	//-------- constants --------
	
	/** The service container icon. */
	private static final UIDefaults icons = new UIDefaults(new Object[]
	{
		"service", SGUI.makeIcon(ProvidedServiceInfoNode.class, "/jadex/base/gui/images/provided_16.png")
	});
	
	//-------- attributes --------
	
	/** The service. */
	private final ProvidedServiceInfo	service;
	
	/** The service id. */
	protected IServiceIdentifier sid;

	/** The properties component (if any). */
	protected ProvidedServiceInfoProperties	propcomp;
	
	/** The external access. */
	protected IExternalAccess ea;
	
	//-------- constructors --------
	
	/**
	 *  Create a new service container node.
	 */
	public ProvidedServiceInfoNode(ISwingTreeNode parent, AsyncSwingTreeModel model, JTree tree, 
		ProvidedServiceInfo service, IServiceIdentifier sid, IExternalAccess ea)
	{
		super(parent, model, tree);
		this.service	= service;
		this.sid = sid;
		this.ea = ea;
//		if(service==null || service.getType().getTypeName()==null)
//			System.out.println("service node: "+this);
		model.registerNode(this);
	}
	
	//-------- methods --------
	
	/**
	 *  Get the service.
	 */
	public ProvidedServiceInfo	getServiceInfo()
	{
		return service;
	}
	
	/**
	 *  Get the sid.
	 *  @return the sid.
	 */
	public IServiceIdentifier getServiceIdentifier()
	{
		return sid;
	}

	/**
	 *  Get the id used for lookup.
	 */
	public Object	getId()
	{
//		return sid;
		return getId(getParent(), service);
	}
	
	/**
	 *  Get the icon as byte[] for a node.
	 */
	public byte[] getIcon()
	{
		return null;
	}

	/**
	 *  Get the icon for a node.
	 */
	public Icon	getSwingIcon()
	{
		return icons.getIcon("service");
	}

	/**
	 *  Asynchronously search for children.
	 *  Called once for each node.
	 *  Should call setChildren() once children are found.
	 */
	protected void	searchChildren()
	{
		IFuture<IService> fut = SServiceProvider.getService(ea.getServiceProvider(), sid);
		fut.addResultListener(new IResultListener<IService>()
		{
			public void resultAvailable(final IService ser)
			{
				ser.getNFPropertyMetaInfos()
					.addResultListener(new SwingResultListener<Map<String,INFPropertyMetaInfo>>(new IResultListener<Map<String,INFPropertyMetaInfo>>()
//					.addResultListener(new SwingResultListener<Map<String,INFPropertyMetaInfo>>(new IResultListener<Map<String,INFPropertyMetaInfo>>()
				{
					public void resultAvailable(Map<String,INFPropertyMetaInfo> result)
					{
						NFPropertyContainerNode cn = null;
						if(result!=null && result.size()>0)
						{
							String name = "Service properties";
							cn = (NFPropertyContainerNode)model.getNode(NFPropertyContainerNode.getId(getId(), name));
							if(cn==null)
								cn = new NFPropertyContainerNode(null, name, ProvidedServiceInfoNode.this, (AsyncSwingTreeModel)model, tree, ea, sid, null);
						}
						
						final NFPropertyContainerNode sercon = cn;
						
						ser.getMethodNFPropertyMetaInfos()
							.addResultListener(new SwingResultListener<Map<MethodInfo,Map<String,INFPropertyMetaInfo>>>(new IResultListener<Map<MethodInfo,Map<String,INFPropertyMetaInfo>>>()
						{
							public void resultAvailable(Map<MethodInfo,Map<String,INFPropertyMetaInfo>> result)
							{
								List<NFPropertyContainerNode> childs = new ArrayList<NFPropertyContainerNode>();
								if(result!=null && result.size()>0)
								{
									Set<String> doublenames = new HashSet<String>();
									Set<String> tmp = new HashSet<String>();
									for(MethodInfo mi: result.keySet())
									{
										if(tmp.contains(mi.getName()))
										{
											doublenames.add(mi.getName());
										}
										else
										{
											tmp.add(mi.getName());
										}
									}
									
									for(MethodInfo mi: result.keySet())
									{
										String name = doublenames.contains(mi.getName())? mi.getNameWithParameters(): mi.getName();
										NFPropertyContainerNode cn = (NFPropertyContainerNode)model.getNode(NFPropertyContainerNode.getId(getId(), name));
										if(cn==null)
											cn = new NFPropertyContainerNode(name, mi.toString(), ProvidedServiceInfoNode.this, (AsyncSwingTreeModel)model, tree, ea, sid, mi);
										
		//								Map<String,INFPropertyMetaInfo> props = result.get(mi);
		//								List<NFPropertyNode> subchilds = new ArrayList<NFPropertyNode>();
		//								for(INFPropertyMetaInfo p: props.values())
		//								{
		//									NFPropertyNode nfpn	= new NFPropertyNode(cn, getModel(), getTree(), p, ea, sid, mi);
		//									subchilds.add(nfpn);
		//								}
		//								
		//								Collections.sort(subchilds, new java.util.Comparator<ISwingTreeNode>()
		//								{
		//									public int compare(ISwingTreeNode t1, ISwingTreeNode t2)
		//									{
		//										String si1 = ((NFPropertyNode)t1).getMetaInfo().getName();
		//										String si2 = ((NFPropertyNode)t2).getMetaInfo().getName();
		//										return si1.compareTo(si2);
		//									}
		//								});
		//								
		//								cn.setChildren(subchilds);
										childs.add(cn);
									}
								}
								
								Collections.sort(childs, new java.util.Comparator<ISwingTreeNode>()
								{
									public int compare(ISwingTreeNode t1, ISwingTreeNode t2)
									{
										String si1 = t1.toString();
										String si2 = t2.toString();
										return si1.compareTo(si2);
									}
								});
								
								if(sercon!=null)
									childs.add(0, sercon);
								
								setChildren(childs);
							}
							
							public void exceptionOccurred(Exception exception)
							{
								exception.printStackTrace();
							}
						}));
					}
					
					public void exceptionOccurred(Exception exception)
					{
						System.out.println("ex on: "+getId());
//						exception.printStackTrace();
					}
				}));
			}
			
			public void exceptionOccurred(Exception exception)
			{
				System.out.println("ex on: "+getId());
//				exception.printStackTrace();
			}
		});
	}
	
	/**
	 * 
	 */
	protected IFuture<Class<?>> getServiceType()
	{
		final Future<Class<?>> ret = new Future<Class<?>>();
		
		if(service.getType().getType(null)==null)
		{
			SServiceProvider.getService(ea.getServiceProvider(), ILibraryService.class, RequiredServiceInfo.SCOPE_PLATFORM)
				.addResultListener(new SwingDefaultResultListener<ILibraryService>()
			{
				public void customResultAvailable(ILibraryService ls)
				{
					ls.getClassLoader(sid.getResourceIdentifier())
						.addResultListener(new SwingDefaultResultListener<ClassLoader>()
					{
						public void customResultAvailable(ClassLoader cl)
						{
							Class type = service.getType().getType(cl);
	//						System.out.println("Found: "+service.getType().getTypeName()+" "+cl+" "+type);
							ret.setResult(type);
						}
					});
				}
			});
		}
		else
		{
			ret.setResult(service.getType().getType(null));
		}
		
		return ret;
	}
	
	/**
	 *  A string representation.
	 */
	public String toString()
	{
		return SReflect.getUnqualifiedTypeName(service.getType().getTypeName());
	}
	
	/**
	 *  Get tooltip text.
	 */
	public String getTooltipText()
	{
		StringBuffer buf = new StringBuffer();
		buf.append(service.getName());
		buf.append(" :").append(service.getType().getTypeName()); 
		return buf.toString();
	}

	/**
	 *  True, if the node has properties that can be displayed.
	 */
	public boolean	hasProperties()
	{
		return true;
	}

	/**
	 *  Get or create a component displaying the node properties.
	 *  Only to be called if hasProperties() is true;
	 */
	public JComponent	getPropertiesComponent()
	{
		if(propcomp==null)
		{
			propcomp	= new ProvidedServiceInfoProperties();
		}
		propcomp.setService(service, sid, ea);
		
		return propcomp;
	}
	
	//-------- helper methods --------
	
	/**
	 *  Build the node id.
	 */
	protected static String	getId(ISwingTreeNode parent, ProvidedServiceInfo service)
	{
		IComponentIdentifier	provider	= (IComponentIdentifier)parent.getParent().getId();
		return ""+provider+":service:"+service.getName();
	}
}
