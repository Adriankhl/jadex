package jadex.tools.bpmn.editor;

import org.eclipse.stp.bpmn.diagram.providers.BpmnEditPartProvider;

public class JadexEditPartProvider extends BpmnEditPartProvider 
{
	
	public JadexEditPartProvider() 
	{
        setFactory(new JadexEditPartFactory());
        setAllowCaching(true);
    }


}
