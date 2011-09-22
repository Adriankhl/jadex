package jadex.micro.testcases.semiautomatic;

import jadex.base.gui.CMSUpdateHandler;
import jadex.base.gui.ComponentSelectorDialog;
import jadex.bridge.IComponentIdentifier;
import jadex.bridge.IComponentManagementService;
import jadex.bridge.IComponentStep;
import jadex.bridge.IExternalAccess;
import jadex.bridge.IInternalAccess;
import jadex.bridge.service.SServiceProvider;
import jadex.commons.future.DefaultResultListener;
import jadex.commons.future.IFuture;
import jadex.micro.MicroAgent;
import jadex.xml.annotation.XMLClassname;

import javax.swing.SwingUtilities;

/**
 *  Agent that opens a component selector dialog and then executes a 
 *  step on the selected component.
 */
public class ExternalAccessInvokerAgent extends MicroAgent
{
	/**
	 *  Execute the functional body of the agent.
	 *  Is only called once.
	 */
	public void executeBody()
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				final ComponentSelectorDialog agentselector	= new ComponentSelectorDialog(null, getExternalAccess(), new CMSUpdateHandler(getExternalAccess()));
				final IComponentIdentifier cid = agentselector.selectAgent(null);
				if(cid!=null)
				{
					SServiceProvider.getServiceUpwards(getExternalAccess().getServiceProvider(), IComponentManagementService.class)
						.addResultListener(new DefaultResultListener()
					{
						public void resultAvailable(Object result)
						{
							IComponentManagementService cms = (IComponentManagementService)result;
							cms.getExternalAccess(cid).addResultListener(new DefaultResultListener()
							{
								public void resultAvailable(Object result)
								{
									IExternalAccess ea = (IExternalAccess)result;
									ea.scheduleStep(new IComponentStep<Void>()
									{
										@XMLClassname("exe")
										public IFuture<Void> execute(IInternalAccess ia)
										{
											System.out.println("Executing step on component: "+ia);
											return IFuture.DONE;
										}
									});
								}
							});
						}
					});
				}
			}
		});
	}
}
