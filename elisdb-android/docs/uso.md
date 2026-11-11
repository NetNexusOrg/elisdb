# Utilização da app — ligar ao servidor e usar no dia a dia

Este é o guia de uso: como ligar a app ao servidor na primeira vez e como usá-la
no dia a dia. Assumimos que o servidor está instalado e a correr
(`../elisdb-server/docs/`) e que a app está no telemóvel
([Execução](execucao.md)).

## 1. Ligar a app ao servidor (primeira vez)

A app só avança depois de ter um **utilizador configurado por completo**: URL do
servidor, chave de assinatura e nome do utilizador, todos vindos do ficheiro
`<utilizador>.config.json`. Enquanto faltar algum destes dados, o arranque mostra
**Acesso Negado** com o botão **Abrir configuração**; a lista de
clientes só fica disponível depois de importares a credencial.

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
o `maria.config.json`. O **URL do servidor**, a **chave de assinatura** e o
**nome do utilizador** ficam preenchidos em simultâneo e a sincronização arranca
logo.

Se importares uma credencial antiga sem `user_name`, a app tenta obter o nome ao
servidor; sem rede, o ecrã **Acesso Negado** pede para reimportares o
ficheiro.

(O cartão **Sincronização**, no ecrã **Configuração**, mostra o URL, a chave mascarada e
o utilizador associado, em modo só de leitura; a credencial entra sempre pela
importação do ficheiro. Ver [Configuração](configuracao.md).)

O URL é validado **antes** de ser guardado. `https://` é aceite para qualquer
host; `http://` só é aceite para `localhost`, endereços de loopback, redes
privadas (`10/8`, `172.16/12`, `192.168/16`), link-local (`169.254/16`),
CGNAT (`100.64/10`) e IPv6 privado/link-local. Um URL público em HTTP ou um
esquema diferente é rejeitado com uma mensagem e a configuração anterior fica
intacta — não há limpeza de watermark, hashes nem sincronização.

### 1.3 Primeira sincronização

Na app: menu (⋮) → **Configuração** → carrega no botão **Sincronizar**. O
resultado aparece nesse mesmo ecrã:

