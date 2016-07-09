package webserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Collection;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controller.Controller;
import controller.RequestMapper;
import db.DataBase;
import http.HttpRequest;
import http.HttpResponse;
import model.User;
import util.HttpRequestUtils;

public class RequestHandler extends Thread {
	private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);

	private Socket connection;
	private RequestMapper requestMapper;

	public RequestHandler(Socket connectionSocket) {
		this.connection = connectionSocket;
	}

	public RequestHandler(Socket connectionSocket, RequestMapper rm) {
		this.connection = connectionSocket;
		this.requestMapper = rm;
	}

	public void run() {
		log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(), connection.getPort());

		try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
			HttpRequest request = new HttpRequest(in);
			HttpResponse response = new HttpResponse(out);
			
			String url = getDefaultUrl(request.getPath());
			Controller controller = requestMapper.getController(url);
			if (controller == null) {
				response.forward(url);
			} else {
				controller.service(request, response);
			}
		} catch (IOException e) {
			log.error(e.getMessage());
		}
	}

	private void listUser(HttpRequest request, HttpResponse response) {
		if (!isLogin(request.getHeader("Cookie"))) {
			log.debug("is not logined...");
			response.sendRedirect("/user/login.html");
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
		response.forwardBody(sb.toString());
	}

	private void loginUser(HttpRequest request, HttpResponse response) {
		User user = DataBase.findUserById(request.getParameter("userId"));
		if (user != null) {
			if (user.login(request.getParameter("password"))) {
				response.addHeader("Set-Cookie", "logined=true");
				response.sendRedirect("/index.html");
			} else {
				response.sendRedirect("/user/login_failed.html");
			}
		} else {
			response.sendRedirect("/user/login_failed.html");
		}
	}

	private void createUser(HttpRequest request, HttpResponse response) {
		User user = new User(request.getParameter("userId"), request.getParameter("password"), 
				request.getParameter("name"), request.getParameter("email"));
		DataBase.addUser(user);
		response.sendRedirect("/index.html");
	}

	private boolean isLogin(String cookieValue) {
		Map<String, String> cookies = HttpRequestUtils.parseCookies(cookieValue);
		String value = cookies.get("logined");
		if (value == null)
			return false;
		
		return Boolean.parseBoolean(value);
	}

	private String getDefaultUrl(String url) {
		if (url.equals("/")) {
			url = "/index.html";
		}
		return url;
	}

}
