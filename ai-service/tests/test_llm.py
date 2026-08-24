from app import llm


def test_llm_configured_false_by_default(monkeypatch):
    monkeypatch.setattr(llm.settings, "llm_api_key", None)
    assert llm.llm_configured() is False


def test_llm_configured_true_when_key_set(monkeypatch):
    monkeypatch.setattr(llm.settings, "llm_api_key", "fake-key-for-testing")
    assert llm.llm_configured() is True


def test_get_chat_model_raises_when_not_configured(monkeypatch):
    monkeypatch.setattr(llm.settings, "llm_api_key", None)
    try:
        llm.get_chat_model()
        assert False, "expected RuntimeError"
    except RuntimeError as exc:
        assert "not configured" in str(exc)
