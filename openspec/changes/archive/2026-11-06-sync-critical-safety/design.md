# Design

## Context

Ver `proposal.md` — Why. O sync atual envia todos os pendentes numa única chamada e usa o full pull para reconciliar o catálogo local com o do servidor. O servidor já tem um limite de 200 registos por push e 500 fotos por cliente, mas o cliente não o conhece nem o aplica. O watermark é limpo em vários caminhos (import de configuração, backup restaurado, resposta `stale`) e o full pull apaga registos locais não pendentes que não vieram do servidor.

## Goals / Non-Goals

**Goals:**

- Tornar o push escalável e resistente a um lote inválido, sem estados de sync presos.
- Tornar o full pull seguro por omissão, com deteção de servidor novo/restaurado e consentimento explícito para substituição.
- Garantir que nenhuma remoção local depende de uma paginação incompleta.
- Manter compatibilidade com servidores e clientes antigos.

**Non-Goals:**

- Não alterar o modelo de conflitos LWW nem o formato dos registos.
- Não introduzir sincronização multi-servidor simultânea.
- Não implementar backup automático do servidor (fica na change de hardening residual).
- Não rever a UI de lista/detalhe além do necessário para o aviso de dados local-only e confirmação.

## Decisions

### 1. Lotes de push com limite descoberto no servidor

O cliente usa um tamanho de lote conservador (100) e o servidor passa a anunciar o seu limite efetivo no endpoint de estado (`/api/latest`, campo aditivo `max_push_records`). O cliente usa `min(100, limite anunciado)`; se o campo não existir (servidor antigo), usa 100, que é inferior ao default 200. O servidor continua a validar o limite e devolve 422 com o máximo quando excedido.

**Alternativas consideradas:** constante fixa de 200 no cliente (falha se o operador baixar o limite); dividir reativamente após 422 (primeiro pedido já inválido e registos ficam pending). O anúncio é aditivo e compatível.

### 2. Duas fases por lote: metadados, depois fotos

Para evitar uploads órfãos, o cliente envia primeiro os registos do lote. O servidor aceita registos com fotos em falta e devolve `missing_photos`. Só depois o cliente envia os bytes das fotos em falta, uma a uma. Um registo só é marcado como sincronizado quando os seus metadados e fotos foram confirmados; se uma foto falhar, o registo permanece pending e o lote seguinte volta a tentar.

**Alternativas consideradas:** manter upload-first e adicionar `DELETE /api/photos/{id}` para limpar órfãos (mais API e mais estados); upload-first com GC periódico (P1) deixa janela de disco e não garante limpeza imediata.

### 3. Full pull não destrutivo por omissão

`pull()` distingue três resultados: completo, incompleto e incremental. Só um pull completo pode ser usado para reconciliação, e mesmo assim os registos locais ausentes do servidor são preservados como “local-only”. A remoção só acontece numa ação explícita do utilizador (“Substituir dados locais pelos do servidor”), que cria primeiro um snapshot local.

**Alternativas consideradas:** manter a remoção automática mas exigir confirmação apenas quando o servidor está vazio (não cobre backup antigo nem cursor inválido); remover sempre os local-only (perda de dados); nunca reconciliar (dados duplicados). A preservação por omissão é a única opção segura para dados de clientes.

### 4. Identidade de instância persistida na base de dados

O servidor gera um UUID na primeira inicialização e guarda-o numa tabela de metadados (`server_meta`), devolvendo-o em `/health` e `/api/latest`. Como vive na DB, sobrevive a restarts e muda quando o volume/DB é recriado. O cliente guarda a identidade associada à configuração; se mudar, bloqueia a reconciliação automática e mostra um aviso.

**Alternativas consideradas:** UUID em ficheiro separado (pode perder-se com o volume); derivar do caminho da DB (não muda numa reposição); não usar identidade e confiar no watermark (não distingue servidor novo de base vazia).

### 5. `stale` não limpa o watermark

Quando o servidor rejeita um push como `stale`, o cliente marca o registo como resolvido pelo servidor e atualiza o registo local a partir de uma leitura de `/api/clients` (filtrada pelo id) ou do próximo pull incremental. O watermark não é limpo; se for necessário um full pull, este é não destrutivo.

**Alternativas consideradas:** manter o clear do watermark (dispara pulls caros e, com a lógica antiga, destrutivos); apagar o registo local (perda de edição); reenviar com timestamp maior (viola LWW).

### 6. Snapshot local antes de substituição

O snapshot usa `VACUUM INTO` para uma cópia consistente da DB Room e copia o diretório de fotos para `filesDir/snapshots/<timestamp>/`. A operação é feita fora da transação principal e o snapshot é removido apenas após confirmação de sucesso. Se o snapshot falhar, a substituição não avança.

**Alternativas consideradas:** exportar JSON (perde fotos e requer novo formato); confiar no backup do dispositivo (não existe garantia); não fazer snapshot (irreversível).

## Risks / Trade-offs

- [Duas fases aumentam o número de pedidos] → Lotes de 100 e uploads apenas de fotos em falta; o tráfego total de fotos mantém-se.
- [Servidor antigo não anuncia `max_push_records`] → fallback de 100, abaixo do default 200; se o operador tiver baixado o limite, o 422 continua a ser tratado e o cliente reduz o lote.
- [Identidade nova bloqueia sync até confirmação] → mensagem clara e opção “continuar sem substituir” para preservar dados locais; o utilizador pode confirmar a substituição com snapshot.
- [Snapshots ocupam espaço] → retenção configurável (por exemplo, últimos 3) e limpeza após sucesso; documentar.
- [Preservar local-only pode duplicar dados] → UI mostra “apenas local” e permite resolver; não há remoção silenciosa.
- [Registos com fotos em falta ficam pending] → o retry é idempotente e o estado é visível; o servidor já suporta placeholders.

## Migration Plan

1. Servidor: criar `server_meta` de forma idempotente e expor `instance_id`/`max_push_records`; clientes antigos ignoram os campos.
2. Android: atualizar o cliente para ler os campos (com fallback), dividir o push e implementar o pull seguro.
3. Não há migração de dados destrutiva; o primeiro pull após upgrade preserva local-only.
4. Rollback: o servidor pode voltar à versão anterior (os campos adicionais desaparecem); o app antigo continua a funcionar, mas mantém o comportamento de full pull destrutivo, pelo que o rollout deve ser app primeiro ou ambos em conjunto.
