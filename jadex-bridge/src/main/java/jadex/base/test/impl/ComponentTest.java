package jadex.base.test.impl;


import jadex.base.Starter;
import jadex.base.test.ComponentTestSuite;
import jadex.base.test.TestReport;
import jadex.base.test.Testcase;
import jadex.bridge.IComponentIdentifier;
import jadex.bridge.IResourceIdentifier;
import jadex.bridge.modelinfo.IModelInfo;
import jadex.bridge.service.BasicService;
import jadex.bridge.service.IService;
import jadex.bridge.service.types.cms.CreationInfo;
import jadex.bridge.service.types.cms.IComponentManagementService;
import jadex.commons.concurrent.TimeoutException;
import jadex.commons.future.Future;
import jadex.commons.future.ITuple2Future;
import jadex.commons.future.IntermediateDefaultResultListener;
import jadex.commons.future.TupleResult;

import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

/**
 *  Test a component.
 */
public class ComponentTest extends TestCase
{
	//-------- attributes --------
	
	/** The component management system. */
	protected IComponentManagementService	cms;
	
	/** The component model. */
	protected String	filename;
	
	/** The component resource identifier. */
	protected IResourceIdentifier	rid;
	
	/** The component full name. */
	protected String	fullname;
	
	/** The component (kernel) type. */
	protected String	type;
	
	/** The timeout. */
	protected long	timeout;
	
	/** The test suite. */
	protected ComponentTestSuite	suite;
	
	//-------- constructors --------
	
	public ComponentTest() {
		Logger.getLogger("ComponentTest").log(Level.SEVERE, "Empty ComponentTest Constructor called");
	}
	
	/**
	 *  Create a component test.
	 */
	public ComponentTest(IComponentManagementService cms, IModelInfo comp, ComponentTestSuite suite)
	{
		this.cms	= cms;
		this.filename	= comp.getFilename();
		this.rid	= comp.getResourceIdentifier();
		this.fullname	= comp.getFullName();
		this.type	= comp.getType();
		Object	to	= comp.getProperty(Testcase.PROPERTY_TEST_TIMEOUT, suite.getClassLoader());
		if(to!=null)
		{
			this.timeout	= ((Number)to).longValue();
		}
		else
		{
			this.timeout	= Starter.getLocalDefaultTimeout(((IService)cms).getServiceIdentifier().getProviderId());
		}
		this.suite	= suite;
	}
	
	//-------- methods --------
	
	/**
	 *  The number of test cases.
	 */
	public int countTestCases()
	{
		return 1;
	}
	
	/**
	 *  Test the component.
	 */
	public void runBare()
	{
		if(suite.isAborted())
		{
			return;
		}
		
		// Start the component.
		final IComponentIdentifier[]	cid	= new IComponentIdentifier[1];
		final Future<Map<String, Object>>	finished	= new Future<Map<String,Object>>();
		Timer	t	= new Timer(true);
		t.schedule(new TimerTask()
		{
			public void run()
			{
				boolean	b	= finished.setExceptionIfUndone(new TimeoutException(ComponentTest.this+" did not finish in "+timeout+" ms."));
				if(b && cid[0]!=null)
				{
					cms.destroyComponent(cid[0]);
				}
			}
		}, timeout);

		ITuple2Future<IComponentIdentifier, Map<String, Object>>	fut	= cms.createComponent(null, filename, new CreationInfo(rid));
		componentStarted(fut);
		fut.addResultListener(new IntermediateDefaultResultListener<TupleResult>()
		{
			public void intermediateResultAvailable(TupleResult result)
			{
				if(result.getNum()==0)
				{
					cid[0]	= (IComponentIdentifier)result.getResult();
				}
				else
				{
					finished.setResultIfUndone((Map<String, Object>)result.getResult());
				}
			}
			
			public void exceptionOccurred(Exception exception)
			{
				finished.setExceptionIfUndone(exception);
			}
		});
		Map<String, Object>	res	= finished.get();
		t.cancel();
		checkTestResults(res);
		
		// Remove references to Jadex resources to aid GC cleanup.
		cms	= null;
		suite	= null;
	}

	/**
	 *  Called when a component has been started.
	 *  @param cid	The cid, set as soon as known.
	 */
	protected void componentStarted(ITuple2Future<IComponentIdentifier, Map<String, Object>> fut)
	{
	}

	/**
	 *  Optional checking after component has finished.
	 *  @param res	The results.
	 */
	protected void checkTestResults(Map<String, Object> res)
	{
		// Evaluate the results.
		Testcase	tc	= null;
		for(Iterator<Map.Entry<String, Object>> it=res.entrySet().iterator(); it.hasNext(); )
		{
			Map.Entry<String, Object> tup = it.next();
			if(tup.getKey().equals("testresults"))
			{
				tc = (Testcase)tup.getValue();
				break;
			}
		}
		
		if(tc!=null && tc.getReports()!=null)
		{
			TestReport[]	reports	= tc.getReports();
			if(tc.getTestCount()!=reports.length)
			{
				throw new AssertionFailedError("Number of testcases do not match. Expected "+tc.getTestCount()+" but was "+reports.length+".");			
			}
			for(int i=0; i<reports.length; i++)
			{
				if(!reports[i].isSucceeded())
				{
					throw new AssertionFailedError(reports[i].getDescription()+" Failed with reason: "+reports[i].getReason());
				}
			}
		}
		else
		{
			throw new AssertionFailedError("No test results provided by component: "+res);
		}
	}

	public String getName()
	{
		return this.toString();
	}
	
	
	/**
	 *  Get a string representation of this test.
	 */
	public String toString()
	{
		return fullname + " (" + type + ")";
	}
}
