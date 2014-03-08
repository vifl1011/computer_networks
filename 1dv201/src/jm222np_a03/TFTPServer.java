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

//@Override
//public String toString() {
//    return String.format(re + " + i" + im);
//}



public class TFTPServer {

    public static int TFTPPORT;
    public static final int BUFSIZE = 516;
    public static final String READDIR = "E:\\develop\\tmp\\read\\";
    public static final String WRITEDIR = "E:\\develop\\tmp\\write\\";
    public static final int OP_RRQ = 1;
    public static final int OP_WRQ = 2;
    public static final int OP_DAT = 3;
    public static final int OP_ACK = 4;
    public static final int OP_ERR = 5;

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
                            /* read request */
                            requestedFile.insert(0, READDIR);

                            System.out.println(requestedFile);
                            HandleRQ(sendSocket, requestedFile.toString(), OP_RRQ);
                        } else {
                            /* write request */
                            requestedFile.insert(0, WRITEDIR);

                            System.out.println(requestedFile);
                            HandleRQ(sendSocket, requestedFile.toString(), OP_WRQ);
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

    private void HandleRQ(DatagramSocket sendSocket, String string, int opRrq) throws FileNotFoundException, IOException {
    	
    	File file = new File(string);
        Path path = Paths.get(string);
        byte[] sendData = Files.readAllBytes(path);
        
        DatagramPacket sendPacket = new DatagramPacket(
        		sendData, sendData.length );

        if(file.exists()){
        
        	System.out.println("PRINT FROM INSIDE HANDLE METHOD!");
        	sendSocket.send(sendPacket);
        }
        else{
            System.out.println("No File!");
        }
    }
}
