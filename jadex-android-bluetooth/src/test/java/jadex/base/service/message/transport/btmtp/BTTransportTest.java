package jadex.base.service.message.transport.btmtp;

import jadex.android.bluetooth.CustomTestRunner;
import jadex.android.bluetooth.TestConstants;
import jadex.android.bluetooth.device.IBluetoothDevice;
import jadex.android.bluetooth.message.BluetoothMessage;
import jadex.android.bluetooth.service.IBTP2PAwarenessInfoCallback;
import jadex.android.bluetooth.service.IBTP2PMessageCallback;
import jadex.android.bluetooth.service.IConnectionServiceConnection;
import jadex.base.fipa.SFipa;
import jadex.base.service.message.ManagerSendTask;
import jadex.base.service.message.transport.ITransport;
import jadex.base.service.message.transport.MessageEnvelope;
import jadex.base.service.message.transport.codecs.CodecFactory;
import jadex.base.service.message.transport.codecs.GZIPCodec;
import jadex.base.service.message.transport.codecs.ICodec;
import jadex.base.service.message.transport.codecs.JadexXMLCodec;
import jadex.base.service.remote.RemoteServiceContainer;
import jadex.base.service.remote.commands.RemoteSearchCommand;
import jadex.base.service.remote.xml.RMIPreProcessor;
import jadex.bridge.ComponentIdentifier;
import jadex.bridge.IComponentIdentifier;
import jadex.bridge.service.BasicService;
import jadex.bridge.service.IService;
import jadex.bridge.service.IServiceProvider;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.bridge.service.annotation.Service;
import jadex.bridge.service.component.BasicServiceInvocationHandler;
import jadex.bridge.service.search.BasicResultSelector;
import jadex.bridge.service.search.IResultSelector;
import jadex.bridge.service.search.ISearchManager;
import jadex.bridge.service.search.IVisitDecider;
import jadex.bridge.service.search.SServiceProvider;
import jadex.commons.IRemotable;
import jadex.commons.future.IFuture;
import jadex.commons.future.IIntermediateFuture;
import jadex.xml.ObjectInfo;
import jadex.xml.TypeInfo;
import jadex.xml.XMLInfo;
import jadex.xml.bean.BeanObjectWriterHandler;
import jadex.xml.bean.JavaWriter;
import jadex.xml.writer.WriteContext;
import jadex.xml.writer.Writer;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javaxx.xml.namespace.QName;

import junit.framework.TestCase;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.os.RemoteException;

@RunWith(CustomTestRunner.class)
public class BTTransportTest extends TestCase {

	private BTTransport btTransport;
	private MessageEnvelope message;
	private Writer writer;
	private ComponentIdentifier sender;

	@Before
	public void setUp() {
		IServiceProvider sp = createServiceProvider();
		btTransport = new BTTransport(sp);
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("key1", "val1");
		map.put("key2", "val2");

		sender = new ComponentIdentifier("senderComponent",
				new String[] { "bt-mtp://" + TestConstants.adapterAddress2 });

		ArrayList<IComponentIdentifier> receivers = new ArrayList<IComponentIdentifier>();

		receivers.add(new ComponentIdentifier("component1",
				new String[] { "bt-mtp://" + TestConstants.sampleAddress }));
		receivers.add(new ComponentIdentifier("component2",
				new String[] { "bt-mtp://"
						+ TestConstants.defaultAdapterAddress }));

		message = new MessageEnvelope(map, receivers,
				SFipa.FIPA_MESSAGE_TYPE.getName());

		btTransport.codecfac = new CodecFactory();
		btTransport.classLoader = BTTransport.class.getClassLoader();

		Set typeinfoswrite = JavaWriter.getTypeInfos();
		// final RMIPreProcessor preproc = new RMIPreProcessor(rrm);
		// TypeInfo ti_proxyable = new TypeInfo(new XMLInfo(pr, null, false,
		// preproc),
		// new ObjectInfo(IRemotable.class));
		// typeinfoswrite.add(ti_proxyable);

		this.writer = new Writer(new BeanObjectWriterHandler(typeinfoswrite,
				true)) {
			public void writeObject(WriteContext wc, Object object, QName tag)
					throws Exception {

				if (object != null && !(object instanceof BasicService)
						&& object.getClass().isAnnotationPresent(Service.class)) {
					{
						object = BasicServiceInvocationHandler
								.getPojoServiceProxy(object);
					}
				}

				super.writeObject(wc, object, tag);
			};
		};
	}

