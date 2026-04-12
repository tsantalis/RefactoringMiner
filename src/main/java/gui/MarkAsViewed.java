package gui;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.refactoringminer.util.GitHubOAuthTokenProvider;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.Map;

public class MarkAsViewed {
	private static final String API_URL = "https://api.github.com/graphql";
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
	private static final String OAUTH_TOKEN = GitHubOAuthTokenProvider.getOAuthToken();

	public static void main(String[] args) {
		PullRequestViewState state = getPullRequestViewState("tsantalis", "RefactoringMiner", 1047);
		String prNodeId = state.pullRequestNodeId();
		Map<String, Boolean> viewedMap = state.viewedFiles();
		System.out.println("Loaded viewed state for " + viewedMap.size() + " files");
		markAsViewed(prNodeId, "README.md");
		unmarkAsViewed(prNodeId, "README.md");
	}

	private static PullRequestViewState getPullRequestViewState(String owner, String repoName, int prNumber) {
		Map<String, Boolean> viewedFiles = new LinkedHashMap<>();
		String prNodeId = null;
		String cursor = null;
		String query = """
				query PullRequestViewedFiles($owner: String!, $name: String!, $number: Int!, $cursor: String){
				  repository(owner: $owner, name: $name) {
				    pullRequest(number: $number) {
				      id
				      files(first: 100, after: $cursor) {
				        pageInfo {
				          hasNextPage
				          endCursor
				        }
				        nodes {
				          path
				          viewerViewedState
				        }
				      }
				    }
				  }
				}
				""";
		try {
			do {
				ObjectNode variables = OBJECT_MAPPER.createObjectNode();
				variables.put("owner", owner);
				variables.put("name", repoName);
				variables.put("number", prNumber);
				if (cursor == null) {
					variables.putNull("cursor");
				} else {
					variables.put("cursor", cursor);
				}
				JsonNode pullRequest = executeGitHubGraphQl(query, variables)
						.path("repository")
						.path("pullRequest");
				if (pullRequest.isMissingNode() || pullRequest.isNull()) {
					break;
				}
				PullRequestViewStatePage page = parsePullRequestViewStatePage(pullRequest);
				if (page.pullRequestNodeId() != null) {
					prNodeId = page.pullRequestNodeId();
				}
				viewedFiles.putAll(page.viewedFiles());
				cursor = page.nextCursor();
			} while (cursor != null);
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			e.printStackTrace();
		}
		return new PullRequestViewState(prNodeId, viewedFiles);
	}

	static PullRequestViewStatePage parsePullRequestViewStatePage(JsonNode pullRequest) {
		String pullRequestNodeId = pullRequest.hasNonNull("id") ? pullRequest.get("id").textValue() : null;
		Map<String, Boolean> viewedFiles = new LinkedHashMap<>();
		JsonNode files = pullRequest.path("files");
		JsonNode nodes = files.path("nodes");
		if (nodes.isArray()) {
			for (JsonNode next : nodes) {
				JsonNode path = next.get("path");
				if (path != null && !path.isNull()) {
					viewedFiles.put(path.textValue(), "VIEWED".equals(next.path("viewerViewedState").textValue()));
				}
			}
		}
		String nextCursor = null;
		JsonNode pageInfo = files.path("pageInfo");
		if (pageInfo.path("hasNextPage").asBoolean(false)) {
			nextCursor = pageInfo.path("endCursor").textValue();
		}
		return new PullRequestViewStatePage(pullRequestNodeId, nextCursor, viewedFiles);
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
			ObjectNode variables = OBJECT_MAPPER.createObjectNode();
			variables.put("path", path);
			variables.put("pullRequestId", prNodeId);
			JsonNode response = executeGitHubGraphQl(mutation, variables);
			System.out.println("Response body: " + response);
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			e.printStackTrace();
		}
	}

	private static JsonNode executeGitHubGraphQl(String query, ObjectNode variables) throws IOException, InterruptedException {
		ObjectNode requestBodyJson = OBJECT_MAPPER.createObjectNode();
		requestBodyJson.put("query", query);
		requestBodyJson.set("variables", variables);

		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(API_URL))
				.header("Authorization", "Bearer " + OAUTH_TOKEN)
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(requestBodyJson)))
				.build();

		HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
		JsonNode root = OBJECT_MAPPER.readTree(response.body());
		if (response.statusCode() >= 400 || root.has("errors")) {
			throw new IOException("GitHub GraphQL request failed: " + response.body());
		}
		return root.path("data");
	}

	static record PullRequestViewState(String pullRequestNodeId, Map<String, Boolean> viewedFiles) {
	}

	static record PullRequestViewStatePage(String pullRequestNodeId, String nextCursor, Map<String, Boolean> viewedFiles) {
	}
}
