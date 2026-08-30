## Why

A documentação e as instruções da app e do servidor acumularam inconsistências após as últimas mudanças (app renomeada para "ElisDB" e servidor agora Docker-first). Em concreto, os guias da app ainda indicam emitir chaves com execução manual do servidor (`python -m app.keys issue ...`) em vez do fluxo Docker, e é preciso confirmar que toda a documentação está coerente entre si e com o comportamento real.

## What Changes

- **Review integral** dos READMEs e `docs/` da app e do servidor (e do `README.md` raiz) para detetar instruções desatualizadas, comandos errados, ligações quebradas e inconsistências.
- **Atualizar a documentação da app** para refletir o fluxo Docker-first do servidor: substituir `python -m app.keys issue ...` por `docker compose exec elisdb-server python -m app.keys issue ...` nos pontos que ensinam a emitir a chave do telemóvel (`README.md` e `docs/uso.md`).
- **Uniformizar o nome da app** para "ElisDB" (a grafia da marca) onde ainda aparecer como "elisdb" no contexto do nome visível, mantendo intactos os identificadores técnicos (`com.example.elisdb`, `rootProject.name = "elisdb"`, pastas, env vars).
- **Corrigir referências cruzadas e comandos** que ficaram obsoletos (ex.: secções de testes do servidor removidas, passos de instalação/execução antigos).
- **Alinhar as instruções** entre app e servidor para que o percurso completo (instalar servidor → subir em Docker → emitir chave → importar na app) seja coerente nos dois lados.

## Capabilities

### New Capabilities

_Nenhuma_ — mudança exclusivamente de documentação e instruções.

### Modified Capabilities

_Nenhuma_ — nenhum requisito de especificação muda (a documentação descreve comportamento, não o define).

## Impact

- `README.md` (raiz), `elisdb-android/README.md`, `elisdb-android/docs/` (index, instalacao, execucao, configuracao, uso).
- `elisdb-server/README.md`, `elisdb-server/docs/` (index, instalacao, execucao, configuracao, uso, nginx-proxy-manager).
- Sem alterações de código, comportamento, API, base de dados ou specs.
