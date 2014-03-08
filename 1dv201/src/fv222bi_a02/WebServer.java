package fv222bi_a02;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.StringTokenizer;

public class WebServer implements Runnable {
	static final File DEFAULT_DIR = new File("E:\\develop\\tmp");
	static final String DEFAULT_HTML = "index.html";
	static final int DEFAULT_PORT = 8080;

	public Socket socket;	

	public WebServer(Socket socket) {
		if (socket == null)
			throw new IllegalArgumentException("No socket availabe.");
		this.socket = socket;
	}

	public void run() {
		/* input from client */
		BufferedReader bufferedReader = null;
		/* sending to client */
		PrintWriter printWriter = null;
		BufferedOutputStream bufferedOutputStream = null;
		String requestedFile = null;

		try {
			/* get input from client */
			bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			/* get output to client - headers */
			printWriter = new PrintWriter(socket.getOutputStream());
			/* get binary output to client - data */
			bufferedOutputStream = new BufferedOutputStream(socket.getOutputStream());

			/* read the request of the client */
			String input = bufferedReader.readLine();
			StringTokenizer parse = new StringTokenizer(input);
			/* parse out http method */
			String method = parse.nextToken().toUpperCase();
			/* parse out name of requested file */
			requestedFile = parse.nextToken().toLowerCase();
			
			/* on root level request give back default page */
			if (requestedFile.endsWith("/")) {
				requestedFile += DEFAULT_HTML;
			}

			/* create a new File to serve the client */
			File file = new File(DEFAULT_DIR.toString(), requestedFile);
			int fileLength = (int) file.length();

			/* get MIME type */
			String content = getContentType(requestedFile);

			if (method.equals("GET")) {
				/* handle GET request by creating new byte array */
				FileInputStream fileInputStream = null;
				byte[] fileData = new byte[fileLength];

				try {
					/* write file to array */
					fileInputStream = new FileInputStream(file);
					fileInputStream.read(fileData);
				} finally {
					close(fileInputStream);
				}

				printWriter.println("HTTP/1.0 200 OK");
				printWriter.println("Server: Java HTTP Server 1.0");
				printWriter.println("Date: " + new Date());
				printWriter.println("Content-type: " + content);
				printWriter.println("Content-length: " + file.length());
				printWriter.println();
				printWriter.flush();
				
				bufferedOutputStream.write(fileData, 0, fileLength);
				bufferedOutputStream.flush();
			}
		} catch (FileNotFoundException fnex) {
			/* file doesn't exist message */
			printWriter.println("HTTP/1.0 404 File Not Found");
			printWriter.println("Server: Java HTTP Server 1.0");
			printWriter.println("Date: " + new Date());
			printWriter.println("Content-Type: text/html");
			printWriter.println();
			printWriter.println("<HTML>");
			printWriter.println("<HEAD><TITLE>File Not Found</TITLE></HEAD>");
			printWriter.println("<BODY>");
			printWriter.println("<H2>404 File Not Found: " + requestedFile + "</H2>");
			printWriter.println("</BODY>");
			printWriter.println("</HTML>");
			printWriter.flush();
		} catch (IOException ioex) {
			System.err.println("Server Error: " + ioex);
		} finally {
			close(bufferedReader); // close character input stream
			close(printWriter); // close character output stream
			close(bufferedOutputStream); // close binary output stream
			close(socket); // close socket connection
		}
	}

	private String getContentType(String fileRequested) {
		if (fileRequested.endsWith(".html")) {
			return "text/html";
		} else if (fileRequested.endsWith(".jpg")) {
			return "image/jpeg";
		} else {
			return "text/plain";
		}
	}

	public void close(Object stream) {
		if (stream == null)
			return;

		try {
			if (stream instanceof Reader) {
				((Reader) stream).close();
			} else if (stream instanceof Writer) {
				((Writer) stream).close();
			} else if (stream instanceof InputStream) {
				((InputStream) stream).close();
			} else if (stream instanceof OutputStream) {
				((OutputStream) stream).close();
			} else if (stream instanceof Socket) {
				((Socket) stream).close();
			} else {
				System.err.println("Unable to close object: " + stream);
			}
		} catch (Exception e) {
			System.err.println("Error closing stream: " + e);
		}
	}

	public void sendFile(FileInputStream fileInputStream, DataOutputStream dataOutputStream) throws Exception {
		byte[] buffer = new byte[1024];
		int bytesRead;

		while ((bytesRead = fileInputStream.read(buffer)) != -1) {
			dataOutputStream.write(buffer, 0, bytesRead);
		}
		fileInputStream.close();
	}

	public static void main(String[] args) throws IOException {
		try {
			ServerSocket serverConnect = new ServerSocket(DEFAULT_PORT,5,InetAddress.getByName("127.0.0.1"));
			System.out.println("\nServer started on port " + DEFAULT_PORT + "...\n");
			/* server will run until it's aborted */
			while (true) {
				WebServer webServer = new WebServer(serverConnect.accept());

				/* create a new thread */
				Thread thread = new Thread(webServer);
				thread.start();
			}
		} catch (IOException ex) {
			System.err.println("Error: " + ex);
		}
	}
}
