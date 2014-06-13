package jadex.bdiv3.model;

import jadex.bridge.modelinfo.IModelInfo;
import jadex.commons.FieldInfo;
import jadex.commons.Tuple2;
import jadex.micro.MicroModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 */
public class BDIModel extends MicroModel
{
	/** The subcapabilities. */
	protected List<Tuple2<FieldInfo, BDIModel>> subcapabilities;

	/** The belief mappings. */
	protected Map<String, String> beliefmappings;


	/** The capability. */
	protected MCapability mcapa;

	/**
	 *  Create a new model.
	 */
	public BDIModel(IModelInfo modelinfo, MCapability mcapa)
	{
		super(modelinfo);
		this.mcapa = mcapa;
	}

	/**
	 *  Get a capability by name.
	 *  @return The mcapa.
	 */
	public MCapability getCapability(String name)
	{
		MCapability ret = null;
		int idx = name.indexOf(MElement.CAPABILITY_SEPARATOR);
		if(idx!=-1)
		{
			String capaname = name.substring(0, idx-1);
			String rest = name.substring(idx+1);
			if(subcapabilities!=null)
			{
				BDIModel subcap = null;
				for(Tuple2<FieldInfo, BDIModel> tup: subcapabilities)
				{
					subcap = tup.getSecondEntity();
					if(subcap.getCapability().getName().equals(capaname))
					{
						break;
					}
				}
				if(subcap==null)
				{
					throw new RuntimeException("Capability not found: "+capaname);
				}
				ret = subcap.getCapability(rest);
			}
		}
		else
		{
			ret = mcapa;
		}
		return ret;
	}
	
	/**
	 *  Get the mcapa.
	 *  @return The mcapa.
	 */
	public MCapability getCapability()
	{
		return mcapa;
	}

	/**
	 *  Set the mcapa.
	 *  @param mcapa The mcapa to set.
	 */
	public void setCapability(MCapability mcapa)
	{
		this.mcapa = mcapa;
	}
	
	/**
	 *  Add a subcapability field.
	 *  @param field The field. 
	 */
	public void addSubcapability(FieldInfo field, BDIModel model)
	{
		if(subcapabilities==null)
		{
			subcapabilities = new ArrayList<Tuple2<FieldInfo, BDIModel>>();
		}
		subcapabilities.add(new Tuple2<FieldInfo, BDIModel>(field, model));
	}
	
	/**
	 *  Get the agent injection fields.
	 *  @return The fields.
	 */
	public Tuple2<FieldInfo, BDIModel>[] getSubcapabilities()
	{
		return subcapabilities==null? new Tuple2[0]: (Tuple2<FieldInfo, BDIModel>[])subcapabilities.toArray(new Tuple2[subcapabilities.size()]);
	}
	
	/**
	 *  Add a belief mapping.
	 *  @param target The target belief in the subcapability. 
	 *  @param source The source belief.
	 */
	public void addBeliefMapping(String target, String source)
	{
		if(beliefmappings==null)
		{
			beliefmappings = new LinkedHashMap<String, String>();
		}
		beliefmappings.put(target, source);
	}
	
	/**
	 *  Get the belief mappings (target->source).
	 */
	public Map<String, String> getBeliefMappings()
	{
		Map<String, String>	ret;
		if(beliefmappings==null)
		{
			ret	= Collections.emptyMap();
		}
		else
		{
			ret	= beliefmappings;
		}
		return ret;
	}
}
