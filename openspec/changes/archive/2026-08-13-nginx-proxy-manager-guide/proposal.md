## Why

O servidor ElisDB só sabe falar métodos privados como `http://IP:8000` e não traz
TLS próprio. Para expor o servidor de forma segura e com um nome de domínio
limpo (HTTPS), a via recomendada (e já sugerida em `uso.md`) é colocar um proxy
inverso à frente — e o **Nginx Proxy Manager (NPM)** é a ferramenta mais comum e
simples. No entanto, quem abre a interface do NPM pela primeira vez depara-se com
várias opções sem perceber qual usar ("Proxy Hosts", "Redirection Hosts",
"Streams", "404 Hosts" e, dentro de "Proxy Hosts", os "Custom Locations").
Não existe documentação do ElisDB que responda a esta questão passo a passo.

## What Changes

- **Nova página `docs/nginx-proxy-manager.md`** em `elisdb-server/docs/`:
  guia prático do zero para expor o servidor ElisDB com o Nginx Proxy Manager.
- **Decisão central documentada e justificada**: usar **"Proxy Hosts"** — é a
  única opção que faz *reverse proxy* HTTP/HTTPS para um serviço de backend como
  o ElisDB.
- **Explicação do que são e quando se usam as outras opções**, para o leitor
  saber que não são para este caso:
  - **Redirection Hosts** — devolvem um redirecionamento HTTP (301/302) para
    outro domínio/URL; não encaminham tráfego para um serviço.
  - **Streams** — fazem proxy de tráfego TCP/UDP (ex.: SSH, bases de dados,
    Minecraft); não aplicáveis a uma API HTTP normal.
  - **404 Hosts** — página/registo para domínios sem configuração; não encaminha
    nada.
- **"Custom Locations" dentro de "Proxy Hosts"**: explicar que **não são
  necessários** para o ElisDB — servem apenas para escoar caminhos URL distintos
  de um domínio para backends diferentes. O ElisDB é um único serviço de API
  HTTP; basta uma entrada de proxy na raiz (`/`). Mencionar o caso em que
  fariam sentido.
- **Passos concretos** apoiados na documentação oficial do NPM:
  - pré-requisito: domínio que aponte para a máquina (ex.: DuckDNS ou IP
    estático) e portas 80/443 abertas no router;
  - opção A: NPM em Docker na mesma rede que o servidor ElisDB (rede custom
    Docker — best practice oficial quando o backend também corre em Docker:
    criar rede como `scoobydoo` no exemplo oficial; usar o nome do serviço como
    hostname, ex.: `elisdb-server:8000` ou `npm:8000`);
  - opção B: apontar para o servidor fora de Docker (ex.: IP da LAN `192.168.1.10:8000`);
  - criar o Proxy Host com o nome do domínio, esquema `http`, hostname e porta;
  - ativar SSL com Let's Encrypt no separador SSL/`Force SSL` (o backend continua
    a falar HTTP internamente — o NPM é que mata o TLS);
  - flag "Block Common Exploits" e como confirmar com `curl https://dominio/health`;
  - emitir a chave na app com `--server-url https://dominio` e recapitular o
    fluxo da app.
- **Nota de segurança**: manter o `ELISDB_DB`/`ELISDB_PHOTOS` e a chave privada
  em privado; o NPM expõe apenas o caminho `https://domínio/`; lembrar que a
  autenticação do ElisDB continua a ser por assinatura.
- **Atualizações menores de pontos de entrada**: mencionar a nova página na guia
  `index.md` do servidor e, se necessário, ligação a partir de `uso.md` (nota de
  segurança pública).

## Capabilities

### New Capabilities

Nenhuma — a mudança é de documentação.

### Modified Capabilities

Nenhuma — não há alteração de comportamento do sistema. O `.openspec.yaml`
declara `skip_specs: true`.

## Impact

- `elisdb-server/docs/nginx-proxy-manager.md` (novo).
- `elisdb-server/docs/index.md` (ligação para a nova página).
- `elisdb-server/docs/uso.md` (opcional: ligação a partir da nota de exposição
  pública).
- Sem alterações a código, API, dependências ou build.