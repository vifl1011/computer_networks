/*
  UDPEchoClient.java
  A simple echo client with no error handling
 */

//package dv201.labb2;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Random;

public class UDPEchoClient {
	public static final int BUFSIZE = 1024;
	public static final int MYPORT = 0;
	static final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	static Random random = new Random();

	public static void main(String[] args) throws IOException {
		byte[] buf = null;
		String msg = null;
		try {
			buf = new byte[Integer.valueOf(args[2])];
			msg = randomString(Integer.valueOf(args[3]));
		} catch (NumberFormatException ex) {
			System.err.printf("usage: %s server_name port buffer_size\n",
					args[2]);
			System.exit(1);
		}
		if (args.length != 2) {
			System.err.printf("usage: %s server_name port\n", args[1]);
			System.exit(1);
		}

		/* Create socket */
		DatagramSocket socket = new DatagramSocket(null);

		/* Create local endpoint using bind() */
		SocketAddress localBindPoint = new InetSocketAddress(MYPORT);
		socket.bind(localBindPoint);

		/* Create remote endpoint */
		SocketAddress remoteBindPoint = new InetSocketAddress(args[0],
				Integer.valueOf(args[1]));

		/* Create datagram packet for sending message */
		DatagramPacket sendPacket = new DatagramPacket(msg.getBytes(),
				msg.length(), remoteBindPoint);

		/* Create datagram packet for receiving echoed message */
		DatagramPacket receivePacket = new DatagramPacket(buf, buf.length);

		/* Send and receive message */
		socket.send(sendPacket);
		socket.receive(receivePacket);

		/* Compare sent and received message */
		String receivedString = new String(receivePacket.getData(),
				receivePacket.getOffset(), receivePacket.getLength());
		if (receivedString.compareTo(msg) == 0)
			System.out.printf("%d bytes sent and received\n",
					receivePacket.getLength());
		else
			System.out.printf("Sent and received msg not equal!\n");
		socket.close();
	}

	public static String randomString(int length) {
		StringBuilder sb = new StringBuilder(length);
		for (int i = 0; i < length; i++)
			sb.append(AB.charAt(random.nextInt(AB.length())));
		return sb.toString();
	}
}