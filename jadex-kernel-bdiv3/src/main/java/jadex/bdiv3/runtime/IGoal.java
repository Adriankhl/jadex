package jadex.bdiv3.runtime;

import jadex.bdiv3x.runtime.IFinishableElement;
import jadex.bdiv3x.runtime.IParameterElement;

/**
 *  Interface for a bdi goal.
 */
public interface IGoal extends IParameterElement, IFinishableElement// needed for xml version of BDI
{
	//-------- goal states --------
	
	public static enum GoalLifecycleState
	{
		NEW, 
		ADOPTED,
		OPTION,
		ACTIVE,
		SUSPENDED,
		DROPPING,
		DROPPED
	};
	
	public static enum GoalProcessingState
	{
		IDLE, 
		INPROCESS,
		PAUSED,
		SUCCEEDED,
		FAILED,
	};
	
	/**
	 *  Get the id.
	 */
	public String getId();
	
	/**
	 *  Drop the goal.
	 */
	public void drop();
	
	/**
	 *  Get the lifecycle state.
	 *  @return The current lifecycle state (e.g. new, active, dropped).
	 */
	public GoalLifecycleState getLifecycleState();
	
	/**
	 *  Get the processingState.
	 *  @return The processingState.
	 */
	public GoalProcessingState getProcessingState();

	/**
	 *  Test if the goal is in lifecyclestate 'active'.
	 */
	// legacy v2 method.
	public boolean isActive();
	
//	/**
//	 *  Get the parent of the goal.
//	 */
//	public IPlan getParent();
}
