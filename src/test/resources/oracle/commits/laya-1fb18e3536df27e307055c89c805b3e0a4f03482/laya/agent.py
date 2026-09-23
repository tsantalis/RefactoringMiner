"""High-level inference runtime for laya System 1 decision models."""
import json
import os
import threading
import warnings
from typing import Any, Dict, Optional, Union

import numpy as np
import torch

from .common import (
    QTYPES,
    TEMP_MAX,
    TEMP_MIN,
    amp_dtype,
    build_model,
    build_sequence,
    clamp_temperature,
    collate_items,
    confidence_from_probs,
    _resolve_noul_labels,
    render_options,
    temp_bucket,
)


def _fix_tokenizer_config(path: str):
    """Ensure tokenizer_config.json can be loaded across all transformers versions."""
    cfg_file = os.path.join(path, "tokenizer", "tokenizer_config.json")
    if not os.path.exists(cfg_file):
        return
    try:
        with open(cfg_file) as f:
            tcfg = json.load(f)
        changed = False
        if tcfg.get("tokenizer_class") in (None, "TokenizersBackend"):
            tcfg["tokenizer_class"] = "PreTrainedTokenizerFast"
            tcfg.pop("backend", None)
            tcfg.pop("is_local", None)
            changed = True
        # Checkpoints built on the mmBERT/Gemma tokenizer store extra_special_tokens as a list;
        # transformers expects a mapping and raises "'list' object has no attribute 'keys'",
        # which makes AutoTokenizer -- and so the whole model -- fail to load.
        extra = tcfg.get("extra_special_tokens")
        if isinstance(extra, list):
            tcfg["extra_special_tokens"] = {"extra_%d" % i: t for i, t in enumerate(extra)}
            changed = True
        if changed:
            with open(cfg_file, "w") as f:
                json.dump(tcfg, f, indent=2)
    except Exception:
        pass


def _verify_compatibility(model: torch.nn.Module, cfg: Dict, weights: Dict[str, torch.Tensor], model_id: str):
    """Verify that the loaded checkpoint weights and config strictly match the expected architecture."""
    # 1. Verify required configuration attributes
    required_cfg = ["encoder", "head_layers"]
    missing_cfg = [k for k in required_cfg if k not in cfg]
    if missing_cfg:
        raise ValueError(
            f"Incompatible model config for {model_id!r}: missing configuration keys {missing_cfg}. "
            f"Ensure this is a valid RL Agent decision model."
        )

    # 2. Check for required component prefixes
    required_prefixes = ("encoder.", "type_emb.", "scorer.", "act_head.")
    for prefix in required_prefixes:
        if not any(k.startswith(prefix) for k in weights.keys()):
            raise ValueError(
                f"Incompatible model weights for {model_id!r}: checkpoint is missing '{prefix}' parameters. "
                f"Expected an RL Agent decision model with encoder and decision heads."
            )

    # 3. Check for parameter shape mismatches
    model_sd = model.state_dict()
    shape_mismatches = []
    missing_keys = []

    for name, param in model.named_parameters():
        if name not in weights:
            missing_keys.append(name)
        elif tuple(weights[name].shape) != tuple(param.shape):
            shape_mismatches.append(f"  - {name}: expected {tuple(param.shape)}, found {tuple(weights[name].shape)}")

    if shape_mismatches:
        err_details = "\n".join(shape_mismatches[:5])
        if len(shape_mismatches) > 5:
            err_details += f"\n  ... and {len(shape_mismatches) - 5} more mismatched layers."
        raise ValueError(
            f"Model architecture mismatch for {model_id!r}:\n{err_details}\n"
            f"The checkpoint weights do not match the configured model architecture."
        )

    if missing_keys:
        raise ValueError(
            f"Model weights incomplete for {model_id!r}: missing {len(missing_keys)} parameter tensors "
            f"(e.g. {missing_keys[:3]})."
        )


_TOKENIZERS: Dict[tuple, Any] = {}
_TOKENIZERS_LOCK = threading.Lock()


