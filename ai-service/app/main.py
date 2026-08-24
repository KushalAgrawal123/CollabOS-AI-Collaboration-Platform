import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.db import init_db
from app.kafka_consumer import start_consumer_thread
from app.routes import router

logging.basicConfig(level=logging.INFO)


@asynccontextmanager
async def lifespan(app: FastAPI):
    init_db()
    start_consumer_thread()
    yield


app = FastAPI(title="CollabOS AI Service", lifespan=lifespan)

# Only the Spring Boot backend calls this service directly (server-to-server) —
# the browser never does — but CORS is harmless to allow for local debugging
# against the FastAPI docs UI.
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:8080", "http://localhost:5173"],
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(router)
