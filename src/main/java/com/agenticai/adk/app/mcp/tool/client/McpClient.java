package com.agenticai.adk.app.mcp.tool.client;
/** @author lalamanil **/
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.agenticai.adk.app.agents.constants.ApplicationConstants;
import com.agenticai.adk.app.dto.JsonRPCRequestModel;
import com.agenticai.adk.app.dto.Params;
import com.agenticai.adk.app.exception.AgentApplicationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Component
public class McpClient {

	private static final Logger LOGGER = LoggerFactory.getLogger(McpClient.class);

	private final HttpClient httpClient;
	private final ObjectMapper objectMapper;
	private volatile CompletableFuture<String> sessionFuture;
	private final Map<Integer, CompletableFuture<JsonNode>> pendingResponse;
	private final ExecutorService executorService;
	private final AtomicInteger requestCounter;

	public McpClient() {
		httpClient = HttpClient.newBuilder().version(Version.HTTP_2).build();
		objectMapper = new ObjectMapper();
		executorService = Executors.newSingleThreadExecutor();
		pendingResponse = new ConcurrentHashMap<Integer, CompletableFuture<JsonNode>>();
		requestCounter = new AtomicInteger(0);
		sessionFuture = new CompletableFuture<String>();
	}

	@PostConstruct
	public void protocolSetUp() {
		connect();
		boolean isInitialized = initialize();
		if (isInitialized) {
			// Notification/initialization call
			boolean isNotified = notification();
			if (isNotified) {
				// MCP call to list tools
				toolList();
			}
		}
	}

	@PreDestroy
	public void shutdown() {
		LOGGER.info("Shutting down MCP Client");
		executorService.shutdown();
	}

	public void connect() {
		int retryCount = 0;
		while (retryCount <= 3) {
			try {
				HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(ApplicationConstants.MCP_SSE_ENDPOINT))
						.GET().build();
				HttpResponse<InputStream> httpResponse = httpClient.send(httpRequest, BodyHandlers.ofInputStream());
				int statusCode = httpResponse.statusCode();
				if (200 != statusCode) {
					LOGGER.error("Failed to connect SSE endpoint. Status={}", statusCode);
					retryCount++;
					Thread.sleep(3000);
					continue;
				}
				startSseListener(httpResponse.body());
				LOGGER.info("Connected to SSE endpoint");
				return;
			} catch (Exception e) {
				// TODO: handle exception

				retryCount++;
				LOGGER.error("Error connecting SSE endpoint. Retry={}", retryCount, e);

				try {
					Thread.sleep(3000);
				} catch (InterruptedException ie) {
					// TODO: handle exception
					Thread.currentThread().interrupt();
					throw new AgentApplicationException("THREAD_INTERRUPTED", ie.getMessage());
				}
			}

		}

