/**
 * 
 */
package jadex.tools.bpmn.editor;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.stp.bpmn.diagram.part.BpmnDiagramEditor;
import org.eclipse.stp.bpmn.diagram.part.BpmnDiagramEditorPlugin;
import org.eclipse.ui.PlatformUI;

/**
 * This editor extends the BPMN-Diagram editor. 
 * With this extension we have full control about the displayed
 * properties and editor sheets. We also can extend the editor with
 * new edit parts and views.
 * 
 * @author Claas Altschaffel
 *
 */
public class JadexBpmnEditor extends BpmnDiagramEditor {

	/** The contributor id for this editor */
	public static final String ID = "jadex.tools.bpmn.editor.JadexBpmnEditorID";

    static {
 
        PlatformUI.getWorkbench().getEditorRegistry().
            setDefaultEditor("*.bpmn_diagram", ID); //$NON-NLS-1$
    }

    // ---- preference keys ----
    
    public static final String PREFERENCE_TASK_PROVIDER_LIST = "Task Provider";
    
    
 
    /** Access the contributor id */
    public String getContributorId() {
        return ID;
    }

	/**
	 * Log a exception into eclipse error log
	 * @param Exception to log
	 * @param int flag from {@link IStatus}
	 */
	public static void log(String message, Exception ex, int iStatus) {
		BpmnDiagramEditorPlugin.getInstance().getLog().log(
				new Status(iStatus, ID,
						iStatus, message + " -> " + ex.getMessage(), ex));
	}

}
