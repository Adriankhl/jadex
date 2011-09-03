package jadex.simulation.analysis.application.commonsMath;

import jadex.bridge.IExternalAccess;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.simulation.analysis.common.data.AExperimentBatch;
import jadex.simulation.analysis.common.data.IAExperiment;
import jadex.simulation.analysis.common.data.IAExperimentBatch;
import jadex.simulation.analysis.common.data.parameter.IAParameter;
import jadex.simulation.analysis.common.data.parameter.IAParameterEnsemble;
import jadex.simulation.analysis.common.superClasses.service.analysis.ABasicAnalysisSessionService;
import jadex.simulation.analysis.service.continuative.optimisation.IAObjectiveFunction;
import jadex.simulation.analysis.service.continuative.optimisation.IAOptimisationService;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.math.analysis.MultivariateRealFunction;
import org.apache.commons.math.exception.MathUserException;
import org.apache.commons.math.exception.MaxCountExceededException;
import org.apache.commons.math.optimization.ConvergenceChecker;
import org.apache.commons.math.optimization.RealPointValuePair;
import org.apache.commons.math.optimization.SimpleScalarValueChecker;
import org.apache.commons.math.util.Incrementor;

public class CommonsMathOptimisationService extends ABasicAnalysisSessionService implements IAOptimisationService
{
	private CommonsMathOptimisationService me = this;
	private Set<String> methods = new HashSet<String>();
	private Map<UUID, Map<String, Object>> sessionState;

	public CommonsMathOptimisationService(IExternalAccess access)
	{
		super(access, IAOptimisationService.class, true);
		sessionState = new HashMap<UUID, Map<String, Object>>();
		methods.add("Simplex Algorithmus");

	}

	@Override
	public IFuture configurateOptimisation(UUID session, String method, IAParameterEnsemble methodParameter, IAParameterEnsemble solution, IAObjectiveFunction objective, IAParameterEnsemble config)

	{
		// session erstellen
		UUID newSession = null;
		if (session != null)
		{
			if (sessions.containsKey(session))
			{
				newSession = session;
			}
		}
		if (newSession == null)
		{
			newSession = (UUID) createSession(null).get(susThread);
		}
		final UUID sess = newSession;

		if (method.equals("Simplex Algorithmus"))
		{
			// set states
			Map<String, Object> state = new HashMap<String, Object>();

			// evaluations (counts numbers of evaluations)
			Incrementor evaluations = new Incrementor();
			evaluations.setMaximalCount(20);
			evaluations.resetCount();
			state.put("evaluations", evaluations);

			// iteration
			state.put("iteration", new Integer(0));

			// terminate
			Boolean terminate = false;
			state.put("terminate", terminate);

			// objective
			state.put("objective", objective);

			// comparator (to sort RealPointValuePair in simplex)
			final boolean isMinim = objective.MinGoal();
			final Comparator<RealPointValuePair> comparator = new Comparator<RealPointValuePair>()
			{
				public int compare(final RealPointValuePair o1,
								final RealPointValuePair o2)
			{
				final double v1 = o1.getValue();
				final double v2 = o2.getValue();
				return isMinim ? Double.compare(v1, v2) : Double.compare(v2, v1);
			}
			};
			state.put("comparator", comparator);

			final MultivariateRealFunction evalFunc = new MultivariateRealFunction()
			{
				public double value(double[] point) throws MathUserException
			{
				return Double.NaN;
			}
			};
			state.put("evaluator", evalFunc);
			//
			// // evaluator
			SimpleScalarValueChecker checker = new SimpleScalarValueChecker();
			state.put("checker", checker);

			// start and mapping
			List<Map.Entry<String, IAParameter>> mappings = new LinkedList<Map.Entry<String, IAParameter>>();
			for (Map.Entry<String, IAParameter> para : solution.getParameters().entrySet())
			{
				mappings.add(para);
			}

			double[] start = new double[mappings.size()];
			System.out.print("Mapping: ");
			for (int i = 0; i < start.length; i++)
			{
				start[i] = (Double) mappings.get(i).getValue().getValue();
				System.out.print(i + " * " + mappings.get(i).getKey() + "(" + start[i] + ")" + "|");
			}
			System.out.println();
			state.put("mappings", mappings);

			state.put("state", new Integer(0));
			state.put("start", start);

			// simplex
			double[] sSet = new double[2];
			sSet[0] = 0.25;
			sSet[1] = 0.25;
			
			double[][] referencesimplex = new double[3][2];
			referencesimplex[0][0] = 0.1;
			referencesimplex[0][1] = 0.1;
			
			referencesimplex[1][0] = 0.2;	
			referencesimplex[1][1] = 0.1;
			
			referencesimplex[2][0] = 0.1;
			referencesimplex[2][1] = 0.2;
			NelderMeadSimplexSim simplex = new NelderMeadSimplexSim(referencesimplex, 1, 2, 0.5, 0.5);
			simplex.build(sSet);
			state.put("simplex", simplex);

			sessionState.put(sess, state);
		}

		return new Future(sess);
	}

