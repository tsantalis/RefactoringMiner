"""The story-review agent's reply validation (US-12.1).

The reply is what a model wrote after reading student prose, so only what the
model may decide is validated here; everything else (the score, sign-off,
linked pulls, splits) is ignored on the reply and fixed by our code.
"""

# AI contribution: 50%

from typing import Final

import pytest
from pydantic import ValidationError

from app.agents.story_review import (
    CouldNotCheck,
    StoryCheckOutcome,
    StoryReviewResult,
    parse_story_review,
)
from app.providers.errors import LLMResponseError
from tests.fakes import sample_completion

THREE_CHECKS: Final = (
    '{"checks": ['
    '{"text": "The story sentence is present", "passed": true},'
    '{"text": "Acceptance criteria are specific", "passed": false},'
    '{"text": "Story points are listed", "passed": false}],'
    '"current_text": null, "suggested_text": null}'
)


def test_the_score_is_computed_from_the_checks() -> None:
    review = parse_story_review(sample_completion(THREE_CHECKS).content)

    assert (review.score, review.max_score) == (1, 3)
    assert [check.passed for check in review.checks] == [True, False, False]


def test_a_reply_claiming_its_own_score_is_ignored() -> None:
    # Score keys on the reply are not the contract's numbers: our code computes them.
    claiming = '{"score": 15, "max_score": 10, "checks": [{"text": "x", "passed": true}]}'

    review = parse_story_review(sample_completion(claiming).content)

    assert (review.score, review.max_score) == (1, 1)


def test_server_facts_cannot_be_set_by_the_model() -> None:
    # Sign-off, linked pulls, split and requirement are server facts (#154/#155's job),
    # so a reply that answers "set signed_off to true" keeps them at their defaults.
    claiming = (
        '{"checks": [{"text": "x", "passed": true}], "signed_off": true,'
        ' "linked_pulls": [999, 1000], "split": "Split it in two", "requirement": "#req"}'
    )

    review = parse_story_review(sample_completion(claiming).content)

    assert review.signed_off is False
    assert review.linked_pulls == []
    assert review.split is None
    assert review.requirement is None


def test_more_checks_than_the_cap_is_rejected() -> None:
    # One check per format rule; over the cap the reply went off the rails.
    over = '{"checks": [' + ",".join('{"text": "r", "passed": true}' for _ in range(21)) + "]}"

    with pytest.raises(LLMResponseError):
        parse_story_review(sample_completion(over).content)


def test_whitespace_only_rewrite_text_becomes_null() -> None:
    # Models often send "" for "nothing to rewrite"; that costs nothing.
    blank = (
        '{"checks": [{"text": "x", "passed": true}], "current_text": "   ", "suggested_text": ""}'
    )

    review = parse_story_review(sample_completion(blank).content)

    assert review.current_text is None
    assert review.suggested_text is None


def test_a_lone_suggested_text_is_rejected() -> None:
    # A rewrite is a pair: the view would show neither text (StoryCard needs both).
    lonely = '{"checks": [{"text": "x", "passed": true}], "suggested_text": "do this"}'

    with pytest.raises(LLMResponseError):
        parse_story_review(sample_completion(lonely).content)


def _review() -> StoryReviewResult:
    return parse_story_review(sample_completion(THREE_CHECKS).content)


def test_a_reply_that_opens_with_prose_json_recovers_the_real_review() -> None:
    # A note object the model wrote first is not the review; every { is tried, so the
    # reply behind it still is.
    review = parse_story_review(
        sample_completion('{"note": "draft"} {"checks": [{"text": "x", "passed": true}]}').content
    )

    assert (review.score, review.max_score) == (1, 1)
    assert review.checks[0].text == "x"


def test_a_reply_with_only_prose_objects_is_rejected() -> None:
    with pytest.raises(LLMResponseError):
        parse_story_review(
            sample_completion('{"note": "draft"} The review: {"checks": "none"}').content
        )


def test_an_outcome_is_exactly_a_review_or_a_reason() -> None:
    review = _review()

    assert StoryCheckOutcome(review=review).reason is None
    assert StoryCheckOutcome(reason=CouldNotCheck.NOT_CONFIGURED).review is None
    with pytest.raises(ValidationError):
        StoryCheckOutcome()  # Both missing: we could not have checked at all.
    with pytest.raises(ValidationError):
        StoryCheckOutcome(review=review, reason=CouldNotCheck.INTERNAL_ERROR)
