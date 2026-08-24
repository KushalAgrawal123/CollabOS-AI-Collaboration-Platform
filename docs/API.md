# API reference

Base URL: `http://localhost:8080/api`. Every route except `/auth/**` and the WebSocket handshake (`/ws/**`) requires `Authorization: Bearer <token>`.

For runnable examples with real request bodies, import [`CollabOS.postman_collection.json`](../CollabOS.postman_collection.json) — it also auto-captures the token and resource ids into collection variables as you go (register/login → `{{token}}`, create org → `{{orgId}}`, etc.).

## Auth

| Method | Path | Notes |
|---|---|---|
| POST | `/auth/register` | Returns `{ token, user }` |
| POST | `/auth/login` | Rate-limited (30 attempts / 60s / IP) |
| GET | `/users/me` | Current user from the JWT |

## Organizations, members, invites

| Method | Path | Notes |
|---|---|---|
| POST | `/organizations` | Creator becomes `OWNER` |
| GET | `/organizations` | Orgs the caller belongs to, with their role in each |
| GET | `/organizations/{orgId}` | |
| GET | `/organizations/{orgId}/members` | |
| PATCH | `/organizations/{orgId}/members/{userId}/role` | Owner only |
| DELETE | `/organizations/{orgId}/members/{userId}` | Owner only; blocked if it would leave zero owners |
| POST | `/organizations/{orgId}/invites` | Owner/Admin; returns a shareable token, no email sent |
| GET | `/organizations/{orgId}/invites` | Owner/Admin |
| DELETE | `/organizations/{orgId}/invites/{inviteId}` | Owner/Admin |
| POST | `/invites/{token}/accept` | Idempotent — accepting twice is a no-op, not an error |

## Projects & tasks

| Method | Path | Notes |
|---|---|---|
| POST | `/organizations/{orgId}/projects` | Editor role required (not `VIEWER`) |
| GET | `/organizations/{orgId}/projects` | |
| GET | `/organizations/{orgId}/projects/{id}` | |
| DELETE | `/organizations/{orgId}/projects/{id}` | Owner/Admin, or the project's own owner |
| POST | `/organizations/{orgId}/projects/{projectId}/tasks` | Publishes a `task-events` Kafka event |
| GET | `/organizations/{orgId}/projects/{projectId}/tasks` | Redis-cached |
| PATCH | `/organizations/{orgId}/projects/{projectId}/tasks/{taskId}` | |
| PATCH | `/organizations/{orgId}/projects/{projectId}/tasks/reorder` | Bulk position update for drag-and-drop |
| DELETE | `/organizations/{orgId}/projects/{projectId}/tasks/{taskId}` | Owner/Admin, or the task's creator |
| GET \| POST | `/organizations/{orgId}/projects/{projectId}/tasks/{taskId}/comments` | |

## Documents

| Method | Path | Notes |
|---|---|---|
| POST | `/organizations/{orgId}/projects/{projectId}/documents` | `multipart/form-data`; PDF/txt/md, 10MB max; optional `taskId` field pins it to a task instead of the project library. Publishes a `document-events` Kafka event (drives both notifications and AI ingestion). |
| GET | `/organizations/{orgId}/projects/{projectId}/documents` | Optional `?taskId=` filter |
| GET | `/organizations/{orgId}/projects/{projectId}/documents/{documentId}/download` | |
| DELETE | `/organizations/{orgId}/projects/{projectId}/documents/{documentId}` | Owner/Admin, or the uploader. Publishes `document-deleted-events`. |

## Chat

| Method | Path | Notes |
|---|---|---|
| GET \| POST | `/organizations/{orgId}/channels` | |
| POST | `/organizations/{orgId}/channels/direct` | Creates or reuses a 1:1 DM channel with `{ userId }` |
| GET \| POST | `/organizations/{orgId}/channels/{channelId}/messages` | Live delivery is over WebSocket (STOMP), this is history |

## Notifications

| Method | Path | Notes |
|---|---|---|
| GET | `/organizations/{orgId}/notifications` | Most recent 30 |
| GET | `/organizations/{orgId}/notifications/unread-count` | |
| POST | `/organizations/{orgId}/notifications/{id}/read` | |
| POST | `/organizations/{orgId}/notifications/read-all` | |

## AI

All AI routes return `{"configured": false, ...}` (never an error) if no LLM key is set anywhere in the chain.

| Method | Path | Notes |
|---|---|---|
| GET | `/organizations/{orgId}/ai/status` | `{ configured, provider, model }` |
| POST | `/organizations/{orgId}/projects/{projectId}/ai/ask` | `{ question }` → Document Assistant Agent; any role including `VIEWER` |
| POST | `/organizations/{orgId}/projects/{projectId}/ai/project-manager` | Project Manager Agent report; any role |
| POST | `/organizations/{orgId}/projects/{projectId}/documents/{documentId}/ai/summarize` | |
| POST | `/organizations/{orgId}/projects/{projectId}/ai/feedback` | `{ agentType, question, answer, rating, correction? }` |
| GET | `/organizations/{orgId}/projects/{projectId}/ai/feedback` | Owner/Admin only |

## WebSocket

STOMP over a raw WebSocket at `/ws` (no SockJS). The JWT goes inside the STOMP `CONNECT` frame's `Authorization` header, not an HTTP header — this endpoint is `permitAll()` at the HTTP layer since the real auth check happens in a `ChannelInterceptor` on `CONNECT`.

| Destination | Direction | Purpose |
|---|---|---|
| `/topic/organizations/{orgId}/projects/{projectId}/tasks` | subscribe | Live task-board updates (created/updated/reordered/deleted) |
| `/topic/organizations/{orgId}/projects/{projectId}/presence` | subscribe | Who's viewing this board |
| `/topic/organizations/{orgId}/channels/{channelId}/messages` | subscribe | Live chat messages |
| `/topic/organizations/{orgId}/channels/{channelId}/typing` | subscribe | Typing indicators |
| `/topic/organizations/{orgId}/channels/{channelId}/presence` | subscribe | Who's viewing this channel |
| `/app/organizations/{orgId}/channels/{channelId}/typing` | send | Client → server: "I'm typing" |

Presence for both boards and channels is driven by the same `PresenceEventListener` reacting to STOMP `SUBSCRIBE`/`DISCONNECT` frames, keyed generically by the destination string — no per-feature branching needed to support both.
