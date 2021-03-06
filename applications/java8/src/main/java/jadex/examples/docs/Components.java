package jadex.examples.docs;

import jadex.bridge.IExternalAccess;
import jadex.bridge.service.annotation.OnStart;
import jadex.bridge.service.types.cms.CreationInfo;
import jadex.commons.SUtil;
import jadex.commons.future.Future;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.AgentArgument;
import jadex.micro.annotation.AgentBody;
import jadex.micro.annotation.Argument;
import jadex.micro.annotation.Arguments;

/**
 * 
 */
public class Components 
{
    @Agent
    @Arguments(@Argument(name="myName", description = "Name of this agent", clazz=String.class, defaultvalue = "\"Hugo\""))
    public class MyAgent
    {
        @AgentArgument
        protected String myName;

        public MyAgent() {
            super();
            CreationInfo ci = new CreationInfo(SUtil.createHashMap(new String[]{"myName"}, new Object[]{"\"Harald\""}));
        }

        //@AgentBody 
        @OnStart
        public void body() {
            System.out.println("Hello World");
            System.out.println(myName);
        }
    }

    private void scheduleStep() 
    {
        IExternalAccess extAcc = null;

        extAcc.scheduleStep(iAccess -> {
            // now you are on the component's thread
            return Future.DONE;
        });
    }
}
