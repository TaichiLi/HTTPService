package httpservice;

import java.util.ArrayList;
import java.util.List;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URI;

/**
 * Client is a class representing a simple HTTP client.
 *
 * @author Li Taiji 
 * @date: 2019-11-17
 */

public class Client {

	/**
	 * Input is taken from the keyboard
	 */
	private static BufferedReader keyboard = new BufferedReader(new InputStreamReader(System.in));

	/**
	 * Output is written to the screen (standard out)
	 */
	private static PrintWriter screen = new PrintWriter(System.out, true);

	/**
	 * write the content of file, and return the URI of file.
	 * 
	 * @param fileContent
	 * @param path
	 * @return URI
	 * @throws IOException
	 */
	private static URI writeFile(String fileContent, String path) throws IOException {
		File file;
		int index = path.lastIndexOf("/");
		if (index != -1) {
			String fileName = path.substring(index + 1); // Get the name of file
			String dir = path.substring(0, index); // Get the directory
			file = new File(dir);
			if (!file.exists()) {
				file.mkdirs(); // If the directory does not exist, it will be created.
			}
			file = new File(dir, fileName);
			FileOutputStream outfile = new FileOutputStream(file);
			outfile.write(fileContent.getBytes("ISO-8859-1"));
			outfile.flush();
			outfile.close();
		} else {
			file = new File(path);
			FileOutputStream outfile = new FileOutputStream(file);
			outfile.write(fileContent.getBytes("ISO-8859-1"));
			outfile.flush();
			outfile.close();
		}
		return file.toURI();
	}

	/**
	 * get the response of bad request, and return the URI of file.
	 * 
	 * @param statusCode
	 * @param fileContent
	 * @return URI
	 * @throws IOException
	 */
	private static URI getBadRequest(String statusCode, String fileContent) throws IOException {
		File file = new File(statusCode + ".html");
		if (!file.exists()) {
			FileOutputStream outfile = new FileOutputStream(file);
			outfile.write(fileContent.getBytes("ISO-8859-1"));
			outfile.flush();
			outfile.close();
		}
		return file.toURI();
	}

	/**
	 * handle the GET request, print the header of response, and return the URI of
	 * file. Need to enter the name of file
	 * 
	 * @param myClient
	 * @return URI
	 * @throws IOException
	 */
	private static URI doGETRequest(HttpClient client) throws IOException {
		screen.println("Reponse Header: \n");
		screen.print(client.getHeader() + "\n");
		screen.flush();

		String statusCode = client.getStatusCode();
		if (statusCode.equals("200")) { // Get the file successfully
			screen.println();
			screen.print("Enter the name of the file to save: ");
			screen.flush();
			String path = keyboard.readLine();

			/**
			 * Save the response to the specified file.
			 */
			return writeFile(client.getResponse(), path);
		} else {
			return getBadRequest(statusCode, client.getResponse());
		}
	}

	/**
	 * handle the GET request, print the header of response, and return the URI of
	 * file. No need to enter the name of file.
	 * 
	 * @param myClient
	 * @param path
	 * @return URI
	 * @throws IOException
	 */
	private static URI doGETRequest(HttpClient client, String path) throws IOException {
		screen.println("Reponse Header: \n");
		screen.print(client.getHeader() + "\n");
		screen.flush();

		String statusCode = client.getStatusCode();
		if (statusCode.equals("200")) { // If get the file successfully
			/**
			 * Save the response to the specified file.
			 */
			return writeFile(client.getResponse(), path);
		} else {
			return getBadRequest(statusCode, client.getResponse());
		}
	}

	/**
	 * print the header of response.
	 * 
	 * @param client
	 */
	private static void doPUTRequest(HttpClient client) {
		screen.println("Reponse Header: \n");
		screen.print(client.getHeader() + "\n");
		screen.flush();
	}

