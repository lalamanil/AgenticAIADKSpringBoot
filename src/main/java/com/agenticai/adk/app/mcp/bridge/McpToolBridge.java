package com.agenticai.adk.app.mcp.bridge;
/** @author lalamanil **/
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.agenticai.adk.app.mcp.tool.client.McpClient;
import com.google.adk.tools.Annotations.Schema;

@Component
public class McpToolBridge {

	private static final Logger LOGGER = LoggerFactory.getLogger(McpToolBridge.class);
	private final McpClient mcpClient;

	@Autowired(required = true)
	public McpToolBridge(McpClient mcpClient) {
		this.mcpClient = mcpClient;
	}

	@Schema(name = "calculate_discount", description = "Calculates the final price of an item after applying a promotional percentage discount.")
	public String calculateDiscount(
			@Schema(name = "originalPrice", description = "The original retail price before discounts") double originalPrice,
			@Schema(name = "discountPercentage", description = "Percentage to cut from price (e.g. 20.0)") double discountPercentage) {
		LOGGER.debug("In calculate_discount bridge");
		return mcpClient.callTool("calculate_discount",
				Map.of("originalPrice", originalPrice, "discountPercentage", discountPercentage));
	}

}
