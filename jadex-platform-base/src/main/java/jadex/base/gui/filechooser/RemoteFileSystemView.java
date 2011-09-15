package jadex.base.gui.filechooser;

import jadex.base.gui.SwingDefaultResultListener;
import jadex.base.gui.SwingDelegationResultListener;
import jadex.base.gui.filetree.FileData;
import jadex.bridge.IComponentStep;
import jadex.bridge.IExternalAccess;
import jadex.bridge.IInternalAccess;
import jadex.commons.SUtil;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.xml.annotation.XMLClassname;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileSystemView;
import javax.swing.filechooser.FileView;


/**
 *  Remote file system view.
 *  Is programmed asynchronously, i.e. delivers always
 *  what is has and then initiates a background search for
 *  retrieving current values. When values arrive a refresh
 *  operation is used to update the chooser.
 */
public class RemoteFileSystemView extends FileSystemView
{
	//-------- attributes --------
	
	/** The external access. */
	protected IExternalAccess	exta;

	/** The cache of children files (String parent -> File[] children). */
	protected Map children;
	
	/** The cache of parent files (String child -> File[] parent). */
	protected Map parents;
	
	/** The filechooser. */
	protected JFileChooser chooser;
	
	/** The home directory. */
	protected RemoteFile homedir;
	
	/** The default directory. */
	protected RemoteFile defaultdir;
	
	/** The current directory. */
	protected RemoteFile currentdir;
	
	//-------- constructors --------
	
	/**
	 *  Create a new file system view.
	 */
	public RemoteFileSystemView(IExternalAccess exta)
	{
		this.exta = exta;
		this.children = new HashMap();
		this.parents = new HashMap();
	}
	
	/**
	 *  Initialize the remote file system view such that
	 *  home, default and current directorty as well as roots
	 *  are available.
	 */
	public IFuture	init()
	{
		final Future	ret	= new Future();
		
		exta.scheduleStep(new IComponentStep()
		{
			@XMLClassname("init")
			public Object execute(IInternalAccess ia)
			{
				Object[]	ret	= new Object[4];
				FileSystemView view = FileSystemView.getFileSystemView();
				ret[0]	= FileData.convertToRemoteFiles(File.listRoots());
				ret[1]	= new FileData(view.getHomeDirectory());
				ret[2]	= new FileData(view.getDefaultDirectory());
				
				String	path	= new File(".").getAbsolutePath();
				if(path.endsWith("."))
				{
					path	= path.substring(0, path.length()-1);
				}
				ret[3]	= new FileData(new File(path));					
				
				return ret;
			}
		}).addResultListener(new SwingDelegationResultListener(ret)
		{
			public void customResultAvailable(Object result)
			{
				Object[]	res	= (Object[])result;
				FileData[] remfiles = (FileData[])res[0];
				File[] files = FileData.convertToFiles(remfiles);
				children.put("roots", files);
				homedir = new RemoteFile((FileData)res[1]);
				defaultdir = new RemoteFile((FileData)res[2]);
				currentdir = new RemoteFile((FileData)res[3]);

				ret.setResult(null);
			}
		});
		
		return ret;
	}
	
	//-------- methods --------
	
	/**
	 *  Set the file chooser.
	 */
	public void setFileChooser(JFileChooser chooser)
	{
		this.chooser = chooser;
		chooser.rescanCurrentDirectory();
	}

	/**
	 * Determines if the given file is a root in the navigatable tree(s).
	 * Examples: Windows 98 has one root, the Desktop folder. DOS has one root
	 * per drive letter, <code>C:\</code>, <code>D:\</code>, etc. Unix has one
	 * root, the <code>"/"</code> directory. The default implementation gets
	 * information from the <code>ShellFolder</code> class.
	 * 
	 * @param f a <code>File</code> object representing a directory
	 * @return <code>true</code> if <code>f</code> is a root in the navigatable
	 *         tree.
	 * @see #isFileSystemRoot
	 */
//	public boolean isRoot(File f)
//	{
//		if(f == null || !f.isAbsolute())
//		{
//			return false;
//		}
//
//		File[] roots = getRoots();
//		for(int i = 0; i < roots.length; i++)
//		{
//			if(roots[i].equals(f))
//			{
//				return true;
//			}
//		}
//		return false;
//	}

