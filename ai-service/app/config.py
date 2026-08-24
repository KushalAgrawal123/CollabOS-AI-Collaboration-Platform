from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    database_url: str = "postgresql://collabos:collabos@localhost:5432/collabos"
    uploads_dir: str = "../backend/uploads"

    kafka_bootstrap_servers: str = "localhost:9092"
    kafka_topic_document_events: str = "document-events"
    kafka_topic_document_deleted_events: str = "document-deleted-events"
    kafka_consumer_group: str = "ai-service-ingestion"

    # A hosted LLM API behind a provider-agnostic OpenAI-compatible interface —
    # llm_provider is purely a display label, llm_base_url/llm_api_key/llm_model
    # are what actually configure the client. Swapping providers (Groq,
    # OpenRouter, Together, local Ollama, real OpenAI) is an env var change,
    # never a code change. No key here is ever hardcoded or defaulted.
    llm_provider: str | None = None
    llm_api_key: str | None = None
    llm_base_url: str | None = None
    llm_model: str = "llama-3.1-8b-instant"

    embedding_model: str = "all-MiniLM-L6-v2"
    embedding_dim: int = 384


settings = Settings()
