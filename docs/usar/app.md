# Usar a app

Para ligar o telemóvel ao servidor pela primeira vez, começa em
[ligar um telemóvel](ligar-telemovel.md). Este guia assume que a app já está
configurada.

A app é **offline-first**: tudo funciona sem rede. A sincronização é um extra.

## Lista de clientes

É o ecrã inicial. Cada cartão mostra o nome, o contrato por baixo e as tags à
direita (em maiúsculas, passando para a linha seguinte quando não cabem).
Clientes inativos têm a etiqueta **Inativo** e texto atenuado.

- **Pesquisa** — filtra enquanto escreves.
- **Controlos** (ícone dentro da caixa de pesquisa) — estado (**Todos**, **Só
  ativos**, **Só inativos**), tag e ordem: **alfabética** (predefinição), **mais
  recentes** ou **última edição**. Com um filtro ativo, o ícone fica destacado.

## Criar e editar

Formulário com nome, contrato, notas, tags, coordenadas e o interruptor
**Cliente ativo**.

- **Tags:** toca em **Adicionar tag**, escreve e confirma. Cada tag é um chip
  removível. Máximo 10, guardadas em maiúsculas e por ordem alfabética.
- **Localização:** escolhe no mapa ou usa a posição atual (ver abaixo).
- **Fotos:** captura ou escolhe imagens; vê-as em ecrã inteiro.

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

Nome com as tags, mapa, navegação, notas e fotos.

- O mapa em ecrã inteiro centra-se no cliente e mostra também a tua posição.
- O menu (≡) permite **editar**, **apagar** e abrir o **Log**.
- O **Log** mostra o histórico agrupado por dia: quem alterou, a que horas, que
  campos mudaram (valor antigo e novo), ativações/desativações e fotos
  adicionadas, removidas ou reordenadas. Carrega por páginas; **Carregar mais**
  continua.

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

Em **Sobre** → **Procurar atualizações**:

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
