package com.agenticai.adk.app.dto;
/** @author lalamanil **/
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Params {

	@JsonProperty("name")
	private String name;

	@JsonProperty("arguments")
	private Map<String, Object> arguments;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Map<String, Object> getArguments() {
		return arguments;
	}

	public void setArguments(Map<String, Object> arguments) {
		this.arguments = arguments;
	}

}
