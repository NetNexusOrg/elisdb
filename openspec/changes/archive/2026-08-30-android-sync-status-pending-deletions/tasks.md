## 1. Incluir eliminações pendentes no indicador

- [x] 1.1 Em `SyncManager.kt`, calcular `hasPendingUploads` combinando edições pendentes com eliminações pendentes (`getPendingDeletions()` ou `getAll()`)
- [x] 1.2 Expor o sinal combinado no `StateFlow` de estado de sync
- [x] 1.3 Garantir que `ClienteDao` expõe um observável de eliminações pendentes (ou usar `getAll()`)

## 2. Testes e verificação

- [x] 2.1 Testar que apagar um cliente offline faz o indicador mostrar "pendente" (uploads pendentes)
- [x] 2.2 Após sync bem-sucedido do delete, o indicador limpa
- [x] 2.3 Correr `./gradlew :app:assembleDebug` e testes da app
