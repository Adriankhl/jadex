/**
 * Copyright (c) 2009, Universität Hamburg
 * All rights reserved. This program and the accompanying 
 * materials are made available under the terms of the 
 * ###_LICENSE_REPLACEMENT_MARKER_###
 * which accompanies this distribution, and is available at
 * ###_LICENSE_URL_REPLACEMENT_MARKER_###
 */
package jadex.tools.gpmn.diagram.part;

import jadex.tools.gpmn.Plan;
import jadex.tools.gpmn.diagram.edit.parts.PlanEditPart;
import jadex.tools.gpmn.properties.ModifyModelElementCommand;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.gmf.runtime.common.core.command.CommandResult;
import org.eclipse.gmf.runtime.notation.View;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;

/**
 * @author claas
 *
 */
public class SelectBPMNPlanFileAction extends AbstractFileAction implements IObjectActionDelegate
{

	public final static String ID = "jadex.tools.gpmn.diagram.popup.SelectBPMNPlanFileActionID";

	private PlanEditPart selectedElement;


	/**
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public void run(IAction action)
	{

		Display display = targetPartWorkbench.getDisplay();
		Shell shell = display.getActiveShell();
		FileDialog dialog = new FileDialog(shell, SWT.OPEN);
		dialog.setText("Please choose BPMN plan file");
		String[] filterNames = new String[] { "BPMN Files", "All Files (*)" };
		String[] filterExtensions = new String[] { "*.bpmn_diagram;*.bpmn", "*" };

		String platform = SWT.getPlatform();
		if (platform.equals("win32") || platform.equals("wpf"))
		{
			filterNames = new String[] { "BPMN Files", "All Files (*.*)" };
			filterExtensions = new String[] { "*.bpmn_diagram;*.bpmn", "*.*" };
		}

		String filterPath = getActiveFileEditorInput().getFile().getLocation()
				.removeLastSegments(1).toOSString();
		
		// use location of current file if set
		String bpmnPlanFile = ((Plan) ((View) selectedElement.getModel()).getElement()).getBpmnPlan();
		if (bpmnPlanFile != null && !bpmnPlanFile.isEmpty())
		{
			try
			{
				// HACK! Should be done through APIs
				IClasspathEntry[] classpathArray = JavaCore.create(getActiveProject()).getRawClasspath();
				IFile currentFile = null;
				for (int i = 0; i < classpathArray.length; i++)
				{
					String filePath = classpathArray[i].getPath().append(bpmnPlanFile + "_diagram").removeFirstSegments(1).toPortableString();
					currentFile = getActiveProject().getFile(filePath);
					if (currentFile.exists() && currentFile.isAccessible())
					{
						filterPath = currentFile.getLocation().removeLastSegments(1).toOSString();
						break;
					}
				}
			}
			catch (JavaModelException e)
			{
				// ignore?
				GpmnDiagramEditorPlugin.getInstance().getLog().log(
						new Status(IStatus.WARNING, GpmnDiagramEditorPlugin.ID,
								IStatus.WARNING, e.getMessage(), e));
			}
		}

		dialog.setFilterNames(filterNames);
		dialog.setFilterExtensions(filterExtensions);
		dialog.setFilterPath(filterPath);

		String planFile = null;
		String planLocation = dialog.open();
		
		// remove "_diagram" from extension
		if (planLocation.endsWith("_diagram")) ////$NON-NLS-1$
		{
			planLocation = planLocation.substring(0, planLocation.indexOf("_diagram")); ////$NON-NLS-1$
		}
		
		// remove leading project path
		IPath planPath = new Path(planLocation);
		IPath projectPath = getActiveProject().getLocation();
		if (projectPath.isPrefixOf(planPath))
		{
			planPath = planPath.makeRelativeTo(projectPath);
		}

		// check file access
		IFile file = getActiveProject().getFile(planPath);
		if (file.exists() && file.isAccessible())
		{
			try
			{
				// HACK! Should be done through APIs
				IClasspathEntry[] classpathArray = JavaCore.create(getActiveProject()).getRawClasspath();
				for (int i = 0; i < classpathArray.length; i++)
				{
					if (classpathArray[i].getPath().isPrefixOf(file.getFullPath()))
					{
						planFile = file.getFullPath().makeRelativeTo(
								classpathArray[i].getPath()).toPortableString();
						break;
					}
				}

			}
			catch (JavaModelException e)
			{
				GpmnDiagramEditorPlugin.getInstance().getLog().log(
						new Status(IStatus.ERROR, GpmnDiagramEditorPlugin.ID,
								IStatus.ERROR, e.getMessage(), e));
			}
		}

		// replace doesn't work bidirectional
		final String fPlanFile = planFile; // .replaceAll("/", "."); // this
		final Plan plan = (Plan) ((View) selectedElement.getModel()).getElement();

		// modify the model element
		if (fPlanFile != null)
		{
			// modify the plan
			ModifyModelElementCommand command = new ModifyModelElementCommand(
					plan,
					GpmnDiagramMessages.SelectBPMNPlanFileAction_update_element_command_name)
			{
				@Override
				protected CommandResult doExecuteWithResult(
						IProgressMonitor arg0, IAdaptable arg1)
						throws ExecutionException
				{
					plan.setBpmnPlan(fPlanFile);
					return CommandResult.newOKCommandResult();
				}
			};

			try
			{
				command.execute(null, null);
			}
			catch (ExecutionException e)
			{
				GpmnDiagramEditorPlugin.getInstance().getLog().log(
						new Status(IStatus.ERROR, GpmnDiagramEditorPlugin.ID,
								IStatus.ERROR, e.getMessage(), e));
			}

		}
	}

	/**
	 * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction, org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection)
	{
		selectedElement = null;
		if (selection instanceof IStructuredSelection)
		{
			IStructuredSelection structuredSelection = (IStructuredSelection) selection;
			if (structuredSelection.getFirstElement() instanceof PlanEditPart)
			{
				selectedElement = (PlanEditPart) structuredSelection
						.getFirstElement();
			}
		}
	}

}
