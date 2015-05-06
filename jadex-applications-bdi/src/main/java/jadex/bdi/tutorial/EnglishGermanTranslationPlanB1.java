package jadex.bdi.tutorial;

import jadex.bdiv3x.runtime.IMessageEvent;
import jadex.bdiv3x.runtime.Plan;
import jadex.bridge.fipa.SFipa;

import java.util.HashMap;
import java.util.Map;

/**
 *  An initial english german translation plan can translate
 *  english words to german.
 */
public class EnglishGermanTranslationPlanB1 extends Plan
{
	//-------- attributes --------

	/** The wordtable. */
	protected Map<String, String> wordtable;

	//-------- methods --------

	/**
	 *  Create a new plan.
	 */
	public EnglishGermanTranslationPlanB1()
	{
		System.out.println("Created: "+this);

		this.wordtable = new HashMap<String, String>();
		this.wordtable.put("coffee", "Kaffee");
		this.wordtable.put("milk", "Milch");
		this.wordtable.put("cow", "Kuh");
		this.wordtable.put("cat", "Katze");
		this.wordtable.put("dog", "Hund");
	}

	//-------- methods --------

	/**
	 *  The plan body.
	 */
	public void body()
	{
		while(true)
		{
			IMessageEvent me = waitForMessageEvent("request_translation");
//			String eword = (String)me.getContent();
			String eword = (String)me.getParameter(SFipa.CONTENT).getValue();
			String gword = (String)this.wordtable.get(eword);
			if(gword!=null)
			{
				System.out.println("Translating from English to German: "+eword+" - "+gword);
			}
			else
			{
				System.out.println("Sorry word is not in database: "+eword);
			}
		}
	}
}
