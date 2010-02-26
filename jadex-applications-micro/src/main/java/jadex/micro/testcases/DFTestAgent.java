package jadex.micro.testcases;

import jadex.adapter.base.fipa.IDF;
import jadex.adapter.base.fipa.IDFComponentDescription;
import jadex.adapter.base.fipa.IDFServiceDescription;
import jadex.adapter.base.fipa.SFipa;
import jadex.adapter.base.test.TestReport;
import jadex.adapter.base.test.Testcase;
import jadex.bridge.Argument;
import jadex.bridge.IArgument;
import jadex.bridge.IComponentIdentifier;
import jadex.bridge.ISearchConstraints;
import jadex.bridge.MessageType;
import jadex.commons.concurrent.IResultListener;
import jadex.micro.MicroAgent;
import jadex.micro.MicroAgentMetaInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *  Test DF usage from micro agent.
 *  @author Dirk, Alex
 */
public class DFTestAgent extends MicroAgent
{
	/** The reports of executed tests, used as result. */
	protected List	reports;
	
	/**
	 *  At startup register the agent at the DF.
	 */
	public void executeBody()
	{
		this.reports	= new ArrayList();
		registerDF();
	}
	
	/**
	 *  Called when agent finishes.
	 */
	public void agentKilled()
	{
		// Store test results.
		setResultValue("testresults", new Testcase(reports.size(), (TestReport[])reports.toArray(new TestReport[reports.size()])));

		// Deregister agent.
		IDF df = (IDF)getServiceContainer().getService(IDF.class);
		IDFComponentDescription ad = df.createDFComponentDescription(getComponentIdentifier(), null);
		df.deregister(ad, null);
	}
	
	/**
	 *  Register the agent at the DF.
	 */
	protected void registerDF()
	{
		final TestReport	tr	= new TestReport("#1", "Test DF registration.");
		reports.add(tr);

		IDF df = (IDF)getServiceContainer().getService(IDF.class);
		IDFServiceDescription sd = df.createDFServiceDescription(null, "testType", null);
		IDFComponentDescription ad = df.createDFComponentDescription(getComponentIdentifier(), sd);

		df.register(ad, createResultListener(new IResultListener()
		{
			public void resultAvailable(Object source, Object result)
			{
				// Set test success and continue test.
				tr.setSucceeded(true);
				searchDF();
			}
			
			public void exceptionOccurred(Object source, Exception e)
			{
				// Set test failure and kill agent.
				tr.setFailed(e.toString());
				killAgent();
			}
		}));
	}
	
	/**
	 *  Search for the agent at the DF.
	 */
	protected  void searchDF()
	{
		final TestReport	tr	= new TestReport("#2", "Test DF search.");
		reports.add(tr);

		// Create a service description to search for.
		IDF df = (IDF)getServiceContainer().getService(IDF.class);
		IDFServiceDescription sd = df.createDFServiceDescription(null, "testType", null);
		IDFComponentDescription ad = df.createDFComponentDescription(null, sd);
		ISearchConstraints	cons = df.createSearchConstraints(-1, 0);
		
		df.search(ad, cons, createResultListener(new IResultListener() {
			
			public void resultAvailable(Object sourcem, Object result)
			{
				IDFComponentDescription[] agentDesc = (IDFComponentDescription[])result;
				if(agentDesc.length != 0)
				{
					// Set test success and continue test.
					tr.setSucceeded(true);
					IComponentIdentifier receiver = agentDesc[0].getName();
					sendMessageToReceiver(receiver);
				}
				else
				{
					// Set test failure and kill agent.
					tr.setFailed("No suitable service found.");
					killAgent();
				}
			}
			
			public void exceptionOccurred(Object source, Exception e)
			{
				// Set test failure and kill agent.
				tr.setFailed(e.toString());
				killAgent();
			}
		}));
	}
	
	private void sendMessageToReceiver(IComponentIdentifier cid)
	{
		final TestReport	tr	= new TestReport("#3", "Test sending message to service (i.e. myself).");
		reports.add(tr);

		Map hlefMessage = new HashMap();
		hlefMessage.put(SFipa.PERFORMATIVE, SFipa.INFORM);
		hlefMessage.put(SFipa.SENDER, getComponentIdentifier());
		hlefMessage.put(SFipa.RECEIVERS, cid);
		hlefMessage.put(SFipa.CONTENT, "testMessage");
		
		sendMessage(hlefMessage, SFipa.FIPA_MESSAGE_TYPE);
		
		waitFor(1000, new Runnable()
		{
			public void run()
			{
				// Set test failure and kill agent.
				tr.setFailed("No message received.");
				killAgent();
			}
		});
	}
	
	public void messageArrived(Map msg, MessageType mt)
	{
		TestReport	tr	= (TestReport)reports.get(reports.size()-1);
		
		if("testMessage".equals(msg.get(SFipa.CONTENT)))
		{
			tr.setSucceeded(true);
		}
		else
		{
			tr.setFailed("Wrong message received: "+msg);
		}

		// All tests done.
		killAgent();
	}

	
	/**
	 *  Add the 'testresults' marking this agent as a testcase. 
	 */
	public static Object getMetaInfo()
	{
		return new MicroAgentMetaInfo("Test DF usage from micro agent.", 
			null, null, new IArgument[]{new Argument("testresults", null, "Testcase")}, null);
	}
}
