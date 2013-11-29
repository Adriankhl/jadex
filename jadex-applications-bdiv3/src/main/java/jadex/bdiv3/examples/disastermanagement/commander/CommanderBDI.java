package jadex.bdiv3.examples.disastermanagement.commander;

import jadex.bdiv3.BDIAgent;
import jadex.bdiv3.annotation.Belief;
import jadex.bdiv3.annotation.Body;
import jadex.bdiv3.annotation.Capability;
import jadex.bdiv3.annotation.Deliberation;
import jadex.bdiv3.annotation.Goal;
import jadex.bdiv3.annotation.GoalCreationCondition;
import jadex.bdiv3.annotation.GoalInhibit;
import jadex.bdiv3.annotation.GoalTargetCondition;
import jadex.bdiv3.annotation.Plan;
import jadex.bdiv3.annotation.Plans;
import jadex.bdiv3.annotation.RawEvent;
import jadex.bdiv3.annotation.Trigger;
import jadex.bdiv3.examples.disastermanagement.IClearChemicalsService;
import jadex.bdiv3.examples.disastermanagement.IExtinguishFireService;
import jadex.bdiv3.examples.disastermanagement.ITreatVictimsService;
import jadex.bdiv3.examples.disastermanagement.commander.CommanderBDI.ClearChemicals;
import jadex.bdiv3.examples.disastermanagement.commander.CommanderBDI.ExtinguishFires;
import jadex.bdiv3.examples.disastermanagement.commander.CommanderBDI.HandleDisaster;
import jadex.bdiv3.examples.disastermanagement.commander.CommanderBDI.SendRescueForce;
import jadex.bdiv3.examples.disastermanagement.commander.CommanderBDI.TreatVictims;
import jadex.bdiv3.examples.disastermanagement.movement.MovementCapa;
import jadex.bdiv3.runtime.ChangeEvent;
import jadex.bridge.service.IService;
import jadex.extension.envsupport.environment.ISpaceObject;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.AgentBody;
import jadex.micro.annotation.RequiredService;
import jadex.micro.annotation.RequiredServices;
import jadex.rules.eca.EventType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RequiredServices(
{
	@RequiredService(name="treatvictimservices", type=ITreatVictimsService.class, multiple=true),
	@RequiredService(name="extinguishfireservices", type=IExtinguishFireService.class, multiple=true),
	@RequiredService(name="clearchemicalsservices", type=IClearChemicalsService.class, multiple=true)
})
@Plans(
{
	@Plan(trigger=@Trigger(goals=HandleDisaster.class), body=@Body(HandleDisasterPlan.class)),
	@Plan(trigger=@Trigger(goals=ClearChemicals.class), body=@Body(HandleFireBrigadesClearChemicalsPlan.class)),
	@Plan(trigger=@Trigger(goals=ExtinguishFires.class), body=@Body(HandleFireBrigadesExtinguishFiresPlan.class)),
	@Plan(trigger=@Trigger(goals=TreatVictims.class), body=@Body(HandleAmbulancesPlan.class)),
	@Plan(trigger=@Trigger(goals=SendRescueForce.class), body=@Body(ClearChemicalsPlan.class)),
	@Plan(trigger=@Trigger(goals=SendRescueForce.class), body=@Body(ExtinguishFirePlan.class)),
	@Plan(trigger=@Trigger(goals=SendRescueForce.class), body=@Body(TreatVictimsPlan.class)),
})
@Agent
public class CommanderBDI
{
	/** The agent. */
	@Agent
	protected BDIAgent agent;
	
	/** The capa. */
	@Capability
	protected MovementCapa movecapa = new MovementCapa();
	
	/** The disasters. */
	@Belief//((updaterate=1000)
//	protected ISpaceObject[] disasters = new ISpaceObject[0];//movecapa.getEnvironment().getSpaceObjectsByType("disaster");
	protected Set<ISpaceObject> disasters = new HashSet<ISpaceObject>();
	
