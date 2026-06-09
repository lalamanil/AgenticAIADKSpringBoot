package com.agenticai.adk.app.dto;
/** @author lalamanil **/
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JsonRPCRequestModel {

	@JsonProperty("jsonrpc")
	private String jsonrpc;
	@JsonProperty("id")
	private int id;
	@JsonProperty("method")
	private String method;
	@JsonProperty("params")
	private Params params;

	public String getJsonrpc() {
		return jsonrpc;
	}

	public void setJsonrpc(String jsonrpc) {
		this.jsonrpc = jsonrpc;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public Params getParams() {
		return params;
	}

	public void setParams(Params params) {
		this.params = params;
	}

}
