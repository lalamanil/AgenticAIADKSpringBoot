package com.agenticai.adk.app.agents;
/** @author lalamanil **/
import com.agenticai.adk.app.dto.AgentRequest;
import com.agenticai.adk.app.dto.AgentResponse;

public interface Agents {

	public AgentResponse runbaseAgent(AgentRequest request);

}
