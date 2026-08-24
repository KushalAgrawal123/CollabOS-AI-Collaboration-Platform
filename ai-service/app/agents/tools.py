from datetime import date

from langchain_core.tools import tool

from app.db import get_connection
from app.rag import retrieve_chunks


def make_project_manager_tools(project_id: int):
    @tool
    def list_tasks(status: str = "") -> str:
        """List tasks in this project, optionally filtered by status
        (TODO, IN_PROGRESS, DONE). Leave the status argument blank for all tasks."""
        query = """
            SELECT t.title, t.status, t.priority, t.due_date, u.name
            FROM tasks t
            LEFT JOIN users u ON u.id = t.assignee_id
            WHERE t.project_id = %s
        """
        params = [project_id]
        if status:
            query += " AND t.status = %s"
            params.append(status)
        query += " ORDER BY t.due_date NULLS LAST, t.priority DESC"

        with get_connection() as conn:
            with conn.cursor() as cur:
                cur.execute(query, params)
                rows = cur.fetchall()

        if not rows:
            return "No tasks found."
        lines = []
        for title, task_status, priority, due_date, assignee in rows:
            due = due_date.isoformat() if due_date else "no due date"
            lines.append(f"- [{task_status}/{priority}] {title} — due {due}, assigned to {assignee or 'unassigned'}")
        return "\n".join(lines)

    @tool
    def list_overdue_tasks() -> str:
        """List tasks in this project that are past their due date and not yet done."""
        with get_connection() as conn:
            with conn.cursor() as cur:
                cur.execute(
                    """
                    SELECT t.title, t.due_date, u.name
                    FROM tasks t
                    LEFT JOIN users u ON u.id = t.assignee_id
                    WHERE t.project_id = %s AND t.status != 'DONE' AND t.due_date < %s
                    ORDER BY t.due_date
                    """,
                    (project_id, date.today()),
                )
                rows = cur.fetchall()

        if not rows:
            return "No overdue tasks."
        return "\n".join(
            f"- {title} — was due {due.isoformat()}, assigned to {assignee or 'unassigned'}"
            for title, due, assignee in rows
        )

    return [list_tasks, list_overdue_tasks]


def make_document_assistant_tools(project_id: int):
    @tool
    def search_documents(query: str) -> str:
        """Search this project's uploaded documents for information relevant to
        the query. Returns matching excerpts, each labeled with its source file name."""
        chunks = retrieve_chunks(project_id, query)
        if not chunks:
            return "No relevant documents found."
        return "\n\n".join(f"[{c['file_name']}]\n{c['content']}" for c in chunks)

    return [search_documents]
