package httpservice;

import java.io.*;
import java.net.Socket;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.Date;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 * HttpHandler is class handle the client's request.
 * 
 * @author Li Taiji 
 * @date: 2019-11-17
 * 
 */
public class HttpHandler implements Runnable {
	private File file;
	private Socket socket;
	private final String logHeader; // The header of log term
	private String rootpath;
	private String savepath;
	private static final String CRLF = "\r\n";
	private StringBuilder request = null; // Save the request from the client
	private BufferedInputStream inputStream = null;
	private BufferedOutputStream outputStream = null;
	private String requestLine; // The first line of request
	private boolean keepAlive; // The "Connection" attribute
	private Logger logger; // The log file

	/**
	 * @param socket   The socket with client
	 * @param rootpath The root path of server
	 * @param logger   Log file
	 * @throws UnsupportedEncodingException If the encoding is unsupported
	 */
	public HttpHandler(Socket socket, String rootpath, Logger logger) throws UnsupportedEncodingException {
		this.socket = socket;
		this.rootpath = rootpath;
		this.logger = logger;
		this.logHeader = "Client on " + this.socket.getInetAddress().getHostAddress() + " <" + this.socket.getPort()
				+ ">: ";
		this.logger.info(this.logHeader + "Connect successfully!");
	}

	/**
	 * Initialize
	 * 
	 * @throws IOException If an error occurred in the connection
	 */
	public void init() throws IOException {
		inputStream = new BufferedInputStream(socket.getInputStream());
		outputStream = new BufferedOutputStream(socket.getOutputStream());
		request = new StringBuilder();
		requestLine = "";
		keepAlive = true;
		savepath = rootpath + "\\saving";
	}

	@Override
	/**
	 * Implement run thread
	 */
	public void run() {
		try {
			init(); // Initialize
			while (keepAlive) {
				receiveRequest(); // receive request
				this.logger.info('\n' + this.logHeader + request.toString());
				if (request.indexOf("GET") != -1) {
					doGetResponse();
				} else if (request.indexOf("PUT") != -1) {
					// System.out.println(request.toString());
					doPutResponse();
				} else { // Only respond to GET and PUT
					this.logger.log(Level.WARNING, this.logHeader + "Incorrert Request");
					String filePath = rootpath + "\\response\\400.html";
					File file = new File(filePath);
					sendHeader("HTTP/1.1 400 Bad Request", URLConnection.getFileNameMap().getContentTypeFor(filePath),
							file.length(), false);
					sendContent(filePath);
				}
			}

		} catch (IOException ex) {
			this.logger.log(Level.WARNING, this.logHeader + "Resolve Request Error", ex);
		}
	}

	/**
	 * handle the GET request
	 * 
	 * @throws IOException
	 */
	private void doGetResponse() throws IOException {
		String[] tokens = requestLine.split("\\s+");
		String filePath = null;
		File file = null;
		keepAlive = false;

		if (tokens.length != 3) {
			this.logger.log(Level.WARNING, this.logHeader + "Incorrect Request! Missing necessary parts");
			filePath = rootpath + "\\response\\400.html";
			file = new File(filePath);
			sendHeader("HTTP/1.1 400 Bad Request", URLConnection.getFileNameMap().getContentTypeFor(filePath),
					file.length(), keepAlive);
			sendContent(filePath);
		} else {

			String url = tokens[1];
			String version = tokens[2];

			tokens = request.toString().split("\\s+");
			for (int i = 0; i < tokens.length; i++) {
				if (tokens[i].equals("Connection:")) {
					// Get the "Connection"
					keepAlive = tokens[i + 1].equals("keep-alive");
					break;
				}
			}

			if (version.equals("HTTP/1.0") || version.equals("HTTP/1.1")) {
				if (url.endsWith("/")) {
					url = url + "index.html";
				}
				filePath = rootpath + url.replaceAll("/", "\\\\");
				file = new File(filePath);
				if (file.exists()) {
					sendHeader("HTTP/1.1 200 OK", URLConnection.getFileNameMap().getContentTypeFor(filePath),
							file.length(), keepAlive);
					sendContent(filePath);
				} else {
					filePath = rootpath + "\\response\\404.html";
					file = new File(filePath);
					sendHeader("HTTP/1.1 404 Not Found", URLConnection.getFileNameMap().getContentTypeFor(filePath),
							file.length(), keepAlive);
					sendContent(filePath);
				}
			} else {
				this.logger.log(Level.WARNING, this.logHeader + "HTTP version not accepted");
				filePath = rootpath + "\\response\\400.html";
				file = new File(filePath);
				sendHeader("HTTP/1.1 400 Bad Request", URLConnection.getFileNameMap().getContentTypeFor(filePath),
						file.length(), keepAlive);
				sendContent(filePath);
			}
		}
		if (!keepAlive) {
			// If Connection is not keep-alive, close the connection with client.
			inputStream.close();
			outputStream.close();
			socket.close();
		} else {
			request.setLength(0);
			// clear the request
		}
	}

