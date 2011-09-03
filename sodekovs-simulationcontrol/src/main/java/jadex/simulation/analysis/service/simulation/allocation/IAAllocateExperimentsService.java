package jadex.simulation.analysis.service.simulation.allocation;

import jadex.commons.future.IFuture;
import jadex.simulation.analysis.common.data.IAExperimentBatch;
import jadex.simulation.analysis.common.data.allocation.IAllocationStrategy;
import jadex.simulation.analysis.common.superClasses.service.analysis.IAnalysisSessionService;

import java.util.UUID;

/**
 * Service to allocate experiments
 * 
 * @author 5Haubeck
 */
public interface IAAllocateExperimentsService extends IAnalysisSessionService
{
	/**
	 * Set the strategy to allocate
	 * 
	 * @param strategy
	 *            strategy to allocate experiments
	 * @param session
	 *            if any, already opened session
	 */
	public void setStrategy(UUID session, IAllocationStrategy strategy);

	/**
	 * Allocate experiments with strategy
	 * 
	 * @param session
	 *            if any, already opened session
	 * @param experiments
	 *            experiments to allocate
	 * @return List<IAExecuteExperimentsService>, List of experiments to allocate
	 */
	public IFuture allocateExperiment(UUID session, IAExperimentBatch experiments);
}
