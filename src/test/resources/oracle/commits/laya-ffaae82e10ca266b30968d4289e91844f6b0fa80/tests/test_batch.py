"""Batch inference: predict_batch packs many states into shared forward passes.

These tests need no model weights. They cover two things:
  1. Contract — predict_batch exists and system_one is defined in terms of it, so the single-state
     and batched paths can never numerically drift apart.
  2. Orchestration — empty input, bad input, per-state row mapping, and batch_size chunking, all
     exercised against a fake whose forward is stubbed. The numerical equivalence of the real
     model path (predict_batch(states) == [system_one(s) for s in states]) is checked with weights
     in tests/test_local_e2e.py.
"""
import inspect
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

import numpy as np  # noqa: E402

from laya import agent as _agent  # noqa: E402
from laya.agent import Agent  # noqa: E402

PASS, FAIL = [], []


def check(name, got, want):
    if got == want:
        PASS.append(name)
    else:
        FAIL.append("%s:\n     got  %r\n     want %r" % (name, got, want))


def check_true(name, cond, detail=""):
    if cond:
        PASS.append(name)
    else:
        FAIL.append("%s %s" % (name, detail))


def check_raises(name, exc, fn):
    try:
        fn()
    except exc:
        PASS.append(name)
    except Exception as e:  # noqa: BLE001
        FAIL.append("%s: raised %r, expected %s" % (name, e, exc.__name__))
    else:
        FAIL.append("%s: did not raise %s" % (name, exc.__name__))


# --------------------------------------------------------------- contract (source inspection)
check_true("contract/predict_batch is defined", callable(getattr(Agent, "predict_batch", None)))

_so = inspect.getsource(Agent.system_one)
check_true("contract/system_one delegates to predict_batch", "self.predict_batch(" in _so)
check_true("contract/predict alias preserved", Agent.predict is Agent.system_one)

_sig = inspect.signature(Agent.predict_batch)
check_true("contract/predict_batch has batch_size kwarg", "batch_size" in _sig.parameters)


# --------------------------------------------------------------- orchestration (weight-free fake)
# We drive the real predict_batch method but stub the three helpers it composes, so no encoder,
# tokenizer vocabulary, or weights are needed. collate_items runs for real on the fake items.
NQ = 2
QUESTIONS = {"a": {"type": "noul", "instructions": "?"}, "b": {"type": "noul", "instructions": "?"}}


def make_fake():
    fake = _agent.Agent.__new__(_agent.Agent)
    fake.tok = type("Tok", (), {"pad_token_id": 0})()
    fake._to_internal = staticmethod(Agent._to_internal).__func__  # reuse the real normalizer
    fake._forward_calls = []

    def _encode_state(state, ids, internal):
        # one 3-token, 2-marker item per question; content is irrelevant to the mapping test
        return [{"ids": [1, 2, 3], "markers": [0, 1], "qtype": 2} for _ in ids]

    def _forward(b):
        n = b["input_ids"].shape[0]
        fake._forward_calls.append(n)
        # deterministic logits so decode is stable; act pre-softmaxed as _forward would return it
        return np.zeros((n, 2), dtype=np.float32), np.full((n, 2), 0.5, dtype=np.float32)

    def _decode_answers(logits, act, items, ids, internal, offset):
        # record the row offset this state was decoded from, to verify per-state alignment
        return {"_offset": offset}

    fake._encode_state = _encode_state
    fake._forward = _forward
    fake._decode_answers = _decode_answers
    return fake


f = make_fake()
check("orchestration/empty states -> empty list", f.predict_batch([], QUESTIONS), [])

f = make_fake()
res = f.predict_batch(["s0", "s1", "s2"], QUESTIONS)
check("orchestration/one result per state", len(res), 3)
check("orchestration/state 0 decoded from row 0", res[0]["answers"]["_offset"], 0)
check("orchestration/state 1 decoded from row NQ", res[1]["answers"]["_offset"], NQ)
check("orchestration/state 2 decoded from row 2*NQ", res[2]["answers"]["_offset"], 2 * NQ)
check_true("orchestration/result shape matches system_one",
           all(set(r) == {"model", "answers", "usage"} for r in res))
check("orchestration/single forward pass by default", f._forward_calls, [6])  # 3 states x 2 questions

f = make_fake()
f.predict_batch(["s0", "s1", "s2"], QUESTIONS, batch_size=1)
check("orchestration/batch_size=1 chunks into three passes", f._forward_calls, [2, 2, 2])

f = make_fake()
f.predict_batch(["s0", "s1", "s2", "s3", "s4"], QUESTIONS, batch_size=2)
check("orchestration/batch_size=2 chunks 5 states as 2+2+1", f._forward_calls, [4, 4, 2])

f = make_fake()
check_raises("orchestration/bare string rejected", TypeError, lambda: f.predict_batch("just a string", QUESTIONS))
f = make_fake()
check_raises("orchestration/bare dict rejected", TypeError, lambda: f.predict_batch({"body": "x"}, QUESTIONS))


# --------------------------------------------------------------------------- report
print("\n%d passed, %d failed" % (len(PASS), len(FAIL)))
for f_ in FAIL:
    print("  FAIL " + f_)
sys.exit(1 if FAIL else 0)
