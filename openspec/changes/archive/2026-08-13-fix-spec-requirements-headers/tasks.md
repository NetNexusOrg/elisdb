## 1. Corrigir cabeçalhos das main specs

- [x] 1.1 Inserir o cabeçalho `## Requirements` (secção nível 2) entre o `## Purpose` e o primeiro `### Requirement:` nas 12 specs: `app-build`, `client-data`, `client-key-import`, `client-location`, `client-photos`, `client-screens`, `client-settings`, `duplicate-reconciliation`, `server-auth`, `server-key-provisioning`, `server-photo-storage`, `system-theme`.
- [x] 1.2 Em `client-sync-state`, substituir o cabeçalho `## ADDED Requirements` por `## Requirements` (forma de main spec), mantendo o conteúdo.

## 2. Verificação

- [x] 2.1 Confirmar que as 13 specs têm `^## Requirements` e que os requisitos/cenários não mudaram (grep comparativo).
- [x] 2.2 `openspec validate --all` passa nas 21 specs (0 falhas).