	/**
	 * resolve the HTML file and get the list of files that are contained in HTML
	 * file.
	 * 
	 * @param HTMLFile
	 * @return List<String>
	 * @throws IOException
	 */
	private static List<String> resolveHTML(String HTMLFile) throws IOException {
		List<String> fileNames = new ArrayList<String>();
		BufferedReader br = new BufferedReader(
				new InputStreamReader(new ByteArrayInputStream(HTMLFile.getBytes("ISO-8859-1")), "ISO-8859-1"));
		String line;
		while ((line = br.readLine()) != null) {
			int index = 0;
			int formerIndex = 0;
			while ((index = line.indexOf("src=\"", formerIndex)) != -1) {
				// Get the file that is behind "src"
				StringBuilder fileName = new StringBuilder();
				boolean flag = false;
				for (int i = index; i < line.length(); i++) {
					if (!flag && line.charAt(i) == '\"') {
						flag = true;
						continue;
					} else if (flag && line.charAt(i) == '\"') {
						break;
					} else if (flag) {
						fileName.append(line.charAt(i));
					}

				}
				formerIndex = index + 4;
				fileNames.add(fileName.toString());
			}

			formerIndex = line.length() - 1;
			while ((index = line.lastIndexOf(".css\"", formerIndex)) != -1) {
				// Get the css file
				StringBuilder fileName = new StringBuilder();
				boolean flag = false;
				for (int i = index + 4; i >= 0; i--) {
					if (!flag && line.charAt(i) == '\"') {
						flag = true;
						continue;
					} else if (flag && line.charAt(i) == '\"') {
						break;
					} else if (flag) {
						fileName.append(line.charAt(i));
					}

				}
				formerIndex = index - 1;
				fileNames.add(fileName.reverse().toString());
			}
		}
		return fileNames;
	}

	/**
	 * brower the file
	 * 
	 * @param url
	 */
	private static void brower(URI url) {
		if (java.awt.Desktop.isDesktopSupported()) {
			try {
				java.awt.Desktop dp = java.awt.Desktop.getDesktop();
				if (dp.isSupported(java.awt.Desktop.Action.BROWSE)) {
					dp.browse(url);
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * handle the GET request of HTML file.
	 * 
	 * @param client
	 * @param request
	 * @param host
	 * @throws Exception
	 */
	private static void doHTMLRequests(HttpClient client, String request, String host) throws Exception {
		client.processGetRequest(request, false);
		URI uri = doGETRequest(client);
		List<String> fileNames = resolveHTML(client.getResponse());
		client.clearHeader(); // Clear the header
		client.clearResponse(); // Clear the response
		if (fileNames.size() > 0) {
			// If the HTML file contain other files
			client.close(); // Close the HTTPClient
			client = new HttpClient();
			client.connect(host); // reconnect
			for (int i = 0; i < fileNames.size(); i++) {
				// Get the files that are contained in HTML file
				String fileName = fileNames.get(i);
				System.out.println("File Name: " + fileName);
				request = "GET /" + fileName + " HTTP/1.1";
				System.out.println(request);
				System.out.println();
				if (i != fileNames.size() - 1) {
					client.processGetRequest(request, true);
					doGETRequest(client, fileName);
					client.clearHeader();
					client.clearResponse();
				} else {
					client.processGetRequest(request, false);
					// When get the last file, do not need to keep connection alive.
					doGETRequest(client, fileName);
				}
			}
		}
		brower(uri);
	}

	public static void main(String[] args) {
		try {
			/**
			 * Create a new HttpClient object.
			 */
			HttpClient myClient = new HttpClient();

			/**
			 * Parse the input arguments.
			 */
			if (args.length != 1) {
				System.err.println("Usage: Client <server>");
				System.exit(0);
			}

			/**
			 * Connect to the input server
			 */
			myClient.connect(args[0]);

			/**
			 * Read the get request from the terminal.
			 */
			screen.println(args[0] + " is listening to your request:");
			String request = keyboard.readLine();

			if (request.startsWith("GET")) {
				/**
				 * Ask the client to process the GET request.
				 */
				try {
					String[] tokens = request.split("\\s+");
					if (tokens[1].indexOf("html") != -1 || tokens[1].indexOf("htm") != -1 || tokens[1].equals("/")) {
						// If it is a HTML file
						doHTMLRequests(myClient, request, args[0]);
					} else {
						myClient.processGetRequest(request, false);
						URI uri = doGETRequest(myClient);
						brower(uri);
					}
				} catch (IOException e) {
					screen.println("Bad request! \n");
					return;
				}
			} else if (request.startsWith("PUT")) {
				/**
				 * Ask the client to process the PUT request.
				 */
				myClient.processPutRequest(request);
				doPUTRequest(myClient);
			} else {
				/**
				 * Do not process other request.
				 */
				screen.println("Bad request! \n");
				myClient.close();
				return;
			}

			myClient.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