	@Override
	public IFuture supportedMethods()
	{
		return new Future(methods);
	}

	@Override
	public IFuture getMethodParameter(String methodName)
	{
		Future result = new Future();
		if (methodName.equals("Simplex Algorithmus"))
		{
			Set<String> parameters = new HashSet<String>();
		}
		return result;
	}

	@Override
	public IFuture nextSolutions(UUID session, IAExperimentBatch previousSolutions)
	{
		Map<String, Object> state = sessionState.get(session);
		NelderMeadSimplexSim simplex = (NelderMeadSimplexSim) state.get("simplex");
		Comparator<RealPointValuePair> comparator = (Comparator<RealPointValuePair>) state.get("comparator");
		Incrementor evaluations = (Incrementor) state.get("evaluations");
		ConvergenceChecker<RealPointValuePair> checker = (ConvergenceChecker<RealPointValuePair>) state.get("checker");
		MultivariateRealFunction evalFunc = (MultivariateRealFunction) state.get("evaluator");
		Boolean terminate = (Boolean) sessionState.get(session).get("terminate");
		List<Map.Entry<String, IAParameter>> mappings = (List<Map.Entry<String, IAParameter>>) sessionState.get(session).get("mappings");
		Integer iteration = (Integer) sessionState.get(session).get("iteration");
		IAObjectiveFunction objective = (IAObjectiveFunction) sessionState.get(session).get("objective");
		Integer iterState = (Integer) sessionState.get(session).get("state");

		IAExperimentBatch newExperiments = new AExperimentBatch("solutions");
		if (iteration > 0)
		{
			if (evaluations.getCount() >= 20)
			{
				// We have found an optimum.
				terminate = true;
				System.out.println("OPTIMUM!");
				double[] best = simplex.getPoint(0).getPoint();
				IAParameterEnsemble result = (IAParameterEnsemble) ((IAExperiment) state.get("baseExperiment")).getInputParameters().clonen();

				for (int j = 0; j < best.length; j++)
				{
					result.getParameter(mappings.get(j).getKey()).setValue(best[j]);
				}

				sessionState.get(session).put("optimum", result);
				sessionState.get(session).put("optimumValue", simplex.getPoint(0).getValue());
				for (double d : best)
				{
					System.out.print(d + "|");
				}
				System.out.println();
				state.put("terminate", terminate);
			}

			if (!terminate)
			{

				IAExperiment startExp = (IAExperiment) state.get("baseExperiment");

				if (iterState == 0)
				{
					RealPointValuePair[] lastsimplex = simplex.getPoints();
					for (IAExperiment exp : previousSolutions.getExperiments().values())
					{
						for (int k = 0; k < lastsimplex.length; k++)
						{
							Integer found = 0;
							RealPointValuePair realPointValuePair = lastsimplex[k];
							double[] point = realPointValuePair.getPoint();
							for (int j = 0; j < point.length; j++)
							{
								if (point[j] == (Double) exp.getInputParameter(mappings.get(j).getKey()).getValue()) found++;
							}
							if (found == point.length)
							{
								RealPointValuePair newPoint = new RealPointValuePair(getPointsOfExperiment(exp, mappings), (Double) objective.evaluate(exp.getOutputParameters()).get(susThread), true);
								lastsimplex[k] = newPoint;
							}
						}
					}
					Arrays.sort(lastsimplex, comparator);
					simplex.setSimplex(lastsimplex);
					

					
					

					System.out.println("Iteration" + iteration + ":");
					for (int i = 0; i < lastsimplex.length; i++)
					{
						System.out.print("Point" + i + ":");
						double[] poin = lastsimplex[i].getPoint();
						for (int j = 0; j < poin.length; j++)
						{
							System.out.print(poin[j] + ",");
						}
						System.out.println("=" + lastsimplex[i].getValue());
					}

					double[] reflected = simplex.iterate(comparator);
					state.put("state", new Integer(1));
					iteration++;
					sessionState.get(session).put("iteration", iteration);
					
					IAExperiment experiment = setNewExperiment(startExp, reflected, mappings, evaluations.getCount());
					newExperiments.addExperiment(experiment);
					
					try
					{
						evaluations.incrementCount();
						sessionState.get(session).put("evaluations", evaluations);
					}
					catch (MaxCountExceededException e)
					{}
				}
				else if (iterState == 1)
				{
					// reflect
					IAExperiment exp = (IAExperiment) previousSolutions.getExperiments().values().iterator().next();
					RealPointValuePair reflected = new RealPointValuePair(getPointsOfExperiment(exp, mappings), (Double) objective.evaluate(exp.getOutputParameters()).get(susThread), true);
					if (simplex.tryReflect(reflected, comparator))
					{
						state.put("state", new Integer(0));
						state.put("reflected", null);
					}
					else
					{
						double[] expanded = simplex.getExpand(reflected, comparator);
						if (expanded != null)
						{
							state.put("state", new Integer(2));
							state.put("reflected", reflected);
							IAExperiment experiment = setNewExperiment(startExp, expanded, mappings, evaluations.getCount());
							newExperiments.addExperiment(experiment);
							try
							{
								evaluations.incrementCount();
								sessionState.get(session).put("evaluations", evaluations);

							}
							catch (MaxCountExceededException e)
							{}
						}
						else
						{
							double[] outContracted = simplex.getOutContracted(reflected, comparator);
							if (outContracted != null)
							{
								state.put("state", new Integer(3));
								state.put("reflected", reflected);
								IAExperiment experiment = setNewExperiment(startExp, outContracted, mappings, evaluations.getCount());
								newExperiments.addExperiment(experiment);
								try
								{
									evaluations.incrementCount();
									sessionState.get(session).put("evaluations", evaluations);
									state.put("state", new Integer(2));

								}
								catch (MaxCountExceededException e)
								{}
							}
							else
							{
								double[] inContracted = simplex.getInContracted(reflected, comparator);
								state.put("state", new Integer(4));
								state.put("reflected", reflected);
								IAExperiment experiment = setNewExperiment(startExp, inContracted, mappings, evaluations.getCount());
								newExperiments.addExperiment(experiment);
								try
								{
									evaluations.incrementCount();
									sessionState.get(session).put("evaluations", evaluations);
								}
								catch (MaxCountExceededException e)
								{}
							}
						}
					}

				}
				else if (iterState == 2)
				{
					// expand!
					IAExperiment exp = (IAExperiment) previousSolutions.getExperiments().values().iterator().next();
					RealPointValuePair expanded = new RealPointValuePair(getPointsOfExperiment(exp, mappings), (Double) objective.evaluate(exp.getOutputParameters()).get(susThread), true);
					RealPointValuePair reflected = (RealPointValuePair) sessionState.get(session).get("reflected");
					simplex.tryExpand(reflected, expanded, comparator);
					state.put("state", new Integer(0));
					state.put("reflected", null);
				}
				else if (iterState == 3)
				{
					// outcontract!
					IAExperiment exp = (IAExperiment) previousSolutions.getExperiments().values().iterator().next();
					RealPointValuePair outContracted = new RealPointValuePair(getPointsOfExperiment(exp, mappings), (Double) objective.evaluate(exp.getOutputParameters()).get(susThread), true);
					RealPointValuePair reflected = (RealPointValuePair) sessionState.get(session).get("reflected");
					if (simplex.tryOutContracted(reflected, outContracted, comparator))
					{
						state.put("state", new Integer(0));
						state.put("reflected", null);
					}
					else
					{
						simplex.shrink(comparator);
						RealPointValuePair[] nextsimplex = simplex.getPoints();
						for (int i = 0; i < nextsimplex.length; i++)
						{
							final RealPointValuePair vertex = nextsimplex[i];
							final double[] point = vertex.getPointRef();

							if (Double.isNaN(vertex.getValue()))
							{
								IAExperiment experiment = setNewExperiment(startExp, point, mappings, evaluations.getCount());
								newExperiments.addExperiment(experiment);
								try
								{
									evaluations.incrementCount();
									sessionState.get(session).put("evaluations", evaluations);
								}
								catch (MaxCountExceededException e)
								{
									// omit here
								}
							}
						}
						state.put("state", new Integer(0));
						state.put("reflected", null);
					}

				}
				else if (iterState == 4)
				{
					// incontract!
					IAExperiment exp = (IAExperiment) previousSolutions.getExperiments().values().iterator().next();
					RealPointValuePair inContracted = new RealPointValuePair(getPointsOfExperiment(exp, mappings), (Double) objective.evaluate(exp.getOutputParameters()).get(susThread), true);
					RealPointValuePair reflected = (RealPointValuePair) sessionState.get(session).get("reflected");
					if (simplex.tryOutContracted(reflected, inContracted, comparator))
					{
						state.put("state", new Integer(0));
						state.put("reflected", null);
					}
					else
					{
						simplex.shrink(comparator);
						RealPointValuePair[] nextsimplex = simplex.getPoints();
						for (int i = 0; i < nextsimplex.length; i++)
						{
							final RealPointValuePair vertex = nextsimplex[i];
							final double[] point = vertex.getPointRef();

							if (Double.isNaN(vertex.getValue()))
							{
								IAExperiment experiment = setNewExperiment(startExp, point, mappings, evaluations.getCount());
								newExperiments.addExperiment(experiment);
								try
								{
									evaluations.incrementCount();
									sessionState.get(session).put("evaluations", evaluations);
								}
								catch (MaxCountExceededException e)
								{
									// omit here
								}
							}
						}
						state.put("state", new Integer(0));
						state.put("reflected", null);
					}

				}
			}
		}
		else
		{
			IAExperiment startExp = (IAExperiment) previousSolutions.getExperiments().values().iterator().next();
			state.put("baseExperiment", startExp);
			RealPointValuePair[] nextsimplex = simplex.getPoints();
			for (int i = 0; i < nextsimplex.length; i++)
			{
				final RealPointValuePair vertex = nextsimplex[i];
				final double[] point = vertex.getPointRef();

				if (Double.isNaN(vertex.getValue()))
				{
					IAExperiment experiment = setNewExperiment(startExp, point, mappings, evaluations.getCount());
					newExperiments.addExperiment(experiment);
					try
					{
						evaluations.incrementCount();
						sessionState.get(session).put("evaluations", evaluations);
					}
					catch (MaxCountExceededException e)
					{
						// omit here
					}
				}
			}
			state.put("state", new Integer(0));
			state.put("reflected", null);
			iteration++;
			sessionState.get(session).put("iteration", iteration);
		}
		
		//TODO: Hack for Constraints
		for (IAExperiment exp : newExperiments.getExperiments().values())
		{
			for (IAParameter para : exp.getInputParameters().getParameters().values())
			{
				if (((Double)para.getValue()) < 0.0 || ((Double)para.getValue()) > 1.0)
				{
					for (IAParameter opara : exp.getOutputParameters().getParameters().values())
					{
						opara.setValue(Double.MAX_VALUE);
					}
					exp.setEvaluated(true);
				}
			}
		}

		if (newExperiments.isEvaluated() && !terminate)
		{
			return nextSolutions(session, newExperiments);
		} else
		{
			return new Future(newExperiments);
		}
		

	}

