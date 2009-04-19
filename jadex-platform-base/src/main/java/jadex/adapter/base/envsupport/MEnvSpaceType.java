package jadex.adapter.base.envsupport;

import jadex.adapter.base.appdescriptor.ApplicationContext;
import jadex.adapter.base.appdescriptor.MApplicationType;
import jadex.adapter.base.appdescriptor.MSpaceType;
import jadex.adapter.base.envsupport.environment.AbstractEnvironmentSpace;
import jadex.adapter.base.envsupport.environment.IAgentAction;
import jadex.adapter.base.envsupport.environment.IEnvironmentSpace;
import jadex.adapter.base.envsupport.environment.IPerceptGenerator;
import jadex.adapter.base.envsupport.environment.ISpaceAction;
import jadex.adapter.base.envsupport.environment.ISpaceProcess;
import jadex.adapter.base.envsupport.environment.space2d.Space2D;
import jadex.adapter.base.envsupport.environment.view.IView;
import jadex.adapter.base.envsupport.math.IVector2;
import jadex.adapter.base.envsupport.math.Vector2Double;
import jadex.adapter.base.envsupport.math.Vector2Int;
import jadex.adapter.base.envsupport.observer.graphics.drawable.DrawableCombiner;
import jadex.adapter.base.envsupport.observer.graphics.drawable.IDrawable;
import jadex.adapter.base.envsupport.observer.graphics.drawable.TexturedRectangle;
import jadex.adapter.base.envsupport.observer.graphics.layer.GridLayer;
import jadex.adapter.base.envsupport.observer.graphics.layer.TiledLayer;
import jadex.adapter.base.envsupport.observer.gui.Configuration;
import jadex.adapter.base.envsupport.observer.gui.ObserverCenter;
import jadex.bridge.ILibraryService;
import jadex.commons.SReflect;
import jadex.commons.SUtil;
import jadex.commons.collection.MultiCollection;
import jadex.commons.xml.BasicTypeConverter;
import jadex.commons.xml.BeanAttributeInfo;
import jadex.commons.xml.ITypeConverter;
import jadex.commons.xml.LinkInfo;
import jadex.commons.xml.TypeInfo;
import jadex.javaparser.IExpressionParser;
import jadex.javaparser.IParsedExpression;
import jadex.javaparser.SimpleValueFetcher;
import jadex.javaparser.javaccimpl.JavaCCExpressionParser;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.text.html.StyleSheet;

/**
 *  Java representation of environemnt space type for xml description.
 */
public class MEnvSpaceType	extends MSpaceType
{
	//-------- attributes --------
	
	/** The properties. */
	protected Map properties;
	
	//-------- methods --------
	
	/**
	 *  Add a property.
	 *  @param key The key.
	 *  @param value The value.
	 */
	public void addProperty(String key, Object value)
	{
		if(properties==null)
			properties = new MultiCollection();
		properties.put(key, value);
	}
	
	/**
	 *  Get a property.
	 *  @param key The key.
	 *  @return The value.
	 */
	public List getPropertyList(String key)
	{
		return properties!=null? (List)properties.get(key):  null;
	}
	
	/**
	 *  Get the properties.
	 *  @return The properties.
	 */
	public Map getProperties()
	{
		return properties;
	}

//	/**
//	 *  Get a string representation of this AGR space type.
//	 *  @return A string representation of this AGR space type.
//	 */
//	public String	toString()
//	{
//		StringBuffer	sbuf	= new StringBuffer();
//		sbuf.append(SReflect.getInnerClassName(getClass()));
//		sbuf.append("(name=");
//		sbuf.append(getName());
//		sbuf.append(", dimensions=");
//		sbuf.append(getDimensions());
//		sbuf.append(", agent action types=");
//		sbuf.append(getMEnvAgentActionTypes());
//		sbuf.append(", space action types=");
//		sbuf.append(getMEnvSpaceActionTypes());
//		sbuf.append(", class=");
//		sbuf.append(getClazz());
//		sbuf.append(")");
//		return sbuf.toString();
//	}
	
	//-------- static part --------
	