def _load_tokenizer(tok_dir: str, cfg: Dict) -> Any:
    """Tokenizer for a checkpoint, parsed once per process.

    Parsing `tokenizer.json` is not free and `huggingface_hub` caches only the download, not
    the parsed object: the multilingual checkpoint ships a 34 MB, 256k-vocabulary file that
    costs seconds to load, several times the cost of applying its weights. A tokenizer is
    read-only during inference, so one instance is shared by every Agent that wants the same
    directory -- including an Agent the Router has rebuilt after eviction.

    Keyed on the tokenizer directory and its mtime, so a re-download or a config rewritten by
    `_fix_tokenizer_config()` still produces a fresh parse. Non-directory sources (a hub id)
    are not cached, so a caller cannot pin a stale remote revision.
    """
    from transformers import AutoTokenizer

    if not os.path.isdir(tok_dir):
        return AutoTokenizer.from_pretrained(cfg.get("encoder"))

    try:
        stamp = (os.path.abspath(tok_dir), os.path.getmtime(os.path.join(tok_dir, "tokenizer_config.json")))
    except OSError:
        return AutoTokenizer.from_pretrained(tok_dir)

    with _TOKENIZERS_LOCK:
        cached = _TOKENIZERS.get(stamp)
        if cached is not None:
            return cached
        tokenizer = AutoTokenizer.from_pretrained(tok_dir)
        _TOKENIZERS[stamp] = tokenizer
        return tokenizer


