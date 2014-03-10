package jm222np_a03;

import java.io.*;
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
    public static final String READDIR = "E:\\develop\\tmp\\read\\";
    public static final String WRITEDIR = "E:\\develop\\tmp\\write\\";
    public static final String READDIR_ = "/home/user/tmp/read/";
    public static final String WRITEDIR_ = "/home/user/tmp/write/";
    public static final int OP_RRQ = 1;
    public static final int OP_WRQ = 2;
    public static final int OP_DAT = 3;
    public static final int OP_ACK = 4;
    public static final int OP_ERR = 5;
    public static final String OP_MOD = "octet";
    public static final byte[] readResp = {0, 3, 0, 1};
    public static final byte[] writeResp = {0, 4, 0, 0};
    public static final byte[] ACKResp = {0, 4};

    public static void main(String[] args) {

        //Listen on predefined port 
        if (args.length == 0) {
            //Default port if no commandline arg is entered
            TFTPPORT = 4970;
        } else if (args.length == 1) {
            //accept commandline arg for port number
            TFTPPORT = Integer.valueOf(args[0]);
        } else {
            System.err.printf("usage: java %s\n", TFTPServer.class.getCanonicalName());
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
                                clientAddress.getHostName(), clientAddress.getAddress(), clientAddress.getPort());
                        sendSocket.connect(clientAddress);

                        if (reqtype == OP_RRQ) {
                        	System.out.println("OP_ReadRQ");
                            /* read request */
                            requestedFile.insert(0, READDIR);

                            System.out.println(requestedFile);
                            HandleRQ(sendSocket, clientAddress, requestedFile.toString(), OP_RRQ);
                        } else if (reqtype == OP_WRQ) {
                        	System.out.println("OP_WriteRQ");
                            /* write request */
                            requestedFile.insert(0, WRITEDIR);

                            System.out.println(requestedFile);
                            HandleRQ(sendSocket, clientAddress, requestedFile.toString(), OP_WRQ);
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
     * Reads the first block of data, i.e., the request for action (read or
     * write).
     *
     * @param socket socket to read from
     * @param buf where to store the read data
     * @return the Internet socket address of the client
     */
    private InetSocketAddress receiveFrom(DatagramSocket socket, byte[] buf) {
        // Create packet
        DatagramPacket packet = new DatagramPacket(buf, buf.length);

        // Receive packet contents
        try {
            socket.receive(packet);
        } catch (IOException ex) {
            Logger.getLogger(TFTPServer.class.getName()).log(Level.SEVERE, null, ex);
        }

        // Get InetSocketAddress from packet 
        return new InetSocketAddress(packet.getAddress(), packet.getPort());
    }

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

        //Return opcode in integer format
        return (int) opcode;
    }

    private void HandleRQ(DatagramSocket sendSocket, InetSocketAddress clientAddress, String string, int opRrq) throws FileNotFoundException, IOException {        
        if(opRrq == OP_RRQ) {
        	File file = new File(string);
        	if(file.exists()){
        		System.out.println("transfer started...");
        		transferFile(sendSocket, clientAddress, file);
            }
            else{
            	System.out.println("File not found");
            	//sendError(sendSocket,clientAddress, OP_ERR);
            }
        }
        	
        else if (opRrq == OP_WRQ) {
        	System.out.println("incoming file");
        	recieveFile(sendSocket, clientAddress, string);
        }
    }
    
    private void transferFile(DatagramSocket sendSocket, InetSocketAddress clientAddress, File file) throws IOException {
    	/* this only works for one packet */
    	Path path = Paths.get(file.getPath());
        byte[] data = Files.readAllBytes(path);
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream( );
		byteArrayOutputStream.write(readResp);
		byteArrayOutputStream.write(data);
		byte sendData[] = byteArrayOutputStream.toByteArray( );
        
        DatagramPacket sendPacket = new DatagramPacket(
							        		sendData, 
							        		sendData.length, 
							        		clientAddress.getAddress(), 
							        		clientAddress.getPort());
        sendSocket.send(sendPacket);
        
        System.out.println("...transfer finished");
    }
    
    private void recieveFile(DatagramSocket sendSocket, InetSocketAddress clientAddress, String string) throws IOException {

    	FileOutputStream f = null;
		if ( (new File(string)).exists() ) {
			//sendError(6, "file exists", clientAddress.getHostName(), clientAddress.getPort());
			System.out.println( "File already exists!");
			return;
		} else sendACK(0, clientAddress, sendSocket);
        System.out.println("...ACK sended");
        DatagramPacket datagramPacket = new DatagramPacket(new byte[BUFSIZE], BUFSIZE);
        
        int count=1; // expected block
		boolean last = false;
		try {
			do {
				//datagramPacket.setLength(BUFSIZE);
				sendSocket.receive( datagramPacket ) ;
				short[] optBlock = getOptBlock(datagramPacket);
				if ( optBlock[0]==OP_DAT && optBlock[1]==count ) {
					if ( !datagramPacket.getAddress().equals(clientAddress.getAddress()) || datagramPacket.getPort() != clientAddress.getPort() ) {
						System.out.println("received datagram from unknown client... ignored!");
						continue;
					}
					
					if ( f==null ) { // first data
						f = new FileOutputStream(string);
					}
					int n = datagramPacket.getLength()-4; // received block size
					f.write( datagramPacket.getData(), 4, n );			
					System.out.printf("received: block %d/%d bytes\n", count, n );
					count++;
					last = n < BUFSIZE;
				} else {
					System.out.printf("error! unexpected packet (opcode=%d, count=%d)\n", optBlock[0], optBlock[1]);
				}
				sendACK(count-1, clientAddress, sendSocket );
			} while ( !last );
			f.close();
		} catch (IOException e) {
			System.out.println( "Aborting receiveFile with IOException: "+e.getMessage() );
		}
    }
    
    private void sendACK(int count, InetSocketAddress clientAddress, DatagramSocket sendSocket) throws IOException {
    	byte[] buf = new byte[2];
    	short shortVal= (short) count;
    	ByteBuffer wrap= ByteBuffer.wrap(buf);
    	byte[] blockNumber= wrap.putShort(shortVal).array();
    	ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream( );
    	byteArrayOutputStream.write(ACKResp);
		byteArrayOutputStream.write(blockNumber);
    	byte sendResponse[] = byteArrayOutputStream.toByteArray( );
    	DatagramPacket datagramPacket = new DatagramPacket( sendResponse, 4, clientAddress.getAddress(), clientAddress.getPort());
		try {
			sendSocket.send(datagramPacket) ;
		} catch (IOException e) {
			System.err.println("failed to send ack");
		}
	}
    
    private short[] getOptBlock(DatagramPacket datagramPacket) {
		byte[] data = datagramPacket.getData();
		byte[] opt = new byte[2];
		byte[] block = new byte[2];
		short[] result = new short[2];
		for (int i = 0; i < 4; i++) {
			if(i < 2)
				opt[i] = data[i];
			else
				block[i-2] = data[i];
	    }
		ByteBuffer wrap= ByteBuffer.wrap(opt);
		result[0] = wrap.getShort();
		wrap = null;
		wrap= ByteBuffer.wrap(block);
		result[1] = wrap.getShort();
		return result;
    }
}