	@Test
	public void testEncodeDecode() {
		// byte[] encoded = btTransport.encodeMessage(message, new byte[] {
		// JadexXMLCodec.CODEC_ID, GZIPCodec.CODEC_ID });
		// MessageEnvelope message2 = btTransport.decodeMessage(encoded);
		//
		// String typeName = message2.getTypeName();
		// assertEquals(SFipa.FIPA_MESSAGE_TYPE.getName(), typeName);
		//
		// Map map = message2.getMessage();
		// assertEquals("val1", map.get("key1"));
		// assertEquals("val2", map.get("key2"));
		//
		// assertTrue(Arrays.equals(message.getReceivers(),
		// message2.getReceivers()));
	}

	@Test
	public void testEncodeDecodeComplex() {

//		ISearchManager sm = SServiceProvider.getSearchManager(false,
//				RequiredServiceInfo.SCOPE_GLOBAL);
//		IVisitDecider vd = SServiceProvider.getVisitDecider(false,
//				RequiredServiceInfo.SCOPE_GLOBAL);
//		BasicResultSelector rs = new BasicResultSelector();
//
//		RemoteSearchCommand rsc = new RemoteSearchCommand("rms@anywhere", sm,
//				vd, rs, "1");
//
//		String cont = Writer.objectToXML(writer, rsc, getClass()
//				.getClassLoader(), new ComponentIdentifier("testcomponent"));
//
//		HashMap<String, Object> msg = new HashMap<String, Object>();
//		msg.put(SFipa.CONTENT, cont);
//
//		for (int i = 0; i < 10; i++) {
//			msg.put("" + i, cont);
//		}
//
//		msg.put(SFipa.SENDER, sender);
//		msg.put(SFipa.RECEIVERS, message.getReceivers());
//
//		message.setMessage(msg);
//		// byte[] xmlencoded = btTransport.encodeMessage(message,
//		// new byte[] { JadexXMLCodec.CODEC_ID });
//		//
//		// System.out.println(new String(xmlencoded));
//
//		byte[] encoded = btTransport.encodeMessage(message, null);
//		MessageEnvelope message2 = btTransport.decodeMessage(encoded);
//
//		String typeName = message2.getTypeName();
//		assertEquals(SFipa.FIPA_MESSAGE_TYPE.getName(), typeName);
//
//		Map<String, Object> receivedMessage = message2.getMessage();
//
//		assertMessagesEquals(msg, receivedMessage);
//
//		// Map map = message2.getMessage();
//		// assertEquals("val1", map.get("key1"));
//		// assertEquals("val2", map.get("key2"));
//		//
//		assertTrue(Arrays.equals(message.getReceivers(),
//				message2.getReceivers()));
	}

	private void assertMessagesEquals(HashMap<String, Object> msg,
			Map<String, Object> receivedMessage) {
		Set<Entry<String, Object>> entrySet = receivedMessage.entrySet();
		for (Entry<String, Object> entry : entrySet) {
			Object value = entry.getValue();
			String key = entry.getKey();
			if (value instanceof IComponentIdentifier) {
				IComponentIdentifier id = (IComponentIdentifier) value;
				IComponentIdentifier expectedId = (IComponentIdentifier) msg
						.get(key);
				assertEquals(expectedId.getName(), id.getName());
				assertTrue(Arrays.equals(expectedId.getAddresses(),
						id.getAddresses()));
				assertEquals(expectedId.getPlatformName(), id.getPlatformName());
			} else if (value instanceof IComponentIdentifier[]) {
				IComponentIdentifier[] ids = (IComponentIdentifier[]) value;
				IComponentIdentifier[] expectedIds = (IComponentIdentifier[]) msg
						.get(key);

				for (int i = 0; i < ids.length; i++) {
					assertEquals(expectedIds[i].getName(), ids[i].getName());
					assertTrue(Arrays.equals(expectedIds[i].getAddresses(),
							ids[i].getAddresses()));
					assertEquals(expectedIds[i].getPlatformName(),
							ids[i].getPlatformName());
				}
			} else {
				assertEquals(msg.get(key), value);
			}
		}
	}

