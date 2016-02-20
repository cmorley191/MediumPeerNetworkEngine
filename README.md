# MediumPeerNetworkEngine

MediumPeerNetworkEngine is a Java peer-to-peer networking utility.
The project has potential support for all networking frameworks via
its two main interfaces - `PeerConnection` allows applications to
form connections to other peers and send data, and `ConnectionListener`
facilitates reacting to data received from other peers.

The project's current framework support is only minimal UDP.
`MPNESocket` and its inner extension, `SocketPeerConnection`,
implement the above interfaces into Java's Datagram I/O.

Planned future development includes connection verification (creating
more security in the insecure UDP framework), peer groups, and more.

## Installation

This directory is an 
[Eclipse Mars IDE](https://projects.eclipse.org/releases/mars) 
Java project. The source files in `src` may be used in any Java
development space without need for any extra procedure. Usage in
Eclipse is best done by using File...Import...

## Usage

To extend into new networking frameworks (such as TCP, Bluetooth,
etc.), implement `PeerConnection` into the framework's basic I/O.

To use the existing UDP framework...
 * Open a datagram socket on either a specified or any available port
  by constructing an `MPNESocket`
 * Open sending and receiving from specific peers (by destination IP
 address and port) by creating new `SocketPeerConnections` on the
 `MPNESocket`
 * Send data to the peer by using `send(byte[])` in 
 `SocketPeerConnection`
 * Receive data from the peer by using `addConnectionListener` in
 `SocketPeerConnection`
	* Data can be filtered by data "header" using the overloaded
	versions of `addConnectionListener`. For example, to only 
	receive data from a peer that starts with "mygame player update",
	specify that header when adding the connection listener to the
	peer connection

## Contributing

Fork the repository and create a pull request to implement new
features.

## History

  * 2016-2-12 Fundamental interfaces
  * 2016-2-12 Minimal UDP framework integration

## Credits

Developed by Charlie Morley

## License

Copyright (c) 2016 Charlie Morley Some Rights Reserved  
CC BY  
cmorley191@gmail.com

This software's binary and source code are released under the 
Creative Commons Attribution license.  
[http://creativecommons.org/licenses/by/4.0/](http://creativecommons.org/licenses/by/4.0/)