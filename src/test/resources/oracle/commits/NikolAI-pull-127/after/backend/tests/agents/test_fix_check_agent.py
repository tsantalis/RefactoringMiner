"""The fix-check agent's reply is untrusted: only well-formed entries are read (US-9.4)."""

# AI contribution: 50%

import json

import pytest

from app.agents.fix_check import parse_push_reply
from app.providers.errors import LLMResponseError

SHA = "a" * 40
FIX = {"item": 1, "commit": SHA, "file_path": "./a.py", "start_line": 3, "end_line": 4}
FINDING = {
    "name": "Unchecked input",
    "description": "Validate n before using it.",
    "severity": "High",
    "category": "correctness",
    "commit": SHA,
    "file_path": "a.py",
    "start_line": 2,
    "end_line": 2,
    "confidence": 0.9,
}


def test_a_reply_gives_its_fixes_and_new_findings() -> None:
    reply = f"Here you go:\n```json\n{json.dumps({'fixed': [FIX], 'findings': [FINDING]})}\n```"

    parsed = parse_push_reply(reply)

    assert [(fix.item, fix.commit, fix.file_path) for fix in parsed.fixes] == [(1, SHA, "a.py")]
    assert [(f.name, f.severity, f.commit) for f in parsed.findings] == [
        ("Unchecked input", "high", SHA)
    ]
    assert parsed.invalid_count == 0


def test_malformed_entries_are_dropped_and_counted() -> None:
    reply = {
        "fixed": [
            FIX,
            FIX | {"item": 0},
            FIX | {"commit": "main"},
            FIX | {"start_line": 5},  # ends before it starts
            "item 2",
        ],
        "findings": [FINDING, FINDING | {"commit": None}, FINDING | {"start_line": 3}],
    }

    parsed = parse_push_reply(json.dumps(reply))

    assert len(parsed.fixes) == 1
    assert len(parsed.findings) == 1
    assert parsed.invalid_count == 6


def test_a_reply_without_fixes_has_none() -> None:
    assert parse_push_reply('{"findings": []}').fixes == []


def test_a_reply_without_a_findings_list_is_rejected() -> None:
    with pytest.raises(LLMResponseError):
        parse_push_reply('{"fixed": []}')
