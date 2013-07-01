package jadex.bpmn.model.io;

import jadex.bpmn.model.MActivity;
import jadex.bpmn.model.MBpmnModel;
import jadex.bpmn.model.MContextVariable;
import jadex.bpmn.model.MDataEdge;
import jadex.bpmn.model.MIdElement;
import jadex.bpmn.model.MLane;
import jadex.bpmn.model.MMessagingEdge;
import jadex.bpmn.model.MParameter;
import jadex.bpmn.model.MPool;
import jadex.bpmn.model.MProperty;
import jadex.bpmn.model.MSequenceEdge;
import jadex.bpmn.model.MSubProcess;
import jadex.bridge.ClassInfo;
import jadex.bridge.modelinfo.Argument;
import jadex.bridge.modelinfo.ConfigurationInfo;
import jadex.bridge.modelinfo.ModelInfo;
import jadex.bridge.modelinfo.UnparsedExpression;
import jadex.bridge.service.ProvidedServiceImplementation;
import jadex.bridge.service.ProvidedServiceInfo;
import jadex.bridge.service.RequiredServiceBinding;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.commons.Tuple2;
import jadex.javaparser.SJavaParser;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

public class SBpmnModelReader
{
	/** Key for visual object bounds. */
	public static final String BOUNDS_KEY = "bounds";
	
	/** Key for alternative visual object bounds. */
	public static final String ALT_BOUNDS_KEY = "altbounds";
	
	/** Key for alternative visual object bounds. */
	public static final String WAYPOINTS_KEY = "waypoints";
	
	/** Key for visual internalized parameters. */
	public static final String INTERNAL_PARAMETERS_KEY = "internalparameters";
	
	/** Tag aliases. */
	public static final Map<String, String> TAG_ALIASES;
	static
	{
		Map<String, String> aliases = new HashMap<String, String>();
		aliases.put("taskclass", "class");
		TAG_ALIASES = Collections.unmodifiableMap(aliases);
	}
	
	/** Activity type mapping. */
	public static final Map<String, String> ACT_TYPE_MAPPING = new HashMap<String, String>();
	static
	{
		for (Map.Entry<String, String> entry : SBpmnModelWriter.ACT_TYPE_MAPPING.entrySet())
		{
			if (!entry.getKey().contains("Event"))
			{
				ACT_TYPE_MAPPING.put(entry.getValue(), entry.getKey());
			}
		}
		
		ACT_TYPE_MAPPING.put("userTask", MBpmnModel.TASK);
		
	}
	
	/**
	 *  Loads the model from a file without parsing expressions.
	 *  
	 *  @param file The model file.
	 *  @param vreader The visual model reader, may be null.
	 */
	public static final MBpmnModel readModel(File file, IBpmnVisualModelReader vreader) throws Exception
	{
		return readModel(file, vreader, null);
	}
	
	/**
	 *  Loads the model from a file.
	 *  
	 *  @param file The model file.
	 *  @param vreader The visual model reader, may be null.
	 *  @param cl The class loader for parsing expressions, may be null.
	 */
	public static final MBpmnModel readModel(File file, IBpmnVisualModelReader vreader, ClassLoader cl) throws Exception
	{
		BufferedInputStream fis = new BufferedInputStream(new FileInputStream(file));
		XMLInputFactory fac = XMLInputFactory.newInstance(); 
		XMLStreamReader reader = fac.createXMLStreamReader(fis);
		
		LinkedList<QName> tagstack = new LinkedList<QName>();
		LinkedList<Map<String, String>> attrstack = new LinkedList<Map<String,String>>();
		LinkedList<String> contentstack = new LinkedList<String>();
		
		MBpmnModel ret = new MBpmnModel();
		ret.setClassLoader(cl);
		ret.setFilename(file.getPath());
		ret.setName(file.getName().substring(0, file.getName().length() - 6));
		Map<String, MIdElement> bpmnelementmap = new HashMap<String, MIdElement>();
		Map<String, MLane> lanemap = new HashMap<String, MLane>();
		Map<String, String> laneparents = new HashMap<String, String>();
		Map<String, Object> buffer = new HashMap<String, Object>();
		buffer.put("subprocessstack", new LinkedList<MSubProcess>());
		buffer.put("subprocesselementmap", new HashMap<String, MSubProcess>());
		buffer.put("eventhandlerparentmap", new HashMap<String, String>());
		
		buffer.put("unresolvedsequencesources", new HashMap<String, MSequenceEdge>());
		buffer.put("unresolvedsequencetargets", new HashMap<String, MSequenceEdge>());
		
		String sempre = null;
		
		String text = null;
		while (reader.hasNext())
		{
		    reader.next();
		    if (reader.getEventType() == XMLStreamConstants.START_ELEMENT &&
		    	"definitions".equals(reader.getLocalName()) &&
		    	reader.getPrefix() != null &&
		    	reader.getPrefix().equals(reader.getNamespaceContext().getPrefix("http://www.omg.org/spec/BPMN/20100524/MODEL")))
		    {
				sempre = reader.getNamespaceContext().getPrefix("http://www.omg.org/spec/BPMN/20100524/MODEL");
				buffer.put("di", reader.getNamespaceContext().getPrefix("http://www.omg.org/spec/DD/20100524/DI"));
				buffer.put("dc", reader.getNamespaceContext().getPrefix("http://www.omg.org/spec/DD/20100524/DC"));
				buffer.put("bpmndi", reader.getNamespaceContext().getPrefix("http://www.omg.org/spec/BPMN/20100524/DI"));
				buffer.put("semantic", sempre);
				buffer.put("jadex", reader.getNamespaceContext().getPrefix("http://www.activecomponents.org/bpmnextensions"));
				buffer.put("jadexvisual", reader.getNamespaceContext().getPrefix("http://www.activecomponents.org/bpmnvisualextensions"));
		    }
		    if (reader.getEventType() == XMLStreamConstants.START_ELEMENT)
		    {
		    	text = null;
		    	
		    	tagstack.push(reader.getName());
		    	
		    	Map<String, String> attrs = null;
		    	if (reader.getAttributeCount() > 0)
		    	{
		    		attrs = new HashMap<String, String>(reader.getAttributeCount());
			    	for (int i = 0; i < reader.getAttributeCount(); ++i)
			    	{
			    		attrs.put(reader.getAttributeLocalName(i), unescapeString(reader.getAttributeValue(i)));
			    	}
		    	}
		    	attrstack.push(attrs);
		    	
		    	if ("extension".equals(reader.getName().getLocalPart()) && sempre.equals(reader.getName().getPrefix()))
	    		{
	    			buffer.put("extension", null);
	    		}
		    	else if ("process".equals(reader.getName().getLocalPart()) && sempre.equals(reader.getName().getPrefix()))
	    		{
	    			handlePool(ret, attrs, bpmnelementmap, buffer);
	    		}
		    	else if ("subProcess".equals(reader.getName().getLocalPart()) && sempre.equals(reader.getName().getPrefix()))
		    	{
		    		LinkedList<MSubProcess> sps = (LinkedList<MSubProcess>) buffer.get("subprocessstack");
		    		sps.push(new MSubProcess());
		    	}
		    }
		    else if (reader.getEventType() == XMLStreamConstants.CHARACTERS)
		    {
		    	String moretext = reader.getText();
		    	if (moretext != null)
		    	{
		    		text = text != null? text + moretext : moretext;
		    	}
		    }
		    else if (reader.getEventType() == XMLStreamConstants.END_ELEMENT)
		    {
		    	text = text!=null && text.trim().length()>0? text.trim(): null;
		    	text = text != null? unescapeString(text) : null;
		    	contentstack.push(text);
		    	text = null;
		    	
		    	if ("extension".equals(reader.getName().getLocalPart()) && sempre.equals(reader.getName().getPrefix()))
	    		{
	    			buffer.remove("extension");
	    		}
		    	
		    	handleElement(ret, bpmnelementmap, lanemap, laneparents, tagstack.pop(), tagstack, attrstack.pop(), contentstack.pop(), buffer, vreader);
		    }
		}
		
		return ret;
	}
	
