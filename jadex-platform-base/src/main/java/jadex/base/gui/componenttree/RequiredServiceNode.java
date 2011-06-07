package jadex.base.gui.componenttree;

import jadex.base.gui.asynctree.AbstractTreeNode;
import jadex.base.gui.asynctree.AsyncTreeModel;
import jadex.base.gui.asynctree.ITreeNode;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.commons.SReflect;
import jadex.commons.gui.SGUI;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.UIDefaults;

/**
 *  Node object representing a service container.
 */
public class RequiredServiceNode extends AbstractTreeNode
{
	//-------- constants --------
	
	/** The service container icon. */
	private static final UIDefaults icons = new UIDefaults(new Object[]
	{
		"service", SGUI.makeIcon(RequiredServiceNode.class, "/jadex/base/gui/images/required_16.png"),
		"services", SGUI.makeIcon(RequiredServiceNode.class, "/jadex/base/gui/images/required_multiple_16.png")
	});
	
	//-------- attributes --------
	
	/** The service info. */
	private final RequiredServiceInfo info;
	
	/** The node id. */
	protected final String nid;

	/** The properties component (if any). */
	protected RequiredServiceProperties	propcomp;

	//-------- constructors --------
	
	/**
	 *  Create a new service container node.
	 */
	public RequiredServiceNode(ITreeNode parent, AsyncTreeModel model, JTree tree, RequiredServiceInfo info, String nid)
	{
		super(parent, model, tree);
		this.info = info;
		this.nid = nid;
//		if(service==null || service.getServiceIdentifier()==null)
//			System.out.println("service node: "+this);
		model.registerNode(this);
	}
	
	//-------- methods --------
	
	/**
	 *  Get the service info.
	 */
	public RequiredServiceInfo getServiceInfo()
	{
		return info;
	}

	/**
	 *  Get the id used for lookup.
	 */
	public Object getId()
	{
		return nid;
	}

	/**
	 *  Get the icon for a node.
	 */
	public Icon	getIcon()
	{
		return info.isMultiple()? icons.getIcon("services"): icons.getIcon("service");
	}

	/**
	 *  Asynchronously search for children.
	 *  Called once for each node.
	 *  Should call setChildren() once children are found.
	 */
	protected void	searchChildren()
	{
		// no children
	}
	
	/**
	 *  A string representation.
	 */
	public String toString()
	{
		StringBuffer buf = new StringBuffer();
		buf.append(SReflect.getUnqualifiedClassName(info.getType()));
		if(info.getName()!=null)	
			buf.append(" (").append(info.getName()).append(")");
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
			propcomp	= new RequiredServiceProperties();
		}
		propcomp.setService(info);
		return propcomp;
	}
}
