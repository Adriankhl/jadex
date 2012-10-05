package jadex.backup.resource;

import jadex.bridge.IInternalAccess;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.bridge.service.annotation.Service;
import jadex.bridge.service.types.deployment.FileData;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.commons.future.IResultListener;
import jadex.commons.future.ITerminableIntermediateFuture;
import jadex.commons.future.IntermediateDefaultResultListener;
import jadex.commons.future.TerminableIntermediateFuture;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.AgentArgument;
import jadex.micro.annotation.AgentCreated;
import jadex.micro.annotation.AgentKilled;
import jadex.micro.annotation.Argument;
import jadex.micro.annotation.Arguments;
import jadex.micro.annotation.Implementation;
import jadex.micro.annotation.ProvidedService;
import jadex.micro.annotation.ProvidedServices;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *  A component that publishes a local folder.
 */
@Arguments({
	@Argument(name="dir", clazz=String.class, description="The directory to publish."),
	@Argument(name="id", clazz=String.class, description="The unique id of the global resource.")
})
@ProvidedServices({
	@ProvidedService(type=IResourceService.class, implementation=@Implementation(expression="$pojoagent")),
	@ProvidedService(type=ILocalResourceService.class, implementation=@Implementation(expression="$pojoagent"))
})
@Agent
@Service(IResourceService.class)
public class ResourceProviderAgent	implements IResourceService, ILocalResourceService
{
	//-------- attributes --------
	
	/** The component. */
	@Agent
	protected IInternalAccess	component;
	
	/** The directory to publish as resource. */
	@AgentArgument
	protected String	dir;
	
	/** The global resource id. */
	@AgentArgument
	protected String	id;
	
	/** The resource meta information. */
	protected BackupResource	resource;
	
	/** Synchronization requests that are queued or in progress. */
	protected Map<IResourceService, TerminableIntermediateFuture<FileData>>	updates;
	
	//-------- constructors --------
	
	/**
	 *  Called at startup.
	 */
	@AgentCreated
	public IFuture<Void>	start()
	{
		Future<Void>	ret	= new Future<Void>();
		try
		{
			this.resource	= new BackupResource(id, new File(dir), component.getComponentIdentifier());
			ret.setResult(null);			
		}
		catch(Exception e)
		{
			ret.setException(e);
		}
		return ret;
	}
	
	/**
	 *  Called on shutdown.
	 */
	@AgentKilled
	public void	stop()
	{
		if(updates!=null)
		{
			for(ITerminableIntermediateFuture<FileData> update: updates.values())
			{
				update.terminate();
			}
		}
		
		resource.dispose();
	}
	
	//-------- IResourceService interface --------

	/**
	 *  Get the global resource id.
	 *  The global resource id is a unique id that is
	 *  used to identify the same resource on different
	 *  hosts, i.e. provided by different service instances.
	 *  
	 *  The id is static, i.e. it does not change during
	 *  the lifetime of the resource.
	 */
	public String	getResourceId()	
	{
		return resource.getResourceId();
	}
	
	/**
	 *  Get the local resource id.
	 *  The local resource id is a unique id that is
	 *  used to identify an individual instance of a
	 *  distributed resource on a specific host.
	 *  
	 *  The id is static, i.e. it does not change during
	 *  the lifetime of the resource.
	 */
	public String	getLocalId()
	{
		return resource.getLocalId();
	}
	
	/**
	 *  Get information about local files.
	 */
	public IFuture<FileInfo[]>	getFiles(FileInfo dir)
	{
		List<FileInfo>	ret	= null;
		File	fdir	= resource.getFile(dir);
//		if(fdir.lastModified()>dir.getTimeStamp())
		{
			ret	= new ArrayList<FileInfo>();
			for(String file: fdir.list())
			{
				FileInfo	fi	= resource.getFileInfo(new File(fdir, file));
				if(!".jadexbackup".equals(file))
				{
					ret.add(fi);
				}
			}
		}
		return new Future<FileInfo[]>(ret==null ? null : ret.toArray(new FileInfo[ret.size()]));
	}
	
	
	/**
	 *  Get all changes files and directories since a given time point.
	 *  @param time	The local vector time point.
	 *  @return File infos for changed files and directories.
	 */
	public IFuture<FileInfo[]>	getChanges(int time)
	{
		List<FileInfo>	ret	= new ArrayList<FileInfo>();
		List<File>	todo	= new LinkedList<File>();
		todo.add(resource.getResourceRoot());
		while(!todo.isEmpty())
		{
			File	next	= todo.remove(0);
			if(!".jadexbackup".equals(next.getName()))
			{
				FileInfo	fi	= resource.getFileInfo(next);
				
				if(fi.getVTime(getLocalId())>time)
				{
					ret.add(fi);
				}
				
				if(next.isDirectory())
				{
					todo.addAll(Arrays.asList(next.listFiles()));
				}
			}
		}
		return new Future<FileInfo[]>(ret.toArray(new FileInfo[ret.size()]));
	}
	
