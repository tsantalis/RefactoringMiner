package org.refactoringminer;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.refactoringminer.api.GitHistoryRefactoringMiner;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

public class RefactoringMinerHttpServer {

	private static final ExecutorService MINER_POOL = new ThreadPoolExecutor(
			4, 16, 60L, TimeUnit.SECONDS,
			new ArrayBlockingQueue<>(200),
			new ThreadPoolExecutor.CallerRunsPolicy()
	);
	public static void main(String[] args) throws Exception {
		Properties prop = new Properties();
		try {
			InputStream input = new FileInputStream("server.properties");
			prop.load(input);
		} catch (Exception ignored) {}
		String hostName = prop.getProperty("hostname", System.getenv("hostname"));
		int port = Integer.parseInt(prop.getProperty("port", System.getenv("port")));
		
		InetSocketAddress inetSocketAddress = new InetSocketAddress(InetAddress.getByName(hostName), port);
		HttpServer server = HttpServer.create(inetSocketAddress, 0);
		server.createContext("/RefactoringMiner", (HttpExchange exchange) -> {
			printRequestInfo(exchange);
			URI requestURI = exchange.getRequestURI();
			String query = requestURI.getQuery();
			Map<String, String> queryToMap = queryToMap(query);

			String gitURL = queryToMap.get("gitURL");
			String commitId = queryToMap.get("commitId");
			String oAuthToken = queryToMap.getOrDefault("token", "");
			int timeout = Integer.parseInt(queryToMap.get("timeout"));
			List<Refactoring> detectedRefactorings = new ArrayList<Refactoring>();

			GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
			if (!oAuthToken.isEmpty()) {
				miner.connectToGitHub(oAuthToken);
			}
			Future<?> future = MINER_POOL.submit(() -> miner.detectAtCommit(gitURL, commitId, (commitId1, refactorings) -> detectedRefactorings.addAll(refactorings), timeout));
			int responseStatusCode = 200;
			try {
				try {
					future.get(Math.max(timeout, 30L), TimeUnit.SECONDS);
				} catch (InterruptedException e) {
					responseStatusCode = 503;
					Thread.currentThread().interrupt();
					throw new RuntimeException("Handler interrupted");
				} catch (TimeoutException te) {
					responseStatusCode = 503;
					future.cancel(true);
					throw new RuntimeException("Server-side timeout");
				}
				String response = JSON(gitURL, commitId, detectedRefactorings);
				System.out.println(response);
				exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
				exchange.sendResponseHeaders(200, response.length());
				OutputStream os = exchange.getResponseBody();
				os.write(response.getBytes());
				os.close();
			} catch (Exception e) {
				String error = "{\"error\":\"" + e.getMessage().replace("\"", "'") + "\"}";
				exchange.sendResponseHeaders(responseStatusCode == 200 ? 500 : responseStatusCode, error.length());
				OutputStream os = exchange.getResponseBody();
				os.write(error.getBytes());
				os.close();
			} finally {
				exchange.close();
			}
		});
		server.setExecutor(new ThreadPoolExecutor(4, 8, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(100), new ThreadPoolExecutor.CallerRunsPolicy()));
		server.start();
		System.out.println(InetAddress.getLocalHost());
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
