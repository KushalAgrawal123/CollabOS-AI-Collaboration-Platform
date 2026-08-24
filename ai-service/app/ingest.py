import logging
from pathlib import Path

from langchain_text_splitters import RecursiveCharacterTextSplitter
from pypdf import PdfReader

from app.config import settings
from app.db import get_connection
from app.embeddings import embed_texts

log = logging.getLogger(__name__)

_splitter = RecursiveCharacterTextSplitter(chunk_size=800, chunk_overlap=100)


def _extract_text(path: Path, content_type: str) -> str:
    if content_type == "application/pdf":
        reader = PdfReader(str(path))
        return "\n".join((page.extract_text() or "") for page in reader.pages)
    return path.read_text(encoding="utf-8", errors="ignore")


def ingest_document(
    document_id: int,
    organization_id: int,
    project_id: int,
    stored_file_name: str,
    content_type: str,
) -> None:
    path = Path(settings.uploads_dir) / stored_file_name
    if not path.exists():
        log.warning("Document file not found on disk, skipping ingestion: %s", path)
        return

    text = _extract_text(path, content_type)
    if not text.strip():
        log.warning("Document has no extractable text, skipping ingestion: document_id=%s", document_id)
        return

    chunks = _splitter.split_text(text)
    if not chunks:
        return
    vectors = embed_texts(chunks)

    with get_connection() as conn:
        with conn.cursor() as cur:
            # Delete-then-insert handles re-ingestion (e.g. a future re-index
            # after an embedding model change) without leaving stale chunks.
            cur.execute("DELETE FROM document_chunks WHERE document_id = %s", (document_id,))
            for index, (chunk, vector) in enumerate(zip(chunks, vectors)):
                cur.execute(
                    """
                    INSERT INTO document_chunks
                        (document_id, organization_id, project_id, chunk_index, content, embedding)
                    VALUES (%s, %s, %s, %s, %s, %s)
                    """,
                    (document_id, organization_id, project_id, index, chunk, vector),
                )
        conn.commit()
    log.info("Ingested document_id=%s into %d chunks", document_id, len(chunks))


def delete_document_chunks(document_id: int) -> None:
    with get_connection() as conn:
        with conn.cursor() as cur:
            cur.execute("DELETE FROM document_chunks WHERE document_id = %s", (document_id,))
        conn.commit()
