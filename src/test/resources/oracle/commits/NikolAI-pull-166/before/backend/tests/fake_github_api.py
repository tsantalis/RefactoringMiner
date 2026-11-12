"""GitHub's REST API for the refactoring report, as githubkit reaches it over the network (US-8).

Drives the real githubkit client, so the tests catch what a ``MagicMock`` client can't: how
githubkit opens and closes its HTTP clients, the redirect the artifact download goes through, and
how githubkit reads a comment's ``performed_via_github_app`` from GitHub's JSON.
"""

# AI contribution: 50%

import io
import json
import re
import zipfile
from collections.abc import AsyncIterator
from pathlib import Path
from typing import Any, Final

import httpx
import pytest
from githubkit import GitHub
from githubkit.auth import TokenAuthStrategy

from app.services import refactoring_comments
from tests.fakes import REFACTORING_WORKFLOW_RUN_ID

ARTIFACT_ID: Final = 9
# GitHub answers the artifact download with a redirect to its blob storage, on another host.
BLOB_HOST: Final = "productionresultssa0.blob.core.windows.net"
PULL_REQUEST: Final = Path(__file__).parent / "fixtures/github/pull_request.json"
# A RefactoringMiner Action report with three refactorings: 2 Rename Method, 1 Extract Method.
SAMPLE_REPORT: Final = Path(__file__).parent / "fixtures/refactoringminer/refactorings.json"
# The id GitHub gives the first comment posted through this fake, the next one gets 1002, etc.
FIRST_COMMENT_ID: Final = 1001
_EDITED_COMMENT: Final = re.compile(r"/repos/[^/]+/[^/]+/issues/comments/(\d+)")

ARTIFACT: Final = {
    "id": ARTIFACT_ID,
    "node_id": "MDg6QXJ0aWZhY3Q5",
    "name": "refactoring-diff",
    "size_in_bytes": 2_914_894,
    "url": f"https://api.github.com/repos/octocat/demo/actions/artifacts/{ARTIFACT_ID}",
    "archive_download_url": (
        f"https://api.github.com/repos/octocat/demo/actions/artifacts/{ARTIFACT_ID}/zip"
    ),
    "expired": False,
    "created_at": "2026-11-03T22:51:00Z",
    "expires_at": "2027-02-01T22:51:00Z",
    "updated_at": "2026-11-03T22:51:00Z",
}


def pull_request(number: int, head_sha: str) -> dict[str, Any]:
    """A real ``GET /pulls/{number}`` answer (a sandbox PR, with neutral names)."""
    pull: dict[str, Any] = json.loads(PULL_REQUEST.read_text(encoding="utf-8"))
    pull["number"] = number
    pull["head"]["sha"] = head_sha
    return pull


def artifact_zip(files: dict[str, bytes]) -> bytes:
    """An Action artifact as GitHub serves it: a zip of ``files``."""
    buffer = io.BytesIO()
    with zipfile.ZipFile(buffer, "w") as archive:
        for name, content in files.items():
            archive.writestr(name, content)
    return buffer.getvalue()


def use_fake_github(monkeypatch: pytest.MonkeyPatch, api: GitHubApi) -> None:
    """Make the refactoring report job sign in to ``api`` through a real githubkit client."""
    monkeypatch.setattr(
        refactoring_comments,
        "installation_client",
        lambda _settings, _installation: GitHub(TokenAuthStrategy("token"), async_transport=api),
    )


def _user(login: str, user_id: int, kind: str) -> dict[str, Any]:
    """A GitHub user object, as comments and Apps carry it."""
    url = f"https://api.github.com/users/{login}"
    return {
        "login": login,
        "id": user_id,
        "node_id": f"U_{user_id}",
        "avatar_url": f"https://avatars.githubusercontent.com/u/{user_id}?v=4",
        "gravatar_id": "",
        "url": url,
        "html_url": f"https://github.com/{login}",
        "followers_url": f"{url}/followers",
        "following_url": f"{url}/following{{/other_user}}",
        "gists_url": f"{url}/gists{{/gist_id}}",
        "starred_url": f"{url}/starred{{/owner}}{{/repo}}",
        "subscriptions_url": f"{url}/subscriptions",
        "organizations_url": f"{url}/orgs",
        "repos_url": f"{url}/repos",
        "events_url": f"{url}/events{{/privacy}}",
        "received_events_url": f"{url}/received_events",
        "type": kind,
        "site_admin": False,
    }


def _github_app(app_id: int) -> dict[str, Any]:
    """The ``performed_via_github_app`` of a comment made through App ``app_id``."""
    return {
        "id": app_id,
        "slug": "nikolai",
        "node_id": f"A_{app_id}",
        "owner": _user("Pogut", 2001, "User"),
        "name": "NikolAI",
        "description": "Reviews the pull requests of SOEN 490 teams",
        "external_url": "https://github.com/Pogut/NikolAI",
        "html_url": "https://github.com/apps/nikolai",
        "created_at": "2026-09-01T12:00:00Z",
        "updated_at": "2026-09-01T12:00:00Z",
        "permissions": {"actions": "read", "issues": "write", "pull_requests": "write"},
        "events": ["pull_request", "workflow_run"],
    }