	/**
	 *  Get the XML mapping.
	 */
	public static Set getXMLMapping()
	{
		Set types = new HashSet();
		
		ITypeConverter typeconv = new ClassConverter();
		ITypeConverter colorconv = new ColorConverter();
		
		types.add(new TypeInfo("envspacetype", MEnvSpaceType.class, null, null,
			SUtil.createHashMap(new String[]{"class"}, 
			new BeanAttributeInfo[]{new BeanAttributeInfo("clazz", typeconv, "property")}), null));
		
		types.add(new TypeInfo("envspace", MEnvSpaceInstance.class, null, null,
			SUtil.createHashMap(new String[]{"type"}, 
			new BeanAttributeInfo[]{new BeanAttributeInfo("typeName")
			}), null));
		
		types.add(new TypeInfo("agentactiontype", MultiCollection.class, null, null,
			SUtil.createHashMap(new String[]{"class", "name"}, 
			new BeanAttributeInfo[]{new BeanAttributeInfo("clazz", typeconv, ""),
			new BeanAttributeInfo(null, null, "")}), null));
		
		types.add(new TypeInfo("spaceactiontype", MultiCollection.class, null, null,
			SUtil.createHashMap(new String[]{"class", "name"}, 
			new BeanAttributeInfo[]{new BeanAttributeInfo("clazz", typeconv, ""),
			new BeanAttributeInfo(null, null, "")}), null));
		
		types.add(new TypeInfo("processtype", MultiCollection.class, null, null,
			SUtil.createHashMap(new String[]{"class", "name"}, 
			new BeanAttributeInfo[]{new BeanAttributeInfo("clazz", typeconv, ""),
			new BeanAttributeInfo(null, null, "")}), null));
		
		types.add(new TypeInfo("perceptgeneratortype", MultiCollection.class, null, null,
			SUtil.createHashMap(new String[]{"class", "name"}, 
			new BeanAttributeInfo[]{new BeanAttributeInfo("clazz", typeconv, ""),
			new BeanAttributeInfo(null, null, "")}), null));
		
		types.add(new TypeInfo("view", MultiCollection.class, null, null,
			SUtil.createHashMap(new String[]{"class", "name", "creator"}, 
			new BeanAttributeInfo[]{new BeanAttributeInfo("clazz", typeconv, ""),
			new BeanAttributeInfo(null, null, ""),
			new BeanAttributeInfo(null, null, "", new IObjectCreator()
			{
				public Object createObject(Map args) throws Exception
				{
					Map sourceview = (Map)args.get("sourceview");
					Configuration cfg = (Configuration)args.get("cfg");
					IEnvironmentSpace space = (IEnvironmentSpace)args.get("space");
					
					List sourcethemes = (List)sourceview.get("themes");
					if(sourcethemes!=null)
					{
						for(int j=0; j<sourcethemes.size(); j++)
						{
							Map sourcetheme = (Map)sourcethemes.get(j);
							cfg.setTheme((String)MEnvSpaceInstance.getProperty(sourcetheme, "name"), 
								(Map)((IObjectCreator)MEnvSpaceInstance.getProperty(sourcetheme, "creator")).createObject(sourcetheme));
						}
					}
					
					IView ret = (IView)((Class)MEnvSpaceInstance.getProperty(sourceview, "clazz")).newInstance();
					ret.setSpace(space);
					return ret;
				}
			})
			}), null));
		
		types.add(new TypeInfo("theme", MultiCollection.class, null, null,
			SUtil.createHashMap(new String[]{"name", "creator"}, 
			new BeanAttributeInfo[]{new BeanAttributeInfo(null, null, ""),
			new BeanAttributeInfo(null, null, "", new IObjectCreator()
			{
				public Object createObject(Map args) throws Exception
				{
					Map ret = new HashMap();
					
					List drawables = (List)args.get("drawables");
					if(drawables!=null)
					{
						for(int k=0; k<drawables.size(); k++)
						{
							Map sourcedrawable = (Map)drawables.get(k);
							ret.put(MEnvSpaceInstance.getProperty(sourcedrawable, "objecttype"), 
								((IObjectCreator)MEnvSpaceInstance.getProperty(sourcedrawable, "creator")).createObject(sourcedrawable));
						}
					}
					
					List prelayers = (List)args.get("prelayers");
					if(prelayers!=null)
					{
						List targetprelayers = new ArrayList();
						ret.put("prelayers", targetprelayers);
						for(int k=0; k<prelayers.size(); k++)
						{
							Map layer = (Map)prelayers.get(k);
							System.out.println("prelayer: "+layer);
							targetprelayers.add(((IObjectCreator)MEnvSpaceInstance.getProperty(layer, "creator")).createObject(layer));
						}
					}
					
					List postlayers = (List)args.get("postlayers");
					if(postlayers!=null)
					{
						List targetprelayers = new ArrayList();
						ret.put("postlayers", targetprelayers);
						for(int k=0; k<postlayers.size(); k++)
						{
							Map layer = (Map)postlayers.get(k);
							System.out.println("postlayer: "+layer);
							targetprelayers.add(((IObjectCreator)MEnvSpaceInstance.getProperty(layer, "creator")).createObject(layer));
						}
					}
					return ret;
				}
			})
			}), null));
		
		types.add(new TypeInfo("drawable", MultiCollection.class, null, null,
			SUtil.createHashMap(new String[]{"objecttype", "width", "height", "creator"}, 
			new BeanAttributeInfo[]{new BeanAttributeInfo(null, null, ""),
			new BeanAttributeInfo(null, BasicTypeConverter.DOUBLE_CONVERTER, ""),
			new BeanAttributeInfo(null, BasicTypeConverter.DOUBLE_CONVERTER, ""),
			new BeanAttributeInfo(null, null, "", new IObjectCreator()
			{
				public Object createObject(Map args) throws Exception
				{
					IVector2 size = MEnvSpaceInstance.getVector2((Double)MEnvSpaceInstance.getProperty(args, "width"), (Double)MEnvSpaceInstance.getProperty(args, "height"));
					DrawableCombiner ret = new DrawableCombiner(size);
					
					List parts = (List)args.get("parts");
					if(parts!=null)
					{
						for(int l=0; l<parts.size(); l++)
						{
							Map sourcepart = (Map)parts.get(l);
							ret.addDrawable((IDrawable)((IObjectCreator)MEnvSpaceInstance.getProperty(sourcepart, "creator")).createObject(sourcepart));
						}
					}
					return ret;
				}
			})
			}), null));

		types.add(new TypeInfo("texturedrectangle", MultiCollection.class, null, null,
			SUtil.createHashMap(new String[]{"imagepath", "width", "height", "shiftx", "shifty", "rotating", "creator"}, 
			new BeanAttributeInfo[]{new BeanAttributeInfo(null, null, ""),
			new BeanAttributeInfo(null, BasicTypeConverter.DOUBLE_CONVERTER, ""),
			new BeanAttributeInfo(null, BasicTypeConverter.DOUBLE_CONVERTER, ""),
			new BeanAttributeInfo(null, BasicTypeConverter.DOUBLE_CONVERTER, ""),
			new BeanAttributeInfo(null, BasicTypeConverter.DOUBLE_CONVERTER, ""),
			new BeanAttributeInfo(null, BasicTypeConverter.BOOLEAN_CONVERTER, ""),
			new BeanAttributeInfo(null, null, "", new IObjectCreator()
			{
				public Object createObject(Map args) throws Exception
				{
					IVector2 size = MEnvSpaceInstance.getVector2((Double)MEnvSpaceInstance.getProperty(args, "width"),
						(Double)MEnvSpaceInstance.getProperty(args, "height"));
					IVector2 shift = MEnvSpaceInstance.getVector2((Double)MEnvSpaceInstance.getProperty(args, "shiftx"), 
						(Double)MEnvSpaceInstance.getProperty(args, "shifty"));
					boolean rotating = MEnvSpaceInstance.getProperty(args, "rotating")==null? false: 
						((Boolean)MEnvSpaceInstance.getProperty(args, "rotating")).booleanValue();
					return new TexturedRectangle(size, shift, rotating, (String)MEnvSpaceInstance.getProperty(args, "imagepath"));
				}
			})
			}), null));
		
		types.add(new TypeInfo("gridlayer", MultiCollection.class, null, null,
			SUtil.createHashMap(new String[]{"color", "width", "height", "type", "creator"}, 
			new BeanAttributeInfo[]{new BeanAttributeInfo(null, colorconv, ""),
			new BeanAttributeInfo(null, BasicTypeConverter.DOUBLE_CONVERTER, ""),
			new BeanAttributeInfo(null, BasicTypeConverter.DOUBLE_CONVERTER, ""),
			new BeanAttributeInfo(null, null, "", "gridlayer"),
			new BeanAttributeInfo(null, null, "", new IObjectCreator()
			{
				public Object createObject(Map args) throws Exception
				{
					IVector2 size = MEnvSpaceInstance.getVector2((Double)MEnvSpaceInstance.getProperty(args, "width"),
							(Double)MEnvSpaceInstance.getProperty(args, "height"));
					return new GridLayer(size, (Color)MEnvSpaceInstance.getProperty(args, "color"));
				}
			})
			}), null));

		types.add(new TypeInfo("tiledlayer", MultiCollection.class, null, null,
			SUtil.createHashMap(new String[]{"imagepath", "width", "height", "type", "creator"}, 
			new BeanAttributeInfo[]{new BeanAttributeInfo(null, null, ""),
			new BeanAttributeInfo(null, BasicTypeConverter.DOUBLE_CONVERTER, ""),
			new BeanAttributeInfo(null, BasicTypeConverter.DOUBLE_CONVERTER, ""),
			new BeanAttributeInfo(null, null, "", "tiledlayer"),
			new BeanAttributeInfo(null, null, "", new IObjectCreator()
			{
				public Object createObject(Map args) throws Exception
				{
					IVector2 size = MEnvSpaceInstance.getVector2((Double)MEnvSpaceInstance.getProperty(args, "width"),
							(Double)MEnvSpaceInstance.getProperty(args, "height"));
					return new TiledLayer(size, (String)MEnvSpaceInstance.getProperty(args, "imagepath"));
				}
			})
			}), null));
			
		types.add(new TypeInfo("object", MultiCollection.class, null, null,
			SUtil.createHashMap(new String[]{"name", "type", "owner"}, 
			new BeanAttributeInfo[]{new BeanAttributeInfo(null, null, ""),
			new BeanAttributeInfo(null, null, ""),
			new BeanAttributeInfo(null, null, ""),
			new BeanAttributeInfo(null, null, "")
			}), null));
		
		return types;
	}
	
