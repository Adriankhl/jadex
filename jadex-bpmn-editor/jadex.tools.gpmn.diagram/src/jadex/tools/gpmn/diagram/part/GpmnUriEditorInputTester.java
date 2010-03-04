/*
 * Copyright (c) 2009, Universität Hamburg
 * All rights reserved. This program and the accompanying 
 * materials are made available under the terms of the 
 * ###_LICENSE_REPLACEMENT_MARKER_###
 * which accompanies this distribution, and is available at
 * ###_LICENSE_URL_REPLACEMENT_MARKER_###
 */
package jadex.tools.gpmn.diagram.part;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.emf.common.ui.URIEditorInput;

/**
 * @generated
 */
public class GpmnUriEditorInputTester extends PropertyTester
{

	/**
	 * @generated
	 */
	public boolean test(Object receiver, String method, Object[] args,
			Object expectedValue)
	{
		if (false == receiver instanceof URIEditorInput)
		{
			return false;
		}
		URIEditorInput editorInput = (URIEditorInput) receiver;
		return "gpmn_diagram".equals(editorInput.getURI().fileExtension()); //$NON-NLS-1$
	}

}
