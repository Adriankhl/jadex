package jadex.extension.rs.publish;

import jadex.bridge.IComponentIdentifier;
import jadex.bridge.ServiceCall;
import jadex.bridge.modelinfo.UnparsedExpression;
import jadex.bridge.service.IServiceIdentifier;
import jadex.bridge.service.IServiceProvider;
import jadex.bridge.service.PublishInfo;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.bridge.service.annotation.Service;
import jadex.bridge.service.search.SServiceProvider;
import jadex.bridge.service.types.cms.IComponentDescription;
import jadex.bridge.service.types.cms.IComponentManagementService;
import jadex.bridge.service.types.library.ILibraryService;
import jadex.commons.Tuple2;
import jadex.commons.future.ExceptionDelegationResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.javaparser.SJavaParser;

import java.io.Writer;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.glassfish.grizzly.http.server.CLStaticHttpHandler;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.server.ServerConfiguration;
import org.glassfish.grizzly.http.server.StaticHttpHandler;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpContainer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ContainerFactory;
import org.glassfish.jersey.server.ResourceConfig;

/**
 *  The default web service publish service.
 *  Publishes web services using the Grizzly web server.
 */
@Service
public class GrizzlyRestServicePublishService extends AbstractRestServicePublishService
{
	//-------- constants --------
	
	/** The servers per service id. */
	protected Map<IServiceIdentifier, Tuple2<HttpServer, HttpHandler>> sidservers;
	
	/** The servers per port. */
	protected Map<Integer, HttpServer> portservers;
	
	//-------- constructors --------
	
	/**
	 *  Create a new publish service.
	 */
	public GrizzlyRestServicePublishService()
	{
	}
	
	/**
	 *  Create a new publish service.
	 */
	public GrizzlyRestServicePublishService(IRestMethodGenerator generator)
	{
		super(generator);
	}
	
	//-------- methods --------
	
