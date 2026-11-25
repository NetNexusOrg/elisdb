# Usar a app

Para ligar o telemóvel ao servidor pela primeira vez, começa em
[ligar um telemóvel](ligar-telemovel.md). Este guia assume que a app já está
configurada.

A app é **offline-first**: depois de configurada, tudo funciona sem rede. A
sincronização é um extra.

## Lista de clientes

É o ecrã inicial. No topo aparece uma saudação com o nome do utilizador. Cada
cartão mostra o nome, o contrato por baixo e as tags à direita (em maiúsculas);
quando não cabem, as tags deslizam horizontalmente. Se o cliente tiver fotos, o
cartão mostra a foto de perfil; sem fotos, um avatar genérico. Clientes inativos
têm a etiqueta **Inativo** e texto atenuado.

- **Pesquisa** — filtra enquanto escreves. Com tags escolhidas, a pesquisa
  limita-se a esses clientes.
- **Controlos** (ícone dentro da caixa de pesquisa) — estado (**Todos os
  clientes**, **Só clientes ativos**, **Só clientes inativos**), **tags** (podes
  escolher várias) e ordem: **alfabética** (predefinição), **tags (A–Z)**,
  **mais recentes** ou **última edição**. Os filtros e a ordem ficam guardados
  entre visitas; com um filtro ativo, o ícone fica destacado.
- **Menu (≡)** — **Novo**, **Logs**, **Configuração** e **Sobre**. O ícone de
  mapa na barra superior abre o mapa.

## Criar e editar

Formulário com nome, contrato, notas, tags, coordenadas e o interruptor
**Cliente ativo**. O **Nome** é obrigatório.

- **Tags:** toca em **Adicionar tag**, escreve e confirma. Cada tag é um chip
  removível. Máximo 10, guardadas em maiúsculas e por ordem alfabética.
- **Localização:** escolhe no mapa ou usa a posição atual (ver abaixo).
- **Aparelhos e tratamentos:** escolhe a categoria (*Roedores*, *Insetos
  Rastejantes*, *Insetos Voadores* ou *Tratamentos*) e o modelo. Nos aparelhos
  indica a quantidade instalada (1–9999); os tratamentos não levam quantidade.
  Cada linha pode ser removida, mas o mesmo modelo não pode entrar duas vezes.
- **Fotos:** **Foto** abre a câmara e **Galeria** escolhe imagens. Cada foto tem
  um **×** no canto superior direito para a remover e uma **estrela** no canto
  superior esquerdo: a estrela preenchida marca a foto de perfil; toca na
  estrela de outra foto para a escolher. Ao remover a foto de perfil, a primeira
  foto restante passa automaticamente a ser a de perfil.

## Mapa

O ícone de mapa na barra superior abre o mapa dos clientes com localização.

- Ao abrir, pede permissão e centra na tua posição (ponto azul). Sem permissão,
  enquadra os pins.
- Pins vermelhos são clientes ativos; cinzas, inativos. Cada pin mostra o nome.
- Tocar num pin mostra nome, contrato e estado, com **Abrir cliente**.
- O menu (≡) filtra por **Estado** (predefinição: apenas ativos) e **Tags**. A
  pesquisa e o filtro da lista não se aplicam ao mapa.
- Os mapas usam tiles vetoriais do OpenStreetMap servidos pelo OpenFreeMap;
  precisam de rede ou cache. Sem fundo, os pins continuam visíveis.

## Detalhe do cliente

Nome e tags, seguidos da foto de perfil à esquerda e do mapa à direita,
navegação, aparelhos/tratamentos, notas e fotos.

- A foto de perfil aparece à esquerda e o mapa à direita. Sem foto aparece um
  ícone de pessoa; sem localização aparece apenas um ícone de mapa, sem botão de
  navegação.
- **Aparelhos e Tratamentos** aparece depois da localização e antes das notas, por
  categoria e por ordem alfabética. Cada grupo mostra o total (soma das
  quantidades nos aparelhos, número de modelos nos tratamentos) e cada linha o
  nome e a quantidade. Modelos inativos continuam visíveis e contam.
- O mapa em ecrã inteiro centra-se no cliente e mostra também a tua posição.
- O menu (≡) permite **Editar**, **Log** e **Apagar**, por esta ordem.
- O **Log** mostra o histórico agrupado por dia: quem alterou, a que horas, que
  campos mudaram (valor antigo e novo), ativações/desativações e fotos
  adicionadas, removidas ou reordenadas. Carrega por páginas; **Carregar mais**
  continua. Tal como no **Logs** global, só aparecem alterações confirmadas pelo
  servidor.

