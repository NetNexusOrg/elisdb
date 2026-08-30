## Why

O onboarding de um dispositivo exige hoje dois passos manuais: o operador regista a
chave emitida (`python -m app.keys issue <nome>`) e depois configura o URL do
servidor na app nas Definições. A app extrai o seed procurando o label
"private seed (import into app):" no output do comando — um contrato frágil.
Emitir um único ficheiro de configuração com o URL do servidor e a chave resolve o
onboarding de uma só vez: o servidor entrega o ficheiro, o operador importa-o na
app e fica tudo configurado.

## What Changes

- O comando `python -m app.keys issue <nome>` do servidor passa a emitir um ficheiro
  de configuração JSON com o URL do servidor e a seed privada ed25519, em vez de
  apenas imprimir a chave. Para tal o comando passa a conhecer o URL do servidor
  (flag `--server-url` ou env `ELISDB_SERVER_URL`, obrigatório) e grava o ficheiro
  (`--output`, por omissão `<nome>.config.json`); o registo na allowlist mantém-se.
- A app substitui a importação da seed por importação do ficheiro de configuração:
  `importKey` lê `server_url` e `private_seed`, grava ambos no `SettingsStore` e
  aplica/trigga a sincronização, configurando o dispositivo de uma só vez. A
  extração por label do output do comando é removida.
- Labels e texto de ajuda das Definições passam a referenciar o ficheiro de
  configuração emitido pelo servidor.

## Capabilities

### New Capabilities
- nenhuma.

### Modified Capabilities
- `server-key-provisioning`: a emissão deixa de devolver só o seed — o comando
  `issue` emite um ficheiro de configuração JSON com o URL do servidor e o seed
  privado (a allowlist e os restantes comandos mantêm-se).
- `client-key-import`: a importação passa de "ficheiro com seed" para "ficheiro de
  configuração" que define URL + chave ao mesmo tempo; o cenário que tolerava o
  output do comando é substituído pela validação do ficheiro JSON.

## Impact

- Servidor: `elisdb-server/app/keys.py` (comando `issue`: `--server-url`, escrita
  do ficheiro JSON), docs (`docs/configuracao.md`, `docs/uso.md`) e testes em
  `test_main.py`.
- App Android: `SettingsViewModel.kt` (parser do ficheiro de configuração no lugar
  de `extractSeed`), `SettingsScreen.kt` (labels/ajuda), testes unitários e
  instrumentados.
- Sem alterações ao transporte de sync (`ApiClient`), ao `SettingsStore` (já guarda
  URL + chave), ao motor de sincronização nem à allowlist.