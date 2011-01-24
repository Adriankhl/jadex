package jadex.xml.tutorial.example10;

import jadex.commons.SUtil;
import jadex.xml.AccessInfo;
import jadex.xml.LinkingInfo;
import jadex.xml.MappingInfo;
import jadex.xml.ObjectInfo;
import jadex.xml.SubobjectInfo;
import jadex.xml.TypeInfo;
import jadex.xml.XMLInfo;
import jadex.xml.bean.BeanObjectReaderHandler;
import jadex.xml.bean.BeanObjectWriterHandler;
import jadex.xml.bean.JavaReader;
import jadex.xml.bean.JavaWriter;
import jadex.xml.reader.Reader;
import jadex.xml.tutorial.example09.Invoice;
import jadex.xml.tutorial.example09.InvoiceList;
import jadex.xml.writer.Writer;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

/**
 *  Main class to execute tutorial lesson.
 */
public class Main
{
	/**
	 *  Main method for using the xml reader/writer.
	 */
	public static void main(String[] args) throws Exception
	{
		// Using collections. Here a number of invoice items are stored in a list.
		// Here it is shown that also set methods can be used when working with
		// collections. This requires using the bulk mode, which first assembles
		// all elements and then sets them alltogether.
		
		// Create minimal type infos for types that need to be mapped
		Set typeinfos = new HashSet();
		typeinfos.add(new TypeInfo(new XMLInfo("invoice"), new ObjectInfo(InvoiceList.class), null, new
			LinkingInfo(true)));
		typeinfos.add(new TypeInfo(new XMLInfo("item"), new ObjectInfo(Invoice.class),
			new MappingInfo(null, new SubobjectInfo[]{
			new SubobjectInfo(new AccessInfo("product-key", "key"))		
			})));
		
		// Create an xml reader with standard bean object reader and the
		// custom typeinfos
		Reader xmlreader = new Reader(new BeanObjectReaderHandler(JavaReader.joinTypeInfos(typeinfos)));
		InputStream is = SUtil.getResource("jadex/xml/tutorial/example10/data.xml", null);
		Object object = xmlreader.read(is, null, null);
		is.close();

		typeinfos = new HashSet();
		typeinfos.add(new TypeInfo(new XMLInfo("invoice"), new ObjectInfo(InvoiceList.class), 
			new MappingInfo(null, new SubobjectInfo[]{
			new SubobjectInfo(new AccessInfo("item", "items"), null, true)
			})));
		typeinfos.add(new TypeInfo(new XMLInfo("item"), new ObjectInfo(Invoice.class),
			new MappingInfo(null, new SubobjectInfo[]{
			new SubobjectInfo(new AccessInfo("product-key", "key"))		
			})));
		
		// Write the xml to the output file.
		Writer xmlwriter = new Writer(new BeanObjectWriterHandler(JavaWriter.joinTypeInfos(typeinfos), false, true), false);
		String xml = Writer.objectToXML(xmlwriter, object, null);
//		OutputStream os = new FileOutputStream("out.xml");
//		xmlwriter.write(object, os, null, null);
//		os.close();
		
		// And print out the result.
		System.out.println("Read object: "+object);
		System.out.println("Wrote xml: "+xml);
	}
}
