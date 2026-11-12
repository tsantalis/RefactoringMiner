"""Check a team's backlog against its course's project requirements (US-13.2).

Access is the caller's job: the API runs ``readable_repository`` first. The requirements come
from the course, the stories from GitHub, and the answer from the ``BacklogCoverageAgent``.
"""

# AI contribution: 50%

import asyncio
import logging
import re
from typing import Final

from githubkit.exception import GitHubException
from psycopg import AsyncConnection
from psycopg.rows import DictRow
from pydantic import ValidationError

from app.agents.backlog_coverage import (
    BacklogCoverageAgent,
    ParsedCoverageReply,
    ProposedRequirement,
    parse_coverage_reply,
)
from app.agents.base import AgentContext
from app.agents.untrusted import description_for_prompt, neutralize_tags
from app.core.config import ConfigurationError, Settings
from app.db.models.backlog_coverage import (
    BacklogCoverage,
    CoverageOutcome,
    CoveringStory,
    RequirementCoverage,
)
from app.db.models.users import User
from app.db.repositories.backlog_coverage import find_coverage
from app.db.repositories.feedback import RegisteredRepository
from app.github.client import repository_client
from app.github.user_stories import (
    MAX_USER_STORIES,
    TooManyUserStoriesError,
    UserStory,
    list_user_stories,
)
from app.providers.base import LLMProvider
from app.providers.errors import LLMError, LLMResponseError
from app.services.model_slots import model_call_slots
from app.services.viewers import team_viewer

logger = logging.getLogger(__name__)

_STORY_BODY_LIMIT: Final = 2_000
# The whole check, retries included. Production is behind a Cloudflare tunnel, which cuts a request
# after about 100 seconds, and the extension waits longer than that (COVERAGE_CHECK_TIMEOUT_MS in
# nikolai-client.ts), so a slow failure still reaches the student as a stored reason.
CHECK_TIME_LIMIT_SECONDS: Final = 90
# "1." or "2)" before a requirement: list numbering, not words the model must quote.
_LIST_NUMBER: Final = re.compile(r"^\s*\d+[.)]\s+")

# Why a check couldn't run, as the student reads it.
NO_MODEL: Final = "No AI model is configured on this NikolAI server."
MODEL_FAILED: Final = "The AI model couldn't be reached. Try again later."
TIMED_OUT: Final = "The check took too long. Try again later."
INVALID_REPLY: Final = "The AI model's answer couldn't be used. Try again."
GITHUB_FAILED: Final = (
    "NikolAI couldn't read this repository's issues from GitHub. Try again later."
)
TOO_MANY_STORIES: Final = (
    f"This backlog has too many user stories to check at once (the limit is {MAX_USER_STORIES})."
)


async def check_backlog(
    project_requirements: str,
    full_name: str,
    settings: Settings,
    provider: LLMProvider | None,
) -> CoverageOutcome:
    """Run the check on the repository ``full_name``.

    No GitHub or model failure raises: each is a ``failed`` outcome with a reason the student can
    read. The errors themselves are only logged by type, since their messages can echo secrets.
    """
    if not project_requirements.strip():
        return CoverageOutcome(status="no_requirements", reason=None, requirements=[])
    if provider is None:
        logger.warning("Could not check the backlog of %s: no model configured", full_name)
        return _could_not_check(NO_MODEL)
    try:
        async with asyncio.timeout(CHECK_TIME_LIMIT_SECONDS):
            stories = await _read_stories(settings, full_name)
            reply = await _ask_model(provider, project_requirements, stories)
    except TimeoutError:
        logger.error("The coverage check of %s ran out of time", full_name)
        return _could_not_check(TIMED_OUT)
    except TooManyUserStoriesError:
        logger.warning("Could not check the backlog of %s: too many stories", full_name)
        return _could_not_check(TOO_MANY_STORIES)
    except (GitHubException, ConfigurationError, ValidationError) as error:
        logger.error("Could not read the stories of %s: %s", full_name, type(error).__name__)
        return _could_not_check(GITHUB_FAILED)
    except LLMResponseError:
        logger.warning("The model's coverage reply for %s was invalid", full_name)
        return _could_not_check(INVALID_REPLY)
    except LLMError as error:
        logger.error("The coverage check of %s failed: %s", full_name, type(error).__name__)
        return _could_not_check(MODEL_FAILED)
    requirements = _mapped(reply.requirements, stories) + _left_out(reply, project_requirements)
    return CoverageOutcome(status="completed", reason=None, requirements=requirements)


