package fv222bi_a01;
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

	public static void main(String[] args) throws IOException {
		byte[] buf = null;
		String msg = null;
		int msgPerSecond = 0;
		/* Try to convert single arguments into Integer */
		try {
			buf = new byte[Integer.valueOf(args[2])];
			msg = randomString(Integer.valueOf(args[3]));
			msgPerSecond = Integer.valueOf(args[4]);
		} 
		/* On error give the needed information back as String */
		catch (NumberFormatException ex) {
			System.err.printf("usage: server_name port buffer_size "
					+ "message_length message_per_second\n");
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
		/* Check if more than one message should be send */
		if (msgPerSecond > 1) {
			do {
				/* Timer that messages were send every second */
				try {
					TimeUnit.SECONDS.sleep(1);
				} catch (InterruptedException e) {
					System.err.printf(e.getStackTrace().toString());
				}

				/* Send and receive multiple messages */
				for (int it = 0; it < msgPerSecond; it++) {
					sendReceive(datagramSocket, msg, sendPacket, receivePacket);
				}
				System.out.printf("---------------------------------------------------\n");

			} while (true);
		} 
		/* Send one single message */
		else {
			sendReceive(datagramSocket, msg, sendPacket, receivePacket);
			System.out.printf("------------- single message sent --------------\n");
		}
	}

	/* Method to generate random strings depending on the given value of the argument */
	public static String randomString(int length) {
		StringBuilder sb = new StringBuilder(length);
		for (int i = 0; i < length; i++)
			sb.append(charSet.charAt(random.nextInt(charSet.length())));
		return sb.toString();
	}
	
	/* extract the send and receive part in own method */
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