	/**
	 *  Handles a pool.
	 *  
	 *  @param model The model.
	 *  @param lastobject The last object.
	 *  @param tag The current tag.
	 *  @param tagstack The tag stack.
	 *  @param attrs Current attributes.
	 *  @param content The tag content.
	 *  @param buffer Buffered information.
	 */
	protected static final void handlePool(MBpmnModel model, Map<String, String> attrs, Map<String, MIdElement> emap, Map<String, Object> buffer)
	{
		
		MPool currentpool = new MPool();
		currentpool.setName(attrs.get("name"));
		currentpool.setId(attrs.get("id"));
		emap.put(currentpool.getId(), currentpool);
		model.addPool(currentpool);
		buffer.put("lastobject", currentpool);
		buffer.put("pool", currentpool);
	}
	
	/**
	 *  Handles XML elements.
	 */
	protected static final void handleElement(MBpmnModel model,
											  Map<String, MIdElement> emap,
											  Map<String, MLane> lanemap,
											  Map<String, String> laneparents,
											  QName tag,
											  LinkedList<QName> tagstack,
											  Map<String, String> attrs,
											  String content,
											  Map<String, Object> buffer,
											  IBpmnVisualModelReader vreader)
	{
		if(buffer.get("semantic").equals(tag.getPrefix()))
		{
			handleSemanticElement(model, emap, lanemap, laneparents, tag, tagstack, attrs, content, buffer);
		}
		else if(buffer.get("jadex").equals(tag.getPrefix()))
		{
			handleJadexElement(model, tag, tagstack, attrs, content, buffer, emap);
		}
		else if (vreader != null && (buffer.get("bpmndi").equals(tag.getPrefix()) || buffer.get("dc").equals(tag.getPrefix()) || buffer.get("di").equals(tag.getPrefix()) || buffer.get("jadexvisual").equals(tag.getPrefix())))
		{
			handleVisualElement(vreader, tag, attrs, content, laneparents, emap, buffer);
		}
	}
	
