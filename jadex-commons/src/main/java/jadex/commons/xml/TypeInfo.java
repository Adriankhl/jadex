package jadex.commons.xml;

import java.util.HashMap;
import java.util.Map;

/**
 *  Mapping from tag (or path fragment) to OAV type.
 */
public class TypeInfo	extends AbstractInfo
{
	//-------- attributes -------- 
	
	/** The type info. */
	protected Object typeinfo;
	
	/** The comment info. */
	protected Object commentinfo;
	
	/** The content info. */
	protected Object contentinfo;
	
	/** The attributes info. */
	protected Map attributesinfo;
	
	/** The attributes converters. */
	protected Map attributesconverters;

	/** The post processor (if any). */
	protected IPostProcessor postproc;
	
	//-------- constructors --------
	
	/**
	 *  Create a new type info.
	 *  @param xmlpath The path or tag.
	 *  @param typeinfo The type of object to create.
	 */
	public TypeInfo(String xmlpath, Object typeinfo)
	{
		this(xmlpath, typeinfo, null, null, null, null, null);
	}
	
	/**
	 *  Create a new type info.
	 *  @param xmlpath The path or tag.
	 *  @param type The type of object to create.
	 *  @param commentinfo The commnentinfo.
	 *  @param contentinfo The contentinfo.
	 */
	public TypeInfo(String xmlpath, Object typeinfo, Object commentinfo, Object contentinfo)
	{
		this(xmlpath, typeinfo, commentinfo, contentinfo, null, null, null);
	}
	
	/**
	 *  Create a new type info.
	 *  @param xmlpath The path or tag.
	 *  @param typeinfo The type of object to create.
	 *  @param commentinfo The commnent.
	 *  @param contentinfo The content.
	 *  @param attributesinfo The attributes map.
	 *  @param postproc The post processor. 
	 */
	public TypeInfo(String xmlpath, Object typeinfo, Object commentinfo, Object contentinfo, 
		Map attributesinfo, Map attributesconverters, IPostProcessor postproc)
	{
		super(xmlpath);
		this.typeinfo = typeinfo;
		this.commentinfo = commentinfo;
		this.contentinfo = contentinfo;
		this.attributesinfo = attributesinfo;
		this.attributesconverters = attributesconverters;
		this.postproc = postproc;
	}
	
	//-------- methods --------

	/**
	 *  Get the type info.
	 *  @return The type.
	 */
	public Object getTypeInfo()
	{
		return this.typeinfo;
	}

	/**
	 *  Set the type info.
	 *  @param type The type to set.
	 */
	public void setTypeInfo(Object type)
	{
		this.typeinfo = typeinfo;
	}

	/**
	 *  Get the comment info.
	 *  @return The comment
	 */
	public Object getCommentInfo()
	{
		return this.commentinfo;
	}

	/**
	 *  Set the comment info.
	 *  @param commentinfo The comment to set.
	 */
	public void setCommentInfo(Object commentinfo)
	{
		this.commentinfo = commentinfo;
	}

	/**
	 *  Get the content info.
	 *  @return The content info.
	 */
	public Object getContentInfo()
	{
		return this.contentinfo;
	}

	/**
	 *  Set the content info.
	 *  @param contentinfo The content info to set.
	 */
	public void setContentInfo(Object content)
	{
		this.contentinfo = contentinfo;
	}
	
	/**
	 *  Add an attribute info.
	 *  @param xmlname The xml attribute name.
	 *  @param attrinfo The attribute info.
	 */
	public void addAttributeInfo(String xmlname, Object attrinfo)
	{
		if(attributesinfo==null)
			attributesinfo = new HashMap();
		attributesinfo.put(xmlname, attrinfo);
	}
	
	/**
	 *  Add an attribute info.
	 *  @param xmlname The xml attribute name.
	 *  @param attrinfo The attribute info.
	 */
	public void addAttributeConverter(String xmlname, ITypeConverter converter)
	{
		if(attributesconverters==null)
			attributesconverters = new HashMap();
		attributesconverters.put(xmlname, converter);
	}
	
	/**
	 *  Get the attribute info.
	 *  @param xmlname The xml name of the attribute.
	 *  @return The attribute info.
	 */
	public Object getAttributeInfo(String xmlname)
	{
		return attributesinfo==null? null: attributesinfo.get(xmlname);
	}
	
	/**
	 *  Get the attribute converter.
	 *  @param xmlname The xml name of the attribute.
	 *  @return The attribute converter.
	 */
	public ITypeConverter getAttributeConverter(String xmlname)
	{
		return (ITypeConverter)(attributesconverters==null? null: attributesconverters.get(xmlname));
	}

	/**
	 *  Get the post-processor.
	 *  @return The post-processor
	 */
	public IPostProcessor getPostProcessor()
	{
		return this.postproc;
	}

	/**
	 *  Set the post-processor.
	 *  @param pproc The post-processor.
	 */
	public void setPostProcessor(IPostProcessor pproc)
	{
		this.postproc = pproc;
	}	
}
