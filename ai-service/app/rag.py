from langchain_core.prompts import ChatPromptTemplate

from app.db import get_connection
from app.embeddings import embed_texts
from app.llm import get_chat_model, llm_configured

TOP_K = 5
MAX_SUMMARY_CHARS = 12000

_SUMMARY_PROMPT = ChatPromptTemplate.from_messages([
    ("system", "Summarize the following document in 3-5 concise sentences."),
    ("human", "{document}"),
])


def retrieve_chunks(project_id: int, question: str, top_k: int = TOP_K) -> list[dict]:
    query_vector = embed_texts([question])[0]
    with get_connection() as conn:
        with conn.cursor() as cur:
            cur.execute(
                """
                SELECT dc.content, dc.document_id, d.original_file_name,
                       1 - (dc.embedding <=> %s::vector) AS similarity
                FROM document_chunks dc
                JOIN documents d ON d.id = dc.document_id
                WHERE dc.project_id = %s
                ORDER BY dc.embedding <=> %s::vector
                LIMIT %s
                """,
                (query_vector, project_id, query_vector, top_k),
            )
            rows = cur.fetchall()
    return [
        {"content": row[0], "document_id": row[1], "file_name": row[2], "similarity": float(row[3])}
        for row in rows
    ]


def summarize_document(document_id: int) -> dict:
    if not llm_configured():
        return {"configured": False, "summary": None}

    with get_connection() as conn:
        with conn.cursor() as cur:
            cur.execute(
                "SELECT content FROM document_chunks WHERE document_id = %s ORDER BY chunk_index",
                (document_id,),
            )
            rows = cur.fetchall()

    if not rows:
        return {"configured": True, "summary": "This document hasn't been indexed yet."}

    full_text = "\n\n".join(row[0] for row in rows)[:MAX_SUMMARY_CHARS]
    chain = _SUMMARY_PROMPT | get_chat_model()
    result = chain.invoke({"document": full_text})
    return {"configured": True, "summary": result.content}
