package gui;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Properties;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class MarkAsViewed {
	private static final String API_URL = "https://api.github.com/graphql";
	private static final String OAUTH_TOKEN = getOAuthToken();
	
	public static void main(String[] args) {
		String prNodeId = getPullRequestNodeId("tsantalis", "RefactoringMiner", 897);
		markAsViewed(prNodeId, "README.md");
		unmarkAsViewed(prNodeId, "README.md");
	}

	private static String getOAuthToken() {
		try {
			String oAuthToken = System.getenv("OAuthToken");
			if (oAuthToken == null || oAuthToken.isEmpty()) {
				Properties prop = new Properties();
				InputStream input = new FileInputStream("github-oauth.properties");
				prop.load(input);
				oAuthToken = prop.getProperty("OAuthToken");
			}
			return oAuthToken;
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
		return null;
	}
	
	private static String getPullRequestNodeId(String owner, String repoName, int prNumber) {
		try {
			String query = """
			query PullRequestId($owner: String!, $name: String!, $number: Int!) {
			  repository(owner: $owner, name: $name) {
			    pullRequest(number: $number) {
			      id
			      number
			      title
			    }
			  }
			}
			""";
			ObjectMapper objectMapper = new ObjectMapper();
			ObjectNode variables = objectMapper.createObjectNode();
			variables.put("owner", owner);
			variables.put("name", repoName);
			variables.put("number", prNumber);
			ObjectNode requestBodyJson = objectMapper.createObjectNode();
			requestBodyJson.put("query", query);
			requestBodyJson.set("variables", variables);

			String requestBody = objectMapper.writeValueAsString(requestBodyJson);

			HttpClient client = HttpClient.newHttpClient();
			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(API_URL))
					.header("Authorization", "Bearer " + OAUTH_TOKEN)
					.header("Content-Type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(requestBody))
					.build();

			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

			JsonNode root = objectMapper.readTree(response.body());
			if(root.has("data") && !root.get("data").isNull()) {
				JsonNode data = root.get("data");
				if(data.has("repository")) {
					JsonNode repository = data.get("repository");
					if(repository.has("pullRequest")) {
						JsonNode pullRequest = repository.get("pullRequest");
						if(pullRequest.has("id")) {
							JsonNode id = pullRequest.get("id");
							return id.textValue();
						}
					}
				}
			}
		} catch(IOException ioe) {
			ioe.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return null;
	}

	private static void unmarkAsViewed(String prNodeId, String path) {
		String mutation = """
				mutation UnmarkFileViewed($path: String!, $pullRequestId: ID!) {
				  unmarkFileAsViewed(input: {path: $path, pullRequestId: $pullRequestId}) {
				    clientMutationId
				  }
				}
				""";
		process(prNodeId, path, mutation);
	}

	private static void markAsViewed(String prNodeId, String path) {
		String mutation = """
				mutation MarkFileViewed($path: String!, $pullRequestId: ID!) {
				  markFileAsViewed(input: {path: $path, pullRequestId: $pullRequestId}) {
				    clientMutationId
				  }
				}
				""";
		process(prNodeId, path, mutation);
	}

	private static void process(String prNodeId, String path, String mutation) {
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			ObjectNode variables = objectMapper.createObjectNode();
			variables.put("path", path);
			variables.put("pullRequestId", prNodeId);

			ObjectNode requestBodyJson = objectMapper.createObjectNode();
			requestBodyJson.put("query", mutation);
			requestBodyJson.set("variables", variables);

			String requestBody = objectMapper.writeValueAsString(requestBodyJson);

			HttpClient client = HttpClient.newHttpClient();
			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(API_URL))
					.header("Authorization", "Bearer " + OAUTH_TOKEN)
					.header("Content-Type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(requestBody))
					.build();

			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

			System.out.println("Response status code: " + response.statusCode());
			System.out.println("Response body: " + response.body());
		} catch(IOException ioe) {
			ioe.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
