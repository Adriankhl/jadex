package jadex.android.exampleproject.extended;

import jadex.android.EventReceiver;
import jadex.android.commons.JadexPlatformOptions;
import jadex.android.exampleproject.MyEvent;
import jadex.android.exampleproject.extended.agent.IAgentInterface;
import jadex.android.exampleproject.extended.agent.MyAgent;
import jadex.android.service.JadexPlatformService;
import jadex.bridge.IComponentIdentifier;
import jadex.bridge.IExternalAccess;
import jadex.bridge.service.types.cms.IComponentManagementService;
import jadex.commons.future.DefaultResultListener;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Toast;

public class MyJadexService extends JadexPlatformService
{
	// BEGIN: nested classes
	public interface MyPlatformListener
	{
		void onPlatformStarting();
		void onPlatformStarted();
		void onHelloWorldAgentStarted(String name);
	}

	public class MyServiceInterface extends Binder
	{

		public boolean isJadexPlatformRunning()
		{
			return MyJadexService.this.isPlatformRunning();
		}

		public void setPlatformListener(MyPlatformListener l)
		{
			listener = l;
		}

		public String getPlatformId()
		{
			return MyJadexService.this.getPlatformId().toString();
		}

		public void startPlatform()
		{
			MyJadexService.this.startPlatform();
		}

		public void startHelloWorldAgent()
		{
			MyJadexService.this.startHelloWorldAgent();
		}

		public void stopPlatforms()
		{
			MyJadexService.this.stopPlatforms();
		}

	}

	// END: nested classes

	/** agent counter */
	private int num;

	/** listener for the activity **/
	public MyPlatformListener listener;

	private Handler handler;

	public MyJadexService()
	{
		setPlatformAutostart(false);
		setPlatformKernels(JadexPlatformOptions.KERNEL_MICRO);
		setPlatformName("JadexAndroidExample");
	}
	
	@Override
	public void onCreate()
	{
		super.onCreate();
		this.handler = new Handler(); // needed to make Toasts on UI Thread.
		
		registerEventReceiver(new EventReceiver<MyEvent>(MyEvent.class)
		{

			@Override
			public void receiveEvent(final MyEvent event)
			{
				handler.post(new Runnable()
				{
					
					@Override
					public void run()
					{
						Toast.makeText(MyJadexService.this, event.getMessage(), Toast.LENGTH_LONG).show();
					}
				});
			}
		});
	}
	

	@Override
	public IBinder onBind(Intent intent)
	{
		return new MyServiceInterface();
	}

	@Override
	protected void onPlatformStarting()
	{
		super.onPlatformStarting();
		if (listener != null)
		{
			listener.onPlatformStarting();
		}
	}

	@Override
	protected void onPlatformStarted(IExternalAccess platform)
	{
		super.onPlatformStarted(platform);
		if (listener != null)
		{
			listener.onPlatformStarted();
		}
	}

	public void startHelloWorldAgent()
	{
		num++;
		
		startComponent("HelloWorldAgent " + num, MyAgent.class).addResultListener(new DefaultResultListener<IComponentIdentifier>()
		{

			@Override
			public void resultAvailable(IComponentIdentifier result)
			{
				if (listener != null) {
					listener.onHelloWorldAgentStarted(result.toString());
				}
				
				System.out.println("calling Agent");
				
				IComponentManagementService getsService = getsService(IComponentManagementService.class);
				IAgentInterface agent = getsService(IAgentInterface.class);
				agent.callAgent("Hello Agent!");
				
			}
		});
	}
}
