# Migração do `security-crypto`

## Estado atual

As credenciais do dispositivo (URL do servidor, chave ed25519 e nome do
utilizador) são guardadas em `EncryptedSharedPreferences`
(`androidx.security:security-crypto` 1.1.0), com uma chave mestra AES256-GCM no
Android Keystore. A escrita é verificada por leitura de retorno e falha de forma
segura se o armazenamento não estiver disponível.

A biblioteca está **deprecated** (o Jetpack Security foi descontinuado), mas a
API continua estável e funcional. Não há, neste momento, uma substituição oficial
com migração automática.

## Porque não foi migrado já

- O caminho atual já cumpre o objetivo de segurança: a chave privada nunca é
  escrita em texto simples e a chave mestra não sai do Keystore.
- Uma migração mal feita pode **perder a chave do utilizador**, obrigando a
  reimportar o ficheiro de credenciais em todos os dispositivos.
- É necessário testar o upgrade em dispositivos reais (API 24+) e em instalações
  que ainda tenham dados legados em texto simples.

## Plano proposto

1. Criar um `KeystoreSettingsStore` que use uma chave AES-GCM no Android
   Keystore diretamente (sem `EncryptedSharedPreferences`), com um formato
   versionado e um marcador de migração.
2. Na primeira abertura após o upgrade:
   - ler os valores antigos;
   - escrever no novo formato;
   - reler e confirmar;
   - só então remover os valores antigos.
3. Manter um caminho de leitura dos dados antigos enquanto a migração não estiver
   confirmada, para nunca deixar o utilizador sem credenciais.
4. Testes instrumentados de upgrade: instalação limpa, instalação com dados
   antigos, Keystore indisponível e repetição idempotente da migração.
5. Só remover `security-crypto` depois de os testes passarem numa matriz de APIs
   suportadas (24, 30, 34, 37).

## Decisão

Adiado para uma change própria. O risco de perda de credenciais é superior ao
benefício imediato de remover uma dependência deprecated; a decisão deve ser
revista quando houver uma alternativa oficial estável ou uma janela de testes
com dispositivos reais.
