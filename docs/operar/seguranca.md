# Segurança e modelo de confiança

O ElisDB foi desenhado para **um grupo de confiança** (a mesma casa, equipa ou
família), não para utilizadores que não confiam uns nos outros.

## Autenticação não é autorização

Cada telemóvel assina os pedidos com a sua chave ed25519 e o servidor verifica-a
contra a lista de chaves registadas. Uma assinatura válida prova apenas que o
pedido veio de **alguma** chave registada: **não existe ownership**.

Qualquer chave registada pode ler todos os clientes e fotos, criar/editar/apagar
qualquer cliente, substituir fotos e consultar o histórico. É isso que permite
ter o mesmo dataset em vários telemóveis — e é o risco a assumir. O isolamento
faz-se por **servidor**, não por chave.

## O que a auditoria faz (e não faz)

`client_audit` regista cada criação, edição e eliminação aceite: `key_id`,
`username`, o momento da alteração (`updated_at`/`deleted_at` do dispositivo) e os
campos alterados. As eliminações guardam também uma cópia dos dados
(`details.deleted_client`), na mesma transação do tombstone.

Serve para **atribuir** alterações a um ator. Não impede que esse ator altere ou
apague dados de outros.

## Segredos e chaves

A chave privada só existe em `<nome>.config.json`, escrito a `0600` em
`configs/`. A emissão não imprime a seed; `--print-seed` é uma escolha explícita
para depuração. Nada é criado ou revogado no arranque.

Para rodar uma chave comprometida: revoga-a (`python -m elisdb.keys revoke --id
<id>` ou a TUI), emite uma nova e importa o ficheiro novo no telemóvel. A chave
antiga deixa de autenticar imediatamente. Os nomes de utilizador são validados
para impedir `../` e separadores.

## Fotos

O upload exige que o id já tenha sido referenciado por um push (senão `404`). Há
quotas por chave e globais (`ELISDB_MAX_PHOTO_BYTES_PER_KEY`,
`ELISDB_MAX_PHOTO_BYTES_TOTAL`) verificadas antes de escrever, e os órfãos são
limpos periodicamente. Isto limita o dano de uma chave comprometida, mas não
substitui as quotas de disco nem a monitorização do host.

## Recomendações

- **Um servidor por grupo de confiança.** Para isolar grupos, corre instâncias
  separadas (BD, fotos e chaves próprias).
- **Trata qualquer chave como acesso total.** Revoga as que já não usas.
- **Protege o transporte.** O servidor fala HTTP simples: expõe-no por um proxy
  com TLS ([Nginx](nginx.md)), liga-o só ao proxy (`ELISDB_BIND_ADDRESS=127.0.0.1`)
  e define `ELISDB_TRUST_PROXY=true` para o rate limiting usar o IP real.
- **Nunca exponhas nem versiones** `elisdb.db`, `photos/` ou `*.config.json`.
- **Faz backups.** Uma chave comprometida pode apagar dados; o restauro é a única
  recuperação — ver [Backups](backup.md).

## Resumo

| Pergunta | Resposta |
| --- | --- |
| Uma chave só vê os seus clientes? | Não. Vê o dataset inteiro. |
| Só pode editar os seus clientes? | Não. Edita e apaga qualquer um. |
| A auditoria impede alterações de outras chaves? | Não. Só identifica o ator. |
| Como isolar utilizadores? | Servidores separados. |
| Como reduzir a exposição? | TLS no proxy, bind local, chaves revogáveis, rede restrita. |

Detalhe da autenticação: [Referência da API](../referencia/api.md).
