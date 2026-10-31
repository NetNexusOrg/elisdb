# Segurança e modelo de confiança

Este documento explica **quem pode fazer o quê** no servidor ElisDB. É curto e
importante: o servidor foi desenhado para um grupo de confiança (por exemplo, a
mesma casa ou equipa), não para utilizadores mutuamente não confiáveis.

## Autenticação não é autorização

Cada telemóvel assina os pedidos com a sua chave ed25519. O servidor verifica a
assinatura contra a lista de chaves registadas (`/api/keys` responde `403` às
apps; a gestão é feita pela linha de comandos). Uma assinatura válida prova
apenas que o pedido veio de **alguma** chave registada.

**Não existe ownership por cliente, foto ou chave.** Qualquer chave registada
pode:

- ler **todos** os clientes e **todas** as fotos (`/api/pull`, `/api/clients`,
  `/api/photos/{id}`);
- criar, editar e apagar **qualquer** cliente (`/api/push`,
  `/api/clients/{id}`);
- enviar ou substituir fotos de qualquer cliente (`PUT /api/photos/{id}`);
- consultar o histórico de qualquer cliente (`/api/clients/{id}/history`).

O dataset é **partilhado**: é isso que permite sincronizar o mesmo conjunto de
clientes entre vários telemóveis. A separação de dados faz-se por **servidor**,
não por chave dentro do mesmo servidor.

## O que a auditoria faz (e não faz)

O histórico (`client_audit`) regista, para cada criação ou edição aceite:

- a chave que fez o pedido (`key_id`);
- o nome associado à chave (`username`);
- a hora do registo (o `updated_at` do dispositivo, ou seja, o momento da
  edição — não a hora a que o servidor recebeu o push) e o que mudou (campos e
  fotos).

Isto permite **atribuir** uma alteração a um ator, mas **não impede** que esse
ator altere ou apague dados de outra pessoa. As eliminações deixam um tombstone
(para propagar a remoção), não uma entrada de auditoria com o ator. A auditoria
é um registo de responsabilidade, não um controlo de acesso.

## Recomendações

- **Um servidor por grupo de confiança.** Se precisas de isolamento entre
  utilizadores que não confiam uns nos outros, corre instâncias separadas do
  ElisDB (cada uma com a sua base de dados, pasta de fotos e chaves). Não
  partilhes o mesmo servidor entre esses grupos.
- **Trata qualquer chave como acesso total.** Emitir uma chave é equivalente a
  dar acesso de leitura e escrita a toda a base de dados. Revoga chaves que já
  não são usadas (menu interativo de `python -m app.keys`, opção `3`).
- **Protege o transporte.** O servidor fala HTTP simples. Expõe-no através de
  um proxy com TLS (ver [Nginx Proxy Manager](nginx-proxy-manager.md)) e,
  nesse caso, liga apenas o proxy ao servidor (`ELISDB_BIND_ADDRESS=127.0.0.1`)
  e configura `ELISDB_TRUST_PROXY=true` para o rate limiting usar o IP real.
- **Não exponhas ficheiros internos.** A base de dados (`ELISDB_DB`), a pasta de
  fotos (`ELISDB_PHOTOS`) e os ficheiros `*.config.json` (com a chave privada)
  nunca devem ser servidos nem versionados.
- **Faz cópias de segurança.** Ver [Cópia de segurança e restauro](backup.md).
  Uma chave comprometida pode apagar dados; o restauro é a única recuperação.

## Resumo

| Pergunta | Resposta |
| -------- | -------- |
| Uma chave só vê os seus clientes? | Não. Vê o dataset inteiro. |
| Uma chave só pode editar os seus clientes? | Não. Pode editar/apagar qualquer cliente. |
| A auditoria impede alterações de outras chaves? | Não. Só regista o ator de criações/edições. |
| Como isolar utilizadores? | Servidores separados, um por grupo de confiança. |
| Como reduzir a exposição? | TLS no proxy, bind local, chaves revogáveis e rede restrita. |

Para o modelo de autenticação em detalhe, ver [Configuração](configuracao.md).
