## Context

Veja `proposal.md` para a motivação. Estado: o servidor ElisDB é uma API HTTP
(FastAPI) servida na porta 8000, sem TLS próprio — a exposição pública exige um
proxy com TLS (já é notado na secção de segurança de `docs/uso.md`). O projeto
já tem `Dockerfile`/`compose.yaml` (serviço `elisdb-server`, porta 8000), por isso
o cenário principal é: ElisDB em Docker na mesma máquina que o NPM, e o NPM em
primeiro plano com HTTPS.

## Goals / Non-Goals

**Goals:**
- O leitor percebe imediatamente **qual opção do NPM usar** (Proxy Hosts) e **porquê
  não usar** as outras (Redirection, Streams, 404) — sem ter de adivinhar.
- Instruções passo a passo com valores concretos (domínio, hostname, porta)
  funcionais num cenário real (ElisDB em Docker ao lado do NPM e também fora de
  Docker).
- HTTPS ativo com Let's Encrypt, funcionando com o `--server-url https://domínio`
  e a verificação `/health`.
- Fiel à documentação oficial: rede Docker customizada, "Block Common Exploits",
  separador SSL, "Force SSL".

**Non-Goals:**
- Configurar o NPM ou o router por nós (fora do alcance — é modo de leitura).
- Alterar o código/a API/segurança do ElisDB.
- Cobrir load balancing, ACLs com autenticação (username/password) — fora do
  escopo do guia básico; apenas referido como avançado.
- Não traduzir para inglês; manter o português dos restantes guias.

## Decisions

**D1 — "Proxy Hosts" é a escolha para o ElisDB; "Custom Locations" não são
necessários.**
Um Proxy Host encaminha todos os pedidos de um domínio para um backend
HTTP/HTTPS. O ElisDB é **um único serviço de API**; não há caminhos URL a
reencaminhar para serviços diferentes. Logo, uma entrada na raiz (`/`) resolve
tudo.
Alternativa considerada: usar "Custom Locations" para escoar `/api/` para o
ElisDB e outro domínio/caminhos para outros serviços. Rejeitada como cenário
principal — só faz sentido se quiseres "sobre-inserir" o ElisDB num domínio que
já serve outra coisa; menciona-se como nota, não como fluxo principal.

**D2 — Duas opções de backend apresentadas: Docker na mesma rede vs fora de
Docker.**
- **A (recomendada, backend em Docker):** best practice oficial do NPM — criar
  uma rede Docker customizada partilhada entre o NPM e a app; o NPM usa o **nome
  do serviço** como hostname (ex.: `elisdb-server`) e porta 8000, sem publicar a
  porta 8000 no host. Isto é mais seguro (não expõe o ElisDB em todas as
  interfaces) e evita conflitos de porta.
- **B (backend fora de Docker):** apontar para o IP da LAN +
  porta (ex.: `192.168.1.10`, porta `8000`). Útil quando o servidor corre num
  `.venv`.
Alternativa considerada: só apresentar uma opção. Rejeitada porque um leitor
pode ter o servidor de cada uma das formas, e o guia atual prioriza dar o
caminho certo para cada setup.

**D3 — Estrutura da página orientada a "qual opção e depois como".**
1. "Qual opção usar?" com tabela das 4 opções + veredicto Proxy Hosts, e a nota
   dos Custom Locations.
2. Pré-requisitos (domínio + portas 80/443). 3. Opção A (Docker). 4. Opção B
   (fora de Docker). 5. Criar o Proxy Host + SSL. 6. Verificação `/health`.
7. Ligar a app (`--server-url https://domínio`). 8. Segurança.
Isso evita que a tabela técnica (opções NPM) atrase quem só quer pôr o servidor
online.

**D4 — Exemplos ancorados na documentação oficial.**
Usa terminologia e valores reais do NPM (rede `scoobydoo` do exemplo oficial;
host `elisdb-server`; porta 8000; domínio de exemplo `db.exemplo.com`).
"Block Common Exploits", "Force SSL" e o Note sobre os certificados Let's Encrypt
são apresentados como passos concretos.

## Risks / Trade-offs

- [Risco: leitor usar "Custom Locations" por engano e ter o proxy mudo abaixo] → Mitigação: explicação clara e no início de que não são necessários; no fim indica-se o caso em que seriam.
- [Risco: o leitor não ter um domínio público (só IP)] → Mitigação: nota de que o NPM exige um domínio para Let's Encrypt (ACME) ou certificado próprio; mencionar DuckDNS como caminho simples.
- [Risco: backend fora de Docker ficar inacessível por firewall do host] → Mitigação: nota de que é preciso deixar a porta 8000 acessível ao NPM (rede/serviço Docker ou firewall local).
- [Trade-off: o guia usa HTTPS com Let's Encrypt, mas o servidor continua sem TLS] → normal: o TLS fica no NPM; o ElisDB só fala HTTP internamente.

## Migration Plan

Sem migração de dados. A mudança é documental: criar a página nova, ligá-la em
`index.md` (e opcionalmente em `uso.md`), validar com `openspec validate`.

## Open Questions

Nenhuma — a decisão (Proxy Hosts, sem Custom Locations) e o formato do guia estão
resolvidos; detalhes de redação podem ajustar-se durante a implementação.