	/**
	 *  Get the XML link infos.
	 */
	public static Set getXMLLinkInfos()
	{
		Set linkinfos = new HashSet();

		ITypeConverter expconv = new ExpressionConverter();
		
		// applicationtype
		linkinfos.add(new LinkInfo("envspacetype", new BeanAttributeInfo("MSpaceType")));
		
		// application
		linkinfos.add(new LinkInfo("envspace", new BeanAttributeInfo("MSpaceInstance")));
	
		// spacetype
		linkinfos.add(new LinkInfo("dimension", new BeanAttributeInfo("dimensions", BasicTypeConverter.DOUBLE_CONVERTER, "property")));
		linkinfos.add(new LinkInfo("agentactiontype", new BeanAttributeInfo("agentactiontypes", null, "property")));
		linkinfos.add(new LinkInfo("spaceactiontype", new BeanAttributeInfo("spaceactiontypes", null, "property")));
		linkinfos.add(new LinkInfo("processtype", new BeanAttributeInfo("processtypes", null, "property")));
		linkinfos.add(new LinkInfo("perceptgeneratortype", new BeanAttributeInfo("perceptgeneratortypes", null, "property")));
		linkinfos.add(new LinkInfo("view", new BeanAttributeInfo("views", null, "property")));
		linkinfos.add(new LinkInfo("spaceexecutor", new BeanAttributeInfo(null, expconv, "property")));
		
		// view
		linkinfos.add(new LinkInfo("theme", new BeanAttributeInfo("themes", null, "")));
		
		// theme
		linkinfos.add(new LinkInfo("drawable", new BeanAttributeInfo("drawables", null, "")));
		linkinfos.add(new LinkInfo("prelayers/gridlayer", new BeanAttributeInfo("prelayers", null, "")));
		linkinfos.add(new LinkInfo("prelayers/tiledlayer", new BeanAttributeInfo("prelayers", null, "")));
		linkinfos.add(new LinkInfo("postlayers/gridlayer", new BeanAttributeInfo("postlayers", null, "")));
		linkinfos.add(new LinkInfo("postlayers/tiledlayer", new BeanAttributeInfo("postlayers", null, "")));
		
		// drawable
		linkinfos.add(new LinkInfo("texturedrectangle", new BeanAttributeInfo("parts", null, "")));		
		
		// space instance
		linkinfos.add(new LinkInfo("object", new BeanAttributeInfo("objects", null, "property")));
		
		return linkinfos;
	}
	
