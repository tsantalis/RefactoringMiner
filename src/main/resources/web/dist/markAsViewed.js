async function markFileAsViewed(OAUTH_TOKEN, prNodeId, filePath) {
    return markAsViewed(OAUTH_TOKEN, prNodeId, filePath);
}

async function unmarkFileAsViewed(OAUTH_TOKEN, prNodeId, filePath) {
    return unmarkAsViewed(OAUTH_TOKEN, prNodeId, filePath);
}

async function executeGitHubGraphQl(OAUTH_TOKEN, query, variables) {
    const response = await fetch("https://api.github.com/graphql", {
        method: "POST",
        headers: {
            "Authorization": `Bearer ${OAUTH_TOKEN}`,
            "Content-Type": "application/json"
        },
        body: JSON.stringify({
            query: query,
            variables: variables
        })
    });
    const root = await response.json();
    if (!response.ok || root.errors) {
        throw new Error(`GitHub GraphQL request failed: ${JSON.stringify(root.errors || root)}`);
    }
    return root.data;
}

async function getPullRequestViewState(OAUTH_TOKEN, owner, repoName, prNumber) {
    const query = `
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
        }`;
    const viewedFiles = {};
    let pullRequestNodeId = null;
    let cursor = null;
    try {
        do {
            const data = await executeGitHubGraphQl(OAUTH_TOKEN, query, {
                owner: owner,
                name: repoName,
                number: prNumber,
                cursor: cursor
            });
            const pullRequest = data?.repository?.pullRequest;
            const files = pullRequest?.files;
            pullRequestNodeId = pullRequest?.id || pullRequestNodeId;
            const nodes = files?.nodes || [];
            nodes.forEach(next => {
                viewedFiles[next.path] = next.viewerViewedState === "VIEWED";
            });
            cursor = files?.pageInfo?.hasNextPage ? files.pageInfo.endCursor : null;
        } while (cursor);
    } catch (error) {
        console.error("Error fetching pull request view state:", error);
    }
    return {
        pullRequestNodeId: pullRequestNodeId,
        viewedFiles: viewedFiles
    };
}

function unmarkAsViewed(OAUTH_TOKEN, prNodeId, path) {
    const mutation = `
        mutation UnmarkFileViewed($path: String!, $pullRequestId: ID!) {
          unmarkFileAsViewed(input: {path: $path, pullRequestId: $pullRequestId}) {
            clientMutationId
          }
        }`;
    return process(OAUTH_TOKEN, prNodeId, path, mutation);
}

function markAsViewed(OAUTH_TOKEN, prNodeId, path) {
    const mutation = `
        mutation MarkFileViewed($path: String!, $pullRequestId: ID!) {
          markFileAsViewed(input: {path: $path, pullRequestId: $pullRequestId}) {
            clientMutationId
          }
        }`;
    return process(OAUTH_TOKEN, prNodeId, path, mutation);
}

async function process(OAUTH_TOKEN, prNodeId, path, mutation) {
    return executeGitHubGraphQl(OAUTH_TOKEN, mutation, {
        path: path,
        pullRequestId: prNodeId
    });
}

function setViewedDiffCollapsed(checkbox, collapseTarget, viewed) {
    if (!collapseTarget) {
        return;
    }
    const panel = document.querySelector(collapseTarget);
    if (!panel) {
        return;
    }
    panel.classList.toggle("show", !viewed);
    checkbox.setAttribute("aria-expanded", (!viewed).toString());
}

async function initializeViewedFiles(config) {
    if (!config || !config.oauthToken || !config.owner || !config.repoName || !config.prNumber) {
        return;
    }
    const checkboxes = Array.from(document.querySelectorAll(".viewed-file-checkbox[data-file-path]"));
    if (checkboxes.length === 0) {
        return;
    }
    const accordion = document.getElementById("accordion");
    if (accordion) {
        accordion.style.visibility = "hidden";
    }
    try {
        const state = await getPullRequestViewState(
            config.oauthToken,
            config.owner,
            config.repoName,
            config.prNumber
        );
        const viewedFiles = state.viewedFiles || {};
        const prNodeId = state.pullRequestNodeId;
        if (!prNodeId) {
            console.error("Unable to resolve pull request node id for viewed-file toggles.");
            return;
        }
        checkboxes.forEach(checkbox => {
            const filePath = checkbox.dataset.filePath;
            const collapseTarget = checkbox.dataset.collapseTarget;
            const viewed = viewedFiles[filePath] === true;
            checkbox.checked = viewed;
            checkbox.disabled = false;
            setViewedDiffCollapsed(checkbox, collapseTarget, viewed);
            checkbox.addEventListener("change", async () => {
                checkbox.disabled = true;
                try {
                    if (checkbox.checked) {
                        await markFileAsViewed(config.oauthToken, prNodeId, filePath);
                    } else {
                        await unmarkFileAsViewed(config.oauthToken, prNodeId, filePath);
                    }
                    setViewedDiffCollapsed(checkbox, collapseTarget, checkbox.checked);
                } catch (error) {
                    checkbox.checked = !checkbox.checked;
                    console.error("Error updating viewed state:", error);
                } finally {
                    checkbox.disabled = false;
                }
            }, { once: false });
        });
    } catch (error) {
        console.error("Error initializing viewed files:", error);
    } finally {
        if (accordion) {
            accordion.style.visibility = "";
        }
    }
}
