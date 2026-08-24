-- Runs automatically on first container init (docker-entrypoint-initdb.d),
-- as the postgres superuser -- unlike local dev, no manual superuser step
-- needed here (see the pgvector-from-source note in the project plan for why
-- that workaround exists locally: this project's local Postgres is v14,
-- older than pgvector's Homebrew bottle range).
CREATE EXTENSION IF NOT EXISTS vector;
