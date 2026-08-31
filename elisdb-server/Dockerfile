FROM python:3.14-slim AS build

WORKDIR /app

RUN python -m pip install --no-cache-dir --prefix=/install \
    "fastapi>=0.111" \
    "uvicorn[standard]>=0.30" \
    "cryptography>=43.0"

FROM python:3.14-slim AS runtime

ARG VERSION=0.0.0-dev

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
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]