# Expor o servidor com Nginx Proxy Manager (HTTPS)

O servidor ElisDB fala HTTP simples (porta 8000) e não traz o seu próprio
HTTPS. Para o expor na internet com um domínio e ligação segura (HTTPS), a
forma mais simples é pôr um **proxy inverso** à frente dele. Este guia usa o
**Nginx Proxy Manager (NPM)** — uma ferramenta gratuita com interface web que
faz exatamente isto sem teres de escrever configurações de nginx à mão.

## 1. Qual opção do NPM devo usar?

O NPM tem quatro tipos de "hosts". Só **um** é o que precisas:

| Opção no NPM | O que faz | Serve para o ElisDB? |
| ------------ | --------- | -------------------- |
| **Proxy Hosts** | Encaminha os pedidos de um domínio para um serviço (proxy inverso HTTP/HTTPS) | **SIM — é esta.** O ElisDB é uma API HTTP e o Proxy Host reencaminha tudo para ela. |
| Redirection Hosts | Devolve um redirecionamento HTTP (301/302) para outro domínio/URL | Não — não encaminha tráfego, só "salta" para outro endereço. |
| Streams | Faz proxy de tráfego TCP/UDP (ex.: SSH, bases de dados, Minecraft) | Não — o ElisDB é HTTP, não tráfego de rede cru. |
| 404 Hosts | Regista um domínio para mostrar "não encontrado" | Não — só mostra um erro. |

**Conclusão: vais criar um "Proxy Host".**

### E os "Custom Locations" dentro de um Proxy Host?

**Não são necessários para o ElisDB.** Os "Custom Locations" servem apenas para
encaminhar **caminhos URL diferentes** de um mesmo domínio para **serviços
diferentes**. Exemplo: `dominio.com/app` para uma app e `dominio.com/api` para
outra coisa.

O ElisDB é **um único serviço de API**: cria um Proxy Host na raiz (`/`) e está
tudo resolvido. Só considerarias "Custom Locations" se, no mesmo domínio,
quisesses ter o ElisDB ao lado de outro serviço — e nesse caso recomendamos
antes usar domínios separados, que é mais simples.

## 2. Pré-requisitos

