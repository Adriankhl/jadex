package jadex.base.gui.filetree;

import jadex.base.gui.filechooser.RemoteFile;
import jadex.commons.SUtil;

import java.io.File;

import javax.swing.filechooser.FileSystemView;

/**
 *  A file data represents a java.io.File that
 *  can be transferred to remote address spaces.
 *  Does only transfer file information, not the
 *  binary data itself.
 */
public class FileData
{
	//-------- attributes --------
	
	/** The file name. */
	protected String filename;
	
	/** The path. */
	protected String path;
	
	/** The boolean for directory. */
	protected boolean directory;
	
	/** The display name. */
	protected String displayname;

	/** The last modified date. */
	protected long lastmodified;
	
	//-------- constructors --------

	/**
	 *  Create a new remote file.
	 */
	public FileData()
	{
		// Needed for bean creation.
	}

	/**
	 *  Create a new remote file.
	 */
	public FileData(String filename, String path, boolean directory, 
		String displayname, long lastmodified)
	{
		this.filename = filename;
		this.path = path;
		this.directory = directory;
		this.displayname = displayname;
		this.lastmodified = lastmodified;
	}
	
	/**
	 *  Create a new remote file.
	 */
	public FileData(File file)
	{
		this.filename = file.getName();
		this.path = file.getAbsolutePath();
		this.directory = SUtil.arrayToSet(File.listRoots()).contains(file) || file.isDirectory();	// Hack to avoid access to floppy disk.
		this.displayname = getDisplayName(file);
		this.lastmodified = FileSystemView.getFileSystemView().isFloppyDrive(file)
			? 0 : file.lastModified();
//		this.root = SUtil.arrayToSet(file.listRoots()).contains(file);
	}
	
	//-------- methods --------
	
	/**
	 *  Get the filename.
	 *  @return the filename.
	 */
	public String getFilename()
	{
		return filename;
	}

	/**
	 *  Set the filename.
	 *  @param filename The filename to set.
	 */
	public void setFilename(String filename)
	{
		this.filename = filename;
	}

	/**
	 *  Get the path.
	 *  @return the path.
	 */
	public String getPath()
	{
		return path;
	}

	/**
	 *  Set the path.
	 *  @param path The path to set.
	 */
	public void setPath(String path)
	{
		this.path = path;
	}

	/**
	 *  Get the directory.
	 *  @return the directory.
	 */
	public boolean isDirectory()
	{
		return directory;
	}

	/**
	 *  Set the directory.
	 *  @param directory The directory to set.
	 */
	public void setDirectory(boolean directory)
	{
		this.directory = directory;
	}

	/**
	 *  Get the displayname.
	 *  @return the displayname.
	 */
	public String getDisplayName()
	{
		return displayname;
	}

	/**
	 *  Set the displayname.
	 *  @param displayname The displayname to set.
	 */
	public void setDisplayName(String displayname)
	{
		this.displayname = displayname;
	}
	
	/**
	 *  Get the display name for a file.
	 */
	public static String getDisplayName(File file)
	{
		String ret = FileSystemView.getFileSystemView().isFloppyDrive(file) 
			? null : FileSystemView.getFileSystemView().getSystemDisplayName(file);
		if(ret==null || ret.length()==0)
			ret = file.getName();
		if(ret==null || ret.length()==0)
			ret = file.getPath();
		return ret;
	}
	
	/**
	 *  Get the lastmodified.
	 *  @return the lastmodified.
	 */
	public long getLastModified()
	{
		return lastmodified;
	}

	/**
	 *  Set the lastmodified.
	 *  @param lastmodified The lastmodified to set.
	 */
	public void setLastModified(long lastmodified)
	{
		this.lastmodified = lastmodified;
	}

	/**
	 *  Convert remote files to files.
	 */
	public static RemoteFile[] convertToFiles(FileData[] remfiles)
	{
		RemoteFile[] ret = remfiles==null? new RemoteFile[0]: new RemoteFile[remfiles.length];
		for(int i=0; i<ret.length; i++)
		{
			ret[i] = new RemoteFile(remfiles[i]);
		}
		return ret;
	}
	
	/**
	 *  Convert files to remote files.
	 */
	public static FileData[] convertToRemoteFiles(File[] files)
	{
		FileData[] ret = files==null? new FileData[0]: new FileData[files.length];
		for(int i=0; i<ret.length; i++)
		{
			ret[i] = new FileData(files[i]);
		}
		return ret;
	}

	/**
	 *  Get the string representation.
	 *  @return The string representation.
	 */
	public String toString()
	{
		return "FileData(filename=" + filename + ", path=" + path
			+ ", directory=" + directory + ", displayname=" + displayname+ ")";
	}
	
	
}