class Agent:
    """System 1 decision model runtime: fast, non-autoregressive, calibrated decisions."""

    def __init__(
        self,
        model_id_or_path: str = "convaiinnovations/laya",
        device: Optional[str] = None,
        token: Optional[str] = None,
        subfolder: Optional[str] = None,
        fast: bool = False,
    ):
        """Load a Laya checkpoint.

        `fast=True` swaps the encoder/head forward for the TileLang fast path (CUDA only, needs
        `pip install laya[fast]`); see `Agent.accelerate`.

        `subfolder` selects one checkpoint from a repo that bundles several, e.g.
        `Agent("convaiinnovations/laya", subfolder="multilingual")`. Only that subfolder is
        downloaded, so bundling does not cost every user the whole family.
        """
        from safetensors.torch import load_file
        try:
            from transformers.initialization import no_init_weights
        except ImportError:  # Transformers 4.x
            from transformers.modeling_utils import no_init_weights

        model_dir = model_id_or_path
        if not os.path.exists(model_dir):
            if model_id_or_path.startswith(("/", "./", "../")) or os.path.isabs(model_id_or_path):
                raise FileNotFoundError(
                    f"Local model path not found: {model_id_or_path!r}. "
                    f"Check that the directory exists and that training saved the model successfully."
                )
            from huggingface_hub import snapshot_download

            # Restrict root checkpoints too: the default repo also contains sibling
            # checkpoints, which an unfiltered snapshot would unnecessarily download.
            prefix = f"{subfolder}/" if subfolder else ""
            kw = {
                "token": token or os.environ.get("HF_TOKEN"),
                "allow_patterns": [prefix + name for name in (
                    "rl_agent_config.json", "model.safetensors", "tokenizer/*", "encoder/*",
                )],
            }
            model_dir = snapshot_download(model_id_or_path, **kw)

        if subfolder:
            model_dir = os.path.join(model_dir, subfolder)
            if not os.path.isdir(model_dir):
                raise FileNotFoundError(
                    f"Subfolder {subfolder!r} not found in {model_id_or_path!r}."
                )

        _fix_tokenizer_config(model_dir)

        cfg_path = os.path.join(model_dir, "rl_agent_config.json")
        if not os.path.exists(cfg_path):
            raise FileNotFoundError(
                f"Incompatible model: {model_id_or_path!r} does not contain 'rl_agent_config.json'. "
                f"That file ships with the weights of a Laya checkpoint, so load one of those "
                f"(e.g. 'convaiinnovations/laya') or a directory your own training run wrote."
            )

        with open(cfg_path) as f:
            self.cfg = json.load(f)

        weights_path = os.path.join(model_dir, "model.safetensors")
        if not os.path.exists(weights_path):
            raise FileNotFoundError(
                f"Incompatible model: 'model.safetensors' not found in {model_id_or_path!r}."
            )

        # 1. Device resolution with automatic fallback
        if device is not None:
            target_device = torch.device(device)
            if target_device.type == "cuda" and not torch.cuda.is_available():
                print("Warning: CUDA requested but not available. Falling back to CPU.")
                self.device = torch.device("cpu")
            elif target_device.type == "mps" and not (hasattr(torch.backends, "mps") and torch.backends.mps.is_available()):
                print("Warning: MPS requested but not available. Falling back to CPU.")
                self.device = torch.device("cpu")
            elif target_device.type == "xpu" and not (hasattr(torch, "xpu") and torch.xpu.is_available()):
                print("Warning: XPU requested but not available. Falling back to CPU.")
                self.device = torch.device("cpu")
            else:
                self.device = target_device
        else:
            if torch.cuda.is_available():
                self.device = torch.device("cuda")
            elif hasattr(torch.backends, "mps") and torch.backends.mps.is_available():
                self.device = torch.device("mps")
            elif hasattr(torch, "xpu") and torch.xpu.is_available():
                self.device = torch.device("xpu")
            else:
                self.device = torch.device("cpu")

        tok_dir = os.path.join(model_dir, "tokenizer")
        self.tok = _load_tokenizer(tok_dir, self.cfg)

        enc_dir = os.path.join(model_dir, "encoder")
        # The checkpoint supplies every parameter; skip random/base-model weights.
        with no_init_weights():
            self.model = build_model(self.cfg, encoder_dir=enc_dir if os.path.exists(enc_dir) else None,
                                     pretrained=False)

        # Load weights and verify architectural compatibility
        weights = load_file(weights_path)
        _verify_compatibility(self.model, self.cfg, weights, model_id_or_path)

        self.model.load_state_dict(weights, strict=True)

        # ModernBERT's reference_compile defaults to "auto" and will torch.compile the encoder.
        # That is a loss for the batch sizes Laya runs (a handful of questions per call) and can
        # hang on some platforms, so keep the eager path.
        try:
            self.model.encoder.config.reference_compile = False
        except Exception:
            pass

        # Keep what the checkpoint shipped for inspection, but only ever apply clamped values:
        # some buckets are fitted to sharpen rather than soften (see clamp_temperature).
        self.temperature_raw = self.cfg.get("temperature", [1.0, 1.0, 1.0])
        self.temperature_by_options_raw = self.cfg.get("temperature_by_options", {})
        self.temperature = [clamp_temperature(t) for t in self.temperature_raw]
        self.temperature_by_options = {k: clamp_temperature(v)
                                       for k, v in self.temperature_by_options_raw.items()}
        entries = [(k, v, self.temperature_by_options[k]) for k, v in self.temperature_by_options_raw.items()]
        entries += [("temperature[%d]" % i, t, self.temperature[i]) for i, t in enumerate(self.temperature_raw)]
        rejected = []
        for name, raw, applied in entries:
            try:
                if float(raw) == applied:
                    continue
            except (TypeError, ValueError):
                # Invalid entries already have a neutral fallback; diagnostics must not
                # repeat the failed conversion or prevent the checkpoint from loading.
                pass
            rejected.append("%s=%r -> %g" % (name, raw, applied))
        if rejected:
            warnings.warn(
                "laya: this checkpoint ships invalid temperatures or values outside [%g, %g]; "
                "using %s. Treat confidence from the affected entries as uncalibrated."
                % (TEMP_MIN, TEMP_MAX, ", ".join(rejected)),
                RuntimeWarning, stacklevel=2)
        self.dtype = amp_dtype(self.cfg.get("amp_dtype", "fp16"))

        self._fast = None
        if self.device.type == "cuda" and torch.cuda.get_device_capability(self.device)[0] < 8:
            self.dtype = torch.float16
        elif self.device.type in ("cpu", "mps", "xpu"):
            self.dtype = torch.float32

        # 2. Place on device with graceful fallback to CPU on memory error
        fell_back_from = fell_back_why = None
        try:
            self.model.to(self.device).eval()
        except (RuntimeError, torch.cuda.OutOfMemoryError) as e:
            if self.device.type != "cpu":
                # Record what actually went wrong: the reason matters more than the symptom,
                # and it is the only place the underlying exception is ever surfaced.
                fell_back_from, fell_back_why = self.device, e
                self.device = torch.device("cpu")
                self.dtype = torch.float32
                self.model.to(self.device).eval()
            else:
                raise e

        if fast:
            self.accelerate()

        if fell_back_from is not None:
            print(
                "\n[laya] Warning: could not place the model on %s, so it is running on CPU.\n"
                "  Reason: %s\n"
                "  Inference will be roughly 10-15x slower (~200-500 ms rather than ~35 ms).\n"
                "  If this is a newer NVIDIA GPU (Blackwell / RTX 50-series), your PyTorch build\n"
                "  may not support its CUDA architecture:\n"
                "    pip install --pre torch --index-url https://download.pytorch.org/whl/nightly/cu128\n"
                "  See https://pytorch.org/get-started/locally/\n"
                % (fell_back_from, fell_back_why), flush=True)

    def accelerate(self, use_graphs: bool = True, strict: bool = False):
        """Replace the model forward with the TileLang fast path (fused GEMM/GEGLU/LayerNorm/RoPE kernels,
        sliding-window flash attention, bf16 resident weights, CUDA graphs per shape bucket).

        Same numerics as the stock bf16 autocast path (see benchmarks/bench_fast.py). Returns True if
        enabled. With `strict=False` any failure (no CUDA, tilelang missing) leaves the stock path in place.
        """
        if self._fast is not None:
            return True
        if self.device.type != "cuda":
            if strict:
                raise RuntimeError("laya fast path needs a CUDA device")
            return False
        last = None
        for _attempt in range(2):  # tilelang's JIT cache has been seen to fail once, then succeed
            try:
                from .fast import FastLaya
                self._fast = FastLaya(self.model, max_len=self.cfg.get("max_len", 512), use_graphs=use_graphs)
                break
            except Exception as e:  # tilelang missing / unsupported arch
                last = e
        if self._fast is None:
            if strict:
                raise last
            print("Warning: laya fast path unavailable (%s); using the stock forward." % last)
            return False
        self._stock_forward = self.model.forward
        self.model.forward = self._fast.forward
        return True

    def deaccelerate(self):
        """Restore the stock forward."""
        if self._fast is not None:
            self.model.forward = self._stock_forward
            self._fast = None

    @staticmethod
    def _check_question(qid: str, qdef: Any) -> None:
        """Reject a question that cannot be answered, naming it and what to fix.

        `render_options` reads `criteria` in the shape the question's type expects and the decision
        head needs at least one option, so a malformed definition used to surface from three frames
        down as something that names neither the question nor the problem: `AttributeError:
        'NoneType' object has no attribute 'items'`, `KeyError: 'bool'`, or a `selected index k out
        of range` raised inside the model for a question that ended up with no options at all.
        """
        if not isinstance(qdef, dict):
            raise ValueError("question %r: definition must be a dict, got %s"
                             % (qid, type(qdef).__name__))
        t = qdef.get("type")
        if t not in QTYPES:
            raise ValueError("question %r: unknown type %r; use one of %s" % (qid, t, sorted(QTYPES)))
        if "instructions" not in qdef:
            raise ValueError("question %r: no 'instructions'; add the text the model should answer" % (qid,))
        crit = qdef.get("criteria")
        if t == "choice":
            if not isinstance(crit, (dict, list)):
                raise ValueError("question %r: a choice question takes 'criteria' as a dict of "
                                 "label -> description, or a list of labels" % (qid,))
            if not crit:
                raise ValueError("question %r: a choice question needs at least one criterion" % (qid,))
        elif t == "score":
            if not isinstance(crit, list):
                raise ValueError("question %r: a score question takes 'criteria' as a list of level "
                                 "descriptions, index 0 first" % (qid,))
            if not crit:
                raise ValueError("question %r: a score question needs at least one level" % (qid,))
        elif crit is not None and not isinstance(crit, dict):
            raise ValueError("question %r: a noul question takes 'criteria' as a dict with optional "
                             "'true'/'false' descriptions, or omits it" % (qid,))
        if "labels" in qdef:
            if t != "noul":
                raise ValueError("question %r: 'labels' is only supported for noul questions" % (qid,))
            try:
                _resolve_noul_labels(qdef["labels"])
            except ValueError as e:
                raise ValueError("question %r: %s" % (qid, e)) from e

    @staticmethod
    def _to_internal(qdef: Dict) -> Dict:
        t = qdef["type"]
        crit = qdef.get("criteria")
        if t == "choice" and isinstance(crit, list):
            crit = {c: None for c in crit}
        elif t == "noul" and isinstance(crit, dict):
            # Normalize boolean literal keys to string keys ("true"/"false")
            crit = {str(k).lower(): v for k, v in crit.items()}
        ins = qdef["instructions"]
        if not isinstance(ins, str):
            # `ensure_ascii=False`, matching `serialize_state` and `render_criterion` in
            # common.py and the instructions path in shortlist.py. The default escaped
            # non-ASCII to literal `\uXXXX`, which the tokenizer then read as escape text:
            # on the English checkpoint one German question answered noul=0.1652 as a dict
            # and noul=0.2650 as the identical plain string.
            ins = json.dumps(ins, ensure_ascii=False)
        q = {"t": t, "ins": ins, "crit": crit}
        if "labels" in qdef:
            q["labels"] = qdef["labels"]
        return q

    @torch.no_grad()
    def system_one(self, state: Union[str, dict, list], questions: Dict[str, Dict[str, Any]]) -> Dict[str, Any]:
        """Evaluate typed questions across state in a single, parallel forward pass.

        Args:
            state: Text string, JSON dict, or conversation turn list.
            questions: Dictionary mapping question_id -> question definition.
                - choice: {"type": "choice", "instructions": "...", "criteria": {"optA": "...", ...}}
                - score:  {"type": "score",  "instructions": "...", "criteria": ["lvl0", "lvl1", ...]}
                - noul:   {"type": "noul", "instructions": "...",
                           "criteria": {"false": "...", "true": "..."},
                           "labels": {"false": "B", "true": "A"}}

                  Noul criteria and labels are optional. Labels only control the text shown to the
                  model; their keys retain false/true semantics, and the returned `noul` value is
                  always P(true). Labels default to false/true for compatibility.

        Returns:
            Dictionary with answers, probabilities, calibrated confidence, and token usage.
            Empty questions return empty answers and zero token usage without tokenization
            or a model forward pass.
        """
        ids = list(questions.keys())
        if not ids:
            return {
                "model": "laya-rl-agent",
                "answers": {},
                "usage": {"input_tokens": 0, "output_tokens": 0},
            }
        items = []
        max_len = self.cfg.get("max_len", 512)
        head_max_len = self.cfg.get("head_max_len", 192)
        # A chronological conversation list is serialized newest-last, so the default
        # right-truncation (st[:room]) would silently drop the newest turn. Truncate
        # from the left for lists so the most recent intent is preserved.
        truncate_left = isinstance(state, list)

        for qid in ids:
            self._check_question(qid, questions[qid])
            q = self._to_internal(questions[qid])
            seq, markers = build_sequence(self.tok, state, q, max_len, head_max_len,
                                          truncate_left=truncate_left)
            if len(markers) != len(render_options(q)):
                raise ValueError("question %r options exceed head_max_len=%d" % (qid, head_max_len))
            items.append({"ids": seq, "markers": markers, "qtype": QTYPES[q["t"]]})

        b = collate_items([items], self.tok.pad_token_id)
        use_amp = self.device.type == "cuda"

        try:
            with torch.autocast(device_type=self.device.type, dtype=self.dtype, enabled=use_amp):
                logits, act = self.model(
                    b["input_ids"].to(self.device),
                    b["attention_mask"].to(self.device),
                    b["marker_pos"].to(self.device),
                    b["marker_mask"].to(self.device),
                    b["qtype"].to(self.device),
                )
        except (RuntimeError, torch.cuda.OutOfMemoryError) as e:
            if self.device.type != "cpu" and ("memory" in str(e).lower() or "cuda" in str(e).lower()):
                print("Warning: GPU memory exceeded during inference. Falling back to CPU...")
                self.device = torch.device("cpu")
                self.dtype = torch.float32
                self.model.to(self.device)
                logits, act = self.model(
                    b["input_ids"].to(self.device),
                    b["attention_mask"].to(self.device),
                    b["marker_pos"].to(self.device),
                    b["marker_mask"].to(self.device),
                    b["qtype"].to(self.device),
                )
            else:
                raise e

        logits = logits.float().cpu().numpy()
        act = torch.softmax(act.float(), -1).cpu().numpy()

        answers = {}
        n_tokens = int(b["attention_mask"].sum())

        for r, qid in enumerate(ids):
            q = self._to_internal(questions[qid])
            k = len(items[r]["markers"])
            qt = QTYPES[q["t"]]
            t_scale = self.temperature_by_options.get(temp_bucket(qt, k), self.temperature[qt])
            z = logits[r, :k] / t_scale
            p = np.exp(z - z.max())
            p = p / p.sum()

            conf_score = round(confidence_from_probs(p, k), 4)
            ext = {"act_probability": round(float(act[r, 0]), 4)}

            if q["t"] == "choice":
                keys = list(q["crit"].keys())
                answers[qid] = {
                    "type": "choice",
                    "choice": keys[int(p.argmax())],
                    "probabilities": {kk: round(float(v), 4) for kk, v in zip(keys, p)},
                    "confidence": conf_score,
                    "action": ext,
                }
            elif q["t"] == "score":
                exp_score = float((np.arange(k) * p).sum())
                answers[qid] = {
                    "type": "score",
                    "score": round(exp_score, 4),
                    "legend": {str(i): c for i, c in enumerate(q["crit"])},
                    "probabilities": {str(i): round(float(v), 4) for i, v in enumerate(p)},
                    "confidence": conf_score,
                    "action": ext,
                }
            else:
                answers[qid] = {
                    "type": "noul",
                    "noul": round(float(p[1]), 4),
                    "confidence": round(max(float(p[1]), 1.0 - float(p[1])), 4),
                    "action": ext,
                }

        return {
            "model": "laya-rl-agent",
            "answers": answers,
            "usage": {"input_tokens": n_tokens, "output_tokens": 0},
        }

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        if hasattr(self, "model") and self.model is not None:
            del self.model
            self.model = None
        try:
            import gc
            gc.collect()
            import torch
            if torch.cuda.is_available():
                torch.cuda.empty_cache()
        except Exception:
            pass
        return False

    predict = system_one


RLAgent = Agent


def load(model_id_or_path: str = "convaiinnovations/laya", device: Optional[str] = None,
         token: Optional[str] = None, subfolder: Optional[str] = None, fast: bool = False) -> Agent:
    """Load a Laya agent.

    `subfolder` picks one checkpoint out of a repo that bundles several:

        laya.load("convaiinnovations/laya")                           # English (repo root)
        laya.load("convaiinnovations/laya", subfolder="multilingual")
        laya.load("convaiinnovations/laya", fast=True)                # TileLang GPU fast path
    """
    return Agent(model_id_or_path, device=device, token=token, subfolder=subfolder, fast=fast)
