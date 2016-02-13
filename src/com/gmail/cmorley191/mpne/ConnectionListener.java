package com.gmail.cmorley191.mpne;

/**
 * The listener interface for receiving data from a networking peer.
 * 
 * @author Charlie Morley
 * @see PeerConnection
 */
public interface ConnectionListener {

	/**
	 * Called when a connection this listener is listening to receives data.
	 * 
	 * @param data
	 *            the data received from the connection
	 */
	public void dataReceived(byte[] data);
}