class GitHubApi(httpx.AsyncBaseTransport):
    """The few endpoints the refactoring report uses, over a network-like connection.

    A streamed body can't be read once the client that asked for it has closed, as with a real
    connection. githubkit shares this one transport between every client it opens, so closes
    are counted: a body is readable only if no client closed since it was sent.

    Posted comments are kept: listing the PR's comments returns them, and ``PATCH`` edits them.
    They are made through App ``app_id``, or through no App when it is None.
    """

    def __init__(
        self,
        artifact_zip: bytes,
        *,
        pull: dict[str, Any] | None = None,
        breaks_midway: bool = False,
        app_id: int | None = None,
    ) -> None:
        self.artifact_zip = artifact_zip
        self.pull = pull
        self.breaks_midway = breaks_midway
        self.app_id = app_id
        self.closes = 0
        self.requests: list[httpx.Request] = []
        self.streams: list[StreamedBody] = []
        self.comments: list[dict[str, Any]] = []
        self.posted_comments: list[str] = []
        self.patched_comments: list[tuple[int, str]] = []

    async def handle_async_request(self, request: httpx.Request) -> httpx.Response:
        self.requests.append(request)
        path = request.url.path
        if request.url.host == BLOB_HOST:
            body = StreamedBody(self)
            self.streams.append(body)
            return httpx.Response(200, stream=body)
        if path.endswith(f"/actions/runs/{REFACTORING_WORKFLOW_RUN_ID}/artifacts"):
            return httpx.Response(200, json={"total_count": 1, "artifacts": [ARTIFACT]})
        if path.endswith(f"/actions/artifacts/{ARTIFACT_ID}/zip"):
            location = f"https://{BLOB_HOST}/artifacts/{ARTIFACT_ID}.zip?sig=signed"
            return httpx.Response(302, headers={"Location": location})
        return await self._issues_and_pulls(request)

    async def _issues_and_pulls(self, request: httpx.Request) -> httpx.Response:
        path = request.url.path
        if self.pull is not None and path.endswith(f"/pulls/{self.pull['number']}"):
            return httpx.Response(200, json=self.pull)
        if path.endswith("/comments") and request.method == "GET":
            return httpx.Response(200, json=self.comments)
        if path.endswith("/comments") and request.method == "POST":
            body = json.loads(await request.aread())["body"]
            self.posted_comments.append(body)
            return httpx.Response(201, json=self._new_comment(request, body))
        edited = _EDITED_COMMENT.fullmatch(path)
        if edited is not None and request.method == "PATCH":
            return await self._edit_comment(int(edited.group(1)), request)
        return httpx.Response(404, json={"message": "Not Found"})

    def _new_comment(self, request: httpx.Request, body: str) -> dict[str, Any]:
        comment_id = FIRST_COMMENT_ID + len(self.comments)
        # The path is /repos/{owner}/{repo}/issues/{number}/comments.
        _, _, owner, repo, _, number, _ = request.url.path.split("/")
        api = f"https://api.github.com/repos/{owner}/{repo}/issues"
        issue_url = f"{api}/{number}"
        comment = {
            "id": comment_id,
            "node_id": f"IC_{comment_id}",
            "url": f"{api}/comments/{comment_id}",
            "html_url": f"https://github.com/{owner}/{repo}/pull/{number}#issuecomment-{comment_id}",
            "body": body,
            "user": _user("nikolai[bot]", 3001, "Bot"),
            "created_at": "2026-11-03T22:52:00Z",
            "updated_at": "2026-11-03T22:52:00Z",
            "issue_url": issue_url,
            "author_association": "NONE",
            "performed_via_github_app": None if self.app_id is None else _github_app(self.app_id),
        }
        self.comments.append(comment)
        return comment

    async def _edit_comment(self, comment_id: int, request: httpx.Request) -> httpx.Response:
        body = json.loads(await request.aread())["body"]
        self.patched_comments.append((comment_id, body))
        for comment in self.comments:
            if comment["id"] == comment_id:
                comment["body"] = body
                return httpx.Response(200, json=comment)
        return httpx.Response(404, json={"message": "Not Found"})

    async def aclose(self) -> None:
        self.closes += 1

    def request_to(self, host: str) -> httpx.Request:
        return next(request for request in self.requests if request.url.host == host)


class StreamedBody(httpx.AsyncByteStream):
    """The artifact zip, sent in two chunks so a download can break off between them."""

    def __init__(self, api: GitHubApi) -> None:
        self.api = api
        self.sent_after_closes = api.closes
        self.closed = False

    async def __aiter__(self) -> AsyncIterator[bytes]:
        content = self.api.artifact_zip
        if self.api.closes != self.sent_after_closes:
            raise httpx.ReadError("The connection closed before the body was read")
        yield content[: len(content) // 2]
        if self.api.breaks_midway:
            raise httpx.ReadError("The connection dropped partway through the download")
        yield content[len(content) // 2 :]

    async def aclose(self) -> None:
        self.closed = True
