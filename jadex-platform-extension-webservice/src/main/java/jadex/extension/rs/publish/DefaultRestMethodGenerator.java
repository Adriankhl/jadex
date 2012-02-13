package jadex.extension.rs.publish;

import jadex.bridge.service.IService;
import jadex.commons.SReflect;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * 
 */
public class DefaultRestMethodGenerator implements IRestMethodGenerator
{
	/**
	 * 
	 * @param service
	 * @param classloader
	 * @param baseclass
	 * @param mapprops
	 * @return
	 * @throws Exception
	 */
	public List<RestMethodInfo> generateRestMethodInfos(IService service, ClassLoader classloader, 
		Class<?> baseclass, Map<String, Object> mapprops) throws Exception
	{
		List<RestMethodInfo> ret = new ArrayList<RestMethodInfo>();
		
		boolean gen = mapprops.get(DefaultRestServicePublishService.GENERATE)!=null? 
			((Boolean)mapprops.get(DefaultRestServicePublishService.GENERATE)).booleanValue(): true;
		boolean geninfo = mapprops.get(DefaultRestServicePublishService.GENERATE_INFO)!=null? 
			((Boolean)mapprops.get(DefaultRestServicePublishService.GENERATE_INFO)).booleanValue(): true;
		MediaType[] formats = mapprops.get(DefaultRestServicePublishService.FORMATS)==null? 
			DefaultRestServicePublishService.DEFAULT_FORMATS: (MediaType[])mapprops.get(DefaultRestServicePublishService.FORMATS);
		Class<?> iface = service.getServiceIdentifier().getServiceType().getType(classloader);

		// Generation can be either
		// a) on basis of original interface
		// b) on basis of rest interface/abstract/normal class
		
		// Determine methods to be generated
		
		Set<MethodWrapper> methods = new LinkedHashSet<MethodWrapper>();
		if(gen)
		{
			if(baseclass!=null)
			{
				// Add all methods if is specific interface
				if(baseclass.isInterface())
				{
					Method[] ims = baseclass.getMethods();
					for(int i=0; i<ims.length; i++)
					{
						addMethodWrapper(new MethodWrapper(ims[i]), methods);
					}
				}
				// Else check for abstract methods (others are user implemented and will not be touched)
				else
				{
					Method[] bms = baseclass.getMethods();
					for(int i=0; i<bms.length; i++)
					{
						if(Modifier.isAbstract(bms[i].getModifiers()))
						{
							addMethodWrapper(new MethodWrapper(bms[i]), methods);
						}
					}
				}
				
				// Add additional interface methods of original interface if not already implemented
				Method[] ims = iface.getMethods();
				for(int i=0; i<ims.length; i++)
				{
					try
					{
						// Add method only if not already present
						baseclass.getMethod(ims[i].getName(), ims[i].getParameterTypes());
						addMethodWrapper(new MethodWrapper(ims[i]), methods);
					}
					catch(Exception e)
					{
					}
				}
			}
			// Add all interface methods
			else
			{
				Method[] ims = iface.getMethods();
				for(int i=0; i<ims.length; i++)
				{
					addMethodWrapper(new MethodWrapper(ims[i]), methods);
				}
			}
		}
		
		Set<String> paths = new HashSet<String>();
		for(Iterator<MethodWrapper> it = methods.iterator(); it.hasNext(); )
		{
			MethodWrapper mw = it.next();
			Method method = mw.getMethod();
			List<MediaType> consumed = new ArrayList<MediaType>();
			List<MediaType> produced = new ArrayList<MediaType>();
			
			// Determine rest method type.
			Class<?> resttype = getDeclaredRestType(method);
			// User defined method, use as is
			if(resttype!=null)
			{
				if(method.isAnnotationPresent(Consumes.class))
				{
					String[] cons = method.getAnnotation(Consumes.class).value();
					if(cons!=null)
					{
						for(int i=0; i<cons.length; i++)
						{
							consumed.add(MediaType.valueOf(cons[i]));
						}
					}
				}
				if(method.isAnnotationPresent(Produces.class))
				{
					String[] prods = method.getAnnotation(Produces.class).value();
					if(prods!=null)
					{
						for(int i=0; i<prods.length; i++)
						{
							produced.add(MediaType.valueOf(prods[i]));
						}
					}
				}
				
				ret.add(new RestMethodInfo(method, mw.getName(), getPathName(mw.getName(), paths), resttype, consumed, produced, 
					DefaultRestServicePublishService.class, "invoke"));
			}
			// Guess how method should be restified
			else
			{
				resttype = guessRestType(method);
				
				// Determine how many and which rest methods have to be created for the set of consumed media types.
				if(!GET.class.equals(resttype))
				{
					for(int j=0; j<formats.length; j++)
					{
						consumed.add(formats[j]);
					}
				}
				if(POST.class.equals(resttype))
				{
					consumed.add(MediaType.MULTIPART_FORM_DATA_TYPE);
				}
				if(GET.class.equals(resttype))
				{
					consumed.add(MediaType.TEXT_PLAIN_TYPE);
				}
				
				for(int j=0; j<formats.length; j++)
				{
					produced.add(formats[j]);
				}
				
				ret.add(new RestMethodInfo(method, mw.getName(), getPathName(mw.getName(), paths), resttype, consumed, produced,
					DefaultRestServicePublishService.class, "invoke"));
			}
		}
		
		if(geninfo)
		{
			List<MediaType> consumed = new ArrayList<MediaType>();
			List<MediaType> produced = new ArrayList<MediaType>();
			produced.add(MediaType.TEXT_HTML_TYPE);
			ret.add(new RestMethodInfo(new Class[0], String.class, new Class[0], "getServiceInfo", "", GET.class, 
				consumed, produced, DefaultRestServicePublishService.class, "getServiceInfo"));
		}
		
		return ret;
	}
	
