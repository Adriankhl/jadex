package jadex.android.bluetooth.connection;

import jadex.android.bluetooth.device.IBluetoothAdapter;
import jadex.android.bluetooth.device.IBluetoothDevice;
import jadex.android.bluetooth.device.IBluetoothSocket;
import jadex.android.bluetooth.exceptions.MessageConvertException;
import jadex.android.bluetooth.message.DataPacket;
import jadex.android.bluetooth.util.Helper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

import android.util.Log;

public abstract class AConnection implements IConnection {

	protected static final int CONNECTION_TIMEOUT = 2000;

	public static final int MESSAGE_READ = 0;
	protected IBluetoothAdapter adapter;
	protected IBluetoothDevice remoteDevice;

	protected ConnectedThread connectedThread;

	protected List<IConnectionListener> listeners;

	private int lastPacketBytesRead;
	private byte[] lastReceivedPacket;
	private boolean lastPacketComplete;

	public AConnection(IBluetoothAdapter adapter, IBluetoothDevice remoteDevice) {
		this.adapter = adapter;
		this.remoteDevice = remoteDevice;
		listeners = new CopyOnWriteArrayList<IConnectionListener>();
		lastReceivedPacket = new byte[DataPacket.PACKET_SIZE];
		lastPacketBytesRead = 0;
		lastPacketComplete = true;
	}

	protected class ConnectedThread extends Thread {
		private final IBluetoothSocket mmSocket;
		private InputStream mmInStream;
		private OutputStream mmOutStream;
		private writerThread writer;
		private readerThread reader;
		boolean running;

		private BlockingQueue<byte[]> packetQueue;

		public ConnectedThread(IBluetoothSocket socket) {
			mmSocket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;
			running = false;

			packetQueue = new LinkedBlockingQueue<byte[]>();

			// Get the input and output streams, using temp objects because
			// member streams are final
			try {
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			} catch (IOException e) {
			}

			mmInStream = tmpIn;
			mmOutStream = tmpOut;

			reader = new readerThread();
			writer = new writerThread();
		}

		public void run() {
			running = true;
			reader.start();
			writer.start();
		}

		private synchronized void _write(byte[] bytes) throws IOException {
			try {
				mmOutStream.write(bytes);
				mmOutStream.flush();
			} catch (IOException e) {
				Log.e(Helper.LOG_TAG,
						"catched Exception while writing to Stream: "
								+ e.toString() + "\n "
								+ Helper.stackTraceToString(e.getStackTrace()));
				setConnectionAlive(false);
				throw e;
			}
		}

		/* Call this from the main Activity to send data to the remote device */
		public void write(byte[] bytes) {
			if (running) {
				packetQueue.add(bytes);
			}
		}

		public synchronized void cancel() {
			if (running) {
				running = false;
				writer.running = false;
				reader.running = false;
				try {
					// this wakes up the writer thread
					// which will then terminate
					packetQueue.add(new byte[0]);
					mmOutStream.close();
					mmInStream.close();
					mmSocket.close();
				} catch (IOException e) {

				} finally {
					mmOutStream = null;
					mmInStream = null;
					writer = null;
					reader = null;
					packetQueue = null;
				}
			}
		}

		class writerThread extends Thread {
			public boolean running = true;

			@Override
			public void run() {
				while (running) {
					try {
						byte[] take = packetQueue.take();
						_write(take);
					} catch (Exception e) {
						if (running) {
							e.printStackTrace();
						}
					}
				}
			}
		}

		class readerThread extends Thread {

			public boolean running = true;

			@Override
			public void run() {
				setConnectionAlive(true);
				byte[] buffer; // buffer store for the
								// stream

				// hardcoded value because android seems to use this value as
				// internal buffer, too.
				// InputStream.read() doesn't return more than 1008 bytes at
				// once.
				buffer = new byte[1024];

				// Keep listening to the InputStream until an exception
				// occurs

				while (running) {
					try {
						// Read from the InputStream
						List<DataPacket> packets = readPacketFromStream(
								mmInStream, buffer);
						synchronized (ConnectedThread.this) {
							for (DataPacket dataPacket : packets) {
								notifyMessageReceived(dataPacket);
							}
						}
					} catch (IOException e) {
						// maybe remove connection from list here?
						if (running) {
							Log.e(Helper.LOG_TAG,
									"catched IOException while reading from Stream:"
											+ e.toString()
											+ "\n "
											+ Helper.stackTraceToString(e
													.getStackTrace()));
						}
						setConnectionAlive(false);
						break;
					} catch (MessageConvertException e) {
						e.logThisException();
					}
				}
			}
		}

	}

