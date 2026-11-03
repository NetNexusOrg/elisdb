# Execução do servidor

Com a instalação feita (ver [Instalação](instalacao.md)), este guia mostra como
subir o servidor em Docker — o único modo suportado (Docker-only) — como saber
se está a funcionar e onde ficam os dados.

## Subir o servidor

A partir da pasta `elisdb-server/`:

```bash
docker compose up -d
```

O serviço fica em `http://localhost:8000`. Para a app num telemóvel (na mesma
rede) o alcançar, usa o **IP da máquina do servidor** (ex.:
`http://192.168.1.10:8000`), nunca `127.0.0.1` — esse endereço aponta para o
próprio telemóvel.

> Se a porta 8000 já estiver ocupada no teu computador, muda o mapeamento
> `8000:8000` para outra porta no `compose.yaml`. Para limitar a interface onde
> a porta é publicada, define `ELISDB_BIND_ADDRESS` (ex.: `127.0.0.1` quando
> usares um proxy TLS — ver [Nginx Proxy Manager](nginx-proxy-manager.md)).

## Como saber se está a funcionar

```bash
curl http://localhost:8000/health
# → {"status":"ok","version":"...","api_version":...}
```

Se a resposta contiver `"status":"ok"`, o servidor está vivo e pronto.

> Os restantes endereços `/api/*` (exceto `/api/time`, público) só respondem a
> telemóveis com a chave registada — até lá devolvem `401`. Isso é o
> comportamento normal e esperado.

## Onde ficam os dados

- Os dados ficam num volume Docker (`elisdb-data`), montado em `/app/data`
  dentro do contentor: a base de dados em `elisdb.db` e as fotos numa pasta
  `photos/`. Sobrevivem a reinícios e recriações do contentor.

Para mudar estes caminhos, vê a referência em [Configuração](configuracao.md).

> **Permissões dos dados:** no arranque, o contentor ajusta o dono de
> `/app/data` para `PUID`/`PGID` (por omissão `1000`/`1000`) antes de iniciar
> o servidor. Isto corrige automaticamente volumes que nascem com dono
> `root` — não precisas de apagar o volume para resolver. Se usares um *bind
> mount* do host, define `PUID`/`PGID` no `compose.yaml` com o id do teu
> utilizador (`id -u` / `id -g`) para que o servidor consiga escrever.

## Publicar uma nova versão da app

Criar a tag `v*` publica o APK no GitHub **e** copia-o para o servidor: o
trabalho `release` do `android-release.yml` envia
`elisdb-<versão>-<código>.apk` para a pasta de atualizações por `rsync` (só em
tags, nunca em `main`). Não é preciso reiniciar o servidor — o ficheiro é
descoberto no pedido seguinte.

1. Sobe `app.versionCode` e `app.versionName` em `elisdb-android/gradle.properties`.
2. Escreve a secção da versão em `elisdb-android/CHANGELOG.md` (`## [1.6.0]`, com
   **Novidades** e **Correções**): é o texto que aparece como descrição do release
   no GitHub. Uma tag sem secção correspondente falha o `release` antes de gerar o
   APK.
3. Cria a tag (`git tag v1.6.0 && git push origin v1.6.0`).
4. Em cada telemóvel: **Sobre** → **Procurar atualizações** → **Transferir** →
   **Instalar**.

### Configurar a publicação automática (uma vez)

O CI autentica-se por chave SSH. Cria uma chave dedicada (nunca a pessoal) e
regista a pública na VPS:

```bash
ssh-keygen -t ed25519 -C "github-actions@elisdb" -f ~/.ssh/elisdb_deploy -N ""
ssh-copy-id -i ~/.ssh/elisdb_deploy.pub ubuntu@vps01   # ou acrescenta a .pub a authorized_keys
gh secret set DEPLOY_SSH_KEY < ~/.ssh/elisdb_deploy
# O runner não conhece aliases do teu ~/.ssh/config: usa o host ou o IP público.
gh secret set DEPLOY_HOST --body "144.21.63.93"
gh secret set DEPLOY_USER --body "ubuntu"
# Opcional: porta (por omissão 22) e destino (por omissão
# docker/elisdb-server/elisdb-data/apk/latest/, relativo à home do utilizador).
gh secret set DEPLOY_PORT --body "22"
# Obrigatório: fixa a host key. O runner recusa publicar sem este secret
# (não faz ssh-keyscan em runtime, para não confiar num host não verificado).
ssh-keyscan -H 144.21.63.93 | gh secret set DEPLOY_KNOWN_HOSTS
```

Enquanto `DEPLOY_SSH_KEY`, `DEPLOY_HOST`, `DEPLOY_USER` ou
`DEPLOY_KNOWN_HOSTS` não existirem, o passo é ignorado com um aviso e o release
continua. Em particular, **sem `DEPLOY_KNOWN_HOSTS` a publicação é saltada** —
nunca é feito um `ssh-keyscan` automático.

Para endurecer, restringe a chave em `~/.ssh/authorized_keys` a essa pasta:

```
restrict,command="rrsync -wo /home/ubuntu/docker/elisdb-server/elisdb-data/apk/latest" ssh-ed25519 AAAA... github-actions@elisdb
```

Com a chave restrita por `rrsync`, o `rsync` já entrega sempre nessa pasta: o
caminho que o cliente indica é **relativo** a ela e um caminho absoluto é
concatenado (acaba em `latest/home/ubuntu/...`). Nesse caso define
`DEPLOY_PATH` como `/`:

```bash
gh secret set DEPLOY_PATH --body "/"
```

A chave restrita também recusa abrir uma shell (`rrsync error:
SSH_ORIGINAL_COMMAND does not run rsync`), o que é o comportamento desejado.
Para testar a chave a partir da tua máquina é preciso ignorar o teu
`~/.ssh/config` (`-F /dev/null`) e desligar as outras identidades
(`-o IdentitiesOnly=yes`); caso contrário outra chave do config responde e o
teste não prova nada:

```bash
ssh -F /dev/null -i ~/.ssh/elisdb_deploy -o IdentitiesOnly=yes ubuntu@144.21.63.93 true
```

### Publicar à mão (alternativa)

1. No GitHub, descarrega o APK do release. Já vem com o nome versionado
   (`elisdb-<versão>-<código>.apk`, por exemplo `elisdb-1.6.0-11.apk`).
2. Copia-o para a pasta de atualizações, mantendo o nome:

   ```bash
   scp elisdb-1.6.0-11.apk ubuntu@vps01:~/docker/elisdb-server/elisdb-data/apk/latest/
   ```

3. Em cada telemóvel: **Sobre** → **Procurar atualizações** → **Transferir** →
   **Instalar**.

O servidor publica o ficheiro com o maior `versionCode` que encontrar na pasta;
nomes fora do padrão são ignorados (podes deixar o build seguinte ao lado). Os
endpoints são autenticados e o download serve sempre o ficheiro descoberto —
não aceita nome de ficheiro. Para retirar a atualização, apaga o ficheiro.

Dentro do contentor a pasta é `/app/data/apk/latest` (volume `elisdb-data`);
com *bind mount* corresponde a `elisdb-data/apk/latest` no host. O caminho pode
ser mudado com `ELISDB_APK_DIR`.

## Provisionar utilizadores automaticamente

Podes declarar os utilizadores no `compose.yaml` e o contentor cria as chaves
no arranque, sem entrar no contentor:

```yaml
environment:
  ELISDB_USERS: "maria,joao"      # lista separada por vírgulas
  ELISDB_SERVER_URL: "http://192.168.1.10:8000"   # URL que os telemóveis usam
```

