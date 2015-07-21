package jadex.transformation.jsonserializer.processors;

import java.lang.reflect.Constructor;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.eclipsesource.json.JsonObject;

import jadex.commons.SReflect;
import jadex.commons.transformation.BasicTypeConverter;
import jadex.commons.transformation.IObjectStringConverter;
import jadex.commons.transformation.IStringObjectConverter;
import jadex.commons.transformation.binaryserializer.BeanIntrospectorFactory;
import jadex.commons.transformation.traverser.BeanProperty;
import jadex.commons.transformation.traverser.IBeanIntrospector;
import jadex.commons.transformation.traverser.ITraverseProcessor;
import jadex.commons.transformation.traverser.Traverser;

/**
 * 
 */
public class JsonBeanProcessor implements ITraverseProcessor
{
	
	/** Bean introspector for inspecting beans. */
	protected IBeanIntrospector intro = BeanIntrospectorFactory.getInstance().getBeanIntrospector(5000);
	
	/**
	 *  Test if the processor is applicable.
	 *  @param object The object.
	 *  @param targetcl	If not null, the traverser should make sure that the result object is compatible with the class loader,
	 *    e.g. by cloning the object using the class loaded from the target class loader.
	 *  @return True, if is applicable. 
	 */
	public boolean isApplicable(Object object, Class<?> clazz, boolean clone, ClassLoader targetcl)
	{
		return object instanceof JsonObject && (clazz!=null && !SReflect.isSupertype(Map.class, clazz));
	}
	
	/**
	 *  Process an object.
	 *  @param object The object.
	 *  @param targetcl	If not null, the traverser should make sure that the result object is compatible with the class loader,
	 *    e.g. by cloning the object using the class loaded from the target class loader.
	 *  @return The processed object.
	 */
	public Object process(Object object, Class<?> clazz, List<ITraverseProcessor> processors, 
		Traverser traverser, Map<Object, Object> traversed, boolean clone, ClassLoader targetcl, Object context)
	{
		Object ret = null;
		
		if(clazz==null)
			System.out.println("clazz is null");
		
		ret = getReturnObject(object, clazz, targetcl);
		traversed.put(object, ret);
		
		try
		{
			traverseProperties(object, clazz, traversed, processors, traverser, clone, targetcl, ret, context);
		}
		catch(Exception e)
		{
			throw (e instanceof RuntimeException) ? (RuntimeException)e : new RuntimeException(e);
		}
		
		return ret;
	}
	
	/**
	 *  Clone all properties of an object.
	 */
	protected void traverseProperties(Object object, Class<?> clazz, Map<Object, Object> cloned, 
		List<ITraverseProcessor> processors, Traverser traverser, boolean clone, ClassLoader targetcl, Object ret, Object context)
	{
		// Get all declared fields (public, protected and private)
		
		JsonObject jval = (JsonObject)object;
		Map<String, BeanProperty> props = intro.getBeanProperties(clazz, true, false);
		
		for(Iterator<String> it=props.keySet().iterator(); it.hasNext(); )
		{
			try
			{
				String name = (String)it.next();
				BeanProperty prop = (BeanProperty)props.get(name);
				if(prop.isReadable() && prop.isWritable())
				{
					Object val = jval.get(name);
					if(val!=null) 
					{
						if(prop.getGenericType()!=null)
						{
							((JsonContext)context).setComponentType(SReflect.unwrapGenericType(prop.getGenericType()));
						}
						else
						{
							((JsonContext)context).setComponentType(null);
						}
						Object newval = traverser.doTraverse(val, prop.getType(), cloned, processors, clone, targetcl, context);
						if(newval != Traverser.IGNORE_RESULT && (object!=ret || val!=newval))
						{
							prop.setPropertyValue(ret, convertBasicType(newval, prop.getType()));
						}
					}
				}
			}
			catch(Exception e)
			{
				throw (e instanceof RuntimeException) ? (RuntimeException)e : new RuntimeException(e);
			}
		}
	}
	
	/**
	 *  Get the object that is returned.
	 */
	public Object getReturnObject(Object object, Class<?> clazz, ClassLoader targetcl)
	{
		Object ret = null;
		
		if(targetcl!=null)
			clazz	= SReflect.classForName0(clazz.getName(), targetcl);
		
		Constructor<?>	c;
		
		try
		{
			c	= clazz.getConstructor(new Class[0]);
		}
		catch(NoSuchMethodException nsme)
		{
			c	= clazz.getDeclaredConstructors()[0];
		}

		try
		{
			c.setAccessible(true);
			Class<?>[] paramtypes = c.getParameterTypes();
			Object[] paramvalues = new Object[paramtypes.length];
			for(int i=0; i<paramtypes.length; i++)
			{
				if(paramtypes[i].equals(boolean.class))
				{
					paramvalues[i] = Boolean.FALSE;
				}
				else if(SReflect.isBasicType(paramtypes[i]))
				{
					paramvalues[i] = 0;
				}
			}
			ret = c.newInstance(paramvalues);
		}
		catch(Exception e)
		{
			System.out.println("beanproc ex: "+object+" "+c);
			throw new RuntimeException(e);
		}
		return ret;
	}
	
	/**
	 * 
	 * @param value
	 * @param targetclazz
	 * @return
	 */
	public static Object convertBasicType(Object value, Class<?> targetclazz)
	{
		if(!SReflect.isSupertype(targetclazz, value.getClass()))
		{
			// Autoconvert basic from string
			if(value instanceof String)
			{
				IStringObjectConverter conv = BasicTypeConverter.getBasicStringConverter(targetclazz);
				if(conv!=null)
				{
					try
					{
						value = conv.convertString((String)value, null);
					}
					catch(Exception e)
					{
						e.printStackTrace();
					}
				}
			}
			// Autoconvert basic to string
			else if(targetclazz.equals(String.class))
			{
				IObjectStringConverter conv = BasicTypeConverter.getBasicObjectConverter(value.getClass());
				if(conv!=null)
				{
					try
					{
						value = conv.convertObject(value, null);
					}
					catch(Exception e)
					{
						e.printStackTrace();
					}
				}
			}
		}
		
		return value;
	}
}