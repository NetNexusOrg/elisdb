# Documentação do ElisDB

Tudo o que existe para ler está aqui. Escolhe pela intenção, não pelo nome do
ficheiro.

## Quero instalar e usar

1. [Instalar o servidor](usar/instalar-servidor.md) — Docker, primeira subida.
2. [Ligar um telemóvel](usar/ligar-telemovel.md) — emitir a chave, importar na
   app, primeira sincronização. **É o guia central; os outros apontam para aqui.**
3. [Usar a app](usar/app.md) — criar clientes, fotos, mapa, sincronização
   automática, atualizações.

## Quero operar o servidor

| Guia | Para quê |
|---|---|
| [Servidor](operar/servidor.md) | Subir, parar, logs, dados, modo local vs Docker |
| [TUI de gestão](operar/tui.md) | Menu: estado, arrancar/parar, utilizadores, configuração |
| [Configuração](operar/configuracao.md) | `.env` e tabela completa de variáveis |
| [Backups](operar/backup.md) | Automáticos, verificação, restauro |
| [Segurança](operar/seguranca.md) | Modelo de confiança, chaves, o que a auditoria não faz |
| [Expor com HTTPS](operar/nginx.md) | Nginx Proxy Manager |
| [Publicar uma versão da app](operar/publicar-app.md) | Tag, APK, segredos do CI |
| [Deploy do servidor](operar/deploy.md) | `deploy-server.sh`, VPS |
| [Problemas](operar/problemas.md) | 401, 429, duplicados, sincronização |

## Quero entender o funcionamento

- [API HTTP](referencia/api.md) — endpoints, autenticação, observabilidade.
- [Sincronização](referencia/sincronizacao.md) — `updated_at`, `delivery_ts`,
  conflitos, tombstones, fotos.

## Quero desenvolver

- [App](desenvolver/app.md) — SDK, build, instalar, emulador, testes.
- [Assinatura](desenvolver/assinatura.md) — versão, keystore, credenciais.
- Regras e contexto: [`AGENTS.md`](../AGENTS.md) e `.opencode/skills/`.
- Especificações: [`openspec/`](../openspec/) (não é documentação de uso).

## Histórico

Registos de trabalho já concluído. Não precisas de os ler para usar ou operar o
sistema; ficam para contexto e auditoria.

- [Arquivo](historia/index.md) — upgrade 2.0.0, migração de criptografia,
  revisão do sistema de sincronização.
- [Changelog da app](../elisdb-android/CHANGELOG.md) — o que mudou em cada versão.
- [Changelog do servidor](../elisdb-server/CHANGELOG.md).
