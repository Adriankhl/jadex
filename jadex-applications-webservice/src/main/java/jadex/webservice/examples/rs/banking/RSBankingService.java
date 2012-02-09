package jadex.webservice.examples.rs.banking;

import jadex.commons.SUtil;
import jadex.commons.future.ThreadSuspendable;
import jadex.extension.rs.publish.DefaultRestServicePublishService;
import jadex.xml.bean.JavaReader;
import jadex.xml.bean.JavaWriter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.multipart.FormDataParam;

/**
 *  Rest service implementation that provides some methods by itself.
 *  Using the 'generate=true' option the missing interface methods
 *  will be automatically added by Jadex Rest publishing. 
 */
// Note, if the class has a Path annotation jesery automatically activates it.
//@Path("/")
public class RSBankingService
{
	public static String info;
	
	static
	{
		try
		{
			InputStream is = SUtil.getResource0("jadex/webservice/examples/rs/banking/BankingServiceInfo.html", 
				Thread.currentThread().getContextClassLoader());
			info = new Scanner(is).useDelimiter("\\A").next();
//			System.out.println(info);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	@Context 
	public ResourceConfig rc;
	
	@Context
	public UriInfo uriinfo;

	/**
	 *  Get the account statement.
	 *  @param request The request.
	 *  @return The account statement.
	 */
	@GET
	@Produces(MediaType.TEXT_HTML)
	public String getServiceInfo()
	{
		UriBuilder ub = uriinfo.getBaseUriBuilder();
		ub.path("getAccountStatement");
		String req = ub.build(null).toString();
		ub = uriinfo.getBaseUriBuilder();
		ub.path("addTransaction");
		String add = ub.build(null).toString();
		String ret = info.replace("$req", req);
		ret = ret.replace("$add", add);
		System.out.println(ret);
		return ret;
	}
	
//	/**
//	 *  Get the account statement.
//	 *  @param request The request.
//	 *  @return The account statement.
//	 */
//	@GET
//	@Path("getAccountStatement")
//	@Produces(MediaType.APPLICATION_XML)
//	public String getAccountStatement(@QueryParam("begin") String begin, @QueryParam("end") String end)
//	{
//		System.out.println("getAccountStatement: "+begin+" "+end);
//		
//		//Jadex service can be fetched from properties
//		IBankingService bs = (IBankingService)rc.getProperties().get(DefaultRestServicePublishService.JADEXSERVICE);
//		Request req;
//		SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
//		Date b = null;
//		Date e = null;
//		if(begin!=null && end!=null)
//		{
//			try
//			{
//				b = sdf.parse(begin);
//			}
//			catch(Exception ex)
//			{
//				b = new Date();
//			}
//			try
//			{
//				e = sdf.parse(end);
//			}
//			catch(Exception ex)
//			{
//				e = new Date();
//			}
//		}
//		req = new Request(b, e);
//		AccountStatement ret = bs.getAccountStatement(req).get(new ThreadSuspendable(this));
//		
//		return JavaWriter.objectToXML(ret, null);
//	}
	
	/**
	 *  Get the account statement.
	 *  @param request The request.
	 *  @return The account statement.
	 */
	@POST
	@Path("getAccountStatement")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_XML)
	public String getAccountStatement(@FormDataParam("begin") Date begin, @FormDataParam("end") Date end)
	{
		return "called";
	}
	
}
