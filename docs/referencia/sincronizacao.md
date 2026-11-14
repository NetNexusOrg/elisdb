# Como funciona a sincronização

O que a app e o servidor fazem por baixo. Para o dia a dia, ver
[usar a app](../usar/app.md); para os endpoints, [API](api.md).

## Dois relógios

Cada registo tem dois tempos, com papéis diferentes:

- **`updated_at`** (cliente) e **`deleted_at`** vêm do telemóvel e **só decidem
  conflitos** (*last-write-wins*). O servidor guarda-os como os recebe, mesmo que
  o relógio esteja atrasado.
- **`delivery_ts`** é atribuído pelo servidor (`max(timestamp do dispositivo,
  watermark + 1)`, sob o *write lock*) e é o **cursor do pull**. `GET /api/pull`
  filtra, ordena e pagina por ele; cada registo e tombstone devolve o seu.

Consequências úteis ao diagnosticar:

- Uma eliminação feita com o relógio atrasado chega na mesma: é entregue pelo
  `delivery_ts`, que é sempre maior do que tudo o que já saiu.
- Um push com `updated_at` antigo **não é rejeitado**: mantém o seu `updated_at`
  e recebe um `delivery_ts` novo.
- Timestamps negativos ou demasiado no futuro (> 1 h) são recusados com `400`.
- O `ts` da auditoria é o `updated_at` do dispositivo (momento da edição), não a
  hora de chegada — o histórico mantém a ordem real.

## Conflitos e duplicados

- **Duplicado:** o nome é normalizado (trim, espaços colapsados, maiúsculas).
  Criar dois clientes com o mesmo nome é recusado; o servidor devolve
  `duplicate` com o registo canónico e a app **funde** o local nesse id,
  mantendo o que o utilizador escreveu e reenviando-o.
- **Conflito:** vence o `updated_at` mais recente. `409` quando uma eliminação
  não é mais recente do que o registo guardado — nada é apagado.
- **Eliminação:** é um tombstone. O cliente compara-o com o `updated_at` local:
  se a cópia local for mais recente, preserva-a e reenvia-a (o servidor aceita
  como recriação).
- **`ativo`** faz parte do registo e participa no LWW como os outros campos.

## Lotes no envio

- Registos pendentes são enviados em lotes de até 100 (ou o `max_push_records`
  anunciado em `/api/latest`).
- Uma falha num lote não bloqueia os restantes: esses registos ficam pendentes
  para a tentativa seguinte.
- Cada lote leva uma `Idempotency-Key` estável: um reenvio após *timeout*
  devolve a resposta anterior sem duplicar registos nem auditoria. Se o conteúdo
  mudar, a chave muda.
- As fotos só sobem **depois** de o servidor aceitar os metadados e indicar
  quais faltam — um lote rejeitado não deixa ficheiros órfãos.
- Mais de 500 fotos num cliente adia esse cliente com mensagem, em vez de entrar
  num ciclo de `422`.

## Fotos

- Guardadas como ficheiros no servidor; a base de dados guarda o caminho e o
  SHA-256 (calculado na primeira leitura e reutilizado).
- O cliente usa o hash como identidade de conteúdo: só transfere o que mudou.
- Ao descarregar, valida os *magic bytes* contra o MIME declarado (JPEG, PNG,
  WebP); se não corresponder, mantém a cópia anterior.
- As fotos capturadas aplicam as oito orientações EXIF antes de converter para
  WebP.
- Uma foto escolhida no formulário fica *staged* até ser guardada; o varrimento
  de órfãos ignora-a.

## Rede e retentativas

- Falha de rede: nova tentativa em segundo plano com atraso exponencial a partir
  de 30 s. Erros de autenticação, relógio ou configuração **não** são repetidos.
- O botão **Sincronizar** tenta imediatamente e ignora o atraso.
- Se o relógio do dispositivo estiver mais de 2 min fora do servidor, a app
  avisa e oferece **Corrigir**.

## Servidor substituído e dados locais

- Um *full pull* **nunca** apaga clientes locais que o servidor não tenha;
  ficam marcados como **apenas locais**.
- O `instance_id` identifica a base de dados. Se mudar (servidor novo, BD
  recriada, backup restaurado), a app avisa e preserva os dados locais; a
  substituição só acontece depois de confirmação.
- Antes de substituir, cria um snapshot local recuperável (`VACUUM INTO` +
  fotos em `files/snapshots/`, mantendo os três mais recentes).

## Troca de configuração

Ao limpar ou importar uma credencial nova, a sincronização em curso é cancelada
e aguardada antes de limpar watermark, hashes de fotos e relógio — o sync antigo
não pode publicar estado nem escrever um watermark do servidor anterior.
