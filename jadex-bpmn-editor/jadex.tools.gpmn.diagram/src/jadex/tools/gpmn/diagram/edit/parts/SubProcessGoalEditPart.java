/*
 * Copyright (c) 2009, Universität Hamburg
 * All rights reserved. This program and the accompanying 
 * materials are made available under the terms of the 
 * ###_LICENSE_REPLACEMENT_MARKER_###
 * which accompanies this distribution, and is available at
 * ###_LICENSE_URL_REPLACEMENT_MARKER_###
 */
package jadex.tools.gpmn.diagram.edit.parts;

import jadex.tools.gpmn.Goal;
import jadex.tools.gpmn.GoalType;
import jadex.tools.gpmn.GpmnPackage;
import jadex.tools.gpmn.SubProcessGoal;
import jadex.tools.gpmn.diagram.edit.policies.SubProcessGoalItemSemanticEditPolicy;
import jadex.tools.gpmn.diagram.part.GpmnDiagramMessages;
import jadex.tools.gpmn.diagram.part.GpmnVisualIDRegistry;
import jadex.tools.gpmn.diagram.providers.GpmnElementTypes;
import jadex.tools.gpmn.figures.GPMNNodeFigure;
import jadex.tools.gpmn.figures.GpmnShapesDefaultSizes;
import jadex.tools.gpmn.figures.connectionanchors.impl.ConnectionAnchorFactory;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.Shape;
import org.eclipse.draw2d.StackLayout;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.LayoutEditPolicy;
import org.eclipse.gef.editpolicies.NonResizableEditPolicy;
import org.eclipse.gef.requests.CreateRequest;
import org.eclipse.gmf.runtime.diagram.ui.editparts.IGraphicalEditPart;
import org.eclipse.gmf.runtime.diagram.ui.editpolicies.EditPolicyRoles;
import org.eclipse.gmf.runtime.draw2d.ui.figures.ConstrainedToolbarLayout;
import org.eclipse.gmf.runtime.draw2d.ui.figures.WrappingLabel;
import org.eclipse.gmf.runtime.emf.type.core.IElementType;
import org.eclipse.gmf.runtime.gef.ui.figures.NodeFigure;
import org.eclipse.gmf.runtime.notation.View;
import org.eclipse.stp.bpmn.figures.connectionanchors.IConnectionAnchorFactory;
import org.eclipse.swt.graphics.Color;

/**
 * @generated NOT
 */
public class SubProcessGoalEditPart extends AbstractEditPartSupport
{

	/**
	 * @generated
	 */
	public static final int VISUAL_ID = 2009;

	/**
	 * @generated
	 */
	protected IFigure contentPane;

	/**
	 * @generated
	 */
	protected IFigure primaryShape;

	/**
	 * @generated
	 */
	public SubProcessGoalEditPart(View view)
	{
		super(view);
	}

	/**
	 * @generated
	 */
	protected void createDefaultEditPolicies()
	{
		super.createDefaultEditPolicies();
		installEditPolicy(EditPolicyRoles.SEMANTIC_ROLE,
				new SubProcessGoalItemSemanticEditPolicy());
		installEditPolicy(EditPolicy.LAYOUT_ROLE, createLayoutEditPolicy());
		// XXX need an SCR to runtime to have another abstract superclass that would let children add reasonable editpolicies
		// removeEditPolicy(org.eclipse.gmf.runtime.diagram.ui.editpolicies.EditPolicyRoles.CONNECTION_HANDLES_ROLE);
	}

	/**
	 * @generated
	 */
	protected LayoutEditPolicy createLayoutEditPolicy()
	{
		LayoutEditPolicy lep = new LayoutEditPolicy()
		{

			protected EditPolicy createChildEditPolicy(EditPart child)
			{
				EditPolicy result = child
						.getEditPolicy(EditPolicy.PRIMARY_DRAG_ROLE);
				if (result == null)
				{
					result = new NonResizableEditPolicy();
				}
				return result;
			}

			protected Command getMoveChildrenCommand(Request request)
			{
				return null;
			}

			protected Command getCreateCommand(CreateRequest request)
			{
				return null;
			}
		};
		return lep;
	}

	/**
	 * @generated
	 */
	protected IFigure createNodeShapeGen()
	{
		SubProcessGoalFigure figure = new SubProcessGoalFigure();
		return primaryShape = figure;
	}

	/**
	 * @generated NOT
	 */
	protected IFigure createNodeShape()
	{
		SubProcessGoalFigure goalFigure = (SubProcessGoalFigure) createNodeShapeGen();
		SubProcessGoal goal = (SubProcessGoal) getPrimaryView().getElement();

		if (isGoalRefSet())
		{
			goalFigure.setLinked(true);
		}

		setLabelAndLayout(goalFigure, goal);

		return primaryShape = goalFigure;
	}

