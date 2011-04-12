package jadex.base.gui.filetree;

import jadex.base.gui.asynctree.AsyncTreeModel;
import jadex.base.gui.asynctree.ITreeNode;
import jadex.bridge.IComponentStep;
import jadex.bridge.IExternalAccess;
import jadex.bridge.IInternalAccess;
import jadex.commons.IRemoteFilter;
import jadex.commons.collection.MultiCollection;
import jadex.commons.future.CollectionResultListener;
import jadex.commons.future.DelegationResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.commons.future.IResultListener;
import jadex.xml.annotation.XMLClassname;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.ZipEntry;

import javax.swing.JTree;

/**
 *  Node for remote jar file.
 */
public class RemoteJarNode extends RemoteDirNode
{
	//-------- constructors --------
	
	/**
	 *  Create a new jar node.
	 */
	public RemoteJarNode(ITreeNode parent, AsyncTreeModel model, JTree tree, FileData file, 
		IIconCache iconcache, IRemoteFilter filter, IExternalAccess exta, INodeFactory factory)
	{
		super(parent, model, tree, file, iconcache, filter, exta, factory);
//		System.out.println("node: "+getClass()+" "+desc.getName());
	}
	
	//-------- methods --------
	
	/**
	 *	Get a file filter according to current file type settings. 
	 */
	protected IFuture listFiles()
	{
		Future ret = new Future();
		
		final FileData myfile = file;
		final IRemoteFilter myfilter = filter;
		exta.scheduleStep(new IComponentStep()
		{
			@XMLClassname("listFiles")
			public Object execute(IInternalAccess ia)
			{
				final Future ret = new Future();
				
				final JarAsDirectory jad = new JarAsDirectory(myfile.getPath());
				jad.refresh();
								
				final Map rjfentries = new MultiCollection();
				MultiCollection zipentries = jad.createEntries();
				final CollectionResultListener lis = new CollectionResultListener(zipentries.size(), 
					true, new DelegationResultListener(ret)
				{
					public void customResultAvailable(Object result)
					{
						Collection col = (Collection)result;
						for(Iterator it=col.iterator(); it.hasNext(); )
						{
							Object[] tmp = (Object[])it.next();
							rjfentries.put(tmp[0], tmp[1]);
						}
						RemoteJarFile rjf = new RemoteJarFile(jad.getName(), jad.getAbsolutePath(), true, 
							FileData.getDisplayName(jad), rjfentries, "/", jad.getLastModified());
						Collection files = rjf.listFiles();
						ret.setResult(files);
					}
				});

				for(Iterator it=zipentries.keySet().iterator(); it.hasNext(); )
				{
					final String name = (String)it.next();
					Collection childs = (Collection)zipentries.get(name);
//					System.out.println("childs: "+childs);
					for(Iterator it2=childs.iterator(); it2.hasNext(); )
					{
						ZipEntry entry = (ZipEntry)it2.next();
						String ename = entry.getName();
						int	slash = ename.lastIndexOf("/", ename.length()-2);
						ename = ename.substring(slash!=-1? slash+1: 0, ename.endsWith("/")? ename.length()-1: ename.length());
//						System.out.println("ename: "+ename+" "+entry.getName());
						final RemoteJarFile tmp = new RemoteJarFile(ename, "jar:file:"+jad.getJarPath()+"!/"+entry.getName(), 
							entry.isDirectory(), ename, rjfentries, entry.getName(), entry.getTime());
						
						myfilter.filter(jad.getFile(entry.getName())).addResultListener(new IResultListener()
						{
							public void resultAvailable(Object result)
							{
								if(((Boolean)result).booleanValue())
								{
									lis.resultAvailable(new Object[]{name, tmp});
								}
								else
								{
									lis.exceptionOccurred(null);
								}
							}
							
							public void exceptionOccurred(Exception exception)
							{
								lis.exceptionOccurred(null);
							}
						});
						
					}
				}
				
				return ret;
			}
		}).addResultListener(new DelegationResultListener(ret));
		
		return ret;
	}
	
}
