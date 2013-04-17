package jadex.android.standalone.clientapp;

import jadex.android.IEventReceiver;
import jadex.android.commons.Logger;
import jadex.android.exception.JadexAndroidPlatformNotStartedError;
import jadex.android.service.IJadexPlatformBinder;
import jadex.android.service.JadexPlatformManager;
import jadex.android.service.JadexPlatformService;
import jadex.bridge.IComponentIdentifier;
import jadex.bridge.IExternalAccess;
import jadex.bridge.fipa.SFipa;
import jadex.bridge.service.types.cms.CreationInfo;
import jadex.bridge.service.types.cms.IComponentManagementService;
import jadex.bridge.service.types.message.IMessageService;
import jadex.bridge.service.types.message.MessageType;
import jadex.commons.future.DefaultResultListener;
import jadex.commons.future.DelegationResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;

import java.util.HashMap;
import java.util.Map;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

/**
 * This is an Android Fragment Class which provides needed Functionality and
 * comfort Features for Fragments using the Jadex platform.
 * 
 * Subclasses of this class can be used together with the jadex platformapp.
 * 
 * @author Julian Kalinowski
 */
public class PlatformProvidingClientAppFragment extends ClientAppFragment implements ServiceConnection
{

	private Intent serviceIntent;
	private IJadexPlatformBinder platformService;
	protected IComponentIdentifier platformId;

	private boolean platformAutostart;
	private String[] platformKernels;
	private String platformOptions;
	private String platformName;
	
	/**
	 * Constructor
	 */
	public PlatformProvidingClientAppFragment()
	{
		super();
		platformAutostart = false;
		platformKernels = JadexPlatformManager.DEFAULT_KERNELS;
		platformOptions = "";
		platformName = JadexPlatformManager.getInstance().getRandomPlatformID();
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		serviceIntent = new Intent(getActivity(), JadexPlatformService.class);
		bindService(serviceIntent, this, Service.BIND_AUTO_CREATE);
	}
	
	@Override
	public void onDestroy()
	{
		super.onDestroy();
		unbindService(this);
	}

	/**
	 * Sets the autostart parameter for this jadex platform. If true, the
	 * platform will be started during onCreate.
	 * 
	 * @param autostart
	 */
	protected void setPlatformAutostart(boolean autostart)
	{
		if (!isJadexPlatformRunning())
		{
			this.platformAutostart = autostart;
		}
		else
		{
			throw new IllegalStateException("Cannot set autostart, platform already running!");
		}
	}

	/**
	 * Sets the Kernels. See {@link JadexPlatformManager} Constants for
	 * available Kernels.
	 * 
	 * @param kernels
	 */
	protected void setPlatformKernels(String... kernels)
	{
		this.platformKernels = kernels;
	}

	/**
	 * Sets platform options.
	 * 
	 * @param options
	 */
	protected void setPlatformOptions(String options)
	{
		this.platformOptions = options;
	}

	/**
	 * Sets the name of the platform that is started by this activity.
	 * 
	 * @param name
	 */
	protected void setPlatformName(String name)
	{
		this.platformName = name;
	}

	protected boolean isJadexPlatformRunning()
	{
		if (platformService != null)
		{
			return platformService.isPlatformRunning(platformId);
		}
		else
		{
			return false;
		}
	}

	protected IExternalAccess getPlatformAccess()
	{
		checkIfJadexIsRunning("getPlatformAccess()");
		return platformService.getExternalPlatformAccess(platformId);
	}

	protected boolean isJadexPlatformRunning(IComponentIdentifier platformId)
	{
		if (platformService != null)
		{
			return platformService.isPlatformRunning(platformId);
		}
		else
		{
			return false;
		}
	}

	/**
	 * Starts a Micro Agent.
	 * 
	 * @param name
	 *            Name of the Micro Agent created
	 * @param clazz
	 *            Class which defines the Micro Agent
	 * @return IFuture<IComponentIdentifier>
	 */
	protected IFuture<IComponentIdentifier> startMicroAgent(final String name, final Class<?> clazz)
	{
		checkIfJadexIsRunning("startMicroAgent()");
		final Future<IComponentIdentifier> ret = new Future<IComponentIdentifier>();
		platformService.getCMS(platformId).addResultListener(new DefaultResultListener<IComponentManagementService>()
		{

			public void resultAvailable(IComponentManagementService cms)
			{
				HashMap<String, Object> args = new HashMap<String, Object>();

				cms.createComponent(name, clazz.getName().replaceAll("\\.", "/") + ".class", new CreationInfo(args), null)
						.addResultListener(new DelegationResultListener<IComponentIdentifier>(ret));
			}
		});

		return ret;
	}

	/**
	 * Starts a Component.
	 * 
	 * @param name
	 *            Name of the Component created
	 * @param modelPath
	 *            Path to the Component XML definition file
	 * @return IFuture<IComponentIdentifier>
	 */
	protected IFuture<IComponentIdentifier> startBDIAgent(final String name, final String modelPath)
	{
		return startComponent(name, modelPath);
	}

	/**
	 * Starts a Component.
	 * 
	 * @param name
	 *            Name of the Component created
	 * @param modelPath
	 *            Path to the Component XML definition file
	 * @return IFuture<IComponentIdentifier>
	 */
	protected IFuture<IComponentIdentifier> startBPMNAgent(final String name, final String modelPath)
	{
		return startComponent(name, modelPath);
	}

