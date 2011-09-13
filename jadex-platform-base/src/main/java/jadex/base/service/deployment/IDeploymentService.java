package jadex.base.service.deployment;

import jadex.base.gui.filetree.FileData;
import jadex.bridge.service.IService;
import jadex.commons.Tuple2;
import jadex.commons.future.IFuture;

/**
 *  Interface for the deployment service.
 */
public interface IDeploymentService extends IService
{
//	/**
//	 *  Get a file.
//	 *  @return The file data.
//	 */
//	public IFuture<Tuple2<FileContent,String>> getFile(String path, int fragment, int fileid);

	/**
	 *  Put a file.
	 *  @param file The file data.
	 *  @param path The target path.
	 *  @return True, when the file has been copied.
	 */
	public IFuture<String> putFile(FileContent filedata, String path, String fileid);
	
	/**
	 *  Rename a file.
	 *  @param path The target path.
	 *  @param name The name.
	 *  @return True, if rename was successful.
	 */
	public IFuture<String> renameFile(String path, String name);
	
	/**
	 *  Delete a file.
	 *  @param path The target path.
	 *  @return True, if delete was successful.
	 */
	public IFuture<Void> deleteFile(String path);
	
	/**
	 *  Open a file.
	 *  @param path The filename to open.
	 */
	public IFuture<Void> openFile(String path);
	
	/**
	 *  Get the root devices.
	 *  @return The root device files.
	 */
	public IFuture<FileData[]> getRoots();
}
