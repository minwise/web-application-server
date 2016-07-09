package http;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import util.HttpRequestUtils;

public class RequestLine {
	private static final Logger log = LoggerFactory.getLogger(RequestLine.class);
	private HttpMethod method;
	private String url;
	private String path;
	Map<String, String> params;
	
	public RequestLine(String line) {
		String[] tokens = line.split(" ");
		
		method = HttpMethod.valueOf(tokens[0]);
		url = tokens[1];
		
		if (method.isPost()) {
			path = url;
		} else {
			int idx = tokens[1].indexOf("?");
			if (idx == -1) {
				path = url;
				params = new HashMap<>();
			} else {
				path = url.substring(0, idx);
				String parameters = url.substring(idx + 1);
				params = HttpRequestUtils.parseQueryString(parameters);
			}
		}
	}
	
	public boolean isGet() {
		return !method.isPost();
	}
	
	public boolean isPost() {
		return method.isPost();
	}
	
	public HttpMethod getMethod() {
		return method;
	}
	
	public String getPath() {
		return path;
	}
	
	public Map<String, String> getParameter() {
		return params;
	}
	
	public String getUrl() {
		return url;
	}
}
