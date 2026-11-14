# Expor o servidor com Nginx Proxy Manager (HTTPS)

O servidor fala HTTP simples na porta 8000. Para o expor na internet com domínio
e HTTPS, põe um proxy inverso à frente. Este guia usa o **Nginx Proxy Manager
(NPM)**, que faz tudo por interface web.

## 1. Que opção do NPM usar

**Proxy Host.** É o único tipo que encaminha pedidos HTTP para um serviço. Os
outros (Redirection, Stream, 404) servem para redirecionar, tráfego TCP/UDP ou
páginas de erro.

Cria o Proxy Host na raiz (`/`): o ElisDB é um único serviço. *Custom Locations*
só fariam sentido para dividir o domínio por vários serviços — nesse caso, usa
domínios separados.

## 2. Pré-requisitos

- Um **domínio** a apontar para a máquina do NPM (DNS ou DuckDNS).
- **Portas 80 e 443** encaminhadas no router para essa máquina (o 80 serve a prova
  do Let's Encrypt).
- O **NPM instalado** via Docker:

  ```yaml
  services:
    app:
      image: 'jc21/nginx-proxy-manager:2.15.1'
      restart: unless-stopped
      ports:
        - '80:80'
        - '81:81'    # administração
        - '443:443'
      volumes:
        - ./data:/data
        - ./letsencrypt:/etc/letsencrypt
  ```

  A administração fica em `http://127.0.0.1:81`; na primeira entrada crias a
  conta.

## 3. Ligar o NPM ao servidor

A prática recomendada é uma **rede Docker partilhada**: o NPM chega ao servidor
pelo nome do serviço, sem publicar a porta 8000 no host.

```bash
docker network create elisdb-net
```

Nos dois `compose.yaml` (NPM e ElisDB):

```yaml
networks:
  default:
    external: true
    name: elisdb-net
```

Se preferires manter a porta publicada, limita-a ao host com
`ELISDB_BIND_ADDRESS=127.0.0.1`.

## 4. Criar o Proxy Host

1. **Hosts → Proxy Hosts → Add Proxy Host**.
2. **Details**: domínio `db.exemplo.com`, scheme `http`, forward hostname
   `elisdb-server`, forward port `8000`, *Block Common Exploits* ativo.
3. **SSL**: *Request a new SSL Certificate*, *Force SSL*, aceitar os termos, e
   **Save**.

O TLS termina no NPM; o NPM fala HTTP com o servidor.

### IP real e rate limiting

Atrás do proxy, o servidor vê o IP do NPM. Para o limite de pedidos ser por
cliente, define `ELISDB_TRUST_PROXY: "true"` no servidor — passa a usar o primeiro
IP de `X-Forwarded-For`.

**Só o ligues se o servidor for alcançável apenas pelo proxy**; caso contrário
qualquer cliente pode forjar o header e contornar o limite.

## 5. Verificar

```bash
curl https://db.exemplo.com/health
# → {"status":"ok",...}
```

## 6. Emitir a chave com o domínio

```bash
docker compose exec elisdb-server \
  python -m elisdb.keys issue <nome> --server-url https://db.exemplo.com
```

O ficheiro aparece em `configs/<nome>.config.json` (0600). Importa-o na app — ver
[Ligar um telemóvel](../usar/ligar-telemovel.md).

## 7. Segurança

- HTTPS termina no NPM; mantém o servidor em rede privada e publicado só em
  `127.0.0.1` quando usares `ELISDB_TRUST_PROXY=true`.
- A autenticação não muda: quem não tem chave recebe `401` em tudo menos
  `/health`.
- Qualquer chave tem acesso total ao dataset — ver
  [Segurança](seguranca.md).
- Opcionalmente, restringe o acesso com *Access Lists* (IPs permitidos) no NPM.