	@Test
	public void testSendMessage() {
		ISearchManager sm = SServiceProvider.getSearchManager(false,
				RequiredServiceInfo.SCOPE_GLOBAL);
		IVisitDecider vd = SServiceProvider.getVisitDecider(false,
				RequiredServiceInfo.SCOPE_GLOBAL);
		BasicResultSelector rs = new BasicResultSelector();

		RemoteSearchCommand rsc = new RemoteSearchCommand("rms@anywhere", sm,
				vd, rs, "1");

		String cont = Writer.objectToXML(writer, rsc, getClass()
				.getClassLoader(), new ComponentIdentifier("testcomponent"));

		HashMap<String, Object> msg = new HashMap<String, Object>();
		msg.put(SFipa.SENDER, sender);
		msg.put(SFipa.CONTENT, cont);
		msg.put(SFipa.RECEIVERS, message.getReceivers());

		ArrayList<BluetoothMessage> receivedMsgs = new ArrayList<BluetoothMessage>();

		btTransport.binder = createDummyBinder(receivedMsgs);

		ManagerSendTask sendTask = new ManagerSendTask(msg,
				SFipa.FIPA_MESSAGE_TYPE, message.getReceivers(),
				new ITransport[] { btTransport },
				new byte[] { JadexXMLCodec.CODEC_ID },
				new ICodec[] { btTransport.codecfac
						.getCodec(JadexXMLCodec.CODEC_ID) });

		btTransport.sendMessage(sendTask);

		// btTransport.sendMessage(msg, SFipa.FIPA_MESSAGE_TYPE.getName(),
		// message.getReceivers(), new byte[] { JadexXMLCodec.CODEC_ID });

		assertFalse(receivedMsgs.isEmpty());
		BluetoothMessage recMsg = receivedMsgs.get(0);
		byte[] rawEncoded = recMsg.getData();

		// System.out.println(new String(rawEncoded));

		MessageEnvelope decodeMessage = btTransport.decodeMessage(rawEncoded);

		IComponentIdentifier[] receivers = decodeMessage.getReceivers();
		assertTrue(Arrays.equals(message.getReceivers(), receivers));

		Map recMap = decodeMessage.getMessage();

		assertMessagesEquals(msg, recMap);
	}

	private IConnectionServiceConnection createDummyBinder(
			final List<BluetoothMessage> receivedMsgsList) {
		return new IConnectionServiceConnection.Stub() {

			@Override
			public void stopBTServer() throws RemoteException {
			}

			@Override
			public void stopAutoConnect() throws RemoteException {
			}

			@Override
			public void startBTServer() throws RemoteException {
			}

			@Override
			public void startAutoConnect() throws RemoteException {
			}

			@Override
			public void sendMessage(BluetoothMessage msg)
					throws RemoteException {
				receivedMsgsList.add(msg);
			}

			@Override
			public void scanEnvironment() throws RemoteException {
			}

			@Override
			public void registerMessageCallback(IBTP2PMessageCallback callback)
					throws RemoteException {
			}

			@Override
			public void registerAwarenessInfoCallback(
					IBTP2PAwarenessInfoCallback callback)
					throws RemoteException {
			}

			@Override
			public IBluetoothDevice[] getUnbondedDevicesInRange()
					throws RemoteException {
				return null;
			}

			@Override
			public IBluetoothDevice[] getReachableDevices()
					throws RemoteException {
				return null;
			}

			@Override
			public IBluetoothDevice[] getConnectedDevices()
					throws RemoteException {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public IBluetoothDevice[] getBondedDevicesInRange()
					throws RemoteException {
				return null;
			}

			@Override
			public String getBTAddress() throws RemoteException {
				return null;
			}

			@Override
			public void disconnectDevice(IBluetoothDevice dev)
					throws RemoteException {
			}

			@Override
			public void connectToDevice(IBluetoothDevice dev)
					throws RemoteException {
			}
		};
	}

	private IServiceProvider createServiceProvider() {
		return new IServiceProvider() {

			@Override
			public String getType() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public IIntermediateFuture<IService> getServices(
					ISearchManager manager, IVisitDecider decider,
					IResultSelector selector) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public IFuture<IServiceProvider> getParent() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public IComponentIdentifier getId() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public IFuture<Collection<IServiceProvider>> getChildren() {
				// TODO Auto-generated method stub
				return null;
			}
		};
	}
}
