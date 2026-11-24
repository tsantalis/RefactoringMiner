"""The planning page's Brainstorm list (US-20.2, #245), against real PostgreSQL.

Pixel (alice) owns ``Pixel-Org/App``. Alpha (bob) is another team of the same course. The issues
come from the fake GitHub API, read through the App's installation token.
"""

# AI contribution: 50%

from collections.abc import Awaitable, Callable

import httpx
import pytest
from fastapi.testclient import TestClient
from githubkit import GitHub
from pydantic import SecretStr

from app.core.config import Settings, get_settings
from app.github import client as github_client
from app.main import app
from tests.api.courses_support import (
    SignIn,
    TwoTeams,
    course_with_two_teams,
    make_stakeholder,
    make_ta,
    member_id,
    run_sql,
)
from tests.fake_github_api import GitHubApi, github_issue

pytest_plugins = ("tests.api.backlog_coverage_support",)

REPOSITORY = "Pixel-Org/App"
ALL_OPEN = "?q=is%3Aissue%20is%3Aopen%20label%3Abrainstorm"


def brainstorm(team_id: str) -> str:
    return f"/api/teams/{team_id}/planning/brainstorm"


def idea(number: int, title: str) -> dict[str, object]:
    return {
        "number": number,
        "title": title,
        "url": f"https://github.com/Pixel-Org/App/issues/{number}",
    }


def listed(
    issues: list[dict[str, object]], *, full_name: str = REPOSITORY, more: bool = False
) -> dict[str, object]:
    return {
        "full_name": full_name,
        "status": "listed",
        "issues": issues,
        "more": more,
        "all_url": f"https://github.com/{full_name}/issues{ALL_OPEN}",
    }


def fake_github(
    monkeypatch: pytest.MonkeyPatch,
    course_settings: Settings,
    app_private_key: str,
    api: GitHubApi,
) -> GitHubApi:
    settings = course_settings.model_copy(
        update={"github_app_id": "1", "github_private_key": SecretStr(app_private_key)}
    )
    app.dependency_overrides[get_settings] = lambda: settings
    monkeypatch.setattr(github_client, "GitHub", lambda auth: GitHub(auth, async_transport=api))
    return api


@pytest.fixture
def ideas(
    monkeypatch: pytest.MonkeyPatch,
    sign_in: SignIn,
    course_settings: Settings,
    app_private_key: str,
) -> GitHubApi:
    api = GitHubApi(
        b"",
        issues=[
            github_issue(21, "Dark mode", label="brainstorm"),
            github_issue(22, "Old idea", label="brainstorm", state="closed"),
            github_issue(23, "Sign in", label="user story"),
            github_issue(24, "Idea PR", label="brainstorm", is_pull_request=True),
            github_issue(25, "Export to CSV", label="brainstorm"),
        ],
    )
    return fake_github(monkeypatch, course_settings, app_private_key, api)


@pytest.mark.usefixtures("ideas")
def test_a_member_sees_the_open_brainstorm_issues_of_their_repository(
    sign_in: SignIn, teams: TwoTeams
) -> None:
    response = sign_in("alice").get(brainstorm(teams.pixel))

    assert response.status_code == 200
    assert response.json() == {
        "label": "brainstorm",
        "repositories": [listed([idea(21, "Dark mode"), idea(25, "Export to CSV")])],
    }


def test_a_repository_with_no_brainstorm_issues_lists_none(
    monkeypatch: pytest.MonkeyPatch,
    sign_in: SignIn,
    teams: TwoTeams,
    course_settings: Settings,
    app_private_key: str,
) -> None:
    api = GitHubApi(b"", issues=[github_issue(23, "Sign in", label="user story")])
    fake_github(monkeypatch, course_settings, app_private_key, api)

    response = sign_in("alice").get(brainstorm(teams.pixel))

    assert response.json()["repositories"] == [listed([])]


def test_a_full_page_of_issues_says_there_may_be_more(
    monkeypatch: pytest.MonkeyPatch,
    sign_in: SignIn,
    teams: TwoTeams,
    course_settings: Settings,
    app_private_key: str,
) -> None:
    issues = [github_issue(n, f"Idea {n}", label="brainstorm") for n in range(1, 102)]
    fake_github(monkeypatch, course_settings, app_private_key, GitHubApi(b"", issues=issues))

    repository = sign_in("alice").get(brainstorm(teams.pixel)).json()["repositories"][0]

    assert len(repository["issues"]) == 100
    assert repository["more"] is True
    assert repository["all_url"] == f"https://github.com/{REPOSITORY}/issues{ALL_OPEN}"


