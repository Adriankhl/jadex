package jadex.simulation.analysis.buildingBlocks.optimisation.opt4j;

import java.util.ArrayList;
import java.util.Collection;

import org.opt4j.common.random.RandomDefault;
import org.opt4j.core.Archive;
import org.opt4j.core.Individual;
import org.opt4j.operator.crossover.BasicCrossoverModule;
import org.opt4j.operator.crossover.CrossoverDoubleDefault;
import org.opt4j.operator.crossover.CrossoverDoubleSBX;
import org.opt4j.operator.crossover.CrossoverModule;
import org.opt4j.operator.neighbor.BasicNeighborModule;
import org.opt4j.operator.normalize.NormalizeDoubleBorder;
import org.opt4j.optimizer.ea.EvolutionaryAlgorithmModule;
import org.opt4j.optimizer.sa.CoolingScheduleLinear;
import org.opt4j.optimizer.sa.CoolingSchedulesModule;
import org.opt4j.optimizer.sa.SimulatedAnnealingModule;
import org.opt4j.start.Opt4JTask;
import org.opt4j.viewer.ViewerModule;

import com.google.inject.Module;

public class SimulationStarter {
	
	public static void main(String[] args) {
		
		EvolutionaryAlgorithmModule ea = new EvolutionaryAlgorithmModule();
		ea.setGenerations(10);
		ea.setAlpha(4);
//		SimulatedAnnealingModule sa = new SimulatedAnnealingModule();
//		sa.setIterations(10);
//		CoolingSchedulesModule cs = new CoolingSchedulesModule();
//		cs.bindCoolingSchedule(CoolingScheduleLinear.class);
//		cs.setType( CoolingSchedulesModule.Type.LINEAR);
//		cs.setInitialTemperature(30000);
//		cs.setFinalTemperature(1);

		SimulationModule sm = new SimulationModule();
//		BasicNeighborModule com = new BasicNeighborModule();
//		CrossoverDoubleSBX cdd = new CrossoverDoubleSBX(2,new NormalizeDoubleBorder(), new RandomDefault())
//		com.addNeighbor(VariableDoubleNeighbor.class);
		ViewerModule viewer = new ViewerModule();
		viewer.setCloseOnStop(true);
		                
		Collection<Module> modules = new ArrayList<Module>();
		modules.add(ea);
//		modules.add(com);
//		modules.add(sa);
//		modules.add(cs);
		modules.add(sm);
		modules.add(viewer);

		Opt4JTask task = new Opt4JTask(false);
		task.init(modules);

		try {
		        task.execute();
		        Archive archive = task.getInstance(Archive.class);

		        for (Individual individual : archive) {
		                //...
		        }
		        Thread.currentThread().wait();
		} catch (Exception e) {
		        e.printStackTrace();
		} finally {
//		        task.close();
		} 
		
	}
	
	
}
