package jadex.simulation.analysis.buildingBlocks.simulation.netLogo;

import jadex.bdi.runtime.ICapability;
import jadex.commons.Future;
import jadex.commons.IFuture;
import jadex.commons.service.BasicService;
import jadex.simulation.analysis.buildingBlocks.simulation.IModelInspectionService;
import jadex.simulation.analysis.common.dataObjects.parameter.ABasicParameter;
import jadex.simulation.analysis.common.dataObjects.parameter.AParameterEnsemble;
import jadex.simulation.analysis.common.dataObjects.parameter.IAParameterEnsemble;

import java.util.HashSet;
import java.util.Set;

public class NetLogoModelService extends BasicService implements IModelInspectionService {

	public NetLogoModelService(ICapability cap) {
		super(cap.getServiceProvider().getId(), IModelInspectionService.class, null);
//		Map prop = getPropertyMap();
//		prop.put(IAbstractViewerPanel.PROPERTY_VIEWERCLASS, "jadex.simulation.analysis.buildingBlocks.execution.ExecutionServiceView");
//		setPropertyMap(prop);
	}

	@Override
	public IFuture modelParamter(String name) {
		Future res = new Future();
		
		// TODO: Search right Model (globals in netLogo model)
		IAParameterEnsemble paramters = new AParameterEnsemble();
		paramters.addParameter(new ABasicParameter("population", Double.class, new Double(100)));
		paramters.addParameter(new ABasicParameter("diffusion-rate", Double.class, new Double(40)));
		paramters.addParameter(new ABasicParameter("evaporation-rate", Double.class, new Double(10)));
		res.setResult(paramters);
		return res;
	}
	
	@Override
	public IFuture resultParamter(String name) {
		Future res = new Future();
		
		// TODO: Search right Model
		IAParameterEnsemble paramters = new AParameterEnsemble();
		paramters.addParameter(new ABasicParameter("ticks", Double.class));
		res.setResult(paramters);
		return res;
	}

	@Override
	public Set<String> supportedModels() {
		Set<String> result = new HashSet<String>();
		result.add("netLogo");
		return result;
	}
}
