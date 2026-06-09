/** @author lalamanil **/
package com.agenticai.adk.app.agents.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.agenticai.adk.app.agents.Agents;
import com.agenticai.adk.app.agents.constants.ApplicationConstants;
import com.agenticai.adk.app.dto.AgentRequest;
import com.agenticai.adk.app.dto.AgentResponse;
import com.agenticai.adk.app.exception.AgentApplicationException;
import com.agenticai.adk.app.mcp.bridge.McpToolBridge;
import com.google.adk.agents.LlmAgent;
import com.google.adk.agents.RunConfig;
import com.google.adk.events.Event;
import com.google.adk.models.Gemini;
import com.google.adk.models.VertexCredentials;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.sessions.Session;
import com.google.adk.tools.FunctionTool;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import io.reactivex.rxjava3.core.Flowable;
import jakarta.annotation.PostConstruct;

@Service
public class AgentsImpl implements Agents {

	private static final Logger LOGGER = LoggerFactory.getLogger(AgentsImpl.class);

	private LlmAgent baseAgent;
	private final McpToolBridge mcpToolBridge;
	RunConfig runConfig;
	private InMemoryRunner runner;

	@Autowired(required = true)
	public AgentsImpl(McpToolBridge mcpToolBridge) {
		this.mcpToolBridge = mcpToolBridge;
	}

	@PostConstruct
	public void initializeAgents() {

		try (InputStream inputStream = AgentsImpl.class.getClassLoader()
				.getResourceAsStream("AI-ServiceAccount.json")) {
			if (null == inputStream) {
				LOGGER.error(
						"inputstream for resource AI-ServiceAccount.json is null. Please check the service account file in src/main/resources");
				throw new AgentApplicationException("SERVICE_ACCOUNT_NOT_FOUND",
						"AI-ServiceAccount.json not found in classpath");
			} else {
				GoogleCredentials googleCredentials = GoogleCredentials.fromStream(inputStream)
						.createScoped(List.of("https://www.googleapis.com/auth/cloud-platform"));
				VertexCredentials vertexCredentials = VertexCredentials.builder().setCredentials(googleCredentials)
						.setProject(ApplicationConstants.PROJECT_NAME).setLocation(ApplicationConstants.LOCATION)
						.build();
				Gemini gemini = Gemini.builder().vertexCredentials(vertexCredentials)
						.modelName(ApplicationConstants.MODEL).build();
				baseAgent = LlmAgent.builder().name("calculate-discount-agent").description(
						"Provides the how much ammount is saved after the discount is applied on original price")
						.instruction(
								"You are a discount calculation assistant.Always use the calculateDiscount tool to calculate discounts and savings.Never calculate discounts manually.")
						.model(gemini).tools(FunctionTool.create(mcpToolBridge, "calculateDiscount")).build();
				LOGGER.info("calculate-discount-agent initialized successfully");

				if (null != baseAgent) {
					runConfig = RunConfig.builder().build();
					runner = new InMemoryRunner(baseAgent);

				}
			}
		} catch (IOException e) {
			// TODO: handle exception
			LOGGER.error("Failed loading Google credentials", e);
			throw new AgentApplicationException("CREDENTIAL_LOAD_FAILED", e.getMessage());

		}

	}

	@Override
	public AgentResponse runbaseAgent(AgentRequest agentRequest) {

		if (null == baseAgent) {
			throw new AgentApplicationException("AGENT_NOT_INITIALIZED", "Base agent is not initialized");
		}
		AgentResponse agentResponse = new AgentResponse();
		Session session = runner.sessionService().createSession(runner.appName(), "anillalam123").blockingGet();
		Content userMsg = Content.fromParts(Part.fromText(agentRequest.getUserQuery()));
		Flowable<Event> events = runner.runAsync(session.userId(), session.id(), userMsg, runConfig);
		events.blockingForEach(event -> {
			LOGGER.info(event.stringifyContent());	
			if (event.finalResponse()) {
				agentResponse.setResult(event.stringifyContent());
			}
		});
		return agentResponse;
	}

}