	/**
	 *  Guess the http type (GET, POST, PUT, DELETE, ...) of a method.
	 *  @param method The method.
	 *  @return  The rs annotation of the method type to use 
	 */
	public Class<?> guessRestType(Method method)
	{
	    // Retrieve = GET (!hasparams && hasret)
	    // Update = POST (hasparams && hasret)
	    // Create = PUT  return is pointer to new resource (hasparams? && hasret)
	    // Delete = DELETE (hasparams? && hasret?)

		Class<?> ret = GET.class;
		
		Class<?> rettype = SReflect.unwrapGenericType(method.getGenericReturnType());
		Class<?>[] paramtypes = method.getParameterTypes();
		
		boolean hasparams = paramtypes.length>0;
		boolean hasret = !rettype.equals(Void.class) && !rettype.equals(void.class);
		
		// GET or POST if has both
		if(hasparams && hasret)
		{
			if(hasStringConvertableParameters(method, rettype, paramtypes))
			{
				ret = GET.class;
			}
			else
			{
				ret = POST.class;
			}
		}
		
//		System.out.println("rest-type: "+ret.getName()+" "+method.getName()+" "+hasparams+" "+hasret);
		
		return ret;
//		return GET.class;
	}
	
	/**
	 * 
	 */
	public static Class<?> getDeclaredRestType(Method method)
	{
		java.lang.annotation.Annotation ret = method.getAnnotation(GET.class);
		if(ret==null)
		{
			ret =  method.getAnnotation(POST.class);
			if(ret==null)
			{
				ret =  method.getAnnotation(PUT.class);
				if(ret==null)
				{
					ret =  method.getAnnotation(DELETE.class);
					if(ret==null)
					{
						ret =  method.getAnnotation(HEAD.class);
						if(ret==null)
						{
							ret =  method.getAnnotation(OPTIONS.class);
						}
					}
				}
			}
		}
		return ret==null? null: ret.annotationType();
	}
	
	/**
	 * 
	 * @param mw
	 * @param methods
	 */
	protected static void addMethodWrapper(MethodWrapper mw, Set<MethodWrapper> methods)
	{
		if(methods.contains(mw))
		{
			String basename = mw.getName();
			for(int j=0; methods.contains(mw); j++)
			{
				mw.setName(basename+j);
			}
		}
		methods.add(mw);
	}
	
	/**
	 * 
	 * @param mw
	 * @param methods
	 */
	protected static String getPathName(String name, Set<String> names)
	{
		if(names.contains(name))
		{
			String basename = name;
			for(int i=0; names.contains(name); i++)
			{
				name = basename+i;
			}
		}
		names.add(name);
		return name;
	}
	
	/**
	 * 
	 */
	public boolean hasStringConvertableParameters(Method method, Class<?> rettype, Class<?>[] paramtypes)
	{
		boolean ret = true;
		
		for(int i=0; i<paramtypes.length && ret; i++)
		{
			ret = isStringConvertableType(paramtypes[i]);
		}
		
		return ret;
	}
	
	/**
	 * 
	 */
	public static boolean isStringConvertableType(Class<?> type)
	{
		boolean ret = true;
		if(!SReflect.isStringConvertableType(type))
		{
			try
			{
				Method m = type.getMethod("fromString", new Class[]{String.class});
			}
			catch(Exception e)
			{
				try
				{
					Method m = type.getMethod("valueOf", new Class[]{String.class});
				}
				catch(Exception e2)
				{
					ret = false;
				}
			}
		}
		return ret;
	}
}
