package org.refactoringminer;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;

import org.refactoringminer.api.GitHistoryRefactoringMiner;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringHandler;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;

public class RefactoringMinerHttpsServer {

	public static void main(String[] args) throws Exception {
		Properties prop = new Properties();
		InputStream input = new FileInputStream("server.properties");
		prop.load(input);
		String hostName = prop.getProperty("hostname");
		int port = Integer.parseInt(prop.getProperty("port"));
		String keystore = prop.getProperty("keystore");
		String keyStorePass = prop.getProperty("keystore-password");
		
		InetSocketAddress inetSocketAddress = new InetSocketAddress(InetAddress.getByName(hostName), port);
		HttpsServer server = HttpsServer.create(inetSocketAddress, 0);
		SSLContext sslContext = SSLContext.getInstance("TLS");

		// initialize the keystore
		char[] password = keyStorePass.toCharArray();
		KeyStore ks = KeyStore.getInstance("JKS");
		FileInputStream fis = new FileInputStream(keystore);
		ks.load(fis, password);

		// setup the key manager factory
		KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
		kmf.init(ks, password);

		// setup the trust manager factory
		TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
		tmf.init(ks);

		// setup the HTTPS context and parameters
		sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
		server.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
			public void configure(HttpsParameters params) {
				try {
					// initialize the SSL context
					SSLContext context = getSSLContext();
					SSLEngine engine = context.createSSLEngine();
					params.setNeedClientAuth(false);
					params.setCipherSuites(engine.getEnabledCipherSuites());
					params.setProtocols(engine.getEnabledProtocols());

					// Set the SSL parameters
					SSLParameters sslParameters = context.getSupportedSSLParameters();
					params.setSSLParameters(sslParameters);

				} catch (Exception ex) {
					System.out.println("Failed to create HTTPS port");
				}
			}
		});
		
		server.createContext("/RefactoringMiner", new MyHandler());
		server.setExecutor(new ThreadPoolExecutor(4, 8, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(100)));
		server.start();
		System.out.println(InetAddress.getLocalHost());
	}

	static class MyHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange exchange) throws IOException {
			printRequestInfo(exchange);
			URI requestURI = exchange.getRequestURI();
			String query = requestURI.getQuery();
			Map<String, String> queryToMap = queryToMap(query);

			String gitURL = queryToMap.get("gitURL");
			String commitId = queryToMap.get("commitId");
			int timeout = Integer.parseInt(queryToMap.get("timeout"));
			List<Refactoring> detectedRefactorings = new ArrayList<Refactoring>();

			GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
			miner.detectAtCommit(gitURL, commitId, new RefactoringHandler() {
				@Override
				public void handle(String commitId, List<Refactoring> refactorings) {
					detectedRefactorings.addAll(refactorings);
				}
			}, timeout);

			String response = JSON(gitURL, commitId, detectedRefactorings);
			System.out.println(response);
			exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
			exchange.sendResponseHeaders(200, response.length());
			OutputStream os = exchange.getResponseBody();
			os.write(response.getBytes());
			os.close();
		}
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
