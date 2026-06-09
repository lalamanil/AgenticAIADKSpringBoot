package com.agenticai.adk.app.dto;
/** @author lalamanil **/
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AgentRequest {

	@NotBlank(message = "userQuery is required and cannot be empty")
	@JsonProperty("userQuery")
	private String userQuery;

	public String getUserQuery() {
		return userQuery;
	}

	public void setUserQuery(String userQuery) {
		this.userQuery = userQuery;
	}

}
