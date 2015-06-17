package jadex.bdi.examples.shop;

import jadex.bdiv3.features.IBDIAgentFeature;
import jadex.bdiv3.features.impl.IInternalBDIAgentFeature;
import jadex.bdiv3.runtime.IGoal;
import jadex.bdiv3.runtime.impl.RCapability;
import jadex.bridge.IInternalAccess;
import jadex.bridge.service.annotation.Service;
import jadex.bridge.service.annotation.ServiceComponent;
import jadex.commons.future.ExceptionDelegationResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;

/**
 *  The shop for buying goods at the shop.
 */
@Service
public class ShopService implements IShopService 
{
	//-------- attributes --------
	
	/** The component. */
	@ServiceComponent
	protected IInternalAccess comp;
	
	/** The shop name. */
	protected String name;
	
	//-------- constructors --------
	
	/**
	 *  Create a new shop service.
	 */
	public ShopService()
	{
		this.name = "noname-";
	}
	
	/**
	 *  Create a new shop service.
	 */
	public ShopService(String name)
	{
		this.name = name;
	}

	//-------- methods --------
	
	/**
	 *  Get the shop name. 
	 *  @return The name.
	 *  
	 *  @directcall (Is called on caller thread).
	 */
	public String getName()
	{
		return name;
	}
	
	/**
	 *  Buy an item.
	 *  @param item The item.
	 */
	public IFuture<ItemInfo> buyItem(final String item, final double price)
	{
		final Future<ItemInfo> ret = new Future<ItemInfo>();
		
		// Hack, as long as we do not have a specific XML feature interface
		IInternalBDIAgentFeature bdif = (IInternalBDIAgentFeature)comp.getComponentFeature(IBDIAgentFeature.class);
		RCapability capa = bdif.getCapability();
		
		final IGoal sell = capa.getGoalbase().createGoal("sell");
		sell.getParameter("name").setValue(item);
		sell.getParameter("price").setValue(Double.valueOf(price));
//		sell.addGoalListener(new IGoalListener()
//		{
//			public void goalFinished(AgentEvent ae)
//			{
//				if(sell.isSucceeded())
//					ret.setResult((ItemInfo)sell.getParameter("result").getValue());
//				else
//					ret.setException(sell.getException());
//			}
//			
//			public void goalAdded(AgentEvent ae)
//			{
//			}
//		});
		IFuture<Void> fut = capa.getGoalbase().dispatchTopLevelGoal(sell);
		fut.addResultListener(new ExceptionDelegationResultListener<Void, ItemInfo>(ret)
		{
			public void customResultAvailable(Void result)
			{
				if(sell.isSucceeded())
					ret.setResult((ItemInfo)sell.getParameter("result").getValue());
				else
					ret.setException(sell.getException());
			}
		});
		
		return ret;
	}
	
	/**
	 *  Get the item catalog.
	 *  @return  The catalog.
	 */	
	public IFuture<ItemInfo[]> getCatalog()
	{
		// Hack, as long as we do not have a specific XML feature interface
		IInternalBDIAgentFeature bdif = (IInternalBDIAgentFeature)comp.getComponentFeature(IBDIAgentFeature.class);
		RCapability capa = bdif.getCapability();
		
		final Future<ItemInfo[]> ret = new Future<ItemInfo[]>();
		ret.setResult((ItemInfo[])capa.getBeliefbase().getBeliefSet("catalog").getFacts());
		return ret;
	}

	/**
	 *  Get the string representation.
	 *  @return The string representation.
	 */
	public String toString()
	{
		return name;
	}
}
