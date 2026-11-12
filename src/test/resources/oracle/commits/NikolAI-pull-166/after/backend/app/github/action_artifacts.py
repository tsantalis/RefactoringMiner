"""Fetch a report from a workflow run's artifact: RefactoringMiner's (US-8) or Semgrep's (US-15).

The RefactoringMiner Action (https://github.com/Pogut/RefactoringMiner-action) uploads one of two
artifacts:

- ``refactoring-diff`` on private repositories: the exported web view, with
  ``refactorings.json`` at its root;
- ``github-pages`` on public ones: an ``artifact.tar`` of the whole Pages site, with each open
  PR's report at ``refactorings/pr-<number>/refactorings.json``.

The Semgrep workflow (the wiki's Static Analysis page) uploads ``semgrep-results``, with
Semgrep's JSON output as ``semgrep.json`` at its root.

Artifacts are written by a team's workflow, so they are untrusted: only the one report file is
read, never extracted to disk, and every size is capped.
"""

# AI contribution: 50%

import asyncio
import tarfile
import tempfile
import zipfile
from collections.abc import Callable
from typing import IO, Final

from githubkit import GitHub
from githubkit.auth import AppInstallationAuthStrategy

# The artifact names the Action uploads, in the order NikolAI looks for them.
REPORT_ARTIFACTS: Final = ("refactoring-diff", "github-pages")
SEMGREP_ARTIFACT: Final = "semgrep-results"
MAX_REPORT_BYTES: Final = 10 * 1024 * 1024
# The Pages site holds every open PR's view, Monaco editor included, so it can be large.
MAX_ARTIFACT_BYTES: Final = 500 * 1024 * 1024
# Downloads bigger than this spill from memory to a temporary file.
_SPOOL_BYTES: Final = 16 * 1024 * 1024

_REPORT_FILE: Final = "refactorings.json"
_SITE_FILE: Final = "artifact.tar"
_SEMGREP_FILE: Final = "semgrep.json"

type GitHubClient = GitHub[AppInstallationAuthStrategy]


class ArtifactError(ValueError):
    """The artifact is corrupt or bigger than NikolAI accepts."""


async def download_refactorings_json(
    github: GitHubClient, owner: str, repo: str, run_id: int, pr_number: int
) -> bytes | None:
    """This PR's ``refactorings.json`` from workflow run ``run_id``, or None if it has none.

    ``github`` must be open (``async with github:``). The artifact is streamed, and githubkit
    closes a client that isn't open as soon as each request returns, before the body is read.

    Raises:
        ArtifactError: if the artifact is corrupt or too big.
        GitHubException: if GitHub refuses or fails a request, or the download breaks off.
    """
    return await _read_artifact(
        github,
        owner,
        repo,
        run_id,
        REPORT_ARTIFACTS,
        MAX_ARTIFACT_BYTES,
        lambda artifact: read_refactorings_json(artifact, pr_number),
    )


async def download_semgrep_json(
    github: GitHubClient, owner: str, repo: str, run_id: int
) -> bytes | None:
    """Semgrep's ``semgrep.json`` from workflow run ``run_id``, or None if the run has none.

    ``github`` must be open, as for ``download_refactorings_json``. The artifact holds only
    that file, so the download is capped at the file's own limit.

    Raises:
        ArtifactError: if the artifact is corrupt or too big.
        GitHubException: if GitHub refuses or fails a request, or the download breaks off.
    """
    return await _read_artifact(
        github, owner, repo, run_id, (SEMGREP_ARTIFACT,), MAX_REPORT_BYTES, _read_semgrep_json
    )


async def _read_artifact[T](
    github: GitHubClient,
    owner: str,
    repo: str,
    run_id: int,
    names: tuple[str, ...],
    max_bytes: int,
    read: Callable[[IO[bytes]], T | None],
) -> T | None:
    """Download the first of ``names`` that run ``run_id`` uploaded, and ``read`` it.

    None when the run uploaded none of them, or they expired. Both the listed size and the
    download are capped at ``max_bytes``.
    """
    response = await github.rest.actions.async_list_workflow_run_artifacts(
        owner=owner, repo=repo, run_id=run_id, per_page=100
    )
    found = {a.name: a for a in response.parsed_data.artifacts if not a.expired}
    artifact = next((found[name] for name in names if name in found), None)
    if artifact is None:
        return None
    if artifact.size_in_bytes > max_bytes:
        raise ArtifactError(f"Artifact {artifact.name} is too big")
    download = await github.rest.actions.async_download_artifact(
        owner=owner, repo=repo, artifact_id=artifact.id, archive_format="zip", stream=True
    )
    with tempfile.SpooledTemporaryFile(max_size=_SPOOL_BYTES) as buffer:
        size = 0
        async for chunk in download.aiter_bytes():
            # The listed size could be stale, so the download itself is capped too.
            size += len(chunk)
            if size > max_bytes:
                raise ArtifactError(f"Artifact {artifact.name} is too big")
            buffer.write(chunk)
        buffer.seek(0)
        return await asyncio.to_thread(read, buffer)


def read_refactorings_json(artifact: IO[bytes], pr_number: int) -> bytes | None:
    """This PR's ``refactorings.json`` from a downloaded artifact zip, or None if it has none.

    Raises:
        ArtifactError: if the artifact is corrupt or a file in it is too big.
    """
    try:
        with zipfile.ZipFile(artifact) as archive:
            names = set(archive.namelist())
            if _REPORT_FILE in names:
                return _read_capped(archive.open(_REPORT_FILE), _REPORT_FILE)
            if _SITE_FILE not in names:
                return None
            if archive.getinfo(_SITE_FILE).file_size > MAX_ARTIFACT_BYTES:
                raise ArtifactError("The Pages site artifact is too big")
            with archive.open(_SITE_FILE) as site:
                return _read_from_site(site, pr_number)
    except (zipfile.BadZipFile, tarfile.TarError, EOFError) as error:
        raise ArtifactError("Corrupt RefactoringMiner artifact") from error


def _read_semgrep_json(artifact: IO[bytes]) -> bytes | None:
    """``semgrep.json`` from a downloaded artifact zip, or None if it isn't there.

    Raises:
        ArtifactError: if the artifact is corrupt or the file is too big.
    """
    try:
        with zipfile.ZipFile(artifact) as archive:
            if _SEMGREP_FILE not in archive.namelist():
                return None
            return _read_capped(archive.open(_SEMGREP_FILE), _SEMGREP_FILE)
    except (zipfile.BadZipFile, EOFError) as error:
        raise ArtifactError("Corrupt Semgrep artifact") from error


def _read_from_site(site: IO[bytes], pr_number: int) -> bytes | None:
    wanted = f"refactorings/pr-{pr_number}/{_REPORT_FILE}"
    # "r|" streams the tar member by member, so the site is never held in memory.
    with tarfile.open(fileobj=site, mode="r|") as archive:
        for member in archive:
            if member.isfile() and member.name.removeprefix("./") == wanted:
                report = archive.extractfile(member)
                return None if report is None else _read_capped(report, _REPORT_FILE)
    return None


def _read_capped(stream: IO[bytes], name: str) -> bytes:
    with stream:
        content = stream.read(MAX_REPORT_BYTES + 1)
    if len(content) > MAX_REPORT_BYTES:
        raise ArtifactError(f"{name} is too big")
    return content
