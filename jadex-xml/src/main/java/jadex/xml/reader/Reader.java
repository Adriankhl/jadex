package jadex.xml.reader;

import jadex.commons.SUtil;
import jadex.xml.AttributeInfo;
import jadex.xml.IPostProcessor;
import jadex.xml.IStringObjectConverter;
import jadex.xml.SXML;
import jadex.xml.StackElement;
import jadex.xml.SubobjectInfo;
import jadex.xml.TypeInfo;
import jadex.xml.TypeInfoPathManager;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/* $if !android $ */
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLReporter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
/* $else $
import javaxx.xml.XMLConstants;
import javaxx.xml.namespace.QName;
import javaxx.xml.stream.Location;
import javaxx.xml.stream.XMLInputFactory;
import javaxx.xml.stream.XMLReporter;
import javaxx.xml.stream.XMLStreamException;
import javaxx.xml.stream.XMLStreamReader;
$endif $ */

/**
 *  Stax XML reader.
 */
public class Reader
{
	//-------- constants --------
	
	/** The debug flag. */
	public static boolean DEBUG = false;
	
	/** The string marker object. */
	public static final Object STRING_MARKER = new String();

	/** This thread local variable provides access to the read context,
	 *  e.g. from the XML reporter, if required. */
	public static final ThreadLocal	READ_CONTEXT	= new ThreadLocal();
	
	//-------- attributes --------
	
	/** The type info manager. */
	protected TypeInfoPathManager tipmanager;
	
	/** The default object reader handler (if any). */
	protected IObjectReaderHandler defaulthandler;
	
	/** The link mode. */
	protected boolean bulklink;
	
	/** The xml input factory. */
	protected XMLInputFactory	factory;
	
	//-------- constructors --------

	/**
	 *  Create a new reader.
	 *  @param handler The handler.
	 */
	public Reader(TypeInfoPathManager tipmanager)
	{
		this(tipmanager, false);
	}
	
	/**
	 *  Create a new reader.
	 *  @param handler The handler.
	 */
	public Reader(TypeInfoPathManager tipmanager, boolean bulklink)
	{
		this(tipmanager, bulklink, false, null);
	}
	
	/**
	 *  Create a new reader.
	 *  @param handler The handler.
	 */
	public Reader(TypeInfoPathManager tipmanager, boolean bulklink, boolean validate, XMLReporter reporter)
	{
		// Xerces has a stackoverflow bug when coalescing is set to true :-(
		this(tipmanager, bulklink, validate, false, reporter, null);
	}
	
	/**
	 *  Create a new reader.
	 *  @param handler The handler.
	 */
	public Reader(TypeInfoPathManager tipmanager, boolean bulklink, boolean validate, boolean coalescing, XMLReporter reporter, IObjectReaderHandler defaulthandler)
	{
		this.tipmanager = tipmanager;
		this.bulklink = bulklink;
		this.defaulthandler = defaulthandler;
		factory	= XMLInputFactory.newInstance();
		
		try
		{
			factory.setProperty(XMLInputFactory.IS_VALIDATING, validate ? Boolean.TRUE : Boolean.FALSE);
		}
		catch(Exception e)
		{
			// Validation not supported.
			System.err.println("Error setting validation to "+validate);
//			e.printStackTrace();
		}
		
		try
		{
			factory.setProperty(XMLInputFactory.IS_COALESCING, coalescing ? Boolean.TRUE : Boolean.FALSE);
		}
		catch(Exception e)
		{
			// Validation not supported.
			System.err.println("Error setting coalescing to "+coalescing);
//			e.printStackTrace();
		}
		
		if(reporter!=null)
		{
			factory.setProperty(XMLInputFactory.REPORTER, reporter);
		}
		else
		{
			factory.setProperty(XMLInputFactory.REPORTER, new XMLReporter()
			{
				public void report(String message, String error, Object related, Location location)	throws XMLStreamException
				{
					throw new XMLStreamException(message, location);
				}
			});			
		}
	}
	