	/**
	 *  Handles a semantic section.
	 *  
	 *  @param model The model.
	 *  @param lastobject The last object.
	 *  @param tag The current tag.
	 *  @param tagstack The tag stack.
	 *  @param attrs Current attributes.
	 *  @param content The tag content.
	 *  @param buffer Buffered information.
	 */
	protected static final void handleSemanticElement(MBpmnModel model,
													  Map<String, MIdElement> emap,
													  Map<String, MLane> lanemap,
													  Map<String, String> laneparents,
													  QName tag,
													  LinkedList<QName> tagstack,
													  Map<String, String> attrs,
													  String content,
													  Map<String, Object> buffer)
	{
		/*if ("process".equals(tag.getLocalPart()))
		{
			MPool currentpool = new MPool();
			currentpool.setName(attrs.get("name"));
			currentpool.setId(attrs.get("id"));
			emap.put(currentpool.getId(), currentpool);
			model.addPool(currentpool);
			buffer.put("lastobject", currentpool);
			buffer.put("pool", currentpool);
		}*/
		ClassLoader cl = model.getClassLoader();
		
		if("lane".equals(tag.getLocalPart()))
		{
			MLane lane = new MLane();
			lane.setName(attrs.get("name"));
			lane.setId(attrs.get("id"));
			emap.put(lane.getId(), lane);
			MPool pool = (MPool) buffer.get("pool");
			pool.addLane(lane);
			List<String> noderefs = (List<String>) buffer.remove("flownoderefs");
			if (noderefs != null)
			{
				for (String ref : noderefs)
				{
					lanemap.put(ref, lane);
				}
			}
			laneparents.put(lane.getId(), pool.getId());
		}
		else if("flowNodeRef".equals(tag.getLocalPart()))
		{
			List<String> noderefs = (List<String>) buffer.get("flownoderefs");
			if (noderefs == null)
			{
				noderefs = new ArrayList<String>();
				buffer.put("flownoderefs", noderefs);
			}
			noderefs.add(content);
		}
		else if (ACT_TYPE_MAPPING.containsKey(tag.getLocalPart()))
		{
			MActivity act;
			if (MBpmnModel.SUBPROCESS.equals(ACT_TYPE_MAPPING.get(tag.getLocalPart())))
			{
				LinkedList<MSubProcess> sps = (LinkedList<MSubProcess>) buffer.get("subprocessstack");
				act = sps.pop();
			}
			else
			{
				act = new MActivity();
			}
			
			if (attrs.containsKey("name"))
			{
				act.setName(attrs.get("name"));
			}
			act.setId(attrs.get("id"));
			
			if (attrs.containsKey("default"))
			{
				Set<String> de = (Set<String>) buffer.get("defaultedges");
				if (de == null)
				{
					de = new HashSet<String>();
					buffer.put("defaultedges", de);
				}
				de.add(attrs.get("default"));
			}
			
			act.setActivityType(ACT_TYPE_MAPPING.get(tag.getLocalPart()));
			
			if(buffer.containsKey("class"))
			{
				act.setClazz(new ClassInfo((String) buffer.remove("class")));
			}
			
			if(buffer.containsKey("parameters"))
			{
				List<MParameter> params = (List<MParameter>) buffer.remove("parameters");
				for(MParameter param : params)
				{
					act.addParameter(param);
				}
			}
			
			if(buffer.containsKey("properties"))
			{
				List<MProperty> props = (List<MProperty>) buffer.remove("properties");
				for(MProperty prop : props)
				{
					act.addProperty(prop);
				}
			}
			
			connectActivityEdges(act, buffer, emap);
			
			insertActivity(act, buffer, lanemap, emap);
		}
		else if (tag.getLocalPart() != null && tag.getLocalPart().contains("Event") && !tag.getLocalPart().contains("Definition"))
		{
			MActivity evt = new MActivity();
			
			if (attrs.containsKey("name"))
			{
				evt.setName(attrs.get("name"));
			}
			evt.setId(attrs.get("id"));
			
			if(buffer.containsKey("class"))
			{
				evt.setClazz(new ClassInfo((String) buffer.remove("class")));
			}
			
			String acttype = "Event";
			if (tag.getLocalPart().startsWith("end") || tag.getLocalPart().contains("Throw"))
			{
				evt.setThrowing(true);
			}
			
			if (tag.getLocalPart().startsWith("start"))
			{
				acttype += "Start";
			}
			else if (tag.getLocalPart().startsWith("end"))
			{
				acttype += "End";
			}
			else
			{
				acttype += "Intermediate";
				
				if (tag.getLocalPart().startsWith("boundary"))
				{
					evt.setEventHandler(true);
					Map<String, String> ehpm = (Map<String, String>) buffer.get("eventhandlerparentmap");
					ehpm.put(evt.getId(), attrs.get("attachedToRef"));
				}
			}
			
			List<Tuple2<String, String>> evttypes = (List<Tuple2<String, String>>) buffer.remove("evttypes");
			if (evttypes == null || evttypes.size() == 0)
			{
				acttype += "Empty";
			}
			else if (evttypes.size() == 1)
			{
				acttype += evttypes.get(0).getFirstEntity();
			}
			else
			{
				//TODO: Multi
			}
			
			String dur = (String) buffer.remove("duration");
			if (dur != null)
			{
				UnparsedExpression exp = parseExp(new UnparsedExpression("duration", "java.lang.Number", dur, null),model.getModelInfo().getAllImports(), cl);
				MProperty mprop = new MProperty(exp.getClazz(), exp.getName(), exp);
				evt.addProperty(mprop);
			}
			
			evt.setActivityType(acttype);
			
			connectActivityEdges(evt, buffer, emap);
			
			insertActivity(evt, buffer, lanemap, emap);
		}
		else if (tag.getLocalPart() != null && tag.getLocalPart().endsWith("EventDefinition"))
		{
			List<Tuple2<String, String>> evttypes = (List<Tuple2<String, String>>) buffer.get("evttypes");
			if (evttypes == null)
			{
				evttypes = new ArrayList<Tuple2<String,String>>();
				buffer.put("evttypes", evttypes);
			}
			
			if ("messageEventDefinition".equals(tag.getLocalPart()))
			{
				evttypes.add(new Tuple2("Message", null));
			}
			else if ("timerEventDefinition".equals(tag.getLocalPart()))
			{
				evttypes.add(new Tuple2("Timer", null));
			}
			else if ("conditionalEventDefinition".equals(tag.getLocalPart()))
			{
				evttypes.add(new Tuple2("Rule", null));
			}
			else if ("signalEventDefinition".equals(tag.getLocalPart()))
			{
				evttypes.add(new Tuple2("Signal", null));
			}
			else if ("errorEventDefinition".equals(tag.getLocalPart()))
			{
				evttypes.add(new Tuple2("Error", null));
			}
			else if ("compensateEventDefinition".equals(tag.getLocalPart()))
			{
				evttypes.add(new Tuple2("Compensation", null));
			}
			else if ("cancelEventDefinition".equals(tag.getLocalPart()))
			{
				evttypes.add(new Tuple2("Cancel", null));
			}
		}
		else if("sequenceFlow".equals(tag.getLocalPart()))
		{
			MSequenceEdge edge = new MSequenceEdge();
			edge.setId(attrs.get("id"));
			String edgename = attrs.get("name");
			edgename = edgename != null? unescapeString(edgename) : null;
			edge.setName(edgename);
			MActivity src = (MActivity) emap.get(attrs.get("sourceRef"));
			MActivity tgt = (MActivity) emap.get(attrs.get("targetRef"));
			edge.setSource(src);
			edge.setTarget(tgt);
			
			Set<String> de = (Set<String>) buffer.get("defaultedges");
			if (de != null && de.contains(edge.getId()))
			{
				edge.setDefault(true);
			}
			
			String cond = (String) buffer.remove("condition");
			if (cond != null)
			{
				edge.setCondition(parseExp(new UnparsedExpression("", "java.lang.Boolean", cond, null), model.getModelInfo().getAllImports(), cl));
			}
			
			Map<String, String> mappings = (Map<String, String>) buffer.remove("parametermappings");
			if (mappings != null)
			{
				for (Map.Entry<String, String> entry : mappings.entrySet())
				{
					String name = entry.getKey();
					String expstring = entry.getValue();
					UnparsedExpression exp = new UnparsedExpression(name, "java.lang.Object", expstring, null);
					parseExp(exp, model.getModelInfo().getAllImports(), cl);
					UnparsedExpression iexp	= null;
					
					if(name.endsWith("]") && name.indexOf("[")!=-1)
					{
						String	itext	= name.substring(name.indexOf("[")+1, name.length()-1);
						name = name.substring(0, name.indexOf("["));
						iexp = new UnparsedExpression(name, "java.lang.Object", itext, null);
						parseExp(iexp, model.getModelInfo().getAllImports(), cl);
					}
					edge.addParameterMapping(entry.getKey(), exp, iexp);
				}
			}
			
			if (src != null)
			{
				src.addOutgoingSequenceEdge(edge);
			}
			if (tgt != null)
			{
				tgt.addIncomingSequenceEdge(edge);
			}
			
//			LinkedList<MSubProcess> sps = (LinkedList<MSubProcess>) buffer.get("subprocessstack");
			//TODO: No longer necessary, cleanup?
//			if (sps.isEmpty())
//			{
//				((MPool) buffer.get("pool")).addSequenceEdge(edge);
//			}
//			else
//			{
//				sps.peek().addSequenceEdge(edge);
//			}
			
			emap.put(edge.getId(), edge);
		}
		else if("messageFlow".equals(tag.getLocalPart()))
		{
			MMessagingEdge edge = new MMessagingEdge();
			edge.setId(attrs.get("id"));
			String edgename = attrs.get("name");
			edgename = edgename != null? unescapeString(edgename) : null;
			edge.setName(edgename);
			MActivity src = (MActivity) emap.get(attrs.get("sourceRef"));
			MActivity tgt = (MActivity) emap.get(attrs.get("targetRef"));
			edge.setSource(src);
			edge.setTarget(tgt);
			
			src.addOutgoingMessagingEdge(edge);
			tgt.addIncomingMessagingEdge(edge);
			
			emap.put(edge.getId(), edge);
		}
		else if ("conditionExpression".equals(tag.getLocalPart()))
		{
			buffer.put("condition", content);
		}
		else if ("timeDuration".equals(tag.getLocalPart()))
		{
			buffer.put("duration", content);
		}
		else if ("incoming".equals(tag.getLocalPart()))
		{
			List<String> incoming = (List<String>) buffer.get("incoming");
			if (incoming == null)
			{
				incoming = new ArrayList<String>();
				buffer.put("incoming", incoming);
			}
			incoming.add(content);
		}
		else if ("outgoing".equals(tag.getLocalPart()))
		{
			List<String> outgoing = (List<String>) buffer.get("outgoing");
			if (outgoing == null)
			{
				outgoing = new ArrayList<String>();
				buffer.put("outgoing", outgoing);
			}
			outgoing.add(content);
		}
	}
	
