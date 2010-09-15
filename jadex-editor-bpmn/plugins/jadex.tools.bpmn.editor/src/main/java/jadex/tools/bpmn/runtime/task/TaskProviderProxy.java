package jadex.tools.bpmn.runtime.task;

import jadex.tools.bpmn.editor.JadexBpmnEditor;

import org.eclipse.core.runtime.IStatus;

public class TaskProviderProxy implements IEditorTaskProvider
{

	Object provider;
	
	/**
	 * Create a TaskProviderProxy for the given provider not 
	 * implementing the {@link IEditorTaskProvider} interface.
	 * 
	 * @param provider to proxy for
	 */
	protected TaskProviderProxy(Object provider)
	{
		super();
		this.provider = provider;
	}
	
	/* (non-Javadoc)
	 * @see jadex.tools.bpmn.runtime.task.IEditorTaskProvider#dispose()
	 */
	@Override
	public void dispose()
	{
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see jadex.tools.bpmn.runtime.task.IEditorTaskProvider#refresh()
	 */
	@Override
	public void refresh()
	{
		// nothing to refresh
	}

	@Override
	public String[] getAvailableTaskImplementations()
	{
		String[] tasks;

		Object returnValue = WorkspaceClassLoaderHelper
			.callUnparametrizedReflectionMethod(
				provider,
				IEditorTaskProvider.METHOD_IJADEXTASKPROVIDER_GET_AVAILABLE_TASK_IMPLEMENTATIONS);

		// check the return value
		if (returnValue != null && returnValue instanceof String[])
		{
			tasks = (String[]) returnValue;
		}
		else
		{
			tasks = new String[0];
		}
		
		return tasks;
	}

	@Override
	public IEditorTaskMetaInfo getTaskMetaInfo(String fqClassName)
	{
		try
		{
			Object returnValue = WorkspaceClassLoaderHelper
			.callUnparametrizedReflectionMethod(
					provider,
					IEditorTaskProvider.METHOD_IJADEXTASKPROVIDER_GET_TASK_META_INFO);
			
			// check the return value
			if (returnValue instanceof IEditorTaskMetaInfo)
			{
				return (IEditorTaskMetaInfo) returnValue;
			}
			else if (returnValue != null)
			{
				// use reflection proxy
				return new TaskMetaInfoProxy(returnValue);
			}
		}
		catch (Exception e)
		{
			JadexBpmnEditor.log("Exception getting TaskMetaInfo from '"+fqClassName+"' in "+this.getClass().getSimpleName(), e, IStatus.ERROR);
		}
		
		// fall through
		return TaskProviderSupport.NO_TASK_META_INFO_PROVIDED;
	}

}
