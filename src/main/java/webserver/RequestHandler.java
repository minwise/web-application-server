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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import db.DataBase;
import model.User;
import util.HttpRequestUtils;

public class RequestHandler extends Thread {
	private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);
	
	private Socket connection;

	public RequestHandler(Socket connectionSocket) {
		this.connection = connectionSocket;
	}

	public void run() {
		log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(), connection.getPort());
		
		try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
			
			BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
			
			Map<String, String> header = readHttpHeader(br);
			if (header == null) {
				return;
			}
			log.debug("header => {}", header.toString());
			
			boolean logined = false;
			if (header.containsKey("Cookie")) {
				Map<String, String> cookies = HttpRequestUtils.parseCookies(header.get("Cookie"));
				String value = cookies.get("logined");
				if (value == null)
					logined = false;
				logined = Boolean.parseBoolean(value);
			}
			
			if (header.get("Url").equals("/user/create")) {
				if (header.get("Method").equals("GET")) {
					String url = header.get("Url");
					String params = getParamsFromGetUrl(url);
					
					User user = addUserFromParams(params);
					log.debug("[GET] user => {}", user.toString());
				} else if (header.get("Method").equals("POST")) {
					String contentLength = header.get("Content-Length");
					String params = util.IOUtils.readData(br, Integer.parseInt(contentLength));
					User user = addUserFromParams(params);
					log.debug("[POST] user => {}", user.toString());
					
					DataOutputStream dos = new DataOutputStream(out);
					response302Header(dos, "/index.html");
				}
			} else if (header.get("Url").equals("/user/login")) {
				if (header.get("Method").equals("POST")) {
					String contentLength = header.get("Content-Length");
					String params = util.IOUtils.readData(br, Integer.parseInt(contentLength));
					Map<String, String> userMap = util.HttpRequestUtils.parseQueryString(params);
					
					User user = DataBase.findUserById(userMap.get("userId"));
					if (user == null) {
						log.error("User is not found...");
						responseResource(out, "/user/login_failed.html");
					} else {
						if (user.getPassword().equals(userMap.get("password"))) {
							log.debug("login success!!");
							DataOutputStream dos = new DataOutputStream(out);
							response302HeaderWithCookie(dos, "/index.html", "logined=true");
						} else {
							log.debug("login failed...");
							DataOutputStream dos = new DataOutputStream(out);
							responseResource(out, "/user/login_failed.html");
							//response302HeaderWithCookie(dos, "/user/login_failed.html", "logined=false");
						}
					}
				}
			} else if (header.get("Url").equals("/user/list")) {
				if (!logined) {
					log.debug("you are not logged in!!!!!!!!!!!!!");
					responseResource(out, "/user/login.html");
					return;
				}
				
				Collection<User> users = DataBase.findAll();
				StringBuilder sb = new StringBuilder();
				sb.append("<table border='1'>");
				for (User user : users) {
					sb.append("<tr>");
					sb.append("<td>" + user.getUserId() + "</td>");
					sb.append("<td>" + user.getName() + "</td>");
					sb.append("<td>" + user.getEmail() + "</td>");
					sb.append("</tr>");
				}
				sb.append("</table>");
				byte[] body = sb.toString().getBytes();
				DataOutputStream dos = new DataOutputStream(out);
				response200Header(dos, body.length);
				responseBody(dos, body);
				log.debug("you are logged in!!");
			} else if (header.get("Url").endsWith(".css")) {
				responseCssResource(out, header.get("Url"));
			} else {
				log.debug("Request url : {}", header.get("Url"));
				responseResource(out, header.get("Url"));
			}
		} catch (IOException e) {
			log.error(e.getMessage());
		}
	}

	

	private User addUserFromParams(String params) {
		Map<String, String> userMap = util.HttpRequestUtils.parseQueryString(params);
		User user = new User(userMap.get("userId"), userMap.get("password"), 
				userMap.get("name"), userMap.get("email"));
		DataBase.addUser(user);
		return user;
	}
	
	private String getParamsFromGetUrl(String url) {
		int idx = url.indexOf("?");
		String requestPath = url.substring(0, idx);
		String params = url.substring(idx + 1);
		return params;
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
			String[] headerTokens = line.split(": ");
			if (headerTokens.length == 2)
				header.put(headerTokens[0], headerTokens[1]);
		}
		
		return header;
	}
	
	private void responseResource(OutputStream out, String url) throws IOException {
		DataOutputStream dos = new DataOutputStream(out);

		byte[] body = Files.readAllBytes(new File("./webapp" + url).toPath());
		response200Header(dos, body.length);
		responseBody(dos, body);
	}
	
	private void responseCssResource(OutputStream out, String url) throws IOException {
		DataOutputStream dos = new DataOutputStream(out);

		byte[] body = Files.readAllBytes(new File("./webapp" + url).toPath());
		response200CssHeader(dos, body.length);
		responseBody(dos, body);
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
	
	private void response200CssHeader(DataOutputStream dos, int lengthOfBodyContent) {
		try {
			dos.writeBytes("HTTP/1.1 200 OK \r\n");
			dos.writeBytes("Content-Type: text/css;charset=utf-8\r\n");
			dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
			dos.writeBytes("\r\n");
		} catch (IOException e) {
			log.error(e.getMessage());
		}
	}
	
	private void response302Header(DataOutputStream dos, String redirectUrl) {
		try {
			dos.writeBytes("HTTP/1.1 302 Redirect \r\n");
			dos.writeBytes("Location: " + redirectUrl + "\r\n");
			dos.writeBytes("\r\n");
		} catch (IOException e) {
			log.error(e.getMessage());
		}
	}
	
	private void response302HeaderWithCookie(DataOutputStream dos, String redirectUrl, String Cookie) {
		try {
			dos.writeBytes("HTTP/1.1 302 Redirect \r\n");
			dos.writeBytes("Location: " + redirectUrl + "\r\n");
			dos.writeBytes("Set-Cookie: " + Cookie + "\r\n");
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
