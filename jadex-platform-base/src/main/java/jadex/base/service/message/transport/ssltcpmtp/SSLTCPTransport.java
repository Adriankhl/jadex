package jadex.base.service.message.transport.ssltcpmtp;

import jadex.base.service.message.transport.tcpmtp.TCPTransport;
import jadex.bridge.service.IServiceProvider;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.bridge.service.search.SServiceProvider;
import jadex.bridge.service.types.security.ISecurityService;
import jadex.commons.SSecurity;
import jadex.commons.future.DelegationResultListener;
import jadex.commons.future.ExceptionDelegationResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;

import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 *  The ssl based transport.
 *  
 *  (It requires a keystore being present in the start directory.
 *  This keystore can be generated manually with:
 *  
 *  keytool -genkey -keystore keystore -keyalg RSA)
 *  Added support for automatic generation.
 *  
 *  Currently the transport does not check the certificates as this
 *  would require to install all trusted platforms (servers) in all
 *  platforms.
 */
public class SSLTCPTransport extends TCPTransport
{
	/** The schema name. */
	public final static String[] SCHEMAS = new String[]{"ssltcp-mtp://"};

	/** The ssl context. */
	protected SSLContext context;
	
	/** The keystore path. */
	protected String storepath;
	
	/** The keystore password. */
	protected String storepass;
	
	/** The key password. */
	protected String keypass;
	
	//-------- constructors --------

	/**
	 *  Init the transport.
	 *  @param platform The platform.
	 *  @param settings The settings.
	 */
	public SSLTCPTransport(final IServiceProvider container, int port)
	{
		this(container, port, true);
	}

	/**
	 *  Init the transport.
	 *  @param platform The platform.
	 *  @param settings The settings.
	 */
	public SSLTCPTransport(final IServiceProvider container, int port, final boolean async)
	{
		super(container, port, async);
	}
	
	/**
	 *  Start the transport.
	 */
	public IFuture<Void> start()
	{
		final Future<Void> ret = new Future<Void>();
	
		SServiceProvider.getService(container, ISecurityService.class, RequiredServiceInfo.SCOPE_PLATFORM)
			.addResultListener(new ExceptionDelegationResultListener<ISecurityService, Void>(ret)
		{
			public void customResultAvailable(ISecurityService ss)
			{
				ss.getKeystoreInfo().addResultListener(new ExceptionDelegationResultListener<String[], Void>(ret)
				{
					public void customResultAvailable(String[] info)
					{
						setKeystoreInfo(info[0], info[1], info[2]);
						
						SSLTCPTransport.super.start().addResultListener(new DelegationResultListener<Void>(ret));
					}
				});
			}
		});
		
		return ret;
	}
	
	/**
	 *  Get the ssl context.
	 */
	public SSLContext getSSLContext()
	{
		if(context==null)
		{
			try
			{
				context = SSLContext.getInstance("TLS");
				KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
				
				// possibly create a new keystore on disk
//				System.out.println("Using keystore: "+storepath+" "+storepass+" "+keypass);
				KeyStore ks = SSecurity.getKeystore(storepath, storepass, keypass, "jadex");
				
				kmf.init(ks, keypass.toCharArray());
				
				// This allows ssl handshake without signed certificates.
				TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
				tmf.init(ks);				
				NaiveTrustManager tm = new NaiveTrustManager();
				context.init(kmf.getKeyManagers(), new TrustManager[]{tm}, new SecureRandom());

//				context.init(kmf.getKeyManagers(), null, null);
			}
			catch(Exception e)
			{
				throw new RuntimeException(e);
			}
		}
		return context;
	}
	
	//-------- methods --------

	/**
	 *  Create a server socket.
	 *  @return The server socket.
	 */
	public ServerSocket createServerSocket() throws Exception
	{
		SSLServerSocketFactory fac = getSSLContext().getServerSocketFactory();
        SSLServerSocket ret = (SSLServerSocket)fac.createServerSocket(port);
        return ret;
	}
	
	/**
	 *  Create a client socket.
	 *  @return The client socket.
	 */
	public Socket createClientSocket(String host, int port) throws Exception
	{
		SSLSocketFactory fac = getSSLContext().getSocketFactory();
        SSLSocket ret = (SSLSocket)fac.createSocket(host, port);
        return ret;
	}
	
	/**
	 *  Returns the prefix of this transport
	 *  @return Transport prefix.
	 */
	public String[] getServiceSchemas()
	{
		return SCHEMAS;
	}
	
	/**
	 *  Test if a transport satisfies the non-functional requirements.
	 *  @return True, if the transport satisfies the non-functional requirements.
	 */
	public boolean isNonFunctionalSatisfied(Map<String, Object> nonfunc)
	{
		// todo: how to check if https connection?
//		Boolean sec = nonfunc!=null? (Boolean)nonfunc.get(RemoteServiceManagementService.SECURE_TRANSMISSION): null;
//		return sec==null || !sec.booleanValue();
		return true;
	}
	
	/**
	 *  Set the keystore info.
	 */
	public void setKeystoreInfo(String storepath, String storepass, String keypass)
	{
		this.storepath = storepath;
		this.storepass = storepass;
		this.keypass = keypass;
		
		// reset context
		this.context = null;
	}
	
	/**
	 *  Trust manager that does not check certificates. 
	 */
	protected static class NaiveTrustManager implements X509TrustManager
	{
		public X509Certificate[] getAcceptedIssuers()
		{
			return null;
		}

		public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException
		{
		}

		public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException
		{
		}
	}
	
}
