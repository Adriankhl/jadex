package jadex.base.relay;

import java.io.File;

import com.maxmind.geoip.Location;
import com.maxmind.geoip.LookupService;
import com.maxmind.geoip.regionName;

/**
 *  Helper object to resolve IP addresses to Geo locations.
 *  Uses free API and database file available at: http://www.maxmind.com/app/geolitecity
 *  Requires GeoLiteCity.dat to be present in <user.home>/.relaystats directory.
 */
public class GeoIPService
{
	//-------- static part --------

	/** The singleton db object. */
	protected static GeoIPService	singleton	= new GeoIPService();
	
	/**
	 *  Get the db instance.
	 */
	public static GeoIPService	getGeoIPService()
	{
		return singleton;
	}
	
	//-------- attributes --------
	
	/** The lookup service (if any). */
	protected LookupService	ls;
	
	//-------- constructors --------
	
	/**
	 *  Create the db object.
	 */
	public GeoIPService()
	{
		try
		{
			// Set up geo ip lookup service.
			String	systemdir	= new File(System.getProperty("user.home"), ".relaystats").getAbsolutePath();
			System.out.println("Expecting GeoLiteCity.dat in: "+systemdir);
		    ls	= new LookupService(new File(systemdir, "GeoLiteCity.dat").getAbsolutePath(), LookupService.GEOIP_MEMORY_CACHE);
		}
		catch(Exception e)
		{
			// Ignore errors and let relay work without geo location.
			System.err.println("Warning: Relay could not initialize GeoIP service: "+ e);
		}
	}
	
	//-------- methods --------
	
	/**
	 *  Fetch city name for an IP address.
	 */
	public String	getCity(String ip)
	{
		String	ret	= null;
		
		if(ls!=null)
		{
			try
			{
				Location	loc	= ls.getLocation(ip);
				ret	= loc.city;
			}
			catch(Exception e)
			{
				// Ignore errors and let relay work without stats.
				System.err.println("Warning: Could not get Geo location: "+ e);
			}
		}
		
		if(ret==null)
		{
			ret	= "unknown";
		}
		
		return ret;
	}

	/**
	 *  Fetch region name for an IP address.
	 */
	public String	getRegion(String ip)
	{
		String	ret	= null;
		
		if(ls!=null)
		{
			try
			{
				Location	loc	= ls.getLocation(ip);
				ret	= regionName.regionNameByCode(loc.countryCode, loc.region);
			}
			catch(Exception e)
			{
				// Ignore errors and let relay work without stats.
				System.err.println("Warning: Could not get Geo location: "+ e);
			}
		}
		
		if(ret==null)
		{
			ret	= "unknown";
		}
		
		return ret;
	}

	/**
	 *  Fetch country information for an IP address.
	 */
	public String	getCountry(String ip)
	{
		String	ret	= null;
		
		if(ls!=null)
		{
			try
			{
				Location	loc	= ls.getLocation(ip);
				ret	= loc.countryName;
			}
			catch(Exception e)
			{
				// Ignore errors and let relay work without stats.
				System.err.println("Warning: Could not get Geo location: "+ e);
			}
		}
		
		if(ret==null)
		{
			ret	= "unknown";
		}
		
		return ret;
	}
}
