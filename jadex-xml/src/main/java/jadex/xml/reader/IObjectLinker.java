package jadex.xml.reader;

import javax.xml.namespace.QName;

/**
 *  Interface for sequential linker. 
 */
public interface IObjectLinker
{
	/**
	 *  Link an object to its parent.
	 *  @param object The object.
	 *  @param parent The parent object.
	 *  @param linkinfo The link info.
	 *  @param tagname The current tagname (for name guessing).
	 *  @param context The context.
	 */
	public void linkObject(Object object, Object parent, Object linkinfo, 
		QName[] pathname, ReadContext context) throws Exception;
}
