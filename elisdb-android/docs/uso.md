# Utilização da app — ligar ao servidor e usar no dia a dia

Este é o guia de uso: como ligar a app ao servidor na primeira vez e como usá-la
no dia a dia. Assumimos que o servidor está instalado e a correr
(`../elisdb-server/docs/`) e que a app está no telemóvel
([Execução](execucao.md)).

## 1. Ligar a app ao servidor (primeira vez)

Precisas de três coisas: o servidor a correr, a chave emitida nesse servidor, e
a importação dessa chave na app.

### 1.1 Emitir a chave (no servidor)

No computador do servidor, na pasta `elisdb-server/`:

```bash
docker compose exec elisdb-server python -m app.keys issue <nome-do-utilizador> --server-url <URL-do-servidor>
```

Exemplo:

```bash
docker compose exec elisdb-server python -m app.keys issue maria --server-url http://192.168.1.10:8000
```

Isto cria o ficheiro **`maria.config.json`** — o único ficheiro de que a app
precisa. Transfere-o para o telemóvel.

> **Primeira vez:** o servidor começa sem nenhuma chave confiada. Até emitires
> a primeira, a sincronização da app devolve `401` — é a segurança a funcionar.
> Este comando `issue` é o **único** caminho para dar acesso: não existem
> comandos separados para gerar, registar, listar ou remover chaves.

### 1.2 Importar a chave na app

Na app: menu (⋮) → **Configuração** → **Importar credencial do utilizador** → escolhe
o `maria.config.json`. O **URL do servidor** e a **chave de assinatura** ficam
preenchidos em simultâneo e a sincronização arranca logo.

(O cartão **Sincronização**, no ecrã **Configuração**, mostra o URL, a chave mascarada e
o utilizador associado, em modo só de leitura; a credencial entra sempre pela
importação do ficheiro. Ver [Configuração](configuracao.md).)

### 1.3 Primeira sincronização

Na app: menu (⋮) → **Configuração** → carrega no botão **Sincronizar**. O
resultado aparece nesse mesmo ecrã:

- **Sincronizado** — o servidor aceitou os dados.
- Outra mensagem? → consulta a [tabela de resolução de problemas](#3-resolucao-de-problemas).

## 2. Uso diário

A app trabalha **offline-first**: podes criar e editar clientes, tirar e remover
fotos, e marcar a localização no mapa mesmo sem servidor. A sincronização é
acionada no ecrã **Configuração** (menu ⋮ → **Configuração**), automaticamente
**sempre que guardas um cliente** e **ao abrir a app** (uma vez por arranque,
quando há servidor configurado) — o envio segue em segundo
plano, sem bloquear o ecrã. O estado aparece no ecrã
**Configuração** e na lista — ícone + mensagem curta.

Se o relógio do dispositivo estiver mais de 2 minutos diferente do servidor,
o ecrã **Configuração** mostra um aviso a dizer quanto está atrasado ou
adiantado, com o botão **Corrigir**. As alterações feitas com o relógio errado
podem não chegar aos outros dispositivos, por isso vale a pena corrigir antes
de continuar a trabalhar.

Fluxos principais:

- **Lista de clientes** — o ecrã inicial; pesquisa e ordenação. O contrato
  aparece por baixo do nome e os clientes inativos têm a etiqueta **Inativo** e
  o texto atenuado. O ícone de controlos dentro da caixa de pesquisa filtra por
  estado (**Todos os clientes**, **Só clientes ativos** ou **Só clientes
  inativos**) e define a ordem: **Ordem alfabética** (predefinição), **Mais
  recentes primeiro** (pelo cliente criado mais recentemente) ou **Última edição
  primeiro** (pelo cliente alterado mais recentemente). Com um filtro ou uma ordem
  diferentes da predefinição o ícone fica destacado.
- **Novo/editar cliente** — formulário com nome, contrato, coordenadas, notas e
  o interruptor **Cliente ativo**.
- **Mapa de clientes** — o ícone de mapa na barra superior da lista abre o mapa
  dos clientes com localização. O menu (≡) permite mostrar **Todos os clientes**,
  **Apenas ativos** (predefinição) ou **Apenas inativos**; a pesquisa e o filtro
  da lista não se aplicam ao mapa. Os pins vermelhos são clientes ativos e os
  cinzas inativos; tocar num pin mostra o nome, o contrato e o estado, com o
  botão **Abrir cliente**. Os mapas precisam de rede ou de tiles já em cache
  para mostrar o fundo; os pins continuam visíveis.
- **Mapa** — escolher/indicar a localização (Picker).
- **Fotos** — capturar e associar imagens; ver em ecrã inteiro.
- **Detalhe do cliente** — mapa, navegação, notas e fotos; um cliente inativo
  mostra a etiqueta **Inativo** na barra superior, depois do contrato; no menu (≡) podes
  editar, apagar ou abrir o **Log**, uma página com o histórico de alterações
  agrupado por dia: quem alterou, a que horas, que campos mudaram (valor antigo
  e novo), quando o cliente foi ativado ou desativado, e que fotos foram
  adicionadas, removidas ou reordenadas. As
  miniaturas das fotos adicionadas aparecem quando ainda existem neste
  dispositivo.
- **Configuração** — o ecrã único de sincronização e servidor: estado com etiqueta, última sincronização, resumo do que falta sincronizar, totais de clientes na app e no servidor e o botão **Sincronizar** (envia e recebe os dados); o cartão **Sincronização** inclui também o URL, a chave mascarada e o utilizador associado. Sem credencial, mostra **Importar credencial do utilizador** como ação principal; depois de configurado, mostra **Remover credencial**.
- **Sobre** — cabeçalho com o ícone, o nome e a versão da app, o cartão
  **Atualizações** e a **Comunidade**, com o QR code do grupo de Telegram e o
  botão **Abrir no Telegram**.

### Atualizar a app

No ecrã **Sobre**, o botão **Procurar atualizações** pergunta ao servidor se há
uma versão mais recente. Se houver, **Transferir** descarrega o pacote com
progresso e **Instalar** abre o instalador do Android, onde confirmas a
instalação. O ficheiro é verificado (tamanho e SHA-256) antes de ser oferecido;
se não houver nenhuma versão publicada, a app di-lo e não há nada a instalar.
As versões são publicadas pelo administrador do servidor (ver
`elisdb-server/docs/execucao.md`).

Durante uma sincronização, as fotos que já existem no servidor **sem mudanças**
não são reescritas, e um ficheiro novo só é gravado antes da sua linha na base
de dados — se a sincronização falhar a meio, as fotos anteriores ficam intactas.

Duas garantias que evitam surpresas:

- Uma alteração feita **enquanto** a sincronização está a decorrer não é
  substituída: fica pendente e é enviada na sincronização seguinte.
- Um cliente eliminado noutro telemóvel desaparece também aqui, mesmo que o
  relógio desse telemóvel estivesse atrasado.

> **Registo duplicado:** se criares um cliente com um nome que já exista no
> servidor (comparação por nome normalizado), o servidor responde `duplicate` e
> a app **funde** o registo local com o existente: o teu registo é reatribuído
> ao id canónico, mantendo o que introduziste, e re-enviado na sincronização
> seguinte (vence pelo mais recente). No ecrã **Configuração** aparece: *"Registo
> «Nome» foi fundido com um registo existente."*. O teu registo nunca é apagado
> em silêncio.

## 3. Resolução de problemas

| O que aparece na barra de estado   | O que está a acontecer                | O que fazer |
| ---------------------------------- | ------------------------------------- | ----------- |
| "Não configurado" | Falta o URL ou a chave (no ecrã Configuração ou no build) | Importa o `<nome>.config.json` (ou emite um novo no servidor) |
| "Servidor rejeitou a sincronização (chave não registada)." | A chave pública não está registada no servidor | No servidor: `docker compose exec elisdb-server python -m app.keys issue <nome> --server-url <url>` e importa o ficheiro (ver passo 1) |
| "Relógio do dispositivo dessincronizado. Verifique a hora do dispositivo." | O relógio do telemóvel está fora da janela de ±5 min do servidor | Ativa a hora automática no telemóvel (e no servidor) e sincroniza de novo |
| "Falha na sincronização. Verifique a ligação." | O servidor não está acessível no URL definido | Confirma a rede e o URL (usa o IP do servidor, não `127.0.0.1`) |

## Referência cruzada

- Configuração de build (versão, assinatura, variáveis): [Configuração](configuracao.md).
- Servidor: instalação, execução, emissão de chaves e API em `../elisdb-server/docs/`.