	/**
	 * Reads {@link DataPacket}s from an Input Stream.
	 * 
	 * @param inputStream
	 *            The Input stream to read from.
	 * @param buffer
	 *            The Buffer to use while reading.
	 * @return List of received {@link DataPacket}s.
	 * @throws IOException
	 *             this exception is passed through from InputStream.read().
	 * @throws MessageConvertException
	 *             thrown if data contains invalid {@link DataPacket}.
	 */
	protected synchronized List<DataPacket> readPacketFromStream(
			InputStream inputStream, byte[] buffer) throws IOException,
			MessageConvertException {
		int bytes; // bytes returned from read()
		ArrayList<DataPacket> receivedPackets = new ArrayList<DataPacket>();
		DataPacket dataPacket = null;

		while (dataPacket == null && isAlive()) { // read at least one full
													// packet
			if (inputStream.available() <= 0) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}
				continue;
			}
			bytes = inputStream.read(buffer);
//			Log.d(Helper.LOG_TAG,
//					"(Connection) received a stream chunk of size: " + bytes);
			short totalSize;

			int byteIndexInPacket;
			int byteIndexInBuffer = 0;
			int missingBytes = 0;

			if (lastPacketComplete) {
				totalSize = getTotalPacketSize(buffer, 0, buffer.length);
				byteIndexInPacket = 0;
				lastPacketBytesRead = 0;
				lastPacketComplete = false;
			} else {
				// Resuming packet receiption from earlier read()
				byteIndexInPacket = lastPacketBytesRead;
				totalSize = getTotalPacketSize(lastReceivedPacket, 0,
						byteIndexInPacket + 1);
			}
			
			if (totalSize == -1) {
				// last time, we haven't received the DataSize field.
				// read up to the dataSize field:
				for (; byteIndexInBuffer < bytes
				&& byteIndexInPacket <= DataPacket.INDEX_dataSize_END; 
				byteIndexInBuffer++, byteIndexInPacket++) {
					lastReceivedPacket[byteIndexInPacket] = buffer[byteIndexInBuffer];
				}
				lastPacketBytesRead = byteIndexInPacket;
				totalSize = getTotalPacketSize(lastReceivedPacket, 0, byteIndexInPacket +1);
				if (totalSize == -1) {
//						throw new MessageConvertException(
//						"(AConnection) After two read() calls i still don't know the dataSize of this Packet!");
					continue;
				}
			}

