package jadex.android.applications.demos.bdi;

import jadex.bdi.runtime.Plan;
import android.widget.Toast;

/**
 *  Say Hello
 */
public class SayHelloPlan extends Plan
{
	/**
	 * The body method is called on the
	 * instatiated plan instance from the scheduler.
	 */
	public void body()
	{
		final String	message = (String)getBeliefbase().getBelief("HelloMessage").getFact();
		
		BDIDemoActivity.INSTANCE.runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				Toast.makeText(BDIDemoActivity.INSTANCE, message, Toast.LENGTH_LONG).show();				
			}
		});
		
//		final Activity	act = (Activity)getBeliefbase().getBelief("androidContext").getFact();
//		
//		act.runOnUiThread(new Runnable()
//		{
//			
//			@Override
//			public void run()
//			{
//				Toast.makeText(act, message, Toast.LENGTH_LONG).show();
//			}
//		});
		
	}
	
}

