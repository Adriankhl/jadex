package jadex.bpmn.model;

import java.util.ArrayList;
import java.util.List;

/**
 *  A pool represents some kind of unit inside a bpmn model.
 */
public class MPool extends MAssociationTarget
{
	//-------- attributes --------
	
	/** The vertices. */
	protected List activities;
	
	/** The sequence edges. */
	protected List sequenceedges;
	
	/** The pools. */
	protected List lanes;
	
	/** The artifacs. */
	protected List artifacts;
	
	/** The type. */
	protected String type;
	
	//-------- methods --------
	
	/**
	 *  Get the activities.
	 *  @return The activities.
	 */
	public List getActivities()
	{
		return activities;
	}
	
	/**
	 *  Add an activity.
	 *  @param activity The activity.
	 */
	public void addActivity(MActivity activity)
	{
		if(activities==null)
			activities = new ArrayList();
		activities.add(activity);
	}
	
	/**
	 *  Remove an activity.
	 *  @param activity The activity.
	 */
	public void removeActivity(MActivity activity)
	{
		if(activities!=null)
			activities.remove(activity);
	}
	
	/**
	 *  Get the sequence edges.
	 *  @return The edges. 
	 */
	public List getSequenceEdges()
	{
		return sequenceedges;
	}
	
	/**
	 *  Add a sequence edge.
	 *  @param edge The edge.
	 */
	public void addSequenceEdge(MSequenceEdge edge)
	{
		if(sequenceedges==null)
			sequenceedges = new ArrayList();
		sequenceedges.add(edge);
	}
	
	/**
	 *  Remove a sequence edge.
	 *  @param edge The edge.
	 */
	public void removeSequenceEdge(MSequenceEdge edge)
	{
		if(sequenceedges!=null)
			sequenceedges.remove(edge);
	}
	
	/**
	 *  Get the lanes.
	 *  @return The lanes.
	 */
	public List getLanes()
	{
		return lanes;
	}
	
	/**
	 *  Add a lane.
	 *  @param lane The lane. 
	 */
	public void addLane(MLane lane)
	{
		if(lanes==null)
			lanes = new ArrayList();
		lanes.add(lane);
	}
	
	/**
	 *  Remove a lane.
	 *  @param lane The lane.
	 */
	public void removeLane(MLane lane)
	{
		if(lanes!=null)
			lanes.remove(lane);
	}
	
	/**
	 *  Get the artifacts.
	 *  @return The artifacts.
	 */
	public List getArtifacts()
	{
		return artifacts;
	}
	
	/**
	 *  Add an artifact.
	 *  @param artifact The artifact.
	 */
	public void addArtifact(MArtifact artifact)
	{
		if(artifacts==null)
			artifacts = new ArrayList();
		artifacts.add(artifact);
	}
	
	/**
	 *  Remove an artifact.
	 *  @param artifact The artifact.
	 */
	public void removeArtifact(MArtifact artifact)
	{
		if(artifacts!=null)
			artifacts.remove(artifact);
	}
	
	/**
	 *  Get the type.
	 *  @return The type.
	 */
	public String getType()
	{
		return this.type;
	}

	/**
	 *  Set the type.
	 *  @param type The type to set.
	 */
	public void setType(String type)
	{
		this.type = type;
	}
	
	/**
	 *  Get all start events of the pool.
	 *  @return A non-empty List of start events or null, if none.
	 */
	public List getStartEvents()
	{
		List	ret	= null;
		for(int i=0; activities!=null && i<activities.size(); i++)
		{
			MActivity	act	= (MActivity)activities.get(i);
			// Todo: use constants
			// Todo: other start event types.
			if("EventStartEmpty".equals(act.getActivityType()))
			{
				if(ret==null)
					ret	= new ArrayList();
				ret.add(act);
			}
		}
		return ret;
	}
	
	
}
