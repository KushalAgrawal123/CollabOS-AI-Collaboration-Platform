from datetime import date
from unittest.mock import MagicMock, patch

from app.agents.tools import make_project_manager_tools


def _mock_connection(rows):
    fake_cursor = MagicMock()
    fake_cursor.fetchall.return_value = rows
    fake_conn = MagicMock()
    fake_conn.__enter__.return_value = fake_conn
    fake_conn.cursor.return_value.__enter__.return_value = fake_cursor
    return fake_conn


def test_list_overdue_tasks_formats_rows_as_bullets():
    rows = [("Fix login bug", date(2020, 1, 1), "Alice")]
    with patch("app.agents.tools.get_connection", return_value=_mock_connection(rows)):
        list_overdue = make_project_manager_tools(project_id=1)[1]
        result = list_overdue.invoke({})

    assert "Fix login bug" in result
    assert "Alice" in result
    assert "2020-01-01" in result


def test_list_overdue_tasks_handles_no_results():
    with patch("app.agents.tools.get_connection", return_value=_mock_connection([])):
        list_overdue = make_project_manager_tools(project_id=1)[1]
        result = list_overdue.invoke({})

    assert result == "No overdue tasks."


def test_list_tasks_shows_unassigned_when_no_assignee():
    rows = [("Untitled work", "TODO", "MEDIUM", None, None)]
    with patch("app.agents.tools.get_connection", return_value=_mock_connection(rows)):
        list_tasks = make_project_manager_tools(project_id=1)[0]
        result = list_tasks.invoke({})

    assert "unassigned" in result
    assert "no due date" in result
