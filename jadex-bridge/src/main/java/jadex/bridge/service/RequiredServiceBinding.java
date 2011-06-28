package jadex.bridge.service;

import java.util.ArrayList;
import java.util.List;

import jadex.bridge.modelinfo.UnparsedExpression;

/**
 *  Required service binding information.
 */
public class RequiredServiceBinding
{
	//-------- attributes --------
	
	/** The service name. */
	protected String name;
	
	/** The component name. */
	protected String componentname;
	
	/** The component type, i.e. the model name. */
	protected String componenttype;

	/** The component filename. */
//	protected String componentfilename;
	
	/** Flag if binding is dynamic. */
	protected boolean dynamic;

	/** The search scope. */
	protected String scope;
	
	/** The create flag. */
	protected boolean create;
	
	/** The recover flag. */
	protected boolean recover;
	
	/** The interceptors. */
	protected List interceptors;

	//-------- constructors --------

	/**
	 *  Create a new binding. 
	 */
	public RequiredServiceBinding()
	{
	}
	
	/**
	 *  Create a new binding. 
	 */
	public RequiredServiceBinding(String name, String scope)
	{
		this(name, scope, false);
	}
	
	/**
	 *  Create a new binding. 
	 */
	public RequiredServiceBinding(String name, String scope, boolean dynamic)
	{
		this(name, null, null, dynamic, scope, false, false, null);
	}

	/**
	 *  Create a new binding.
	 */
	public RequiredServiceBinding(String name, String componentname,
		String componenttype, boolean dynamic, String scope, boolean create, boolean recover,
		UnparsedExpression[] interceptors)
	{
		this.name = name;
		this.componentname = componentname;
		this.componenttype = componenttype;
		this.dynamic = dynamic;
		this.scope = scope;
		this.create = create;
		this.recover = recover;
		if(interceptors!=null)
		{
			for(int i=0; i<interceptors.length; i++)
			{
				addInterceptor(interceptors[i]);
			}
		}
//		this.componentfilename = componentfilename;
	}
	
	/**
	 *  Create a new binding.
	 */
	public RequiredServiceBinding(RequiredServiceBinding orig)
	{
		this(orig.getName(), orig.getComponentName(), orig.getComponentType(), 
			orig.isDynamic(), orig.getScope(), orig.isCreate(), orig.isRecover(), orig.getInterceptors());
	}

	//-------- methods --------
	
	/**
	 *  Get the name.
	 *  @return the name.
	 */
	public String getName()
	{
		return name;
	}

	/**
	 *  Set the name.
	 *  @param name The name to set.
	 */
	public void setName(String name)
	{
		this.name = name;
	}

	/**
	 *  Get the componentname.
	 *  @return the componentname.
	 */
	public String getComponentName()
	{
		return componentname;
	}

	/**
	 *  Set the componentname.
	 *  @param componentname The componentname to set.
	 */
	public void setComponentName(String componentname)
	{
		this.componentname = componentname;
	}

	/**
	 *  Get the componenttype.
	 *  @return the componenttype.
	 */
	public String getComponentType()
	{
		return componenttype;
	}

	/**
	 *  Set the componenttype.
	 *  @param componenttype The componenttype to set.
	 */
	public void setComponentType(String componenttype)
	{
		this.componenttype = componenttype;
	}

	/**
	 *  Get the dynamic.
	 *  @return the dynamic.
	 */
	public boolean isDynamic()
	{
		return dynamic;
	}

	/**
	 *  Set the dynamic.
	 *  @param dynamic The dynamic to set.
	 */
	public void setDynamic(boolean dynamic)
	{
		this.dynamic = dynamic;
	}

	/**
	 *  Get the scope.
	 *  @return the scope.
	 */
	public String getScope()
	{
		return scope;
	}

	/**
	 *  Set the scope.
	 *  @param scope The scope to set.
	 */
	public void setScope(String scope)
	{
		this.scope = scope;
	}

	/**
	 *  Get the create.
	 *  @return The create.
	 */
	public boolean isCreate()
	{
		return create;
	}

	/**
	 *  Set the create.
	 *  @param create The create to set.
	 */
	public void setCreate(boolean create)
	{
		this.create = create;
	}

	/**
	 *  Get the recover.
	 *  @return The recover.
	 */
	public boolean isRecover()
	{
		return recover;
	}

	/**
	 *  Set the recover.
	 *  @param recover The recover to set.
	 */
	public void setRecover(boolean recover)
	{
		this.recover = recover;
	}
	
	/**
	 *  Add an interceptor.
	 *  @param interceptor The interceptor.
	 */
	public void addInterceptor(UnparsedExpression interceptor)
	{
		if(interceptors==null)
			interceptors = new ArrayList();
		interceptors.add(interceptor);
	}
	
	/**
	 *  Remove an interceptor.
	 *  @param interceptor The interceptor.
	 */
	public void removeInterceptor(UnparsedExpression interceptor)
	{
		interceptors.remove(interceptor);
	}
	
	/**
	 *  Get the interceptors.
	 *  @return All interceptors.
	 */
	public UnparsedExpression[] getInterceptors()
	{
		return interceptors==null? new UnparsedExpression[0]: (UnparsedExpression[])
			interceptors.toArray(new UnparsedExpression[interceptors.size()]);
	}

	/**
	 *  Get the string representation.
	 */
	public String toString()
	{
		return " scope=" + scope + ", dynamic="+ dynamic + ", create=" + create + ", recover=" 
			+ recover+ ", componentname=" + componentname + ", componenttype="+ componenttype;
	}

	
}
