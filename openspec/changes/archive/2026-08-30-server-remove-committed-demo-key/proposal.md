## Why

O repositório contém `elisdb-server/demo.config.json` com um `private_seed` ed25519 real e funcional commitado. Mesmo estando rotulado como "demo", um seed cuja chave pública correspondente esteja (ou já tenha estado) na allowlist permite a qualquer pessoa autenticar pedidos contra o servidor — é um segredo de autenticação comprometido.

Além disso, o ficheiro demonstra um padrão perigoso: configs de exemplo com segredos reais. Se este seed já foi provisionado alguma vez, deve ser revogado.

## What Changes

- Remover o `demo.config.json` do controlo de versões (e do working tree) para que nenhum segredo vivo fique no repo.
- Adicionar um exemplo não-funcional (`demo.config.example.json`) que contenha apenas placeholders (URL, sem `private_seed` válido), para documentação.
- Garantir que ficheiros de configuração gerados (que contêm seeds) ficam ignorados pelo git (`.gitignore`), pois são produzidos em runtime pela provisioning/issue flow.
- Documentar que configs com seeds reais NUNCA devem ser commitados e, se o seed atual estiver comprometido, revogar a chave na allowlist e reemitir.

## Capabilities

### New Capabilities

### Modified Capabilities
- `server-key-provisioning`: nova requirement "Example configs contain no live secrets" — ficheiros de exemplo/demo commitados NÃO devem conter um `private_seed` funcional; seeds reais só existem em runtime e ficam gitignorados.

## Impact
- Servidor: remoção de `demo.config.json`; novo `demo.config.example.json`; atualização de `.gitignore`; documentação em `docs/`.
- Ops: se o seed estiver na allowlist, o operador deve revogá-lo (`python -m app.keys` remove) e reemitir para os utilizadores afetados.
