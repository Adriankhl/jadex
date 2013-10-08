package jadex.bridge;

import java.util.UUID;

/**
 *  A cause is used to link events. It has a source and a target id that
 *  can be used to chain the events. Events roll the causes by using the
 *  target as source and creating a new target etc.
 */
public class Cause
{
	/** The id. Identical for all events of the same origin. */
	protected String chainid;

	/** The source id. */
	protected String sourceid;
	
	/** The target id. */
	protected String targetid;
	
	/** The source name. */
	protected String sourcename;
	
	/** The target name. */
	protected String targetname;

	/**
	 *  Create a new cause.
	 */
	public Cause()
	{
		// bean constructor
	}
	
	/**
	 *  Create a new cause.
	 */
	public Cause(String sourcename, String targetname)
	{
		this(null, null, null, sourcename, targetname);
	}
	
	/**
	 *  Create a new cause.
	 */
	public Cause(String sourceid, String targetid, String sourcename, String targetname)
	{
		this(null, sourceid, targetid, sourcename, targetname);
	}
	
	/**
	 *  Create a new cause.
	 */
	public Cause(String chainid, String sourceid, String targetid, String sourcename, String targetname)
	{
		this.chainid = chainid==null? createUniqueId(): chainid;
		this.sourceid = sourceid==null? createUniqueId(): sourceid;
		this.targetid = targetid==null? createUniqueId(): targetid;
		
		// If chainid is null it will be set to sourceid
		// This allows to check if an event is top-level
//		this.chainid = chainid==null? this.sourceid: chainid;
		
		this.sourcename = sourcename;
		this.targetname = targetname;
	}
	
	/**
	 *  Create a new cause.
	 */
	public Cause(Cause old, String targetname)
	{
		this.chainid = old!=null? old.getChainId(): createUniqueId();
		this.sourceid = old!=null? old.getTargetId(): createUniqueId();
		this.sourcename = old!=null? old.getTargetName(): null;
		this.targetid = createUniqueId();
		this.targetname = targetname;
	}
	
	/**
	 *  Create a new cause as clone of the other.
	 */
	public Cause(Cause other)
	{
		this.chainid = other.getChainId();
		this.sourceid = other.getSourceId();
		this.sourcename = other.getSourceName();
		this.targetid = other.getTargetId();
		this.targetname = other.getTargetName();
	}
	
//	/**
//	 *  Create a new cause.
//	 */
//	public Cause(Cause old, String targedid, String targetname)
//	{
//		this.callid = old!=null? old.getCallId(): SUtil.createUniqueId("callid");
//		this.sourceid = old!=null? old.getTargetId(): null;
//		this.sourcename = old!=null? old.getTargetName(): null;
//		this.targetid = targedid;
//		this.targetname = targetname;
//	}

	/**
	 *  Create the next cause.
	 *  @param targetname The new target name.
	 */
	public Cause createNext(String targetname)
	{
		return new Cause(this, targetname);
	}
	
	/**
	 *  Create a unique id.
	 */
	public String createUniqueId()
	{
//		return createUniqueId(5);
		return createUniqueId(-1);
	}
	
	/**
	 *  Create a unique id.
	 */
	protected String createUniqueId(int len)
	{
		String ret = UUID.randomUUID().toString();
//		String ret = SUtil.createUniqueId("cause");
		if(len>0)
		{
			ret = ret.substring(0, len);
		}
		return ret;
	}
	
	/**
	 *  Get the chain id.
	 *  @return The chain id.
	 */
	public String getChainId()
	{
		return chainid;
	}

	/**
	 *  Set the chain id.
	 *  @param chainid The chainid to set.
	 */
	public void setChainId(String callid)
	{
		this.chainid = callid;
	}

	/**
	 *  Get the sourceId.
	 *  @return The sourceId.
	 */
	public String getSourceId()
	{
		return sourceid;
	}

	/**
	 *  Set the sourceId.
	 *  @param sourceid The sourceId to set.
	 */
	public void setSourceId(String sourceid)
	{
		this.sourceid = sourceid;
	}

	/**
	 *  Get the targetId.
	 *  @return The targetId.
	 */
	public String getTargetId()
	{
		return targetid;
	}

	/**
	 *  Set the targetId.
	 *  @param targetid The targetId to set.
	 */
	public void setTargetId(String targetid)
	{
		this.targetid = targetid;
	}

	/**
	 *  Get the sourceName.
	 *  @return The sourceName.
	 */
	public String getSourceName()
	{
		return sourcename;
	}

	/**
	 *  Set the sourceName.
	 *  @param sourcename The sourceName to set.
	 */
	public void setSourceName(String sourcename)
	{
		this.sourcename = sourcename;
	}

	/**
	 *  Get the targetName.
	 *  @return The targetName.
	 */
	public String getTargetName()
	{
		return targetname;
	}

	/**
	 *  Set the targetName.
	 *  @param targetname The targetName to set.
	 */
	public void setTargetName(String targetname)
	{
		this.targetname = targetname;
	}

	/**
	 *  Get the string representation.
	 */
	public String toString()
	{
		return "Cause(chainid=" + chainid + ", sourceid=" + sourceid
			+ ", targetid=" + targetid + ", sourcename=" + sourcename
			+ ", targetname=" + targetname + ")";
	}
}
