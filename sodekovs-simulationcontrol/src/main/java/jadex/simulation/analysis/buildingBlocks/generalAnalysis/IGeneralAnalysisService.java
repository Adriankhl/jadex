package jadex.simulation.analysis.buildingBlocks.generalAnalysis;

import jadex.commons.IFuture;
import jadex.commons.service.IService;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComponent;

public interface IGeneralAnalysisService extends IService{

	public void registerView(JComponent view);
	
	public IFuture getView();
	
	public void signal(ActionEvent ar);
	
	public void registerListener(ActionListener listener);

}
