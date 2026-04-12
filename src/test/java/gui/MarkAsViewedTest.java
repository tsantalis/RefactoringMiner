package gui;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkAsViewedTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void parsesPullRequestViewStatePage() throws Exception {
        JsonNode pullRequest = OBJECT_MAPPER.readTree("""
                {
                  "id": "PR_node_id",
                  "files": {
                    "pageInfo": {
                      "hasNextPage": true,
                      "endCursor": "cursor-1"
                    },
                    "nodes": [
                      {
                        "path": "src/main/java/example/A.java",
                        "viewerViewedState": "VIEWED"
                      },
                      {
                        "path": "src/main/java/example/B.java",
                        "viewerViewedState": "UNVIEWED"
                      }
                    ]
                  }
                }
                """);

        MarkAsViewed.PullRequestViewStatePage page = MarkAsViewed.parsePullRequestViewStatePage(pullRequest);

        assertEquals("PR_node_id", page.pullRequestNodeId());
        assertEquals("cursor-1", page.nextCursor());
        assertEquals(Boolean.TRUE, page.viewedFiles().get("src/main/java/example/A.java"));
        assertEquals(Boolean.FALSE, page.viewedFiles().get("src/main/java/example/B.java"));
    }

    @Test
    void handlesMissingFilesInPullRequestViewStatePage() throws Exception {
        JsonNode pullRequest = OBJECT_MAPPER.readTree("""
                {
                  "id": "PR_node_id"
                }
                """);

        MarkAsViewed.PullRequestViewStatePage page = MarkAsViewed.parsePullRequestViewStatePage(pullRequest);

        assertEquals("PR_node_id", page.pullRequestNodeId());
        assertNull(page.nextCursor());
        assertTrue(page.viewedFiles().isEmpty());
    }
}
