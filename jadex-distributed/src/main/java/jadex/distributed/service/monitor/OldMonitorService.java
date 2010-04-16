package jadex.distributed.service.monitor;

import jadex.commons.concurrent.IResultListener;
import jadex.distributed.service.discovery.IDiscoveryService;
import jadex.distributed.service.discovery.IDiscoveryServiceListener;
import jadex.service.IService;
import jadex.service.IServiceContainer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import com.sun.jdmk.discovery.DiscoveryClient;
import com.sun.jdmk.discovery.DiscoveryResponse;

public class OldMonitorService implements IMonitorService, IDiscoveryServiceListener, IService {

	/* braucht einen eigenen thread der zyklisch die workload werte abgreift */
	
	/*
	 Die Klasse kümmert sich darum dem Server alle notwendigen Informationen
	 zur Verfügung zu stellen. Der OldMonitorService kümmert sich also um die
	 Managementinformationen. Dementsprechend wird der CMS, oder eine andere
	 Klasse die der CMS per Komposition(=Instanvariable) kennt (vorzugsweise
	 durch dependency injection), die Managementmethode(n) zur Verfügung stellen.
	 
	 Es kommt darauf an was JMX schon alle leistet. Darauf basieren dann die
	 Aufgaben, die der OldMonitorService zu leisten hat. Diese sind
	  - 
	 */
	
	private DiscoveryClient discoveryClient;
	
	private Set<IMonitorServiceListener> listeners; /** get informed when new management information is available **/
	//private Map<InetSocketAddress, MBeanServerConnection> remoteMBeanServers;
	private Map<InetSocketAddress, JMXConnector> remoteMBeanServers;
	
	private IDiscoveryService discoveryService;
	
	// TODO maybe change to JDMK discovery **/
	
	private IServiceContainer container; /* needed to get IDiscoveryService */
	
	public OldMonitorService(IServiceContainer container) {
		this.container = container;
		this.listeners = new HashSet<IMonitorServiceListener>();
		this.remoteMBeanServers = new HashMap<InetSocketAddress, JMXConnector>();
		
		/* 
		   am DummyDiscoveryService anmelden um initialie Liste von Client-Plattformen zu unterhalten
		   und um laufen über neue ClientPlattformen informiert zu werden
		   
		   TODO das Java Dynamic Management Kit scheint auch eine Form von Discovery zu unterstützen
		        diese könnte der DummyDiscoveryService nutzen, anstatt eine eigene Discovery-Lösung zu implementieren
		        oder gleich im OldMonitorService das Discovery nutzen und so den DummyDiscoveryService nutzen? eher nicht,
		        eine Aufteilung in verschiedene Services ist sinnvoller, da so verschiedene Discovery-Methoden
		        genutzt werden können, um die Plattformen zu finden.
		 */
		IDiscoveryService discoveryService = (IDiscoveryService)this.container.getService(IDiscoveryService.class);
		discoveryService.register(this); /* behavior of discovery service: initiale push with all currently known platforms */
		this.discoveryService = discoveryService;
		/* NOW the monitor service has an initial list of known platforms */
		
		/* establich JMX connection to the list of initial client platforms */
		/*
		for( InetSocketAddress machine : machines ) {
			//JMXConnectorFactory.connect( new JMXServiceURL("service:jmx:rmi:///jndi/rmi://134.100.11.94:4711/jmxrmi") );
			StringBuilder sb = new StringBuilder().append("service:jmx:rmi:///jndi/rmi://").append(machine.getHostName()).append(machine.getPort()).append("/jmxrmi");
			try {
				JMXConnector connector = JMXConnectorFactory.connect( new JMXServiceURL(sb.toString()) );
				MBeanServerConnection mbeanServer = connector.getMBeanServerConnection();
				//connectors.put(machine, connector);
				remoteMBeanServers.put(machine, mbeanServer);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
		*/
	}
	
	
	/** register und unregister, damit sich listener (un-)registieren können; um IMonitorSerivce zu implementieren; **/
	@Override
	public void register(IMonitorServiceListener listener) {
		/*
		 * Prevent that some weirdo supplies the method with a null parameter.
		 * The check for null is also necessary, because a HashMap permits null for key and value.
		 * Of course in Objective-C this wouldn't be a problem, because there it is possible to call
		 * anything on nil; the nil in Obj-C is the same as null in the Java world.
		 * But because we are in the Java world, we need to prevent null values to avoid a call to null.
		 */
		if(listener != null) { // 
			synchronized (this.listeners) { // yes, it could be that many threads concurrently want to register objects
				this.listeners.add(listener);
			}
		}
	}

