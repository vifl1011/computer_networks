package jm222np_a03;

import java.io.*;
import java.io.ObjectInputStream.GetField;
import java.net.*;
import java.nio.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TFTPServer {

	public static int TFTPPORT;
	public static final int BUFSIZE = 516;
	public static final String READDIR_ = "E:\\develop\\tmp\\read\\";
	public static final String WRITEDIR_ = "E:\\develop\\tmp\\write\\";
	public static final String READDIR = "/home/user/tmp/read/";
	public static final String WRITEDIR = "/home/user/tmp/write/";
	public static final int OP_RRQ = 1;
	public static final int OP_WRQ = 2;
	public static final int OP_DAT = 3;
	public static final int OP_ACK = 4;
	public static final int OP_ERR = 5;
	public static final String OP_MOD = "octet";
	public static final byte[] readResp = { 0, 3, 0, 1 };
	public static final byte[] writeResp = { 0, 4, 0, 0 };
	public static final byte[] ACKResp = { 0, 4 };
	static final int TIMEOUT = 5000;

	public static void main(String[] args) {

		// Listen on predefined port
		if (args.length == 0) {
			// Default port if no commandline arg is entered
			TFTPPORT = 4970;
		} else if (args.length == 1) {
			// accept commandline arg for port number
			TFTPPORT = Integer.valueOf(args[0]);
		} else {
			System.err.printf("usage: java %s\n",TFTPServer.class.getCanonicalName());
			System.exit(1);
		}

		try {
			TFTPServer server = new TFTPServer();
			server.start();
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}

	private void start() throws SocketException {
		byte[] buf = new byte[BUFSIZE];

		/* Create socket */
		DatagramSocket socket = new DatagramSocket(null);

		/* Create local bind point */
		SocketAddress localBindPoint = new InetSocketAddress(TFTPPORT);
		socket.bind(localBindPoint);

		System.out.printf("Listening at port %d for new requests\n", TFTPPORT);

		/* Loop to handle various requests */
		while (true) {
			final InetSocketAddress clientAddress = receiveFrom(socket, buf);

			/* If clientAddress is null, an error occurred in receiveFrom() */
			if (clientAddress == null) {
				continue;
			}

			final StringBuffer requestedFile = new StringBuffer();
			final int reqtype = ParseRQ(buf, requestedFile);

			new Thread() {
				public void run() {

					try {
						DatagramSocket sendSocket = new DatagramSocket(null);

						System.out.printf("%s request for %s from %s using port %d\n",
								(reqtype == OP_RRQ) ? "Read" : "Write",
								clientAddress.getHostName(),
								clientAddress.getAddress(),
								clientAddress.getPort());
						sendSocket.connect(clientAddress);

						if (reqtype == OP_RRQ) {
							/* read request */
							requestedFile.insert(0,READDIR);

							System.out.println(requestedFile);
							HandleRQ(sendSocket, clientAddress,
									requestedFile.toString(), OP_RRQ);
						} else if (reqtype == OP_WRQ) {
							/* write request */
							requestedFile.insert(0, WRITEDIR);

							System.out.println(requestedFile);
							HandleRQ(sendSocket, clientAddress,
									requestedFile.toString(), OP_WRQ);
						} else {
							System.out.println("Neij ! ! !");
						}
						sendSocket.close();
					} catch (SocketException e) {
						e.printStackTrace();
					} catch (FileNotFoundException ex) {
						Logger.getLogger(TFTPServer.class.getName()).log(Level.SEVERE, null, ex);
					} catch (IOException ex) {
						Logger.getLogger(TFTPServer.class.getName()).log(Level.SEVERE, null, ex);
					}
				}
			}.start();
		}
	}

	/**
	 * Reads the first block of data, i.e., the request for action (read or write).
	 * 
	 * @param socket
	 *            socket to read from
	 * @param buf
	 *            where to store the read data
	 * @return the Internet socket address of the client
	 */
	private InetSocketAddress receiveFrom(DatagramSocket socket, byte[] buf) {
		// Create packet
		DatagramPacket packet = new DatagramPacket(buf, buf.length);

		// Receive packet contents
		try {
			socket.receive(packet);
		} catch (IOException ex) {
			Logger.getLogger(TFTPServer.class.getName()).log(Level.SEVERE,null, ex);
		}

		// Get InetSocketAddress from packet
		return new InetSocketAddress(packet.getAddress(), packet.getPort());
	}
	
	/* method to extract the request type */
	private int ParseRQ(byte[] buf, StringBuffer requestedFile) {
		// Parse request to get file name
		// Start loop at index 2
		// index 0 and 1 is opcode
		for (int i = 2; i < buf.length; i++) {
			if (buf[i] != 0) {
				requestedFile.append((char) buf[i]);
			} else {
				break;
			}
		}
		System.out.println("Requested File: " + requestedFile);

		// Get opcode and convert to short type
		ByteBuffer wrap = ByteBuffer.wrap(buf);
		short opcode = wrap.getShort();

		System.out.println("Opcode: " + opcode);

		// Return opcode in integer format
		return (int) opcode;
	}

	/* method to handle different request types */
	private void HandleRQ(DatagramSocket sendSocket,
			InetSocketAddress clientAddress, String string, int opRrq)
			throws FileNotFoundException, IOException {
		if (opRrq == OP_RRQ) {
			File file = new File(string);
			if (file.exists()) {
				System.out.println("transfer started...");
				transferFile(sendSocket, clientAddress, string);
			} else {
				System.out.println("File not found");
				sendError(1, "file not found", clientAddress.getAddress(),
						clientAddress.getPort(), sendSocket);
			}
		}

		else if (opRrq == OP_WRQ) {
			System.out.println("incoming file");
			recieveFile(sendSocket, clientAddress, string);
		}
	}

	/* method to send a file to the client */
	private void transferFile(DatagramSocket sendSocket, InetSocketAddress clientAddress, String string) throws IOException {
		System.out.println("sending file: \"" + string + "\"");

		DatagramPacket datagramPacket = new DatagramPacket(new byte[BUFSIZE], BUFSIZE, clientAddress.getAddress(), clientAddress.getPort());
		setOptCode(OP_DAT, datagramPacket);

		try {
			FileInputStream f = new FileInputStream(string);
			int n;
			boolean last = false;
			short count = 1; // block count starts at 1
			do {
				n = f.read(datagramPacket.getData(), 4, BUFSIZE - 4);
				if (n == -1)
					n = 0;
				if (n < BUFSIZE)
					last = true;

				datagramPacket.setLength(n + 4);
				setBlocknumber(count, datagramPacket);
				System.out.printf("sending: block %d/%d bytes\n", count, n);

				sendrecv(datagramPacket, sendSocket);

				count++;
			} while (!last);
			f.close();

		} catch (FileNotFoundException e) {
			System.out.printf("Can't read \"%s\"\n", string);
			sendError(1, "file not found", clientAddress.getAddress(), clientAddress.getPort(), sendSocket);
		} catch (IOException e) {
			System.out.println("Failed with IO error (file or socket)\n");
		}
	}

	/* method to receive a file from the client */
	private void recieveFile(DatagramSocket sendSocket,
			InetSocketAddress clientAddress, String string) throws IOException {

		FileOutputStream f = null;
		if ((new File(string)).exists()) {
			sendError(6, "File already exist", clientAddress.getAddress(), clientAddress.getPort(), sendSocket);
			System.out.println("File already exists!");
			return;
		} else
			sendACK(0, clientAddress, sendSocket);
		System.out.println("...ACK sended");
		DatagramPacket datagramPacket = new DatagramPacket(new byte[BUFSIZE], BUFSIZE);

		int count = 1; 
		boolean last = false;
		try {
			do {
				sendSocket.receive(datagramPacket);
				short[] optBlock = getOptBlock(datagramPacket);
				if (optBlock[0] == OP_DAT && optBlock[1] == count) {
					if (!datagramPacket.getAddress().equals( clientAddress.getAddress()) || datagramPacket.getPort() != clientAddress .getPort()) {
						System.out.println("received datagram from unknown client... ignored!");
						continue;
					}

					if (f == null) {
						f = new FileOutputStream(string);
					}
					int n = datagramPacket.getLength() - 4;
					f.write(datagramPacket.getData(), 4, n);
					System.out .printf("received: block %d/%d bytes\n", count, n);
					count++;
					last = n < BUFSIZE;
				} else {
					System.out.printf("error! unexpected packet (opcode=%d, count=%d)\n", optBlock[0], optBlock[1]);
				}
				sendACK(count - 1, clientAddress, sendSocket);
			} while (!last);
			f.close();
		} catch (IOException e) {
			System.out.println("Aborting receiveFile with IOException: " + e.getMessage());
		}
	}

	/* method to handle retry and timeout */
	private void sendrecv(DatagramPacket datagramPacket, DatagramSocket sendSocket) throws IOException {
		DatagramPacket msgACK = new DatagramPacket(new byte[1024], 1024);
		sendSocket.setSoTimeout(TIMEOUT);

		int retry = 3;
		do {
			sendSocket.send(datagramPacket);
			try {
				sendSocket.receive(msgACK); // waits for ACK

				if (msgACK.getAddress().equals(datagramPacket.getAddress())
						&& msgACK.getPort() == datagramPacket.getPort()
						&& getOptBlock(msgACK)[0] == OP_ACK) {
					if (getOptBlock(msgACK)[1] == getOptBlock(datagramPacket)[1]) {
						System.out.println("ok! (ack)");
						break;
					} else {
						System.out.println("wrong ack ignored, block= "
								+ getOptBlock(msgACK)[1]);
					}
				} else {
					System.out.println("error! (unexpected packet)");
				}
			} catch (SocketTimeoutException e) {
			}
			retry--;
		} while (retry > 0);
		sendSocket.setSoTimeout(0); // no timeout
		if (retry == 0) {
			System.out.println("Too many retries!");
			throw new IOException("To many retries");
		}
	}

	/* method to provide client with error messages */
	private void sendError(int err, String errMsg, InetAddress host, int port, DatagramSocket sendSocket) throws IOException {
		byte[] buff = new byte[4 + errMsg.length() + 1];
		DatagramPacket datagramPacket = new DatagramPacket(buff, buff.length, host, port);
		setOptCode(OP_ERR, datagramPacket);
		setBlocknumber(err, datagramPacket);
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		byteArrayOutputStream.write(datagramPacket.getData(), 0, 4);
		byteArrayOutputStream.write(errMsg.getBytes(), 4, errMsg.length() - 4);
		try {
			sendSocket.send(datagramPacket);
		} catch (IOException e) {
			System.err.println("failed to send error datagram");
		}
	}

	/* method to send an ACK flag to the client */
	private void sendACK(int count, InetSocketAddress clientAddress, DatagramSocket sendSocket) throws IOException {
		byte[] buf = new byte[2];
		short shortVal = (short) count;
		ByteBuffer wrap = ByteBuffer.wrap(buf);
		byte[] blockNumber = wrap.putShort(shortVal).array();
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		byteArrayOutputStream.write(ACKResp);
		byteArrayOutputStream.write(blockNumber);
		byte sendResponse[] = byteArrayOutputStream.toByteArray();
		DatagramPacket datagramPacket = new DatagramPacket(sendResponse, 4,
				clientAddress.getAddress(), clientAddress.getPort());
		try {
			sendSocket.send(datagramPacket);
		} catch (IOException e) {
			System.err.println("failed to send ack");
		}
	}

	/* method to extract the optCode and the blockCount */
	private short[] getOptBlock(DatagramPacket datagramPacket) {
		byte[] data = datagramPacket.getData();
		byte[] opt = new byte[2];
		byte[] block = new byte[2];
		short[] result = new short[2];
		for (int i = 0; i < 4; i++) {
			if (i < 2)
				opt[i] = data[i];
			else
				block[i - 2] = data[i];
		}
		ByteBuffer wrap = ByteBuffer.wrap(opt);
		result[0] = wrap.getShort();
		wrap = null;
		wrap = ByteBuffer.wrap(block);
		result[1] = wrap.getShort();
		return result;
	}

	/* method to set the first two byte mostly optCode */
	private void setOptCode(int number, DatagramPacket datagramPacket) throws IOException {
		byte[] data = datagramPacket.getData();
		byte[] buf = new byte[2];
		short shortVal = (short) number;
		ByteBuffer wrap = ByteBuffer.wrap(buf);
		byte[] blockNumber = wrap.putShort(shortVal).array();
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		byteArrayOutputStream.write(blockNumber);
		byteArrayOutputStream.write(data, 2, data.length - 2);
		datagramPacket.setData(byteArrayOutputStream.toByteArray());
	}

	/* method to set the second two byte mostly blockCount */
	private void setBlocknumber(int number, DatagramPacket datagramPacket) throws IOException {
		byte[] data = datagramPacket.getData();
		byte[] buf = new byte[2];
		short shortVal = (short) number;
		ByteBuffer wrap = ByteBuffer.wrap(buf);
		byte[] blockNumber = wrap.putShort(shortVal).array();
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		byteArrayOutputStream.write(data, 0, 2);
		byteArrayOutputStream.write(blockNumber);
		byteArrayOutputStream.write(data, 4, data.length - 4);
		datagramPacket.setData(byteArrayOutputStream.toByteArray());
	}
}