- **Sincronizado** — o servidor aceitou os dados.
- Outra mensagem? → consulta a [tabela de resolução de problemas](#3-resolucao-de-problemas).

## 2. Uso diário

A app trabalha **offline-first**: podes criar e editar clientes, tirar e remover
fotos, e marcar a localização no mapa mesmo sem servidor. A sincronização é
acionada no ecrã **Configuração** (menu ⋮ → **Configuração**), automaticamente
**sempre que guardas um cliente**, **quando apagas um cliente** e **ao abrir a
app** (uma vez por arranque, quando há servidor configurado) — o envio segue em
segundo plano, sem bloquear o ecrã. O estado aparece no ecrã
**Configuração** e na lista — ícone + mensagem curta.

Ao abrir a app com a configuração completa, a lista de clientes fica disponível
de imediato.

A app sincroniza no primeiro arranque, ao guardar um cliente novo, ao guardar
uma alteração e quando carregas em **Sincronizar**. Se a rede falhar, o sistema
faz uma nova tentativa em segundo plano com atraso exponencial (a partir de 30
segundos); erros de autenticação, relógio ou configuração não são repetidos
automaticamente. O botão **Sincronizar** tenta imediatamente e ignora o atraso.

Se o relógio do dispositivo estiver mais de 2 minutos diferente do servidor,
o ecrã **Configuração** mostra um aviso a dizer quanto está atrasado ou
adiantado, com o botão **Corrigir**. As alterações feitas com o relógio errado
podem não chegar aos outros dispositivos, por isso vale a pena corrigir antes
de continuar a trabalhar.

Fluxos principais:

- **Lista de clientes** — o ecrã inicial; pesquisa e ordenação. O contrato
  aparece por baixo do nome e as tags à direita do cartão, na mesma linha do
  contrato, passando para as linhas seguintes quando não cabem (sempre em
  maiúsculas). Os clientes inativos têm a etiqueta **Inativo** e
  o texto atenuado. O ícone de controlos dentro da caixa de pesquisa filtra por
  estado (**Todos os clientes**, **Só clientes ativos** ou **Só clientes
  inativos**), a lista de tags existentes (para mostrar apenas os clientes que
  tenham a tag escolhida) e define a ordem: **Ordem alfabética** (predefinição),
  **Mais recentes primeiro** (pelo cliente criado mais recentemente) ou **Última
  edição primeiro** (pelo cliente alterado mais recentemente). Com um filtro ou
  uma ordem diferentes da predefinição o ícone fica destacado. Os grupos
  **Tags**, **Ordenação** e **Estado** aparecem recolhidos e mostram as opções ao
  tocar no respetivo título.
- **Novo/editar cliente** — formulário com contrato, nome, notas, tags
  (toca em **Adicionar tag** para abrir o campo, escreve a tag e confirma; cada
  tag aparece como um chip que pode ser removido), coordenadas e o interruptor
  **Cliente ativo**. Um cliente pode ter até 10 tags; são guardadas em maiúsculas
  e por ordem alfabética.
- **Mapa de clientes** — o ícone de mapa na barra superior da lista abre o mapa
  dos clientes com localização. Ao abrir, pede acesso à localização e centra o
  mapa na posição atual, com um ponto azul; sem permissão ou localização
  disponível, enquadra os pins como antes. O menu (≡) agrupa **Tags** e
  **Estado** em secções expansíveis: em **Estado** podes mostrar **Todos os
  clientes**, **Apenas ativos** (predefinição) ou **Apenas inativos**; em
  **Tags** escolhes uma tag ou **Todas as tags**. A pesquisa e o filtro da lista
  não se aplicam ao mapa. Os pins vermelhos são clientes
  ativos e os cinzas inativos; cada pin mostra o nome do cliente por cima. Tocar
  num pin mostra o nome, o contrato e o estado, com o botão **Abrir cliente**.
  Os mapas usam tiles vetoriais do OpenStreetMap servidos pelo OpenFreeMap e
  precisam de rede ou de tiles já em cache para mostrar o fundo; os pins
  continuam visíveis.
- **Mapa** — escolher/indicar a localização (Picker).
- **Fotos** — capturar e associar imagens; ver em ecrã inteiro.
- **Detalhe do cliente** — nome com todas as tags por baixo, mapa, navegação, notas e fotos. O mapa em ecrã
  inteiro centra-se no cliente e mostra também a tua localização atual (ponto
  azul), se houver permissão; afasta o zoom para te situares. Um cliente inativo
  mostra a etiqueta **Inativo** na barra superior, depois do contrato; no menu (≡) podes
  editar, apagar ou abrir o **Log**, uma página com o histórico de alterações
  agrupado por dia: quem alterou, a que horas, que campos mudaram (valor antigo
  e novo), quando o cliente foi ativado ou desativado, e que fotos foram
  adicionadas, removidas ou reordenadas. O histórico carrega por páginas e o
  botão **Carregar mais** acrescenta as entradas seguintes. As
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

Ao abrir a app, e quando há servidor configurado e rede, esta verifica em
segundo plano se existe uma versão mais recente. Se existir, aparece um aviso
com a versão nova e as opções **Atualizar** e **Adiar**: **Atualizar** abre o
ecrã **Sobre** (onde transferes e instalas o pacote) e **Adiar** fecha o aviso
até ao próximo arranque. Sem versão nova, sem release publicada, sem
configuração ou sem rede, o aviso não aparece.

Durante uma sincronização, as fotos que já existem no servidor **sem mudanças**
não são reescritas, e um ficheiro novo só é gravado antes da sua linha na base
de dados — se a sincronização falhar a meio, as fotos anteriores ficam intactas.
Ao descarregar uma foto, a app confirma os *magic bytes* contra o MIME declarado
(só JPEG, PNG e WebP). Se não corresponder, mantém a cópia anterior e volta a
tentar em vez de guardar conteúdo inválido. As fotos capturadas ou escolhidas
aplicam as oito orientações EXIF, incluindo as variantes espelhadas
(`TRANSPOSE`/`TRANSVERSE`), antes de serem convertidas para WebP.

Os registos pendentes são enviados em lotes (até 100, ou o limite que o servidor
anunciar em `/api/latest`). Uma falha num lote não bloqueia os restantes: os
registos desse lote ficam pendentes e são tentados novamente. As fotos só são
enviadas depois de o servidor aceitar os metadados e indicar quais faltam, pelo
que um lote rejeitado não deixa ficheiros órfãos. Cada lote leva uma chave de
idempotência estável: um reenvio após timeout devolve a resposta anterior sem
duplicar registos nem auditoria; se o conteúdo do lote mudar, a chave muda. Se
um cliente tiver mais de 500 fotos, a sincronização desse cliente é adiada com
uma mensagem — não fica num ciclo de erro 422.

Duas garantias que evitam surpresas:

- Uma alteração feita **enquanto** a sincronização está a decorrer não é
  substituída: fica pendente e é enviada na sincronização seguinte.
- Um cliente eliminado noutro telemóvel desaparece também aqui, mesmo que o
  relógio desse telemóvel estivesse atrasado. Se a cópia local tiver uma edição
  **mais recente** do que a eliminação, é preservada e reenviada; o servidor
  aceita-a como re-criação.
- Uma foto escolhida ou capturada no formulário fica registada como **staged**
  até ser guardada ou descartada. O varrimento de órfãos ignora esses ficheiros
  e só remove ficheiros sem linha na base de dados com mais de uma hora, pelo
  que não apaga uma foto que ainda estás a preparar.
- Ao limpar ou importar uma nova configuração, a sincronização em curso é
  cancelada e aguardada antes de o watermark, hashes de fotos e relógio serem
  limpos. O sync antigo não pode voltar a publicar estado nem escrever um
  watermark do servidor anterior.
- O cliente HTTP é partilhado e contado por referências: uma troca de
  configuração só fecha o cliente antigo depois de terminar o último pedido que
  o estava a usar; pedidos novos recebem sempre o cliente da configuração atual.

### Dados apenas locais e servidor substituído

Um full pull **nunca apaga** clientes locais que o servidor não tenha. Esses
registos ficam marcados como **apenas locais** e o ecrã **Configuração** mostra
quantos são, com a opção **Substituir dados locais pelos do servidor**.

O servidor tem uma identidade estável (`instance_id`). Se ela mudar — servidor
novo, volume recriado ou backup restaurado — a app avisa e preserva os dados
locais; não faz reconciliação destrutiva sozinha. A substituição só acontece
depois de confirmares, e antes disso é criada uma cópia local recuperável da
base de dados (`VACUUM INTO`) e das fotos em `files/snapshots/` (mantêm-se as
três mais recentes).

### Recuperar uma cópia local

As cópias ficam em `files/snapshots/<timestamp>/` dentro do armazenamento
privado da app, com `elisdb.db` (e, quando existirem, `elisdb.db-wal` e
`elisdb.db-shm`) e a pasta `photos/`. Para restaurar, fecha a app, substitui a
base de dados e a pasta `photos/` pelas da cópia (por exemplo com `adb
backup`/`adb pull` num dispositivo de testes) e volta a abrir. A app mantém
apenas as três cópias mais recentes; as mais antigas são removidas na criação de
uma nova.

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
| "Acesso Negado" no arranque | Falta o URL, a chave ou o nome do utilizador | Importa o `<nome>.config.json` na Configuração |
| "Não configurado" | Falta o URL ou a chave (no ecrã Configuração ou no build) | Importa o `<nome>.config.json` (ou emite um novo no servidor) |
| "Servidor rejeitou a sincronização (chave não registada)." | A chave pública não está registada no servidor | No servidor: `docker compose exec elisdb-server python -m app.keys issue <nome> --server-url <url>` e importa o ficheiro (ver passo 1) |
| "Relógio do dispositivo dessincronizado. Verifique a hora do dispositivo." | O relógio do telemóvel está fora da janela de ±5 min do servidor | Ativa a hora automática no telemóvel (e no servidor) e sincroniza de novo |
| "Falha na sincronização. Verifique a ligação." | O servidor não está acessível no URL definido | Confirma a rede e o URL (usa o IP do servidor, não `127.0.0.1`) |

## Referência cruzada

- Configuração de build (versão, assinatura, variáveis): [Configuração](configuracao.md).
- Servidor: instalação, execução, emissão de chaves e API em `../elisdb-server/docs/`.