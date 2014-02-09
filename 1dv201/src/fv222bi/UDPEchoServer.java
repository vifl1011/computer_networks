package fv222bi;
/*
  UDPEchoServer.java
  A simple echo server with no error handling
 */

//package dv201.labb2;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class UDPEchoServer {
	public static final int BUFSIZE = 1024;
	public static final int MYPORT = 4950;

	public static void main(String[] args) throws IOException {
		byte[] buf = null;
		/* If no buffersize is given take default value */
		if (args.length != 1) {
			buf = new byte[BUFSIZE];
		} else {
			try {
				buf = new byte[Integer.valueOf(args[0])];
			} catch (NumberFormatException ex) {
				System.err.printf("usage: %s buffer_size\n", args[1]);
				System.exit(1);
			}
		}

		/* Create socket */
		DatagramSocket socket = new DatagramSocket(null);

		/* Create local bind point */
		SocketAddress localBindPoint = new InetSocketAddress(MYPORT);
		socket.bind(localBindPoint);
		System.out.printf("Server started...");
		while (true) {
			/* Create datagram packet for receiving message */
			DatagramPacket receivePacket = new DatagramPacket(buf, buf.length);

			/* Receiving message */
			socket.receive(receivePacket);

			/* Create datagram packet for sending message */
			DatagramPacket sendPacket = new DatagramPacket(
					receivePacket.getData(), receivePacket.getLength(),
					receivePacket.getAddress(), receivePacket.getPort());

			/* Send message */
			socket.send(sendPacket);
			System.out.printf("UDP echo request from %s", receivePacket
					.getAddress().getHostAddress());
			System.out.printf(" using port %d\n", receivePacket.getPort());
		}
	}
}