		throw new AgentApplicationException("SSE_CONNECTION_FAILED", "Unable to connect after retries");

	}

	public void startSseListener(InputStream inputStream) {

		executorService.submit(() -> {
			try (BufferedReader reader = new BufferedReader(
					new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
				String line;
				while ((line = reader.readLine()) != null) {
					if (line.startsWith("data:")) {
						String data = line.substring(5).trim();
						if (data.contains("sessionId=")) {
							String sessionid = data.substring(data.indexOf("sessionId=") + "sessionId=".length());
							if (null != sessionid) {
								sessionFuture.complete(sessionid);
							}

						} else if (data.startsWith("{")) {
							try {
								JsonNode jsonNode = objectMapper.readTree(data);
								if (jsonNode.has("id")) {
									int id = jsonNode.get("id").asInt();
									CompletableFuture<JsonNode> future = pendingResponse.remove(id);
									if (null != future) {
										future.complete(jsonNode);
									}

								}
							} catch (Exception e) {
								// TODO: handle exception
								LOGGER.info("Failed processing SSE event", e);

							}

						}
					}

				}
			} catch (Exception e) {
				// TODO: handle exception
				LOGGER.error("Fatal SSE listener failure", e);
				pendingResponse.values().forEach(future -> future.completeExceptionally(e));
				pendingResponse.clear();
				sessionFuture.completeExceptionally(e);
				sessionFuture = new CompletableFuture<String>();
				connect();

			}

		});

	}

	public JsonNode sendRequest(int id, String jsonRpcRequest) {
		JsonNode responseJsonNode = null;
		CompletableFuture<JsonNode> future = new CompletableFuture<JsonNode>();
		pendingResponse.put(id, future);
		try {
			String sessionId = sessionFuture.get(10, TimeUnit.SECONDS);
			if (null != sessionId) {
				HttpRequest httpRequest = HttpRequest
						.newBuilder(URI.create(ApplicationConstants.MCP_SEND_REQUEST_ENDPOINT + sessionId))
						.header("Content-Type", "application/json").POST(BodyPublishers.ofString(jsonRpcRequest))
						.build();
				HttpResponse<Void> httpResponse = httpClient.send(httpRequest, BodyHandlers.discarding());
				if (null != httpResponse) {
					int statusCode = httpResponse.statusCode();
					if (200 == statusCode) {
						responseJsonNode = future.get(30, TimeUnit.SECONDS);
					}
				}
			} else {
				LOGGER.info("sessionid is null.Not able to call MCP JsonRPC calls");
			}

		} catch (IllegalArgumentException e) {
			// TODO: handle exception
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO: handle exception
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO: handle exception
			e.printStackTrace();
		} catch (TimeoutException e) {
			// TODO: handle exception
			e.printStackTrace();
		} catch (IOException e) {
			// TODO: handle exception
			e.printStackTrace();
		} finally {
			pendingResponse.remove(id);
		}

		return responseJsonNode;

	}

	public boolean initialize() {
		boolean initializeFlag = Boolean.FALSE;
		int id = requestCounter.incrementAndGet();
		Map<String, Object> initializeRequest = Map.of("jsonrpc", "2.0", "id", id, "method", "initialize", "params",
				Map.of("protocolVersion", "2024-11-05", "capabilities", Map.of(), "clientInfo",
						Map.of("name", "google-adk-client", "version", "1.0")));
		try {
			String initializeJsonRPCRequest = objectMapper.writeValueAsString(initializeRequest);
			LOGGER.info("initialize request payload jsonrpc format:" + initializeJsonRPCRequest);
			JsonNode responseJsonNode = sendRequest(id, initializeJsonRPCRequest);
			initializeFlag = null != responseJsonNode ? true : false;
		} catch (JsonProcessingException e) {
			// TODO: handle exception
			LOGGER.error("JsonProcessingException converting java object to json", e);
		} catch (Exception e) {
			// TODO: handle exception
			LOGGER.error(e.getMessage(), e);
		}
		return initializeFlag;
	}

	public boolean notification() {
		boolean notificationFlag = Boolean.FALSE;
		Map<String, Object> notificationRequest = Map.of("jsonrpc", "2.0", "method", "notifications/initialized");
		try {
			String notificatioPayload = objectMapper.writeValueAsString(notificationRequest);
			String sessionId = sessionFuture.get(10, TimeUnit.SECONDS);
			HttpRequest httpRequest = HttpRequest
					.newBuilder(URI.create(ApplicationConstants.MCP_SEND_REQUEST_ENDPOINT + sessionId))
					.header("Content-Type", "application/json").POST(BodyPublishers.ofString(notificatioPayload))
					.build();
			HttpResponse<Void> httpResponse = httpClient.send(httpRequest, BodyHandlers.discarding());
			if (null != httpResponse) {
				if (200 == httpResponse.statusCode()) {
					notificationFlag = Boolean.TRUE;
				}
			}

		} catch (JsonProcessingException e) {
			// TODO: handle exception
			LOGGER.error(e.getMessage(), e);
		} catch (IllegalArgumentException e) {
			// TODO: handle exception
			LOGGER.error(e.getMessage(), e);
		} catch (ExecutionException e) {
			// TODO: handle exception
			LOGGER.error(e.getMessage(), e);
		} catch (TimeoutException e) {
			// TODO: handle exception
			LOGGER.error(e.getMessage(), e);
		} catch (InterruptedException e) {
			// TODO: handle exception
			LOGGER.error(e.getMessage(), e);
		} catch (IOException e) {
			// TODO: handle exception
			LOGGER.error(e.getMessage(), e);
		}

		return notificationFlag;

	}

	public void toolList() {
		int id = requestCounter.incrementAndGet();
		Map<String, Object> toolListRequest = Map.of("jsonrpc", "2.0", "id", id, "method", "tools/list");
		try {
			String jsonrpcToolRequest = objectMapper.writeValueAsString(toolListRequest);
			JsonNode responseJsonNode = sendRequest(id, jsonrpcToolRequest);
			if (null != responseJsonNode) {
				JsonNode result = responseJsonNode.get("result");
				if (null != result) {
					String resultJsonResponseBody = objectMapper.writerWithDefaultPrettyPrinter()
							.writeValueAsString(result);
					LOGGER.info("tool List:" + resultJsonResponseBody);
				}
			}
		} catch (JsonProcessingException e) {
			// TODO: handle exception
			LOGGER.error(e.getMessage(), e);
		}

	}

	public String callTool(String toolName, Map<String, Object> arguments) {
		String result = null;
		int id = requestCounter.incrementAndGet();
		JsonRPCRequestModel jsonRPCRequestModel = new JsonRPCRequestModel();
		jsonRPCRequestModel.setJsonrpc("2.0");
		jsonRPCRequestModel.setId(id);
		jsonRPCRequestModel.setMethod("tools/call");

		Params params = new Params();
		params.setName(toolName);
		params.setArguments(arguments);
		jsonRPCRequestModel.setParams(params);

		try {
			String toolCallRequest = objectMapper.writeValueAsString(jsonRPCRequestModel);
			LOGGER.info("toolCallRequestPayLoad:" + toolCallRequest);
			JsonNode responseJsonNode = sendRequest(id, toolCallRequest);
			result = getResultFromResponse(responseJsonNode);
		} catch (Exception e) {
			// TODO: handle exception
			LOGGER.error(e.getMessage(), e);
			result = "MCP tool invocation failed: " + e.getMessage();
		}
		if (null == result) {
			result = "No results found";
		}
		return result;
	}

	public String getResultFromResponse(JsonNode responseJsonNode) {
		String result = null;
		if (null != responseJsonNode) {
			JsonNode resultJsonNode = responseJsonNode.get("result");
			if (null != resultJsonNode) {
				JsonNode contentJsonNode = resultJsonNode.get("content");
				if (null != contentJsonNode && contentJsonNode.isArray() && !contentJsonNode.isEmpty()) {
					JsonNode firstContent = contentJsonNode.get(0);
					if (null != firstContent) {
						JsonNode textContent = firstContent.get("text");
						if (null != textContent) {
							result = textContent.asText();
						}
					}
				}

			}

		}
		return result;
	}

}