	//-------- methods --------
	
	/**
	 *  Read properties from xml.
	 *  @param input The input stream.
	 *  @param classloader The classloader.
	 * 	@param context The context.
 	 */
	public Object read(java.io.Reader input, final ClassLoader classloader, final Object callcontext) throws Exception
	{
		XMLStreamReader	parser;
		synchronized(factory)
		{
			parser	= factory.createXMLStreamReader(input);
		}
		return read(parser, classloader, callcontext);
	}
	
	/**
	 *  Read properties from xml.
	 *  @param input The input stream.
	 *  @param classloader The classloader.
	 * 	@param context The context.
 	 */
	public Object read(InputStream input, final ClassLoader classloader, final Object callcontext) throws Exception
	{
		XMLStreamReader	parser;
		synchronized(factory)
		{
			parser	= factory.createXMLStreamReader(input);
		}
		return read(parser, classloader, callcontext);
	}

	/**
	 *  Read properties from xml.
	 *  @param input The input stream.
	 *  @param classloader The classloader.
	 * 	@param context The context.
 	 */
	public Object read(XMLStreamReader parser, final ClassLoader classloader, final Object callcontext) throws Exception
	{
		ReadContext readcontext = new ReadContext(parser, factory.getXMLReporter(), callcontext, classloader);
		READ_CONTEXT.set(readcontext);
		try
		{
			while(parser.hasNext())
			{
				int	next = parser.next();
				
				if(next==XMLStreamReader.COMMENT)
				{
					handleComment(readcontext);
				}
				else if(next==XMLStreamReader.CHARACTERS || next==XMLStreamReader.CDATA)
				{
					handleContent(readcontext);
				}
				else if(next==XMLStreamReader.START_ELEMENT)
				{	
					handleStartElement(readcontext);
				}
				else if(next==XMLStreamReader.END_ELEMENT)
				{
					handleEndElement(readcontext);
				}
			}
			
			// Handle post-processors.
			for(int i=1; readcontext.getPostProcessors().size()>0; i++)
			{
				List ps = (List)readcontext.getPostProcessors().remove(new Integer(i));
				if(ps!=null)
				{
					for(int j=0; j<ps.size(); j++)
					{
						((IPostProcessorCall)ps.get(j)).callPostProcessor();
					}
				}
	//			System.out.println("i: "+i);
			}
		}
		catch(RuntimeException e)
		{
//			e.printStackTrace();
			Location	loc	= readcontext.getStackSize()>0 ? readcontext.getTopStackElement().getLocation() : parser.getLocation();
			readcontext.getReporter().report(e.toString(), "XML error", readcontext, loc);
		}
		finally
		{
			READ_CONTEXT.set(null);
			parser.close();
		}

		return readcontext.getRootObject();
	}
	
	/**
	 *  Handle the comment.
	 *  @param readcontext The context for reading with all necessary information.
	 */
	protected void handleComment(ReadContext readcontext) throws Exception
	{
		if(readcontext.getReadIgnore()==0)
			readcontext.setComment(readcontext.getParser().getText());
		else if(DEBUG)
			System.out.println("Ignoring: "+readcontext.getParser().getText());
		//	System.out.println("Found comment: "+comment);
	}

	/**
	 *  Handle the content.
	 *  @param readcontext The context for reading with all necessary information.
	 */
	protected void handleContent(ReadContext readcontext) throws Exception
	{
		if(readcontext.getReadIgnore()==0)
			readcontext.getTopStackElement().addContent(readcontext.getParser().getText()); 
		else if(DEBUG)
			System.out.println("Ignoring: "+readcontext.getParser().getText());
//		System.out.println("content: "+parser.getLocalName()+" "+content);
	}
	
	// For debugging: ReadContext -> Integer.
//	private static Map	stackdepth	= Collections.synchronizedMap(new HashMap());
	
