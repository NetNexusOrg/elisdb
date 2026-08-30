## Context

Veja `proposal.md` para a motivação. Estado atual: a documentação cobre servidor
(`elisdb-server/docs/`) e app (`elisdb-android/docs/`), cada uma com
`index`, `instalacao`, `execucao`, `configuracao` e `uso`, mais os `README.md`
raiz e de cada subprojeto. O conteúdo está tecnicamente correto, mas abre com
stack/variáveis/endpoints em vez de com o fluxo que o leitor quer executar.

Factos verificados no código que a reescrita deve refletir:
- CLI do servidor: único comando `python -m app.keys issue <nome> [--server-url <url>] [--output <path>]`; comandos individuais removidos.
- Endpoints reais: `/health`, `/api/push`, `/api/pull`, `/api/clients`, `/api/clients/{id}` (DELETE), `/api/keys`, `/api/keys/me` — este último **não** está documentado atualmente.
- `/health` não exige autenticação; todos `/api/*` exigem assinatura (401 sem chave).
- Build release: `keystore.properties` ou env vars; `scripts/build-release.sh` lê `~/.keystores/elisdb-release.env` ou `ELISDB_KEYSTORE_ENV`.
- Runtime override nas Definições sobrepõe `BuildConfig`.

## Goals / Non-Goals

**Goals:**
- Cada guia abre com a tarefa prática e os passos para a executar, com o comando
  ou clique exato, antes de qualquer contexto técnico.
- O fluxo de ligar a app ao servidor é apresentado idêntico de ponta a ponta
  (mesmos passos, mesma terminologia) nos documentos de servidor e de app.
- Referência técnica (endpoints, variáveis de build/ambiente) fica disponível em
  secções de consulta, não como abertura.
- "Resolução de problemas" lista sintoma → causa → correção para os erros reais.
- A reescrita é acompanhada de verificação do código (comandos, endpoints,
  variáveis) para que nenhum exemplo fique desatualizado.

**Non-Goals:**
- Não alterar comportamento do sistema, código, API ou build.
- Não criar nova documentação para os repositórios OpenSpec (dev-workflow).
- Não traduzir as guias para inglês (mantém-se português).
- Não eliminar a referência técnica: manter `uso.md` (API) e `configuracao.md`
  (build) como apêndices de consulta.

## Decisions

**D1 — Documentos orientados por objetivos ("como faço X") em vez de por componente.**
Cada guia reestrutura-se em torno da pergunta do leitor: "como instalo", "como
corro", "como ligo a app", "como resolvo um erro". A referência técnica fica
para o fim, claramente rotulada como consulta opcional.
Alternativa considerada: manter a organização atual e só simplificar texto —
rejeitada porque não resolve o problema real (o leitor não sabe por onde
começar).

**D2 — Fluxo principal repetido no topo com os comandos exatos.**
O caminho «instalar servidor → `python -m app.keys issue <nome> --server-url <url>`
→ transportar `<nome>.config.json` → Definições → Importar configuração →
sincronizar» é o fio condutor de `index.md` de ambos os subprojetos, do `uso.md`
da app e do `configuracao.md` do servidor, sempre com a mesma redação e comandos
verificados. O leitor pode seguir uma única cadeia mental.
Alternativa: espalhar os passos por cada guia — rejeitada por exigir saltos
entre documentos.

**D3 — Linguagem orientada ao utilizador, mantendo código literal.**
Termos internos (virtualenv, allowlist, ed25519, BuildConfig, keystore) são
explicados numa frase curta na primeira ocorrência e colocados entre parêntesis
de contexto, em vez de abrirem secções. Comandos, caminhos e ficheiros mantêm-se
exatos (sem paráfrase) — são a parte que o leitor copia.

**D4 — Secção de resolução de problemas por sintoma observável.**
Tabelas "O que aparece → O que está a acontecer → O que fazer" usando as
mensagens reais da barra de estado da app (`Sincronizado`, `Servidor não
configurado`, `Servidor rejeitou a sincronização`, `Falha na sincronização…`,
relógio dessincronizado) e os códigos HTTP do servidor (401, 400, duplicate).

**D5 — Verificação do código antes de escrever cada exemplo.**
Cada comando/endpoint/variável documentado é confirmado contra o código atual da
app e do servidor antes de ser incluído. Correções de factualidade (ex.:
adicionar `/api/keys/me`, remover referências a comandos extintos, refletir o
fluxo único `app.keys issue`) fazem parte desta reescrita.

## Risks / Trade-offs

- [Risco: simplificar demasiado e omitir segurança — expor o servidor sem TLS na internet] → Mitigação: `uso.md` mantém a nota de segurança (rede privada; proxy TLS para exposição pública) dentro da secção "como expor".
- [Risco: leitores avançados procurarem a referência técnica e não a encontrarem] → Mitigação: cada `README.md` e `index.md` liga explicitamente às secções de referência (API e build) com uma indicação "para consulta".
- [Risco: exemplos ficarem desatualizados após futuras alterações] → Mitigação: a mudança inclui verificação contra o código na fonte (D5); os exemplos passam a referir o fluxo único atual.
- [Trade-off: reescrita mais extensa do texto pode alongar os ficheiros] → os guias mantêm tamanho focado; detalhe de stack/publica em `README.md` do subprojeto, não nos guias de uso.

## Migration Plan

Não existe runtime a migrar. A reescrita é direta: editar os ficheiros de
documentação no lugar e confirmar a consistência dos ficheiros (mesmos nomes),
da ligação entre documentos (links relativos) e dos exemplos contra o código.
Rollback: reversível por histórico de edição; nenhuma migração de dados.

## Open Questions

Nenhuma — as decisões de estrutura e tom estão resolvidas; detalhes de redação
podem ser ajustados durante a implementação sem mudar abordagem ou tarefas.