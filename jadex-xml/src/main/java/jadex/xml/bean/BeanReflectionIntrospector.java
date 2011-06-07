package jadex.xml.bean;


import jadex.commons.collection.LRU;
import jadex.xml.SXML;
import jadex.xml.annotation.XMLClassname;
import jadex.xml.annotation.XMLExclude;
import jadex.xml.annotation.XMLInclude;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


/**
 * Introspector for Java beans. It uses the reflection to build up a map with
 * property infos (name, read/write method, etc.)
 */
public class BeanReflectionIntrospector implements IBeanIntrospector
{
	// -------- attributes --------

	/** The cache for saving time for multiple lookups. */
	protected LRU	beaninfos;

	// -------- constructors --------

	/**
	 * Create a new introspector.
	 */
	public BeanReflectionIntrospector()
	{
		this(200);
	}

	/**
	 * Create a new introspector.
	 */
	public BeanReflectionIntrospector(int lrusize)
	{
		this.beaninfos = new LRU(lrusize);
	}

	// -------- methods --------

	/**
	 * Get the bean properties for a specific clazz.
	 */
	public Map getBeanProperties(Class clazz, boolean includefields)
	{
		Map ret = (Map)beaninfos.get(clazz);

		if(ret == null)
		{
			Method[] ms = clazz.getMethods();
			HashMap getters = new HashMap();
			ArrayList setters = new ArrayList();
			for(int i = 0; i < ms.length; i++)
			{
				String method_name = ms[i].getName();
				XMLExclude ex = ms[i].getAnnotation(XMLExclude.class);
				if(ex==null)
				{
					if((method_name.startsWith("is") || method_name.startsWith("get"))
						&& ms[i].getParameterTypes().length == 0)
					{
						getters.put(method_name, ms[i]);
					}
					else if(method_name.startsWith("set")
						&& ms[i].getParameterTypes().length == 1)
					{
						setters.add(ms[i]);
					}
				}
			}

			ret = new HashMap();
			Iterator it = setters.iterator();

			while(it.hasNext())
			{
				Method setter = (Method)it.next();
				String setter_name = setter.getName();
				String property_name = setter_name.substring(3);
				Method getter = (Method)getters.get("get" + property_name);
				if(getter == null)
					getter = (Method)getters.get("is" + property_name);

				if(getter != null)
				{
					Class[] setter_param_type = setter.getParameterTypes();
					String property_java_name = Character.toLowerCase(property_name.charAt(0))
						+ property_name.substring(1);
					ret.put(property_java_name, new BeanProperty(property_java_name, 
						getter.getReturnType(), getter, setter, setter_param_type[0]));
				}
			}

			// Get all public fields.
			if(includefields)
			{
				Field[] fields = clazz.getFields();
				for(int i = 0; i < fields.length; i++)
				{
					String property_java_name = fields[i].getName();
					XMLExclude ex = fields[i].getAnnotation(XMLExclude.class);
					if(!ret.containsKey(property_java_name) && ex==null)
					{
						ret.put(property_java_name, new BeanProperty(property_java_name, fields[i]));
					}
				}
			}
			else
			{
				Field[] fields = clazz.getFields();
				for(int i = 0; i < fields.length; i++)
				{
					String property_java_name = fields[i].getName();
					XMLInclude in = fields[i].getAnnotation(XMLInclude.class);
					if(!ret.containsKey(property_java_name) && in!=null)
					{
						ret.put(property_java_name, new BeanProperty(property_java_name, fields[i]));
					}
				}
			}

			// Get final values (val$xyz fields) for anonymous classes.
			if(clazz.isAnonymousClass())
			{
				Field[] fields = clazz.getDeclaredFields();
				for(int i = 0; i < fields.length; i++)
				{
					String property_java_name = fields[i].getName();
					if(property_java_name.startsWith("val$"))
					{
						property_java_name = property_java_name.substring(4);
						if(!ret.containsKey(property_java_name))
						{
							ret.put(property_java_name, new BeanProperty(property_java_name, fields[i]));
						}
					}

					// Add XML class name property if field present (hack!!!)
					else if(SXML.XML_CLASSNAME.equals(property_java_name))
					{
						ret.put(property_java_name, new BeanProperty(property_java_name, fields[i]));
					}
				}

				// Add value of xml class name annotation. (hack!!! shouldn't be
				// property)
				if(!ret.containsKey(SXML.XML_CLASSNAME))
				{
					XMLClassname xmlc = SXML.getXMLClassnameAnnotation(clazz);

					if(xmlc != null)
						ret.put(SXML.XML_CLASSNAME, xmlc);
				}

				if(!ret.containsKey(SXML.XML_CLASSNAME))
				{
					System.err.println("Warning: Anonymous class without XML class name property (XML_CLASSNAME) / annotation (@XMLClassname): "+clazz.getName());
				}
			}

			beaninfos.put(clazz, ret);
		}

		return ret;
	}
}