## 1. Servidor

- [x] 1.1 Em `app/keys.py`, comando `issue`: adicionar `--server-url` (ou env `ELISDB_SERVER_URL`, obrigatório) e `--output` (default `<nome>.config.json`).
- [x] 1.2 Gravar o ficheiro JSON `{"server_url": ..., "private_seed": ...}` com a seed gerada e manter o registo na allowlist e o resumo no terminal.
- [x] 1.3 Atualizar a docs do servidor (`docs/configuracao.md`, `docs/uso.md`, help do módulo) para o novo comando e formato.
- [x] 1.4 Corrigir testes: adicionar casos para emissão do ficheiro, recusa sem `--server-url`, e ficheiro com URL+seed; `python -m pytest` verde.

## 2. App Android

- [x] 2.1 Em `SettingsViewModel.kt`: substituir `extractSeed`/`SEED_LABEL` por parsing do ficheiro JSON; `importKey` grava `baseUrl`+`seed` no `SettingsStore`, aplica e trigga sync; validação (`isValidSeed`, URL não vazio) mantém-se com mensagens claras.
- [x] 2.2 Em `SettingsScreen.kt`: atualizar labels/ajuda para o ficheiro `.config.json` e o comando com `--server-url`.
- [x] 2.3 Testes unitários (`SettingsViewModelTest`): import configura URL+chave, rejeita ficheiro inválido sem alterar o existente, atualiza configuração existente.
- [x] 2.4 Testes instrumentados (`SettingsScreenTest`): fluxo de import e UI.

## 3. Verificação

- [x] 3.1 `python -m app.keys issue --server-url http://10.0.2.2:8000 <nome>` do servidor gera o `.config.json` com URL+seed.
- [x] 3.2 No emulador, importar o ficheiro de configuração deixa URL+chave preenchidos e a sincronização corre sem passos extra.
- [x] 3.3 Correr suites: `testDebugUnitTest`, `connectedDebugAndroidTest` e testes do servidor.