- **Um domínio** que aponte para a máquina onde o NPM corre (um nome DNS ou um
  subdomínio — por exemplo, através de DuckDNS ou de um IP estático). O NPM
  precisa de um domínio para pedir o certificado HTTPS (Let's Encrypt).
- **Portas 80 e 443 abertas** no router, encaminhadas para a máquina do NPM
  (port forwarding). O 80 é usado para a prova de domínio do Let's Encrypt; o
  443 para o tráfego HTTPS.
- **O NPM instalado** via Docker. O exemplo oficial mínimo:

  ```yaml
  services:
    app:
      image: 'jc21/nginx-proxy-manager:2.15.1'
      restart: unless-stopped
      ports:
        - '80:80'      # HTTP (prova do Let's Encrypt)
        - '81:81'      # interface de administração do NPM
        - '443:443'    # HTTPS
      volumes:
        - ./data:/data
        - ./letsencrypt:/etc/letsencrypt
  ```

- A interface de administração fica em `http://127.0.0.1:81` (ou
  `http://IP-da-maquina:81`). Na primeira entrada, cria a conta de administrador.

## 3. Ligar o servidor ElisDB

### Servidor em Docker, na mesma rede do NPM

Como o ElisDB corre em Docker na mesma máquina, a **best practice oficial** do
NPM é colocarem ambos numa **rede Docker partilhada**. Assim o NPM alcança o
servidor pelo **nome do serviço**, sem precisar de publicar a porta 8000 no
computador (mais seguro).

1. Cria uma rede Docker para os dois:
   ```bash
   docker network create elisdb-net
   ```
2. No `compose.yaml` do **NPM**, faz-o entrar nessa rede:
   ```yaml
   networks:
     default:
       external: true
       name: elisdb-net
   ```
3. No `compose.yaml` do **servidor ElisDB** (o que o projeto já inclui), faz o
   mesmo — o serviço chama-se `elisdb-server`:
   ```yaml
   networks:
     default:
       external: true
       name: elisdb-net
   ```
   O serviço `elisdb-server` fica com o nome `elisdb-server` dentro da rede.
4. No NPM, o "Forward Hostname / IP" será **`elisdb-server`** e o porto **`8000`**
   (ver secção seguinte).

> Não precisas de publicar a porta 8000 no host (remove o mapeamento `ports` do
> compose do servidor, se quiseres ficar ainda mais fechado): o NPM alcança-o
> pela rede interna. Se mantiveres a porta publicada, limita-a ao próprio host
> com `ELISDB_BIND_ADDRESS=127.0.0.1` (ou no `.env` do `docker compose`): assim
> só o proxy local lhe chega.

## 4. Criar o Proxy Host

1. No NPM: **Hosts** → **Proxy Hosts** → **Add Proxy Host**.
2. **Details**:
   - **Domain Names:** `db.exemplo.com` (o teu domínio real);
   - **Scheme:** `http`;
   - **Forward Hostname / IP:** `elisdb-server`;
   - **Forward Port:** `8000`;
   - **Block Common Exploits:** ativa.
3. **SSL** (separador):
   - **SSL Certificate:** *Request a new SSL Certificate* (Let's Encrypt);
   - **Force SSL:** ativa (redireciona HTTP → HTTPS);
   - **I Agree to the Let's Encrypt Terms of Service:** assinala;
   - **Save** e espera até o certificado ser emitido.

O **TLS fica todo no NPM**: os clientes falam HTTPS com o NPM, e o NPM fala HTTP
com o servidor ElisDB.

### IP real e rate limiting

Atrás do proxy, o servidor vê o IP do contentor do NPM em todos os pedidos. Para
que o limite de pedidos (`429`) seja aplicado por cliente e não por proxy,
define no `compose.yaml` do servidor:

```yaml
environment:
  ELISDB_TRUST_PROXY: "true"
```

O servidor passa a usar o primeiro IP de `X-Forwarded-For`, que o NPM preenche.
**Só ligues esta opção se o servidor só for alcançável através do proxy** —
publica a porta apenas em `127.0.0.1` (`ELISDB_BIND_ADDRESS=127.0.0.1`) ou não
publiques a porta de todo. Se o servidor estiver diretamente acessível, um
cliente pode forjar `X-Forwarded-For` e contornar o limite.

## 5. Verificar que funciona

Com o Proxy Host criado, de qualquer máquina na internet:

```bash
curl https://db.exemplo.com/health
# → {"status":"ok","version":"...","api_version":...}
```

`"status":"ok"` significa que o domínio, o NPM, o certificado e o servidor
estão todos a funcionar em cadeia.

## 6. Ligar a app ao servidor

Agora usas o domínio HTTPS como URL do servidor. No repositório `elisdb-server/`:

```bash
docker compose exec elisdb-server python -m app.keys issue <nome-do-utilizador> --server-url https://db.exemplo.com
```

Isto cria o `<nome>.config.json` com o URL `https://db.exemplo.com`. Copia-o do
contentor para a tua máquina (`docker cp elisdb-server:/app/<nome>.config.json .`)
e importa-o na app: menu (⋮) → **Servidor** → **Importar configuração do servidor**, e
sincroniza (menu ⋮ → **Sincronização**).

## 7. Segurança

- **O HTTPS termina no NPM.** O servidor ElisDB continua a falar HTTP dentro da
  tua máquina/rede — mantém-no em infraestrutura privada. Publica a porta só em
  `127.0.0.1` (`ELISDB_BIND_ADDRESS=127.0.0.1`), sobretudo quando usares
  `ELISDB_TRUST_PROXY=true`.
- **A autenticação do ElisDB não muda**: os pedidos continuam assinados com uma
  chave registada. Um visitante anónimo continua a receber `401` em tudo o que
  não seja `/health`.
- **Qualquer chave registada tem acesso total ao dataset partilhado** (ler,
  editar e apagar todos os clientes/fotos). A auditoria identifica o ator, mas
  não impõe ownership. Para utilizadores que não confiam uns nos outros, usa
  servidores separados — ver [Segurança e modelo de confiança](seguranca.md).
- **Nunca exposes fora da tua infraestrutura** o ficheiro `ELISDB_DB`, a pasta
  `ELISDB_PHOTOS`, nem as chaves privadas (o seed dentro do config ou no
  ecrã Configuração da app).
- Para exposição pública considera restringir o acesso no NPM com *Access Lists*
  (limitar IPs) — é configurável em cada Proxy Host e fora do âmbito deste guia.