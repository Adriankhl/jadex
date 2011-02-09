package jadex.base.gui.filetree;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * 
 */
public class RemoteJarFile extends RemoteFile
{
	/** The map of jar entries. */
	protected Map jarentries;
	
	/** The relative path inside of the jar file. */
	protected String relativepath;
	
	/**
	 * 
	 */
	public RemoteJarFile()
	{
	}

	/**
	 * 
	 */
	public RemoteJarFile(String filename, String path, boolean directory, String displayname, Map jarentries, String relativepath)
	{
		super(filename, path, directory, displayname);
		this.jarentries = jarentries;
		this.relativepath = relativepath;
	}
	
	/**
	 * 
	 */
	public Collection listFiles()
	{
		return jarentries.get(relativepath)!=null? (Collection)jarentries.get(relativepath): Collections.EMPTY_LIST;
	}

	/**
	 *  Get the jarentries.
	 *  @return the jarentries.
	 */
	public Map getJarEntries()
	{
		return jarentries;
	}

	/**
	 *  Set the jarentries.
	 *  @param jarentries The jarentries to set.
	 */
	public void setJarEntries(Map jarentries)
	{
		this.jarentries = jarentries;
	}

	/**
	 *  Get the relativepath.
	 *  @return the relativepath.
	 */
	public String getRelativePath()
	{
		return relativepath;
	}

	/**
	 *  Set the relativepath.
	 *  @param relativepath The relativepath to set.
	 */
	public void setRelativePath(String relativepath)
	{
		this.relativepath = relativepath;
	}
	
}