	/**
	 * Returns true if the file (directory) can be visited. Returns false if the
	 * directory cannot be traversed.
	 * 
	 * @param f the <code>File</code>
	 * @return <code>true</code> if the file/directory can be traversed,
	 *         otherwise <code>false</code>
	 * @see JFileChooser#isTraversable
	 * @see FileView#isTraversable
	 * @since 1.4
	 */
	public Boolean isTraversable(File f)
	{
		return Boolean.valueOf(f.isDirectory());
	}

	/**
	 * Name of a file, directory, or folder as it would be displayed in a system
	 * file browser. Example from Windows: the "M:\" directory displays as
	 * "CD-ROM (M:)" The default implementation gets information from the
	 * ShellFolder class.
	 * 
	 * @param f a <code>File</code> object
	 * @return the file name as it would be displayed by a native file chooser
	 * @see JFileChooser#getName
	 * @since 1.4
	 */
	public String getSystemDisplayName(File f)
	{
		if(f instanceof RemoteFile)
		{
			return ((RemoteFile)f).getFiledata().getDisplayName();
		}
		else
		{
			System.out.println("normal file:" +f.getName());
			return super.getSystemDisplayName(f);
		}
	}

	/**
	 * Type description for a file, directory, or folder as it would be
	 * displayed in a system file browser. Example from Windows: the "Desktop"
	 * folder is desribed as "Desktop". Override for platforms with native
	 * ShellFolder implementations.
	 * 
	 * @param f a <code>File</code> object
	 * @return the file type description as it would be displayed by a native
	 *         file chooser or null if no native information is available.
	 * @see JFileChooser#getTypeDescription
	 * @since 1.4
	 */
	public String getSystemTypeDescription(File f)
	{
		return null;
	}

//	/**
//	 * Icon for a file, directory, or folder as it would be displayed in a
//	 * system file browser. Example from Windows: the "M:\" directory displays a
//	 * CD-ROM icon. The default implementation gets information from the
//	 * ShellFolder class.
//	 * 
//	 * @param f a <code>File</code> object
//	 * @return an icon as it would be displayed by a native file chooser
//	 * @see JFileChooser#getIcon
//	 * @since 1.4
//	 */
//	public Icon getSystemIcon(File f)
//	{
//		if(f == null)
//		{
//			return null;
//		}
//
//		ShellFolder sf;
//
//		try
//		{
//			sf = getShellFolder(f);
//		}
//		catch(FileNotFoundException e)
//		{
//			return null;
//		}
//
//		Image img = sf.getIcon(false);
//
//		if(img != null)
//		{
//			return new ImageIcon(img, sf.getFolderType());
//		}
//		else
//		{
//			return UIManager.getIcon(f.isDirectory() ? "FileView.directoryIcon"
//					: "FileView.fileIcon");
//		}
//	}

	/**
	 * On Windows, a file can appear in multiple folders, other than its parent
	 * directory in the filesystem. Folder could for example be the "Desktop"
	 * folder which is not the same as file.getParentFile().
	 * 
	 * @param folder a <code>File</code> object repesenting a directory or
	 *        special folder
	 * @param file a <code>File</code> object
	 * @return <code>true</code> if <code>folder</code> is a directory or
	 *         special folder and contains <code>file</code>.
	 * @since 1.4
	 */
	public boolean isParent(File folder, File file)
	{
		if(folder instanceof RemoteFile && file instanceof RemoteFile)
		{
			String p1 = folder.getAbsolutePath();
			String p2 = file.getAbsolutePath();
			
			if(p2.startsWith(p1))
			{
				String end = p2.substring(p1.length());
				int cnt = 0;
				boolean allowed = true;
				for(int i=0; i<end.length(); i++)
				{
					char c = end.charAt(i);
					if(allowed && (c=='/' || c=='\\'))
					{
						cnt++;
						allowed = false;
					}
					else
					{
						allowed = true;
					}
				}
				return cnt==1;
			}
			
			return false;
		}
		else
		{
			return super.isParent(folder, file);
		}
	}

//	/**
//	 * @param parent a <code>File</code> object repesenting a directory or
//	 *        special folder
//	 * @param fileName a name of a file or folder which exists in
//	 *        <code>parent</code>
//	 * @return a File object. This is normally constructed with <code>new
//	 * File(parent, fileName)</code> except when parent and child are both
//	 *         special folders, in which case the <code>File</code> is a wrapper
//	 *         containing a <code>ShellFolder</code> object.
//	 * @since 1.4
//	 */
//	public File getChild(File parent, String fileName)
//	{
//		if(parent instanceof ShellFolder)
//		{
//			File[] children = getFiles(parent, false);
//			for(int i = 0; i < children.length; i++)
//			{
//				if(children[i].getName().equals(fileName))
//				{
//					return children[i];
//				}
//			}
//		}
//		return createFileObject(parent, fileName);
//	}