- No arranque, o contentor emite uma chave para cada utilizador que ainda não
  tenha chave confiada (não reemite quem já tem — os telemóveis já ligados
  continuam a funcionar).
- Os ficheiros de configuração ficam em `/app/data/configs/` dentro do volume.
  Recupera-os com:

  ```bash
  docker cp elisdb-server:/app/data/configs/maria.config.json .
  ```

  (ou monta `/app/data/configs` como *bind mount* para aparecerem no host) e
  importa cada ficheiro na app — ver [Configuração](configuracao.md).

- Se limpar `ELISDB_USERS` (ou definir vazio), todas as chaves
  auto-provisionadas são revogadas no arranque seguinte — apenas quem estiver
  na lista tem acesso. Chaves emitidas manualmente via CLI não são afetadas.

## Emitir a chave de um telemóvel

```bash
docker compose exec elisdb-server python -m app.keys issue <nome> --server-url http://<ip-do-host>:8000
```

O ficheiro `<nome>.config.json` é escrito dentro do contentor; copia-o para a
máquina e importa-o na app (ver [Configuração](configuracao.md)).

## Logs e paragem

```bash
docker compose logs -f    # acompanhar os logs
docker compose down       # parar (não apaga o volume de dados)
```

## Atualizar o servidor em produção

O código do servidor publicado tem de ser um commit que o **Server CI** já
testou. Como o repositório é privado e o plano atual do GitHub não permite
exigir checks verdes na `main` (*branch protection*), essa garantia vive no
script de deploy, a partir da raiz do repositório:

```bash
./elisdb-server/scripts/deploy-server.sh --check   # só verifica, não toca na VPS
./elisdb-server/scripts/deploy-server.sh           # copia e reconstrói o contentor
```

O script recusa publicar quando:

- há alterações por commitar em `elisdb-server/` (o que é copiado é a árvore de
  trabalho, não o último commit);
- a `main` local não coincide com `origin/main`;
- o último commit que mexeu em `elisdb-server/` não tem um run do **Server CI**
  concluído com sucesso (em falta, a correr, ou vermelho).

Depois copia `elisdb-server/` para a VPS por `rsync` (sem `compose.yaml`, que em
produção é próprio — *bind mount*, `PUID/PGID` e utilizadores da VPS) e corre
`docker compose up -d --build`. Não é preciso reiniciar para publicar uma versão
da app: o pacote é descoberto pelo nome (ver
[Publicar uma nova versão da app](#publicar-uma-nova-versão-da-app)).

Variáveis opcionais:

| Variável | Por omissão | Para quê |
| --- | --- | --- |
| `ELISDB_DEPLOY_HOST` | `vps01` | host SSH do servidor |
| `ELISDB_DEPLOY_TARGET` | `docker/elisdb-server` | pasta de destino (relativa à home) |
| `ELISDB_HEALTH_URL` | (vazio) | URL de `/health` a confirmar no fim |

`-y` salta a confirmação interativa.

## Produção (considerações)

- **HTTPS é responsabilidade do operador.** O servidor fala HTTP simples (porta
  8000). Para o expor fora da tua rede, põe um proxy inverso à frente (ex.: Nginx
  Proxy Manager) e aponta a app para o URL **HTTPS** — ver
  [Expor com Nginx Proxy Manager](nginx-proxy-manager.md). A app permite HTTP em
  claro para servidores self-hosted em LAN; usar HTTPS é uma decisão de operação.
- **Um único worker.** O servidor corre um único worker uvicorn: o cache
  anti-replay e o rate limiter são mantidos em memória. Não corras múltiplos
  workers nem réplicas sem um backend partilhado (enfraquece a proteção
  anti-replay e torna os limites de ritmo por-worker).
- **Versão de dependências.** As versões do servidor estão fixadas em
  `requirements.txt`, para que as imagens sejam reprodutíveis.

## Passo seguinte

[Configuração](configuracao.md) — como ligar cada telemóvel ao servidor.
