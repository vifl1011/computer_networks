package fv22bi_TCP;
/*
  UDPEchoClient.java
  A simple echo client with no error handling
 */

//package dv201.labb2;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class TCPEchoClient {
	public static final int BUFSIZE = 1024;
	public static final int MYPORT = 0;
	static final String charSet = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	static Random random = new Random();

	public static void main(String[] args) throws IOException {
		byte[] buf = null;
		String msg = null;
		String receive = null;
		int msgPerSecond = 0;
		try {
			buf = new byte[Integer.valueOf(args[2])];
			msg = randomString(Integer.valueOf(args[3]));
			msgPerSecond = Integer.valueOf(args[4]);

		} catch (NumberFormatException ex) {
			System.err.printf("usage: server_name port buffer_size "
					+ "message_length message_per_second\n");
			System.exit(1);
		}

		/* Create socket */
		Socket socket = new Socket(null);

		/* Create local endpoint using bind() */
		SocketAddress localBindPoint = new InetSocketAddress(MYPORT);
		socket.bind(localBindPoint);
		
		/* Create DataOutputStream which sends to the server */
		DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
		
		/* Create BufferedReader to receive echo of the server */
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		
		if (msgPerSecond > 1) {
			do {
				try {
					TimeUnit.SECONDS.sleep(1);
				} catch (InterruptedException e) {
					System.err.printf(e.getStackTrace().toString());
				}

				/* Send and receive message */

				for (int it = 0; it < msgPerSecond; it++) {
					sendReceive(socket, msg, receive, dataOutputStream, bufferedReader);
				}
				System.out.printf("---------------------------------------------------\n");

			} while (true);
		} else {
			sendReceive(socket, msg, receive, dataOutputStream, bufferedReader);
			System.out.printf("-------------send 1 and done--------------\n");
		}
	}

	public static String randomString(int length) {
		StringBuilder sb = new StringBuilder(length);
		for (int i = 0; i < length; i++)
			sb.append(charSet.charAt(random.nextInt(charSet.length())));
		return sb.toString();
	}

	public static void sendReceive(Socket socket, String msg, String receive,
			DataOutputStream dataOutputStream, BufferedReader bufferedReader) {
		try {
			dataOutputStream.writeBytes(msg);
			receive = bufferedReader.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		/* Compare sent and received message */
		if (receive.compareTo(msg) == 0)
			System.out.printf("%d bytes sent and received\n",
					receive.length());
		else
			System.out.printf("Sent and received msg not equal!\n");
	}
}