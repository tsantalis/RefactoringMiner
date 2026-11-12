"""Which completed runs of a team's Actions workflows NikolAI reads (US-8, US-15).

Teams run RefactoringMiner (US-8) and Semgrep (US-15) in their own repository, each in a workflow
with a fixed name. NikolAI reads a run's results only when the run succeeded for a pull request
into that same repository. GitHub leaves a run's PR list empty for PRs from forks, so those runs
are never read. A run also counts only while its commit is still the PR's head, so a slow run for
an older push can't replace the results of a newer one.
"""

# AI contribution: 50%

from githubkit_schemas.latest.models import PullRequest

from app.github.payloads import WorkflowRun, WorkflowRunEvent


def succeeded_on_pull_request(event: WorkflowRunEvent, workflow: str) -> bool:
    """Whether the event is a successful run of ``workflow`` for a PR of the repository."""
    run = event.workflow_run
    return (
        event.action == "completed"
        and run.event == "pull_request"
        and run.name == workflow
        and run.conclusion == "success"
        and bool(pull_request_numbers(event))
    )


def pull_request_numbers(event: WorkflowRunEvent) -> list[int]:
    """The numbers of the run's PRs into the repository that sent the event."""
    return [
        pull_request.number
        for pull_request in event.workflow_run.pull_requests
        if pull_request.base.repo.id == event.repository.id
    ]


def is_current_head(pull: PullRequest, run: WorkflowRun) -> bool:
    """Whether the run analysed the PR's current head: a newer push gets its own run."""
    return pull.head.sha == run.head_sha
