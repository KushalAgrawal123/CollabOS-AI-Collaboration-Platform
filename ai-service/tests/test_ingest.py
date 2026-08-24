from app.ingest import _extract_text, _splitter


def test_extract_text_reads_plain_text_files(tmp_path):
    file = tmp_path / "note.txt"
    file.write_text("hello world", encoding="utf-8")

    assert _extract_text(file, "text/plain") == "hello world"


def test_extract_text_reads_markdown_files(tmp_path):
    file = tmp_path / "note.md"
    file.write_text("# Heading\n\nSome content.", encoding="utf-8")

    assert _extract_text(file, "text/markdown") == "# Heading\n\nSome content."


def test_splitter_breaks_long_text_into_multiple_overlapping_chunks():
    long_text = "word " * 1000

    chunks = _splitter.split_text(long_text)

    assert len(chunks) > 1
    assert all(len(chunk) <= 900 for chunk in chunks)  # chunk_size=800 plus overlap slack
