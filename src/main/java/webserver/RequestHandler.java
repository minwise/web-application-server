package webserver;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestHandler extends Thread {
	private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);
	
	private Socket connection;

	public RequestHandler(Socket connectionSocket) {
		this.connection = connectionSocket;
	}

	public void run() {
		log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(), connection.getPort());
		
		try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
			DataOutputStream dos = new DataOutputStream(out);
			BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
			
			Map<String, String> header = readHttpHeader(br);
			log.debug("header => {}", header.toString());
			
			byte[] body = Files.readAllBytes(new File("./webapp" + header.get("Url")).toPath());
			response200Header(dos, body.length);
			responseBody(dos, body);
		} catch (IOException e) {
			log.error(e.getMessage());
		}
	}
	
	private Map<String, String> readHttpHeader(BufferedReader br) throws IOException {
		Map<String, String> header = new HashMap<>();
		
		String line = br.readLine();
		if (line == null) {
			return null;
		}
		
		String[] firstInfo = line.split(" ");
		
		header.put("Method", firstInfo[0]);
		header.put("Url", firstInfo[1]);
		
		while (!"".equals((line = br.readLine()))) {
			makeHeader(header, line);
		}
		
		return header;
	}

	private void makeHeader(Map<String, String> header, String line) {
		String[] headerTokens = line.split(": ");
		if (headerTokens.length == 2)
			header.put(headerTokens[0], headerTokens[1]);
	}

	private void response200Header(DataOutputStream dos, int lengthOfBodyContent) {
		try {
			dos.writeBytes("HTTP/1.1 200 OK \r\n");
			dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
			dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
			dos.writeBytes("\r\n");
		} catch (IOException e) {
			log.error(e.getMessage());
		}
	}
	
	private void responseBody(DataOutputStream dos, byte[] body) {
		try {
			dos.write(body, 0, body.length);
			dos.flush();
		} catch (IOException e) {
			log.error(e.getMessage());
		}
	}
}
