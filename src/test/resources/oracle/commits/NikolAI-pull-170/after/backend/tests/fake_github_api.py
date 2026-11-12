"""GitHub's REST API for the Action runs' jobs, as githubkit reaches it over the network.

The refactoring report (US-8) and the static findings (US-15) both read a workflow run's artifact.

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

from app.services import refactoring_comments, static_findings
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
_ISSUE_LIST: Final = re.compile(r"/repos/[^/]+/[^/]+/issues")
_INSTALLATION_TOKEN: Final = re.compile(r"/app/installations/\d+/access_tokens")
_REPOSITORY_INSTALLATION: Final = re.compile(r"/repos/[^/]+/[^/]+/installation")

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


def changed_file(
    filename: str, patch: str | None, status: str = "modified", additions: int | None = None
) -> dict[str, Any]:
    """One file of a PR's ``GET /pulls/{number}/files``.

    GitHub leaves out ``patch`` for a binary file, or a diff too large to send (``patch=None``),
    but still counts the lines added: ``additions``, counted from the patch when not given.
    """
    blob = f"https://github.com/octocat/demo/blob/{'a' * 40}/{filename}"
    added = (patch or "").count("\n+") if additions is None else additions
    entry: dict[str, Any] = {
        "sha": "b" * 40,
        "filename": filename,
        "status": status,
        "additions": added,
        "deletions": (patch or "").count("\n-"),
        "changes": added + (patch or "").count("\n-"),
        "blob_url": blob,
        "raw_url": blob.replace("/blob/", "/raw/"),
        "contents_url": f"https://api.github.com/repos/octocat/demo/contents/{filename}",
    }
    if patch is not None:
        entry["patch"] = patch
    return entry


def use_fake_github(monkeypatch: pytest.MonkeyPatch, api: GitHubApi) -> None:
    """Make the Action runs' jobs sign in to ``api`` through a real githubkit client."""
    for job in (refactoring_comments, static_findings):
        monkeypatch.setattr(
            job,
            "installation_client",
            lambda _settings, _installation: GitHub(
                TokenAuthStrategy("token"), async_transport=api
            ),
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


def installation() -> dict[str, Any]:
    """A ``GET /repos/{owner}/{repo}/installation`` answer."""
    return {
        "id": 42,
        "account": _user("octocat", 2002, "User"),
        "repository_selection": "selected",
        "access_tokens_url": "https://api.github.com/app/installations/42/access_tokens",
        "repositories_url": "https://api.github.com/installation/repositories",
        "html_url": "https://github.com/settings/installations/42",
        "app_id": 1,
        "target_id": 2002,
        "target_type": "User",
        "permissions": {"issues": "write"},
        "events": ["issues"],
        "created_at": "2026-09-01T12:00:00Z",
        "updated_at": "2026-09-01T12:00:00Z",
        "single_file_name": None,
        "app_slug": "nikolai",
        "suspended_by": None,
        "suspended_at": None,
    }


def github_issue(
    number: int,
    title: str,
    *,
    label: str = "user story",
    state: str = "open",
    body: str | None = None,
    is_pull_request: bool = False,
) -> dict[str, Any]:
    """An issue as ``GET /repos/{owner}/{repo}/issues`` lists it. Pull requests are listed too."""
    url = f"https://api.github.com/repos/Pixel-Org/App/issues/{number}"
    issue: dict[str, Any] = {
        "id": 5000 + number,
        "node_id": f"I_{number}",
        "url": url,
        "repository_url": "https://api.github.com/repos/Pixel-Org/App",
        "labels_url": f"{url}/labels{{/name}}",
        "comments_url": f"{url}/comments",
        "events_url": f"{url}/events",
        "html_url": f"https://github.com/Pixel-Org/App/issues/{number}",
        "number": number,
        "state": state,
        "title": title,
        "body": body,
        "user": _user("alice", 2003, "User"),
        # As GitHub sends it: `archived_by` is newer than githubkit's schema, which rejects it.
        "labels": [{"id": 77, "name": label, "archived_by": _user("alice", 2003, "User")}],
        "assignee": None,
        "assignees": [],
        "milestone": None,
        "locked": False,
        "comments": 0,
        "closed_at": None if state == "open" else "2026-11-02T00:00:00Z",
        "created_at": "2026-11-01T00:00:00Z",
        "updated_at": "2026-11-01T00:00:00Z",
        "author_association": "MEMBER",
    }
    if is_pull_request:
        issue["pull_request"] = {
            "url": f"https://api.github.com/repos/Pixel-Org/App/pulls/{number}",
            "html_url": f"https://github.com/Pixel-Org/App/pull/{number}",
            "diff_url": f"https://github.com/Pixel-Org/App/pull/{number}.diff",
            "patch_url": f"https://github.com/Pixel-Org/App/pull/{number}.patch",
        }
    return issue


class GitHubApi(httpx.AsyncBaseTransport):
    """The few endpoints the refactoring report uses, over a network-like connection.

    A streamed body can't be read once the client that asked for it has closed, as with a real
    connection. githubkit shares this one transport between every client it opens, so closes
    are counted: a body is readable only if no client closed since it was sent.

    Posted comments are kept: listing the PR's comments returns them, and ``PATCH`` edits them.
    They are made through App ``app_id``, or through no App when it is None.

    The run uploaded one artifact, ``artifact_name``. ``files`` is the PR's changed files list.
    """

    def __init__(
        self,
        artifact_zip: bytes,
        *,
        pull: dict[str, Any] | None = None,
        breaks_midway: bool = False,
        app_id: int | None = None,
        issues: list[dict[str, Any]] | None = None,
        artifact_name: str = "refactoring-diff",
        files: list[dict[str, Any]] | None = None,
    ) -> None:
        self.artifact_zip = artifact_zip
        self.pull = pull
        self.breaks_midway = breaks_midway
        self.app_id = app_id
        self.issues = issues
        self.artifact_name = artifact_name
        self.files = files or []
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
            artifact = {**ARTIFACT, "name": self.artifact_name}
            return httpx.Response(200, json={"total_count": 1, "artifacts": [artifact]})
        if path.endswith(f"/actions/artifacts/{ARTIFACT_ID}/zip"):
            location = f"https://{BLOB_HOST}/artifacts/{ARTIFACT_ID}.zip?sig=signed"
            return httpx.Response(302, headers={"Location": location})
        return self._backlog(request) or await self._issues_and_pulls(request)

    def _backlog(self, request: httpx.Request) -> httpx.Response | None:
        """The App's lookup of the repository, its sign-in and the issue list, once given issues."""
        path = request.url.path
        if self.issues is None:
            return None
        if _REPOSITORY_INSTALLATION.fullmatch(path):
            return httpx.Response(200, json=installation())
        if _INSTALLATION_TOKEN.fullmatch(path):
            expires_at = "2030-01-01T00:00:00Z"
            return httpx.Response(201, json={"token": "ghs_test", "expires_at": expires_at})
        if _ISSUE_LIST.fullmatch(path) and request.method == "GET":
            return httpx.Response(200, json=self._issue_page(request.url.params))
        return None

    def _issue_page(self, params: httpx.QueryParams) -> list[dict[str, Any]]:
        """One page of the issues matching ``state`` and ``labels``, as GitHub filters them."""
        state, label = params.get("state", "open"), params.get("labels")
        matching = [
            issue
            for issue in self.issues or []
            if state in ("all", issue["state"])
            and (label is None or label in [each["name"] for each in issue["labels"]])
        ]
        per_page, page = int(params.get("per_page", 30)), int(params.get("page", 1))
        return matching[(page - 1) * per_page : page * per_page]

    async def _issues_and_pulls(self, request: httpx.Request) -> httpx.Response:
        path = request.url.path
        if self.pull is not None and path.endswith(f"/pulls/{self.pull['number']}"):
            return httpx.Response(200, json=self.pull)
        if self.pull is not None and path.endswith(f"/pulls/{self.pull['number']}/files"):
            first_page = request.url.params.get("page", "1") == "1"
            return httpx.Response(200, json=self.files if first_page else [])
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
