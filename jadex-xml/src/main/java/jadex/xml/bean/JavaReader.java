package jadex.xml.bean;

import jadex.commons.Base64;
import jadex.commons.SReflect;
import jadex.commons.collection.MultiCollection;
import jadex.xml.AccessInfo;
import jadex.xml.AttributeConverter;
import jadex.xml.AttributeInfo;
import jadex.xml.IContext;
import jadex.xml.IObjectObjectConverter;
import jadex.xml.IPostProcessor;
import jadex.xml.IStringObjectConverter;
import jadex.xml.MappingInfo;
import jadex.xml.ObjectInfo;
import jadex.xml.SXML;
import jadex.xml.SubObjectConverter;
import jadex.xml.SubobjectInfo;
import jadex.xml.TypeInfo;
import jadex.xml.TypeInfoPathManager;
import jadex.xml.XMLInfo;
import jadex.xml.reader.Reader;

import java.awt.Color;
import java.awt.Image;
import java.awt.Toolkit;
import java.io.ByteArrayInputStream;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;

import javax.imageio.ImageIO;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLReporter;

/**
 *  Java specific reader that supports collection classes and arrays.
 */
public class JavaReader
{
	//-------- attributes --------

	/** The reader. */
	protected static Reader reader;

	/**
	 *  Join sets of typeinfos.
	 *  @param typeinfos The user specific type infos. 
	 *  @return The joined type infos.
	 */
	public static Set joinTypeInfos(Set typeinfos)
	{
		Set ret = getTypeInfos();
		if(typeinfos!=null)
			ret.addAll(typeinfos);
		return ret;
	}
	
