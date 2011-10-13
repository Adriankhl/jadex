package jadex.bridge.service.library;

import jadex.bridge.IResourceIdentifier;
import jadex.bridge.service.IService;
import jadex.bridge.service.annotation.Excluded;
import jadex.bridge.service.annotation.GuiClassName;
import jadex.commons.future.IFuture;

import java.net.URL;
import java.util.List;

/** 
 *  Interface for the Library Service. It provides a platform service
 *  for dynamic loading and unloading of classes, libraries and resources.
 */
@GuiClassName("jadex.tools.libtool.LibServiceBrowser")
public interface ILibraryService extends IService
{
	//-------- constants --------
	
	/** 
	 * The (standard) Library service name.
	 */
	public static final String LIBRARY_SERVICE = "library_service";
	
	//-------- methods --------
	
	/**
	 *  Add a new url.
	 *  @param url The url.
	 */
	// todo: make return IFuture
	public void addURL(URL url);
	
	/**
	 *  Remove a url.
	 *  @param url The url.
	 */
	public void removeURL(URL url);
	
	/**
	 *  Remove a url completely (all references).
	 *  @param url The url.
	 */
	public void removeURLCompletely(URL url);
	
	/**
	 *  Get all managed entries as URLs.
	 *  @return The list of urls.
	 */
	public IFuture<List<URL>> getURLs();
	
	/**
	 *  Get other contained (but not directly managed) URLs.
	 *  @return The list of urls.
	 */
	public IFuture<List<URL>> getNonManagedURLs();
	
	/**
	 *  Get all urls (managed and non-managed).
	 *  @return The list of urls.
	 */
	public IFuture<List<URL>> getAllURLs();
	
	/**
	 *  Get the non-managed classpath entries as strings.
	 *  @return Classpath entries as a list of strings.
	 */
	public IFuture<List<String>> getURLStrings();
	
	/**
	 *  Get the non-managed classpath entries.
	 *  @return Classpath entries as a list of strings.
	 */
	public IFuture<List<String>> getNonManagedURLStrings();
	
	/**
	 *  Get a class definition.
	 *  @param name The class name.
	 *  @return The class definition as byte array.
	 */
	public IFuture<byte[]> getClassDefinition(String name);
	
	/** 
	 *  Returns the current ClassLoader.
	 *  @return the current ClassLoader
	 */
//	@Excluded()
//	public ClassLoader getClassLoader();
	
	/** 
	 *  Returns the current ClassLoader.
	 *  @return the current ClassLoader
	 */
	@Excluded()
	public ClassLoader getClassLoader(IResourceIdentifier rid);
	
	/** 
	 *  Returns the resource identifier.
	 *  @return The resource identifier.
	 */
	@Excluded()
	public IResourceIdentifier getResourceIdentifier(String filename);

	//-------- listener methods --------
	
	/**
     *  Add an Library Service listener.
     *  The listener is registered for changes in the loaded library states.
     *  @param listener The listener to be added.
     */
    public void addLibraryServiceListener(ILibraryServiceListener listener);
    
    /**
     *  Remove an Library Service listener.
     *  @param listener  The listener to be removed.
     */
    public void removeLibraryServiceListener(ILibraryServiceListener listener);
}