	/**
	 *  Handle the start element.
	 *  @param readcontext The context for reading with all necessary information.
	 */
	protected void handleStartElement(ReadContext readcontext) throws Exception
	{
		XMLStreamReader parser = readcontext.getParser();
		
		if(readcontext.getReadIgnore()>0)
		{
			readcontext.setReadIgnore(readcontext.getReadIgnore()+1);
			if(DEBUG)
				System.out.println("Ignoring: "+parser.getLocalName());
		}
		else if(readcontext.getReadIgnore()==0)
		{
//			List stack = readcontext.getStack();

			// Fetch for info when creating attributes.
			Map rawattrs = null;
			int attrcnt = parser.getAttributeCount();
			if(attrcnt>0)
			{
				rawattrs = new HashMap();
				for(int i=0; i<attrcnt; i++)
				{
					String attrname = parser.getAttributeLocalName(i);
					String attrval = parser.getAttributeValue(i);
					
					rawattrs.put(attrname, attrval);
				}
			}
			
			Object object = null;
			
//			System.out.println("here: "+parser.getPrefix()+" "+parser.getNamespaceURI()+" "+parser.getLocalName()+" "+parser.getName());
			QName localname = parser.getName();
			
//			QName localname = parser.getPrefix()==null || parser.getPrefix()==XMLConstants.DEFAULT_NS_PREFIX? new QName(parser.getLocalName())
//				: new QName(parser.getNamespaceURI(), parser.getLocalName(), parser.getPrefix());
			
			QName[] fullpath = readcontext.getXMLPath(localname);
			
			// Get type info and corresponding handler.
			TypeInfo typeinfo = tipmanager.getTypeInfo(localname, fullpath, rawattrs);
			IObjectReaderHandler	handler	= typeinfo!=null ? typeinfo.getReaderHandler() : null;
			if(handler==null)
			{
				if(readcontext.getTopStackElement()!=null && readcontext.getTopStackElement().getReaderHandler()!=null)
				{
					handler	= readcontext.getTopStackElement().getReaderHandler();
				}
				else if(defaulthandler!=null)
				{
					handler	= defaulthandler;
				}
				else
				{
					readcontext.getReporter().report("No handler for element: "+localname, "type info error", readcontext, parser.getLocation());
				}
			}
			
			// Find out if we need to ignore. 
			if(readcontext.getStackSize()>0)
			{
				StackElement pse = (StackElement)readcontext.getTopStackElement();
				List pathname = new ArrayList();
				pathname.add(localname);
				for(int i=readcontext.getStackSize()-2; i>=0 && pse.getObject()==null; i--)
				{
					pse = (StackElement)readcontext.getStackElement(i);
					pathname.add(0, readcontext.getStackElement(i+1).getTag());
				}
				
				if(pse!=null)
				{
					TypeInfo patypeinfo = ((StackElement)pse).getTypeInfo();
					SubobjectInfo linkinfo = getSubobjectInfoRead(localname, fullpath, patypeinfo, 
						readcontext.getTopStackElement()!=null? readcontext.getTopStackElement().getRawAttributes(): null);
					if(linkinfo!=null && linkinfo.getAccessInfo().isIgnoreRead())
					{
						readcontext.setReadIgnore(readcontext.getReadIgnore()+1);
						if(DEBUG)
							System.out.println("Ignoring: "+parser.getLocalName());
					}
				}
			}
			
			if(readcontext.getReadIgnore()==0)
			{
				// Test if it is an object reference
				String idref = rawattrs!=null? (String)rawattrs.get(SXML.IDREF): null;
				if(idref!=null)
				{
					if(readcontext.getReadObjects().containsKey(idref))
					{
						object = readcontext.getReadObjects().get(idref);
						StackElement se = new StackElement(handler, localname, object, rawattrs, typeinfo, parser.getLocation());
						readcontext.addStackElement(se);
					}
					else
					{
						StackElement se = new StackElement(handler, localname, null, rawattrs, typeinfo, parser.getLocation());
						readcontext.addStackElement(se);
						readcontext.getReporter().report("idref not contained: "+idref, "idref error", se, se.getLocation());						
					}
				}
				else
				{	
					// Create object.
					// todo: do not call createObject on every tag?!
					Object ti = typeinfo;
					if(localname.getNamespaceURI().startsWith(SXML.PROTOCOL_TYPEINFO)
						&& (typeinfo==null || typeinfo.isCreateFromTag()))
					{
						ti = localname;
					}
					
					try
					{
						object = handler.createObject(ti, readcontext.getStackSize()==0, readcontext, rawattrs);
					}
					catch(Exception e)
					{
//						e.printStackTrace();
						readcontext.getReporter().report(e.toString(), "creation error", readcontext, parser.getLocation());
					}
					
					if(DEBUG && object==null)
						System.out.println("No mapping found: "+localname);
					
					// Try to search type info via type (when tag contained type information)
					if(typeinfo==null && object!=null)
					{
						typeinfo = handler.getTypeInfo(object, fullpath, readcontext);
					}
					
					// If object has internal id save it in the readobjects map.
					String id = rawattrs!=null? (String)rawattrs.get(SXML.ID): null;
					if(id!=null && object!=null)
					{
//						System.out.println("ID: "+id+", "+object.getClass());
						readcontext.getReadObjects().put(id, object);
					}
					
					readcontext.addStackElement(new StackElement(handler, localname, object, rawattrs, typeinfo, parser.getLocation()));
				
					// Handle attributes.
					int atcnt = attrcnt;
					if(rawattrs!=null)
					{
						if(rawattrs.containsKey(SXML.ID))
							atcnt--;
						if(rawattrs.containsKey(SXML.ARRAYLEN))
							atcnt--;
					}
					if(atcnt>0)
					{
						List	attrpath	= null;
						// If no type use last element from stack to map attributes.
						if(object==null)	
						{
							attrpath = new ArrayList();
							attrpath.add(readcontext.getTopStackElement().getTag());
							for(int i=readcontext.getStackSize()-2; i>=0 && object==null; i--)
							{
								StackElement pse = readcontext.getStackElement(i);
								attrpath.add(pse.getTag());
								typeinfo = pse.getTypeInfo();
								object = pse.getObject();
							}
						}
						
						// Handle attributes
						if(object!=null)
						{
							Set attrs = typeinfo==null? Collections.EMPTY_SET: typeinfo.getXMLAttributeNames();
							for(int i=0; i<parser.getAttributeCount(); i++)
							{
								QName attrname = parser.getAttributePrefix(i)==null || parser.getAttributePrefix(i)==XMLConstants.DEFAULT_NS_PREFIX? new QName(parser.getAttributeLocalName(i))
									: new QName(parser.getAttributeNamespace(i), parser.getAttributeLocalName(i), parser.getAttributePrefix(i));
	
	//							System.out.println("here: "+attrname);
								
								if(!SXML.ID.equals(attrname.getLocalPart()) && !SXML.ARRAYLEN.equals(attrname.getLocalPart()))
								{	
									String attrval = parser.getAttributeValue(i);
									attrs.remove(attrname);
									
									Object attrinfo = typeinfo!=null ? typeinfo.getAttributeInfo(attrname) : null;
									if(!(attrinfo instanceof AttributeInfo && ((AttributeInfo)attrinfo).isIgnoreRead()))
									{
			//							ITypeConverter attrconverter = typeinfo!=null ? typeinfo.getAttributeConverter(attrname) : null;
			//							Object val = attrconverter!=null? attrconverter.convertObject(attrval, root, classloader): attrval;
										
										handler.handleAttributeValue(object, attrname, attrpath, attrval, attrinfo, readcontext);
									
										if(attrinfo instanceof AttributeInfo && AttributeInfo.ID.equals(((AttributeInfo)attrinfo).getId()))
										{
	//										System.out.println("ID: "+attrval+", "+object);
											readcontext.getReadObjects().put(attrval, object);
										}
									}
								}
							}
							// Handle unset attributes (possibly have default value).
							for(Iterator it=attrs.iterator(); it.hasNext(); )
							{
								QName attrname = (QName)it.next();
								Object attrinfo = typeinfo.getAttributeInfo(attrname);
								
								// Hack. want to read attribute info here
								handler.handleAttributeValue(object, attrname, attrpath, null, attrinfo, readcontext);
							}
						}
						else
						{
							StackElement	se	= readcontext.getTopStackElement();
							readcontext.getReporter().report("No element on stack for attributes", "stack error", se, se.getLocation());													
						}
					}
					
					// Handle comment.
					if(readcontext.getComment()!=null && typeinfo!=null)
					{
						Object commentinfo = typeinfo.getCommentInfo();
						if(commentinfo!=null)
						{
							handler.handleAttributeValue(object, null, null, readcontext.getComment(), commentinfo, 
								readcontext);
						}
					}
				}
			}
			
			readcontext.setComment(null);
			
//			System.out.println("start: "+parser.getLocalName());
		}
	}
	
