package jadex.bdi.model.impl.flyweights;

import jadex.bdi.model.IMBelief;
import jadex.bdi.model.IMBeliefSet;
import jadex.bdi.model.IMBeliefbase;
import jadex.bdi.model.OAVBDIMetaModel;
import jadex.rules.state.IOAVState;

import java.util.Collection;
import java.util.Iterator;

/**
 *  Flyweight for the belief base model.
 */
public class MBeliefbaseFlyweight extends MElementFlyweight implements IMBeliefbase 
{
	//-------- constructors --------
	
	/**
	 *  Create a new beliefbase flyweight.
	 *  @param state	The state.
	 *  @param scope	The scope handle.
	 */
	private MBeliefbaseFlyweight(IOAVState state, Object scope)
	{
		super(state, scope, scope);
	}
	
	//-------- methods concerning beliefs --------

    /**
	 *  Get a belief for a name.
	 *  @param name	The belief name.
	 */
	public IMBelief getBelief(final String name)
	{
		if(getInterpreter().isExternalThread())
		{
			AgentInvocation invoc = new AgentInvocation(name)
			{
				public void run()
				{
					Object handle = getState().getAttributeValue(getScope(), OAVBDIMetaModel.capability_has_beliefs, name);
					if(handle==null)
						throw new RuntimeException("Belief not found: "+name);
					object = new MBeliefFlyweight(getState(), getScope(), handle);
				}
			};
			return (IMBelief)invoc.object;
		}
		else
		{
			Object handle = getState().getAttributeValue(getScope(), OAVBDIMetaModel.capability_has_beliefs, name);
			if(handle==null)
				throw new RuntimeException("Belief not found: "+name);
			return new MBeliefFlyweight(getState(), getScope(), handle);
		}
	}

	/**
	 *  Get a belief set for a name.
	 *  @param name	The belief set name.
	 */
	public IMBeliefSet getBeliefSet(final String name)
	{
		if(getInterpreter().isExternalThread())
		{
			AgentInvocation invoc = new AgentInvocation(name)
			{
				public void run()
				{
					Object handle = getState().getAttributeValue(getScope(), OAVBDIMetaModel.capability_has_beliefsets, name);
					if(handle==null)
						throw new RuntimeException("Beliefset not found: "+name);
					object = new MBeliefFlyweight(getState(), getScope(), handle);
				}
			};
			return (IMBeliefSet)invoc.object;
		}
		else
		{
			Object handle = getState().getAttributeValue(getScope(), OAVBDIMetaModel.capability_has_beliefsets, name);
			if(handle==null)
				throw new RuntimeException("Beliefset not found: "+name);
			return new MBeliefSetFlyweight(getState(), getScope(), handle);
		}
	}

	/**
	 *  Returns all beliefs.
	 *  @return All beliefs.
	 */
	public IMBelief[] getBeliefs()
	{
		if(getInterpreter().isExternalThread())
		{
			AgentInvocation invoc = new AgentInvocation()
			{
				public void run()
				{
					Collection bels = (Collection)getState().getAttributeValue(getScope(), OAVBDIMetaModel.capability_has_beliefs);
					IMBelief[] ret = new IMBelief[bels==null? 0: bels.size()];
					if(bels!=null)
					{
						int i=0;
						for(Iterator it=bels.iterator(); it.hasNext(); )
						{
							ret[i++] = new MBeliefFlyweight(getState(), getScope(), it.next());
						}
					}
					object = ret;
				}
			};
			return (IMBelief[])invoc.object;
		}
		else
		{
			Collection bels = (Collection)getState().getAttributeValue(getScope(), OAVBDIMetaModel.capability_has_beliefs);
			IMBelief[] ret = new IMBelief[bels==null? 0: bels.size()];
			if(bels!=null)
			{
				int i=0;
				for(Iterator it=bels.iterator(); it.hasNext(); )
				{
					ret[i++] = new MBeliefFlyweight(getState(), getScope(), it.next());
				}
			}
			return ret;
		}
	}

	/**
	 *  Return all belief sets.
	 *  @return All belief sets.
	 */
	public IMBeliefSet[] getBeliefSets()
	{
		if(getInterpreter().isExternalThread())
		{
			AgentInvocation invoc = new AgentInvocation()
			{
				public void run()
				{
					Collection belsets = (Collection)getState().getAttributeValue(getScope(), OAVBDIMetaModel.capability_has_beliefsets);
					IMBeliefSet[] ret = new IMBeliefSet[belsets==null? 0: belsets.size()];
					if(belsets!=null)
					{
						int i=0;
						for(Iterator it=belsets.iterator(); it.hasNext(); )
						{
							ret[i++] = new MBeliefSetFlyweight(getState(), getScope(), it.next());
						}
					}
					object = ret;
				}
			};
			return (IMBeliefSet[])invoc.object;
		}
		else
		{
			Collection belsets = (Collection)getState().getAttributeValue(getScope(), OAVBDIMetaModel.capability_has_beliefsets);
			IMBeliefSet[] ret = new IMBeliefSet[belsets==null? 0: belsets.size()];
			if(belsets!=null)
			{
				int i=0;
				for(Iterator it=belsets.iterator(); it.hasNext(); )
				{
					ret[i++] = new MBeliefSetFlyweight(getState(), getScope(), it.next());
				}
			}
			return ret;
		}
	}
}