	/**
	 * Starts a Component.
	 * 
	 * @param name
	 *            Name of the Component created
	 * @param modelPath
	 *            Path to the Component XML definition file
	 * @return IFuture<IComponentIdentifier>
	 */
	protected IFuture<IComponentIdentifier> startComponent(final String name, final String modelPath)
	{
		checkIfJadexIsRunning("startComponent()");
		final Future<IComponentIdentifier> ret = new Future<IComponentIdentifier>();
		platformService.getCMS(platformId).addResultListener(new DefaultResultListener<IComponentManagementService>()
		{

			public void resultAvailable(IComponentManagementService cms)
			{
				HashMap<String, Object> args = new HashMap<String, Object>();

				args.put("androidContext", PlatformProvidingClientAppFragment.this);
				cms.createComponent(name, modelPath, new CreationInfo(args), null).addResultListener(
						new DelegationResultListener<IComponentIdentifier>(ret));
			}
		});

		return ret;
	}

	protected void registerEventReceiver(String eventName, IEventReceiver<?> rec)
	{
		platformService.registerEventListener(eventName, rec);
	}

	protected void unregisterEventReceiver(String eventName, IEventReceiver<?> rec)
	{
		platformService.unregisterEventListener(eventName, rec);
	}

	/**
	 * Sends a FIPA Message to the specified receiver. The Sender is
	 * automatically set to the Platform.
	 * 
	 * @param message
	 * @param receiver
	 * @return Future<Void>
	 */
	protected Future<Void> sendMessage(final Map<String, Object> message, IComponentIdentifier receiver)
	{
		message.put(SFipa.FIPA_MESSAGE_TYPE.getReceiverIdentifier(), receiver);
		IComponentIdentifier cid = platformId;
		message.put(SFipa.FIPA_MESSAGE_TYPE.getSenderIdentifier(), cid);
		return sendMessage(message, SFipa.FIPA_MESSAGE_TYPE, receiver);
	}

	/**
	 * Sends a Message to a Component on the Jadex Platform.
	 * 
	 * @param message
	 * @param type
	 * @return Future<Void>
	 */
	protected Future<Void> sendMessage(final Map<String, Object> message, final MessageType type, final IComponentIdentifier receiver)
	{
		checkIfJadexIsRunning("sendMessage");

		final Future<Void> ret = new Future<Void>();

		getMS().addResultListener(new DefaultResultListener<IMessageService>()
		{
			public void resultAvailable(IMessageService ms)
			{
				ms.sendMessage(message, type, platformId, null, receiver, null).addResultListener(new DelegationResultListener<Void>(ret));
			}
		});

		return ret;
	}

	private void checkIfJadexIsRunning(String caller)
	{
		if (!platformService.isPlatformRunning(platformId))
		{
			throw new JadexAndroidPlatformNotStartedError(caller);
		}
	}

	protected IFuture<IMessageService> getMS()
	{
		return platformService.getMS(platformId);
	}

	protected IFuture<IComponentManagementService> getCMS()
	{
		return platformService.getCMS(platformId);
	}

	public void onServiceConnected(ComponentName name, IBinder service)
	{
		platformService = (IJadexPlatformBinder) service;
		if (platformAutostart)
		{
			startPlatform();
		}
	}

	public void onServiceDisconnected(ComponentName name)
	{
		platformService = null;
	}

	/**
	 * Terminates a given Jadex platform.
	 * 
	 * @param platformID
	 *            Identifier of the platform to terminate
	 */
	public void shutdownJadexPlatform(IComponentIdentifier platformID)
	{
		platformService.shutdownJadexPlatform(platformID);
	}

	/**
	 * Called right before the platform startup.
	 */
	protected void onPlatformStarting()
	{
		setProgressBarIndeterminateVisibility(true);
	}

	/**
	 * Called right after the platform is started.
	 * 
	 * @param result
	 *            The external access to the platform
	 */
	protected void onPlatformStarted(IExternalAccess result)
	{
		this.platformId = result.getComponentIdentifier();
		runOnUiThread(new Runnable()
		{

			@Override
			public void run()
			{
				setProgressBarIndeterminateVisibility(false);
			}
		});
	}

	/**
	 * Starts the Jadex Platform. To set Parameters, use setPlatformKernels(),
	 * setPlatformOptions() or setPlatformName() before calling this Method.
	 * 
	 * Will be automatically called when setPlatformAutostart(true) was called
	 * in the Constructor.
	 * 
	 * The Lifecycle methods onPlatformStarting() and onPlatformStarted() will
	 * be executed during Startup.
	 */
	final protected void startPlatform()
	{
		onPlatformStarting();
		IFuture<IExternalAccess> platform = platformService.startJadexPlatform(platformKernels, platformName, platformOptions);

		platform.addResultListener(new DefaultResultListener<IExternalAccess>()
		{

			@Override
			public void resultAvailable(IExternalAccess result)
			{
				onPlatformStarted(result);
			}
			
			@Override
			public void exceptionOccurred(Exception exception)
			{
				Logger.e(exception);
			}

		});
		
	}

	/**
	 * Stops all running jadex platforms.
	 */
	protected void stopPlatforms()
	{
		platformService.shutdownJadexPlatforms();
	}

}
