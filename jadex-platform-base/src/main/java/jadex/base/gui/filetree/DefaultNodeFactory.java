package jadex.base.gui.filetree;

import jadex.base.gui.asynctree.AsyncTreeModel;
import jadex.base.gui.asynctree.ITreeNode;
import jadex.bridge.IExternalAccess;
import jadex.bridge.service.types.deployment.FileData;
import jadex.commons.IRemoteFilter;
import jadex.commons.SUtil;

import java.io.File;

import javax.swing.JTree;

/** 
 *  Default factory for creating nodes.
 */
public class DefaultNodeFactory implements INodeFactory
{
	//-------- methods --------
	
	/**
	 *  Create a new component node.
	 */
	public ITreeNode createNode(ITreeNode parent, AsyncTreeModel model, JTree tree,
		Object value, IIconCache iconcache, IRemoteFilter filter, IExternalAccess exta, INodeFactory factory)
	{
		ITreeNode ret = null;
		
		if(value instanceof File)
		{
			File file = (File)value;
			if(SUtil.arrayToSet(File.listRoots()).contains(file) || file.isDirectory())
			{
				ret = new DirNode(parent, model, tree, file, iconcache, filter, factory);
			}
			else if(file.getName().endsWith(".jar") || file.getName().endsWith(".zip"))
			{
				ret = new JarNode(parent, model, tree, file, iconcache, filter, factory);
			}
			else
			{
				ret = new FileNode(parent, model, tree, file, iconcache);
			}
		}
		else if(value instanceof FileData)
		{
			FileData file = (FileData)value;
			if(file.isDirectory())
			{
				ret = new RemoteDirNode(parent, model, tree, file, iconcache, filter, exta, factory);
			}
			else if(file.getFilename().endsWith(".jar") || file.getFilename().endsWith(".zip"))
			{
				ret = new RemoteJarNode(parent, model, tree, file, iconcache, filter, exta, factory);
			}
			else
			{
				ret = new RemoteFileNode(parent, model, tree, file, iconcache, exta);
			}
		}
		
		if(ret==null)
			throw new IllegalArgumentException("Unknown value: "+value);
		
		return ret;
	}
}
