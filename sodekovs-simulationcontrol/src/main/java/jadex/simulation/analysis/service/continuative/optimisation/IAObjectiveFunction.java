package jadex.simulation.analysis.service.continuative.optimisation;

import jadex.commons.future.IFuture;
import jadex.simulation.analysis.common.data.parameter.IAParameterEnsemble;

public interface IAObjectiveFunction
{
	/**
	 * Wertet die L�sungsalternative des Ensembles aus
	 * und gibt ein Zielfunktinswert zur�ck.
	 */
	public IFuture benchmark(IAParameterEnsemble ensemble);
	
	/**
	 * Ziel der Minimierung oder Maximierung
	 */
	public Boolean MinGoal();
}