	/**
	 *  Handle the end element.
	 *  @param readcontext The context for reading with all necessary information.
	 */
	protected void handleEndElement(final ReadContext readcontext) throws Exception
	{
		if(readcontext.getReadIgnore()==0)
		{
			XMLStreamReader parser = readcontext.getParser();
//			List stack = readcontext.getStack();
			StackElement topse = readcontext.getTopStackElement();
			
	//		System.out.println("end: "+parser.getLocalName());
//			QName localname = parser.getPrefix()==null || parser.getPrefix()==XMLConstants.DEFAULT_NS_PREFIX? new QName(parser.getLocalName())
//				: new QName(parser.getNamespaceURI(), parser.getLocalName(), parser.getPrefix());
			
			QName localname = parser.getName();
			QName[] fullpath = readcontext.getXMLPath();
			final TypeInfo typeinfo = tipmanager.getTypeInfo(localname, fullpath, topse.getRawAttributes());
	
			// Hack. Change object to content when it is element of its own.
			if((topse.getObject()==null && topse.getContent()!=null && topse.getContent().trim().length()>0) || topse.getObject()==STRING_MARKER)
			{
				// Handle possible content type conversion.
				Object val = topse.getContent()!=null? topse.getContent(): topse.getObject();
				if(val instanceof String)
				{
					if(typeinfo!=null && typeinfo.getContentInfo()!=null)
					{
						Object coninfo = typeinfo.getContentInfo();
						if(coninfo!=null && coninfo instanceof AttributeInfo)
						{
							IStringObjectConverter conv = ((AttributeInfo)coninfo).getConverter();
							if(conv!=null)
							{
								val = conv.convertString((String)val, readcontext);
							}
						}
					}
					else
					{
						val = topse.getReaderHandler().convertContentObject((String)val, localname, readcontext);
					}
				}
				
				topse = new StackElement(topse.getReaderHandler(), topse.getTag(), val, topse.getRawAttributes(), null, topse.getLocation());
				readcontext.setStackElement(topse, readcontext.getStackSize()-1);
//				stack.set(stack.size()-1, topse);
//				readcontext.setTopse(topse);
			}
			
			// Handle content.
			if(topse.getObject()!=null && topse.getContent()!=null && topse.getContent().trim().length()>0)
			{
				if(typeinfo!=null && typeinfo.getContentInfo()!=null) 
				{
					topse.getReaderHandler().handleAttributeValue(topse.getObject(), null, null, topse.getContent(), typeinfo.getContentInfo(), readcontext);
				}
				else
				{
					StackElement	se	= readcontext.getTopStackElement();
					readcontext.getReporter().report("No content mapping for: "+topse.getContent()+" tag="+topse.getTag(), "link error", se, se.getLocation());													
				}
			}
				
			// Handle post-processing			
			final IPostProcessor postproc = topse.getReaderHandler().getPostProcessor(topse.getObject(), typeinfo);
			if(postproc!=null)
			{
				if(postproc.getPass()==0)
				{
					try
					{
						Object changed = postproc.postProcess(readcontext, topse.getObject());
						if(changed!=null)
							topse.setObject(changed);
					}
					catch(RuntimeException e)
					{
//						e.printStackTrace();
						readcontext.getReporter().report("Error during postprocessing: "+e, "postprocessor error", topse, topse.getLocation());																				
					}
				}
				else
				{
					final StackElement	ftopse	= topse;
					final StackElement[]	stack	= readcontext.getStack();	// Use snapshot of stack for error report, as stack isn't available in delayed post processors.
					readcontext.getPostProcessors().put(new Integer(postproc.getPass()), new IPostProcessorCall()
					{
						public void callPostProcessor() throws XMLStreamException
						{
							try
							{
								Object check = postproc.postProcess(readcontext, ftopse.getObject());
								if(check!=null)
								{
									readcontext.getReporter().report("Object replacement only possible in first pass.", "postprocessor error", ftopse, ftopse!=null ? ftopse.getLocation() : readcontext.getParser().getLocation());																				
								}
							}
							catch(RuntimeException e)
							{
//								e.printStackTrace();
								readcontext.getReporter().report("Error during postprocessing: "+e, "postprocessor error", stack, ftopse!=null ? ftopse.getLocation() : readcontext.getParser().getLocation());																				
							}
						}
					});
				}
			}
			
			// If object has internal id save it in the readobjects map.
			String id = topse.getRawAttributes()!=null? (String)topse.getRawAttributes().get(SXML.ID): null;
			if(id!=null && topse.getObject()!=null)
			{
//				System.out.println("ID: "+id+", "+val.getClass());
				readcontext.getReadObjects().put(id, topse.getObject());
			}				
	
			// Link current object to parent
			if(topse.getObject()!=null)
			{					
				// Handle linking
				boolean bulklink = typeinfo!=null? typeinfo.isBulkLink(): this.bulklink;
				if(readcontext.getStackSize()>0 && bulklink)
				{
					// Invoke bulk link for the finished object (as parent).
					List childs = readcontext.removeChildren(topse.getObject());
					if(childs!=null)
					{
						IBulkObjectLinker linker = (IBulkObjectLinker)(typeinfo!=null && typeinfo.getLinker()!=null? typeinfo.getLinker(): topse.getReaderHandler());
						linker.bulkLinkObjects(topse.getObject(), childs, readcontext);
					}
				}
				if(readcontext.getStackSize()>1)
				{
					StackElement pse = readcontext.getStackElement(readcontext.getStackSize()-2);
					List pathname = new ArrayList();
					pathname.add(localname);
					for(int i=readcontext.getStackSize()-3; i>=0 && pse.getObject()==null; i--)
					{
						pse = readcontext.getStackElement(i);
						pathname.add(0, readcontext.getStackElement(i+1).getTag());
					}
					
					if(pse.getObject()!=null)
					{
	//						System.out.println("here: "+parser.getLocalName()+" "+getXMLPath(stack)+" "+topse.getRawAttributes());
					
						TypeInfo patypeinfo = pse.getTypeInfo();
						SubobjectInfo linkinfo = getSubobjectInfoRead(localname, fullpath, patypeinfo, topse.getRawAttributes());
						bulklink = patypeinfo!=null? patypeinfo.isBulkLink(): this.bulklink;
						
						if(!bulklink)
						{
							IObjectLinker linker = (IObjectLinker)(patypeinfo!=null && patypeinfo.getLinker()!=null? patypeinfo.getLinker(): pse.getReaderHandler());
							linker.linkObject(topse.getObject(), pse.getObject(), linkinfo==null? null: linkinfo, 
								(QName[])pathname.toArray(new QName[pathname.size()]), readcontext);
						}
						else
						{
							// Save the finished object as child for its parent.
							readcontext.addChild(pse.getObject(), new LinkData(topse.getObject(), linkinfo==null? null: linkinfo, 
								(QName[])pathname.toArray(new QName[pathname.size()])));	
						}
					}
					else
					{
						StackElement	se	= readcontext.getTopStackElement();
						readcontext.getReporter().report("No parent object found for: "+SUtil.arrayToString(fullpath), "link error", se, se.getLocation());													
					}
				}
			}
			
			readcontext.removeStackElement();
		}
		else
		{
			readcontext.setReadIgnore(readcontext.getReadIgnore()-1);
		}
	}
	
