## 1. Controlo de versões

- [ ] 1.1 Inicializar o repositório Git na raiz do monorepo
- [ ] 1.2 Criar `.gitignore` na raiz (Gradle/Android, Python, Node/OpenCode, secrets, dados)
- [ ] 1.3 Verificar que segredos e dados ficam excluídos (`git status`/`git check-ignore`)
- [ ] 1.4 Definir a identidade Git para os commits (user.name/user.email)

## 2. Segurança

- [ ] 2.1 Auditar o repositório à procura de secrets antes do primeiro commit
- [ ] 2.2 Confirmar com o utilizador os ficheiros com dados/segredos excluídos do versionamento

## 3. Primeiro commit

- [ ] 3.1 Criar o commit inicial `chore: initial project baseline` (sem alterar código)

## 4. Repositório GitHub

- [ ] 4.1 Verificar `gh auth status`
- [ ] 4.2 Criar repositório PRIVATE `elisdb` (sem README/.gitignore automáticos)
- [ ] 4.3 Configurar remote `origin` por SSH (`git@github.com:NetNexusOrg/elisdb.git`)

## 5. GitHub Actions

- [ ] 5.1 Criar `.github/workflows/android-release.yml` (jobs `test` e `release`)
- [ ] 5.2 Usar Java 17 + Android SDK + Gradle Wrapper do projeto
- [ ] 5.3 Fazer upload do APK como artifact e criar GitHub Release em tags `v*`
- [ ] 5.4 Aplicar permissions mínimas e sem impressão de segredos nos logs

## 6. Signing

- [ ] 6.1 Mapear secrets `ANDROID_KEYSTORE_BASE64`/`ANDROID_KEYSTORE_PASSWORD`/`ANDROID_KEY_ALIAS`/`ANDROID_KEY_PASSWORD` para as env vars de signing existentes
- [ ] 6.2 Documentar como configurar os secrets no GitHub (sem valores fictícios)

## 7. Verificação

- [ ] 7.1 Validar o YAML do workflow e o `openspec validate`
- [ ] 7.2 Confirmar build/testes locais quando possível
- [ ] 7.3 Confirmar remote, branch `main` e repositório PRIVATE

## 8. Push inicial

- [ ] 8.1 Fazer push do branch `main` (sem criar release de produção)
- [ ] 8.2 Confirmar que o código e o workflow estão no GitHub
