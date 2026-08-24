from functools import lru_cache

from sentence_transformers import SentenceTransformer

from app.config import settings


@lru_cache(maxsize=1)
def _model() -> SentenceTransformer:
    # Loaded lazily and cached — this runs locally (no API key, no per-call
    # cost), which keeps indexing free and working even when no LLM key is
    # configured. Only answer generation needs the hosted LLM.
    return SentenceTransformer(settings.embedding_model)


def embed_texts(texts: list[str]) -> list[list[float]]:
    vectors = _model().encode(texts, normalize_embeddings=True)
    return vectors.tolist()