def test_a_label_spelled_with_capitals_is_listed_as_the_webhook_matches_it(
    monkeypatch: pytest.MonkeyPatch,
    sign_in: SignIn,
    teams: TwoTeams,
    course_settings: Settings,
    app_private_key: str,
) -> None:
    api = GitHubApi(b"", issues=[github_issue(26, "Offline mode", label="Brainstorm")])
    fake_github(monkeypatch, course_settings, app_private_key, api)

    response = sign_in("alice").get(brainstorm(teams.pixel))

    assert response.json()["repositories"] == [listed([idea(26, "Offline mode")])]


@pytest.mark.usefixtures("ideas")
def test_staff_and_a_granted_stakeholder_read_the_same_list(
    sign_in: SignIn, teams: TwoTeams
) -> None:
    expected = sign_in("alice").get(brainstorm(teams.pixel)).json()
    tara = sign_in("tara")
    make_ta(teams.instructor, teams.course_id, "tara")
    course = teams.instructor.get(f"/api/courses/{teams.course_id}").json()
    sign_in("sam").post("/api/courses/join", json={"join_code": course["join_code"]})
    make_stakeholder(teams.instructor, teams.course_id, "sam")
    teams.instructor.post(
        f"/api/courses/{teams.course_id}/teams/{teams.pixel}/grants",
        json={"user_id": member_id(teams.instructor, teams.course_id, "sam")},
    )

    for reader in (teams.instructor, tara, sign_in("sam")):
        response = reader.get(brainstorm(teams.pixel))

        assert response.status_code == 200
        assert response.json() == expected


@pytest.mark.usefixtures("ideas")
def test_a_student_of_another_team_is_refused_before_github_is_read(
    sign_in: SignIn, teams: TwoTeams, ideas: GitHubApi
) -> None:
    response = sign_in("bob").get(brainstorm(teams.pixel))

    assert response.status_code == 403
    assert ideas.requests == []


@pytest.mark.usefixtures("teams")
def test_an_unknown_team_is_refused_like_one_the_viewer_cant_read(
    sign_in: SignIn, ideas: GitHubApi
) -> None:
    response = sign_in("alice").get(brainstorm("00000000-0000-4000-8000-000000000000"))

    assert response.status_code == 403
    assert ideas.requests == []


@pytest.mark.usefixtures("ideas")
def test_a_signed_out_request_is_refused(teams: TwoTeams) -> None:
    response = TestClient(app, base_url="https://testserver").get(brainstorm(teams.pixel))

    assert response.status_code == 401


def test_a_repository_github_cant_be_read_for_is_failed_and_the_others_still_listed(
    monkeypatch: pytest.MonkeyPatch,
    sign_in: SignIn,
    teams: TwoTeams,
    course_settings: Settings,
    app_private_key: str,
    database_url: str,
) -> None:
    run_sql(
        database_url,
        """INSERT INTO repositories (team_id, github_repository_id, full_name)
           VALUES (%s, 701, 'Pixel-Org/Gone')""",
        (teams.pixel,),
    )
    api = GitHubApi(b"", issues=[github_issue(21, "Dark mode", label="brainstorm")])
    fake_github(monkeypatch, course_settings, app_private_key, api)
    # The App isn't installed on Pixel-Org/Gone: GitHub answers its installation lookup with 404.
    monkeypatch.setattr(api, "handle_async_request", _uninstalled_on("Gone", api))

    response = sign_in("alice").get(brainstorm(teams.pixel))

    assert response.status_code == 200
    gone = {**listed([], full_name="Pixel-Org/Gone"), "status": "failed"}
    assert response.json()["repositories"] == [listed([idea(21, "Dark mode")]), gone]


def test_a_team_with_no_repository_has_an_empty_list(sign_in: SignIn) -> None:
    teams = course_with_two_teams(sign_in)

    response = sign_in("alice").get(brainstorm(teams.pixel))

    assert response.status_code == 200
    assert response.json() == {"label": "brainstorm", "repositories": []}


def _uninstalled_on(
    repo: str, api: GitHubApi
) -> Callable[[httpx.Request], Awaitable[httpx.Response]]:
    answer = api.handle_async_request

    async def handle(request: httpx.Request) -> httpx.Response:
        if request.url.path == f"/repos/Pixel-Org/{repo}/installation":
            return httpx.Response(404, json={"message": "Not Found"})
        return await answer(request)

    return handle
