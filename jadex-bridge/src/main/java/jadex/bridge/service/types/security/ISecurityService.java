package jadex.bridge.service.types.security;

import java.util.Map;

import jadex.bridge.IComponentIdentifier;
import jadex.bridge.service.annotation.GuiClassName;
import jadex.bridge.service.annotation.GuiClassNames;
import jadex.bridge.service.annotation.Reference;
import jadex.commons.Tuple2;
import jadex.commons.future.IFuture;

/**
 *  The security service is responsible for
 *  validating (remote) requests.
 *  Currently only platform level authentication
 *  is provided. More fine grained control on
 *  service/method level based on user/group
 *  access rights is planned for the mid-term future.
 */
//Safe to be allowed remotely, as it can only be called, when platform access is granted.
//Putting method in service allows security settings to be administered using remote JCCs.
@GuiClassNames({
	@GuiClassName("jadex.tools.security.SecuritySettingsPanel"),
	@GuiClassName("jadex.android.controlcenter.settings.SecuritySettings")
})
public interface ISecurityService
{
	//-------- message-level encryption/authentication -------
	
	/**
	 *  Encrypts and signs the message for a receiver.
	 *  
	 *  @param receiver The receiver.
	 *  @param content The content
	 *  @return Encrypted/signed message.
	 */
	public IFuture<byte[]> encryptAndSign(@Reference Map<String, Object> header, @Reference byte[] content);
	
	/**
	 *  Decrypt and authenticates the message from a sender.
	 *  
	 *  @param sender The sender.
	 *  @param content The content.
	 *  @return Decrypted/authenticated message or null on invalid message.
	 */
	public IFuture<Tuple2<IMsgSecurityInfos,byte[]>> decryptAndAuth(@Reference IComponentIdentifier sender, @Reference byte[] content);
}
