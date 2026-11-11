# Tasks — production-hardening-phase-4

## 1. Servidor

- [x] 1.1 Adicionar `Database.get_photo_path()` sem ler os bytes.
- [x] 1.2 Servir `GET /api/photos/{id}` com `FileResponse` e `Cache-Control`.
- [x] 1.3 Correr a suite completa do servidor (154 testes).

## 2. Dependências

- [x] 2.1 Adicionar `.github/dependabot.yml` para pip, gradle e actions.
- [x] 2.2 Adicionar `pip-audit` não bloqueante ao CI do servidor.

## 3. Android e operação

- [x] 3.1 Documentar o plano de migração do `security-crypto`.
- [x] 3.2 Confirmar nas docs os backups agendados, o restauro e o processo único.

## 4. Fecho

- [x] 4.1 Validar a change com `openspec validate --strict`.
- [x] 4.2 Arquivar após revisão e commit aprovado pelo utilizador.

> Commits e push apenas com confirmação explícita do utilizador.