	/**
	 *  Get the type infos.
	 */
	public static Set getTypeInfos()
	{
		Set typeinfos = new HashSet();
		try
		{
			// java.util.Map
			IObjectObjectConverter entryconv = new IObjectObjectConverter()
			{
				public Object convertObject(Object val, IContext context)
				{
					return ((MapEntry)val).getValue();
				}
			};
			
			TypeInfo ti_map = new TypeInfo(new XMLInfo(new QName[]{new QName(SXML.PROTOCOL_TYPEINFO+"java.util", "Map")}),
				new ObjectInfo(Map.class), new MappingInfo(null, new SubobjectInfo[]{
				new SubobjectInfo(new XMLInfo("entry"), new AccessInfo("entry", null, null, null,  
					new BeanAccessInfo(Map.class.getMethod("put", new Class[]{Object.class, Object.class}), null, "", MapEntry.class.getMethod("getKey", new Class[0]))), 
				new SubObjectConverter(entryconv, null), true, null)
			}));
			typeinfos.add(ti_map);
			
			TypeInfo ti_mapentry = new TypeInfo(new XMLInfo("entry"), new ObjectInfo(MapEntry.class),
				new MappingInfo(null, new SubobjectInfo[]{
				new SubobjectInfo(new AccessInfo("key")),
				new SubobjectInfo(new AccessInfo("value"))
			}));
			typeinfos.add(ti_mapentry);
			
			// jadex.commons.collection.MultiCollection
			TypeInfo ti_mc = new TypeInfo(new XMLInfo(new QName[]{new QName(SXML.PROTOCOL_TYPEINFO+"jadex.commons.collection", "MultiCollection")}),
				new ObjectInfo(MultiCollection.class), new MappingInfo(null, new SubobjectInfo[]{
				new SubobjectInfo(new XMLInfo("entry"), new AccessInfo("entry", null, null, null,  
					new BeanAccessInfo(MultiCollection.class.getMethod("putCollection", new Class[]{Object.class, Collection.class}), null, "", MapEntry.class.getMethod("getKey", new Class[0]))), 
				new SubObjectConverter(entryconv, null), true, null)
			}));
			typeinfos.add(ti_mc);
			
			// java.util.List
			TypeInfo ti_list = new TypeInfo(new XMLInfo(new QName[]{new QName(SXML.PROTOCOL_TYPEINFO+"java.util", "List")}),
				new ObjectInfo(List.class), new MappingInfo(null, new SubobjectInfo[]{
				new SubobjectInfo(new AccessInfo("entries", null, null, null, 
				new BeanAccessInfo(List.class.getMethod("add", new Class[]{Object.class}), null)))
			}));
			typeinfos.add(ti_list);
			
			// java.util.Set
			TypeInfo ti_set = new TypeInfo(new XMLInfo(new QName[]{new QName(SXML.PROTOCOL_TYPEINFO+"java.util", "Set")}),
				new ObjectInfo(Set.class), new MappingInfo(null, new SubobjectInfo[]{
				new SubobjectInfo(new AccessInfo("entries", null, null, null,
				new BeanAccessInfo(Set.class.getMethod("add", new Class[]{Object.class}), null)))
			}));
			typeinfos.add(ti_set);
			
			// java.util.EmptySet
			TypeInfo ti_emptyset = new TypeInfo(new XMLInfo(new QName[]{new QName(SXML.PROTOCOL_TYPEINFO+"java.util", "Collections-EmptySet")}),
				new ObjectInfo(new IBeanObjectCreator()
				{
					public Object createObject(IContext context, Map rawattributes) throws Exception
					{
						return Collections.EMPTY_SET;
					}
				}
			));
			typeinfos.add(ti_emptyset);
			
			// java.util.EmptyList
			TypeInfo ti_emptylist = new TypeInfo(new XMLInfo(new QName[]{new QName(SXML.PROTOCOL_TYPEINFO+"java.util", "Collections-EmptyList")}),
				new ObjectInfo(new IBeanObjectCreator()
				{
					public Object createObject(IContext context, Map rawattributes) throws Exception
					{
						return Collections.EMPTY_LIST;
					}
				}
			));
			typeinfos.add(ti_emptylist);
			
			// java.util.EmptyMap
			TypeInfo ti_emptymap = new TypeInfo(new XMLInfo(new QName[]{new QName(SXML.PROTOCOL_TYPEINFO+"java.util", "Collections-EmptyMap")}),
				new ObjectInfo(new IBeanObjectCreator()
				{
					public Object createObject(IContext context, Map rawattributes) throws Exception
					{
						return Collections.EMPTY_MAP;
					}
				}
			));
			typeinfos.add(ti_emptymap);
			
			// java.util.UnmodifyableSet
			TypeInfo ti_unset = new TypeInfo(new XMLInfo(new QName[]{new QName(SXML.PROTOCOL_TYPEINFO+"java.util", "Collections-UnmodifiableSet")}),
				new ObjectInfo(HashSet.class), new MappingInfo(null, new SubobjectInfo[]{
				new SubobjectInfo(new AccessInfo("entries", null, null, null,
				new BeanAccessInfo(Set.class.getMethod("add", new Class[]{Object.class}), null)))
			}));
			typeinfos.add(ti_unset);
				
			// java.util.UnmodifyableList
			TypeInfo ti_unlist = new TypeInfo(new XMLInfo(new QName[]{new QName(SXML.PROTOCOL_TYPEINFO+"java.util", "Collections-UnmodifiableList")}),
				new ObjectInfo(ArrayList.class), new MappingInfo(null, new SubobjectInfo[]{
				new SubobjectInfo(new AccessInfo("entries", null, null, null, 
				new BeanAccessInfo(List.class.getMethod("add", new Class[]{Object.class}), null)))
			}));
			typeinfos.add(ti_unlist);
			
			// java.util.UnmodifyableMap
			TypeInfo ti_unmap = new TypeInfo(new XMLInfo(new QName[]{new QName(SXML.PROTOCOL_TYPEINFO+"java.util", "Collections-UnmodifiableMap")}),
				new ObjectInfo(HashMap.class), new MappingInfo(null, new SubobjectInfo[]{
				new SubobjectInfo(new XMLInfo("entry"), new AccessInfo("entry", null, null, null,  
					new BeanAccessInfo(Map.class.getMethod("put", new Class[]{Object.class, Object.class}), null, "", MapEntry.class.getMethod("getKey", new Class[0]))), 
				new SubObjectConverter(entryconv, null), true, null)
			}));
			typeinfos.add(ti_unmap);
			
			// java.util.Color
			IStringObjectConverter coconv = new IStringObjectConverter()
			{
				public Object convertString(String val, IContext context)
				{
					return Color.decode(val);
				}
			};
			
			TypeInfo ti_color = new TypeInfo(new XMLInfo(new QName[]{new QName(SXML.PROTOCOL_TYPEINFO+"java.awt", "Color")}), 
				null, new MappingInfo(null, null, new AttributeInfo(new AccessInfo((String)null, AccessInfo.THIS), new AttributeConverter(coconv, null))));
			typeinfos.add(ti_color);
			
			TypeInfo ti_class = new TypeInfo(new XMLInfo(new QName[]{new QName(SXML.PROTOCOL_TYPEINFO+"java.lang", "Class")}),
				new ObjectInfo(new IBeanObjectCreator()
				{
					public Object createObject(IContext context, Map rawattributes) throws Exception
					{
						return SReflect.findClass((String)rawattributes.get("classname"), null, context.getClassLoader());
					}
				}),
				new MappingInfo(null, new AttributeInfo[]{new AttributeInfo(new AccessInfo("classname", null, AccessInfo.IGNORE_READWRITE))}
			));
			typeinfos.add(ti_class);
			
			// java.util.Date
			// No special read info necessary.
			
			// java.lang.String
			TypeInfo ti_string = new TypeInfo(new XMLInfo(new QName[]{new QName(SXML.PROTOCOL_TYPEINFO+"java.lang", "String")}),
				new ObjectInfo(null, new IPostProcessor()
				{
					public Object postProcess(IContext context, Object object)
					{
//						System.err.println("postprocess: "+object);
						return object!=null ? object : "";
					}
					
					public int getPass()
					{
						return 0;
					}
				})
//				new ObjectInfo(new IBeanObjectCreator()
//				{
//					public Object createObject(IContext context, Map rawattributes) throws Exception
//					{
//						return "";//(String)rawattributes.get("content");
//					}
//				}),
//				new MappingInfo(null, null, new AttributeInfo(new AccessInfo(AccessInfo.THIS)))
//				new MappingInfo(null, new AttributeInfo[]{new AttributeInfo(new AccessInfo("content", null, AccessInfo.IGNORE_READWRITE))}
//			));
			);
			typeinfos.add(ti_string);
			
			// Bean creator works only when all info is in attribues (content is not available).
			// If content is used a attribute converter for content should be used that maps back to THIS
			
			// java.lang.Boolean
			TypeInfo ti_boolean = new TypeInfo(new XMLInfo(new QName[]{new QName(SXML.PROTOCOL_TYPEINFO+"java.lang", "Boolean")}),
				new ObjectInfo(new IBeanObjectCreator()
				{
					public Object createObject(IContext context, Map rawattributes) throws Exception
					{
						return new Boolean((String)rawattributes.get("content"));
					}
				}),
				new MappingInfo(null, new AttributeInfo[]{new AttributeInfo(new AccessInfo("content", null, AccessInfo.IGNORE_READWRITE))}
			));
			typeinfos.add(ti_boolean);
			
			// java.lang.Integer
			TypeInfo ti_integer = new TypeInfo(new XMLInfo(new QName[]{new QName(SXML.PROTOCOL_TYPEINFO+"java.lang", "Integer")}),
				new ObjectInfo(new IBeanObjectCreator()
				{
					public Object createObject(IContext context, Map rawattributes) throws Exception
					{
						return new Integer((String)rawattributes.get("content"));
					}
				}),
				new MappingInfo(null, new AttributeInfo[]{new AttributeInfo(new AccessInfo("content", null, AccessInfo.IGNORE_READWRITE))}
			));
			typeinfos.add(ti_integer);
			
			// java.lang.Double
			TypeInfo ti_double = new TypeInfo(new XMLInfo(new QName[]{new QName(SXML.PROTOCOL_TYPEINFO+"java.lang", "Double")}),
				new ObjectInfo(new IBeanObjectCreator()
				{
					public Object createObject(IContext context, Map rawattributes) throws Exception
					{
						return new Double((String)rawattributes.get("content"));
					}
				}),
				new MappingInfo(null, new AttributeInfo[]{new AttributeInfo(new AccessInfo("content", null, AccessInfo.IGNORE_READWRITE))}
			));
			typeinfos.add(ti_double);
			
			// java.lang.Float
			TypeInfo ti_float = new TypeInfo(new XMLInfo(new QName[]{new QName(SXML.PROTOCOL_TYPEINFO+"java.lang", "Float")}),
				new ObjectInfo(new IBeanObjectCreator()
				{
					public Object createObject(IContext context, Map rawattributes) throws Exception
					{
						return new Float((String)rawattributes.get("content"));
					}
				}),
				new MappingInfo(null, new AttributeInfo[]{new AttributeInfo(new AccessInfo("content", null, AccessInfo.IGNORE_READWRITE))}
			));
			typeinfos.add(ti_float);
			
			// java.lang.Long
			TypeInfo ti_long = new TypeInfo(new XMLInfo(new QName[]{new QName(SXML.PROTOCOL_TYPEINFO+"java.lang", "Long")}),
				new ObjectInfo(new IBeanObjectCreator()
				{
					public Object createObject(IContext context, Map rawattributes) throws Exception
					{
						return new Long((String)rawattributes.get("content"));
					}
				}),
				new MappingInfo(null, new AttributeInfo[]{new AttributeInfo(new AccessInfo("content", null, AccessInfo.IGNORE_READWRITE))}
			));
			typeinfos.add(ti_long);
			
			// java.lang.Short
			TypeInfo ti_short = new TypeInfo(new XMLInfo(new QName[]{new QName(SXML.PROTOCOL_TYPEINFO+"java.lang", "Short")}),
				new ObjectInfo(new IBeanObjectCreator()
				{
					public Object createObject(IContext context, Map rawattributes) throws Exception
					{
						return new Short((String)rawattributes.get("content"));
					}
				}),
				new MappingInfo(null, new AttributeInfo[]{new AttributeInfo(new AccessInfo("content", null, AccessInfo.IGNORE_READWRITE))}
			));
			typeinfos.add(ti_short);
			
			// java.lang.Byte
			TypeInfo ti_byte = new TypeInfo(new XMLInfo(new QName[]{new QName(SXML.PROTOCOL_TYPEINFO+"java.lang", "Byte")}),
				new ObjectInfo(new IBeanObjectCreator()
				{
					public Object createObject(IContext context, Map rawattributes) throws Exception
					{
						return new Byte((String)rawattributes.get("content"));
					}
				}),
				new MappingInfo(null, new AttributeInfo[]{new AttributeInfo(new AccessInfo("content", null, AccessInfo.IGNORE_READWRITE))}
			));
			typeinfos.add(ti_byte);
			
			// java.lang.Character
			TypeInfo ti_character = new TypeInfo(new XMLInfo(new QName[]{new QName(SXML.PROTOCOL_TYPEINFO+"java.lang", "Character")}),
				new ObjectInfo(new IBeanObjectCreator()
				{
					public Object createObject(IContext context, Map rawattributes) throws Exception
					{
						return new Character(((String)rawattributes.get("content")).charAt(0));
					}
				}),
				new MappingInfo(null, new AttributeInfo[]{new AttributeInfo(new AccessInfo("content", null, AccessInfo.IGNORE_READWRITE))}
			));
			typeinfos.add(ti_character);

			// java.net.URL
			TypeInfo ti_url = new TypeInfo(new XMLInfo(new QName[]{new QName(SXML.PROTOCOL_TYPEINFO+"java.net", "URL")}),
				new ObjectInfo(new IBeanObjectCreator()
				{
					public Object createObject(IContext context, Map rawattributes) throws Exception
					{
						return new URL((String)rawattributes.get("protocol"), (String)rawattributes.get("host"), 
							new Integer((String)rawattributes.get("port")).intValue(), (String)rawattributes.get("file"));
					}
				}),
				new MappingInfo(null, new AttributeInfo[]{
					new AttributeInfo(new AccessInfo("protocol", null, AccessInfo.IGNORE_READWRITE)),
					new AttributeInfo(new AccessInfo("host", null, AccessInfo.IGNORE_READWRITE)),
					new AttributeInfo(new AccessInfo("port", null, AccessInfo.IGNORE_READWRITE)),
					new AttributeInfo(new AccessInfo("file", null, AccessInfo.IGNORE_READWRITE)),
				}
			));
			typeinfos.add(ti_url);
			
			// java.logging.Level
			TypeInfo ti_level = new TypeInfo(new XMLInfo(new QName[]{new QName(SXML.PROTOCOL_TYPEINFO+"java.util.logging", "Level")}),
				new ObjectInfo(new IBeanObjectCreator()
				{
					public Object createObject(IContext context, Map rawattributes) throws Exception
					{
						String name = (String)rawattributes.get("name");
						Level ret = Level.parse(name);
						return ret;
					}
				}),
				new MappingInfo(null, new AttributeInfo[]{
					new AttributeInfo(new AccessInfo("name", null, AccessInfo.IGNORE_READWRITE))}
			));
			typeinfos.add(ti_level);
			
			// java.net.InetAddress
			TypeInfo ti_inetaddr = new TypeInfo(new XMLInfo(new QName[]{new QName(SXML.PROTOCOL_TYPEINFO+"java.net", "InetAddress")}),
				new ObjectInfo(new IBeanObjectCreator()
				{
					public Object createObject(IContext context, Map rawattributes) throws Exception
					{
						return InetAddress.getByName((String)rawattributes.get("hostAddress"));
					}
				}),
				new MappingInfo(null, new AttributeInfo[]{
					new AttributeInfo(new AccessInfo("hostAddress", null, AccessInfo.IGNORE_READWRITE)),
				}
			));
			typeinfos.add(ti_inetaddr);
			
			// Image
			TypeInfo ti_image = new TypeInfo(new XMLInfo(new QName[]{new QName(SXML.PROTOCOL_TYPEINFO+"java.awt.image", "Image")}),
				new ObjectInfo(new IBeanObjectCreator()
				{
					public Object createObject(IContext context, Map rawattributes) throws Exception
					{
						Image ret = null;
						String encdata = (String)rawattributes.get("imgdata");
						byte[] data = Base64.decode(encdata.getBytes());
						
						String classname = (String)rawattributes.get("classname");
						if(classname.indexOf("Toolkit")!=-1)
						{
							Toolkit t = Toolkit.getDefaultToolkit();
							ret = t.createImage(data);
						}
						else
						{
							ret = ImageIO.read(new ByteArrayInputStream(data));
						}
						return ret;
					}
				}),
				new MappingInfo(null, new AttributeInfo[]{
					new AttributeInfo(new AccessInfo("imgdata", null, AccessInfo.IGNORE_READWRITE)),
					new AttributeInfo(new AccessInfo("classname", null, AccessInfo.IGNORE_READWRITE))
				}
			));
			typeinfos.add(ti_image);
			
			// Shortcut notations for simple array types
			
			// boolean/Boolean Array
			IStringObjectConverter booleanconv = new IStringObjectConverter()
			{
				public Object convertString(String val, IContext context)
				{
					boolean[] ret = new boolean[val.length()];
					for(int i=0; i<val.length(); i++)
						ret[i] = val.charAt(i)=='1'? true: false;
					return ret;
				}
			};
			TypeInfo ti_booleanarray = new TypeInfo(new XMLInfo(new QName[]{new QName(SXML.PROTOCOL_TYPEINFO, "boolean__1")}), null,
				new MappingInfo(null, null, new AttributeInfo(new AccessInfo((String)null, AccessInfo.THIS), new AttributeConverter(booleanconv, null))));
			typeinfos.add(ti_booleanarray);
			
			IStringObjectConverter bbooleanconv = new IStringObjectConverter()
			{
				public Object convertString(String val, IContext context)
				{
					Boolean[] ret = new Boolean[val.length()];
					for(int i=0; i<val.length(); i++)
						ret[i] = val.charAt(i)=='1'? Boolean.TRUE: Boolean.FALSE;
					return ret;
				}
			};
			TypeInfo ti_bbooleanarray = new TypeInfo(new XMLInfo(new QName[]{new QName(SXML.PROTOCOL_TYPEINFO+"java.lang", "Boolean__1")}), null,
				new MappingInfo(null, null, new AttributeInfo(new AccessInfo((String)null, AccessInfo.THIS), new AttributeConverter(bbooleanconv, null))));
			typeinfos.add(ti_bbooleanarray);
			
			// int/Integer Array
			IStringObjectConverter intconv = new IStringObjectConverter()
			{
				public Object convertString(String val, IContext context)
				{
					StringTokenizer stok = new StringTokenizer(val, ",");
					int len = Integer.parseInt(stok.nextToken());
					int[] ret = new int[len];
					
					for(int i=0; stok.hasMoreTokens(); i++)
						ret[i] = Integer.parseInt(stok.nextToken());
					return ret;
				}
			};
			TypeInfo ti_intarray = new TypeInfo(new XMLInfo(new QName[]{new QName(SXML.PROTOCOL_TYPEINFO, "int__1")}), null,
				new MappingInfo(null, null, new AttributeInfo(new AccessInfo((String)null, AccessInfo.THIS), new AttributeConverter(intconv, null))));
			typeinfos.add(ti_intarray);
			
			IStringObjectConverter integerconv = new IStringObjectConverter()
			{
				public Object convertString(String val, IContext context)
				{
					StringTokenizer stok = new StringTokenizer(val, ",");
					int len = Integer.parseInt(stok.nextToken());
					Integer[] ret = new Integer[len];
					
					for(int i=0; stok.hasMoreTokens(); i++)
						ret[i] = new Integer(stok.nextToken());
					return ret;
				}
			};
			TypeInfo ti_integerarray = new TypeInfo(new XMLInfo(new QName[]{new QName(SXML.PROTOCOL_TYPEINFO+"java.lang", "Integer__1")}), null,
				new MappingInfo(null, null, new AttributeInfo(new AccessInfo((String)null, AccessInfo.THIS), new AttributeConverter(integerconv, null))));
			typeinfos.add(ti_integerarray);
			
			// double/Double array
			IStringObjectConverter doubleconv = new IStringObjectConverter()
			{
				public Object convertString(String val, IContext context)
				{
					StringTokenizer stok = new StringTokenizer(val, "_");
					int len = Integer.parseInt(stok.nextToken());
					double[] ret = new double[len];
					
					for(int i=0; stok.hasMoreTokens(); i++)
						ret[i] = Double.parseDouble(stok.nextToken());
					return ret;
				}
			};
			TypeInfo ti_doublearray = new TypeInfo(new XMLInfo(new QName[]{new QName(SXML.PROTOCOL_TYPEINFO, "double__1")}), null,
				new MappingInfo(null, null, new AttributeInfo(new AccessInfo((String)null, AccessInfo.THIS), new AttributeConverter(doubleconv, null))));
			typeinfos.add(ti_doublearray);
			
			IStringObjectConverter bdoubleconv = new IStringObjectConverter()
			{
				public Object convertString(String val, IContext context)
				{
					StringTokenizer stok = new StringTokenizer(val, "_");
					int len = Integer.parseInt(stok.nextToken());
					Double[] ret = new Double[len];
					
					for(int i=0; stok.hasMoreTokens(); i++)
						ret[i] = new Double(stok.nextToken());
					return ret;
				}
			};
			TypeInfo ti_bdoublerarray = new TypeInfo(new XMLInfo(new QName[]{new QName(SXML.PROTOCOL_TYPEINFO+"java.lang", "Double__1")}), null,
				new MappingInfo(null, null, new AttributeInfo(new AccessInfo((String)null, AccessInfo.THIS), new AttributeConverter(bdoubleconv, null))));
			typeinfos.add(ti_bdoublerarray);
			
			// java.lang.Float
			IStringObjectConverter floatconv = new IStringObjectConverter()
			{
				public Object convertString(String val, IContext context)
				{
					StringTokenizer stok = new StringTokenizer(val, "_");
					int len = Integer.parseInt(stok.nextToken());
					float[] ret = new float[len];
					
					for(int i=0; stok.hasMoreTokens(); i++)
						ret[i] = Float.parseFloat(stok.nextToken());
					return ret;
				}
			};
			TypeInfo ti_floatarray = new TypeInfo(new XMLInfo(new QName[]{new QName(SXML.PROTOCOL_TYPEINFO, "float__1")}), null,
				new MappingInfo(null, null, new AttributeInfo(new AccessInfo((String)null, AccessInfo.THIS), new AttributeConverter(floatconv, null))));
			typeinfos.add(ti_floatarray);
			
			IStringObjectConverter bfloatconv = new IStringObjectConverter()
			{
				public Object convertString(String val, IContext context)
				{
					StringTokenizer stok = new StringTokenizer(val, "_");
					int len = Integer.parseInt(stok.nextToken());
					Float[] ret = new Float[len];
					
					for(int i=0; stok.hasMoreTokens(); i++)
						ret[i] = new Float(stok.nextToken());
					return ret;
				}
			};
			TypeInfo ti_bfloatarray = new TypeInfo(new XMLInfo(new QName[]{new QName(SXML.PROTOCOL_TYPEINFO+"java.lang", "Float__1")}), null,
				new MappingInfo(null, null, new AttributeInfo(new AccessInfo((String)null, AccessInfo.THIS), new AttributeConverter(bfloatconv, null))));
			typeinfos.add(ti_bfloatarray);
			
			// java.lang.Long
			IStringObjectConverter longconv = new IStringObjectConverter()
			{
				public Object convertString(String val, IContext context)
				{
					StringTokenizer stok = new StringTokenizer(val, ",");
					int len = Integer.parseInt(stok.nextToken());
					long[] ret = new long[len];
					
					for(int i=0; stok.hasMoreTokens(); i++)
						ret[i] = Long.parseLong(stok.nextToken());
					return ret;
				}
			};
			TypeInfo ti_longarray = new TypeInfo(new XMLInfo(new QName[]{new QName(SXML.PROTOCOL_TYPEINFO, "long__1")}), null,
				new MappingInfo(null, null, new AttributeInfo(new AccessInfo((String)null, AccessInfo.THIS), new AttributeConverter(longconv, null))));
			typeinfos.add(ti_longarray);
			
			IStringObjectConverter blongconv = new IStringObjectConverter()
			{
				public Object convertString(String val, IContext context)
				{
					StringTokenizer stok = new StringTokenizer(val, ",");
					int len = Integer.parseInt(stok.nextToken());
					Long[] ret = new Long[len];
					
					for(int i=0; stok.hasMoreTokens(); i++)
						ret[i] = new Long(stok.nextToken());
					return ret;
				}
			};
			TypeInfo ti_blongarray = new TypeInfo(new XMLInfo(new QName[]{new QName(SXML.PROTOCOL_TYPEINFO+"java.lang", "Long__1")}), null,
				new MappingInfo(null, null, new AttributeInfo(new AccessInfo((String)null, AccessInfo.THIS), new AttributeConverter(blongconv, null))));
			typeinfos.add(ti_blongarray);
			
			// short/Short Array
			IStringObjectConverter shortconv = new IStringObjectConverter()
			{
				public Object convertString(String val, IContext context)
				{
					StringTokenizer stok = new StringTokenizer(val, ",");
					int len = Integer.parseInt(stok.nextToken());
					short[] ret = new short[len];
					
					for(int i=0; stok.hasMoreTokens(); i++)
						ret[i] = Short.parseShort(stok.nextToken());
					return ret;
				}
			};
			TypeInfo ti_shortarray = new TypeInfo(new XMLInfo(new QName[]{new QName(SXML.PROTOCOL_TYPEINFO, "short__1")}), null,
				new MappingInfo(null, null, new AttributeInfo(new AccessInfo((String)null, AccessInfo.THIS), new AttributeConverter(shortconv, null))));
			typeinfos.add(ti_shortarray);
			
			IStringObjectConverter bshortconv = new IStringObjectConverter()
			{
				public Object convertString(String val, IContext context)
				{
					StringTokenizer stok = new StringTokenizer(val, ",");
					int len = Integer.parseInt(stok.nextToken());
					Short[] ret = new Short[len];
					
					for(int i=0; stok.hasMoreTokens(); i++)
						ret[i] = new Short(stok.nextToken());
					return ret;
				}
			};
			TypeInfo ti_bshortarray = new TypeInfo(new XMLInfo(new QName[]{new QName(SXML.PROTOCOL_TYPEINFO+"java.lang", "Short__1")}), null,
				new MappingInfo(null, null, new AttributeInfo(new AccessInfo((String)null, AccessInfo.THIS), new AttributeConverter(bshortconv, null))));
			typeinfos.add(ti_bshortarray);
			
			// byte/Byte Array
			IStringObjectConverter byteconv = new IStringObjectConverter()
			{
				public Object convertString(String val, IContext context)
				{
					return val.getBytes();
				}
			};
			TypeInfo ti_bytearray = new TypeInfo(new XMLInfo(new QName[]{new QName(SXML.PROTOCOL_TYPEINFO, "byte__1")}), null,
				new MappingInfo(null, null, new AttributeInfo(new AccessInfo((String)null, AccessInfo.THIS), new AttributeConverter(byteconv, null))));
			typeinfos.add(ti_bytearray);
			
			IStringObjectConverter bbyteconv = new IStringObjectConverter()
			{
				public Object convertString(String val, IContext context)
				{
					byte[] bytes = val.getBytes();
					Byte[] bbytes = new Byte[bytes.length];
					for(int i=0; i<bytes.length; i++)
						bbytes[i] = new Byte(bytes[i]);
					return bbytes;
				}
			};
			TypeInfo ti_bbytearray = new TypeInfo(new XMLInfo(new QName[]{new QName(SXML.PROTOCOL_TYPEINFO+"java.lang", "Byte__1")}), null,
				new MappingInfo(null, null, new AttributeInfo(new AccessInfo((String)null, AccessInfo.THIS), new AttributeConverter(bbyteconv, null))));
			typeinfos.add(ti_bbytearray);
			
			// java.lang.Character
			IStringObjectConverter charconv = new IStringObjectConverter()
			{
				public Object convertString(String val, IContext context)
				{
					return val.toCharArray();
				}
			};
			TypeInfo ti_chararray = new TypeInfo(new XMLInfo(new QName[]{new QName(SXML.PROTOCOL_TYPEINFO, "char__1")}), null,
				new MappingInfo(null, null, new AttributeInfo(new AccessInfo((String)null, AccessInfo.THIS), new AttributeConverter(charconv, null))));
			typeinfos.add(ti_chararray);
			
			IStringObjectConverter characterconv = new IStringObjectConverter()
			{
				public Object convertString(String val, IContext context)
				{
					char[] chars = val.toCharArray();
					Character[] bchars = new Character[chars.length];
					for(int i=0; i<chars.length; i++)
						bchars[i] = new Character(chars[i]);
					return bchars;
				}
			};
			TypeInfo ti_characterarray = new TypeInfo(new XMLInfo(new QName[]{new QName(SXML.PROTOCOL_TYPEINFO+"java.lang", "Character__1")}), null,
				new MappingInfo(null, null, new AttributeInfo(new AccessInfo((String)null, AccessInfo.THIS), new AttributeConverter(characterconv, null))));
			typeinfos.add(ti_characterarray);
		}
		catch(NoSuchMethodException e)
		{
			// Shouldn't happen
			throw new RuntimeException(e);
		}
		return typeinfos;
	}
	
