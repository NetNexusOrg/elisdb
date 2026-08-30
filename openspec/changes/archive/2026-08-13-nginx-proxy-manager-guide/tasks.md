## 1. Decisão de opção NPM documentada

- [x] 1.1 Na nova página, explicar que a opção a usar é **"Proxy Hosts"** (reverse proxy HTTP/HTTPS) e, numa tabela, o que são e quando se usam as restantes: **Redirection Hosts** (redirecionamento HTTP 301/302), **Streams** (proxy TCP/UDP), **404 Hosts** (domínios sem config) — e por que não se aplicam ao ElisDB.
- [x] 1.2 Explicar que os **"Custom Locations"** dentro de um Proxy Host **não são necessários** para o ElisDB (serviço único de API; basta a raiz `/`), indicando o caso em que fariam sentido.

## 2. Página principal do guia

- [x] 2.1 Criar `elisdb-server/docs/nginx-proxy-manager.md` com: pré-requisitos (domínio a apontar para a máquina, portas 80/443 abertas no router, NPM instalado via Docker — `jc21/nginx-proxy-manager` na porta 81 para a interface).
- [x] 2.2 Opção A (recomendada, ElisDB em Docker): criar rede Docker customizada partilhada entre NPM e servidor; usar o nome do serviço como hostname (ex.: `elisdb-server`, porta `8000`); referir a best practice oficial (rede partilhada, sem expor 8000 no host).
- [x] 2.3 Opção B (servidor fora de Docker): apontar o Proxy Host para o IP da LAN + porta (ex.: `192.168.1.10:8000`), com nota de firewall/porta acessível ao NPM.

## 3. HTTPS, verificação e ligação da app

- [x] 3.1 Passos para criar o Proxy Host: domínio, esquema `http`, forward hostname/IP e porta; no separador SSL, pedir certificado Let's Encrypt e ativar "Force SSL"; mencionar "Block Common Exploits".
- [x] 3.2 Verificação com `curl https://dominio/health` (→ `{"status":"ok"}`) e nota de que o backend continua a falar HTTP internamente.
- [x] 3.3 Fluxo da app: emitir a chave com `python -m app.keys issue <nome> --server-url https://dominio` e importar na app; recapitular as Definições.

## 4. Segurança e integração

- [x] 4.1 Secção de segurança: TLS termina no NPM; a autenticação do ElisDB continua por assinatura; manter `ELISDB_DB`/`ELISDB_PHOTOS` e chaves privadas em privado.
- [x] 4.2 Ligar a nova página em `elisdb-server/docs/index.md` (e opcionalmente referência em `uso.md` na nota de exposição pública); validar ligações relativas e `openspec validate`.