	/**
	 * Checks if <code>f</code> represents a real directory or file as opposed
	 * to a special folder such as <code>"Desktop"</code>. Used by UI classes to
	 * decide if a folder is selectable when doing directory choosing.
	 * 
	 * @param f a <code>File</code> object
	 * @return <code>true</code> if <code>f</code> is a real file or directory.
	 * @since 1.4
	 */
//	public boolean isFileSystem(File f)
//	{
//		if(f instanceof ShellFolder)
//		{
//			ShellFolder sf = (ShellFolder)f;
//			// Shortcuts to directories are treated as not being file system
//			// objects,
//			// so that they are never returned by JFileChooser.
//			return sf.isFileSystem() && !(sf.isLink() && sf.isDirectory());
//		}
//		else
//		{
//			return true;
//		}
//	}

	/**
	 * Creates a new folder with a default folder name.
	 */
	public File createNewFolder(File containingDir) throws IOException
	{
		return null;
	}

	/**
	 * Returns whether a file is hidden or not.
	 */
	public boolean isHiddenFile(File f)
	{
		return f.isHidden();
	}

	/**
	 * Is dir the root of a tree in the file system, such as a drive or
	 * partition. Example: Returns true for "C:\" on Windows 98.
	 * 
	 * @param dir a <code>File</code> object representing a directory
	 * @return <code>true</code> if <code>f</code> is a root of a filesystem
	 * @see #isRoot
	 * @since 1.4
	 */
//	public boolean isFileSystemRoot(File dir)
//	{
//		return ShellFolder.isFileSystemRoot(dir);
//	}

//	/**
//	 * Used by UI classes to decide whether to display a special icon for drives
//	 * or partitions, e.g. a "hard disk" icon. The default implementation has no
//	 * way of knowing, so always returns false.
//	 * 
//	 * @param dir a directory
//	 * @return <code>false</code> always
//	 * @since 1.4
//	 */
//	public boolean isDrive(File dir)
//	{
//		return false;
//	}

//	/**
//	 * Used by UI classes to decide whether to display a special icon for a
//	 * floppy disk. Implies isDrive(dir). The default implementation has no way
//	 * of knowing, so always returns false.
//	 * 
//	 * @param dir a directory
//	 * @return <code>false</code> always
//	 * @since 1.4
//	 */
//	public boolean isFloppyDrive(File dir)
//	{
//		return false;
//	}

	/**
	 * Used by UI classes to decide whether to display a special icon for a
	 * computer node, e.g. "My Computer" or a network server. The default
	 * implementation has no way of knowing, so always returns false.
	 * 
	 * @param dir a directory
	 * @return <code>false</code> always
	 * @since 1.4
	 */
//	public boolean isComputerNode(File dir)
//	{
//		return ShellFolder.isComputerNode(dir);
//	}


	/**
	 * Returns all root partitions on this system. For example, on Windows, this
	 * would be the "Desktop" folder, while on DOS this would be the A: through
	 * Z: drives.
	 */
	public File[] getRoots()
	{
		File[] ret = (File[])children.get("roots");
		
		if(ret==null)
		{
			exta.scheduleStep(new IComponentStep()
			{
				@XMLClassname("getFiles")
				public Object execute(IInternalAccess ia)
				{
//					FileSystemView view = FileSystemView.getFileSystemView();
//					File[] roots = view.getRoots();
					File[] roots = File.listRoots();
					return FileData.convertToRemoteFiles(roots);
				}
			}).addResultListener(new SwingDefaultResultListener()
			{
				public void customResultAvailable(Object result)
				{
					FileData[] remfiles = (FileData[])result;
					File[] files = FileData.convertToFiles(remfiles);
					children.put("roots", files);
//					System.out.println("roots: "+SUtil.arrayToString(files));
					if(chooser!=null)
						chooser.rescanCurrentDirectory();
					
					System.out.println("Found roots: "+SUtil.arrayToString(files));
				}
			});
		}

		return ret==null? new File[0]: ret;
	}


