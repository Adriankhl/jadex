package jadex.xml;

import jadex.xml.annotation.XMLClassname;
import jadex.xml.bean.JavaWriter;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/* $if !android $ */
import javax.xml.namespace.QName;
/* $else $
import javaxx.xml.namespace.QName;
$endif $ */

/**
 *  Constants for xml handling.
 */
public class SXML
{
	//-------- constants --------
	
	/** The ID attribute constant. */
	public static final String ID = "__ID";
	
	/** The IDREF attribute constant. */
	public static final String IDREF = "__IDREF";
	
	/** The package protocol constant. */
	public static final String PROTOCOL_TYPEINFO = "typeinfo:";

	/** The value of this attribute is used as idref. */
	public static final String ARRAYLEN = "__len";
	
	/** The null tag. */
	public static QName NULL = new QName(SXML.PROTOCOL_TYPEINFO, "null");
	
	/** Constant for anonymous inner classes. */
	public static final String XML_CLASSNAME = "XML_CLASSNAME";

	/**
	 *  Get the xmlclassname annotation.
	 */
	public static XMLClassname getXMLClassnameAnnotation(Class clazz)
	{
		XMLClassname	xmlc	= null;
		// Find annotation in fields or methods of class, because annotations are not supported on anonymous classes directly.
		Field[] fields = clazz.getDeclaredFields();
		for(int i=0; xmlc==null && i<fields.length; i++)
		{
			xmlc	= fields[i].getAnnotation(XMLClassname.class);
		}
		if(xmlc==null)
		{
			Method[]	methods	= clazz.getDeclaredMethods();
			for(int i=0; xmlc==null && i<methods.length; i++)
			{
				xmlc	= methods[i].getAnnotation(XMLClassname.class);
			}
		}
		return xmlc;
	}
}
