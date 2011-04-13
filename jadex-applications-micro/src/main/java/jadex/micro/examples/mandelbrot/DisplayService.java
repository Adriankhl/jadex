package jadex.micro.examples.mandelbrot;

import jadex.bridge.service.BasicService;
import jadex.commons.future.IFuture;

/**
 *  The service allows displaying results in the frame
 *  managed by the service providing agent.
 */
public class DisplayService extends BasicService implements IDisplayService
{
	//-------- attributes --------
	
	/** The agent. */
	protected DisplayAgent agent;
	
	//-------- constructors --------

	/**
	 *  Create a new display service.
	 */
	public DisplayService(DisplayAgent agent)
	{
		super(agent.getServiceContainer().getId(), IDisplayService.class, null);
		this.agent	= agent;
	}
	
	//-------- IDisplayService interface --------

	/**
	 *  Display the result of a calculation.
	 */
	public IFuture displayResult(AreaData result)
	{
//		System.out.println("displayRes");
		agent.getPanel().setResults(result);
		return IFuture.DONE;
	}


	/**
	 *  Display intermediate calculation results.
	 */
	public IFuture displayIntermediateResult(ProgressData progress)
	{
//		System.out.println("displayInRes");
		agent.getPanel().addProgress(progress);
		return IFuture.DONE;
	}
}
