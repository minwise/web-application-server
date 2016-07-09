package controller;

import java.util.HashMap;
import java.util.Map;

public class RequestMapper {
	Map<String, Controller> mapper = new HashMap<String, Controller>();
	
	public RequestMapper() {
		mapper.put("/user/create", new CreateUserController());
		mapper.put("/user/login", new LoginUserController());
		mapper.put("/user/list", new ListUserController());
	}
	
	public Controller getController(String url) {
		return mapper.get(url);
	}
}
