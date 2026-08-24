from langchain_openai import ChatOpenAI

from app.config import settings


def llm_configured() -> bool:
    return bool(settings.llm_api_key)


def get_chat_model() -> ChatOpenAI:
    if not llm_configured():
        raise RuntimeError("LLM is not configured")
    return ChatOpenAI(
        model=settings.llm_model,
        api_key=settings.llm_api_key,
        base_url=settings.llm_base_url or None,
        temperature=0.2,
    )
