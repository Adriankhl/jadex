package jadex.tools.common;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager2;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


/**
 *	A ObjectCardLayout is a object based cardlayout.
 */
public class ObjectCardLayout implements LayoutManager2
{
	//-------- constants --------
	
	/** The default component (used when no other component is shown). */
	public static final String	DEFAULT_COMPONENT	= "jtc_ocl_default_component";
	
	//-------- attributes --------

	/** The horizontal gap. */
	protected int	hgap;

	/** The vertical gap. */
	protected int	vgap;

	/**	The components. */
	protected Map	components;
	
	/** The current component (if any). */
	protected Component	current;

	/** The key of the current component (if any). */
	protected Object	curkey;

	//-------- constructors --------
	
	/**
	 *	Construct a new layout.
	 */
	public ObjectCardLayout() 
	{
		this.hgap	= 0;
		this.vgap	= 0;
		this.components	= new HashMap();
	}

	//-------- methods --------
	
	/**
	 *	Shows the component in the parent.
	 *	@param object The object.
	 *  @param parent The parent.
	 */
	public void	show(Object object) 
	{
		// Hide current component.
		if(current!=null)
		{
			current.setVisible(false);
		}

		// Find new component.
		Component comp	= (Component)components.get(object);
		if(comp==null)
		{
			comp	= (Component)components.get(DEFAULT_COMPONENT);
			curkey	= null;
		}
		else
		{
			curkey	= object;
		}

		// Show new component (if any).
		if(comp!=null)
		{
			comp.setVisible(true);
			comp.getParent().validate();
		}
		current	= comp;
	}
	
	/**
	 *  Test if there is a panel 
	 *  for a object available.
	 *  @param object The object.
	 *  @return True, if it exists a panel.
	 */
	public boolean isAvailable(Object object)
	{
		return components.containsKey(object);
	}
	
	/**
	 *  Get a component for the given identifier.
	 */
	public Component	getComponent(Object object)
	{
		return (Component)components.get(object);
	}
	
	/**
	 *  Get the key of the currently shown component (if any).
	 */
	public Object	getCurrentKey()
	{
		return curkey;
	}
	
	//-------- Interface LayoutManager methods --------
	
	/**
	 *	A component was added to the container.
	 *	@param	name	The constraint in a string.
	 *	@param	comp	The component.
	 */
	public void	addLayoutComponent(String name, Component comp) 
	{
		addLayoutComponent(comp, name);
	}
	
	/**
	 *	Set the components bounds in the parent.
	 *	@param	parent	The container.
	 */
	public void	layoutContainer(Container parent) 
	{
		Insets insets = parent.getInsets();
		Rectangle bounds	= parent.getBounds();
		int ncomponents = parent.getComponentCount();
		for (int i = 0 ; i < ncomponents ; i++) 
		{
			Component comp = parent.getComponent(i);
			if (comp.isVisible()) 
			{
				comp.setBounds(hgap + insets.left, vgap + insets.top,
					bounds.width - (hgap*2 + insets.left + insets.right),
					bounds.height - (vgap*2 + insets.top + insets.bottom));
			}
		}
	}
	
	/**
	 *	Compute the minimum layout size.
	 *	@param	parent	The container.
	 *	@return	The minimum layout size.
	 */
	public Dimension	minimumLayoutSize(Container parent) 
	{
		Insets insets = parent.getInsets();
		int ncomponents = parent.getComponentCount();
		int w = 0;
		int h = 0;
		
		for (int i=0 ; i<ncomponents; i++)
		{
			Component comp = parent.getComponent(i);
			Dimension d = comp.getMinimumSize();
			if (d.width > w) 
			{
				w = d.width;
			}
			if (d.height > h)
			{
				h = d.height;
			}
		}
		return new Dimension(insets.left + insets.right + w + hgap*2,
			insets.top + insets.bottom + h + vgap*2);
	}
	
	/**
	 *	Compute the preferred layout size.
	 *	@param	parent	The container.
	 *	@return	The preferred layout size.
	 */
	public Dimension	preferredLayoutSize(Container parent) 
	{
		Insets insets = parent.getInsets();
		int ncomponents = parent.getComponentCount();
		int w = 0;
		int h = 0;
		
		for (int i=0 ; i<ncomponents; i++)
		{
			Component comp = parent.getComponent(i);
			Dimension d = comp.getPreferredSize();
			if (d.width > w) 
			{
				w = d.width;
			}
			if (d.height > h)
			{
				h = d.height;
			}
		}
		return new Dimension(insets.left + insets.right + w + hgap*2,
			insets.top + insets.bottom + h + vgap*2);
	}
	
	/**
	 *	Will be called from the container, when
	 *	a component was removed.
	 *	@param	comp	The component.
	 */
	public void	removeLayoutComponent(Component comp) 
	{
		for(Iterator i=components.values().iterator(); i.hasNext();) 
		{
			if(i.next().equals(comp)) 
			{
				i.remove();
				break;
			}
		}
		
		if(comp==current)
		{
			comp.getParent().repaint();
			show(DEFAULT_COMPONENT);
		}
	}
	
	//-------- Interface LayoutManager2 methods --------

	/**
	 *	Will be called from the container, when
	 *	a new component with constraint object
	 *	was added.
	 *	@param	component	The component.
	 *	@param	constraints	The constraints.
	 */
	public void	addLayoutComponent(Component component, final Object constraints) 
	{	
		if(constraints == null)
			throw new RuntimeException("no_object_for_card_specified");
		components.put(constraints, component);
		component.setVisible(false);
		show(constraints);
	}

	/**	
	 *	Get the component interest where to be aligned.
	 *	(not used).
	 *	@param	target	The container.
	 *	@return	Always 0.5 (centered).
	 */
	public	float	getLayoutAlignmentX(Container target)
	{
		return 0.5f;
	}
	
	/**	
	 *	Get the component interest where to be aligned.
	 *	(not used).
	 *	@param	target	The container.
	 *	@return	Always 0.5 (centered).
	 */
	public	float	getLayoutAlignmentY(Container target)
	{
		return 0.5f;
	}
	
	/**
	 *	When called, all values are recalculated.
	 *	@param	target	The container.
	 */
	public	void	invalidateLayout(Container target)
	{
	}
	
	/**
	 *	Compute the maximum layout size.
	 *	@param	target	The container.
	 *	@return	The maximum layout size.
	 */
	public	Dimension	maximumLayoutSize(Container target)
	{
		return null;
	}

	/*
	 *
	 *	Get the string representation.
	 *	@return The string representation.
	 */
	public String	toString() 
	{
		return this.getClass().getName();
	}
}
