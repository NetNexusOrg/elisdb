## Why

`openspec validate --all` falha em 13 das 21 main specs do projeto. Todas
partilham a mesma causa: as secções saltam de `## Purpose` diretamente para
`### Requirement:`, sem o cabeçalho obrigatório `## Requirements`. O conteúdo
(requisitos e cenários) está completo em todas — a falha é exclusivamente de
estrutura de formatação. Uma delas (`client-sync-state`) está ainda na forma de
delta (`## ADDED Requirements`) em vez da forma de main spec (`## Requirements`).
Enquanto não for corrigido, a validação do repositório está quebrada.

## What Changes

- **Inserir o cabeçalho `## Requirements`** entre o `## Purpose` e o primeiro
  `### Requirement:` nas 13 specs afetadas:
  `app-build`, `client-data`, `client-key-import`, `client-location`,
  `client-photos`, `client-screens`, `client-settings`, `client-sync-state`,
  `duplicate-reconciliation`, `server-auth`, `server-key-provisioning`,
  `server-photo-storage`, `system-theme`.
- **Corrigir `client-sync-state`**: trocar o cabeçalho `## ADDED Requirements`
  pela forma de main spec `## Requirements` (mantendo o conteúdo tal como está).
- **Não alterar nenhum conteúdo** de requisitos, cenários ou descrições — mudança
  puramente estrutural/formativa.
- **Verificação final**: `openspec validate --all` passa nas 21 specs.

## Capabilities

### New Capabilities

Nenhuma — não há novas capacidades.

### Modified Capabilities

Nenhuma — não há alteração de requisitos nem de comportamento; a mudança é de
formatação dos ficheiros de spec. O `.openspec.yaml` declara `skip_specs: true`.

## Impact

- `openspec/specs/*/spec.md` (13 ficheiros): inserção do cabeçalho
  `## Requirements` e, em `client-sync-state`, renomeação do cabeçalho delta.
- Sem alterações a código, API, dependências, docs ou build.