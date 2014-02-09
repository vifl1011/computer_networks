package fv222bi_TCP;
/*
  UDPEchoServer.java
  A simple echo server with no error handling
 */

//package dv201.labb2;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;

public class TCPEchoServer {
	public static final int BUFSIZE = 1024;
	public static final int MYPORT = 4951;

	public static void main(String[] args) throws IOException {
		byte[] buf = null;
		String msg = null;
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
		ServerSocket serverSocket = new ServerSocket();

		/* Create local bind point */
		SocketAddress localBindPoint = new InetSocketAddress(MYPORT);
		serverSocket.bind(localBindPoint);
		Socket socket = serverSocket.accept();
		System.out.printf("Server started...");
		while (true) {
			/* Create DataOutputStream which sends to the client */
			DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
			
			/* Create BufferedReader to receive echo of the client */
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			
			/* Create datagram packet for receiving message */
			DatagramPacket receivePacket = new DatagramPacket(buf, buf.length);

			/* Receiving message */
			msg = bufferedReader.readLine();

			/* Send message */
			dataOutputStream.writeBytes(msg);
			System.out.printf("UDP echo request from %s", receivePacket
					.getAddress().getHostAddress());
			System.out.printf(" using port %d\n", receivePacket.getPort());
		}
	}
}