# ElisDB

[![Android CI](https://github.com/NetNexusOrg/elisdb/actions/workflows/android-ci.yml/badge.svg)](https://github.com/NetNexusOrg/elisdb/actions/workflows/android-ci.yml)
[![Server CI](https://github.com/NetNexusOrg/elisdb/actions/workflows/server-ci.yml/badge.svg)](https://github.com/NetNexusOrg/elisdb/actions/workflows/server-ci.yml)
[![Release](https://img.shields.io/github/v/release/NetNexusOrg/elisdb)](https://github.com/NetNexusOrg/elisdb/releases)

Gestão de clientes **offline-first** para Android: uma app que cria e edita
clientes, tira fotos e marca localizações no mapa — e um servidor próprio que
guarda tudo e sincroniza entre telemóveis.

A app funciona sozinha, sem servidor. Quando há servidor configurado, sincroniza
ao abrir, ao guardar um cliente e no botão **Sincronizar**.

## Começar em 3 passos

1. **Subir o servidor** (Docker):
   ```bash
   cd elisdb-server
   mkdir -p elisdb-data configs
   docker compose up -d
   curl http://localhost:8000/health
   ```
2. **Emitir a chave de um telemóvel**:
   ```bash
   docker compose exec elisdb-server \
     python -m elisdb.keys issue maria --server-url http://192.168.1.10:8000
   ```
   Gera `configs/maria.config.json`.
3. **Importar na app:** menu (⋮) → **Configuração** → **Importar credencial do
   utilizador** → **Sincronizar**.

Guia completo: [ligar um telemóvel](docs/usar/ligar-telemovel.md).

## O que está aqui

| Diretório | O que é |
|---|---|
| `elisdb-android/` | App Android — Kotlin, Jetpack Compose, Room, Ktor, MapLibre |
| `elisdb-server/` | Servidor — Python, FastAPI, SQLite, autenticação ed25519 |
| `elisdb-telegram-bot/` | Bot que anuncia releases no Telegram |
| `docs/` | Toda a documentação de utilização e operação |
| `openspec/` | Especificações e histórico de mudanças |

## Documentação

- **[Mapa da documentação](docs/index.md)** — começa aqui.
- Utilizar: [instalar o servidor](docs/usar/instalar-servidor.md) ·
  [ligar um telemóvel](docs/usar/ligar-telemovel.md) ·
  [usar a app](docs/usar/app.md)
- Operar: [servidor](docs/operar/servidor.md) ·
  [configuração](docs/operar/configuracao.md) ·
  [backups](docs/operar/backup.md) ·
  [segurança](docs/operar/seguranca.md) ·
  [problemas](docs/operar/problemas.md)
- Referência: [API](docs/referencia/api.md) ·
  [sincronização](docs/referencia/sincronizacao.md)
- Desenvolver: [app](docs/desenvolver/app.md) ·
  [assinatura](docs/desenvolver/assinatura.md) ·
  [deploy](docs/operar/deploy.md)

## Desenvolvimento

```bash
cd elisdb-android && ./gradlew :app:testDebugUnitTest   # testes da app
cd elisdb-server && .venv/bin/python -m pytest          # testes do servidor
```

- Regras do projeto: [`AGENTS.md`](AGENTS.md) e skills em `.opencode/skills/`.
- Alterações significativas começam por uma OpenSpec change (`openspec/`).
- Releases: tag `vX.Y.Z` → CI constrói o APK, cria a GitHub Release e publica no
  servidor ([detalhes](docs/operar/publicar-app.md)).

## Licença

[MIT](LICENSE).