	@Override
	public void unregister(IMonitorServiceListener listener) {
		synchronized (this.listeners) {
			this.listeners.remove(listener);
		}
	}
	
	
	/** Methoden um dem IService interface zu genügen **/
	@Override
	public void startService() {
		// actively look for JMX agents with com.sun.jdmk.discovery.DiscoveryClient; those JMX agents need a DiscoveryResponder to be discoverable 
		discoveryClient = new DiscoveryClient();
		try {
			discoveryClient.setTimeToLive(16);
		} catch (IllegalArgumentException e1) { // should never occur, becaus 0 < 16 < 255
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		try {
			discoveryClient.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.out.println("HI");
		//Vector<DiscoveryResponse> v = discoveryClient.findMBeanServers();
		//Vector v = discoveryClient.findMBeanServers();
		Vector<DiscoveryResponse> v = discoveryClient.findMBeanServers();
		System.out.println( v.size() );
		for (DiscoveryResponse response : v) {
		//for(int i=0; i<v.size(); i++) {
			System.out.println( response.getHost() );
			System.out.println( response );
			//System.out.println( v.get(i).toString() );
		}
		System.out.println("HO");
		
		
	}
	
	@Override
	public void shutdownService(IResultListener listener) {
		// DiscoveryClient stoppen, um Multicast socket zu lösen
		discoveryClient.stop();
	}


	/** IDiscoveryServiceListener genügen 
	 *   Called by IDiscoverService to inform about new machines. **/
	// von IDiscoverService aufgerufen wenn neue Maschinen bekannt bzw. bekannte Machinen nicht mehr verfügbar
	// sehr häufig sollte das nicht passieren; eher am anfang beim boot und evtl. ein paar mal zwischendurch wenn neue platformen erscheinen
	@Override
	public void notifyIDiscoveryListener() {
		// what is the difference between these two sets?
		Set<InetAddress> currentMachines = this.discoveryService.getMachineAddresses();
		Set<InetSocketAddress> knownMachines = this.remoteMBeanServers.keySet();
		
		Set<InetSocketAddress> newMachines = new HashSet<InetSocketAddress>(); // currentMachines may increased
		Set<InetAddress> removedMachines = new HashSet<InetAddress>(); // currentMachines may shrinked
		
		for (InetAddress machine : currentMachines) { // find shrinked/deleted machines
			if( !knownMachines.contains(machine) ) {
				removedMachines.add(machine);
			}
		}
		
		for (InetSocketAddress machine : knownMachines) { // find increased/new machines
			if( !currentMachines.contains(machine) ) {
				newMachines.add(machine);
			}
		}
		
		// Verbindung zu neuen Maschinen mit JMX aufbauen bzw. dieses versuchen
		for (InetSocketAddress machine : newMachines) {
			//JMXServiceURL jmxUrl = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://134.100.11.94:4711/jmxrmi");
			StringBuilder sb = new StringBuilder();
			sb.append("service:jmx:rmi:///jndi/rmi://").append(machine.getHostName()).append(":").append(machine.getPort()).append("/jmxrmi");
			
			JMXServiceURL jmxUrl = null;
			try {
				jmxUrl = new JMXServiceURL(sb.toString());
			} catch (MalformedURLException e) { // solle nie passieren
				e.printStackTrace();
			}
			
			try {
				JMXConnector connector = JMXConnectorFactory.connect(jmxUrl); // connection established to remote JMX agent
				// now the remote MBeanServer is availble through connector.getMBeanServerConnection();
				this.remoteMBeanServers.put(machine, connector);
			} catch (IOException e) { // konnte nicht zu remote JMX agent verbinden
				System.err.println("Verbindung zu remote JMX agent fehlgeschlagen; vielleicht ist die client platform falsch konfiguriert?");
				e.printStackTrace();
			}
			
		}
		
		// Verbindung zu nicht mehr verfügbaren Maschinen beenden
		for (InetAddress machine : removedMachines) { // close any pending connections to removed/dismissed/vanished machines
			final JMXConnector connector = this.remoteMBeanServers.get(machine);
			try {
				connector.close(); 
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			Thread t = new Thread() {
				@Override
				public void run() {
					try {
						connector.close(); // potentially slow operation if server crashed, because network protocol must first timeout;
						// this is why a seperate thread is used to avoid such blocking behavior
						// TODO if a machine vanished, then why close the connection? just throw it away from the Map and finish
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}; // Thread to close JMX connection
			t.run();
		}
	}


	@Override
	public void notifyIDiscoveryListenerAdd(InetAddress addr) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void notifyIDiscoveryListenerRemove(InetAddress addr) {
		// TODO Auto-generated method stub
		
	}

}
