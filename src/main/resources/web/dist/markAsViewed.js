async function markFileAsViewed(OAUTH_TOKEN, owner, repoName, prNumber, filePath) {
    const prNodeId = await getPullRequestNodeId(OAUTH_TOKEN, owner, repoName, prNumber);
    markAsViewed(OAUTH_TOKEN, prNodeId, filePath);
}

async function unmarkFileAsViewed(OAUTH_TOKEN, owner, repoName, prNumber, filePath) {
    const prNodeId = await getPullRequestNodeId(OAUTH_TOKEN, owner, repoName, prNumber);
    unmarkAsViewed(OAUTH_TOKEN, prNodeId, filePath);
}

async function getPullRequestNodeId(OAUTH_TOKEN, owner, repoName, prNumber) {
    query = `
        query PullRequestId($owner: String!, $name: String!, $number: Int!) {
          repository(owner: $owner, name: $name) {
            pullRequest(number: $number) {
              id
              number
              title
            }
          }
        }`;

    const variables = {
      owner: owner,
      name: repoName,
      number: prNumber
    };

    const requestBodyJson = {
      query: query,
      variables: variables
    };

    try {
      const response = await fetch(`https://api.github.com/graphql`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${OAUTH_TOKEN}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(requestBodyJson)
      });

      const root = await response.json();

      const ID = root?.data?.repository?.pullRequest?.id;
      //console.log(ID);
      return ID;
    } catch (error) {
      console.error('Error making request:', error);
    }
}

function unmarkAsViewed(OAUTH_TOKEN, prNodeId, path) {
    const mutation = `
        mutation UnmarkFileViewed($path: String!, $pullRequestId: ID!) {
          unmarkFileAsViewed(input: {path: $path, pullRequestId: $pullRequestId}) {
            clientMutationId
          }
        }`;
    process(OAUTH_TOKEN, prNodeId, path, mutation);
}

function markAsViewed(OAUTH_TOKEN, prNodeId, path) {
    const mutation = `
        mutation MarkFileViewed($path: String!, $pullRequestId: ID!) {
          markFileAsViewed(input: {path: $path, pullRequestId: $pullRequestId}) {
            clientMutationId
          }
        }`;
    process(OAUTH_TOKEN, prNodeId, path, mutation);
}

async function process(OAUTH_TOKEN, prNodeId, path, mutation) {
    const requestBodyJson = {
        query: mutation,
        variables: {
            path: path,
            pullRequestId: prNodeId
        }
    };
    try {
      const response = await fetch(`https://api.github.com/graphql`, {
        method: 'POST',
        headers: {
            'Authorization': `Bearer ${OAUTH_TOKEN}`,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(requestBodyJson)
      });
      const responseBody = await response.text();
      //console.log("Response status code: " + response.status);
      //console.log("Response body: " + responseBody);
    } catch (error) {
        console.error(error);
    }
}