	/**
	 * handle the PUT request
	 */
	private void doPutResponse() {
		try {
			String[] tokens = request.toString().split("\\s+");
			int fileLength = 0;
			String contentType = null;
			keepAlive = false;
			for (int i = 0; i < tokens.length; i++) {
				// resolve the request
				if (tokens[i].equals("Content-Length:")) {
					fileLength = Integer.parseInt(tokens[i + 1]);
				} else if (tokens[i].equals("Content-Type:")) {
					contentType = tokens[i + 1];
				} else if (tokens[i].equals("Connection:")) {
					keepAlive = tokens[i + 1].equals("keep-alive");
				}
			}

			sendHeader("HTTP/1.1 200 OK", contentType, fileLength, keepAlive);

			savepath = savepath + tokens[1].replaceAll("/", "\\\\");
			File file = new File(savepath);
			FileOutputStream fos = new FileOutputStream(file);

			byte[] buffer = new byte[fileLength];
			int revBytes = 0;
			int bytes;
			while ((bytes = inputStream.read(buffer, revBytes, fileLength - revBytes)) > 0) {
				revBytes += bytes;

			}
			fos.write(buffer);
			fos.flush();
			fos.close();
			if (!keepAlive) {
				inputStream.close();
				outputStream.close();
				socket.close();
			} else {
				request.setLength(0);
			}
		} catch (IOException ex) {
			this.logger.log(Level.SEVERE, this.logHeader + "Can not receive file", ex);
		}
	}

	/**
	 * resolve request
	 * 
	 * @throws IOException
	 */
	private void receiveRequest() throws IOException {
		boolean finishFirst = false;
		boolean finishAll = false;
		boolean finishLast = false;
		int c = 0;
		while (!finishAll && (c = inputStream.read()) != -1) {
			switch (c) {
			case '\r':
				break;
			case '\n':
				if (!finishFirst) {
					requestLine = request.toString();
					// Get the first line of request
					finishFirst = true;
				}
				if (!finishLast) {
					request.append("\r\n");
					finishLast = true;
				} else if (finishLast) {
					finishAll = true;
				}
				break;
			default:
				request.append((char) c);
				finishLast = false;
				break;
			}
		}
	}

	/**
	 * send the response to client
	 * 
	 * @param responseCode
	 * @param contentType
	 * @param length
	 * @param keepAlive
	 */
	private void sendHeader(String responseCode, String contentType, long length, boolean keepAlive) {
		try {
			StringBuilder response = new StringBuilder();
			response.append(responseCode + CRLF);
			response.append("Date: " + new Date().toString() + CRLF);
			response.append("Server: MyHttpServer/1.0" + CRLF);
			response.append("Content-Length: " + length + CRLF);
			response.append("Content-type: " + contentType + CRLF);
			if (keepAlive) {
				response.append("Connection: keep-alive" + CRLF + CRLF);
			} else {
				response.append("Connection: close" + CRLF + CRLF);
			}

			byte[] buffer = response.toString().getBytes("ISO-8859-1");
			outputStream.write(buffer, 0, buffer.length);
			outputStream.flush();
		} catch (UnsupportedEncodingException ex) {
			this.logger.log(Level.SEVERE, "Unsupported Encoding", ex);
		} catch (IOException ex) {
			this.logger.log(Level.SEVERE, "Send Header Error", ex);
		}

	}

	/**
	 * send the file to client
	 * 
	 * @param filePath
	 */
	private void sendContent(String filePath) {
		try {
			file = new File(filePath);
			byte[] sendData = Files.readAllBytes(file.toPath());
			outputStream.write(sendData);
			outputStream.flush();
		} catch (IOException ex) {
			this.logger.log(Level.SEVERE, "Can not send file", ex);
		}
	}

}