	// Providing default implementations for the remaining methods
	// because most OS file systems will likely be able to use this
	// code. If a given OS can't, override these methods in its
	// implementation.
	public File getHomeDirectory()
	{
		if(homedir==null)
		{
			exta.scheduleStep(new IComponentStep()
			{
				@XMLClassname("getHomeDirectory")
				public Object execute(IInternalAccess ia)
				{
					FileSystemView view = FileSystemView.getFileSystemView();
					File ret = view.getHomeDirectory();
					return ret!=null? new FileData(ret): null;
				}
			}).addResultListener(new SwingDefaultResultListener()
			{
				public void customResultAvailable(Object result)
				{
					if(result!=null)
					{
						FileData file = (FileData)result;
						homedir = new RemoteFile(file);
					}
					if(chooser!=null)
						chooser.setCurrentDirectory(homedir);
					System.out.println("home: "+homedir);
				}
			});
		}
		
		return homedir==null? new RemoteFile(new FileData("unknown", "unknown", true, "unknown", 0, File.separatorChar, 3)): homedir;
	}

	/**
	 *  Get the current directory of the remote VM.
	 */
	public File getCurrentDirectory()
	{
		if(currentdir==null)
		{
			exta.scheduleStep(new IComponentStep()
			{
				@XMLClassname("getCurrentDirectory")
				public Object execute(IInternalAccess ia)
				{
					String	path	= new File(".").getAbsolutePath();
					if(path.endsWith("."))
					{
						path	= path.substring(0, path.length()-1);
					}
					return new FileData(new File(path));					
				}
			}).addResultListener(new SwingDefaultResultListener()
			{
				public void customResultAvailable(Object result)
				{
					if(result!=null)
					{
						FileData file = (FileData)result;
						currentdir = new RemoteFile(file);
					}
					if(chooser!=null)
						chooser.setCurrentDirectory(currentdir);
					System.out.println("currentdir: "+currentdir);
				}
			});
		}
		
		return currentdir==null? new RemoteFile(new FileData("unknown", "unknown", true, "unknown", 0, File.separatorChar, 3)): currentdir;
	}
	
	/**
	 * Return the user's default starting directory for the file chooser.
	 * 
	 * @return a <code>File</code> object representing the default starting
	 *         folder
	 * @since 1.4
	 */
	public File getDefaultDirectory()
	{
		if(defaultdir==null)
		{
			exta.scheduleStep(new IComponentStep()
			{
				@XMLClassname("getDefaultDirectory")
				public Object execute(IInternalAccess ia)
				{
					FileSystemView view = FileSystemView.getFileSystemView();
					File ret = view.getDefaultDirectory();
					return ret!=null? new FileData(ret): null;
				}
			}).addResultListener(new SwingDefaultResultListener()
			{
				public void customResultAvailable(Object result)
				{
					if(result!=null)
					{
						FileData file = (FileData)result;
						defaultdir = new RemoteFile(file);
					}
					if(chooser!=null)
						chooser.setCurrentDirectory(defaultdir);
					System.out.println("default: "+defaultdir);
				}
			});
		}
		
		return defaultdir==null? new RemoteFile(new FileData("unknown", "unknown", true, "unknown", 0, File.separatorChar, 3)): defaultdir;
	}

	/**
	 * Returns a File object constructed in dir from the given filename.
	 */
	public File createFileObject(File dir, String filename)
	{
		System.out.println("createFileObject: "+dir+" "+filename);
		return super.createFileObject(dir, filename);
//		if(dir == null)
//		{
//			return new File(filename);
//		}
//		else
//		{
//			return new File(dir, filename);
//		}
	}

	/**
	 * Returns a File object constructed from the given path string.
	 */
	public File createFileObject(String path)
	{
		System.out.println("createFileObject: "+path);
		return super.createFileObject(path);
//		File f = new File(path);
//		if(isFileSystemRoot(f))
//		{
//			f = createFileSystemRoot(f);
//		}
//		return f;
	}


