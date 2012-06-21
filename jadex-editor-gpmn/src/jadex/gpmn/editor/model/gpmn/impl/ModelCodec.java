package jadex.gpmn.editor.model.gpmn.impl;

import jadex.gpmn.editor.model.gpmn.IActivationEdge;
import jadex.gpmn.editor.model.gpmn.IActivationPlan;
import jadex.gpmn.editor.model.gpmn.IBpmnPlan;
import jadex.gpmn.editor.model.gpmn.IContext;
import jadex.gpmn.editor.model.gpmn.IEdge;
import jadex.gpmn.editor.model.gpmn.IElement;
import jadex.gpmn.editor.model.gpmn.IGoal;
import jadex.gpmn.editor.model.gpmn.IModelCodec;
import jadex.gpmn.editor.model.gpmn.INode;
import jadex.gpmn.editor.model.gpmn.IParameter;
import jadex.gpmn.editor.model.gpmn.IPlanEdge;
import jadex.gpmn.editor.model.gpmn.ISuppressionEdge;
import jadex.gpmn.editor.model.gpmn.ModelConstants;
import jadex.gpmn.editor.model.visual.VEdge;
import jadex.gpmn.editor.model.visual.VElement;
import jadex.gpmn.editor.model.visual.VGoal;
import jadex.gpmn.editor.model.visual.VPlan;
import jadex.gpmn.editor.model.visual.VVirtualActivationEdge;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.model.mxGraphModel;
import com.mxgraph.model.mxIGraphModel;
import com.mxgraph.util.mxPoint;
import com.mxgraph.view.mxGraph;

public class ModelCodec implements IModelCodec
{
	/** The format version */
	protected String VERSION = "3";
	
	/** The GPMN intermediate model. */
	protected GpmnModel model;
	
	/**
	 *  Creates a new model codec.
	 *  
	 *  @param model
	 */
	public ModelCodec(GpmnModel model)
	{
		this.model = model;
	}
	
