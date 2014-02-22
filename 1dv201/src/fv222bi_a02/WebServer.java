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
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.StringTokenizer;

public class WebServer extends Thread {
	static final Path DEFAULT_DIR = Paths.get("E:\\develop\\tmp");
	static final String DEFAULT_FILE = "index.html";
	static final int DEFAULT_PORT = 6666;

	public Socket socket;
	BufferedReader bufferedReader; // input from client
	PrintWriter printWriter; // sending to client
	BufferedOutputStream bufferedOutputStream;
	String fileRequested;

	public WebServer(Socket socket) {
		if (socket == null)
			throw new IllegalArgumentException("No socket availabe.");
		this.socket = socket;
	}

	public void run() {
		bufferedReader = null;
		printWriter = null;
		bufferedOutputStream = null;

		try {
			/* get character input stream from client */
			bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			/* get character output stream to client (for headers) */
			printWriter = new PrintWriter(socket.getOutputStream());
			/* get binary output stream to client (for requested data) */
			bufferedOutputStream = new BufferedOutputStream(socket.getOutputStream());

			/* get first line of request from client */
			String input = bufferedReader.readLine();
			/* create StringTokenizer to parse request */
			StringTokenizer parse = new StringTokenizer(input);
			/* parse out method */
			String method = parse.nextToken().toUpperCase();
			/* parse out file requested */
			fileRequested = parse.nextToken().toLowerCase();

			/* methods other than GET and HEAD are not implemented */
			if (!method.equals("GET") && !method.equals("HEAD")) {
				/* send Not Implemented message to client */
				printWriter.println("HTTP/1.0 501 Not Implemented");
				printWriter.println("Server: Java HTTP Server 1.0");
				printWriter.println("Date: " + new Date());
				printWriter.println("Content-Type: text/html");
				printWriter.println();
				printWriter.println("<HTML>");
				printWriter.println("<HEAD><TITLE>Not Implemented</TITLE> </HEAD>");
				printWriter.println("<BODY>");
				printWriter.println("<H2>501 Not Implemented: " + method + " method.</H2>");
				printWriter.println("</BODY></HTML>");
				printWriter.flush();

				return;
			}

			// If we get to here, request method is GET or HEAD
			if (fileRequested.endsWith("/")) {
				// append default file name to request
				fileRequested += DEFAULT_FILE;
			}

			// create file object
			File file = new File(DEFAULT_DIR.toString(), fileRequested);
			// get length of file
			int fileLength = (int) file.length();

			// get the file's MIME content type
			String content = getContentType(fileRequested);

			// if request is a GET, send the file content
			if (method.equals("GET")) {
				FileInputStream fileIn = null;
				// create byte array to store file data
				byte[] fileData = new byte[fileLength];

				try {
					// open input stream from file
					fileIn = new FileInputStream(file);
					// read file into byte array
					fileIn.read(fileData);
				} finally {
					close(fileIn); // close file input stream
				}

				// send HTTP headers
				printWriter.println("HTTP/1.0 200 OK");
				printWriter.println("Server: Java HTTP Server 1.0");
				printWriter.println("Date: " + new Date());
				printWriter.println("Content-type: " + content);
				printWriter.println("Content-length: " + file.length());
				printWriter.println(); // blank line between headers and content
				printWriter.flush(); // flush character output stream buffer

				bufferedOutputStream.write(fileData, 0, fileLength); // write
																		// file
				bufferedOutputStream.flush(); // flush binary output stream
												// buffer
			}
		} catch (FileNotFoundException fnex) {
			// inform client file doesn't exist
			fileNotFound(printWriter, fileRequested);
		} catch (IOException ioex) {
			System.err.println("Server Error: " + ioex);
		} finally {
			close(bufferedReader); // close character input stream
			close(printWriter); // close character output stream
			close(bufferedOutputStream); // close binary output stream
			close(socket); // close socket connection
		}
	}

	private void fileNotFound(PrintWriter printWriter, String file) {
		printWriter.println("HTTP/1.0 404 File Not Found");
		printWriter.println("Server: Java HTTP Server 1.0");
		printWriter.println("Date: " + new Date());
		printWriter.println("Content-Type: text/html");
		printWriter.println();
		printWriter.println("<HTML>");
		printWriter.println("<HEAD><TITLE>File Not Found</TITLE></HEAD>");
		printWriter.println("<BODY>");
		printWriter.println("<H2>404 File Not Found: " + file + "</H2>");
		printWriter.println("</BODY>");
		printWriter.println("</HTML>");
		printWriter.flush();
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

	public void sendFile(FileInputStream fileInputStream,
			DataOutputStream dataOutputStream) throws Exception {
		byte[] buffer = new byte[1024];
		int bytesRead;

		while ((bytesRead = fileInputStream.read(buffer)) != -1) {
			dataOutputStream.write(buffer, 0, bytesRead);
		}
		fileInputStream.close();
	}

	public static void main(String[] args) throws IOException {
		try {
			ServerSocket serverConnect = new ServerSocket(DEFAULT_PORT);
			System.out.println("\nServer started on port " + DEFAULT_PORT
					+ "...\n");
			while (true) {
				WebServer webServer = new WebServer(serverConnect.accept());

				// create new thread
				Thread thread = new Thread(webServer);
				thread.start();
			}
		} catch (IOException ex) {
			System.err.println("Error: " + ex);
		}
	}
}