	/**
	 * Gets the list of shown (i.e. not hidden) files.
	 */
	public File[] getFiles(final File dir, final boolean useFileHiding)
	{
		if(dir==null)
			return new File[0];
		
		File[] ret = (File[])children.get(dir.getAbsolutePath());
		
		if(ret==null)
		{
			final FileData mydir = new FileData(dir);
			exta.scheduleStep(new IComponentStep()
			{
				@XMLClassname("getFiles")
				public Object execute(IInternalAccess ia)
				{
					File dir = new File(mydir.getPath());
					File[] files;
					if(dir.exists())
					{
						FileSystemView view = FileSystemView.getFileSystemView();
						files = view.getFiles(dir, useFileHiding);
						System.out.println("children: "+dir+" "+SUtil.arrayToString(files));
					}
					else
					{
						System.out.println("file does not exist: "+dir);
						files = new File[0];
					}
					return FileData.convertToRemoteFiles(files);
				}
			}).addResultListener(new SwingDefaultResultListener()
			{
				public void customResultAvailable(Object result)
				{
					FileData[] remfiles = (FileData[])result;
					RemoteFile[] files = FileData.convertToFiles(remfiles);
//					System.out.println("children: "+dir+" "+SUtil.arrayToString(files));
					children.put(dir.getAbsolutePath(), files);
					for(int i=0; i<files.length; i++)
					{
						parents.put(files[i].getAbsolutePath(), dir);
					}
					if(chooser!=null)
						chooser.rescanCurrentDirectory();
				}
			});
		}
		else
		{
			System.out.println("cached children: "+dir+" "+SUtil.arrayToString(ret));
		}

		return ret==null? new File[0]: ret;
	}


	/**
	 * Returns the parent directory of <code>dir</code>.
	 * 
	 * @param dir the <code>File</code> being queried
	 * @return the parent directory of <code>dir</code>, or <code>null</code> if
	 *         <code>dir</code> is <code>null</code>
	 */
	public File getParentDirectory(final File dir)
	{
		if(dir==null)
			return null;
		
		File parent = (File)parents.get(dir.getAbsolutePath());
	
		if(parent==null)
		{
			final String path = dir.getAbsolutePath();
			exta.scheduleStep(new IComponentStep()
			{
				@XMLClassname("getParentDirectory")
				public Object execute(IInternalAccess ia)
				{
					FileSystemView view = FileSystemView.getFileSystemView();
					File parent = view.getParentDirectory(new File(path)); // todo: useFileHandling
					return parent!=null? new FileData(parent): null;
				}
			}).addResultListener(new SwingDefaultResultListener()
			{
				public void customResultAvailable(Object result)
				{
					if(result!=null)
					{
						FileData remfile = (FileData)result;
						RemoteFile parent = new RemoteFile(remfile);
						parents.put(dir.getAbsolutePath(), parent);
						if(chooser!=null)
							chooser.setCurrentDirectory(parent);
					}
//					System.out.println("parent: "+dir+" "+parent);
//					javax.swing.plaf.basic.BasicDirectoryModel
				}
			});
		}
		
		System.out.println("parent: "+dir+" "+parent);
		return parent;
	}

//	/**
//	 * Throws {@code FileNotFoundException} if file not found or current thread
//	 * was interrupted
//	 */
//	ShellFolder getShellFolder(File f) throws FileNotFoundException
//	{
//		if(!(f instanceof ShellFolder) && !(f instanceof FileSystemRoot)
//				&& isFileSystemRoot(f))
//		{
//			f = createFileSystemRoot(f);
//		}
//
//		try
//		{
//			return ShellFolder.getShellFolder(f);
//		}
//		catch(InternalError e)
//		{
//			System.err.println("FileSystemView.getShellFolder: f=" + f);
//			e.printStackTrace();
//			return null;
//		}
//	}
	
	/**
	 *  Main for testing.
	 */
	public static void main(String[] args) throws Exception
	{
		File f = new File("C:\\projects\\jadex.jar");
		System.out.println(f.getName()+" "+f.getPath()+" "+f.getAbsolutePath()+" "+f.getCanonicalPath());
	}
}
