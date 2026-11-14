# Ligar um telemóvel ao servidor

Este é o fluxo único de acesso: **emitir uma chave no servidor → importar o
ficheiro na app → sincronizar**. Tudo o resto na documentação aponta para aqui.

Precisas de:

- o servidor a correr ([instalação](instalar-servidor.md));
- o **URL** por onde o telemóvel alcança o servidor (na mesma rede Wi-Fi, o IP da
  máquina — `http://192.168.1.10:8000`, nunca `127.0.0.1`);
- a app instalada no telemóvel.

## 1. Emitir a chave

Na pasta `elisdb-server/`, dentro do contentor:

```bash
docker compose exec elisdb-server \
  python -m elisdb.keys issue maria --server-url http://192.168.1.10:8000
```

Resultado:

```
issued key for 'maria' (id issue-XXXXXXXXXXXX)
configuration written to: configs/maria.config.json
```

O ficheiro `configs/maria.config.json` (modo `0600`) é a **única** credencial que
o telemóvel precisa. Contém `server_url`, `user_name` e `private_seed`.

- Sem `--server-url`, o comando usa `ELISDB_SERVER_URL`; sem nenhum dos dois,
  recusa-se a correr.
- `--output <caminho>` escreve noutro sítio.
- A seed privada **não é impressa**. Só existe no ficheiro.

Alternativa sem decorar flags: `python -m elisdb.keys` abre um menu com
*emitir*, *listar* e *remover*. A TUI também trata disto — ver
[gestão de utilizadores](../operar/tui.md).

## 2. Passar o ficheiro para o telemóvel

Email, cabo, pen, transferência na rede — o que for mais prático. Não o abras em
sítios públicos: quem tiver o ficheiro tem acesso total aos dados.

## 3. Importar na app

Na app: menu (⋮) → **Configuração** → **Importar credencial do utilizador** →
escolhe `maria.config.json`.

O URL, a chave e o nome do utilizador ficam preenchidos de uma vez. O cartão
**Sincronização** é só de leitura.

> A app exige a configuração completa para arrancar. Sem ela mostra **Acesso
> Negado** com o botão **Abrir configuração**.
>
> `http://` só é aceite para localhost, redes privadas (`10/8`, `172.16/12`,
> `192.168/16`), link-local, CGNAT e IPv6 privado. Para um servidor na internet
> usa `https://`.

## 4. Sincronizar

No mesmo ecrã, carrega em **Sincronizar**. Deve aparecer **Sincronizado**.

A partir daí a app sincroniza sozinha: ao abrir, ao guardar um cliente e ao
apagar um cliente. O envio corre em segundo plano, sem bloquear o ecrã.

Se aparecer um erro, vê [problemas](../operar/problemas.md).

## Retirar o acesso

**Revogar é imediato e definitivo para esse dispositivo.** Na TUI
([utilizadores](../operar/tui.md)) ou por linha de comandos:

```bash
docker compose exec elisdb-server python -m elisdb.keys revoke --id issue-XXXXXXXXXXXX
```

O registo fica na base de dados como revogado (não é apagado) e o telemóvel passa
a receber `401`. Para voltar a dar acesso, emite uma chave nova.

> O servidor arranca **sem chaves**. Até emitires a primeira, todos os pedidos da
> app são recusados — é a segurança a funcionar, não uma avaria.
