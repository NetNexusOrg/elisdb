# Proposal

## Why

A revisão encontrou cinco riscos de segurança e integridade: seeds privadas de contas auto-provisionadas são impressas nos logs do container; o endpoint de upload de fotos aceita ficheiros arbitrários sem quota; uma deleção com timestamp antigo ignora o last-write-wins e apaga uma edição mais recente; uma troca de configuração não cancela o sync em curso; e o sweep de fotos pode apagar fotos staged ainda não guardadas. Em conjunto, estes problemas podem expor credenciais, esgotar o disco, perder edições e perder fotos do utilizador.

## What Changes

- O provisionamento deixa de imprimir seeds privadas; escreve apenas o ficheiro de configuração `0600` e regista o `key_id`/caminho, nunca o segredo.
- Nomes de utilizador e comentários usados em caminhos de configuração são sanitizados e confinados ao diretório de configs; `..` e separadores são rejeitados.
- O upload de fotos passa a exigir uma foto referenciada/reservada, com quotas por chave e por instalação, limite agregado de disco e GC periódico independente de pushes; o servidor responde com erro claro quando a quota é excedida.
- A deleção no servidor passa a respeitar LWW: uma deleção mais antiga do que o `updated_at` guardado é rejeitada como stale; timestamps antigos/negativos implausíveis são rejeitados.
- O cliente compara o timestamp do tombstone com o `updatedAt` local e, se a edição local for mais recente, ressuscita/re-envia em vez de apagar.
- Limpar ou trocar a configuração cancela e aguarda o sync em curso antes de limpar o estado; um sync antigo não pode voltar a publicar watermark, hashes ou sucesso depois do reset.
- O sweep de fotos protege ficheiros staged/capturados e aplica uma carência mínima; staging e captura passam a ser serializados com o sweep.

## Capabilities

### New Capabilities

Nenhuma. A alteração corrige comportamento existente.

### Modified Capabilities

- `server-key-provisioning`: nunca registar seeds privadas em logs e confinar caminhos de configuração.
- `server-photo-storage`: upload referenciado, quotas, GC periódico e proteção contra esgotamento de disco.
- `server-sync`: deleção participa em LWW e rejeita timestamps implausíveis.
- `client-sync`: tombstones não apagam uma edição local mais recente.
- `client-resilience`: troca de configuração cancela e aguarda o sync em curso.
- `photo-hygiene`: sweep não apaga fotos staged/capturadas e respeita uma carência.

## Impact

- Servidor: `app/keys.py`, `app/provision.py`, `app/main.py`, `app/database.py`, `compose.yaml`, testes de auth/provisionamento/fotos/LWW.
- Android: `SyncManager`, `SyncCoordinator`, `ServerConfigViewModel`, `ClienteRepositoryImpl`, `PhotoSweeper`, `PhotoStoreGuard` e testes.
- Operação: rotação das seeds que já possam ter sido expostas em logs; revisão de quotas e do espaço em disco.
- API: erros adicionais para upload não referenciado/quota e para deleção stale; compatível para clientes que cumpram o contrato.
