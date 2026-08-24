import re

from langchain_core.messages import ToolMessage
from langgraph.prebuilt import create_react_agent

from app.agents.tools import make_document_assistant_tools
from app.llm import get_chat_model


def run_document_assistant_agent(project_id: int, question: str) -> tuple[str, list[str]]:
    tools = make_document_assistant_tools(project_id)
    agent = create_react_agent(get_chat_model(), tools)

    prompt = (
        "You are CollabOS's document assistant for this project. Use the search_documents "
        "tool to find relevant information before answering — if a search turns up nothing "
        "relevant, say so honestly rather than guessing.\n\n"
        f"Question: {question}"
    )
    result = agent.invoke({"messages": [("human", prompt)]})
    answer = result["messages"][-1].content

    # Pull real source file names out of what search_documents actually returned,
    # rather than re-querying — this also comes back empty if the agent decided
    # not to search at all, which correctly reflects what happened.
    sources: set[str] = set()
    for message in result["messages"]:
        if isinstance(message, ToolMessage):
            sources.update(re.findall(r"\[([^\]]+)\]", message.content))

    return answer, sorted(sources)