	/** The busy entities. */
	@Belief // to watch in debugger
	protected List<Object> busyentities = new ArrayList<Object>();
	
	/**
	 * 
	 */
	@AgentBody
	public void body()
	{
		while(true)
		{
			agent.waitForDelay(1000).get();
			ISpaceObject[] dis = movecapa.getEnvironment().getSpaceObjectsByType("disaster");
			for(ISpaceObject di: dis)
			{
				if(!disasters.contains(di))
				{
					disasters.add(di);
				}
			}
		}
	}
	
	@Goal(unique=true, deliberation=@Deliberation(inhibits={HandleDisaster.class}))
	public class HandleDisaster
	{
		/** The disaster. */
		protected ISpaceObject disaster;

		/**
		 *  Create a new HandleDisaster. 
		 */
		@GoalCreationCondition(rawevents=@RawEvent(value=ChangeEvent.FACTADDED, second="disasters"))
		public HandleDisaster(ISpaceObject disaster)
		{
			this.disaster = disaster;
		}
		
		/**
		 * 
		 */
		// Current limitation: As the target condition is checked only when plan finished the 
		// completion of a disaster is not immediately noticed.
		@GoalTargetCondition(rawevents=@RawEvent(ChangeEvent.PLANFINISHED)) // Hack! todo: allow for detecting goal parameter changes
		public boolean checkTarget()
		{
//			System.out.println("check target of: "+this);
			Integer fires = (Integer)disaster.getProperty("fire");
			Integer chems = (Integer)disaster.getProperty("chemicals");
			Integer vics = (Integer)disaster.getProperty("victims");
			return fires.intValue()==0 && chems.intValue()==0 && vics.intValue()==0;
		}
		
		/**
		 * 
		 */
		@GoalInhibit(HandleDisaster.class)
		protected boolean inhibitHandlerDisaster(HandleDisaster other)
		{
			// this goal inhibits other if its waste is currently transported
			Boolean severe = (Boolean)getDisaster().getProperty("severe");
			Boolean othsevere = (Boolean)other.getDisaster().getProperty("severe");
			
			return severe.booleanValue() && !othsevere.booleanValue();
		}

		/**
		 *  Get the disaster.
		 *  @return The disaster.
		 */
		public ISpaceObject getDisaster()
		{
			return disaster;
		}
		
		// hashcode and equals implementation for unique flag
		
		/**
		 *  Get the hashcode.
		 */
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + disaster.hashCode();
			return result;
		}

		/**
		 *  Test if equal to other goal.
		 *  @param obj The other object.
		 *  @return True, if equal.
		 */
		public boolean equals(Object obj)
		{
			boolean ret = false;
			if(obj instanceof HandleDisaster)
			{
				HandleDisaster other = (HandleDisaster)obj;
				ret = getOuterType().equals(other.getOuterType()) && getDisaster().equals(other.getDisaster());
			}
			return ret;
		}