	/**
	 *  Handles a Jadex model extension.
	 *  
	 *  @param model The model.
	 *  @param lastobject The last object.
	 *  @param tag The current tag.
	 *  @param tagstack The tag stack.
	 *  @param attrs Current attributes.
	 *  @param content The tag content.
	 *  @param buffer Buffered information.
	 */
	protected static final void handleJadexElement(MBpmnModel model,
												   QName tag,
												   LinkedList<QName> tagstack,
												   Map<String, String> attrs,
												   String content,
												   Map<String, Object> buffer,  
												   Map<String, MIdElement> emap)
	{
		ClassLoader cl = model.getClassLoader();
		
		if ("description".equals(tag.getLocalPart()))
		{
			if (tagstack.size() > 0 && "extension".equals(tagstack.get(0).getLocalPart()))
			{
				((ModelInfo) model.getModelInfo()).setDescription(content);
			}
			else
			{
				buffer.put(tag.getLocalPart(), content);
			}
		}
		else if ("poollane".equals(tag.getLocalPart()))
		{
			buffer.put(tag.getLocalPart(), content);
		}
		else if ("parametermapping".equals(tag.getLocalPart()))
		{
			Map<String, String> mappings = (Map<String, String>) buffer.get("parametermappings");
			if (mappings == null)
			{
				mappings = new HashMap<String, String>();
				buffer.put("parametermappings", mappings);
			}
			mappings.put(attrs.get("name"), content);
		}
		else if (matchLTag("class", tag))
		{
			buffer.put("class", content);
		}
		else if ("modelname".equals(tag.getLocalPart()))
		{
			model.setName(content);
		}
		else if ("subprocessref".equals(tag.getLocalPart()))
		{
			((LinkedList<MSubProcess>) buffer.get("subprocessstack")).peek().addProperty("filename", content);
		}
		else if ("subprocessexpressionref".equals(tag.getLocalPart()))
		{
			UnparsedExpression fileexp = new UnparsedExpression("file", String.class, content, null);
			MProperty mprop = new MProperty(fileexp.getClazz(), fileexp.getName(), fileexp);
			((LinkedList<MSubProcess>) buffer.get("subprocessstack")).peek().addProperty(mprop);
		}
		else if("parameter".equals(tag.getLocalPart()))
		{
			List<MParameter> params = (List<MParameter>) buffer.get("parameters");
			if (params == null)
			{
				params = new ArrayList<MParameter>();
				buffer.put("parameters", params);
			}
			ClassInfo clazz = new ClassInfo(attrs.get("type"));
			String name = attrs.get("name");
			UnparsedExpression exp = null;
			if(content!=null && content.length()>0)
			{
				exp = new UnparsedExpression(name, clazz.getTypeName(), content, null);
				parseExp(exp, model.getModelInfo().getAllImports(), cl);
			}
			MParameter param = new MParameter(attrs.get("direction"), clazz, name, exp);
			params.add(param);
		}
		else if("property".equals(tag.getLocalPart()))
		{
			List<MProperty> props = (List<MProperty>)buffer.get("properties");
			if(props == null)
			{
				props = new ArrayList<MProperty>();
				buffer.put("properties", props);
			}
			String type = attrs.get("type");
			ClassInfo clazz = type != null? new ClassInfo(type) : null;
			String typename = clazz != null? clazz.getTypeName() : null;
			String name = attrs.get("name");
//			UnparsedExpression exp = new UnparsedExpression(name, typename, content, null);
//			parseExp(exp, model.getModelInfo().getAllImports(), cl);
			UnparsedExpression exp = null;
			if(content!=null && content.length()>0)
			{
				exp = new UnparsedExpression(name, typename, content, null);
				parseExp(exp, model.getModelInfo().getAllImports(), cl);
			}
			MProperty prop = new MProperty(clazz, name, exp);
			props.add(prop);
		}
		else if ("argumentvalues".equals(tag.getLocalPart()))
		{
			Object vals = buffer.remove("values");
			if (vals != null)
			{
				buffer.put("argvalues", vals);
			}
		}
		else if ("resultvalues".equals(tag.getLocalPart()))
		{
			Object vals = buffer.remove("values");
			if (vals != null)
			{
				buffer.put("resvalues", vals);
			}
		}
		else if ("contextvariablevalues".equals(tag.getLocalPart()))
		{
			Object vals = buffer.remove("values");
			if (vals != null)
			{
				buffer.put("ctvvalues", vals);
			}
		}
		else if ("value".equals(tag.getLocalPart()))
		{
			if (attrs != null && attrs.containsKey("name"))
			{
				Map<String, String> vals = (Map<String, String>) buffer.get("values");
				if (vals == null)
				{
					vals = new HashMap<String, String>();
					buffer.put("values", vals);
				}
				vals.put(attrs.get("name"), content);
			}
			else
			{
				buffer.put("value", content);
			}
		}
		else if (buffer.containsKey("extension"))
		{
			if ("package".equals(tag.getLocalPart()))
			{
				((ModelInfo) model.getModelInfo()).setPackage(content);
			}
			else if ("componentflags".equals(tag.getLocalPart()))
			{
				if (attrs.containsKey("suspend"))
				{
					((ModelInfo) model.getModelInfo()).setSuspend(Boolean.parseBoolean(attrs.get("suspend")));
				}
				if (attrs.containsKey("master"))
				{
					((ModelInfo) model.getModelInfo()).setMaster(Boolean.parseBoolean(attrs.get("master")));
				}
				if (attrs.containsKey("daemon"))
				{
					((ModelInfo) model.getModelInfo()).setDaemon(Boolean.parseBoolean(attrs.get("daemon")));
				}
				if (attrs.containsKey("autoshutdown"))
				{
					((ModelInfo) model.getModelInfo()).setAutoShutdown(Boolean.parseBoolean(attrs.get("autoshutdown")));
				}
				if (attrs.containsKey("monitoring"))
				{
					((ModelInfo) model.getModelInfo()).setMonitoring(Boolean.parseBoolean(attrs.get("monitoring")));
				}
				if (attrs.containsKey("synchronous"))
				{
					((ModelInfo) model.getModelInfo()).setSynchronous(Boolean.parseBoolean(attrs.get("synchronous")));
				}
				if (attrs.containsKey("keepalive"))
				{
					model.setKeepAlive(Boolean.parseBoolean(attrs.get("keepalive")));
				}
			}
			else if ("import".equals(tag.getLocalPart()))
			{
				model.addImport(content);
			}
			else if ("argument".equals(tag.getLocalPart()))
			{
				Argument arg = new Argument();
				arg.setName(attrs.get("name"));
				arg.setClazz(new ClassInfo(attrs.get("type")));
				
				if (buffer.containsKey("description"))
				{
					arg.setDescription((String) buffer.remove("description"));
				}
				
				if (buffer.containsKey("value"))
				{
					arg.setValue((String) buffer.remove("value"));
				}
				
				((ModelInfo) model.getModelInfo()).addArgument(arg);
			}
			else if ("result".equals(tag.getLocalPart()))
			{
				Argument arg = new Argument();
				arg.setName(attrs.get("name"));
				arg.setClazz(new ClassInfo(attrs.get("type")));
				
				if (buffer.containsKey("description"))
				{
					arg.setDescription((String) buffer.remove("description"));
				}
				
				if (buffer.containsKey("value"))
				{
					arg.setValue((String) buffer.remove("value"));
				}
				
				((ModelInfo) model.getModelInfo()).addResult(arg);
			}
			else if ("contextvariable".equals(tag.getLocalPart()))
			{
				String name = attrs.get("name");
				String type = attrs.get("type");
				//UnparsedExpression exp = new UnparsedExpression(name, clazz.getTypeName(), (String) buffer.remove("value"), null);
				MContextVariable var = new MContextVariable(name, null, type, (String) buffer.remove("value"));
				parseExp(var, model.getModelInfo().getAllImports(), cl);
				//model.addContextVariable(name, clazz, exp, null);
				model.addContextVariable(var);
			}
			else if ("providedservice".equals(tag.getLocalPart()))
			{
				String name = attrs.get("name");
				ClassInfo itrface = attrs.get("interface") != null? new ClassInfo(attrs.get("interface")) : null;
				ClassInfo clazz = attrs.get("class") != null? new ClassInfo(attrs.get("class")) : null;
				String proxytype = attrs.get("proxytype");
				
				ProvidedServiceInfo ps = new ProvidedServiceInfo();
				ps.setName(name);
				ps.setType(itrface);
				ps.setImplementation(new ProvidedServiceImplementation());
				ps.getImplementation().setClazz(clazz);
				ps.getImplementation().setProxytype(proxytype);
				((ModelInfo) model.getModelInfo()).addProvidedService(ps);
			}
			else if ("providedserviceconfiguration".equals(tag.getLocalPart()))
			{
				List<ProvidedServiceInfo> vals = (List<ProvidedServiceInfo>) buffer.get("psconfs");
				if (vals == null)
				{
					vals = new ArrayList<ProvidedServiceInfo>();
					buffer.put("psconfs", vals);
				}
				String name = attrs.get("name");
				ClassInfo clazz = attrs.get("class") != null? new ClassInfo(attrs.get("class")) : null;
				String proxytype = attrs.get("proxytype");
				
				ProvidedServiceInfo ps = new ProvidedServiceInfo();
				ps.setName(name);
				ps.setImplementation(new ProvidedServiceImplementation());
				ps.getImplementation().setClazz(clazz);
				ps.getImplementation().setProxytype(proxytype);
				vals.add(ps);
			}
			else if ("requiredservice".equals(tag.getLocalPart()))
			{
				String name = attrs.get("name");
				ClassInfo itrface = attrs.get("interface") != null? new ClassInfo(attrs.get("interface")) : null;
				Boolean multi = attrs.get("multi") != null? Boolean.parseBoolean(attrs.get("multi")) : null;
				String scope = attrs.get("scope");
				
				RequiredServiceInfo rs = new RequiredServiceInfo();
				rs.setName(name);
				rs.setType(itrface);
				if (multi != null)
				{
					rs.setMultiple(multi.booleanValue());
				}
				rs.setDefaultBinding(new RequiredServiceBinding());
				rs.getDefaultBinding().setScope(scope);
				((ModelInfo) model.getModelInfo()).addRequiredService(rs);
			}
			else if ("requiredserviceconfiguration".equals(tag.getLocalPart()))
			{
				List<RequiredServiceInfo> vals = (List<RequiredServiceInfo>) buffer.get("rsconfs");
				if (vals == null)
				{
					vals = new ArrayList<RequiredServiceInfo>();
					buffer.put("rsconfs", vals);
				}
				String name = attrs.get("name");
				String scope = attrs.get("scope");
				
				RequiredServiceInfo rs = new RequiredServiceInfo();
				rs.setName(name);
				rs.setDefaultBinding(new RequiredServiceBinding());
				rs.getDefaultBinding().setScope(scope);
				vals.add(rs);
			}
			else if ("configuration".equals(tag.getLocalPart()))
			{
				ConfigurationInfo conf = new ConfigurationInfo(attrs.get("name"));
				
				if (attrs.containsKey("suspend"))
				{
					conf.setSuspend(Boolean.parseBoolean(attrs.get("suspend")));
				}
				if (attrs.containsKey("master"))
				{
					conf.setMaster(Boolean.parseBoolean(attrs.get("master")));
				}
				if (attrs.containsKey("daemon"))
				{
					conf.setDaemon(Boolean.parseBoolean(attrs.get("daemon")));
				}
				if (attrs.containsKey("autoshutdown"))
				{
					conf.setAutoShutdown(Boolean.parseBoolean(attrs.get("autoshutdown")));
				}
				
				if (buffer.containsKey("description"))
				{
					conf.setDescription((String) buffer.remove("description"));
				}
				
				if (buffer.containsKey("poollane"))
				{
					model.addPoolLane(conf.getName(), (String) buffer.remove("poollane"));
				}
				
				Map<String, String> vals = (Map<String, String>) buffer.remove("argvalues");
				if (vals != null)
				{
					for (Map.Entry<String, String> entry : vals.entrySet())
					{
						UnparsedExpression exp = new UnparsedExpression();
						exp.setName(entry.getKey());
						exp.setClazz(model.getModelInfo().getArgument(exp.getName()).getClazz());
						exp.setValue(entry.getValue());
						parseExp(exp, model.getModelInfo().getAllImports(), cl);
						conf.addArgument(exp);
					}
				}
				
				vals = (Map<String, String>) buffer.remove("resvalues");
				if (vals != null)
				{
					for (Map.Entry<String, String> entry : vals.entrySet())
					{
						UnparsedExpression exp = new UnparsedExpression();
						exp.setName(entry.getKey());
						exp.setClazz(model.getModelInfo().getResult(exp.getName()).getClazz());
						exp.setValue(entry.getValue());
						parseExp(exp, model.getModelInfo().getAllImports(), cl);
						conf.addResult(exp);
					}
				}
				
				vals = (Map<String, String>) buffer.remove("ctvvalues");
				if (vals != null)
				{
					for (Map.Entry<String, String> entry : vals.entrySet())
					{
						UnparsedExpression exp = new UnparsedExpression();
						exp.setName(entry.getKey());
						//exp.setClazz(model.getContextVariableClass(entry.getKey()));
						MContextVariable contextvar = model.getContextVariable(entry.getKey());
						exp.setClazz(contextvar.getClazz());
						exp.setValue(entry.getValue());
						parseExp(exp, model.getModelInfo().getAllImports(), cl);
						//model.setContextVariableExpression(entry.getKey(), conf.getName(), exp);
						contextvar.setValue(conf.getName(), exp);
					}
				}
				
				List<ProvidedServiceInfo> psconfs = (List<ProvidedServiceInfo>) buffer.get("psconfs");
				if (psconfs != null)
				{
					conf.setProvidedServices(psconfs.toArray(new ProvidedServiceInfo[psconfs.size()]));
				}
				
				List<RequiredServiceInfo> rsconfs = (List<RequiredServiceInfo>) buffer.get("rsconfs");
				if (rsconfs != null)
				{
					conf.setRequiredServices(rsconfs.toArray(new RequiredServiceInfo[rsconfs.size()]));
				}
				
				((ModelInfo) model.getModelInfo()).addConfiguration(conf);
			}
		}
		else if("dataFlow".equals(tag.getLocalPart()))
		{
			MDataEdge edge = new MDataEdge();
			edge.setId(attrs.get("id"));
			String edgename = attrs.get("name");
			edgename = edgename != null? unescapeString(edgename) : null;
			edge.setName(edgename);
			MActivity src = (MActivity)emap.get(attrs.get("sourceRef"));
			MActivity tgt = (MActivity)emap.get(attrs.get("targetRef"));
			edge.setSource(src);
			edge.setTarget(tgt);
			edge.setSourceParameter(attrs.get("sourceParam"));
			edge.setTargetParameter(attrs.get("targetParam"));
			
			String expstr = (String) buffer.remove("dataFlowValueMapping");
			if(expstr!=null && expstr.length()>0)
			{
				UnparsedExpression exp = new UnparsedExpression(
				edge.getSourceParameter()+"-"+edge.getTargetParameter(), "java.lang.Object", expstr, null);
				parseExp(exp, model.getModelInfo().getAllImports(), cl);
				edge.setParameterMapping(exp);
			}
			
			if (src != null &&
				tgt != null &&
				src.getParameters().get(edge.getSourceParameter()) != null &&
				tgt.getParameters().get(edge.getTargetParameter()) != null)
			{
			
				if(src != null)
				{
					src.addOutgoingDataEdge(edge);
				}
				if(tgt != null)
				{
					tgt.addIncomingDataEdge(edge);
				}
				
				emap.put(edge.getId(), edge);
			}
			else
			{
				System.err.println("Dangling data edge: " + edge.getId());
			}
		}
		else if ("dataFlowValueMapping".equals(tag.getLocalPart()))
		{
			buffer.put("dataFlowValueMapping", content);
		}
	}
	