	/**
	 *  Parse class names.
	 */
	static class ExpressionConverter implements ITypeConverter
	{
		// Hack!!! Should be configurable.
		protected static IExpressionParser	exp_parser	= new JavaCCExpressionParser();

		/**
		 *  Convert a string value to a type.
		 *  @param val The string value to convert.
		 */
		public Object convertObject(Object val, Object root, ClassLoader classloader)
		{
			if(!(val instanceof String))
				throw new RuntimeException("Source value must be string: "+val);
			
			Object ret = null;
			
			System.out.println("Found expression: "+val);
			try
			{
				ret = exp_parser.parseExpression((String)val, ((MApplicationType)root).getAllImports(), null, classloader);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			
			return ret;
		}
	}
	
	/**
	 *  Parse class names.
	 */
	static class ClassConverter	implements ITypeConverter
	{
		/**
		 *  Convert a string value to a type.
		 *  @param val The string value to convert.
		 */
		public Object convertObject(Object val, Object root, ClassLoader classloader)
		{
			if(!(val instanceof String))
				throw new RuntimeException("Source value must be string: "+val);
			Class ret = SReflect.findClass0((String)val, ((MApplicationType)root).getAllImports(), classloader);
			if(ret==null)
				throw new RuntimeException("Could not parse class: "+val);
			return ret;
		}
	}
	
	/**
	 *  Parse class names.
	 */
	static class ColorConverter	implements ITypeConverter
	{
		protected StyleSheet ss = new StyleSheet();
		
		/**
		 *  Convert a string value to a type.
		 *  @param val The string value to convert.
		 */
		public Object convertObject(Object val, Object root, ClassLoader classloader)
		{
			if(!(val instanceof String))
				throw new RuntimeException("Source value must be string: "+val);
			// Cannot use CSS.stringToColor() because they haven't made it public :-(
			return ss.stringToColor((String)val);
		}
	}
}
