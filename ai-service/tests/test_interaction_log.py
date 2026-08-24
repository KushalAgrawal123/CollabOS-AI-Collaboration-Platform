import json

import pytest

from app import interaction_log


def test_log_interaction_records_success(tmp_path, monkeypatch):
    monkeypatch.setattr(interaction_log, "_LOG_FILE", tmp_path / "interactions.jsonl")

    with interaction_log.log_interaction("test_kind", 1, "a question") as record:
        record["response_preview"] = "an answer"

    lines = (tmp_path / "interactions.jsonl").read_text().strip().splitlines()
    assert len(lines) == 1
    data = json.loads(lines[0])
    assert data["success"] is True
    assert data["response_preview"] == "an answer"
    assert data["subject_id"] == 1
    assert "duration_ms" in data


def test_log_interaction_records_failure_and_reraises(tmp_path, monkeypatch):
    monkeypatch.setattr(interaction_log, "_LOG_FILE", tmp_path / "interactions.jsonl")

    with pytest.raises(ValueError):
        with interaction_log.log_interaction("test_kind", 1, "a question"):
            raise ValueError("boom")

    data = json.loads((tmp_path / "interactions.jsonl").read_text().strip())
    assert data["success"] is False
    assert "boom" in data["error"]
