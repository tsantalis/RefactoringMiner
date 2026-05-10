package org.refactoringminer;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringHandler;
import org.refactoringminer.api.RefactoringMinerTimedOutException;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import javax.annotation.Nonnull;

public class RefactoringMinerHttpServer implements RefactoringHandler {

	private static final ExecutorService MINER_POOL = new ThreadPoolExecutor(
			4, 16, 60L, TimeUnit.SECONDS,
			new ArrayBlockingQueue<>(200),
			new ThreadPoolExecutor.CallerRunsPolicy()
	);
	private final GitHistoryRefactoringMinerImpl api = new GitHistoryRefactoringMinerImpl();
	private final List<Refactoring> detectedRefactorings = new ArrayList<>();
	private final Future<?> future;
	private final String gitURL, commitId;
	private final int timeout;
	private Throwable error = null;
	private int responseCode = 0;
	private String responseMessage;

	RefactoringMinerHttpServer(String url, String sha, int timeout, String token) {
		gitURL = url;
		commitId = sha;
		this.timeout = timeout;
		if (!token.isEmpty()) {
			api.connectToGitHub(token);
		}
		future = MINER_POOL.submit(this::act);
	}

	static RefactoringMinerHttpServer from(HttpExchange exchange) {
		printRequestInfo(exchange);
		URI requestURI = exchange.getRequestURI();
		String query = requestURI.getQuery();
		Map<String, String> queryToMap = queryToMap(query);

		return new RefactoringMinerHttpServer(queryToMap.get("gitURL"), queryToMap.get("commitId"), Integer.parseInt(queryToMap.get("timeout")), queryToMap.getOrDefault("token", ""));
	}

	public void sendResponse(HttpExchange exchange) throws IOException {
		assert responseCode != 0 : "Please, invoke prepareResponse before sending a response";
		exchange.sendResponseHeaders(responseCode, responseMessage.length());
		OutputStream os = exchange.getResponseBody();
		os.write(responseMessage.getBytes());
		os.close();
	}

	private void act() {
		api.detectRefactorings(this, gitURL, commitId);
	}

	void prepareResponse() {
		try {
			future.get(timeout, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			error = e;
		} catch (ExecutionException e) {
			error = e.getCause();
		} catch (Exception e) {
			error = e;
		}
		if (error == null) {
			responseMessage = JSON(gitURL, commitId, detectedRefactorings);
			responseCode = 200;
		} else {
			responseMessage = "{\"error\":\"" + error.getMessage().replace("\"", "'") + "\"}";
			if (error instanceof RefactoringMinerTimedOutException || error instanceof TimeoutException) {
				future.cancel(true);
				responseCode = 503;
			} else {
				responseCode = 500;
			}
		}
	}

	@Override
	public void handle(String commitId, List<Refactoring> refactorings) {
		detectedRefactorings.addAll(refactorings);
	}

	@Override
	public void handleException(String commitId, Exception e) {
		error = e;
	}

	public static void main(String[] args) throws Exception {
		Properties prop = getProperties();
        HttpServer server = HttpServer.create(setupSocket(prop), 0);
		server.createContext("/RefactoringMiner", (HttpExchange exchange) -> {
			exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
			RefactoringMinerHttpServer miner = RefactoringMinerHttpServer.from(exchange);
			miner.prepareResponse();
			miner.sendResponse(exchange);
		});
		server.setExecutor(new ThreadPoolExecutor(4, 8, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(100), new ThreadPoolExecutor.CallerRunsPolicy()));
		server.start();
		System.out.println(InetAddress.getLocalHost());
	}

	@Nonnull
	public static InetSocketAddress setupSocket(Properties prop) throws UnknownHostException {
		String hostName = prop.getProperty("hostname", System.getenv("hostname"));
		int port = Integer.parseInt(prop.getProperty("port", System.getenv("port")));

		InetSocketAddress inetSocketAddress = new InetSocketAddress(InetAddress.getByName(hostName), port);
		return inetSocketAddress;
	}

	@Nonnull
	public static Properties getProperties() {
		Properties prop = new Properties();
		try {
			InputStream input = new FileInputStream("server.properties");
			prop.load(input);
		} catch (Exception ignored) {}
		return prop;
	}

	private static Map<String, String> queryToMap(String query) {
		Map<String, String> result = new HashMap<>();
		for (String param : query.split("&")) {
			String[] entry = param.split("=");
			if (entry.length > 1) {
				result.put(entry[0], entry[1]);
			}
			else {
				result.put(entry[0], "");
			}
		}
		return result;
	}

	private static String JSON(String gitURL, String currentCommitId, List<Refactoring> refactoringsAtRevision) {
		StringBuilder sb = new StringBuilder();
		sb.append("{").append("\n");
		sb.append("\"").append("commits").append("\"").append(": ");
		sb.append("[");
		sb.append("{");
		sb.append("\t").append("\"").append("repository").append("\"").append(": ").append("\"").append(gitURL).append("\"").append(",").append("\n");
		sb.append("\t").append("\"").append("sha1").append("\"").append(": ").append("\"").append(currentCommitId).append("\"").append(",").append("\n");
		String url = GitHistoryRefactoringMinerImpl.extractCommitURL(gitURL, currentCommitId);
		sb.append("\t").append("\"").append("url").append("\"").append(": ").append("\"").append(url).append("\"").append(",").append("\n");
		sb.append("\t").append("\"").append("refactorings").append("\"").append(": ");
		sb.append("[");
		int counter = 0;
		for(Refactoring refactoring : refactoringsAtRevision) {
			sb.append(refactoring.toJSON());
			if(counter < refactoringsAtRevision.size()-1) {
				sb.append(",");
			}
			sb.append("\n");
			counter++;
		}
		sb.append("]");
		sb.append("}");
		sb.append("]").append("\n");
		sb.append("}");
		return sb.toString();
	}

	private static void printRequestInfo(HttpExchange exchange) {
		System.out.println("-- headers --");
		Headers requestHeaders = exchange.getRequestHeaders();
		requestHeaders.entrySet().forEach(System.out::println);

		System.out.println("-- HTTP method --");
		String requestMethod = exchange.getRequestMethod();
		System.out.println(requestMethod);

		System.out.println("-- query --");
		URI requestURI = exchange.getRequestURI();
		String query = requestURI.getQuery();
		System.out.println(query);
	}
}
