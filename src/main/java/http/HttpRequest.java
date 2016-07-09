package http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import util.HttpRequestUtils;
import util.IOUtils;

public class HttpRequest {
	private static final Logger log = LoggerFactory.getLogger(HttpRequest.class);
	BufferedReader br;
	Map<String, String> header = new HashMap<>();
	Map<String, String> params;
	RequestLine rl;
	
	public HttpRequest(InputStream in) throws IOException {
		 br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
		 
		 String line = br.readLine();
		 if (line == null) return;
		 
		 rl = new RequestLine(line);
		 
		 while (!"".equals((line = br.readLine()))) {
			 String[] tokens = line.split(": ");
			 if (tokens.length == 2) {
				 header.put(tokens[0], tokens[1]);
			 }
		 }
		 
		 if (rl.isPost()) {
			 String contentLength = header.get("Content-Length");
			 if (contentLength == null) {
				 throw new RuntimeException();
			 }
			 
			 String body = IOUtils.readData(br, Integer.parseInt(contentLength));
			 params = HttpRequestUtils.parseQueryString(body);
		 } else {
			 params = rl.getParameter();
		 }
	}

	public HttpMethod getMethod() {
		return rl.getMethod();
	}

	public String getPath() {
		return rl.getPath();
	}

	public String getHeader(String key) {
		return header.get(key);
	}

	public String getParameter(String key) {
		return params.get(key);
	}
}
