import psycopg
from pgvector.psycopg import register_vector

from app.config import settings


def get_connection() -> psycopg.Connection:
    conn = psycopg.connect(settings.database_url)
    register_vector(conn)
    return conn


def init_db() -> None:
    # The `vector` extension itself is installed once, out of band, by a
    # superuser (the app's own DB role isn't granted CREATE EXTENSION) — see
    # the ai-service README. Everything below is safe to run on every startup.
    with get_connection() as conn:
        with conn.cursor() as cur:
            cur.execute(f"""
                CREATE TABLE IF NOT EXISTS document_chunks (
                    id SERIAL PRIMARY KEY,
                    document_id BIGINT NOT NULL,
                    organization_id BIGINT NOT NULL,
                    project_id BIGINT NOT NULL,
                    chunk_index INT NOT NULL,
                    content TEXT NOT NULL,
                    embedding VECTOR({settings.embedding_dim}) NOT NULL,
                    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                    UNIQUE (document_id, chunk_index)
                )
            """)
            cur.execute("""
                CREATE INDEX IF NOT EXISTS document_chunks_embedding_idx
                ON document_chunks USING hnsw (embedding vector_cosine_ops)
            """)
            cur.execute("""
                CREATE INDEX IF NOT EXISTS document_chunks_project_idx
                ON document_chunks (project_id)
            """)
        conn.commit()