## Logs (alterações)

No menu da lista, **Logs** abre a página **Alterações**, com o histórico global
de todas as alterações aceites pelo servidor.

- As entradas aparecem por ordem cronológica inversa (mais recentes primeiro) e
  agrupadas por dia.
- Cada entrada mostra o cliente, o autor, a ação (criou, editou, apagou), a hora
  e os detalhes: campos alterados, tags, fotos e aparelhos/tratamentos
  adicionados, removidos ou com quantidade alterada. Quando disponíveis, as
  fotos adicionadas aparecem em miniatura.
- Alterações ao catálogo aparecem com o nome do modelo e a ação (criou, editou,
  desativou, reativou) e não são clicáveis.
- Toca no nome de um cliente existente para abrir a ficha. Clientes apagados
  aparecem com o nome guardado no momento da eliminação e não são clicáveis.
- **Carregar mais** continua para as entradas anteriores; no fim deixa de
  aparecer.
- Só aparecem alterações confirmadas pelo servidor. Edições feitas offline
  surgem depois de sincronizar.
- Precisa de um servidor atualizado com `GET /api/history`. Num servidor antigo
  aparece **Não foi possível carregar as alterações.** com **Tentar novamente**.

## Catálogo

**Catálogo** no menu da lista abre os modelos que podem ser instalados nos
clientes.

- A lista mostra as quatro categorias sempre abertas e os modelos por ordem
  alfabética. Os inativos ficam visíveis com a etiqueta **Inativo**, mas deixam
  de ser oferecidos em novos clientes.
- **+** cria um modelo com nome e categoria. O nome é guardado e apresentado em
  maiúsculas. A categoria não pode ser alterada depois; nomes iguais na mesma
  categoria (ignorando acentos, espaços e maiúsculas) são recusados.
- **Guardar** envia o catálogo e sincroniza com o servidor; as alterações ficam
  também guardadas localmente de imediato. Se restarem modelos por sincronizar
  (por exemplo, rede em baixo), a app avisa.
- No menu (⋮) de cada modelo podes **Editar**, **Desativar** ou **Reativar**.
  Não há eliminação definitiva.
- Alterações ao catálogo sincronizam como os clientes e aparecem no **Logs**
  global com o nome do modelo.

## Configuração

O ecrã de sincronização: estado, última sincronização, o que falta enviar,
totais de clientes na app e no servidor, e o botão **Sincronizar**.

- Mostra também o URL, a chave mascarada e o utilizador associado (só leitura).
- Sem credencial, a ação principal é **Importar credencial do utilizador**.
  Depois de configurado, **Remover credencial**.
- Se houver clientes **apenas locais**, aparece **Substituir dados locais pelos
  do servidor** (com snapshot antes de substituir).

## Sincronização

Acontece sozinha: **ao abrir a app**, **ao guardar um cliente** e **ao apagar um
cliente**. O envio corre em segundo plano, sem bloquear o ecrã.

- Falha de rede → nova tentativa com atraso exponencial a partir de 30 s.
- Erros de chave, relógio ou configuração não são repetidos automaticamente.
- **Sincronizar** tenta já e ignora o atraso.
- Se o relógio estiver mais de 2 min fora do servidor, aparece um aviso com
  **Corrigir**.

O detalhe técnico (lotes, idempotência, conflitos) está em
[sincronização](../referencia/sincronizacao.md).

## Atualizar a app

Em **Sobre** aparece a versão instalada; no cartão **Atualizações**, toca em
**Procurar atualizações**:

1. Se houver versão nova, **Transferir** descarrega com progresso.
2. **Instalar** abre o instalador do Android.
3. O ficheiro é verificado (tamanho e SHA-256) antes de ser oferecido.

Ao abrir a app com servidor configurado, a verificação também corre em segundo
plano; se houver versão nova aparece um aviso com **Atualizar** e **Adiar**
(adia até ao próximo arranque).

O **Sobre** tem ainda a **Comunidade**, com o QR code do grupo de Telegram.

## Dados locais

- Os dados vivem numa base Room no armazenamento privado da app.
- Um *full pull* nunca apaga clientes que só existem no telemóvel.
- Se o servidor mudar de identidade, a app avisa em vez de reconciliar sozinha.
- Snapshots antes de substituições ficam em `files/snapshots/` (os três mais
  recentes). Para restaurar: fecha a app, substitui `elisdb.db` e `photos/`
  pelos da cópia e volta a abrir.
