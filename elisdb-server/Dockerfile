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

# O drop de privilégios para PUID/PGID usa `setpriv`, que já vem com o
# `util-linux` da imagem base (sem instalações adicionais).

ENV ELISDB_DB=data/elisdb.db \
    ELISDB_PHOTOS=data/photos \
    ELISDB_VERSION=${VERSION} \
    PUID=1000 \
    PGID=1000

EXPOSE 8000

COPY docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh
RUN chmod +x /usr/local/bin/docker-entrypoint.sh

COPY . .

ENTRYPOINT ["docker-entrypoint.sh"]
# Single uvicorn worker: the anti-replay cache and the rate limiter are held
# in-process, so running multiple workers would weaken replay protection and
# make rate limits per-worker. Do NOT add `--workers`.
CMD ["uvicorn", "elisdb.main:app", "--host", "0.0.0.0", "--port", "8000"]