	private double[] getPointsOfExperiment(IAExperiment exp, List<Entry<String, IAParameter>> mappings)
	{
		double[] points = new double[mappings.size()];
		int i = 0;
		for (Iterator iterator = mappings.iterator(); iterator.hasNext();)
		{
			Entry<String, IAParameter> entry = (Entry<String, IAParameter>) iterator.next();
			points[i] = (Double) exp.getInputParameter(entry.getKey()).getValue();
			i++;
		}
		return points;
	}

	private IAExperiment setNewExperiment(IAExperiment baseExp, double[] points, List<Entry<String, IAParameter>> mappings, int i)
	{
		IAExperiment exp = (IAExperiment) baseExp.clonen();
		for (int j = 0; j < points.length; j++)
		{
			exp.getInputParameter(mappings.get(j).getKey()).setValue(points[j]);
		}
		exp.setName("ExpEva" + i);
		return exp;
	}

	@Override
	public IFuture checkEndofOptimisation(UUID session)
	{
		return new Future((Boolean) sessionState.get(session).get("terminate"));
	}

	@Override
	public IFuture getOptimum(UUID session)
	{
		return new Future((IAParameterEnsemble) sessionState.get(session).get("optimum"));
	}

	@Override
	public IFuture getOptimumValue(UUID session)
	{
		return new Future((Double) sessionState.get(session).get("optimumValue"));
	}
}