	/**
	 *  Writes the model to a file.
	 * 
	 *  @param file The target file.
	 *  @param graph The visual graph.
	 *  @param model The GPMN intermediate model.
	 */
	public void writeModel(File file, mxGraph graph) throws IOException
	{
		GpmnModel gm = (GpmnModel) model;
		
		File tmpfile = File.createTempFile("gpmnsave", ".gpmn");
		PrintStream ps = new PrintStream(tmpfile, "UTF-8");
		int ind = 0;
		printlnIndent(ps, ind, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		printlnIndent(ps, ind++, "<gpmn:gpmn xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:gpmn=\"http://jadex.sourceforge.net/gpmn\" version=\"" + VERSION + "\">");
		
		printlnIndent(ps, ind++, "<gpmn:gpmnmodel>");
		
		List<IParameter> cp = gm.getContext().getParameters();
		if (cp.size() > 0)
		{
			printlnIndent(ps, ind++, "<gpmn:context>");
			
			for (IParameter p : cp)
			{
				printlnIndent(ps, ind++, "<gpmn:parameter>");
				
				printIndent(ps, ind, "<gpmn:parametername>");
				ps.print(p.getName());
				ps.println("</gpmn:parametername>");
				
				printIndent(ps, ind, "<gpmn:parametertype>");
				ps.print(p.getType());
				ps.println("</gpmn:parametertype>");
				
				printIndent(ps, ind, "<gpmn:parametervalue>");
				ps.print(p.getValue());
				ps.println("</gpmn:parametervalue>");
				
				if (p.isSet())
				{
					printlnIndent(ps, ind, "<gpmn:parameterset />");
				}
				
				printlnIndent(ps, --ind, "</gpmn:parameter>");
			}
			
			printlnIndent(ps, --ind, "</gpmn:context>");
		}
		
		printlnIndent(ps, ind++, "<gpmn:goals>");
		Set<INode> goals = gm.getNodeSet(IGoal.class);
		for (INode node : goals)
		{
			Goal goal = (Goal) node;
			printlnIndent(ps, ind++, "<gpmn:goal>");
			
			printIndent(ps, ind, "<gpmn:goalname>");
			ps.print(goal.getName());
			ps.println("</gpmn:goalname>");
			
			if (!ModelConstants.DEFAULT_GOAL_TYPE.equals(goal.getGoalType()))
			{
				printIndent(ps, ind, "<gpmn:goaltype>");
				ps.print(goal.getGoalType());
				ps.println("</gpmn:goaltype>");
			}
			
			if (goal.getCreationCondition() != null)
			{
				printIndent(ps, ind, "<gpmn:creationcondition");
				if (goal.getCreationConditionLanguage() != null)
				{
					ps.print(" language=\"");
					ps.print(goal.getCreationConditionLanguage());
					ps.print("\"");
				}
				ps.print(">");
				ps.print(goal.getCreationCondition());
				ps.println("</gpmn:creationcondition>");
			}
			
			if (goal.getContextCondition() != null)
			{
				printIndent(ps, ind, "<gpmn:contextcondition");
				if (goal.getContextConditionLanguage() != null)
				{
					ps.print(" language=\"");
					ps.print(goal.getContextConditionLanguage());
					ps.print("\"");
				}
				ps.print(">");
				ps.print(goal.getContextCondition());
				ps.println("</gpmn:contextcondition>");
			}
			
			if (goal.getDropCondition() != null)
			{
				printIndent(ps, ind, "<gpmn:dropcondition");
				if (goal.getDropConditionLanguage() != null)
				{
					ps.print(" language=\"");
					ps.print(goal.getDropConditionLanguage());
					ps.print("\"");
				}
				ps.print(">");
				ps.print(goal.getDropCondition());
				ps.println("</gpmn:dropcondition>");
			}
			
			if (goal.getTargetCondition() != null &&
				ModelConstants.ACHIEVE_GOAL_TYPE.equals(goal.getGoalType()))
			{
				printIndent(ps, ind, "<gpmn:targetcondition");
				if (goal.getTargetConditionLanguage() != null)
				{
					ps.print(" language=\"");
					ps.print(goal.getTargetConditionLanguage());
					ps.print("\"");
				}
				ps.print(">");
				ps.print(goal.getTargetCondition());
				ps.println("</gpmn:targetcondition>");
			}
			
			if (goal.getFailureCondition() != null)
			{
				printIndent(ps, ind, "<gpmn:targetcondition");
				if (goal.getFailureConditionLanguage() != null)
				{
					ps.print(" language=\"");
					ps.print(goal.getFailureConditionLanguage());
					ps.print("\"");
				}
				ps.print(">");
				ps.print(goal.getFailureCondition());
				ps.println("</gpmn:targetcondition>");
			}
			
			if (goal.getMaintainCondition() != null &&
				ModelConstants.MAINTAIN_GOAL_TYPE.equals(goal.getGoalType()))
			{
				printIndent(ps, ind, "<gpmn:maintaincondition");
				if (goal.getMaintainConditionLanguage() != null)
				{
					ps.print(" language=\"");
					ps.print(goal.getMaintainConditionLanguage());
					ps.print("\"");
				}
				ps.print(">");
				ps.print(goal.getMaintainCondition());
				ps.println("</gpmn:maintaincondition>");
			}
			
			if (goal.getDeliberation() != null)
			{
				printIndent(ps, ind, "<gpmn:deliberation>");
				ps.print(goal.getDeliberation());
				ps.println("</gpmn:deliberation>");
			}
			
			if (goal.getExclude() != null)
			{
				printIndent(ps, ind, "<gpmn:exclude>");
				ps.print(goal.getExclude());
				ps.println("</gpmn:exclude>");
			}
			
			if (goal.isPostToAll())
			{
				printlnIndent(ps, ind, "<gpmn:posttoall />");
			}
			
			if (goal.isRandomSelection())
			{
				printlnIndent(ps, ind, "<gpmn:randomselection />");
			}
			
			if (goal.isRecalculate())
			{
				printlnIndent(ps, ind, "<gpmn:recalculate />");
			}
			
			if (goal.isRecur())
			{
				printlnIndent(ps, ind, "<gpmn:recur delay=\"");
				ps.print(goal.getRecurDelay());
				ps.println("\" />");
			}
			
			if (goal.isRetry())
			{
				printIndent(ps, ind, "<gpmn:retry delay=\"");
				ps.print(goal.getRetryDelay());
				ps.println("\" />");
			}
			
			printlnIndent(ps, --ind, "</gpmn:goal>");
		}
		printlnIndent(ps, --ind, "</gpmn:goals>");
		
		ps.println();
		
		printlnIndent(ps, ind++, "<gpmn:plans>");
		printlnIndent(ps, ind++, "<gpmn:activationplans>");
		Set<INode> plans = gm.getNodeSet(IActivationPlan.class);
		for (INode node : plans)
		{
			printlnIndent(ps, ind++, "<gpmn:activationplan>");
			ActivationPlan plan = (ActivationPlan) node;
			
			printIndent(ps, ind, "<gpmn:planname>");
			ps.print(plan.getName());
			ps.println("</gpmn:planname>");
			
			if (!ModelConstants.ACTIVATION_MODE_DEFAULT.equals(plan.getMode()))
			{
				printIndent(ps, ind, "<gpmn:activationmode>");
				ps.print(plan.getMode());
				ps.println("</gpmn:activationmode>");
			}
			
			printlnIndent(ps, --ind, "</gpmn:activationplan>");
		}
		printlnIndent(ps, --ind, "</gpmn:activationplans>");
		
		printlnIndent(ps, ind++, "<gpmn:bpmnplans>");
		plans = gm.getNodeSet(IBpmnPlan.class);
		for (INode node : plans)
		{
			printlnIndent(ps, ind++, "<gpmn:bpmnplan>");
			BpmnPlan plan = (BpmnPlan) node;
			
			printIndent(ps, ind, "<gpmn:planname>");
			ps.print(plan.getName());
			ps.println("</gpmn:planname>");
			
			//TODO: Throw exception if planref is undefined?
			if (plan.getPlanref() != null)
			{
				printIndent(ps, ind, "<gpmn:planref>");
				ps.print(plan.getPlanref());
				ps.println("</gpmn:planref>");
			}
			
			printlnIndent(ps, --ind, "</gpmn:bpmnplan>");
		}
		printlnIndent(ps, --ind, "</gpmn:bpmnplans>");
		printlnIndent(ps, --ind, "</gpmn:plans>");
		
		ps.println();
		
		printlnIndent(ps, ind++, "<gpmn:edges>");
		Set<IEdge> edges = gm.getEdgeSet(IActivationEdge.class);
		if (edges.size() > 0)
		{
			printlnIndent(ps, ind++, "<gpmn:activationedges>");
			for (IEdge iedge : edges)
			{
				printIndent(ps, ind++, "<gpmn:activationedge");
				ActivationEdge edge = (ActivationEdge) iedge;
				ActivationPlan aplan = (ActivationPlan) edge.getSource();
				if (ModelConstants.ACTIVATION_MODE_SEQUENTIAL.equals(aplan.getMode()))
				{
					ps.print(" order=\"");
					ps.print(edge.getOrder());
					ps.print("\"");
				}
				ps.println(">");
				
				printIndent(ps, ind, "<gpmn:edgename>");
				ps.print(edge.getName());
				ps.println("</gpmn:edgename>");
				
				printIndent(ps, ind, "<gpmn:sourcename>");
				ps.print(aplan.getName());
				ps.println("</gpmn:sourcename>");
				
				Goal goal = (Goal) edge.getTarget();
				printIndent(ps, ind, "<gpmn:targetname>");
				ps.print(goal.getName());
				ps.println("</gpmn:targetname>");
				
				printlnIndent(ps, --ind, "</gpmn:activationedge>");
			}
			printlnIndent(ps, --ind, "</gpmn:activationedges>");
		}
		
		edges = gm.getEdgeSet(IPlanEdge.class);
		if (edges.size() > 0)
		{
			printlnIndent(ps, ind++, "<gpmn:planedges>");
			for (IEdge iedge : edges)
			{
				PlanEdge edge = (PlanEdge) iedge;
				printlnIndent(ps, ind++, "<gpmn:planedge>");
				
				printIndent(ps, ind, "<gpmn:edgename>");
				ps.print(edge.getName());
				ps.println("</gpmn:edgename>");
				
				Goal goal = (Goal) edge.getSource();
				printIndent(ps, ind, "<gpmn:sourcename>");
				ps.print(goal.getName());
				ps.println("</gpmn:sourcename>");
				
				AbstractPlan plan = (AbstractPlan) edge.getTarget();
				printIndent(ps, ind, "<gpmn:targetname>");
				ps.print(plan.getName());
				ps.println("</gpmn:targetname>");
				
				printlnIndent(ps, --ind, "</gpmn:planedge>");
			}
			printlnIndent(ps, --ind, "</gpmn:planedges>");
		}
		
		edges = gm.getEdgeSet(ISuppressionEdge.class);
		if (edges.size() > 0)
		{
			printlnIndent(ps, ind++, "<gpmn:suppressionedges>");
			for (IEdge iedge : edges)
			{
				SuppressionEdge edge = (SuppressionEdge) iedge;
				printlnIndent(ps, ind++, "<gpmn:suppressionedge>");
				
				printIndent(ps, ind, "<gpmn:edgename>");
				ps.print(edge.getName());
				ps.println("</gpmn:edgename>");
				
				Goal goal = (Goal) edge.getSource();
				printIndent(ps, ind, "<gpmn:sourcename>");
				ps.print(goal.getName());
				ps.println("</gpmn:sourcename>");
				
				goal = (Goal) edge.getTarget();
				printIndent(ps, ind, "<gpmn:targetname>");
				ps.print(goal.getName());
				ps.println("</gpmn:targetname>");
				
				printlnIndent(ps, --ind, "</gpmn:suppressionedge>");
			}
			printlnIndent(ps, --ind, "</gpmn:suppressionedges>");
		}
		printlnIndent(ps, --ind, "</gpmn:edges>");
		
		printlnIndent(ps, --ind, "</gpmn:gpmnmodel>");
		
		ps.println();
		
		printlnIndent(ps, ind++, "<gpmn:visualmodel>");
		// TODO: Hack?
		Object root = ((mxCell) graph.getModel().getRoot()).getChildAt(0);
		
		List<VGoal> vgoals = new ArrayList<VGoal>();
		List<VPlan> vplans = new ArrayList<VPlan>();
		List<VEdge> vedges = new ArrayList<VEdge>();
		List<VVirtualActivationEdge> virtactedges = new ArrayList<VVirtualActivationEdge>();
		for (int i = 0; i < graph.getModel().getChildCount(root); ++i)
		{
			VElement element = (VElement) graph.getModel().getChildAt(root, i);
			
			if (element instanceof VGoal)
			{
				vgoals.add((VGoal) element);
			}
			else if (element instanceof VPlan)
			{
				vplans.add((VPlan) element);
			}
			else if (element instanceof VVirtualActivationEdge)
			{
				virtactedges.add((VVirtualActivationEdge) element);
			}
			else if (element instanceof VEdge)
			{
				vedges.add((VEdge) element);
			}
		}
		
		if (vgoals.size() > 0)
		{
			printlnIndent(ps, ind++, "<gpmn:vgoals>");
			
			for (VGoal goal : vgoals)
			{
				printIndent(ps, ind++, "<gpmn:vgoal x=\"");
				mxGeometry geo = goal.getGeometry();
				ps.print(geo.getX());
				ps.print("\" y=\"");
				ps.print(geo.getY());
				ps.println("\">");
				
				printIndent(ps, ind, "<gpmn:goalname>");
				ps.print(goal.getGoal().getName());
				ps.println("</gpmn:goalname>");
				
				printlnIndent(ps, --ind, "</gpmn:vgoal>");
			}
			
			printlnIndent(ps, --ind, "</gpmn:vgoals>");
		}
		
		if (vplans.size() > 0)
		{
			printlnIndent(ps, ind++, "<gpmn:vplans>");
			
			for (VPlan plan : vplans)
			{
				printIndent(ps, ind++, "<gpmn:vplan x=\"");
				mxGeometry geo = plan.getGeometry();
				ps.print(geo.getX());
				ps.print("\" y=\"");
				ps.print(geo.getY());
				if (!plan.isVisible())
				{
					ps.print("\" visible=\"false");
				}
				ps.println("\">");
				
				printIndent(ps, ind, "<gpmn:planname>");
				ps.print(plan.getPlan().getName());
				ps.println("</gpmn:planname>");
				
				printlnIndent(ps, --ind, "</gpmn:vplan>");
			}
			
			printlnIndent(ps, --ind, "</gpmn:vplans>");
		}
		
		if (vedges.size() > 0)
		{
			printlnIndent(ps, ind++, "<gpmn:vedges>");
			
			for (VEdge edge : vedges)
			{
				printlnIndent(ps, ind++, "<gpmn:vedge>");
				
				printIndent(ps, ind, "<gpmn:edgename>");
				ps.print(edge.getEdge().getName());
				ps.println("</gpmn:edgename>");
				
				printlnIndent(ps, --ind, "</gpmn:vedge>");
			}
			
			printlnIndent(ps, --ind, "</gpmn:vedges>");
		}
		
		if (virtactedges.size() > 0)
		{
			printlnIndent(ps, ind++, "<gpmn:virtualactivationedges>");
			
			for (VVirtualActivationEdge edge : virtactedges)
			{
				printlnIndent(ps, ind++, "<gpmn:virtualactivationedge>");
				
				printIndent(ps, ind, "<gpmn:planname>");
				ps.print(edge.getPlan().getPlan().getName());
				ps.println("</gpmn:planname>");
				
				Goal goal = (Goal) ((VGoal) edge.getSource()).getGoal();
				printIndent(ps, ind, "<gpmn:sourcename>");
				ps.print(goal.getName());
				ps.println("</gpmn:sourcename>");
				
				goal = (Goal) ((VGoal) edge.getTarget()).getGoal();
				printIndent(ps, ind, "<gpmn:targetname>");
				ps.print(goal.getName());
				ps.println("</gpmn:targetname>");
				
				printlnIndent(ps, --ind, "</gpmn:virtualactivationedge>");
			}
			
			printlnIndent(ps, --ind, "</gpmn:virtualactivationedges>");
		}
		
		printlnIndent(ps, --ind, "</gpmn:visualmodel>");
		
		printlnIndent(ps, --ind, "</gpmn:gpmn>");
		ps.close();
		
		tmpfile.renameTo(file);
	}
	
	/**
	 *  Loads the model from a file.
	 *  
	 *  @param file The model file.
	 *  @param graph The visual graph.
	 */
	public mxIGraphModel readModel(File file) throws Exception
	{
		FileInputStream fis = new FileInputStream(file);
		XMLInputFactory fac = XMLInputFactory.newInstance(); 
		XMLStreamReader reader = fac.createXMLStreamReader(fis);
		model.clear();
		mxIGraphModel graphmodel = new mxGraphModel();
		Object root = graphmodel.getRoot();
		Object parent = graphmodel.getChildAt(root, 0);
		
		Map<String, Goal> goals = new HashMap<String, Goal>();
		Map<String, AbstractPlan> plans = new HashMap<String, AbstractPlan>();
		Map<String, AbstractEdge> edges = new HashMap<String, AbstractEdge>();
		Map<String, VGoal> vgoals = new HashMap<String, VGoal>();
		Map<String, VPlan> vplans = new HashMap<String, VPlan>();
		Map<String, List<VVirtualActivationEdge>> groups = new HashMap<String, List<VVirtualActivationEdge>>();
		
		Object current = null;
		String localname = null;
		while (reader.hasNext())
		{
		    reader.next();
		    if (reader.getEventType() == XMLStreamReader.START_ELEMENT)
		    {
		    	localname = reader.getLocalName();
		    	if ("parameter".equals(localname))
		    	{
		    		current = new Parameter();
		    	}
		    	else if ("goal".equals(localname))
		    	{
		    		Goal goal = (Goal) model.createNode(IGoal.class);
		    		goal.setRetry(false);
		    		current = goal;
		    	}
		    	else if ("posttoall".equals(localname))
		    	{
		    		((Goal) current).setPostToAll(true);
		    	}
		    	else if ("randomselection".equals(localname))
		    	{
		    		((Goal) current).setRandomSelection(true);
		    	}
		    	else if ("recalculate".equals(localname))
		    	{
		    		((Goal) current).setRecalculate(true);
		    	}
		    	else if ("recur".equals(localname))
		    	{
		    		((Goal) current).setRecur(true);
		    		String delaystr = reader.getAttributeValue("", "delay");
		    		if (delaystr != null)
		    		{
		    			int delay = Integer.parseInt(delaystr);
		    			((Goal) current).setRecurDelay(delay);
		    		}
		    	}
		    	else if ("retry".equals(localname))
		    	{
		    		((Goal) current).setRetry(true);
		    		String delaystr = reader.getAttributeValue("", "delay");
		    		if (delaystr != null)
		    		{
		    			int delay = Integer.parseInt(delaystr);
		    			((Goal) current).setRetryDelay(delay);
		    		}
		    	}
		    	else if ("activationplan".equals(localname))
		    	{
		    		ActivationPlan plan = (ActivationPlan) model.createNode(IActivationPlan.class);
		    		current = plan;
		    	}
		    	else if ("bpmnplan".equals(localname))
		    	{
		    		BpmnPlan plan = (BpmnPlan) model.createNode(IBpmnPlan.class);
		    		current = plan;
		    	}
		    	else if ("activationedge".equals(localname))
		    	{
		    		Object[] obj = new Object[4];
		    		obj[0] = IActivationEdge.class;
		    		String orderstr = reader.getAttributeValue("", "order");
		    		if (orderstr != null)
		    		{
		    			obj[3] = Integer.parseInt(orderstr);
		    		}
		    		current = obj;
		    	}
		    	else if ("planedge".equals(localname))
		    	{
		    		current = new Object[3];
		    		((Object[]) current)[0] = IPlanEdge.class;
		    	}
		    	else if ("suppressionedge".equals(localname))
		    	{
		    		current = new Object[3];
		    		((Object[]) current)[0] = ISuppressionEdge.class;
		    	}
		    	else if ("creationcondition".equals(localname))
		    	{
		    		Goal goal = (Goal) current;
		    		String lang = reader.getAttributeValue("", "language");
		    		if (lang != null)
		    		{
		    			goal.setCreationConditionLanguage(lang);
		    		}
		    	}
		    	else if ("contextcondition".equals(localname))
		    	{
		    		Goal goal = (Goal) current;
		    		String lang = reader.getAttributeValue("", "language");
		    		if (lang != null)
		    		{
		    			goal.setContextConditionLanguage(lang);
		    		}
		    	}
		    	else if ("dropcondition".equals(localname))
		    	{
		    		Goal goal = (Goal) current;
		    		String lang = reader.getAttributeValue("", "language");
		    		if (lang != null)
		    		{
		    			goal.setDropConditionLanguage(lang);
		    		}
		    	}
		    	else if ("targetcondition".equals(localname))
		    	{
		    		Goal goal = (Goal) current;
		    		String lang = reader.getAttributeValue("", "language");
		    		if (lang != null)
		    		{
		    			goal.setTargetConditionLanguage(lang);
		    		}
		    	}
		    	else if ("failurecondition".equals(localname))
		    	{
		    		Goal goal = (Goal) current;
		    		String lang = reader.getAttributeValue("", "language");
		    		if (lang != null)
		    		{
		    			goal.setFailureConditionLanguage(lang);
		    		}
		    	}
		    	else if ("maintaincondition".equals(localname))
		    	{
		    		Goal goal = (Goal) current;
		    		String lang = reader.getAttributeValue("", "language");
		    		if (lang != null)
		    		{
		    			goal.setMaintainConditionLanguage(lang);
		    		}
		    	}
		    	else if ("vgoal".equals(localname))
		    	{
		    		double x = Double.parseDouble(reader.getAttributeValue("", "x"));
		    		double y = Double.parseDouble(reader.getAttributeValue("", "y"));
		    		VGoal vgoal = new VGoal(null, new mxPoint(x, y));
		    		current = vgoal;
		    	}
		    	else if ("vplan".equals(localname))
		    	{
		    		Object[] obj = new Object[3];
		    		obj[0] = "vplan";
		    		double x = Double.parseDouble(reader.getAttributeValue("", "x"));
		    		double y = Double.parseDouble(reader.getAttributeValue("", "y"));
		    		obj[1] = new mxPoint(x, y);
		    		String visiblestr = reader.getAttributeValue("", "visible");
		    		if (visiblestr != null)
		    		{
		    			obj[2] = (Boolean.parseBoolean(visiblestr));
		    		}
		    		current = obj;
		    	}
		    	else if ("vedge".equals(localname))
		    	{
		    		current = "vedge";
		    	}
		    	else if ("virtualactivationedge".equals(localname))
		    	{
		    		current = new Object[3];
		    		((Object[]) current)[0] = "virtualactivationedge";
		    	}
		    }
		    else if (reader.getEventType() == XMLStreamReader.END_ELEMENT)
		    {
		    	if ("parameter".equals(reader.getLocalName()))
		    	{
		    		model.getContext().addParameter((Parameter) current);
		    	}
		    	localname = null;
		    }
		    else if (reader.getEventType() == XMLStreamReader.CHARACTERS)
		    {
		    	if ("parametername".equals(localname))
		    	{
		    		String name = reader.getText();
		    		((Parameter) current).setName(name);
		    	}
		    	else if ("parametertype".equals(localname))
		    	{
		    		String type = reader.getText();
		    		((Parameter) current).setType(type);
		    	}
		    	else if ("parametervalue".equals(localname))
		    	{
		    		String val = reader.getText();
		    		((Parameter) current).setValue(val);
		    	}
		    	else if ("parameterset".equals(localname))
		    	{
		    		((Parameter) current).setSet(true);
		    	}
		    	else if ("goalname".equals(localname))
		    	{
		    		String name = reader.getText();
		    		if (current instanceof Goal)
		    		{
			    		Goal goal = (Goal) current;
			    		goal.setName(name);
			    		goals.put(goal.getName(), goal);
		    		}
		    		else if (current instanceof VGoal)
		    		{
		    			VGoal vgoal = (VGoal) current;
		    			Goal goal = goals.get(name);
		    			vgoal.setValue(goal);
		    			vgoals.put(name, vgoal);
		    			graphmodel.beginUpdate();
		    			graphmodel.add(parent, vgoal, graphmodel.getChildCount(parent));
		    			graphmodel.endUpdate();
		    			/*graph.getModel().beginUpdate();
		    			graph.addCell(vgoal);
		    			graph.getModel().endUpdate();*/
		    		}
		    	}
		    	else if ("goaltype".equals(localname))
		    	{
		    		String goaltype = reader.getText();
		    		//TODO: Catch invalid types
		    		Goal goal = (Goal) current;
		    		goal.setGoalType(goaltype);
		    	}
		    	else if ("creationcondition".equals(localname))
		    	{
		    		String condition = reader.getText();
		    		Goal goal = (Goal) current;
		    		goal.setCreationCondition(condition);
		    	}
		    	else if ("contextcondition".equals(localname))
		    	{
		    		String condition = reader.getText();
		    		Goal goal = (Goal) current;
		    		goal.setContextCondition(condition);
		    	}
		    	else if ("dropcondition".equals(localname))
		    	{
		    		String condition = reader.getText();
		    		Goal goal = (Goal) current;
		    		goal.setDropCondition(condition);
		    	}
		    	else if ("targetcondition".equals(localname))
		    	{
		    		String condition = reader.getText();
		    		Goal goal = (Goal) current;
		    		goal.setTargetCondition(condition);
		    	}
		    	else if ("failurecondition".equals(localname))
		    	{
		    		String condition = reader.getText();
		    		Goal goal = (Goal) current;
		    		goal.setFailureCondition(condition);
		    	}
		    	else if ("maintaincondition".equals(localname))
		    	{
		    		String condition = reader.getText();
		    		Goal goal = (Goal) current;
		    		goal.setMaintainCondition(condition);
		    	}
		    	else if ("deliberation".equals(localname))
		    	{
		    		String val = reader.getText();
		    		Goal goal = (Goal) current;
		    		goal.setDeliberation(val);
		    	}
		    	else if ("exclude".equals(localname))
		    	{
		    		String val = reader.getText();
		    		Goal goal = (Goal) current;
		    		goal.setExclude(val);
		    	}
		    	else if ("planname".equals(localname))
		    	{
		    		String name = reader.getText();
		    		if (current instanceof AbstractPlan)
		    		{
			    		AbstractPlan plan = (AbstractPlan) current;
			    		plan.setName(name);
			    		plans.put(plan.getName(), plan);
		    		}
		    		else if (current instanceof Object[] && "vplan".equals(((Object[]) current)[0]))
		    		{
		    			Object[] obj = (Object[]) current;
		    			mxPoint pos = (mxPoint) obj[1];
		    			AbstractPlan plan = plans.get(name);
		    			VPlan vplan = new VPlan(plan, pos);
		    			if (obj[2] != null)
		    			{
		    				vplan.setVisible(((Boolean) obj[2]).booleanValue());
		    			}
		    			
		    			vplans.put(name, vplan);
		    			graphmodel.beginUpdate();
		    			graphmodel.add(parent, vplan, graphmodel.getChildCount(parent));
		    			graphmodel.endUpdate();
		    			/*graph.getModel().beginUpdate();
		    			graph.addCell(vplan);
		    			graph.getModel().endUpdate();*/
		    		}
		    		else if (current instanceof Object[] && "virtualactivationedge".equals(((Object[]) current)[0]))
		    		{
		    			((Object[]) current)[1] = name;
		    		}
		    	}
		    	else if ("activationmode".equals(localname))
		    	{
		    		String mode = reader.getText();
		    		((ActivationPlan) current).setMode(mode);
		    	}
		    	else if ("planref".equals(localname))
		    	{
		    		String ref = reader.getText();
		    		((BpmnPlan) current).setPlanref(ref);
		    	}
		    	else if ("edgename".equals(localname))
		    	{
		    		String name = reader.getText();
		    		if ("vedge".equals(current))
		    		{
		    			AbstractEdge edge = edges.get(name);
		    			String sourcename = edge.getSource().getName();
		    			String targetname = edge.getTarget().getName();
		    			VElement source = vgoals.get(sourcename);
		    			if (source == null)
		    			{
		    				source = vplans.get(sourcename);
		    			}
		    			VElement target = vgoals.get(targetname);
		    			if (target == null)
		    			{
		    				target = vplans.get(targetname);
		    			}
		    			VEdge vedge = new VEdge(source, target, edge);
		    			current = vedge;
		    			graphmodel.beginUpdate();
		    			graphmodel.add(parent, vedge, graphmodel.getChildCount(parent));
		    			graphmodel.endUpdate();
		    			//graph.addCell(vedge);
		    		}
		    		else if (current instanceof Object[])
		    		{
		    			((Object[]) current)[1] = name;
		    		}
		    	}
		    	else if ("sourcename".equals(localname))
		    	{
		    		String name = reader.getText();
		    		((Object[]) current)[2] = name;
		    	}
		    	else if ("targetname".equals(localname))
		    	{
		    		Object[] obj = (Object[]) current;
		    		String targetname = reader.getText();
		    		String sourcename = (String) obj[2];
		    		if (current instanceof Object[] && "virtualactivationedge".equals(((Object[]) current)[0]))
		    		{
		    			String planname = (String) obj[1];
		    			VPlan plan = vplans.get(planname);
		    			VElement source = vgoals.get(sourcename);
		    			if (source == null)
		    			{
		    				source = vplans.get(sourcename);
		    			}
		    			VElement target = vgoals.get(targetname);
		    			if (target == null)
		    			{
		    				target = vplans.get(targetname);
		    			}
		    			
		    			List<VVirtualActivationEdge> group = groups.get(planname);
		    			if (group == null)
		    			{
		    				group = new ArrayList<VVirtualActivationEdge>();
		    				groups.put(planname, group);
		    			}
		    			
		    			VVirtualActivationEdge edge = new VVirtualActivationEdge(source, target, group, plan);
		    			group.add(edge);
		    			
		    			graphmodel.beginUpdate();
		    			graphmodel.add(parent, edge, graphmodel.getChildCount(parent));
		    			graphmodel.endUpdate();
		    			/*graph.getModel().beginUpdate();
		    			graph.addCell(edge);
		    			graph.getModel().endUpdate();*/
		    		}
		    		else
		    		{
			    		IElement source = goals.get(sourcename);
			    		if (source == null)
			    		{
			    			source = plans.get(sourcename);
			    		}
			    		IElement target = goals.get(targetname);
			    		if (target == null)
			    		{
			    			target = plans.get(targetname);
			    		}
			    		IEdge edge = null;
			    		edge = model.createEdge(source, target, (Class) obj[0]);
			    		String name = (String) obj[1];
			    		edge.setName(name);
			    		edges.put(name, (AbstractEdge) edge);
			    		if (edge instanceof ActivationEdge && obj[3] != null)
			    		{
			    			((ActivationEdge) edge).setOrder(((Integer) obj[3]).intValue());
			    		}
		    		}
		    	}
		    }
		}
		
		return graphmodel;
	}
	
	protected void printlnIndent(PrintStream ps, int num, String line)
	{
		ps.println(getIndent(num).append(line).toString());
	}
	
	protected void printIndent(PrintStream ps, int num, String line)
	{
		ps.print(getIndent(num).append(line).toString());
	}
	
	protected StringBuilder getIndent(int num)
	{
		StringBuilder sb = new StringBuilder();
		while (num-- > 0)
		{
			sb.append("  ");
		}
		return sb;
	}
}
