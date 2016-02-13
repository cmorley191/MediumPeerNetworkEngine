package com.gmail.cmorley191.mpne;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.Scanner;

import com.gmail.cmorley191.mpne.MPNESocket.SocketPeerConnection;

/**
 * Demonstration of MPNE UDP framework ({@link MPNESocket} and
 * {@link MPNESocket.SocketPeerConnection}) in the form of a console
 * peer-to-peer chat application.
 * 
 * @author Charlie Morley
 *
 */
public class MPNEClient {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		MPNESocket socket;
		while (true) {
			System.out.print("Enter local port: ");
			try {
				socket = new MPNESocket(Integer.parseInt(scanner.nextLine().trim()));
				break;
			} catch (Exception e) {
				System.out.println("Invalid port");
			}
		}
		System.out.println("Opened socket.");
		InetAddress address;
		while (true) {
			System.out.print("Enter destination address: ");
			try {
				address = InetAddress.getByName(scanner.nextLine().trim());
				break;
			} catch (UnknownHostException e) {
				System.out.println("Unknown host");
			}
		}
		int port;
		while (true) {
			System.out.print("Enter destination port: ");
			try {
				port = Integer.parseInt(scanner.nextLine().trim());
				break;
			} catch (NumberFormatException e) {
				System.out.println("Invalid port");
			}
		}
		SocketPeerConnection connection = socket.new SocketPeerConnection(address, port);
		System.out.println(
				"Enter to send data. Enter starting with \"/\" for a command. Use \"/help\" for a list of commands.");
		connection.addConnectionListener(new ConnectionListener() {

			@Override
			public void dataReceived(byte[] data) {
				System.out.println("[" + Instant.now().toString() + "] Received: " + new String(data));
			}
		});
		while (true) {
			String line = scanner.nextLine();
			if (line.startsWith("/")) {
				line = line.substring(1);
				if (line.equals("help")) {
					System.out.println("/help - Shows this list");
					System.out.println("/exit - Closes the socket and exits");
				} else if (line.equals("exit"))
					break;
				else
					System.out.println("Unknown command");
			} else {
				try {
					connection.send(line.getBytes());
					System.out.println("[" + Instant.now().toString() + "] Sent:     " + line);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		socket.close();
		scanner.close();
	}
}
