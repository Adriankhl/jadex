package jadex.simulation.analysis.buildingBlocks.generalAnalysis.workflowImpl;

import jadex.base.gui.componentviewer.IAbstractViewerPanel;
import jadex.bpmn.model.MActivity;
import jadex.bpmn.model.MBpmnModel;
import jadex.bpmn.runtime.BpmnInterpreter;
import jadex.commons.Future;
import jadex.commons.IFuture;
import jadex.commons.ISuspendable;
import jadex.commons.ThreadSuspendable;
import jadex.commons.service.BasicService;
import jadex.commons.service.RequiredServiceInfo;
import jadex.commons.service.SServiceProvider;
import jadex.simulation.analysis.buildingBlocks.generalAnalysis.IGeneralAnalysisService;
import jadex.simulation.analysis.buildingBlocks.generalAnalysis.workflowImpl.view.BasicGeneralAnalysisServiceView;
import jadex.simulation.analysis.buildingBlocks.simulation.IModelInspectionService;
import jadex.simulation.analysis.common.dataObjects.AModel;
import jadex.simulation.analysis.common.dataObjects.IAExperimentJob;
import jadex.simulation.analysis.common.dataObjects.IAExperimentalFrame;
import jadex.simulation.analysis.common.dataObjects.IAModel;
import jadex.simulation.analysis.common.dataObjects.parameter.IAParameterEnsemble;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JComponent;

public class BasicGeneralAnalysisService extends BasicService implements IGeneralAnalysisService
{

	private IAModel model = null;
	private IAExperimentalFrame expFrame = null;
	private BpmnInterpreter instance;

	private BasicGeneralAnalysisServiceView view;

	private Set<ActionListener> listeners = new HashSet<ActionListener>();

	public BasicGeneralAnalysisService(BpmnInterpreter instance)
	{
		// TODO: No Interpreter
		super(instance.getServiceProvider().getId(), IGeneralAnalysisService.class, null);
		this.instance = instance;
		Map prop = getPropertyMap();
		prop.put(IAbstractViewerPanel.PROPERTY_VIEWERCLASS, "jadex.simulation.analysis.buildingBlocks.generalAnalysis.viewer.GeneralAnalysisServiceViewerPanel");
		setPropertyMap(prop);
	}

	public IFuture getView()
	{
		Future res = new Future(view);
		return res;
	}

	@Override
	public void registerView(JComponent view)
	{
		this.view = (BasicGeneralAnalysisServiceView) view;
	}

	// @Override
	public IFuture getTasks()
	{
		Future res = new Future();
		List<String> sTaskList = new LinkedList<String>();

		MBpmnModel model = instance.getModelElement();
		Map allActivities = model.getAllActivities();

		List<MActivity> activities = new ArrayList<MActivity>();
		for (Iterator<MActivity> it = model.getAllActivities().values().iterator(); it.hasNext();)
		{
			activities.add(it.next());
		}

		List<MActivity> taskList = new ArrayList<MActivity>();
		for (MActivity activity : activities)
		{
			if (activity.getActivityType().equals("Task"))
				taskList.add(activity);
		}
		for (MActivity mActivity : taskList)
		{
			sTaskList.add(mActivity.getName());
		}
		res.setResult(sTaskList);
		return res;
	}

	// @Override
	public IFuture getModelParameter(String modelName, String modelType)
	{
		Future resFut = new Future();
		ISuspendable susThread = new ThreadSuspendable(this);

		IFuture serviceFut = SServiceProvider.getServices(instance.getServiceProvider(), IModelInspectionService.class, RequiredServiceInfo.SCOPE_GLOBAL);

		Object res = serviceFut.get(susThread);
		ArrayList<IModelInspectionService> services = null;
		if (res instanceof ArrayList)
		{
			services = (ArrayList<IModelInspectionService>) res;
		}
		else
		{
			new RuntimeException("No Service found!");
		}
		IModelInspectionService service = null;
		for (IModelInspectionService iModelService : services)
		{
			if (iModelService.supportedModels().contains(modelType)) service = iModelService;
		}

		IFuture iParaFut = service.modelParamter(modelName);
		IAParameterEnsemble inputParameter = (IAParameterEnsemble) iParaFut.get(susThread);

		IFuture oParaFut = service.resultParamter(modelName);
		IAParameterEnsemble outputParameter = (IAParameterEnsemble) oParaFut.get(susThread);
		IAModel model = new AModel(modelName, modelType, inputParameter, outputParameter);

		resFut.setResult(model);
		return resFut;
	}

	@Override
	public void registerListener(ActionListener listener)
	{
		listeners.add(listener);

	}

	@Override
	public void signal(ActionEvent ae)
	{
		for (ActionListener listener : listeners)
		{
			listener.actionPerformed(ae);
		}

	}

	public void setModel(IAModel model)
	{
		this.model = model;
	}

	public IFuture getModel()
	{
		return new Future(model);
	}

	public void setExpFrame(IAExperimentalFrame expFrame)
	{
		this.expFrame = expFrame;
	}

	public IFuture getFrame()
	{
		return new Future(expFrame);
	}

	public void expFrameStart()
	{
		view.experiment();
	}

	public void experimentStart(JComponent comp)
	{
		view.experimentieren(comp);
	}

	public void present(IAExperimentJob job)
	{
		view.present(job);
	}

	@Override
	public IFuture getView(Frame owner)
	{
		// TODO Auto-generated method stub
		return null;
	}
}
