package httpservice;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 * HttpServer is a class representing a simple HTTP server.
 * 
 * @author Li Taiji 
 * @date: 2019-11-17
 * 
 */
public class HttpServer {

	private static final Logger logger = Logger.getLogger("HTTPServer"); // Log file
	private final int DEFAULT_PORT;
	private static final int POOL_SIZE = 4; // Thread pool capacity
	private String rootpath; // Server root path

	/**
	 * @param args command line argument
	 */
	public HttpServer(String[] args) {

		this.DEFAULT_PORT = Integer.parseInt(args[0]);
		this.rootpath = args[1];
		logger.info("The root path of server " + this.rootpath);
		logger.info("Server Start");

	}

	public static void main(String[] args) throws IOException {
		if (args.length != 2) {
			logger.log(Level.SEVERE, "Missing port or root path!");
			logger.info("Please start server with <port> <root path>!");
			return;
		}
		// Determine if the parameter is valid
		try {
			File root = new File(args[1]);
			if (!root.isDirectory()) {
				logger.log(Level.SEVERE, "The root path does not exist or is not a directory!");
				logger.info("Please start server with a valid root path!");
				return;
			}
		} catch (Exception ex) {
			logger.log(Level.SEVERE, "Can not resolve the root path", ex);
			return;
		}
		new HttpServer(args).service(); // Start the server
	}

	/**
	 * handle the connection with client
	 * 
	 * @throws IOException If an error occurred in the connection
	 */
	public void service() throws IOException {

		ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * POOL_SIZE);
		try (ServerSocket serverSocket = new ServerSocket(DEFAULT_PORT, 2)) { // try-with-resource

			logger.info("Accepting connections on port " + serverSocket.getLocalPort());
			Socket socket = null;
			while (true) {
				try {
					socket = serverSocket.accept();
					// waiting for getting the client
					logger.info("Connect to the client on " + socket.getInetAddress().getHostName());
					HttpHandler httpHandler = new HttpHandler(socket, this.rootpath, logger);
					pool.submit(httpHandler);// Start the thread
				} catch (IOException ex) {
					logger.log(Level.SEVERE, "Accept error", ex);
				} catch (RuntimeException ex) {
					logger.log(Level.SEVERE, "Unexpected error" + ex.getMessage(), ex);
				}
			}
		} catch (IOException ex) {
			logger.log(Level.SEVERE, "Can not start server", ex);
		} catch (RuntimeException ex) {
			logger.log(Level.SEVERE, "Can not start server" + ex.getMessage(), ex);
		}

	}
}
