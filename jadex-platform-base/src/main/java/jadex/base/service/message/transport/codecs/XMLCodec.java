package jadex.base.service.message.transport.codecs;

/* $if !android $ */
import java.beans.ExceptionListener;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
/* $else $
import javaa.beans.ExceptionListener;
import javaa.beans.XMLDecoder;
import javaa.beans.XMLEncoder;

$endif $ */
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 *  The XML codec.
 *  Codec supports parallel calls of multiple concurrent 
 *  clients (no method synchronization necessary).
 *  
 *  Converts object -> byte[] and byte[] -> object.
 */
public class XMLCodec implements ICodec
{
	//-------- constants --------
	
	/** The xml codec id. */
	public static final byte CODEC_ID = 3;

	//-------- methods --------
	
	/**
	 *  Encode an object.
	 *  @param obj The object.
	 *  @throws IOException
	 */
//	public byte[] encode(Object val, ClassLoader classloader)
	public Object encode(Object val, ClassLoader classloader)
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    XMLEncoder enc = new XMLEncoder(baos);
		enc.setExceptionListener(new ExceptionListener()
		{
			public void exceptionThrown(Exception e)
			{
				System.out.println("XML encoding ERROR: ");
				e.printStackTrace();
			}
		});
	    enc.writeObject(val);
	    enc.close();
	    try{baos.close();} catch(Exception e) {}
	    return baos.toByteArray();
	}

	/**
	 *  Decode an object.
	 *  @return The decoded object.
	 *  @throws IOException
	 */
//	public Object decode(byte[] bytes, ClassLoader classloader)
	public Object decode(Object bytes, ClassLoader classloader)
	{
		final ByteArrayInputStream bais = new ByteArrayInputStream((byte[])bytes);
		
		XMLDecoder dec = new XMLDecoder(bais, null, new ExceptionListener()
		{
			public void exceptionThrown(Exception e)
			{
				System.out.println("XML decoding ERROR: "+bais);
				e.printStackTrace();
			}
		}, classloader);
		
		Object ret = dec.readObject();
		dec.close();
		try{bais.close();} catch(Exception e) {}
		return ret;
	}
}
