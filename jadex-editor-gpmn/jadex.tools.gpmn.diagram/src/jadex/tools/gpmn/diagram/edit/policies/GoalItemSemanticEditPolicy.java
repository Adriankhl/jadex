/*
 * Copyright (c) 2009, Universität Hamburg
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package jadex.tools.gpmn.diagram.edit.policies;

import jadex.tools.gpmn.diagram.edit.commands.ActivationEdgeCreateCommand;
import jadex.tools.gpmn.diagram.edit.commands.ActivationEdgeReorientCommand;
import jadex.tools.gpmn.diagram.edit.commands.PlanEdgeCreateCommand;
import jadex.tools.gpmn.diagram.edit.commands.PlanEdgeReorientCommand;
import jadex.tools.gpmn.diagram.edit.commands.SuppressionEdgeCreateCommand;
import jadex.tools.gpmn.diagram.edit.commands.SuppressionEdgeReorientCommand;
import jadex.tools.gpmn.diagram.edit.parts.ActivationEdgeEditPart;
import jadex.tools.gpmn.diagram.edit.parts.PlanEdgeEditPart;
import jadex.tools.gpmn.diagram.edit.parts.SuppressionEdgeEditPart;
import jadex.tools.gpmn.diagram.part.GpmnVisualIDRegistry;
import jadex.tools.gpmn.diagram.providers.GpmnElementTypes;

import java.util.Iterator;

import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.gef.commands.Command;
import org.eclipse.gmf.runtime.diagram.core.commands.DeleteCommand;
import org.eclipse.gmf.runtime.emf.commands.core.command.CompositeTransactionalCommand;
import org.eclipse.gmf.runtime.emf.type.core.commands.DestroyElementCommand;
import org.eclipse.gmf.runtime.emf.type.core.requests.CreateRelationshipRequest;
import org.eclipse.gmf.runtime.emf.type.core.requests.DestroyElementRequest;
import org.eclipse.gmf.runtime.emf.type.core.requests.ReorientRelationshipRequest;
import org.eclipse.gmf.runtime.notation.Edge;
import org.eclipse.gmf.runtime.notation.View;

/**
 * @generated
 */
public class GoalItemSemanticEditPolicy extends GpmnBaseItemSemanticEditPolicy
{

	/**
	 * @generated
	 */
	public GoalItemSemanticEditPolicy()
	{
		super(GpmnElementTypes.Goal_2004);
	}

	/**
	 * @generated
	 */
	protected Command getDestroyElementCommand(DestroyElementRequest req)
	{
		View view = (View) getHost().getModel();
		CompositeTransactionalCommand cmd = new CompositeTransactionalCommand(
				getEditingDomain(), null);
		cmd.setTransactionNestingEnabled(false);
		for (Iterator it = view.getTargetEdges().iterator(); it.hasNext();)
		{
			Edge incomingLink = (Edge) it.next();
			if (GpmnVisualIDRegistry.getVisualID(incomingLink) == ActivationEdgeEditPart.VISUAL_ID)
			{
				DestroyElementRequest r = new DestroyElementRequest(
						incomingLink.getElement(), false);
				cmd.add(new DestroyElementCommand(r));
				cmd.add(new DeleteCommand(getEditingDomain(), incomingLink));
				continue;
			}
			if (GpmnVisualIDRegistry.getVisualID(incomingLink) == SuppressionEdgeEditPart.VISUAL_ID)
			{
				DestroyElementRequest r = new DestroyElementRequest(
						incomingLink.getElement(), false);
				cmd.add(new DestroyElementCommand(r));
				cmd.add(new DeleteCommand(getEditingDomain(), incomingLink));
				continue;
			}
		}
		for (Iterator it = view.getSourceEdges().iterator(); it.hasNext();)
		{
			Edge outgoingLink = (Edge) it.next();
			if (GpmnVisualIDRegistry.getVisualID(outgoingLink) == PlanEdgeEditPart.VISUAL_ID)
			{
				DestroyElementRequest r = new DestroyElementRequest(
						outgoingLink.getElement(), false);
				cmd.add(new DestroyElementCommand(r));
				cmd.add(new DeleteCommand(getEditingDomain(), outgoingLink));
				continue;
			}
			if (GpmnVisualIDRegistry.getVisualID(outgoingLink) == SuppressionEdgeEditPart.VISUAL_ID)
			{
				DestroyElementRequest r = new DestroyElementRequest(
						outgoingLink.getElement(), false);
				cmd.add(new DestroyElementCommand(r));
				cmd.add(new DeleteCommand(getEditingDomain(), outgoingLink));
				continue;
			}
		}
		EAnnotation annotation = view.getEAnnotation("Shortcut"); //$NON-NLS-1$
		if (annotation == null)
		{
			// there are indirectly referenced children, need extra commands: false
			addDestroyShortcutsCommand(cmd, view);
			// delete host element
			cmd.add(new DestroyElementCommand(req));
		}
		else
		{
			cmd.add(new DeleteCommand(getEditingDomain(), view));
		}
		return getGEFWrapper(cmd.reduce());
	}

	/**
	 * @generated
	 */
	protected Command getCreateRelationshipCommand(CreateRelationshipRequest req)
	{
		Command command = req.getTarget() == null ? getStartCreateRelationshipCommand(req)
				: getCompleteCreateRelationshipCommand(req);
		return command != null ? command : super
				.getCreateRelationshipCommand(req);
	}

	/**
	 * @generated
	 */
	protected Command getStartCreateRelationshipCommand(
			CreateRelationshipRequest req)
	{
		if (GpmnElementTypes.ActivationEdge_4001 == req.getElementType())
		{
			return null;
		}
		if (GpmnElementTypes.PlanEdge_4002 == req.getElementType())
		{
			return getGEFWrapper(new PlanEdgeCreateCommand(req,
					req.getSource(), req.getTarget()));
		}
		if (GpmnElementTypes.SuppressionEdge_4004 == req.getElementType())
		{
			return getGEFWrapper(new SuppressionEdgeCreateCommand(req, req
					.getSource(), req.getTarget()));
		}
		return null;
	}

	/**
	 * @generated
	 */
	protected Command getCompleteCreateRelationshipCommand(
			CreateRelationshipRequest req)
	{
		if (GpmnElementTypes.ActivationEdge_4001 == req.getElementType())
		{
			return getGEFWrapper(new ActivationEdgeCreateCommand(req, req
					.getSource(), req.getTarget()));
		}
		if (GpmnElementTypes.PlanEdge_4002 == req.getElementType())
		{
			return null;
		}
		if (GpmnElementTypes.SuppressionEdge_4004 == req.getElementType())
		{
			return getGEFWrapper(new SuppressionEdgeCreateCommand(req, req
					.getSource(), req.getTarget()));
		}
		return null;
	}

	/**
	 * Returns command to reorient EClass based link. New link target or source
	 * should be the domain model element associated with this node.
	 * 
	 * @generated
	 */
	protected Command getReorientRelationshipCommand(
			ReorientRelationshipRequest req)
	{
		switch (getVisualID(req))
		{
			case ActivationEdgeEditPart.VISUAL_ID:
				return getGEFWrapper(new ActivationEdgeReorientCommand(req));
			case PlanEdgeEditPart.VISUAL_ID:
				return getGEFWrapper(new PlanEdgeReorientCommand(req));
			case SuppressionEdgeEditPart.VISUAL_ID:
				return getGEFWrapper(new SuppressionEdgeReorientCommand(req));
		}
		return super.getReorientRelationshipCommand(req);
	}

}