	/**
	 * Update the figure with proper label and layout
	 * 
	 * @param goalFigure
	 * @param goal
	 * @return true if refreshVisual is recommended (there was a change)
	 * 
	 * @generated NOT
	 */
	private boolean setLabelAndLayout(SubProcessGoalFigure goalFigure,
			SubProcessGoal goal)
	{
		boolean res = false;
		WrappingLabel wl = goalFigure.getFigureGoalNameFigure();
		wl.setTextWrap(true);

		if (goal.getName() == null)
		{

			if (!GpmnDiagramMessages.PlanEditPart_plan_default_name.equals(wl
					.getText()))
			{
				wl.setText(GpmnDiagramMessages.PlanEditPart_plan_default_name);
			}
			res = true;
		}

		return setAlignments(goalFigure, goal, wl, res);
	}

	/**
	 * Align label in figure
	 * 
	 * @param goalFigure
	 * @param goal
	 * @param wl
	 * @param res
	 * @return
	 */
	private boolean setAlignments(SubProcessGoalFigure goalFigure, Goal goal,
			WrappingLabel wl, boolean res)
	{

		if (!(goalFigure.getLayoutManager() instanceof StackLayout))
		{
			StackLayout layout = new StackLayout();
			goalFigure.setLayoutManager(layout);
			res = true;
		}
		wl.setAlignment(PositionConstants.CENTER);
		wl.setTextJustification(PositionConstants.CENTER);

		goalFigure.invalidate();
		return res;
	}

	/**
	 * @generated
	 */
	public SubProcessGoalFigure getPrimaryShape()
	{
		return (SubProcessGoalFigure) primaryShape;
	}

	/**
	 * @generated
	 */
	protected boolean addFixedChild(EditPart childEditPart)
	{
		if (childEditPart instanceof SubProcessGoalNameEditPart)
		{
			((SubProcessGoalNameEditPart) childEditPart)
					.setLabel(getPrimaryShape().getFigureGoalNameFigure());
			return true;
		}
		return false;
	}

	/**
	 * @generated
	 */
	protected boolean removeFixedChild(EditPart childEditPart)
	{
		if (childEditPart instanceof SubProcessGoalNameEditPart)
		{
			return true;
		}
		return false;
	}

	/**
	 * @generated
	 */
	protected void addChildVisual(EditPart childEditPart, int index)
	{
		if (addFixedChild(childEditPart))
		{
			return;
		}
		super.addChildVisual(childEditPart, -1);
	}

	/**
	 * @generated
	 */
	protected void removeChildVisual(EditPart childEditPart)
	{
		if (removeFixedChild(childEditPart))
		{
			return;
		}
		super.removeChildVisual(childEditPart);
	}

	/**
	 * @generated
	 */
	protected IFigure getContentPaneFor(IGraphicalEditPart editPart)
	{
		return getContentPane();
	}

	/**
	 * @return An appropriate connection anchor factory
	 * @generated NOT
	 */
	protected IConnectionAnchorFactory getConnectionAnchorFactory()
	{
		return ConnectionAnchorFactory.INSTANCE;
	}

	/**
	 * @generated NOT
	 */
	protected NodeFigure createNodePlate()
	{
		Dimension minSize = (Dimension) getMapMode().DPtoLP(
				GpmnShapesDefaultSizes.GOAL_FIGURE_SIZE);
		return new GPMNNodeFigure(getConnectionAnchorFactory(), minSize);
	}

	/**
	 * Creates figure for this edit part.
	 * 
	 * Body of this method does not depend on settings in generation model
	 * so you may safely remove <i>generated</i> tag and modify it.
	 * 
	 * @generated
	 */
	protected NodeFigure createNodeFigure()
	{
		NodeFigure figure = createNodePlate();
		figure.setLayoutManager(new StackLayout());
		IFigure shape = createNodeShape();
		figure.add(shape);
		contentPane = setupContentPane(shape);
		return figure;
	}

	/**
	 * Default implementation treats passed figure as content pane.
	 * Respects layout one may have set for generated figure.
	 * @param nodeShape instance of generated figure class
	 * @generated
	 */
	protected IFigure setupContentPane(IFigure nodeShape)
	{
		if (nodeShape.getLayoutManager() == null)
		{
			ConstrainedToolbarLayout layout = new ConstrainedToolbarLayout();
			layout.setSpacing(5);
			nodeShape.setLayoutManager(layout);
		}
		return nodeShape; // use nodeShape itself as contentPane
	}

	/**
	 * @generated
	 */
	public IFigure getContentPane()
	{
		if (contentPane != null)
		{
			return contentPane;
		}
		return super.getContentPane();
	}

	/**
	 * @generated
	 */
	protected void setForegroundColor(Color color)
	{
		if (primaryShape != null)
		{
			primaryShape.setForegroundColor(color);
		}
	}

	/**
	 * @generated
	 */
	protected void setBackgroundColor(Color color)
	{
		if (primaryShape != null)
		{
			primaryShape.setBackgroundColor(color);
		}
	}

	/**
	 * @generated
	 */
	protected void setLineWidth(int width)
	{
		if (primaryShape instanceof Shape)
		{
			((Shape) primaryShape).setLineWidth(width);
		}
	}

	/**
	 * @generated
	 */
	protected void setLineType(int style)
	{
		if (primaryShape instanceof Shape)
		{
			((Shape) primaryShape).setLineStyle(style);
		}
	}

