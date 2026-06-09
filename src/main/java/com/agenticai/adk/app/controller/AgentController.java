package com.agenticai.adk.app.controller;
/** @author lalamanil **/
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.agenticai.adk.app.agents.Agents;
import com.agenticai.adk.app.dto.AgentRequest;
import com.agenticai.adk.app.dto.AgentResponse;
import jakarta.validation.Valid;

@Controller
@RequestMapping(value = "/agent")
public class AgentController {

	private static final Logger LOGGER = LoggerFactory.getLogger(AgentController.class);

	@Autowired(required = true)
	private Agents agents;

	@RequestMapping(value = "/healthCheck", method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<String> healthCheck() {
		return ResponseEntity.ok("Health Check Sucessfull");
	}

	@RequestMapping(value = "/base", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public ResponseEntity<AgentResponse> baseAgent(@Valid @RequestBody(required = true) AgentRequest requestbody) {
		LOGGER.info("User Query: {}", requestbody.getUserQuery());
		return ResponseEntity.ok(agents.runbaseAgent(requestbody));
	}

}
