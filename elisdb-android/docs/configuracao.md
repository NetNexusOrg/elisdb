# Configuração da app

Três coisas podem ser configuradas: a versão, a assinatura do APK final e a
ligação ao servidor (sincronização). Só a **ligação ao servidor** precisa de
atenção regular — as outras ficam definidas uma vez.

## Versão da app

O número de versão tem **uma única fonte**: o ficheiro `gradle.properties` na
raiz de `elisdb-android/`. Altera sempre aqui, nunca em `app/build.gradle.kts`:

```properties
app.versionCode=1
app.versionName=1.0
```

## Assinatura do APK final (release)

As credenciais de assinatura são lidas de **`keystore.properties`** (na raiz de
`elisdb-android/`; ignorado pelo git) ou, na ausência, de variáveis de ambiente.
Conteúdo de `keystore.properties`:

```properties
keystore.path=/etc/elisdb/release.keystore
keystore.password=...
key.alias=...
key.password=...
```

Em alternativa, por variáveis de ambiente:

| Propriedade `keystore.properties` | Variável de ambiente |
| --------------------------------- | -------------------- |
| `keystore.path`                   | `KEYSTORE_PATH`      |
| `keystore.password`               | `KEYSTORE_PASSWORD`  |
| `key.alias`                       | `KEY_ALIAS`          |
| `key.password`                    | `KEY_PASSWORD`       |

> **Fail-fast:** um build de release sem credenciais falha de propósito, em vez
> de produzir um APK final não assinado.

### Keystore de teste (só para desenvolver)

Para criar um keystore de teste e apontar o `keystore.properties` para ele:

```bash
keytool -genkeypair -keystore elisdb.keystore -storepass <pass> -alias elisdb \
  -keypass <pass> -keyalg RSA -keysize 2048 -validity 3650 -dname "CN=ElisDB,O=ElisDB,C=PT"
```

## Ligação ao servidor (sincronização)

A app sabe para onde sincronizar através de **URL do servidor** e **chave de
assinatura**. Há duas formas de os definir:

### 1. Em tempo de execução (recomendado)

Abre a app → menu (⋮) → **Configuração** → **Importar configuração do servidor** e
escolhe o ficheiro `<nome>.config.json` emitido no servidor. O URL, a chave e o
utilizador associado ficam preenchidos de uma só vez e a sincronização arranca. O
cartão **Servidor** é só de leitura (não há edição manual).

> Este é o caminho mais simples: ver [Utilização](uso.md), que mostra como
> emitir o ficheiro no servidor.

### 2. No momento do build (opcional)

As variáveis de ambiente preenchem dois campos no momento do build:

| Campo                     | Variável de ambiente   | Omissão                           |
| ------------------------- | ---------------------- | --------------------------------- |
| URL do servidor           | `ELISDB_SERVER_URL`    | *(vazio — sem servidor embutido)* |
| Chave de assinatura       | `ELISDB_PRIVATE_KEY`   | *(vazio — sem chave embutida)*    |

Mesmo sem estas variáveis, a app funciona offline; a sincronização só fica
ativa quando houver URL e chave (de build **ou** do ecrã Configuração).

Exemplo:

```bash
ELISDB_SERVER_URL="https://db.example.com" \
ELISDB_PRIVATE_KEY="<seed base64 de 32 bytes>" \
./gradlew :app:assembleRelease
```

Para obter a chave e registar a chave pública no servidor, vê o fluxo único em
[Utilização](uso.md) e a configuração do servidor
(`../elisdb-server/docs/configuracao.md`).

### Se as duas formas forem usadas

A app guarda o que for importado no ecrã **Configuração**; se existir um valor
guardado, **ele sobrepõe-se** ao valor do build. Sem valor guardado, usa o do
build. Os builds existentes continuam a funcionar sem alterações.