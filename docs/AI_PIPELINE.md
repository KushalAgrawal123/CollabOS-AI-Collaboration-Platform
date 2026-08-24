# AI pipeline

Everything here lives in `ai-service/` (Python, FastAPI) and is called by the Spring Boot backend over plain REST — the AI service never talks directly to the browser, and it never handles auth itself; Spring Boot verifies the caller's org/project membership first and only then forwards the request.

The whole layer follows one rule: **if `LLM_API_KEY` isn't set, every AI endpoint returns `{"configured": false, ...}` immediately, with no side effects.** Ingestion is the one exception — it doesn't need an LLM key at all (see below), so it runs unconditionally.

## 1. Ingestion (RAG indexing)

Triggered by Kafka, not by a direct API call — `ai-service` runs its own consumer group (`ai-service-ingestion`) on the same `document-events` topic the backend's notification consumer reads, and a `document-deleted-events` topic for cleanup.

For each uploaded document:

1. **Extract text** — plain read for `.txt`/`.md`, `pypdf` for `.pdf`.
2. **Chunk** — LangChain's `RecursiveCharacterTextSplitter` (800 chars, 100 overlap).
3. **Embed locally** — `sentence-transformers` (`all-MiniLM-L6-v2`, 384 dimensions), entirely offline, no API key, no per-document cost. This is why search/ingestion work even with zero AI configuration — only the *answer-generation* step below needs a hosted LLM.
4. **Store** — each chunk's text + embedding vector goes into a hand-rolled `document_chunks` table in the same Postgres, via `pgvector`. Deliberately not using LangChain's `PGVector` vectorstore abstraction — the explicit SQL keeps the actual mechanism (embed → store a vector → cosine-similarity search later) visible rather than hidden behind a library.

On a document delete, `ai-service` consumes `document-deleted-events` and removes that document's chunks — nothing points to a document that no longer exists.

One real bug worth noting here: a raw Python `list[float]` bound as a SQL parameter into a `vector <=> ...` comparison fails, because psycopg serializes it as a plain array and pgvector has no `vector <=> double precision[]` operator. Fixed with an explicit `%s::vector` cast in the retrieval query. `INSERT`s never hit this — the target column's declared type drives an implicit cast there.

## 2. Retrieval + agents (LangGraph)

Two independent agents, each built with `langgraph.prebuilt.create_react_agent` — a tool-calling loop where the LLM decides *whether* and *how* to use the tools it's given, rather than a fixed prompt template with a hardcoded retrieval step.

### Document Assistant Agent
- **Tool:** `search_documents(query)` — embeds the query, runs a `pgvector` cosine-similarity search (`embedding <=> query::vector`) scoped to the current project, returns the top-5 chunks labeled by source file name.
- The agent decides whether the question needs a document search at all, and can call the tool with a different phrasing than the user's literal question if that seems more likely to retrieve something useful.
- Source citations in the final response are extracted from the agent's *actual* tool call outputs (regex over the `[filename]` labels), not from a redundant second query — so if the agent never searched, sources come back empty, which is the truthful answer.
- This replaced an earlier, simpler single-shot RAG chain (retrieve top-5 chunks unconditionally, then generate) — same request/response shape, so the frontend "Ask AI" panel needed zero changes when the implementation underneath became agentic.

### Project Manager Agent
- **Tools:** `list_tasks(status?)` and `list_overdue_tasks()` — both run direct SQL against the `tasks`/`users` tables (read-only, no ORM involved on the Python side).
- Given a project, the agent inspects real task data and produces a short bullet-point report: overdue/at-risk items, workload imbalance across assignees, suggested priorities.
- No document retrieval involved — this agent's "knowledge" is live operational state, not indexed text.

## 3. Feedback & observability

Deliberately lightweight — not a full eval harness.

- **User feedback**: 👍/👎 on any AI answer, with an optional free-text correction on 👎, stored in Postgres (`ai_feedback` table, via the Spring Boot backend) tied to the org/project/user and which agent produced the answer. Listable by org Owners/Admins — the point is having real examples to look at, not a dashboard.
- **Interaction logging**: every agent run and summarize call in `ai-service` is wrapped in a context manager (`interaction_log.log_interaction`) that writes one structured JSON line per call to `ai-service/logs/interactions.jsonl` — request, a response preview, success/failure, latency, timestamp. Skipped entirely on the "not configured" no-op path, so the log only ever contains real interactions, never noise from the guard clause.

## Where each piece is defined

| Concern | File |
|---|---|
| Config / env vars | `ai-service/app/config.py` |
| LLM client (provider-agnostic) | `ai-service/app/llm.py` |
| Local embeddings | `ai-service/app/embeddings.py` |
| Chunking + ingestion | `ai-service/app/ingest.py` |
| Kafka consumer | `ai-service/app/kafka_consumer.py` |
| Retrieval + summarization | `ai-service/app/rag.py` |
| Agent tool definitions | `ai-service/app/agents/tools.py` |
| Document Assistant Agent | `ai-service/app/agents/document_assistant.py` |
| Project Manager Agent | `ai-service/app/agents/project_manager.py` |
| Interaction logging | `ai-service/app/interaction_log.py` |
| HTTP routes | `ai-service/app/routes.py` |
| Spring Boot proxy | `backend/.../controller/AiController.java`, `.../service/AiService.java` |
