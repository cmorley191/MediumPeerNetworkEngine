package com.gmail.cmorley191.mpne;

import java.io.IOException;

/**
 * A connection interface to a specific peer. Offers functionality to send data
 * to the peer and react to data received from the peer.
 * <p>
 * Inclusion of identification information (such as ports and addresses) and any
 * data filtering is at the discretion of implementation (although
 * implementation is encouraged).
 * 
 * @author Charlie Morley
 *
 */
public interface PeerConnection {

	/**
	 * Immediately sends the specified data to this peer.
	 * 
	 * @param data
	 *            the data to send to this peer
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public void send(byte[] data) throws IOException;

	/**
	 * Adds a listener for data received from this peer.
	 * 
	 * @param l
	 *            the listener to add to this peer
	 */
	public void addConnectionListener(ConnectionListener l);

	/**
	 * Removes this data receiving listener from this peer.
	 * 
	 * @param l
	 *            the listener to remove from this peer
	 */
	public void removeConnectionListener(ConnectionListener l);
}