			missingBytes = totalSize - lastPacketBytesRead;
			while (byteIndexInBuffer < bytes) {
				// continue as long as we're not finished interpreting the whole
				// buffer

				// read one Packet, or, the most of the Packet we can get:
				int toRead = missingBytes + byteIndexInBuffer;
				for (; byteIndexInBuffer < toRead
						&& byteIndexInBuffer < bytes; byteIndexInBuffer++, byteIndexInPacket++) {
					lastReceivedPacket[byteIndexInPacket] = buffer[byteIndexInBuffer];
				}
				lastPacketBytesRead = byteIndexInPacket;

				if (lastPacketBytesRead >= totalSize) {
					// read one packet successfully
					dataPacket = new DataPacket(lastReceivedPacket);
//					Log.d(Helper.LOG_TAG,
//							"(Connection) received a DataPacket of Size: "
//									+ totalSize);
					receivedPackets.add(dataPacket);
					if (bytes > missingBytes) {
						// received more than one packet.
						// the while-loop will continue to read the input buffer
						// and
						// build more packets
						byteIndexInPacket = 0;
						// next packet starts at byteIndexInBuffer
						// TODO: this will fail if the first 58 bytes of the
						// next packet are not transmitted yet!!
						totalSize = getTotalPacketSize(buffer,
								byteIndexInBuffer, buffer.length);
						if (totalSize == -1) {
							// just make the loop read the beginning of the
							// packet into cache.
							totalSize = Short.MAX_VALUE;
							missingBytes = bytes;
							lastPacketBytesRead = 0;
							lastPacketComplete = false;
						} else if (totalSize == 0) {
							// PROBLEM
							throw new MessageConvertException(
									"(AConnection) Received a packet that claims a size of zero bytes.");
						} else {
							missingBytes = totalSize;
							lastPacketBytesRead = 0;
							lastPacketComplete = false;
						}
					} else {
						lastPacketComplete = true;
					}
				} else {
					// need more data to complete the packet
					lastPacketComplete = false;
				}
			}
		}
		return receivedPackets;
	}

	/**
	 * Reads the dataSize of the packet stored in rawPacketData and calculates
	 * the total size of the DataPacket.
	 * 
	 * @param rawPacketData
	 *            Array containing the Packet
	 * @param offset
	 *            Offset on which position in the rawPacketData Array the Packet
	 *            begins.
	 * @param bufferLength
	 *            Indicates up to which length the buffer is filled with valid
	 *            Data. (ignores everything beyond that length)
	 * @return totalSize of Packet, including header, or -1 if size could not be
	 *         read.
	 */
	private short getTotalPacketSize(byte[] rawPacketData, int offset,
			int bufferLength) {
		short totalSize;
		if (offset + DataPacket.INDEX_dataSize_END < bufferLength) {
			totalSize = (short) (DataPacket.getDataSizeFromPacketByteArray(
					rawPacketData, offset) + DataPacket.HEADER_SIZE);
		} else {
			totalSize = -1;
		}
		return totalSize;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jadex.android.bluetooth.IConnection#write(byte[])
	 */
	@Override
	public void write(DataPacket pkt) throws IOException {
		if (isAlive()) {
			try {
				connectedThread.write(pkt.asByteArray());
			} catch (MessageConvertException e) {
				e.logThisException();
			}
		} else {
			throw new IOException("Not Connected.");
		}
	}

	private boolean connectionAlive;

	protected void setConnectionAlive(boolean alive) {
		if (connectionAlive != alive) {
			connectionAlive = alive;
			notifyConnectionStateChanged();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jadex.android.bluetooth.IConnection#isAlive()
	 */
	@Override
	public boolean isAlive() {
		return connectionAlive;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jadex.android.bluetooth.IConnection#setConnectionListener(de
	 * .unihamburg.vsis.test.bluetooth.BTConnectionListener)
	 */
	@Override
	public void addConnectionListener(final IConnectionListener l) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				synchronized (listeners) {
					listeners.add(l);
				}
			}
		}).start();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jadex.android.bluetooth.IConnection#setConnectionListener(de
	 * .unihamburg.vsis.test.bluetooth.BTConnectionListener)
	 */
	@Override
	public void removeConnectionListener(final IConnectionListener l) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				synchronized (listeners) {
					listeners.remove(l);
				}
			}
		}).start();
	}

	protected void notifyMessageReceived(DataPacket dataPacket) {
		synchronized (listeners) {
			for (IConnectionListener l : listeners) {
				l.messageReceived(dataPacket, this.remoteDevice, this);
			}
		}
	}

	protected void notifyConnectionStateChanged() {
		synchronized (listeners) {
			for (IConnectionListener l : listeners) {
				l.connectionStateChanged(this);
			}
		}
	}

	/* Call this from the main Activity to shutdown the connection */
	/*
	 * (non-Javadoc)
	 * 
	 * @see jadex.android.bluetooth.IConnection#close()
	 */
	@Override
	public void close() {
		if (connectedThread != null) {
			connectedThread.cancel();
			setConnectionAlive(false);
			connectedThread = null;
		}
	}

	@Override
	public IBluetoothDevice getRemoteDevice() {
		return remoteDevice;
	}
}