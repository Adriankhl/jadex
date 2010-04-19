/**
 * 
 */
package jadex.tools.bpmn.editor.properties;



/**
 * @author Claas Altschaffel
 * 
 */
public class JadexSequencePropertiesSection extends
		AbstractMultiTextfieldPropertySection
{

	// ---- constants ----
	
	public static final String SEQUENCE_PROPERTIES_ANNOTATION_IDENTIFIER = JadexBpmnPropertiesUtil.JADEX_GLOBAL_ANNOTATION;
	public static final String SEQUENCE_PROPERTIES_CONDITION_DETAIL_IDENTIFIER = JadexBpmnPropertiesUtil.JADEX_CONDITION_DETAIL;
	
	private static final String[] textFieldNames = new String[]{SEQUENCE_PROPERTIES_CONDITION_DETAIL_IDENTIFIER};
	
	// ---- attributes ----


	// ---- constructor ----
	
	/**
	 * Default constructor, initializes super class
	 */
	public JadexSequencePropertiesSection()
	{
		super(SEQUENCE_PROPERTIES_ANNOTATION_IDENTIFIER,
				textFieldNames);
	}

	// ---- methods ----

}
