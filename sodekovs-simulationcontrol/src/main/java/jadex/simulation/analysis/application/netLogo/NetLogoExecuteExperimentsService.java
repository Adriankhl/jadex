package jadex.simulation.analysis.application.netLogo;

import jadex.bridge.IExternalAccess;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.simulation.analysis.common.data.IAExperiment;
import jadex.simulation.analysis.common.data.parameter.IAParameter;
import jadex.simulation.analysis.common.data.parameter.IAParameterEnsemble;
import jadex.simulation.analysis.common.superClasses.service.analysis.ABasicAnalysisSessionService;
import jadex.simulation.analysis.service.simulation.Modeltype;
import jadex.simulation.analysis.service.simulation.execution.IAExecuteExperimentsService;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.swing.JFrame;
import javax.swing.JTextArea;

import org.nlogo.headless.HeadlessWorkspace;
import org.nlogo.lite.InterfaceComponent;

/**
 * Implementation of a NetLogo service for (single) experiments.
 */
public class NetLogoExecuteExperimentsService extends ABasicAnalysisSessionService implements IAExecuteExperimentsService {

	JTextArea compLite = new JTextArea();
	
	/**
	 * Create a new netLogo Simulation Service
	 * 
	 * @param comp
	 *            The active generalComp.
	 */
	public NetLogoExecuteExperimentsService(IExternalAccess access) {
		super(access, IAExecuteExperimentsService.class, true);
	}
	
	/**
	 * Simulate an experiment
	 */
	public IFuture executeExperiment(UUID session, final IAExperiment exp) {
		final Future res = new Future();

		if (session == null) session = (UUID) createSession(null).get(susThread);
		
		if ((Boolean)exp.getExperimentParameter("Visualisierung").getValue())
		{		
//			NetLogoSessionView view = ((NetLogoSessionView)sessionViews.get(session));
//			final JFrame frame = (JFrame)SwingUtilities.getRoot(taskview.getComponent());
			final JFrame frame = new JFrame();

			final InterfaceComponent comp = new InterfaceComponent(frame);
			frame.add(comp);
				
//			(JFrame) SwingUtilities.getRoot(view)
//			((AServiceCallTaskView) taskview).addServiceGUI(comp, new GridBagConstraints(0, 0, GridBagConstraints.REMAINDER, GridBagConstraints.REMAINDER, 1, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(1, 1, 1, 1), 0, 0));
//			view.showFull(comp);

			try 
	        {
				java.awt.EventQueue.invokeAndWait
                ( new Runnable()
                    { public void run() {
                        try {
                			frame.setSize(800, 800);
                			frame.setVisible(true);
                        	String filePre = new File("..").getCanonicalPath() + "/sodekovs-simulationcontrol/src/main/java/jadex/simulation/analysis/application/netLogo/models/";
							String fileName = filePre +  exp.getModel().getName() +".nlogo";
                          comp.open(fileName);
                        }
                        catch(Exception ex) {
                          ex.printStackTrace();
                        }
                    } } ) ;
            IAParameterEnsemble inputPara = exp.getModel().getInputParameters();
            for (IAParameter parameter : inputPara.getParameters().values()) {
				String comm = "set " + parameter.getName() + " " + parameter.getValue().toString();
//				System.out.println(comm);
				comp.command(comm);
			}

            Integer rep = 0;
			Integer replicationen = (Integer) exp.getExperimentParameter("Wiederholungen").getValue();
			
			while(rep < replicationen)
			{
				comp.command("setup");
		        comp.command("go");
		        for (IAParameter parameter : exp.getModel().getOutputParameters().getParameters().values()) {
		        	exp.getOutputParameter(parameter.getName()).setValue(comp.report(parameter.getName()));
				}
		        
				rep++;
			}
			frame.dispose();
			}
        	catch(Exception ex) {
        		ex.printStackTrace();
        	}
		} else
		{
			((NetLogoSessionView)sessionViews.get(session)).showLite(compLite);
			HeadlessWorkspace workspace =
				HeadlessWorkspace.newInstance();
		try {
			String filePre = new File("..").getCanonicalPath() + "/sodekovs-simulationcontrol/src/main/java/jadex/simulation/analysis/application/netLogo/models/";
			String FileName = filePre + exp.getModel().getName() +".nlogo";
			compLite.append("### netLogo 4.1.2 ###");
			compLite.append("Open file" + FileName + "\n");
			
			workspace.open(FileName);
			 IAParameterEnsemble inputPara = exp.getModel().getInputParameters();
	            for (IAParameter parameter : inputPara.getParameters().values()) {
					String comm = "set " + parameter.getName() + " " + parameter.getValue().toString();
					workspace.command(comm);
				}
	          
	            Integer rep = 0;
				Integer replicationen = (Integer) exp.getExperimentParameter("Wiederholungen").getValue();
				
				while(rep < replicationen)
				{
					compLite.append("Start " +  exp.getModel().getName() + "\n");
					workspace.command("setup");
					workspace.command("go");
			        exp.getOutputParameter("ticks").setValue(workspace.report("ticks"));
					compLite.append("End " +  exp.getModel().getName() + "\n");
					rep++;
				}
			
			workspace.dispose();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		}
		res.setResult(exp);
		return res;
	}
	
	@Override
	public Set<Modeltype> supportedModels()
	{
		Set<Modeltype> result = new HashSet<Modeltype>();
		result.add(Modeltype.NetLogo);
		return result;
	}

}