	//-------- ILocalResourceService interface --------
	
	/**
	 *  Update the local resource with all
	 *  changes from all available remote resource.
	 *  @return All changed files and directories that are being processed.
	 */
	public ITerminableIntermediateFuture<FileData>	updateAll()
	{
		final int[]	finished	= new int[2]; // [search_finished, queued_updates]
		final TerminableIntermediateFuture<FileData>	ret	= new TerminableIntermediateFuture<FileData>();
		component.getServiceContainer().searchServices(IResourceService.class, RequiredServiceInfo.SCOPE_PLATFORM)
			.addResultListener(new IntermediateDefaultResultListener<IResourceService>()
		{
			public void intermediateResultAvailable(IResourceService result)
			{
				// Synchronize with matching remote resources, but exclude self.
				if(!ret.isDone() && result.getResourceId().equals(resource.getResourceId())
					&& !result.getLocalId().equals(resource.getLocalId()))
				{
					finished[1]++;
					update(result).addResultListener(new IntermediateDefaultResultListener<FileData>()
					{
						public void intermediateResultAvailable(FileData file)
						{
							ret.addIntermediateResultIfUndone(file);
						}
						
						public void finished()
						{
							finished[1]--;
							checkFinished();
						}
		
						public void exceptionOccurred(Exception exception)
						{
							finished[1]--;
							checkFinished();
						}
					});
				}
			}
			
			public void finished()
			{
				finished[0]++;
				checkFinished();
			}

			public void exceptionOccurred(Exception exception)
			{
				finished[0]++;
				checkFinished();
			}
			
			protected void	checkFinished()
			{
				if(finished[0]>0 && finished[1]==0)
				{
					ret.setFinishedIfUndone();
				}
			}
		});
		return ret;
	}

	/**
	 *  Update the local resource with all
	 *  changes from the given remote resource.
	 *  @param remote	The remote resource to synchronize to.
	 *  @return All changed files and directories that are being processed.
	 */
	public ITerminableIntermediateFuture<FileData>	update(final IResourceService remote)
	{
		final TerminableIntermediateFuture<FileData>	ret;
		if(updates!=null && updates.containsKey(remote))
		{
			ret	= updates.get(remote);
		}
		else
		{
			ret	= new TerminableIntermediateFuture<FileData>();
			if(updates==null)
			{
				updates	= new LinkedHashMap<IResourceService, TerminableIntermediateFuture<FileData>>();
			}
			updates.put(remote, ret);
			
			if(updates.size()==1)
			{
				startNextUpdate();
			}
		}
		
		return ret;
	}
	
	/**
	 *  Start a queued update.
	 */
	protected void	startNextUpdate()
	{
		if(updates!=null && !updates.isEmpty())
		{
			final IResourceService	remote	= updates.keySet().iterator().next();
			final TerminableIntermediateFuture<FileData>	ret	= updates.get(remote);
			
			remote.getChanges(resource.getVTime(remote.getLocalId()))
				.addResultListener(new IResultListener<FileInfo[]>()
			{
				public void resultAvailable(FileInfo[] result)
				{
					int	maxtime	= -1;
					for(FileInfo fi: result)
					{
						ret.addIntermediateResultIfUndone(new FileData(resource.getFile(fi)));
						maxtime	= Math.max(maxtime, fi.getVTime(remote.getLocalId()));
						resource.updateFileInfo(fi);
					}
					
					if(maxtime!=-1)
					{
						resource.setVTime(remote.getLocalId(), maxtime);
					}
					
					ret.setFinishedIfUndone();
					updates.remove(remote);
					startNextUpdate();
				}
				
				public void exceptionOccurred(Exception exception)
				{
					ret.setExceptionIfUndone(exception);
					updates.remove(remote);
					startNextUpdate();
				}
			});
		}
		else
		{
			updates	= null;
		}
	}
}
