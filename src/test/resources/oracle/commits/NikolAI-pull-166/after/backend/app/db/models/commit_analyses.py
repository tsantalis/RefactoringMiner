"""What a team's Actions workflow analysed: one head commit of a PR (US-8, US-15).

RefactoringMiner (US-8) and Semgrep (US-15) run in the team's repository on each push to a PR,
and the webhook job saves what each found on the PR's head commit as one analysis run.
"""

# AI contribution: 50%

from typing import Literal

from pydantic import BaseModel


class NewCommitAnalysis(BaseModel):
    """The PR, head commit and workflow run a saved analysis belongs to."""

    github_repository_id: int
    pr_number: int
    head_sha: str
    base_sha: str
    pr_state: Literal["open", "closed"]
    pr_merged: bool
    workflow_run_id: int
