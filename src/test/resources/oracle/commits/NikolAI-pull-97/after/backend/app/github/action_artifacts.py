"""Fetch the RefactoringMiner Action's ``refactorings.json`` from a workflow run (US-8).

The Action (https://github.com/Pogut/RefactoringMiner-action) uploads one of two artifacts:

- ``refactoring-diff`` on private repositories: the exported web view, with
  ``refactorings.json`` at its root;
- ``github-pages`` on public ones: an ``artifact.tar`` of the whole Pages site, with each open
  PR's report at ``refactorings/pr-<number>/refactorings.json``.

Artifacts are written by a team's workflow, so they are untrusted: only the one report file is
read, never extracted to disk, and every size is capped.
"""

# AI contribution: 50% or more AI-generated

import asyncio
import tarfile
import tempfile
import zipfile
from typing import IO, Final

from githubkit import GitHub
from githubkit.auth import AppInstallationAuthStrategy

# The artifact names the Action uploads, in the order NikolAI looks for them.
REPORT_ARTIFACTS: Final = ("refactoring-diff", "github-pages")
MAX_REPORT_BYTES: Final = 10 * 1024 * 1024
# The Pages site holds every open PR's view, Monaco editor included, so it can be large.
MAX_ARTIFACT_BYTES: Final = 500 * 1024 * 1024
# Downloads bigger than this spill from memory to a temporary file.
_SPOOL_BYTES: Final = 16 * 1024 * 1024

_REPORT_FILE: Final = "refactorings.json"
_SITE_FILE: Final = "artifact.tar"


class ArtifactError(ValueError):
    """The artifact is corrupt or bigger than NikolAI accepts."""


async def download_refactorings_json(
    github: GitHub[AppInstallationAuthStrategy], owner: str, repo: str, run_id: int, pr_number: int
) -> bytes | None:
    """This PR's ``refactorings.json`` from workflow run ``run_id``, or None if it has none.

    Raises:
        ArtifactError: if the artifact is corrupt or too big.
        GitHubException: if GitHub refuses or fails a request.
    """
    response = await github.rest.actions.async_list_workflow_run_artifacts(
        owner=owner, repo=repo, run_id=run_id, per_page=100
    )
    found = {a.name: a for a in response.parsed_data.artifacts if not a.expired}
    artifact = next((found[name] for name in REPORT_ARTIFACTS if name in found), None)
    if artifact is None:
        return None
    if artifact.size_in_bytes > MAX_ARTIFACT_BYTES:
        raise ArtifactError(f"Artifact {artifact.name} is too big")
    download = await github.rest.actions.async_download_artifact(
        owner=owner, repo=repo, artifact_id=artifact.id, archive_format="zip", stream=True
    )
    with tempfile.SpooledTemporaryFile(max_size=_SPOOL_BYTES) as buffer:
        size = 0
        async for chunk in download.aiter_bytes():
            # The listed size could be stale, so the download itself is capped too.
            size += len(chunk)
            if size > MAX_ARTIFACT_BYTES:
                raise ArtifactError(f"Artifact {artifact.name} is too big")
            buffer.write(chunk)
        buffer.seek(0)
        return await asyncio.to_thread(read_refactorings_json, buffer, pr_number)


def read_refactorings_json(artifact: IO[bytes], pr_number: int) -> bytes | None:
    """This PR's ``refactorings.json`` from a downloaded artifact zip, or None if it has none.

    Raises:
        ArtifactError: if the artifact is corrupt or a file in it is too big.
    """
    try:
        with zipfile.ZipFile(artifact) as archive:
            names = set(archive.namelist())
            if _REPORT_FILE in names:
                return _read_capped(archive.open(_REPORT_FILE), MAX_REPORT_BYTES)
            if _SITE_FILE not in names:
                return None
            if archive.getinfo(_SITE_FILE).file_size > MAX_ARTIFACT_BYTES:
                raise ArtifactError("The Pages site artifact is too big")
            with archive.open(_SITE_FILE) as site:
                return _read_from_site(site, pr_number)
    except (zipfile.BadZipFile, tarfile.TarError, EOFError) as error:
        raise ArtifactError("Corrupt RefactoringMiner artifact") from error


def _read_from_site(site: IO[bytes], pr_number: int) -> bytes | None:
    wanted = f"refactorings/pr-{pr_number}/{_REPORT_FILE}"
    # "r|" streams the tar member by member, so the site is never held in memory.
    with tarfile.open(fileobj=site, mode="r|") as archive:
        for member in archive:
            if member.isfile() and member.name.removeprefix("./") == wanted:
                report = archive.extractfile(member)
                return None if report is None else _read_capped(report, MAX_REPORT_BYTES)
    return None


def _read_capped(stream: IO[bytes], limit: int) -> bytes:
    with stream:
        content = stream.read(limit + 1)
    if len(content) > limit:
        raise ArtifactError("refactorings.json is too big")
    return content
