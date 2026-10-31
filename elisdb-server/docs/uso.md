# Utilização do servidor — como tudo funciona

A maior parte das pessoas **nunca usa estes endereços diretamente**: eles são
usados pela app Android para sincronizar. Este guia explica o que se passa
por baixo e ajuda a resolver problemas quando algo corre mal.

## O que a app faz com o servidor

- **Envia dados** (`/api/push`) — os clientes e fotos guardados no telemóvel.
- **Recebe dados** (`/api/pull`) — tudo o que está no servidor, para o telemóvel.
- **Consulta inconsistências** — clientes (`/api/clients`). A listagem de chaves
  (`/api/keys`) é uma operação de operador, feita pela linha de comandos.

Quase tudo o que começa por `/api/` exige que o pedido venha de um telemóvel
com chave registada. As exceções são `/health` e `/api/time`, que são públicos:
o primeiro serve para confirmar que o servidor está vivo e o segundo para a app
medir o desvio do relógio.

## Endpoints (referência)

| Método | Caminho | Para que serve |
| ------ | ------- | -------------- |
| POST   | `/api/push` | Enviar registos (cliente + fotos) para o servidor |
| GET    | `/api/pull` | Obter os registos do servidor (opções `since` e `limit`) |
| GET    | `/api/clients` | Listar clientes (sem fotos) |
| GET    | `/api/keys` | Só para operadores: responde `403` a clientes; a listagem é feita pela CLI |
| GET    | `/api/keys/me` | Ver a chave que está a ser usada |
| DELETE | `/api/clients/{id}` | Apagar um cliente e as suas fotos |
| GET    | `/health` | Verificação de saúde (sem autenticação) |

A gestão de chaves não é feita pela API: usa o menu interativo de
`python -m app.keys` (criar, listar, remover) dentro do contentor. O endpoint
`/api/keys` existe apenas para responder `403` a um cliente autenticado, para
que uma app comprometida não consiga enumerar as chaves dos outros dispositivos.

## Regras dos dados

- **Duplicado:** o nome do cliente é comparado ignorando maiúsculas, minúsculas
  e espaços. Criar dois clientes com o mesmo nome (normalizado) é recusado — a
  app funde o registo local com o existente.
- **Conflito:** quando dois dispositivos guardaram valores diferentes do mesmo
  cliente, vence o registo com o `updated_at` mais recente.
- **Fotos:** guardadas como ficheiros em disco (na pasta `ELISDB_PHOTOS` ou
  `photos/` ao lado da base de dados). Apagar um cliente apaga também as fotos.
- **Nome vazio:** um cliente sem nome é recusado (`400`).

## Resolução de problemas

| O que aparece / acontece | O que está a acontecer | O que fazer |
| ------------------------ | ---------------------- | ----------- |
| `401` ao sincronizar | O telemóvel não tem chave registada (ou a chave está errada) | Emite a chave com `docker compose exec elisdb-server python -m app.keys issue <nome> --server-url <url>` e importa o ficheiro na app (ver [Configuração](configuracao.md)) |
| `401` com relógio "dessincronizado" | O relógio do telemóvel está fora da janela de ±5 minutos do servidor | Ativa a hora automática no telemóvel (e no servidor) e tenta outra vez |
| `duplicate` ao enviar | Já existe um cliente com o mesmo nome | Normal — a app funde com o registo existente |
| `stale` ao enviar | O registo enviado é mais antigo que o guardado | Nada a fazer; o servidor manteve o mais recente |
| `400` ao enviar | O pedido é mal formado (ex.: cliente sem nome) | Verifica os dados no telemóvel |
| `422` ao enviar | O lote excede `ELISDB_MAX_PUSH_RECORDS` ou um cliente tem mais fotos que `ELISDB_MAX_PHOTOS_PER_CLIENT` | Divide o push ou aumenta os limites no servidor (ver [Configuração](configuracao.md)) |
| `429` ao sincronizar | Demasiados pedidos do mesmo IP na janela configurada | Espera a janela terminar; atrás de um proxy configura `ELISDB_TRUST_PROXY=true` para limitar pelo IP real |

## Expor o servidor para a internet (leitura obrigatória)

O servidor **não tem proteção própria de transporte** (sem TLS/HTTPS próprio):
a autenticação é por assinatura dos pedidos. Está pensado para uso privado/local
numa rede de confiança. Se quiseres expô-lo publicamente, põe-no atrás de um
proxy com TLS (ex.: nginx, Caddy) — e nunca expões a chave privada de um
telemóvel fora do ecrã Configuração da tua app. Para um passo a passo com o Nginx
Proxy Manager, vê [Expor o servidor com NPM](nginx-proxy-manager.md). Antes de
partilhares o servidor com outras pessoas, lê
[Segurança e modelo de confiança](seguranca.md): qualquer chave registada pode
ler e alterar todos os dados.

## Passo seguinte

Volta à app e segue a [configuração do telemóvel](configuracao.md) se algo não
sincronizar.