	/**
	 *  Handles a visual element.
	 * 
	 *  @param vreader The visual reader.
	 *  @param tag The XML tag.
	 *  @param attrs The XML attributes.
	 *  @param emap The element map.
	 *  @param buffer The buffer.
	 */
	protected static final void handleVisualElement(IBpmnVisualModelReader vreader, QName tag, Map<String, String> attrs, String content, Map<String, String> laneparents, Map<String, MIdElement> emap, Map<String, Object> buffer)
	{
		if (vreader != null)
		{
//			long ts = System.currentTimeMillis();
			Map<String, Object> vbuffer = (Map<String, Object>) buffer.get("vbuffer");
			if (vbuffer == null)
			{
				vbuffer = new HashMap<String, Object>();
				buffer.put("vbuffer", vbuffer);
			}
			
			vreader.readElement(tag, attrs, laneparents, emap, buffer);
			
			if ("Bounds".equals(tag.getLocalPart()))
			{
				if (vbuffer.containsKey("bounds"))
				{
					Rectangle2D.Double alt = new Rectangle2D.Double();
					//xRectangle alt = new mxRectangle();
					alt.width = Double.parseDouble(attrs.get("width"));
					alt.height = Double.parseDouble(attrs.get("height"));
					alt.x = Double.parseDouble(attrs.get("x"));
					alt.y = Double.parseDouble(attrs.get("y"));
					//mxGeometry geo = (mxGeometry) buffer.get("bounds");
					//geo.setAlternateBounds(alt);
					vbuffer.put(ALT_BOUNDS_KEY, alt);
				}
				else
				{
	//				mxGeometry geo = new mxGeometry();
	//				geo.setWidth(Double.parseDouble(attrs.get("width")));
	//				geo.setHeight(Double.parseDouble(attrs.get("height")));
	//				geo.setX(Double.parseDouble(attrs.get("x")));
	//				geo.setY(Double.parseDouble(attrs.get("y")));
					Rectangle2D.Double bounds = new Rectangle2D.Double();
					bounds.setRect(Double.parseDouble(attrs.get("x")),
								   Double.parseDouble(attrs.get("y")),
								   Double.parseDouble(attrs.get("width")),
								   Double.parseDouble(attrs.get("height")));
					vbuffer.put(BOUNDS_KEY, bounds);
				}
			}
			else if ("BPMNShape".equals(tag.getLocalPart()))
			{
				String bpmnid = attrs.get("bpmnElement");
				MIdElement e = emap.get(bpmnid);
				String exp = attrs.get("isExpanded");
				Boolean expanded = exp != null? Boolean.parseBoolean(exp) : null;
				String eventparentid = e != null? ((Map<String, String>) buffer.get("eventhandlerparentmap")).get(e.getId()) : null;
				Map<String, MSubProcess> spem = (Map<String, MSubProcess>) buffer.get("subprocesselementmap");
				String subprocessparentid = e != null? spem.get(e.getId()) != null? spem.get(e.getId()).getId() : null : null;
				String laneparentid = e != null? laneparents.get(e.getId()) : null;
				Rectangle2D bounds = (Rectangle2D) vbuffer.remove(BOUNDS_KEY);
				Rectangle2D altbounds = (Rectangle2D) vbuffer.remove(ALT_BOUNDS_KEY);
				Set<String> intparams = (Set<String>) vbuffer.remove(INTERNAL_PARAMETERS_KEY);
				vreader.processBpmnShape(bpmnid, e, expanded, bounds, altbounds, intparams, eventparentid, subprocessparentid, laneparentid);
			}
			else if ("BPMNEdge".equals(tag.getLocalPart()))
			{
				String bpmnid = attrs.get("bpmnElement");
				MIdElement medge = emap.get(bpmnid);
				List<Point2D> waypoints = (List<Point2D>) vbuffer.remove(WAYPOINTS_KEY);
				vreader.processBpmnEdge(bpmnid, medge, waypoints);
			}
			else if ("Edge".equals(tag.getLocalPart()))
			{
				String type = attrs.get("type");
				List<Point2D> waypoints = (List<Point2D>) vbuffer.remove(WAYPOINTS_KEY);
				vreader.processGenericEdge(type, waypoints, attrs, emap);
			}
			else if ("waypoint".equals(tag.getLocalPart()))
			{
				List<Point2D> waypoints = (List<Point2D>) vbuffer.get(WAYPOINTS_KEY);
				if (waypoints == null)
				{
					waypoints = new ArrayList<Point2D>();
					vbuffer.put(WAYPOINTS_KEY, waypoints);
				}
				
				Point2D.Double point = new Point2D.Double();
				point.x = Double.parseDouble(attrs.get("x"));
				point.y = Double.parseDouble(attrs.get("y"));
				waypoints.add(point);
			}
			else if ("internalParameter".equals(tag.getLocalPart()))
			{
				Set<String> intparams = (Set<String>) vbuffer.get(INTERNAL_PARAMETERS_KEY);
				if (intparams == null)
				{
					intparams = new HashSet<String>();
					vbuffer.put(INTERNAL_PARAMETERS_KEY, intparams);
				}
				
				intparams.add(content);
			}
			
//			System.out.println(tag.getLocalPart() + " " + (System.currentTimeMillis() - ts));
		}
	}
	
