package jadex.platform.service.remote;

import jadex.bridge.IInternalAccess;
import jadex.bridge.nonfunctional.NFPropertyMetaInfo;
import jadex.bridge.nonfunctional.NFRootProperty;
import jadex.bridge.sensor.unit.TimeUnit;
import jadex.commons.future.DefaultResultListener;
import jadex.commons.future.IResultListener;

/**
 *  The latency of a remote platform.
 *  Injects the latency property to the platform.
 */
public class ProxyLatencyProperty extends NFRootProperty<Long, TimeUnit>
{
	/** The last measured value. */
	protected Long lastval;
	
	/**
	 *  Create a new property.
	 */
	public ProxyLatencyProperty(final IInternalAccess comp)
	{
		super(comp, new NFPropertyMetaInfo("latency "+((ProxyAgent)comp).rcid.getName(), long.class, null, true, 10000, Target.Root), false);
	}
	
	/**
	 *  Measure the value.
	 */
	public Long measureValue()
	{
		ProxyAgent pa = (ProxyAgent)getComponent();
		pa.getCurrentLatency().addResultListener(new IResultListener<Long>()
		{
			public void resultAvailable(Long result)
			{
//				if(result!=null)
//					System.out.println("lat for "+((ProxyAgent)comp).rcid.getName()+" "+result);
				lastval = result;
				
				if(result!=null && !isInjected())
				{
					injectPropertyToRootComponent().addResultListener(new DefaultResultListener<Void>()
					{
						public void resultAvailable(Void result)
						{
						}
					}); // todo: what to do on failure?
				}
			}
			
			public void exceptionOccurred(Exception exception)
			{
				exception.printStackTrace();
			}
		});
//		if(lastval!=null)
//			System.out.println("measured for "+((ProxyAgent)comp).rcid.getName()+" "+lastval);
		return lastval;
	}
}