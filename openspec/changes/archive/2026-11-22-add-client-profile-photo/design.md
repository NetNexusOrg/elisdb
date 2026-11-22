# Design

## Context

Ver `proposal.md` — Why. Factos que moldam o desenho:

- A lista de clientes usa `it.fotos.firstOrNull()?.uriLocal`
  (`ClientListViewModel`), e `ClienteRepositoryImpl.withOrderedPhotos()`
  ordena por `ordem, id`.
- `ClienteRepositoryImpl.save()` reescreve `ordem = index` para todas as fotos
  do draft. A ordem da grelha é, portanto, a ordem do draft.
- `PhotoMaterializer` compara `mime`, `ordem` e o hash de conteúdo; se algo
  diferir, re-downloada os bytes. Uma mudança só de metadados não deve passar
  por esse caminho.
- `FotoInDto`/`FotoOutDto` (`remote/RemoteDto.kt`) são o contrato do sync;
  `SyncMapper` e `SyncCoordinator` constroem `Foto` em dois pontos (pull e
  merge de duplicados) e a chave de idempotência inclui os metadados.
- No servidor, `fotos` tem `ordem` mas não tem noção de perfil; o pull e
  `_canonical_from_conn` devolvem os metadados de cada foto.
- Room está na versão 12 (`AppDatabase`); o servidor aplica migrações
  idempotentes no arranque.

## Goals / Non-Goals

**Goals:**

- Escolha explícita da foto de perfil, independente da ordem da galeria.
- Flag transportada no sync e persistida nos dois lados, sem re-download de
  bytes.
- Compatibilidade com clientes e servidores anteriores à feature.

**Non-Goals:**

- Reordenar a galeria ou mover a foto escolhida para a primeira posição.
- Crop, rotação ou edição da imagem de perfil.
- Mostrar a estrela no ecrã de detalhe ou no visualizador (o comportamento
  atual da lista mantém-se).

## Decisions

### Flag `preferida` por foto, em vez de reordenar `ordem` ou guardar no cliente

Alternativas:

- **Reordenar `ordem`** (foto escolhida para 0): não altera o protocolo, mas
  muda a ordem visível da galeria e faz o `PhotoMaterializer` re-downloadar a
  foto (compara `ordem`).
- **`fotoPerfilId` no cliente**: obriga a alterar a tabela `clientes`, DTOs,
  auditoria e reconciliação de duplicados; a referência pode apontar para uma
  foto inexistente.

Escolhida a flag booleana na linha da foto: preserva a ordem, isola o
metadado, viaja naturalmente com o registo de fotos e não interfere com a
identidade por conteúdo.

### Invariante: no máximo uma foto preferida

- Na app, `ClienteRepositoryImpl.save()` calcula o índice preferido (a primeira
  foto marcada; se nenhuma, a primeira do draft) e grava `preferida` por índice.
  Isto normaliza também clientes antigos no primeiro save.
- No servidor, após o merge das fotos, uma normalização deixa no máximo uma
  linha com `preferida = 1` (a primeira por `ordem, id`), evitando estados
  inválidos vindos de pushes concorrentes.
- Na leitura, a lista usa `firstOrNull { it.preferida } ?: firstOrNull()`, pelo
  que fotos antigas continuam a mostrar a primeira sem migração de dados.

### Tri-estado no protocolo (`null` = não mencionado)

`FotoIn.preferida` é `bool | None` e `FotoOutDto.preferida` é `Boolean?`:

- Cliente novo → servidor antigo: o campo é ignorado pelo Pydantic
  (`extra=ignore`), sem erro.
- Cliente antigo → servidor novo: chega `None`; o upsert usa
  `COALESCE(excluded.preferida, fotos.preferida, 0)`, preservando a escolha.
- Servidor antigo → cliente novo: o pull omite o campo (`null`); o cliente só
  atualiza a flag quando o valor vem explícito, para não apagar uma escolha
  local.
- `false` explícito limpa a flag; uma foto nova sem flag fica `false`.

### Atualização só de metadados no pull

Quando o conteúdo é igual (`MaterializeResult.Keep`), o `SyncCoordinator`
compara a flag recebida com a local e faz `fotoDao.update(existing.copy(...))`
dentro da transação, sem chamar o download nem o `PhotoStore`. O
`PhotoMaterializer` continua a decidir apenas por conteúdo (hash/MIME).

### UI: estrela em todas as fotos

`PhotoGrid` ganha `onSetProfile`: um `IconButton` no `TopStart` de cada célula,
espelhando o "×" no `TopEnd`. Preenchida (`Star`) e com cor de destaque na foto
preferida; contorno (`StarBorder`) nas restantes. `contentDescription`
distingue "Foto de perfil" de "Definir como foto de perfil" e há `testTag` para
os testes.

### LWW e idempotência

A escolha é feita no formulário e aplicada pelo `save()` normal, que incrementa
`revision` e atualiza `updatedAt`; o push seguinte leva o registo completo e o
LWW do servidor decide entre dispositivos. A chave de idempotência do push
passa a incluir `preferida`, tal como já inclui `ordem`/`mime`.

## Risks / Trade-offs

- [Cliente antigo a fazer push apaga a escolha] → `None` preserva no servidor e
  `null` não é aplicado no pull.
- [Duas escolhas concorrentes] → LWW do registo do cliente; o servidor
  normaliza para uma só flag.
- [Migração Room falha em instalações antigas] → migração aditiva de 12→13,
  coberta por teste de migração; fallback de leitura funciona mesmo sem flag.
- [Foto preferida removida noutro dispositivo] → o pull aplica a lista completa
  e a app reassinala na edição; sem fotos, cai no avatar genérico.
- [Metadados no histórico de auditoria] → a auditoria continua a comparar ids de
  fotos; mudar só a preferência não gera entrada, evitando ruído.

## Migration Plan

1. Servidor primeiro: a coluna é aditiva e `FotoIn.preferida` é opcional, pelo
   que clientes antigos continuam a funcionar.
2. App depois: a migração Room adiciona a coluna com `DEFAULT 0`; sem flag, a
   lista usa a primeira foto.
3. Rollback: uma app anterior ignora o campo (Kotlinx) e um servidor anterior
   ignora o campo (Pydantic); nenhum dado de fotos é perdido.