	/**
	 *  @param val The string value.
	 *  @return The encoded object.
	 */
	public static Object objectFromXML(Reader reader, String val, ClassLoader classloader)
	{
		return objectFromXML(reader, val, classloader, null);
	}
	
	/**
	 *  @param val The string value.
	 *  @return The encoded object.
	 */
	public static Object objectFromXML(Reader reader, String val, ClassLoader classloader, Object context)
	{
//		return objectFromByteArray(reader, val.getBytes(), classloader, context);
		java.io.Reader rd = null;
		try
		{
			rd = new StringReader(val);
			Object ret = reader.read(rd, classloader, context);
			return ret;
		}
		catch(Exception e)
		{
//			t.printStackTrace();
//			System.out.println("problem: "+new String(val));
			throw new RuntimeException(e);
		}
		finally
		{
			if(rd!=null)
			{
				try
				{
					rd.close();
				}
				catch(Exception e)
				{
				}
			}
		}
	}
		
	/**
	 *  @param val The string value.
	 *  @return The encoded object.
	 */
	public static Object objectFromByteArray(Reader reader, byte[] val, ClassLoader classloader)
	{
		return objectFromByteArray(reader, val, classloader, null);
	}
	
	/**
	 *  @param val The string value.
	 *  @return The encoded object.
	 */
	public static Object objectFromByteArray(Reader reader, byte[] val, ClassLoader classloader, Object context)
	{
		InputStream bis = null;
		try
		{
			bis = new ByteArrayInputStream(val);
			Object ret = reader.read(bis, classloader, context);
			return ret;
		}
		catch(Exception e)
		{
//			t.printStackTrace();
//			System.out.println("problem: "+new String(val));
			throw new RuntimeException(e);
		}
		finally
		{
			if(bis!=null)
			{
				try
				{
					bis.close();
				}
				catch(Exception e)
				{
				}
			}
		}
	}
	
