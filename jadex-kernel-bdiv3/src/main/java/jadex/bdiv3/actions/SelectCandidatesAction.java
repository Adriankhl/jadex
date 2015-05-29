package jadex.bdiv3.actions;

import jadex.bdiv3.annotation.Plan;
import jadex.bdiv3.features.IBDIAgentFeature;
import jadex.bdiv3.features.impl.IInternalBDIAgentFeature;
import jadex.bdiv3.model.MCapability;
import jadex.bdiv3.model.MGoal;
import jadex.bdiv3.model.MPlan;
import jadex.bdiv3.runtime.IPlan;
import jadex.bdiv3.runtime.impl.APL;
import jadex.bdiv3.runtime.impl.APL.MPlanInfo;
import jadex.bdiv3.runtime.impl.RGoal;
import jadex.bdiv3.runtime.impl.RPlan;
import jadex.bdiv3.runtime.impl.RProcessableElement;
import jadex.bdiv3x.runtime.ICandidateInfo;
import jadex.bdiv3x.runtime.IElement;
import jadex.bridge.IConditionalComponentStep;
import jadex.bridge.IInternalAccess;
import jadex.commons.SUtil;
import jadex.commons.Tuple2;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.commons.future.IResultListener;

import java.util.ArrayList;
import java.util.List;

/**
 *  Action for selecting a candidate from the APL.
 */
public class SelectCandidatesAction implements IConditionalComponentStep<Void>
{
	/** The element. */
	protected RProcessableElement element;
	
	/**
	 *  Create a new action.
	 */
	public SelectCandidatesAction(RProcessableElement element)
	{
		this.element = element;
	}
	
	/**
	 *  Test if the action is valid.
	 *  @return True, if action is valid.
	 */
	public boolean isValid()
	{
		boolean ret = true;
		
		if(element instanceof RGoal)
		{
			RGoal rgoal = (RGoal)element;
			ret = RGoal.GoalLifecycleState.ACTIVE.equals(rgoal.getLifecycleState())
				&& RGoal.GoalProcessingState.INPROCESS.equals(rgoal.getProcessingState());
		}
			
//		if(!ret)
//			System.out.println("not valid: "+this+" "+element);
		
		return ret;
	}
	
	/**
	 *  Execute the command.
	 *  @param args The argument(s) for the call.
	 *  @return The result of the command.
	 */
	public IFuture<Void> execute(final IInternalAccess ia)
	{
//		if(element.toString().indexOf("Analyze")!=-1)
//			System.out.println("select candidates: "+element);
		
		Future<Void> ret = new Future<Void>();

//		BDIAgentInterpreter ip = (BDIAgentInterpreter)((BDIAgent)ia).getInterpreter();
		MCapability	mcapa = (MCapability)((IInternalBDIAgentFeature)ia.getComponentFeature(IBDIAgentFeature.class)).getCapability().getModelElement();

		List<Object> cands = element.getApplicablePlanList().selectCandidates(mcapa);
		
		if(cands!=null && !cands.isEmpty())
		{
			element.setState(RProcessableElement.State.CANDIDATESSELECTED);
			for(Object cand: cands)
			{
				if(cand instanceof MPlanInfo)
				{
					MPlanInfo mplaninfo = (MPlanInfo)cand;
					RPlan rplan = RPlan.createRPlan(mplaninfo.getMPlan(), cand, element, ia, mplaninfo.getBinding());
					RPlan.executePlan(rplan, ia);
					ret.setResult(null);
				}
				// direct subgoal for goal
				else if(cand instanceof MGoal)
				{
					final RGoal pagoal = (RGoal)element;
					final MGoal mgoal = (MGoal)cand;
					final Object pgoal = mgoal.createPojoInstance(ia, pagoal);
					final RGoal rgoal = new RGoal(ia, mgoal, pgoal, pagoal, null);
					final APL apl = element.getApplicablePlanList();
					
					// Add candidates to meta goal
					if(mgoal.isMetagoal())
					{
						List<Object> allcands = apl.getCandidates();
						if(allcands.size()==1)
						{
							element.planFinished(ia, null);
							ret.setResult(null);
							return ret;
						}
						
						for(Object c: allcands)
						{
							if(!c.equals(cand) && c instanceof MPlanInfo)
							{
								MPlanInfo pi = (MPlanInfo)c;
								final RPlan rplan = RPlan.createRPlan(pi.getMPlan(), c, element, ia, pi.getBinding());
								
								// find by type and direction?!
								rgoal.getParameterSet("applicables").addValue(new ICandidateInfo()
								{
									public IPlan getPlan()
									{
										return rplan;
									}
									
									public IElement getElement()
									{
										return element;
									}
								});
							}
						}
					}
					
					rgoal.addListener(new IResultListener<Void>()
					{
						public void resultAvailable(Void result)
						{
							Object res = RGoal.getGoalResult(rgoal, ia.getClassLoader());
							
							if(mgoal.isMetagoal())
							{
								// Execute selected plans if was metagoal
								// APL is automatically kept uptodate
								for(ICandidateInfo ci: (ICandidateInfo[])res)
								{
									RPlan.executePlan((RPlan)ci.getPlan(), ia);
								}
							}
							else
							{
								// Set goal result on parent goal
								pagoal.setGoalResult(res, ia.getClassLoader(), null, null, rgoal);
								pagoal.planFinished(ia, rgoal);
							}
						}
						
						public void exceptionOccurred(Exception exception)
						{
							// todo: what if meta-level reasoning fails?!
							pagoal.planFinished(ia, rgoal);
						}
					});
					
					RGoal.adoptGoal(rgoal, ia);
				}
				else if(cand.getClass().isAnnotationPresent(Plan.class))
				{
					MPlan mplan = mcapa.getPlan(cand.getClass().getName());
					RPlan rplan = RPlan.createRPlan(mplan, cand, element, ia, null);
					RPlan.executePlan(rplan, ia);
					ret.setResult(null);
				}
				else if(cand instanceof RPlan)
				{
					// dispatch to running plan
					final RPlan rplan = (RPlan)cand;
					rplan.setDispatchedElement(element);
					if(rplan.getResumeCommand()==null)
					{
						// case meta-level reasoning, plan has been created but is new
						RPlan.executePlan(rplan, ia);
					}
					else
					{
						// normal case when plan was waiting
						rplan.getResumeCommand().execute(new Tuple2<Boolean, Boolean>(null, Boolean.FALSE));
					}
					ret.setResult(null);
				}
				else if(cand instanceof List)
				{
					// dispatch to waitqueue
					((List)cand).add(element);
					ret.setResult(null);
				}
//				// Unwrap candidate info coming from meta-level reasoning
//				else if(cand instanceof ICandidateInfo)
//				{
//					ICandidateInfo ci = (ICandidateInfo)cand;
//					RPlan.executePlan((RPlan)ci.getPlan(), ia);
//					ret.setResult(null);
//				}
			}
		}
		else
		{
			// todo: throw goal failed exception for goal listeners
			element.planFinished(ia, null);
//			System.out.println("No applicable plan found.");
		}
		
		return ret;
	}
}
