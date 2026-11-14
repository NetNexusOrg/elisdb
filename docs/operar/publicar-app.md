# Publicar uma versão da app

Uma tag `v*` faz três coisas: constrói o APK, cria a GitHub Release e copia o
APK para o servidor, de onde os telemóveis o descarregam.

## 1. Preparar

1. Sobe `app.versionCode` e `app.versionName` em
   `elisdb-android/gradle.properties` (fonte única da versão).
2. Escreve a secção da versão em `elisdb-android/CHANGELOG.md`:

   ```markdown
   ## [2.1.0]

   **Novidades**
   - ...

   **Correções**
   - ...
   ```

   Esta secção é o texto do release no GitHub. **Uma tag sem secção
   correspondente falha o release antes de gerar o APK.**

3. Antes do release, o job **Sync smoke** corre um push/pull/delete real contra
   uma base descartável; se falhar, o APK não é publicado.

## 2. Criar a tag

```bash
git tag v2.1.0
git push origin v2.1.0
```

O workflow `android-ci.yml`:
- corre testes e lint;
- constrói o APK assinado;
- cria a Release com `elisdb-<versão>-<código>.apk`;
- envia o APK para `elisdb-data/apk/latest/` na VPS por `rsync`.

Não é preciso reiniciar o servidor: o ficheiro é descoberto no pedido seguinte.

## 3. Nos telemóveis

**Sobre** → **Procurar atualizações** → **Transferir** → **Instalar**. A app
também verifica em segundo plano ao abrir e mostra um aviso.

## Configurar a publicação automática (uma vez)

O CI autentica-se por chave SSH dedicada (nunca a pessoal):

```bash
ssh-keygen -t ed25519 -C "github-actions@elisdb" -f ~/.ssh/elisdb_deploy -N ""
ssh-copy-id -i ~/.ssh/elisdb_deploy.pub ubuntu@vps01

gh secret set DEPLOY_SSH_KEY < ~/.ssh/elisdb_deploy
gh secret set DEPLOY_HOST --body "144.21.63.93"
gh secret set DEPLOY_USER --body "ubuntu"

# Fixa a host key: sem este secret a publicação é saltada (não há ssh-keyscan
# automático em runtime).
ssh-keyscan -H 144.21.63.93 | gh secret set DEPLOY_KNOWN_HOSTS
```

Opcionais: `DEPLOY_PORT` (22) e `DEPLOY_PATH`
(`docker/elisdb-server/elisdb-data/apk/latest/` relativo à home).

Enquanto faltar `DEPLOY_SSH_KEY`, `DEPLOY_HOST`, `DEPLOY_USER` ou
`DEPLOY_KNOWN_HOSTS`, o passo é ignorado com aviso e o release continua.

### Restringir a chave (recomendado)

Em `~/.ssh/authorized_keys` na VPS, limita a chave a essa pasta:

```
restrict,command="rrsync -wo /home/ubuntu/docker/elisdb-server/elisdb-data/apk/latest" ssh-ed25519 AAAA... github-actions@elisdb
```

Com `rrsync`, o caminho enviado é relativo à pasta restrita — define então
`DEPLOY_PATH=/`:

```bash
gh secret set DEPLOY_PATH --body "/"
```

Para testar a chave, ignora o teu `~/.ssh/config`:

```bash
ssh -F /dev/null -i ~/.ssh/elisdb_deploy -o IdentitiesOnly=yes ubuntu@144.21.63.93 true
```

## Publicar à mão (alternativa)

1. Descarrega o APK da GitHub Release (já vem como
   `elisdb-<versão>-<código>.apk`).
2. Copia-o mantendo o nome:

   ```bash
   scp elisdb-2.1.0-21.apk ubuntu@vps01:~/docker/elisdb-server/elisdb-data/apk/latest/
   ```

3. Nos telemóveis, **Procurar atualizações**.

O servidor serve o ficheiro com o **maior `versionCode`** da pasta; nomes fora
do padrão são ignorados (podes deixar o build seguinte ao lado). Para retirar a
atualização, apaga o ficheiro.

A pasta pode ser mudada com `ELISDB_APK_DIR`. Os endpoints
(`/api/app/latest`, `/api/app/download`) são autenticados e servem sempre o
ficheiro descoberto — não aceitam nome de ficheiro.
