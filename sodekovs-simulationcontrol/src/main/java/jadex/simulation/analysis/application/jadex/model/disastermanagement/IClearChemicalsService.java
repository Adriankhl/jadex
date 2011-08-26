package jadex.simulation.analysis.application.jadex.model.disastermanagement;

import jadex.commons.future.IFuture;
import jadex.extension.envsupport.environment.ISpaceObject;

/**
 *  Clear chemicals service interface.
 */
public interface IClearChemicalsService
{	
	/**
	 *  Clear chemicals.
	 *  @param disaster The disaster.
	 *  @return Future, null when done.
	 */
	public IFuture clearChemicals(ISpaceObject disaster);

	/**
	 *  Abort clearing chemicals.
	 *  @return Future, null when done.
	 */
	public IFuture abort();
}
