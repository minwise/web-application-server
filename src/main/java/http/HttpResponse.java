package http;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpResponse {
	private static final Logger log = LoggerFactory.getLogger(HttpResponse.class);
	
	DataOutputStream dos;
	Map<String, String> header = new HashMap<>();

	public HttpResponse(OutputStream out) {
		dos = new DataOutputStream(out);
	}

	public void forward(String url) {
		byte[] body;
		try {
			body = Files.readAllBytes(new File("./webapp" + url).toPath());
			
			if (url.endsWith(".css")) {
				header.put("Content-Type", "text/css;charset=utf-8");
			} else if (url.endsWith(".js")) {
				header.put("Content-Type", "application/javascript");
			} else {
				header.put("Content-Type", "text/html;charset=utf-8");
			}
			header.put("Content-Length", body.length + "");
			
			response200Header();
			responseBody(body);
		} catch (IOException e) {
			log.error(e.getMessage());
		}
	}
	
	public void forwardBody(String responseBody) {
		byte[] body = responseBody.getBytes();
		
		header.put("Content-Type", "text/html;charset=utf-8");
		header.put("Content-Length", body.length + "");
		response200Header();
		responseBody(body);
	}

	public void sendRedirect(String url) {
		try {
			dos.writeBytes("HTTP/1.1 302 Redirect \r\n");
			processHeaders();
			dos.writeBytes("Location: " + url + "\r\n");
			dos.writeBytes("\r\n");
		} catch (IOException e) {
			log.error(e.getMessage());
		}
	}

	public void addHeader(String key, String value) {
		header.put(key, value);
	}

	private void response200Header() {
		try {
			dos.writeBytes("HTTP/1.1 200 OK \r\n");
			processHeaders();
			dos.writeBytes("\r\n");
		} catch (Exception e) {
			log.error(e.getMessage());
		}
	}
	
	private void responseBody(byte[] body) {
		try {
			dos.write(body, 0, body.length);
			dos.writeBytes("\r\n");
			dos.flush();
		} catch (IOException e) {
			log.error(e.getMessage());
		}
	}

	private void processHeaders() {
		try {
			Set<String> keys = header.keySet();
			for (String key : keys) {
				dos.writeBytes(key + ": " + header.get(key) + " \r\n");
			}
		} catch (IOException e) {
			log.error(e.getMessage());
		}
		
	}
}
