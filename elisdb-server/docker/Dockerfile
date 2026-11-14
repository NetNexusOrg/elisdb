FROM python:3.14-slim AS build

WORKDIR /app

# Pinned runtime dependencies (reproducible images).
COPY requirements.txt .
RUN python -m pip install --no-cache-dir --prefix=/install -r requirements.txt

FROM python:3.14-slim AS runtime

# Optional version override. Left empty by default so the server reports the
# version from the VERSION file baked into the image (single source of truth).
ARG VERSION=

WORKDIR /app

COPY --from=build /install /usr/local

# Only the runtime pieces are copied: no tests, docs, configs, .env or data.
COPY VERSION ./VERSION
COPY elisdb ./elisdb
COPY docker/entrypoint.sh /usr/local/bin/entrypoint.sh
RUN chmod +x /usr/local/bin/entrypoint.sh

# The container runs as PUID:PGID (set in compose), so it never needs root and
# never writes root-owned files into the bind mounts.
ENV PYTHONDONTWRITEBYTECODE=1 \
    ELISDB_DB=/app/data/elisdb.db \
    ELISDB_PHOTOS=/app/data/photos \
    ELISDB_CONFIGS_DIR=/app/data/configs \
    ELISDB_APK_DIR=/app/data/apk/latest \
    ELISDB_VERSION=${VERSION}

EXPOSE 8000

ENTRYPOINT ["entrypoint.sh"]
# Single uvicorn worker: the anti-replay cache and the rate limiter are held
# in-process, so running multiple workers would weaken replay protection and
# make rate limits per-worker. Do NOT add `--workers`.
CMD ["uvicorn", "elisdb.main:app", "--host", "0.0.0.0", "--port", "8000"]
