package jadex.micro.examples.ws;

import jadex.base.gui.SwingDefaultResultListener;
import jadex.bridge.IComponentStep;
import jadex.bridge.IExternalAccess;
import jadex.bridge.IInternalAccess;
import jadex.commons.SUtil;
import jadex.commons.future.ExceptionDelegationResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.commons.gui.SGUI;
import jadex.micro.MicroAgent;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.AgentCreated;
import jadex.micro.annotation.RequiredService;
import jadex.micro.annotation.RequiredServices;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 *  Agent that lets a user interact with the web service offering a user interface. 
 */
@Agent
@RequiredServices(@RequiredService(name="quoteservice", type=IQuoteService.class))
public class UserAgent
{
	//-------- attributes --------
	
	/** The agent. */
	@Agent
	protected MicroAgent agent;
	
	/** The text field for the symbol. */
	protected JTextField tfsymbol;
	
	/** The text field for the result. */
	protected JTextField tfresult;

	//-------- methods --------
	
	/**
	 *  Called when agent is born.
	 */
	@AgentCreated
	public void agentCreated()
	{
		JFrame f = new JFrame();
		f.setLayout(new BorderLayout());
		JPanel p = new JPanel(new FlowLayout());
		tfsymbol = new JTextField(10);
		tfsymbol.setText("Google");
		JButton b = new JButton("get");
		tfresult = new JTextField(10);
		p.add(tfsymbol);
		p.add(b);
		p.add(tfresult);
		f.add(p, BorderLayout.CENTER);
		f.pack();
		f.setLocation(SGUI.calculateMiddlePosition(f));
		f.setVisible(true);

		final IExternalAccess exta = agent.getExternalAccess();
		
		b.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				final String stock = tfsymbol.getText();
//				System.out.println("stock is: "+stock);
				
				exta.scheduleStep(new IComponentStep<Object>()
				{
					public IFuture<Object> execute(IInternalAccess ia)
					{
						final Future<Object> ret = new Future<Object>();
						IFuture<IQuoteService> fut = ia.getServiceContainer().getRequiredService("quoteservice");
						fut.addResultListener(new ExceptionDelegationResultListener<IQuoteService, Object>(ret)
						{
							public void customResultAvailable(IQuoteService qs)
							{
								qs.getQuote(stock)
									.addResultListener(new ExceptionDelegationResultListener<String, Object>(ret)
								{
									public void customResultAvailable(String result) 
									{
										ret.setResult(result);
//										System.out.println("quote: "+result);
									};
								});
							}
						});
						return ret;
					}
				}).addResultListener(new SwingDefaultResultListener<Object>()
				{
					public void customResultAvailable(Object result)
					{
						tfresult.setText(""+result);
					}
				});
			}
		});
	}
}
