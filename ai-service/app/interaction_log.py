import json
import logging
import time
from contextlib import contextmanager
from pathlib import Path

log = logging.getLogger("ai-service.interactions")

_LOG_DIR = Path(__file__).resolve().parent.parent / "logs"
_LOG_DIR.mkdir(exist_ok=True)
_LOG_FILE = _LOG_DIR / "interactions.jsonl"


@contextmanager
def log_interaction(kind: str, subject_id: int, request_summary: str):
    """Wraps one AI interaction (agent run, summarize) with structured
    success/failure/timing logging to logs/interactions.jsonl. Deliberately
    lightweight, not a full eval harness — the point is having real logged
    examples to point at, not a metrics dashboard. `subject_id` is a project_id
    for agent runs or a document_id for summarize — whichever the request is
    scoped to. Callers can add fields (e.g. a response preview) to the yielded
    dict before the `with` block ends."""
    start = time.monotonic()
    record = {"kind": kind, "subject_id": subject_id, "request": request_summary[:500]}
    try:
        yield record
        record["success"] = True
    except Exception as exc:
        record["success"] = False
        record["error"] = str(exc)
        raise
    finally:
        record["duration_ms"] = round((time.monotonic() - start) * 1000, 1)
        record["timestamp"] = time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime())
        with open(_LOG_FILE, "a") as f:
            f.write(json.dumps(record) + "\n")
        log.info(
            "interaction kind=%s subject_id=%s success=%s duration_ms=%s",
            record["kind"], record["subject_id"], record["success"], record["duration_ms"],
        )
