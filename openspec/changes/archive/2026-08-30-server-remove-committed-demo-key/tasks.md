## 1. Remover o segredo do repo

- [x] 1.1 Remover `elisdb-server/demo.config.json` do working tree e do version control (sem perdê-lo apenas no working tree se o operador quiser inspecionar)
- [x] 1.2 Se o `private_seed` correspondente já esteve na allowlist, documentar a revogação da chave (remover pelo id via `python -m app.keys`)

## 2. Exemplo não-funcional e gitignore

- [x] 2.1 Criar `elisdb-server/demo.config.example.json` com `server_url` de exemplo e sem `private_seed` funcional, documentando o formato
- [x] 2.2 Atualizar `.gitignore` do servidor para excluir ficheiros de config com seeds reais gerados em runtime, mantendo o exemplo trackeado
- [x] 2.3 Atualizar `docs/` para indicar que configs com seeds reais NUNCA devem ser commitados e que cada utilizador emite o seu próprio

## 3. Verificação

- [x] 3.1 Confirmar que `git status` não mostra nenhum `*.config.json` com seed real por trackear
- [x] 3.2 Importar o exemplo num dispositivo de teste e confirmar que não autentica (falha 401 esperado)