	/**
	 *  Inserts an activity into the model and maps.
	 *  
	 *  @param act The activity.
	 *  @param buffer The buffer.
	 *  @param lanemap The lane map.
	 *  @param emap The element map.
	 */
	protected static final void insertActivity(MActivity act, Map<String, Object> buffer, Map<String, MLane> lanemap, Map<String, MIdElement> emap)
	{
		MPool pool = (MPool) buffer.get("pool");
		MLane lane = lanemap.get(act.getId());
		
		if (act.isEventHandler())
		{
			act.setPool(pool);
			act.setLane(lane);
			Map<String, String> ehpm = (Map<String, String>) buffer.get("eventhandlerparentmap");
			((MActivity) emap.get(ehpm.get(act.getId()))).addEventHandler(act);
		}
		else
		{
			LinkedList<MSubProcess> sps = (LinkedList<MSubProcess>) buffer.get("subprocessstack");
			if (!sps.isEmpty())
			{
				act.setPool(pool);
				act.setLane(lane);
				sps.peek().addActivity(act);
				Map<String, MSubProcess> spem = (Map<String, MSubProcess>) buffer.get("subprocesselementmap");
				spem.put(act.getId(), sps.peek());
			}
			else
			{
				if (lane != null)
				{
					lane.addActivity(act);
					act.setPool(pool);
					act.setLane(lane);
				}
				else
				{
					pool.addActivity(act);
					act.setPool(pool);
				}
			}
		}
		
		emap.put(act.getId(), act);
	}
	
