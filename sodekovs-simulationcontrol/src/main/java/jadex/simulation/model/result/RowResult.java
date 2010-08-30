package jadex.simulation.model.result;

import jadex.simulation.model.ObservedEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import cern.colt.list.DoubleArrayList;
import cern.jet.stat.Descriptive;

@XmlRootElement(name = "RowResults")
public class RowResult extends IResult {

	private ArrayList<ExperimentResult> experimentResults = new ArrayList<ExperimentResult>();
	private String optimizationName;
	private String optimizationValue;
	
	// --- contains the FINAL results of the statistical evaluation for
	// each observer type, i.e. the statics for each observed type for this row.
	private HashMap<String, HashMap<String,String>> finalStatsMap;

	@XmlElementWrapper(name = "Experiments")
	@XmlElement(name = "Experiment")
	public ArrayList<ExperimentResult> getExperimentsResults() {
		return experimentResults;
	}

	public void setExperimentsResults(ArrayList<ExperimentResult> experimentResults) {
		this.experimentResults = experimentResults;
	}

	public void addExperimentsResults(ExperimentResult experimentResult) {
		this.experimentResults.add(experimentResult);
	}

	public String getOptimizationName() {
		return optimizationName;
	}

	public void setOptimizationName(String optimizationName) {
		this.optimizationName = optimizationName;
	}

	public String getOptimizationValue() {
		return optimizationValue;
	}

	public void setOptimizationValue(String optimizationValue) {
		this.optimizationValue = optimizationValue;
	}

	/**
	 * Returns the duration of the row
	 * 
	 * @return
	 */
	@XmlAttribute(name = "RowDuration")
	public long getDuraration() {
		return getEndtime() - getStarttime();
	}

	@XmlAttribute(name = "RowNumber")
	public String getId() {
		return id;
	}

	@XmlAttribute(name = "Name")
	public String getName() {
		return name;
	}
	
	/**
	 * 1. HashMap: Key: Name of observer 2. HashMap: Key: Name of statistical
	 * type, Value: computed value
	 * 
	 * @return
	 */
	public HashMap<String, HashMap<String, String>> getFinalStatsMap() {
		return finalStatsMap;
	}

	public void setFinalStatsMap(HashMap<String, HashMap<String, String>> statsMap) {
		this.finalStatsMap = statsMap;
	}

	public String toStringShort() {
//		double meanValue = 0.0;
		DoubleArrayList durations = new DoubleArrayList();

		StringBuffer buffer = new StringBuffer();
		buffer.append("Row Number: ");
		buffer.append(getId());
		buffer.append("\n");
		buffer.append("Optimization: Parameter Name and Value: ");
		buffer.append(getOptimizationName());
		buffer.append(" - ");
		buffer.append(getOptimizationValue());
		buffer.append("\n\n");
		buffer.append("Cumulated Stats of Observed Events:");
		//Print out cumulated stats for each type of observed event
		for (Iterator it = finalStatsMap.keySet().iterator(); it.hasNext();) {
			Object key = it.next();
			HashMap<String,String> resForEvent = finalStatsMap.get(key);
			buffer.append("\n\tName: " + key);
			buffer.append("\tMeanValue: " + resForEvent.get("MeanValue"));
			buffer.append("\tMedianValue: " + resForEvent.get("MedianValue"));
			buffer.append("\tSampleVarianceValue: " + resForEvent.get("sampleVarianceValue"));				
		}
		
		buffer.append("\n");
		buffer.append("Results of Single Experiment: ");
		buffer.append("\n");

		for (ExperimentResult experiment : getExperimentsResults()) {
//			buffer.append(experiment.toStringShort());
			buffer.append("\t");
			buffer.append("ID: ");
			buffer.append(experiment.getId());
			buffer.append("\t");
			buffer.append("Duration: ");
			buffer.append(experiment.getDuraration());
			buffer.append("\n");
			
//			for (Iterator it = finalStatsMap.keySet().iterator(); it.hasNext();) {
//				Object key = it.next();
//				HashMap<String,String> resForEvent = finalStatsMap.get(key);
//				buffer.append("\n\tName: " + key);
//				buffer.append("\tMeanValue: " + resForEvent.get("MeanValue"));
//				buffer.append("\tMedianValue: " + resForEvent.get("MedianValue"));
//				buffer.append("\tSampleVarianceValue: " + resForEvent.get("sampleVarianceValue"));				
//			}
			
//			meanValue += experiment.getDuraration();
			durations.add(experiment.getDuraration());
		}
		//Eval:		
		//list has to be ordered according to the Colt API
		durations.sort();
		double durationTimeMean = Descriptive.mean(durations);
		double durationTimeMedian = Descriptive.median(durations);
		double durationTimeSampleVariance = Descriptive.sampleVariance(durations, durationTimeMean);
		buffer.append("\t");
		buffer.append( "Duration Time Stats:  Mean value: " + durationTimeMean + ", Median value: " + durationTimeMedian + ", Sample Variance Value: " +  durationTimeSampleVariance);		
				
		
		//Hack: to be able to copy the values easily in a new colt.evaluation
		buffer.append("\n\t");
		for(int i=0; i<durations.size(); i++){
			buffer.append(durations.get(i));
			buffer.append(",");
		}
		
		buffer.append("\n");
		return buffer.toString();
	}
	
}
