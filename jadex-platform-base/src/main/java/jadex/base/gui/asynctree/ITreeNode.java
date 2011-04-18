package jadex.base.gui.asynctree;

import jadex.commons.future.IFuture;

import java.util.List;

import javax.swing.Icon;
import javax.swing.JComponent;

/**
 *  Node for the component tree panel.
 */
public interface ITreeNode
{
	/**
	 *  Called when the node is removed or the tree is closed.
	 */
	public void	dispose();
	
	/**
	 *  Get the id used for lookup.
	 */
	public Object	getId();
	
	/**
	 *  Get the parent node.
	 */
	public ITreeNode	getParent();
	
	/**
	 *  Get the child count.
	 */
	public int	getChildCount();
	
	/**
	 *  Get the given child.
	 */
	public ITreeNode	getChild(int index);
	
	/**
	 *  Get the index of a child.
	 */
	public int	getIndexOfChild(ITreeNode child);
	
	/**
	 *  Check if the node is a leaf.
	 */
	public boolean	isLeaf();
	
	/**
	 *  Get the icon for a node.
	 */
	public Icon	getIcon();
	
	/**
	 *  Refresh the node.
	 *  @param recurse	Recursively refresh subnodes, if true.
	 */
	public void	refresh(boolean recurse);
	
	/**
	 *  Get the cached children, i.e. do not start any background processes for updating the children.
	 */
	public List	getCachedChildren();
	
	/**
	 *  Get the current children, i.e. start a new update process and provide the result as a future.
	 */
	public IFuture	getChildren();
	
	/**
	 *  True, if the node has properties that can be displayed.
	 */
	public boolean	hasProperties();
	
	/**
	 *  Get or create a component displaying the node properties.
	 *  Only to be called if hasProperties() is true;
	 */
	public JComponent	getPropertiesComponent();
}
