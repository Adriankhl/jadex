package jadex.commons.service;

/**
 *  Struct for information about a required service.
 */
public class RequiredServiceInfo
{
	//-------- constants --------
	
	/** Local component scope. */
	public static final String SCOPE_LOCAL = "local";
	
	/** Component scope. */
	public static final String SCOPE_COMPONENT = "component";
	
	/** Application scope. */
	public static final String SCOPE_APPLICATION = "application";

	/** Platform scope. */
	public static final String SCOPE_PLATFORM = "platform";

	/** Global scope. */
	public static final String SCOPE_GLOBAL = "global";
	
	/** Upwards scope. */
	public static final String SCOPE_UPWARDS = "upwards";
	
	//-------- attributes --------
	
	/** The component internal service name. */
	protected String name;
	
	/** The service interface type. */
	protected Class type;
	
	/** Flag if binding is dynamic. */
	protected boolean dynamic;

	/** Flag if multiple services should be returned. */
	protected boolean multiple;

	/** The search scope. */
	protected String scope;
	
	//-------- constructors --------
	
	/**
	 *  Create a new service info.
	 */
	public RequiredServiceInfo()
	{
		// bean constructor
	}
	
	/**
	 *  Create a new service info.
	 */
	public RequiredServiceInfo(String name, Class type)
	{
		this(name, type, false, false);
	}
	
	/**
	 *  Create a new service info.
	 */
	public RequiredServiceInfo(String name, Class type, String scope)
	{
		this(name, type, false, false, scope);
	}
	
	/**
	 *  Create a new service info.
	 */
	public RequiredServiceInfo(String name, Class type, boolean dynamic, boolean multiple)
	{
		this(name, type, dynamic, multiple, SCOPE_APPLICATION);
	}
	
	/**
	 *  Create a new service info.
	 */
	public RequiredServiceInfo(String name, Class type, boolean dynamic, boolean multiple, String scope)
	{
		this.name = name;
		this.type = type;
		this.dynamic = dynamic;
		this.multiple = multiple;
		this.scope = scope;
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
	 *  Get the type.
	 *  @return the type.
	 */
	public Class getType()
	{
		return type;
	}

	/**
	 *  Set the type.
	 *  @param type The type to set.
	 */
	public void setType(Class type)
	{
		this.type = type;
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
	 *  Get the multiple.
	 *  @return the multiple.
	 */
	public boolean isMultiple()
	{
		return multiple;
	}

	/**
	 *  Set the multiple.
	 *  @param multiple The multiple to set.
	 */
	public void setMultiple(boolean multiple)
	{
		this.multiple = multiple;
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
}