// next iteration
// IAExperimentBatch newExperiments = new AExperimentBatch("solutions");
// if (!terminate)
// {
// //sort simplex
// Arrays.sort(lastsimplex, comparator);
// simplex.setNewSimplex(lastsimplex);
//
// RealPointValuePair[] evasimplex = simplex.getPoints();
// System.out.println("Evasimplex: ");
// for (int i = 0; i < evasimplex.length; i++)
// {
// System.out.print("Point" + i +":");
// double[] poin = evasimplex[i].getPoint();
// for (int j = 0; j < poin.length; j++)
// {
// System.out.print(poin[j] +",");
// }
// System.out.println("=" + evasimplex[i].getValue());
// }
//
//
// //check converged
// boolean converged = true;
// for (int i = 0; i < simplex.getSize(); i++)
// {
// RealPointValuePair last = lastsimplex[i];
// converged &= checker.converged(evaluations.getCount(), last, simplex.getPoint(i));
// }
// if (converged && evaluations.getCount() >= 20)
// {
// // We have found an optimum.
// terminate = true;
// System.out.println("OPTIMUM!");
// double[] best = lastsimplex[0].getPoint();
// for (double d : best)
// {
// System.out.print(d +"|");
// }
// System.out.println();
// state.put("terminate", terminate);
// }
// }
// System.out.println("-OUT-");
// iteration++;
// sessionState.get(session).put("iteration", iteration);

// for (IAExperiment exp : previousSolutions.getExperiments().values())
// {
// for (int i = 0; i < lastsimplex.length; i++)
// {
// Integer found = 0;
// RealPointValuePair realPointValuePair = lastsimplex[i];
// double[] point = realPointValuePair.getPoint();
// for (int j = 0; j < point.length; j++)
// {
// if (point[j] == (Double)exp.getInputParameter(mappings.get(j).getKey()).getValue()) found++;
// }
// if (found == point.length)
// {
// RealPointValuePair newPoint = new RealPointValuePair(getPointsOfExperiment(exp, mappings), (Double) objective.evaluate(exp.getOutputParameters()).get(susThread), true);
// lastsimplex[i] = newPoint;