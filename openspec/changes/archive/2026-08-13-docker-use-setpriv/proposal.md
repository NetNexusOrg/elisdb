# Proposal: Use setpriv instead of su-exec in the Docker image

## Why

O `docker build` falha na VPS com `E: Unable to locate package su-exec`: o
pacote `su-exec` só existe no Alpine, não nos repositórios Debian (a imagem base
é `python:3.14-slim`, Debian trixie). A instalação do drop de privilégios tem de
usar uma ferramenta disponível no Debian.

## What Changes

- Substituir a instalação de `su-exec` por `setpriv` (parte do `util-linux`, já
  presente na imagem base).
- Atualizar o `docker-entrypoint.sh` para usar `setpriv` no drop de privilégios
  (em vez de `su-exec`).

## Capabilities

- **New Capabilities**: nenhuma.
- **Modified Capabilities**: nenhuma — correção pura de tooling/Docker, sem
  mudança de comportamento observável (skip_specs).

## Impact

- `Dockerfile` e `docker-entrypoint.sh`.