	/**
	 * @generated
	 */
	public EditPart getPrimaryChildEditPart()
	{
		return getChildBySemanticHint(GpmnVisualIDRegistry
				.getType(SubProcessGoalNameEditPart.VISUAL_ID));
	}

	/**
	 * @generated
	 */
	public List/*<org.eclipse.gmf.runtime.emf.type.core.IElementType>*/getMARelTypesOnSource()
	{
		List/*<org.eclipse.gmf.runtime.emf.type.core.IElementType>*/types = new ArrayList/*<org.eclipse.gmf.runtime.emf.type.core.IElementType>*/();
		types.add(GpmnElementTypes.SubGoalEdge_4002);
		types.add(GpmnElementTypes.PlanEdge_4003);
		return types;
	}

	/**
	 * @generated NOT
	 */
	public List/*<org.eclipse.gmf.runtime.emf.type.core.IElementType>*/getMARelTypesOnSourceAndTarget(
			IGraphicalEditPart targetEditPart)
	{
		return super.getMARelTypesOnSourceAndTarget(targetEditPart);
	}

	/**
	 * @generated NOT
	 */
	public List/*<org.eclipse.gmf.runtime.emf.type.core.IElementType>*/getMATypesForTarget(
			IElementType relationshipType)
	{
		return super.getMATypesForTarget(relationshipType);
	}

	/**
	 * @generated NOT remove PlanEdge here because the method in EditPolicy doesn't work as expected :-(
	 */
	public List/*<org.eclipse.gmf.runtime.emf.type.core.IElementType>*/getMARelTypesOnTarget()
	{
		List/*<org.eclipse.gmf.runtime.emf.type.core.IElementType>*/types = new ArrayList/*<org.eclipse.gmf.runtime.emf.type.core.IElementType>*/();
		types.add(GpmnElementTypes.Association_4001);
		types.add(GpmnElementTypes.SubGoalEdge_4002);
		types.add(GpmnElementTypes.PlanEdge_4003);
		return types;
	}

	/**
	 * @generated NOT
	 */
	public List/*<org.eclipse.gmf.runtime.emf.type.core.IElementType>*/getMATypesForSource(
			IElementType relationshipType)
	{
		return super.getMATypesForSource(relationshipType);
	}

	/**
	 * @generated
	 */
	protected void handleNotificationEventGen(Notification event)
	{
		if (event.getNotifier() == getModel()
				&& EcorePackage.eINSTANCE.getEModelElement_EAnnotations()
						.equals(event.getFeature()))
		{
			handleMajorSemanticChange();
		}
		else
		{
			super.handleNotificationEvent(event);
		}
	}

	/**
	 * Synchronizes the shape with the goal
	 * 
	 * @generated NOT
	 * @see org.eclipse.gmf.runtime.diagram.ui.editparts.GraphicalEditPart
	 *      #handlePropertyChangeEvent(java.beans.PropertyChangeEvent)
	 */
	protected void handleNotificationEvent(Notification notification)
	{
		if (notification.getEventType() == Notification.SET
				|| notification.getEventType() == Notification.UNSET)
		{
			if (GpmnPackage.eINSTANCE.getPlan_BpmnPlan().equals(
					notification.getFeature()))
			{
				getPrimaryShape().setLinked(isGoalRefSet());
			}
		}

		this.handleNotificationEventGen(notification);
	}

	/**
	 * @generated
	 */
	public class SubProcessGoalFigure extends
			jadex.tools.gpmn.figures.SubProcessGoalFigure
	{

		/**
		 * @generated
		 */
		private WrappingLabel fFigureGoalNameFigure;

		/**
		 * @generated
		 */
		public SubProcessGoalFigure()
		{

			this
					.setGoalType(getPrimaryView().getElement() instanceof Goal ? ((Goal) getPrimaryView()
							.getElement()).getGoalType().getLiteral()
							: GoalType.META_GOAL.getLiteral());

			this.setForegroundColor(ColorConstants.black);
			createContents();
		}

		/**
		 * @generated
		 */
		private void createContents()
		{

			fFigureGoalNameFigure = new WrappingLabel();
			fFigureGoalNameFigure.setText("Goal");

			this.add(fFigureGoalNameFigure);

		}

		/**
		 * @generated
		 */
		private boolean myUseLocalCoordinates = false;

		/**
		 * @generated
		 */
		protected boolean useLocalCoordinates()
		{
			return myUseLocalCoordinates;
		}

		/**
		 * @generated
		 */
		protected void setUseLocalCoordinates(boolean useLocalCoordinates)
		{
			myUseLocalCoordinates = useLocalCoordinates;
		}

		/**
		 * @generated
		 */
		public WrappingLabel getFigureGoalNameFigure()
		{
			return fFigureGoalNameFigure;
		}

	}

	// ---- some helper methods ----

	/**
	 * Check linking of SubProcess
	 * @generated NOT
	 */
	private boolean isGoalRefSet()
	{
		SubProcessGoal goal = (SubProcessGoal) getPrimaryView().getElement();
		return goal.getGoalref() != null && !goal.getGoalref().trim().isEmpty();
	}

}
