# Proposal

## Why

O sync tem dois bloqueadores: (1) o cliente envia todos os registos pendentes numa única chamada, mas o servidor rejeita lotes acima de 200 registos (ou 500 fotos por cliente), deixando o sync permanentemente preso; (2) um full pull pode apagar clientes e fotos locais não pendentes quando o servidor está vazio, foi restaurado de um backup antigo, mudou de identidade ou devolveu uma paginação incompleta. Estes problemas impedem a utilização segura em cenários reais de recuperação e de bases com mais de 200 clientes.

## What Changes

- O push passa a ser feito em lotes limitados (por omissão 200 registos), aplicando os resultados de cada lote e isolando lotes/registos inválidos para que não bloqueiem os restantes.
- O cliente valida o limite de 500 fotos por cliente antes de enviar e falha de forma explícita e acionável quando o limite é excedido, em vez de deixar o lote inteiro em erro 422.
- As fotos só são enviadas depois de o lote de registos ser aceite, ou os uploads órfãos são limpos de forma determinística; uma falha de push não deixa fotos órfãs indefinidamente.
- O full pull deixa de ser destrutivo por omissão: registos locais não presentes no servidor são preservados e apresentados como “apenas locais”, salvo uma ação explícita do utilizador para substituir os dados locais pelos do servidor.
- O servidor passa a expor uma identidade de instância estável; o cliente guarda-a e exige confirmação quando a identidade muda (servidor novo, volume recriado, backup restaurado).
- A reconciliação local só corre depois de a paginação terminar com `next_since == null`; um cursor repetido ou uma página incompleta nunca desencadeia remoção de registos.
- Um `stale` de push deixa de limpar automaticamente o watermark e de originar um full pull destrutivo.
- Antes de uma substituição destrutiva explícita, o cliente cria um snapshot/backup local recuperável.

## Capabilities

### New Capabilities

Nenhuma. A alteração corrige comportamento existente.

### Modified Capabilities

- `client-sync`: push em lotes, limite de fotos validado no cliente, isolamento de erros e reconciliação local não destrutiva.
- `sync-incremental-pull`: full pull seguro, identidade do servidor e conclusão obrigatória da paginação antes de qualquer remoção.
- `server-sync`: limite de lote explícito e identidade de instância estável na resposta de estado.
- `server-version`: identidade de instância exposta em `/health` e no estado autenticado.

## Impact

- Android: `SyncCoordinator`, `SyncManager`, `ApiClient`, `AppContainer`, `ServerConfigViewModel`, `ClienteRepositoryImpl` e testes de sync.
- Servidor: `app/main.py`, `app/database.py` (ou configuração), endpoints de estado e testes de contrato.
- API: campos aditivos de identidade de instância; sem breaking change para clientes antigos.
- Dados: nenhuma migração destrutiva; o comportamento de pull muda para preservar dados locais.
- Operação: servidores novos/restaurados passam a ser detetados em vez de apagarem dados silenciosamente.
