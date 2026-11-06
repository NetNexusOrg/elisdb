# Tasks

## 1. Contrato do servidor

- [x] 1.1 Criar a tabela `server_meta` com um `instance_id` UUID gerado uma vez e devolvê-lo em `/health` e `/api/latest`; verificar com um teste que o id é estável após reiniciar a app de teste sobre a mesma DB e muda com uma DB nova.
- [x] 1.2 Expor `max_push_records` em `/api/latest` e garantir que o limite configurado é o mesmo usado na validação do push; verificar com teste de contrato e com `ELISDB_MAX_PUSH_RECORDS` alterado.
- [x] 1.3 Garantir que um lote acima do limite continua a devolver 422 sem persistir registos e que a resposta identifica o máximo; verificar com teste de batch oversize e contagem de linhas.
- [x] 1.4 Documentar os campos aditivos e a compatibilidade com clientes antigos em `elisdb-server/docs/`.

## 2. Push em lotes no Android

- [x] 2.1 Introduzir um tamanho de lote efetivo calculado a partir de `max_push_records` (fallback 100) e dividir os pendentes em lotes; verificar com teste de 201 registos e com um limite de servidor inferior a 100.
- [x] 2.2 Aplicar os resultados de cada lote antes do lote seguinte e isolar falhas por lote, mantendo os registos não confirmados em pending; verificar com testes de falha no lote 1 e sucesso no lote 2.
- [x] 2.3 Validar o limite de 500 fotos por cliente antes de qualquer upload e devolver uma mensagem acionável; verificar com teste de 501 fotos e com um cliente exatamente no limite.
- [x] 2.4 Implementar o fluxo em duas fases (metadados, depois upload das `missing_photos`) e só marcar o registo como sincronizado após confirmação das fotos; verificar com testes de foto em falta, upload falhado e retry.
- [x] 2.5 Garantir que um push rejeitado não deixa ficheiros enviados órfãos e que os uploads de um lote rejeitado não são feitos; verificar com teste de lote 422 e ausência de chamadas de upload.
- [x] 2.6 Atualizar a documentação de sync do Android com o novo fluxo de lotes e limites.

## 3. Pull seguro

- [x] 3.1 Introduzir um resultado explícito de paginação (`complete`/`incomplete`) e só permitir reconciliação quando `next_since == null`; verificar com teste de cursor repetido e de página parcial.
- [x] 3.2 Preservar registos locais não pendentes ausentes do servidor e expô-los como “apenas local” em vez de os apagar; verificar com teste de servidor vazio e de servidor com menos registos.
- [x] 3.3 Remover a limpeza automática do watermark em respostas `stale` e substituir por atualização do registo a partir de `/api/clients` ou do pull incremental; verificar com teste de `stale` que o watermark não é limpo.
- [x] 3.4 Garantir que `refreshPendingDownloads` e a UI refletem dados local-only sem os tratar como erro fatal; verificar com teste de estado e screenshot/teste Compose se aplicável.

## 4. Identidade, consentimento e snapshot

- [x] 4.1 Guardar a identidade do servidor nas settings e detetar mudanças; verificar com teste de mudança de identidade e de identidade igual.
- [x] 4.2 Bloquear a reconciliação automática quando a identidade muda ou quando o utilizador ainda não confirmou; verificar com teste de sync que não há remoção local.
- [x] 4.3 Adicionar a ação explícita “Substituir dados locais pelos do servidor” com confirmação e snapshot local (`VACUUM INTO` + cópia de fotos); verificar com teste de snapshot criado antes da remoção e de falha de snapshot que aborta.
- [x] 4.4 Implementar retenção/limpeza de snapshots e documentar o procedimento de recuperação.
- [x] 4.5 Testar o fluxo completo: servidor vazio, servidor restaurado, identidade mudada, confirmação explícita e recuperação a partir do snapshot.

## 5. Verificação integrada

- [x] 5.1 Correr `elisdb-server/.venv/bin/python -m pytest -q` e garantir que a suite do servidor passa.
- [x] 5.2 Correr `elisdb-android/gradlew :app:testDebugUnitTest` e garantir que os testes de sync passam.
- [x] 5.3 Validar a change com `openspec validate sync-critical-safety` (e `--strict` se o projeto o exigir).

## Workflow follow-up

- Arquivar a change depois da implementação e da revisão.
