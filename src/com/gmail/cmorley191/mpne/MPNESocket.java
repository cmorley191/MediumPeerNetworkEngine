package com.gmail.cmorley191.mpne;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * A peer-to-peer IP implementation - based on {@link ConnectionListener} and
 * {@link PeerConnection}. An {@code MPNESocket} is not a socket itself, but
 * rather manages a socket and the various {@link SocketPeerConnection
 * SocketPeerConnections} implicitly added to it.
 * <p>
 * Create an {@code MPNESocket} whenever you would create a
 * {@link DatagramSocket}.<br>
 * To send data out of the socket, create a {@code SocketPeerConnection} on the
 * socket and use {@link SocketPeerConnection#send(byte[])}.<br>
 * To receive data from the socket, add a {@code ConnectionListener} to the
 * {@code SocketPeerConnection} - upon construction the {@code MPNESocket} is
 * automatically receiving data from the socket and forwarding it to the
 * respective {@code SocketPeerConnections} based on source address and port.
 * 
 * @author Charlie Morley
 *
 */
public final class MPNESocket {

	/**
	 * The socket this {@code MPNESocket} manages incoming and outgoing data
	 * for.
	 */
	private final DatagramSocket socket;

	/**
	 * The set of peers this socket is receiving from and sending to.
	 */
	private final ArrayList<SocketPeerConnection> peers = new ArrayList<SocketPeerConnection>();

	/**
	 * The thread managing incoming data and distributing it to the respective
	 * peers in {@link peers}.
	 */
	private final ReceivingThread receivingThread;

	/**
	 * Constructs a new socket bound to any available port.
	 * 
	 * @throws SocketException
	 */
	public MPNESocket() throws SocketException {
		this(new DatagramSocket());
	}

	/**
	 * Constructs a new socket bound to the specified port.
	 * 
	 * @param port
	 *            the port of this socket
	 * @throws SocketException
	 */
	public MPNESocket(int port) throws SocketException {
		this(new DatagramSocket(port));
	}

	/**
	 * Constructs the {@code MPNESocket} with the specified open socket.
	 * Initializes and runs the {@link #receivingThread}.
	 * 
	 * @param socket
	 */
	private MPNESocket(DatagramSocket socket) {
		this.socket = socket;
		receivingThread = new ReceivingThread();
		receivingThread.start();
	}

	/**
	 * Thread for managing incoming data and distributing it to the respective
	 * peer in {@link MPNESocket#peers}.
	 * 
	 * @author Charlie Morley
	 *
	 */
	private final class ReceivingThread extends Thread {

		/**
		 * Flag for whether this thread is enabled. {@link MPNESocket#close()}
		 * sets this to {@code false} to stop receiving data.
		 */
		private boolean running = true;

		@Override
		public void run() {
			while (running) {
				DatagramPacket receivingPacket = new DatagramPacket(new byte[1000], 1000);
				try {
					socket.receive(receivingPacket);
				} catch (IOException e) {
					if (e.getMessage().equals("socket closed"))
						running = false;
					continue;
				}
				synchronized (peers) {
					for (SocketPeerConnection peer : peers) {
						if (!running)
							break;
						if (peer.getAddress().equals(receivingPacket.getAddress())
								&& peer.getPort() == receivingPacket.getPort())
							new Thread(new Runnable() {

								@Override
								public void run() {
									peer.packetReceived(receivingPacket);
								}
							}).start();
					}
				}
			}
		}
	}

	/**
	 * Closes the socket. Causes any current and further receiving or sending
	 * attempts over the socket to throw an exception. This {@code MPNESocket's}
	 * inner threads are shut down.
	 */
	public void close() {
		receivingThread.running = false;
		socket.close();
	}

	/**
	 * A peer connection over a network socket, specifically a
	 * {@code MPNESocket}. Each {@code SocketPeerConnection} refers to a
	 * specific IP address and port.
	 * <p>
	 * A special implementation of {@code PeerConnection} allows
	 * {@link ConnectionListener ConnectionListeners} to be added to only
	 * specific data "headers" - where the first bytes of a packet are checked
	 * to see if they match the specific header, and only matching packets are
	 * sent to the {@code ConnectionListener}.<br>
	 * The basic functionality of receiving all data that enters from the
	 * connection is still implementable by simply assigning a
	 * {@code ConnectionListener} with an empty, 0 byte data header. See
	 * {@link #addConnectionListener(ConnectionListener, byte[])}.
	 * 
	 * @author Charlie Morley
	 *
	 */
	public final class SocketPeerConnection implements PeerConnection, Comparable<SocketPeerConnection> {

		/**
		 * The reused packet for sending data to this peer.
		 */
		private final DatagramPacket sendPacket;

		/**
		 * The mapping of data headers to the set of {@code ConnectionListeners}
		 * that are listening for those headers from this peer.
		 */
		private final HashMap<byte[], ArrayList<ConnectionListener>> receiveListeners = new HashMap<byte[], ArrayList<ConnectionListener>>();

		/**
		 * Constructs a new peer connection from the socket with the specified
		 * destination address and port. Automatically starts receiving data
		 * from the specified address and port from the socket to this
		 * {@code PeerConnection}.
		 * 
		 * @param address
		 *            the IP address of the peer
		 * @param port
		 *            the port used at {@code address}
		 */
		public SocketPeerConnection(InetAddress address, int port) {
			sendPacket = new DatagramPacket(new byte[0], 0, address, port);
			synchronized (peers) {
				peers.add(this);
			}
		}

		/**
		 * Returns the IP address of this peer.
		 * 
		 * @return the IP address of this peer to which data is sent via
		 *         {@link #send(byte[])} and from which data is received
		 */
		public InetAddress getAddress() {
			return sendPacket.getAddress();
		}

		/**
		 * Returns the port being used at this peer's address.
		 * 
		 * @return the destination port for data sent to this peer via
		 *         {@link #send(byte[])} and source port for data received from
		 *         this peer
		 */
		public int getPort() {
			return sendPacket.getPort();
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * This {@code SocketPeerConnection} sends the data via the socket
		 * associated with its {@code MPNESocket}. If the socket has been closed
		 * via {@link MPNESocket#close()}, an exception will be thrown.
		 */
		@Override
		public synchronized void send(byte[] data) throws IOException {
			sendPacket.setData(data);
			socket.send(sendPacket);
		}

		/**
		 * Called by the {@code MPNESocket} when it receives a packet from this
		 * peers address and port - distributes the data to the appropriate
		 * {@link ConnectionListener ConnectionListeners} added to this peer.
		 * 
		 * @param p
		 *            the packet received from this peer
		 */
		private synchronized void packetReceived(DatagramPacket p) {
			byte[] data = Arrays.copyOf(p.getData(), p.getLength());
			for (byte[] key : receiveListeners.keySet())
				if (Arrays.equals(key, Arrays.copyOf(data, key.length)))
					for (ConnectionListener l : receiveListeners.get(key))
						l.dataReceived(Arrays.copyOf(data, data.length));
		}

		/**
		 * Adds a {@code ConnectionListener} that only listens to data that
		 * starts with the specified set of header bytes. An empty, 0 byte array
		 * for a header will result in the listener receiving all data from this
		 * peer.
		 * <p>
		 * Will not add the listener again if it has already been added to the
		 * specified header.<br>
		 * The same listener can be added to this peer twice if added to two
		 * different headers.
		 * 
		 * @param l
		 *            the listener to be added
		 * @param header
		 *            the set of bytes that filters data to be sent to {@code l}
		 *            - these bytes must appear at the start of any received
		 *            data for the data to be sent to the listener
		 * @see #addConnectionListener(ConnectionListener)
		 * @see #removeConnectionListener(ConnectionListener, byte[])
		 * @see #removeConnectionListener(ConnectionListener)
		 */
		public synchronized void addConnectionListener(ConnectionListener l, byte[] header) {
			for (byte[] key : receiveListeners.keySet())
				if (Arrays.equals(key, header)) {
					ArrayList<ConnectionListener> value = receiveListeners.get(key);
					if (value.contains(l))
						return;
					receiveListeners.get(key).add(l);
					return;
				}
			ArrayList<ConnectionListener> value = new ArrayList<ConnectionListener>();
			receiveListeners.put(header, value);
			value.add(l);
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * Equivalent to {@code addConnectionListener(l, new byte[0])} - an
		 * empty data header means the specified listener will receive every set
		 * of data from this peer.
		 * 
		 * @see #addConnectionListener(ConnectionListener, byte[])
		 * @see #removeConnectionListener(ConnectionListener)
		 * @see #removeConnectionListener(ConnectionListener, byte[])
		 */
		@Override
		public synchronized void addConnectionListener(ConnectionListener l) {
			addConnectionListener(l, new byte[0]);
		}

		/**
		 * Removes the specified listener from the set receiving data with the
		 * specified header from this peer.
		 * <p>
		 * Will not remove the listener from receiving data with different
		 * headers.
		 * 
		 * @param l
		 *            the listener to be added
		 * @param header
		 *            the set of bytes that filters data being sent to {@code l}
		 *            - these bytes appear at the start of any received data
		 *            being sent to the listener
		 * @see #addConnectionListener(ConnectionListener, byte[])
		 * @see #addConnectionListener(ConnectionListener)
		 * @see #removeConnectionListener(ConnectionListener)
		 */
		public synchronized void removeConnectionListener(ConnectionListener l, byte[] header) {
			for (byte[] key : receiveListeners.keySet())
				if (Arrays.equals(key, header)) {
					receiveListeners.get(key).remove(l);
					return;
				}
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * Removes the listener from receiving data with any of its headers -
		 * equivalent to calling {@code removeConnectionListener(l, header)} for
		 * every header the listener was added to.
		 * 
		 * @see #removeConnectionListener(ConnectionListener, byte[])
		 * @see #addConnectionListener(ConnectionListener, byte[])
		 * @see #addConnectionListener(ConnectionListener)
		 */
		@Override
		public synchronized void removeConnectionListener(ConnectionListener l) {
			for (ArrayList<ConnectionListener> value : receiveListeners.values())
				value.remove(l);
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * Compares the IP address of the two peers, big byte to small byte. If
		 * the addresses are identical, compares the ports of the same peers.
		 * <p>
		 * For example, the following peers are in order from least to greatest:
		 * <ul>
		 * <li>10.152.68.24:2992
		 * <li>10.154.33.158:2994
		 * <li>10.154.33.158:4486
		 * <li>160.98.210.159:22047
		 * </ul>
		 * (where the peers are formatted {@code address:port})
		 */
		@Override
		public int compareTo(SocketPeerConnection o) {
			byte[] address = getAddress().getAddress();
			byte[] oAddress = o.getAddress().getAddress();
			for (int i = 0; i < address.length; i++) {
				if (i >= oAddress.length)
					break;
				int diff = address[i] - oAddress[i];
				if (diff != 0)
					return diff;
			}
			int lengthDiff = address.length - oAddress.length;
			if (lengthDiff != 0)
				return lengthDiff;
			return getPort() - o.getPort();
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * This peer is equivalent to other {@code SocketPeerConnections} if
		 * they share the same address and port.
		 */
		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof SocketPeerConnection))
				return false;

			SocketPeerConnection other = (SocketPeerConnection) obj;
			return (other.getAddress().equals(getAddress()) && other.getPort() == getPort());
		}
	}
}
