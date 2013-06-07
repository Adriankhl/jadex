package jadex.android.standalone.clientservice;

import jadex.android.exception.JadexAndroidError;
import jadex.android.service.JadexPlatformManager;
import jadex.android.service.JadexPlatformService;
import jadex.android.standalone.clientapp.JadexClientAppService;
import jadex.commons.SReflect;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;

public class UniversalClientService extends Service
{
	/**
	 * Maps ClassNames to Service instances.
	 */
	private Map<String, Service> serviceInstances;

	private Map<ServiceConnection, Service> serviceConnections;
	
	private Map<ServiceConnection, Intent> clientIntents;

	private Map<ServiceConnection, ComponentName> componentNames;
	
	private Map<Service, Boolean> startedServices;

	@Override
	public void onCreate()
	{
		super.onCreate();
		serviceInstances = new HashMap<String, Service>();
		serviceConnections = new HashMap<ServiceConnection, Service>();
		clientIntents = new HashMap<ServiceConnection, Intent>();
		componentNames = new HashMap<ServiceConnection, ComponentName>();
		startedServices = new HashMap<Service, Boolean>();
	}

	@Override
	public IBinder onBind(Intent intent)
	{
		return new UniversalClientServiceBinder();
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		// TODO: destroy all client services
	}

	private Service createClientService(String className)
	{
		Service result;
		ClassLoader cl = JadexPlatformManager.getInstance().getClassLoader(null);
		@SuppressWarnings("unchecked")
		Class<Service> clientServiceClass = SReflect.classForName0(className, cl);
		try
		{
			result = clientServiceClass.newInstance();
			Context baseContext = getBaseContext();
			if (result instanceof JadexClientAppService) {
				((JadexClientAppService) result).attachBaseContext(baseContext);
			} else if (result instanceof JadexPlatformService) {
				((JadexPlatformService) result).attachBaseContext(baseContext);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
		return result;
	}

	public class UniversalClientServiceBinder extends Binder
	{
		public UniversalClientServiceBinder()
		{
		}

		public void bindClientService(Intent intent, ServiceConnection conn, int flags)
		{
			// TODO: handle flags
			ComponentName clientServiceComponent = intent.getComponent();
			String clientServiceClassName = clientServiceComponent.getClassName();

			Service clientService = serviceInstances.get(clientServiceClassName);
			// do we already have an instance?
			if (clientService == null)
			{
				clientService = createClientService(clientServiceClassName);
				clientService.onCreate();
				serviceInstances.put(clientServiceClassName, clientService);
			}

			// has the service already been bound with the given connection?
			if (!serviceConnections.containsKey(conn))
			{
				Intent clientIntent = intent;
				IBinder clientBinder = clientService.onBind(clientIntent);

				clientIntents.put(conn, clientIntent);
				serviceConnections.put(conn, clientService);
				componentNames.put(conn, clientServiceComponent);

				conn.onServiceConnected(clientServiceComponent, clientBinder);
			}
			else
			{
				throw new JadexAndroidError("Service already bound!");
			}
		}

		public boolean unbindClientService(ServiceConnection conn)
		{
			boolean result = false;
			Service service = serviceConnections.get(conn);
			if (service != null)
			{
				Intent clientIntent = clientIntents.get(conn);
				boolean isUnbound = service.onUnbind(clientIntent);
				if (isUnbound) {
					conn.onServiceDisconnected(componentNames.get(conn));
					serviceConnections.remove(conn);
					componentNames.remove(conn);
					clientIntents.remove(conn);
					
					checkDestroyService(service);
				}
				result = isUnbound;
			}
			else
			{
			}
			return result;
		}

		public boolean isClientServiceConnection(ServiceConnection conn)
		{
			return serviceConnections.containsKey(conn);
		}
		
		public void startClientService(Intent service) {
			String clientServiceClassName = service.getComponent().getClassName();

			Service clientService = serviceInstances.get(clientServiceClassName);
			// do we already have an instance?
			if (clientService == null)
			{
				clientService = createClientService(clientServiceClassName);
				clientService.onCreate();
				serviceInstances.put(clientServiceClassName, clientService);
			}
			
			clientService.onStart(service, 0);
			startedServices.put(clientService, Boolean.TRUE);
		}
		
		public boolean stopClientService(Intent intent) {
			boolean result = false;
			Boolean b = startedServices.remove(serviceInstances.get(intent.getComponent().getClassName()));
			if (b != null && b) {
				String clientServiceClassName = intent.getComponent().getClassName();
				Service clientService = serviceInstances.get(clientServiceClassName);
				
				checkDestroyService(clientService);
				
				result = true;
			}
			return result;
		}

		private void checkDestroyService(Service clientService)
		{
			Boolean started = startedServices.get(clientService);
			if (started == null || !started) { 
				// check if clientService is bound anywhere, else destroy.
				boolean isBound = false;
				Set<Entry<ServiceConnection,Service>> entrySet = serviceConnections.entrySet();
				for (Entry<ServiceConnection, Service> entry : entrySet)
				{
					if (entry.getValue() == clientService) {
						isBound = true;
						break;
					}
				}
				if (!isBound) {
					clientService.onDestroy();
					serviceInstances.remove(clientService);
				}
			}
		}

		public boolean isClientServiceStarted(Intent intent)
		{
			boolean result = false;
			Service instance = serviceInstances.get(intent.getComponent().getClassName());
			if (instance != null) {
				Boolean started = startedServices.get(instance);
				if (started != null && started) {
					result = true;
				}
			}

			return result;
		}
	}

}