	/**
	 *  Connects activity edges.
	 */
	protected static final void connectActivityEdges(MActivity act, Map<String, Object> buffer, Map<String, MIdElement> emap)
	{
		//FIXME: Support for other edges
		List<String> outgoing = (List<String>) buffer.remove("outgoing");
		if (outgoing != null)
		{
			for (String outid : outgoing)
			{
				MSequenceEdge edge = (MSequenceEdge) emap.get(outid);
				if (edge != null)
				{
					edge.setSource(act);
					act.addOutgoingSequenceEdge(edge);
				}
			}
		}
		
		List<String> incoming = (List<String>) buffer.remove("incoming");
		if (incoming != null)
		{
			for (String incid : incoming)
			{
				MSequenceEdge edge = (MSequenceEdge) emap.get(incid);
				if (edge != null)
				{
					edge.setTarget(act);
					act.addIncomingSequenceEdge(edge);
				}
			}
		}
	}
	
	/**
	 *  Parses the expression if possible.
	 *  
	 *  @param exp The expression.
	 *  @param imports The imports.
	 *  @param cl The class loader.
	 *  @return Parsed expression or unparsed if class loader is unavailable.
	 */
	protected static final UnparsedExpression parseExp(UnparsedExpression exp, String[] imports, ClassLoader cl)
	{
		if(cl != null)
		{
			try
			{
				SJavaParser.parseExpression(exp, imports, cl);
			}
			catch(Exception e)
			{
				System.err.println("Error parsing expression, name="+exp.getName()+", expstring="+exp.getValue());
				throw new RuntimeException(e);
			}
		}
		return exp;
	}
	
	/**
	 *  Matches local part of the tag.
	 *  TODO: Do this for all tags.
	 */
	protected static final boolean matchLTag(String tagname, QName tag)
	{
		String oltag = tag.getLocalPart();
		String ltag = TAG_ALIASES.get(oltag);
		if (ltag == null)
		{
			ltag = oltag;
		}
		return tagname.equals(ltag);
	}
	
	/**
	 *  Unescapes strings for xml.
	 */
	private static final String unescapeString(String string)
	{
		StringBuilder ret = new StringBuilder();
		boolean escapemode = false;
		for (int i = 0; i < string.length(); ++i)
		{
			String c = string.substring(i, i + 1);
			if (escapemode)
			{
				if (c.equals("n"))
				{
					ret.append("\n");
				}
				else
				{
					ret.append(c);
				}
				escapemode = false;
			}
			else
			{
				if (c.equals("\\"))
				{
					escapemode = true;
				}
				else
				{
					ret.append(c);
				}
			}
		}
		return ret.toString();
	}
}
