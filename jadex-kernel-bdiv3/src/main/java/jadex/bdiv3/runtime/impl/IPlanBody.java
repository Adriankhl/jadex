package jadex.bdiv3.runtime.impl;

import jadex.commons.future.IFuture;

/**
 *  Interface for plan body.
 */
public interface IPlanBody
{
	/**
	 *  Execute the plan body.
	 */
	public IFuture<Void> executePlanBody();
}
