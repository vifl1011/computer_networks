/*
  UDPEchoClient.java
  A simple echo client with no error handling
 */

//package dv201.labb2;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class UDPEchoClient {
	public static final int BUFSIZE = 1024;
	public static final int MYPORT = 0;
	static final String charSet = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	static Random random = new Random();
	static BufferedReader bufferedReader;

	public static void main(String[] args) throws IOException {
		byte[] buf = null;
		String msg = null;
		int msgPerSecond = 0;
		try {
			buf = new byte[Integer.valueOf(args[2])];
			msg = randomString(Integer.valueOf(args[3]));
			msgPerSecond = Integer.valueOf(args[4]);

		} catch (NumberFormatException ex) {
			System.err.printf("usage: %s server_name port buffer_size\n",
					args[2]);
			System.exit(1);
		}

		/* Create socket */
		DatagramSocket datagramSocket = new DatagramSocket(null);

		/* Create local endpoint using bind() */
		SocketAddress localBindPoint = new InetSocketAddress(MYPORT);
		datagramSocket.bind(localBindPoint);

		/* Create remote endpoint */
		SocketAddress remoteBindPoint = new InetSocketAddress(args[0],
				Integer.valueOf(args[1]));

		/* Create datagram packet for sending message */
		DatagramPacket sendPacket = new DatagramPacket(msg.getBytes(),
				msg.length(), remoteBindPoint);

		/* Create datagram packet for receiving echoed message */
		DatagramPacket receivePacket = new DatagramPacket(buf, buf.length);
		if (msgPerSecond > 1) {
			do {
				try {
					TimeUnit.SECONDS.sleep(1);
				} catch (InterruptedException e) {
					System.err.printf(e.getStackTrace().toString());
				}

				/* Send and receive message */

				for (int it = 0; it < msgPerSecond; it++) {
					sendReceive(datagramSocket, msg, sendPacket, receivePacket);
				}
				System.out.printf("---------------------------------------------------\n");

			} while (true);
		} else {
			sendReceive(datagramSocket, msg, sendPacket, receivePacket);
			System.out.printf("-------------send 1 and done--------------\n");
		}

		// socket.close();
	}

	public static String randomString(int length) {
		StringBuilder sb = new StringBuilder(length);
		for (int i = 0; i < length; i++)
			sb.append(charSet.charAt(random.nextInt(charSet.length())));
		return sb.toString();
	}

	public static void sendReceive(DatagramSocket socket, String msg,
			DatagramPacket sendPacket, DatagramPacket receivePacket) {
		try {
			socket.send(sendPacket);
			socket.receive(receivePacket);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		/* Compare sent and received message */
		String receivedString = new String(receivePacket.getData(),
				receivePacket.getOffset(), receivePacket.getLength());
		if (receivedString.compareTo(msg) == 0)
			System.out.printf("%d bytes sent and received\n",
					receivePacket.getLength());
		else
			System.out.printf("Sent and received msg not equal!\n");
	}
}