	/**
	 *  Convert an xml to an object.
	 *  @param val The string value.
	 *  @return The decoded object.
	 */
	public static Object objectFromXML(String val, ClassLoader classloader)
	{
		return Reader.objectFromXML(getInstance(), val, classloader);
	}
	
	/**
	 *  Convert a byte array (of an xml) to an object.
	 *  @param val The byte array.
	 *  @param classloader The class loader.
	 *  @return The decoded object.
	 */
	public static Object objectFromByteArray(byte[] val, ClassLoader classloader)
	{
		return Reader.objectFromByteArray(getInstance(), val, classloader);
	}
	
	/**
	 *  Get the default Java reader.
	 *  @return The Java reader.
	 */
	public static Reader getInstance()
	{
		if(reader==null)
			createReader();
		return reader;
	}
	
	/**
	 *  Get the default Java reader.
	 *  @return The Java reader.
	 */
	public static Reader	getReader(XMLReporter reporter)
	{
		Set	typeinfos	= getTypeInfos();
		return new Reader(new TypeInfoPathManager(typeinfos), false, false, false, reporter, new BeanObjectReaderHandler(typeinfos));
	}
	
	/**
	 *  Conditionally create the reader instance.
	 *  Note that the synchronized check needs to be done.
	 *  Otherwise double creation may happen (leading to
	 *  concurrency issues).
	 */
	protected static synchronized void createReader()
	{
		if(reader==null)
		{
			Set	typeinfos	= getTypeInfos();
			reader	= new Reader(new TypeInfoPathManager(typeinfos), false, false, false, null, new BeanObjectReaderHandler(typeinfos));
		}
	}
}
