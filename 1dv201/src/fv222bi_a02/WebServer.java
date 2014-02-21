package fv222bi_a02;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.StringTokenizer;

public class WebServer extends Thread {
	public static int myPort;
	Socket socket = null;
	BufferedReader bufferedReader = null; // input from client
	DataOutputStream dataOutputStream = null; // sending to client
	
	public WebServer(Socket socket)
	{
		if(socket == null)
			throw new IllegalArgumentException("No socket availabe.");
		this.socket = socket;
	}
	
	public void run()
	{
		System.out.println( "Client " + socket.getInetAddress() + "\nPort " + socket.getPort());

		try {
			bufferedReader = new BufferedReader(new InputStreamReader (socket.getInputStream()));
			dataOutputStream = new DataOutputStream(socket.getOutputStream());
		} catch (IOException ex) {
			System.err.printf("Couldn't establish connection.");			
		}
		
		String requestString = null;
		try {
			requestString = bufferedReader.readLine();
			String headerLine = requestString;
			StringTokenizer tokenizer = new StringTokenizer(headerLine);
			String httpMethod = tokenizer.nextToken();
			String httpQueryString = tokenizer.nextToken();
			
			StringBuffer responseBuffer = new StringBuffer();
			responseBuffer.append("<b> This is the HTTP Server Home Page.... </b><BR>");
			responseBuffer.append("The HTTP Client request is ....<BR>");
		  
			System.out.println("The HTTP request string is ....");
			while (bufferedReader.ready())
			{
				// Read the HTTP complete HTTP Query
				responseBuffer.append(requestString + "<BR>");
				System.out.println(requestString);
				requestString = bufferedReader.readLine();
			}
	
			if (httpMethod.equals("GET")) {
				if (httpQueryString.equals("/")) {
					// The default home page
					sendResponse(200, responseBuffer.toString(), false);
				} else {
					//This is interpreted as a file name
					String fileName = httpQueryString.replaceFirst("/", "");
					fileName = URLDecoder.decode(fileName);
					if (new File(fileName).isFile()){
						sendResponse(200, fileName, true);
					}
					else {
						sendResponse(404, "<b>The Requested resource not found ...." +
								"Usage: http://127.0.0.1:5000 or http://127.0.0.1:5000/</b>", false);
					}
				}
			}
			else 
				sendResponse(404, "<b>The Requested resource not found ...." + "Usage: http://127.0.0.1:5000 or http://127.0.0.1:5000/</b>", false);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void sendResponse (int statusCode, String responseString, boolean isFile) throws Exception {

		String statusLine = null;
		String serverdetails = "Server: Java HTTPServer";
		String contentLengthLine = null;
		String fileName = null;
		String contentTypeLine = "Content-Type: text/html" + "\r\n";
		FileInputStream fin = null;

		if (statusCode == 200)
			statusLine = "HTTP/1.1 200 OK" + "\r\n";
		else
			statusLine = "HTTP/1.1 404 Not Found" + "\r\n";

		if (isFile) {
			fileName = responseString;
			fin = new FileInputStream(fileName);
			contentLengthLine = "Content-Length: " + Integer.toString(fin.available()) + "\r\n";
			if (!fileName.endsWith(".htm") && !fileName.endsWith(".html"))
				contentTypeLine = "Content-Type: \r\n";
		}
		else {
			responseString = myHTTPServer.HTML_START + responseString + myHTTPServer.HTML_END;
			contentLengthLine = "Content-Length: " + responseString.length() + "\r\n";
		}

		DataOutputStream.writeBytes(statusLine);
		DataOutputStream.writeBytes(serverdetails);
		DataOutputStream.writeBytes(contentTypeLine);
		DataOutputStream.writeBytes(contentLengthLine);
		DataOutputStream.writeBytes("Connection: close\r\n");
		DataOutputStream.writeBytes("\r\n");

		if (isFile) sendFile(fin, DataOutputStream);
			else DataOutputStream.writeBytes(responseString);

		DataOutputStream.close();
	}

	public void sendFile (FileInputStream fileInputStream, DataOutputStream dataOutputStream) throws Exception {
		byte[] buffer = new byte[1024] ;
		int bytesRead;

		while ((bytesRead = fileInputStream.read(buffer)) != -1 ) {
			dataOutputStream.write(buffer, 0, bytesRead);
		}
		fileInputStream.close();
	}

	public static void main(String[] args) throws IOException {
		try {
			myPort = Integer.valueOf(args[0]);
		} catch (Exception e) { // catch parse error
			System.err.printf("Unknown Port. Using default value '5000'.");
			myPort = Integer.valueOf(5000);
		}
        
    }
}