	/**
	 * 
	 */
	public void internalPublishService(URI uri, ResourceConfig rc, IServiceIdentifier sid, PublishInfo info)
	{
		try
		{
			HttpServer server = getHttpServer(uri, info);
			System.out.println("Adding http handler to server: "+uri.getPath());
			HttpHandler handler = ContainerFactory.createContainer(HttpHandler.class, rc);
			ServerConfiguration sc = server.getServerConfiguration();
			sc.addHttpHandler(handler, uri.getPath());
			
			if(sidservers==null)
				sidservers = new HashMap<IServiceIdentifier, Tuple2<HttpServer, HttpHandler>>();
			sidservers.put(sid, new Tuple2<HttpServer, HttpHandler>(server, handler));
			
	//		Map<HttpHandler, String[]> handlers = server.getServerConfiguration().getHttpHandlers();
	//		for(HttpHandler hand: handlers.keySet())
	//		{
	//			Set<String> set = SUtil.arrayToSet(handlers.get(hand));
	//			if(set.contains(uri.getPath()))
	//			{
	//				handler = hand;
	//			}
	//		}
	//		if(handler==null)
	//		{
	//			ret.setException(new RuntimeException("Publication error, failed to get http handler: "+uri.getPath()));
	//		}
		}
		catch(Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	/**
	 *  Get or start an api to the http server.
	 */
	public HttpServer getHttpServer(URI uri, PublishInfo info)
	{
		HttpServer server = null;
		
		try
		{
//			URI baseuri = new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(), null, null, null);
			server = portservers==null? null: portservers.get(uri.getPort());
			
			if(server==null)
			{
				System.out.println("Starting new server: "+uri.getPort());
				
				String	keystore	= null;
				String	keystorepass	= null;
				if(info!=null)
				{
					for(UnparsedExpression upex: info.getProperties())
					{
						if("sslkeystore".equals(upex.getName()))
						{
							keystore	= (String) SJavaParser.getParsedValue(upex, null,
								component!=null? component.getFetcher(): null, component!=null? component.getClassLoader(): null);
						}
						else if("sslkeystorepass".equals(upex.getName()))
						{
							keystorepass	= (String) SJavaParser.getParsedValue(upex, null,
								component!=null? component.getFetcher(): null, component!=null? component.getClassLoader(): null);
						}
					}
				}
				
				if(keystore!=null)
				{
					SSLContextConfigurator sslContext = new SSLContextConfigurator();
					sslContext.setKeyStoreFile(keystore); // contains server keypair
					sslContext.setKeyStorePass(keystorepass);
//					sslContext.setTrustStoreFile("./truststore_server"); // contains client certificate
//					sslContext.setTrustStorePass("asdfgh");
					SSLEngineConfigurator sslConf = new SSLEngineConfigurator(sslContext).setClientMode(false);
					
					server = GrizzlyHttpServerFactory.createHttpServer(uri, (GrizzlyHttpContainer)null, true, sslConf, false);
				}
				else
				{
					server	= GrizzlyHttpServerFactory.createHttpServer(uri);
				}
				
				
				server.start();
				
				if(portservers==null)
					portservers = new HashMap<Integer, HttpServer>();
				portservers.put(uri.getPort(), server);
			}
		}
		catch(RuntimeException e)
		{
			throw e;
		}
		catch(Exception e)
		{
			throw new RuntimeException(e);
		}
		
		return server;
	}
	
//	/**
//	 * 
//	 */
//	public IFuture<Void> publishServet(URI uri, Object servlet)
//	{
//		HttpServer server = getHttpServer(uri, createConfig());
//		ServletConfigImpl conf = new ServletConfigImpl();
//		ServletHandler handler = new ServletHandler(conf);
//		handler.setContextPath(uri.getPath());
//        ServerConfiguration sc = server.getServerConfiguration();
//		sc.addHttpHandler(handler, uri.getPath());
//		
//		WebappContext ctx = new WebappContext(uri.getPath());
//		ServletRegistration reg = ctx.addServlet(SReflect.getInnerClassName(servlet.getClass()), servlet);
//		reg.addMapping(alias);
//		ctx.deploy(server);
//		
//		return IFuture.DONE;
//	}
	
	/**
	 *  Publish an html page.
	 */
	public IFuture<Void> publishHMTLPage(URI uri, String vhost, String html)
	{
		HttpServer server = getHttpServer(uri, null);
		
        ServerConfiguration sc = server.getServerConfiguration();
        Map<HttpHandler, String[]>	handlers	= sc.getHttpHandlers();
        HtmlHandler	htmlh	= null;
        for(Map.Entry<HttpHandler, String[]> entry: handlers.entrySet())
        {
        	if(entry.getKey() instanceof HtmlHandler)
        	{
        		if(Arrays.asList(entry.getValue()).contains(uri.getPath()))
        		{
	        		htmlh	= (HtmlHandler)entry.getKey();
	        		break;
        		}
        	}
        }
        
        if(htmlh==null)
        {
        	htmlh	= new HtmlHandler();
    		sc.addHttpHandler(htmlh, uri.getPath());
        }
        
       	htmlh.addMapping(vhost, html);
		
//		System.out.println("published at: "+uri.getPath());
		
		return IFuture.DONE;
	}
	
//	/**
//	 *  Publish a resource.
//	 */
////	public IFuture<Void> publishResource(URI uri, final ResourceInfo ri)
//	public IFuture<Void> publishResource(URI uri, final String type, final String filename)
//	{
//		HttpServer server = getHttpServer(uri, createConfig());
//		
//        ServerConfiguration sc = server.getServerConfiguration();
//		sc.addHttpHandler(new HttpHandler() 
//		{
//            public void service(Request request, Response resp) 
//            {
//            	try
//            	{
//	            	// Set response content type
//	                resp.setContentType(type!=null? type: "text/html");
//	
//	                InputStream is = null;
//	                try
//	        		{
//	        			is = SUtil.getResource0(filename, null);
//	        			OutputStream os = resp.getOutputStream();
//	        			SUtil.copyStream(is, os);
//	        		}
//	        		finally
//	        		{
//	        			try
//	        			{
//	        				if(is!=null)
//	        					is.close();
//	        			}
//	        			catch(Exception e)
//	        			{
//	        			}
//	        		}
//            	}
//            	catch(Exception e)
//            	{
//            		e.printStackTrace();
//            	}
//            }
//        }, uri.getPath());
//		
//		return IFuture.DONE;
//	}
	
	/**
	 *  Publish resources via a rel jar path.
	 *  The resources are searched with respect to the
	 *  component classloader (todo: allow for specifiying RID).
	 */
	public IFuture<Void> publishResources(final URI uri, final String path)
	{
		final Future<Void>	ret	= new Future<Void>();
		IComponentManagementService	cms	= SServiceProvider.getLocalService((IServiceProvider)component.getServiceContainer(), IComponentManagementService.class, RequiredServiceInfo.SCOPE_PLATFORM);
		IComponentIdentifier	cid	= ServiceCall.getLastInvocation()!=null && ServiceCall.getLastInvocation().getCaller()!=null ? ServiceCall.getLastInvocation().getCaller() : component.getComponentIdentifier();
		cms.getComponentDescription(cid)
			.addResultListener(new ExceptionDelegationResultListener<IComponentDescription, Void>(ret)
		{
			public void customResultAvailable(IComponentDescription desc)
			{
				ILibraryService	ls	= SServiceProvider.getLocalService((IServiceProvider)component.getServiceContainer(), ILibraryService.class, RequiredServiceInfo.SCOPE_PLATFORM);
				ls.getClassLoader(desc.getResourceIdentifier())
					.addResultListener(new ExceptionDelegationResultListener<ClassLoader, Void>(ret)
				{
					public void customResultAvailable(ClassLoader cl)
					{
						HttpServer server = getHttpServer(uri, null);
				        ServerConfiguration sc = server.getServerConfiguration();
						sc.addHttpHandler(new CLStaticHttpHandler(cl, path.endsWith("/")? path: path+"/")
						{
							public void service(Request request, Response resp) throws Exception
							{
								super.service(request, resp);
							}
						}, uri.getPath());
						
						System.out.println("Resource published at: "+uri.getPath());
						ret.setResult(null);
					}
				});
			}
		});
		
		return ret;
	}
	
	/**
	 *  Publish file resources from the file system.
	 */
	public IFuture<Void> publishExternal(URI uri, String rootpath)
	{		
		HttpServer server = getHttpServer(uri, null);
		
	    StaticHttpHandler handler	= new StaticHttpHandler(rootpath);
	    handler.setFileCacheEnabled(false);	// see http://stackoverflow.com/questions/13307489/grizzly-locks-static-served-resources
		
        ServerConfiguration sc = server.getServerConfiguration();
		sc.addHttpHandler(handler, uri.getPath());
		
//		System.out.println("published at: "+uri.getPath());
		
		return IFuture.DONE;
	}

	
	/**
	 *  Unpublish a service.
	 *  @param sid The service identifier.
	 */
	public IFuture<Void> unpublishService(IServiceIdentifier sid)
	{
		Future<Void> ret = new Future<Void>();
		boolean stopped = false;
		if(sidservers!=null)
		{
			Tuple2<HttpServer, HttpHandler> tup = sidservers.remove(sid);
			if(tup!=null)
			{
				HttpServer server = tup.getFirstEntity();
			    ServerConfiguration config = server.getServerConfiguration();
			    System.out.println("unpub: "+tup.getSecondEntity());
			    config.removeHttpHandler(tup.getSecondEntity());
			    if(config.getHttpHandlers().size()==0)
			    {
			    	for(Iterator<HttpServer> servers=portservers.values().iterator(); servers.hasNext(); )
			    	{
			    		if(servers.next().equals(server))
			    		{
			    			servers.remove();
			    			break;
			    		}
			    	}
			    	server.shutdownNow();

			    }
			    stopped = true;
				ret.setResult(null);
			}
		}
		if(!stopped)
			ret.setException(new RuntimeException("Published service could not be stopped: "+sid));
		return ret;
	}
	
	/**
	 *  Test if a service is published.
	 */
	public boolean isPublished(IServiceIdentifier sid)
	{
		return sidservers!=null && sidservers.containsKey(sid);
	}
	
	//-------- helper classes --------
	
	/**
	 *	Allow responding with different htmls based on virtual host name. 
	 */
	// Hack!! only works if no different contexts are published at overlapping resources: e.g. hugo.com/foo/bar vs. dummy.com/foo.
	public static class HtmlHandler extends HttpHandler
	{
		//-------- attributes --------
		
		/** The html responses (host->response). */
		protected Map<String, String>	mapping;

		//-------- constructors --------
		
		/**
		 *  Create an html handler.
		 */
		public HtmlHandler()
		{
			this.mapping = new LinkedHashMap<String, String>();
		}
		
		//-------- methods --------
		
		/**
		 *  Add a mapping.
		 */
		public void	addMapping(String host, String html)
		{
			this.mapping.put(host, html);
		}
		
		//-------- HttpHandler methods --------

		/**
		 *  Service the request.
		 */
		public void service(Request request, Response resp) 
		{
			String	host	= request.getHeader("host");
			int	idx	= host.indexOf(":");
			if(idx!=-1)
			{
				host	= host.substring(0, idx);
			}
			String	html	= null;
			
			for(Map.Entry<String, String> entry: mapping.entrySet())
			{
				if(entry.getKey().equals(host))
				{
					html	= entry.getValue();
					break;
				}
				else if(html==null)
				{
					// Use first entry, when no other match is found.
					html	= entry.getValue(); 
				}
			}
			
			try
			{
		    	// Set response content type
		        resp.setContentType("text/html");

		        // Actual logic goes here.
		        Writer out = resp.getWriter();
		        out.write(html);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}
}
