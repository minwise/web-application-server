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
	HttpMethod method;
	String url;
	String path;
	Map<String, String> header = new HashMap<>();
	Map<String, String> params = new HashMap<>();
	
	public HttpRequest(InputStream in) throws IOException {
		 br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
		 
		 String line = br.readLine();
		 if (line == null) return;
		 
		 String[] tokens = line.split(" ");
		 method = HttpMethod.valueOf(tokens[0]);
		 url = tokens[1];
		 
		 while (!"".equals((line = br.readLine()))) {
			 tokens = line.split(": ");
			 if (tokens.length == 2) {
				 header.put(tokens[0], tokens[1]);
			 }
		 }
		 
		 if (method.isPost()) {
			 path = url;
			 String contentLength = header.get("Content-Length");
			 if (contentLength == null) {
				 throw new RuntimeException();
			 }
			 
			 String body = IOUtils.readData(br, Integer.parseInt(contentLength));
			 params = HttpRequestUtils.parseQueryString(body);
		 } else {
			 int idx = url.indexOf("?");
			 if (idx == -1) {
				 path = url;
			 } else {
				 path = url.substring(0, idx);
				 String parameters = url.substring(idx + 1);
				 params = HttpRequestUtils.parseQueryString(parameters);
			 }
		 }
	}

	public HttpMethod getMethod() {
		return this.method;
	}

	public String getPath() {
		return this.path;
	}

	public String getHeader(String key) {
		return header.get(key);
	}

	public Object getParameter(String key) {
		return params.get(key);
	}
}