		/**
		 *  Get the outer type.
		 *  @return The outer type.
		 */
		private CommanderBDI getOuterType()
		{
			return CommanderBDI.this;
		}
	}

	@Goal(unique=true , deliberation=@Deliberation(inhibits={TreatVictims.class}))
	public class ClearChemicals implements IForcesGoal
	{
		/** The disaster. */
		protected ISpaceObject disaster;
		
		/** The units. */
		protected Collection<Object> units;

		/**
		 *  Create a new ClearChemicals. 
		 */
		public ClearChemicals(ISpaceObject disaster)//, Collection<Object> units)
		{
			this.disaster = disaster;
//			this.units = units;
			this.units = new ArrayList<Object>();
		}

		/**
		 *  Get the disaster.
		 *  @return The disaster.
		 */
		public ISpaceObject getDisaster()
		{
			return disaster;
		}

		/**
		 *  Get the units.
		 *  @return The units.
		 */
		public Collection<Object> getUnits()
		{
			return units;
		}

		/**
		 * 
		 */
		@GoalTargetCondition(rawevents=@RawEvent(ChangeEvent.PLANFINISHED)) // Hack! todo: allow for detecting goal parameter changes
		public boolean checkTarget()
		{
			return ((Integer)getDisaster().getProperty("chemicals"))==0;
		}
		
		/**
		 *  Inhibit other achieve cleanup goals that 
		 *  are farer away from the cleaner.
		 */
		@GoalInhibit(TreatVictims.class)
		protected boolean inhibitAchieveCleanUp(TreatVictims other)
		{
			return other.getDisaster().equals(disaster);
		}
	}
	
	@Goal
	public class ExtinguishFires implements IForcesGoal
	{
		/** The disaster. */
		protected ISpaceObject disaster;
		
		/** The units. */
		protected Collection<Object> units;

		/**
		 *  Create a new ClearChemicals. 
		 */
		public ExtinguishFires(ISpaceObject disaster)//, Collection<Object> units)
		{
			this.disaster = disaster;
//			this.units = units;
			this.units = new ArrayList<Object>();
		}

		/**
		 *  Get the disaster.
		 *  @return The disaster.
		 */
		public ISpaceObject getDisaster()
		{
			return disaster;
		}

		/**
		 *  Get the units.
		 *  @return The units.
		 */
		public Collection<Object> getUnits()
		{
			return units;
		}

		/**
		 * 
		 */
		@GoalTargetCondition(rawevents=@RawEvent(ChangeEvent.PLANFINISHED)) // Hack! todo: allow for detecting goal parameter changes
		public boolean checkTarget()
		{
			return ((Integer)getDisaster().getProperty("fire"))==0;
		}
	}
	
	@Goal
	public class TreatVictims implements IForcesGoal
	{
		/** The disaster. */
		protected ISpaceObject disaster;
		
		/** The units. */
		protected Collection<Object> units;

		/**
		 *  Create a new TreatVictims. 
		 */
		public TreatVictims(ISpaceObject disaster)//, Collection<Object> units)
		{
			this.disaster = disaster;
//			this.units = units;
			this.units = new ArrayList<Object>();
		}

		/**
		 *  Get the disaster.
		 *  @return The disaster.
		 */
		public ISpaceObject getDisaster()
		{
			return disaster;
		}

		/**
		 *  Get the units.
		 *  @return The units.
		 */
		public Collection<Object> getUnits()
		{
			return units;
		}

		/**
		 * 
		 */
		@GoalTargetCondition(rawevents=@RawEvent(ChangeEvent.PLANFINISHED)) // Hack! todo: allow for detecting goal parameter changes
		public boolean checkTarget()
		{
			return ((Integer)getDisaster().getProperty("victims"))==0;
		}
	}
	
	@Goal
	public class SendRescueForce
	{
		/** The disaster. */
		protected ISpaceObject disaster;
		
		/** The units. */
		protected IService rescueforce;

		/**
		 *  Create a new SendRescueForce. 
		 */
		public SendRescueForce(ISpaceObject disaster, IService rescueforce)
		{
			this.disaster = disaster;
			this.rescueforce = rescueforce;
		}

		/**
		 *  Get the disaster.
		 *  @return The disaster.
		 */
		public ISpaceObject getDisaster()
		{
			return disaster;
		}

		/**
		 *  Get the rescueforce.
		 *  @return The rescueforce.
		 */
		public IService getRescueForce()
		{
			return rescueforce;
		}
	}

	/**
	 *  Get the movecapa.
	 *  @return The movecapa.
	 */
	public MovementCapa getMoveCapa()
	{
		return movecapa;
	}

	/**
	 *  Get the agent.
	 *  @return The agent.
	 */
	public BDIAgent getAgent()
	{
		return agent;
	}

	/**
	 *  Get the disasters.
	 *  @return The disasters.
	 */
	public Set<ISpaceObject> getDisasters()
	{
		return disasters;
	}

	/**
	 *  Get the busyentities.
	 *  @return The busyentities.
	 */
	public List<Object> getBusyEntities()
	{
		return busyentities;
	}
}


