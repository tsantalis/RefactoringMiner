package org.refactoringminer.mcp;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;

public final class RefactoringMinerMcpServer {
	private static final String SMOKE_ARG = "--smoke";

	private RefactoringMinerMcpServer() {
	}

	public static void main(String[] args) throws Exception {
		if (isSmoke(args)) {
			McpSyncServer server = createServer();
			server.close();
			return;
		}

		McpSyncServer server = createServer();
		Runtime.getRuntime().addShutdownHook(new Thread(server::close, "refactoringminer-mcp-shutdown"));
		new CountDownLatch(1).await();
	}

	private static McpSyncServer createServer() {
		StdioServerTransportProvider transportProvider =
				new StdioServerTransportProvider(new JacksonMcpJsonMapper(new ObjectMapper()));
		return McpServer.sync(transportProvider)
				.serverInfo("refactoringminer", version())
				.capabilities(ServerCapabilities.builder().tools(true).logging().build())
				.tools(RefactoringMinerMcpTools.toolSpecifications())
				.build();
	}

	private static boolean isSmoke(String[] args) {
		return args.length == 1 && Arrays.asList(args).contains(SMOKE_ARG);
	}

	private static String version() {
		String implementationVersion = RefactoringMinerMcpServer.class.getPackage().getImplementationVersion();
		if (implementationVersion == null || implementationVersion.isBlank()) {
			return "development";
		}
		return implementationVersion;
	}
}
