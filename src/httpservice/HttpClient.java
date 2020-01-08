package httpservice;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.URLConnection;
import java.nio.file.Files;

/**
 * HttpClient is a class representing a simple HTTP client.
 *
 * @author Li Taiji 
 * @date: 2019-11-17
 */

public class HttpClient {

	/**
	 * default HTTP server host
	 */
	private String host;

	/**
	 * Allow a maximum buffer size of 8192 bytes
	 */
	private static int buffer_size = 8192;

	/**
	 * Response is stored in a byte array.
	 */
	private byte[] buffer;

	/**
	 * My socket to the world.
	 */
	private Socket socket = null;

	/**
	 * Default port is 18081.
	 */
	private static final int PORT = 18081;

	/**
	 * Output stream to the socket.
	 */
	private BufferedOutputStream ostream = null;

	/**
	 * Input stream from the socket.
	 */
	private BufferedInputStream istream = null;

	/**
	 * StringBuffer storing the header
	 */
	private StringBuffer header = null;

	/**
	 * StringBuffer storing the response.
	 */
	private StringBuffer response = null;

	/**
	 * String to represent the Carriage Return and Line Feed character sequence.
	 */
	private static final String CRLF = "\r\n";

	/**
	 * String to represent the status code of response from HTTP server
	 */
	private String statusCode = null;

	/**
	 * HttpClient constructor;
	 */
	public HttpClient() {
		buffer = new byte[buffer_size + 1];
		header = new StringBuffer();
		response = new StringBuffer();
	}

	/**
	 * connects to the input host on the default HTTP port 80 This function opens
	 * the socket and creates the input and output streams used for communication.
	 * 
	 * @param host The IP address of server
	 * @throws IOException If an error occurred in the connection
	 */
	public void connect(String host) throws IOException {

		this.host = host;

		/**
		 * Open my socket to the specified host at the default port.
		 */
		socket = new Socket(host, PORT);

		/**
		 * Create the output stream.
		 */
		ostream = new BufferedOutputStream(socket.getOutputStream());

		/**
		 * Create the input stream.
		 */
		istream = new BufferedInputStream(socket.getInputStream());
	}

	/**
	 * process the input GET request.
	 * 
	 * @param request   The first line of request
	 * @param keepAlive The "Connection" attribute
	 * @throws IOException If an error occurred when send response
	 */
	public void processGetRequest(String request, boolean keepAlive) throws IOException {
		/**
		 * Send the request to the server.
		 */
		request += CRLF;
		request += "Host: " + this.host + CRLF;
		request += "User-Agent: MyClient-1.0" + CRLF;
		request += "Accept-Encoding: ISO-8859-1" + CRLF;
		if (keepAlive) {
			request += "Connection: keep-alive" + CRLF + CRLF;
		} else {
			request += "Connection: close" + CRLF + CRLF;
		}
		buffer = request.getBytes();
		ostream.write(buffer, 0, request.length());
		ostream.flush();
		/**
		 * waiting for the response.
		 */
		processResponse("GET");
	}

	/**
	 * process the input PUT request.
	 * 
	 * @param request The first line of request
	 * @throws IOException If an error occurred when send response
	 */
	public void processPutRequest(String request) throws IOException {
		String[] tokens = request.split("\\s+");
		if (tokens.length != 3) {
			System.out.println("Incorrect Request");
			close();
		} else {
			String fileName = tokens[1];
			String version = tokens[2];
			if (version.equals("HTTP/1.0") || version.equals("HTTP/1.1")) {
				String filePath = "C:\\Users\\admin\\Desktop\\socket" + fileName.replaceAll("/", "\\\\");
				// The path of file that client put
				File file = new File(filePath);
				if (file.exists()) {
					// Send the response
					StringBuilder putMessage = new StringBuilder();
					putMessage.append(request);
					putMessage.append(CRLF);
					putMessage.append("User-Agent: MyClient-1.0" + CRLF);
					putMessage.append("Accept-Encoding: ISO-8859-1" + CRLF);
					putMessage.append(
							"Content-Type: " + URLConnection.getFileNameMap().getContentTypeFor(fileName) + CRLF);
					putMessage.append("Content-Length: " + file.length() + CRLF);
					putMessage.append("Connection: close" + CRLF);
					putMessage.append(CRLF);
					// Send to server
					String message = putMessage + "";
					buffer = message.getBytes("ISO-8859-1");
					ostream.write(buffer, 0, message.length());
					ostream.flush();
					System.out.println(message);
					// Read file and send it to server
					byte[] sendData = Files.readAllBytes(file.toPath());
					ostream.write(sendData);
					ostream.flush();
				} else {
					System.out.println("File does not exist");
					close();
				}
			} else {
				System.out.println("Bad Request");
				close();
			}
		}
		processResponse("PUT");
	}

	/**
	 * process the server response.
	 * 
	 * @param requestType The type of request
	 * @throws IOException If an error occurred when receive response
	 */
	public void processResponse(String requestType) throws IOException {
		int last = 0, c = 0;
		/**
		 * Process the header and add it to the header StringBuffer.
		 */

		boolean inHeader = true; // loop control
		while (inHeader && ((c = istream.read()) != -1)) {
			switch (c) {
			case '\r':
				break;
			case '\n':
				if (c == last) {
					inHeader = false;
					break;
				}
				last = c;
				header.append("\n");
				break;
			default:
				last = c;
				header.append((char) c);
			}
		}
		header.append("\r\n");
		
		if (requestType.equals("GET")) {
			/**
			 * Read the contents and add it to the response StringBuffer.
			 */
			String[] tokens = getHeader().split("\\s+");
			statusCode = tokens[1];
			int fileLength = 0;
			for (int i = 0; i < tokens.length; i++) {
				if (tokens[i].equals("Content-Length:")) {
					fileLength = Integer.parseInt(tokens[i + 1]);
					// Get the length of file
					break;
				}
			}
			int revBytes = 0;
			int len = 0;
			buffer = new byte[buffer_size];
			while ((len = istream.read(buffer, 0, buffer_size)) > 0) {
				// Get the data of the specified length from server
				revBytes += len;
				response.append(new String(buffer, 0, len, "ISO-8859-1"));
				if (revBytes >= fileLength) {
					break;
				}

			}
		}

	}

	/**
	 * Get the response header.
	 * 
	 * @return header
	 */
	public String getHeader() {
		return header.toString();
	}

	/**
	 * Get the server's response.
	 * 
	 * @return response
	 */
	public String getResponse() {
		return response.toString();
	}

	/**
	 * Get the status code of response
	 * 
	 * @return StatusCode
	 */
	public String getStatusCode() {
		return statusCode;
	}

	/**
	 * Clear the header of response.
	 */
	public void clearHeader() {
		header.setLength(0);
	}

	/**
	 * Clear the content of response.
	 */
	public void clearResponse() {
		response.setLength(0);
	}

	/**
	 * Close all open connections -- sockets and streams.
	 * 
	 * @throws IOException If an error occurred in the connection
	 */
	public void close() throws IOException {
		istream.close();
		ostream.close();
		socket.close();
	}
}