	/**
	 * 
	 * /
	public static Object[] getLastStackElementWithObject(List stack, QName localname)
	{
		StackElement pse = (StackElement)stack.get(stack.size()-2);
		List pathname = new ArrayList();
		pathname.add(localname);
		for(int i=stack.size()-3; i>=0 && pse.getObject()==null; i--)
		{
			pse = (StackElement)stack.get(i);
			pathname.add(0, ((StackElement)stack.get(i+1)).getTag());
		}
		return new Object[]{pse, pathname};
	}*/
	
	/**
	 *  Get a subobject info for reading.
	 */
	public static SubobjectInfo getSubobjectInfoRead(QName localname, QName[] fullpath, TypeInfo patypeinfo, Map attrs)
	{
		SubobjectInfo ret = null;
		if(patypeinfo!=null)
		{
			QName tag = localname;
			QName[] fpath = fullpath;
			// Hack! If localname is classname remove it
			if(localname.getNamespaceURI().startsWith(SXML.PROTOCOL_TYPEINFO))
			{
				tag = fullpath[fullpath.length-2];
				fpath = new QName[fullpath.length-1];
				System.arraycopy(fullpath, 0, fpath, 0, fpath.length);
			}
			ret = patypeinfo.getSubobjectInfoRead(tag, fpath, attrs);
		}
		return ret;
	}
}
