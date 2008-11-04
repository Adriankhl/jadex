package jadex.bdi.planlib.simsupport.environment;

import jadex.bdi.planlib.simsupport.common.graphics.drawable.IDrawable;
import jadex.bdi.planlib.simsupport.common.graphics.layer.ILayer;
import jadex.bdi.planlib.simsupport.common.math.IVector1;
import jadex.bdi.planlib.simsupport.common.math.IVector2;
import jadex.bdi.planlib.simsupport.environment.process.IEnvironmentProcess;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class EuclideanSimulationEngine implements ISimulationEngine
{
	/** Pre-layers
	 */
	private List preLayers_;
	
	/** Post-layers
	 */
	private List postLayers_;
	
	/** The environment processes.
	 */
	private List processes_;
	
	/** Integers/ObjectIDs (keys) and SimObject engine objects (values)
	 */
	private Map simObjects_;
	
	/** Object ID counter for new IDs
	 */
	private AtomicCounter objectIdCounter_;
	
	/** Stack with free object IDs
	 */
	private Stack freeObjectIds_;
	
	/** Area size
	 */
	private IVector2 areaSize_;
	
	/** Creates a new DefaultSimulationEngine
	 * 
	 *  @param title simulation title
	 *  @param areaSize size of the simulated area
	 */
	public EuclideanSimulationEngine(String title,
									 IVector2 areaSize)
	{
		objectIdCounter_ = new AtomicCounter();
		processes_ = Collections.synchronizedList(new ArrayList());
		preLayers_ = Collections.synchronizedList(new ArrayList());
		postLayers_ = Collections.synchronizedList(new ArrayList());
		simObjects_ = Collections.synchronizedMap(new HashMap());
		freeObjectIds_ = new Stack();
		areaSize_ = areaSize.copy();
	}
	
	/** Adds a new SimObject to the simulation.
	 *  
	 *  @param type type of the object
	 *  @param position position of the object
	 *  @param velocity velocity of the object
	 *  @param drawables drawable representing the object
	 *  @return the simulation object ID
	 */
	public Integer createSimObject(String type,
								   IVector2 position,
								   IVector2 velocity,
								   IDrawable drawable)
	{
		Integer id;
		synchronized(freeObjectIds_)
		{
			if (!freeObjectIds_.empty())
			{
				id = (Integer) freeObjectIds_.pop();
			}
			else
			{
				id = objectIdCounter_.getNext();
			}
		}
		SimObject simObject = new SimObject(id, type, position, velocity, drawable);
		simObjects_.put(id, simObject);
		return id;
	}
	
	/** Removes a SimObject from the simulation.
	 * 
	 *  @param objectId the simulation object ID
	 */
	public void destroySimObject(Integer objectId)
	{
		simObjects_.remove(objectId);
		freeObjectIds_.push(objectId);
	}
	
	/** Adds a pre-layer (background).
	 * 
	 *  @param preLayer new pre-layer
	 */
	public void addPreLayer(ILayer preLayer)
	{
		preLayers_.add(preLayer);
	}
	
	/** Removes a pre-layer (background).
	 * 
	 *  @param preLayer the pre-layer
	 */
	public void removePreLayer(ILayer preLayer)
	{
		preLayers_.remove(preLayer);
	}
	
	/** Adds a post-layer.
	 * 
	 *  @param postLayer new post-layer
	 */
	public void addPostLayer(ILayer postLayer)
	{
		postLayers_.add(postLayer);
	}
	
	/** Removes a post-layer.
	 * 
	 *  @param preLayer new post-layer
	 */
	public void removePostLayer(ILayer postLayer)
	{
		postLayers_.remove(postLayer);
	}
	
	/** Adds an environment process.
	 * 
	 *  @param process new environment process
	 */
	public void addEnvironmentProcess(IEnvironmentProcess process)
	{
		processes_.add(process);
	}
	
	/** Removes an environment process.
	 * 
	 *  @param process the environment process
	 */
	public void removeEnvironmentProcess(IEnvironmentProcess process)
	{
		processes_.remove(process);
	}
	
	/** Retrieves a simulation object.
	 *  
	 *  @param objectId the simulation object ID
	 *  @return current the simulated object
	 */
	public SimObject getSimulationObject(Integer objectId)
	{
		SimObject simObject = (SimObject) simObjects_.get(objectId);
		return simObject;
	}
	
	/** Returns the size of the simulated area.
	 *  
	 *  @return size of the simulated area
	 */
	public IVector2 getAreaSize()
	{
		return areaSize_.copy();
	}
	
	/** Retrieves a random position within the simulation area with a minimum
	 *  distance from the edge.
	 *  
	 *  @param distance minimum distance from the edge
	 */
	public IVector2 getRandomPosition(IVector2 distance)
	{
		IVector2 position = areaSize_.copy();
		position.subtract(distance);
		position.randomX(distance.getX(),
						 position.getX());
		position.randomY(distance.getY(),
						 position.getY());
		return position;
	}
	
	/** Returns direct access to the pre-layers.
	 * 
	 *  @return direct access to pre-layers
	 */
	public List getPreLayerAccess()
	{
		return preLayers_;
	}
	
	/** Returns direct access to the post-layers.
	 * 
	 *  @return direct access to post-layers
	 */
	public List getPostLayerAccess()
	{
		return postLayers_;
	}
	
	/** Returns direct access to the simulation objects.\
	 * 
	 *  @return direct access to simulation objects
	 */
	public Map getSimObjectAccess()
	{
		return simObjects_;
	}
	
	/** Progresses the simulation.
	 * 
	 * @param deltaT time difference since the last step
	 */
	public void simulateStep(IVector1 deltaT)
	{
		updatePositions(deltaT);
		executeEnvironmentProcesses(deltaT);
	}
	
	/** Updates the positions of objects.
	 * 
	 * @param deltaT time difference since the last step
	 */
	private void updatePositions(IVector1 deltaT)
	{
		synchronized (simObjects_)
		{
			for (Iterator it = simObjects_.values().iterator(); it.hasNext(); )
			{
				SimObject simObject = (SimObject) it.next();

				simObject.updatePosition(deltaT);
			}
		}
	}
	
	/** Executes the environment processes.
	 * 
	 * @param deltaT time difference since the last step
	 */
	private void executeEnvironmentProcesses(IVector1 deltaT)
	{
		synchronized(processes_)
		{
			for (Iterator it = processes_.iterator(); it.hasNext(); )
			{
				IEnvironmentProcess process = (IEnvironmentProcess) it.next();
				
				// Use a copy of the time delta to prevent direct access
				// by the environment process.
				process.execute(deltaT.copy(), this);
			}
		}
	}
	/** Synchronized counter class
	 */
	private class AtomicCounter
	{
		int count_;
		
		public AtomicCounter()
		{
			count_ = 0;
		}
		
		public synchronized Integer getNext()
		{
			return new Integer(count_++);
		}
	}
}