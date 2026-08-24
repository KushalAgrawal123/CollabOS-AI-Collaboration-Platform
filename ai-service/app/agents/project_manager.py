from langgraph.prebuilt import create_react_agent

from app.agents.tools import make_project_manager_tools
from app.db import get_connection
from app.llm import get_chat_model


def _project_name(project_id: int) -> str:
    with get_connection() as conn:
        with conn.cursor() as cur:
            cur.execute("SELECT name FROM projects WHERE id = %s", (project_id,))
            row = cur.fetchone()
    return row[0] if row else f"project #{project_id}"


def run_project_manager_agent(project_id: int) -> str:
    project_name = _project_name(project_id)
    tools = make_project_manager_tools(project_id)
    agent = create_react_agent(get_chat_model(), tools)

    prompt = (
        f"You are a project manager assistant reviewing the project '{project_name}'. "
        "Use the available tools to inspect its tasks before answering — don't guess. "
        "Identify overdue or at-risk tasks, note any workload imbalance across assignees, "
        "and suggest priorities for the next few days. Keep the report under 200 words, "
        "using short bullet points."
    )
    result = agent.invoke({"messages": [("human", prompt)]})
    return result["messages"][-1].content