def _could_not_check(reason: str) -> CoverageOutcome:
    return CoverageOutcome(status="failed", reason=reason, requirements=[])


async def _read_stories(settings: Settings, full_name: str) -> list[UserStory]:
    owner, repo = full_name.split("/", 1)
    client = await repository_client(settings, owner, repo)
    async with client as github:
        return await list_user_stories(github, owner, repo)


async def _ask_model(
    provider: LLMProvider, project_requirements: str, stories: list[UserStory]
) -> ParsedCoverageReply:
    agent = BacklogCoverageAgent(provider)
    context = AgentContext(prompt=_prompt(project_requirements, stories))
    async with model_call_slots():
        result = await agent.run(context)
    reply = parse_coverage_reply(result.content)
    source = _words(project_requirements)
    if any(not _positions(source, _words(item.requirement)) for item in reply.requirements):
        raise LLMResponseError("The reply has a requirement the text doesn't contain")
    return reply


def _words(text: str) -> list[str]:
    """``text``'s lowercase words, so quotes match whatever markdown surrounds them and only on
    whole words."""
    return re.findall(r"[^\W_]+", text.casefold())


def _left_out(reply: ParsedCoverageReply, project_requirements: str) -> list[RequirementCoverage]:
    """The lines of the text the model didn't account for, reported as uncovered.

    A requirement the model skipped must still reach the team: showing a stray line by mistake
    costs them a glance, while hiding a requirement is the failure this check exists to prevent.
    """
    quotes = [_words(item.requirement) for item in reply.requirements]
    quotes += [_words(line) for line in reply.not_requirements]
    left_out = []
    for line in project_requirements.splitlines():
        words = _words(_LIST_NUMBER.sub("", line))
        if words and not _accounted_for(words, quotes):
            left_out.append(RequirementCoverage(requirement=line.strip(" \t-*#>"), stories=[]))
    return left_out


def _accounted_for(line: list[str], quotes: list[list[str]]) -> bool:
    """Whether the quotes hold the whole line: one quote spanning it, or quotes of its parts that
    together cover every word. A quote of only part of a line never hides the rest of it."""
    covered = [False] * len(line)
    for quote in quotes:
        if _positions(quote, line):
            return True
        for start in _positions(line, quote):
            covered[start : start + len(quote)] = [True] * len(quote)
    return all(covered)


def _positions(words: list[str], part: list[str]) -> list[int]:
    """Each index where ``part`` starts in ``words``, as whole words; empty when it never does."""
    return [
        start
        for start in range(len(words) - len(part) + 1)
        if part and words[start : start + len(part)] == part
    ]


def _prompt(project_requirements: str, stories: list[UserStory]) -> str:
    """The agent's input, with everything people wrote unable to open or close its tags."""
    backlog = "\n".join(
        f'<story number="{story.number}">\n'
        f"<title>{neutralize_tags(story.title)}</title>\n"
        f"<description>{description_for_prompt(story.body, _STORY_BODY_LIMIT)}</description>\n"
        "</story>"
        for story in stories
    )
    return (
        "<backlog_check>\n"
        f"<project_requirements>\n{neutralize_tags(project_requirements)}\n</project_requirements>\n"
        f"<backlog>\n{backlog or '(no user stories)'}\n</backlog>\n"
        "</backlog_check>"
    )


def _mapped(
    proposed: list[ProposedRequirement], stories: list[UserStory]
) -> list[RequirementCoverage]:
    """Each requirement with its stories, keeping only numbers that are really in the backlog."""
    backlog = {story.number: story for story in stories}
    return [
        RequirementCoverage(
            requirement=item.requirement,
            stories=[
                CoveringStory(number=number, title=backlog[number].title, url=backlog[number].url)
                for number in dict.fromkeys(item.stories)
                if number in backlog
            ],
        )
        for item in proposed
    ]


async def latest_coverage(
    connection: AsyncConnection[DictRow], repository: RegisteredRepository, user: User
) -> BacklogCoverage:
    """The repository's latest check as this viewer sees it, or ``not_run`` before the first."""
    _, viewer = await team_viewer(connection, repository, user)
    stored = await find_coverage(connection, repository.id)
    if stored is None:
        return BacklogCoverage(
            viewer=viewer, status="not_run", reason=None, checked_at=None, requirements=[]
        )
    return BacklogCoverage(viewer=viewer, **stored.model_dump())
