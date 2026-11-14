# Assinatura e configuração da app

Três coisas se configuram na app: versão, assinatura do APK final e ligação ao
servidor. Só a última precisa de atenção regular.

## Versão

Fonte única: `elisdb-android/gradle.properties`.

```properties
app.versionCode=20
app.versionName=2.0.0
```

Altera sempre aqui, nunca em `app/build.gradle.kts`. O release do GitHub usa
estes valores e a secção correspondente do `CHANGELOG.md`.

## Assinatura do release

As credenciais vêm de `elisdb-android/keystore.properties` (ignorado pelo git)
ou, na sua ausência, de variáveis de ambiente:

```properties
keystore.path=/caminho/para/elisdb-release.keystore
keystore.password=...
key.alias=...
key.password=...
```

| `keystore.properties` | Variável |
| --- | --- |
| `keystore.path` | `KEYSTORE_PATH` |
| `keystore.password` | `KEYSTORE_PASSWORD` |
| `key.alias` | `KEY_ALIAS` |
| `key.password` | `KEY_PASSWORD` |

Um build de release sem credenciais **falha** (`Release signing not configured`).

O caminho mais simples é o script, que lê `~/.keystores/elisdb-release.env` (ou
`ELISDB_KEYSTORE_ENV`) com as quatro variáveis `KEYSTORE_*`/`KEY_*` e corre
`assembleRelease`:

```bash
elisdb-android/scripts/build-release.sh
```

Para criar um keystore de teste:

```bash
keytool -genkeypair -keystore elisdb.keystore -storepass <pass> -alias elisdb \
  -keypass <pass> -keyalg RSA -keysize 2048 -validity 3650 \
  -dname "CN=ElisDB,O=ElisDB,C=PT"
```

## Ligação ao servidor

A app precisa de **URL do servidor** e **chave de assinatura**. Há duas formas:

**1. Em tempo de execução (recomendado).** Menu ⋮ → **Configuração** →
**Importar credencial do utilizador** e escolhe `<nome>.config.json`. URL, chave
e utilizador ficam preenchidos de uma vez. O cartão de sincronização é só de
leitura. Ver [Ligar um telemóvel](../usar/ligar-telemovel.md).

**2. Em tempo de build (opcional).**

| Campo | Variável | Omissão |
| --- | --- | --- |
| URL do servidor | `ELISDB_SERVER_URL` | vazio |
| Chave de assinatura | `ELISDB_PRIVATE_KEY` | vazio |

```bash
ELISDB_SERVER_URL="https://db.exemplo.com" \
ELISDB_PRIVATE_KEY="<seed base64 de 32 bytes>" \
./gradlew :app:assembleRelease
```

Sem estes valores a app funciona offline; a sincronização só liga quando houver
URL e chave. Se existir uma credencial importada no ecrã Configuração, **ela
sobrepõe-se** à do build.

> `ELISDB_PRIVATE_KEY` é um segredo: nunca a coloques no repositório nem em logs
> de CI. A mesma seed não deve ser partilhada entre telemóveis.
