from fastapi import APIRouter
from pydantic import BaseModel

from app.agents.document_assistant import run_document_assistant_agent
from app.agents.project_manager import run_project_manager_agent
from app.config import settings
from app.ingest import ingest_document
from app.interaction_log import log_interaction
from app.llm import llm_configured
from app.rag import summarize_document

router = APIRouter()


class AskRequest(BaseModel):
    project_id: int
    question: str


class ProjectManagerRequest(BaseModel):
    project_id: int


class IngestRequest(BaseModel):
    document_id: int
    organization_id: int
    project_id: int
    stored_file_name: str
    content_type: str


@router.get("/status")
def status():
    return {
        "configured": llm_configured(),
        "provider": settings.llm_provider,
        "model": settings.llm_model if llm_configured() else None,
    }


@router.post("/ask")
def ask(request: AskRequest):
    # Phase 11: this now runs the Document Assistant Agent (LangGraph, tool-calling)
    # instead of the fixed single-shot RAG chain — same request/response shape, so
    # the existing frontend "Ask AI" panel picked this up with zero changes.
    if not llm_configured():
        return {"configured": False, "answer": None, "sources": []}
    with log_interaction("document_assistant", request.project_id, request.question) as record:
        answer, sources = run_document_assistant_agent(request.project_id, request.question)
        record["response_preview"] = answer[:200]
        record["sources"] = sources
    return {"configured": True, "answer": answer, "sources": sources}


@router.post("/agents/project-manager")
def project_manager(request: ProjectManagerRequest):
    if not llm_configured():
        return {"configured": False, "report": None}
    with log_interaction("project_manager", request.project_id, "(project summary)") as record:
        report = run_project_manager_agent(request.project_id)
        record["response_preview"] = report[:200]
    return {"configured": True, "report": report}


@router.post("/documents/{document_id}/summarize")
def summarize(document_id: int):
    if not llm_configured():
        return {"configured": False, "summary": None}
    with log_interaction("summarize", document_id, "(document summary)") as record:
        result = summarize_document(document_id)
        record["response_preview"] = (result.get("summary") or "")[:200]
    return result


@router.post("/ingest")
def manual_ingest(request: IngestRequest):
    """Manual trigger — backfills documents uploaded before ai-service existed,
    or re-indexes after an embedding model change. Not called by the backend;
    for local/admin use."""
    ingest_document(
        request.document_id,
        request.organization_id,
        request.project_id,
        request.stored_file_name,
        request.content_type,
    )
    return {"status": "ingested"}
