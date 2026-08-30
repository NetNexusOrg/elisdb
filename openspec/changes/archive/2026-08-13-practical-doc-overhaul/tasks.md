## 1. Verificação dos factos no código

- [x] 1.1 Confirmar no código os comandos reais da CLI do servidor (`app.keys issue` apenas; sem `generate`/`add`/`remove`/`list`), os endpoints reais (`/health`, `/api/push`, `/api/pull`, `/api/clients`, `/api/clients/{id}` DELETE, `/api/keys`, `/api/keys/me`) e as variáveis de ambiente (`ELISDB_DB`, `ELISDB_PHOTOS`, `ELISDB_SERVER_URL`).
- [x] 1.2 Confirmar no código da app os passos reais de build/instalação (Gradle, `scripts/build-release.sh`, credenciais de release, runtime override nas Definições) e as mensagens reais da barra de estado de sincronização.

## 2. Documentação do servidor

- [x] 2.1 Reescrever `elisdb-server/docs/index.md`: abrir com o fluxo prático (instalar → correr → emitir config → ligar a app), manter links e referência técnica como secção de consulta.
- [x] 2.2 Reescrever `elisdb-server/docs/instalacao.md` orientado a passos com pré-requisitos claros e verificação de sucesso ao fim (comandos exatos).
- [x] 2.3 Reescrever `elisdb-server/docs/execucao.md` (dev + Docker) com linguagem prática: o que corre, onde surgem os dados, como verificar `/health`, como parar.
- [x] 2.4 Reescrever `elisdb-server/docs/configuracao.md`: fluxo único `app.keys issue` em primeiro lugar com exemplo completo executável; variáveis de ambiente e observabilidade (`/api/keys`, `/api/keys/me`) como referência.
- [x] 2.5 Reescrever `elisdb-server/docs/uso.md`: endpoints em secção de consulta; resolução de problemas (401, duplicate, stale, 400) em tabela sintoma → causa → correção; manter nota de segurança pública.

## 3. Documentação da app

- [x] 3.1 Reescrever `elisdb-android/docs/index.md`: abrir com o objetivo da app e o fluxo para a ligar ao servidor em passos; referência técnica (stack, build) como consulta.
- [x] 3.2 Reescrever `elisdb-android/docs/instalacao.md`: pré-requisitos claros (JDK, SDK, `local.properties`) e verificação de sucesso ao fim.
- [x] 3.3 Reescrever `elisdb-android/docs/execucao.md`: construir debug, instalar em dispositivo/emulador, release assinado e script; além disso "como executar no telemóvel".
- [x] 3.4 Reescrever `elisdb-android/docs/configuracao.md`: versão, keystore e ligação ao servidor — mantendo o fluxo único `app.keys issue` e o runtime override, em linguagem prática.
- [x] 3.5 Reescrever `elisdb-android/docs/uso.md`: primeira execução (emitir → importar → sincronizar), uso diário offline-first e resolução de problemas com as mensagens reais.

## 4. READMEs e consistência

- [x] 4.1 Reescrever o `README.md` raiz: o que é o ElisDB, os dois subprojetos, e o passo seguinte concreto para quem quer começar a usar.
- [x] 4.2 Reescrever `elisdb-server/README.md` e `elisdb-android/README.md` com início rápido funcional e links para os guias práticos.
- [x] 4.3 Verificar consistência transversal: mesmo fluxo de ligação app↔servidor em todos os documentos, links relativos válidos, sem referências a comandos/endpoints/variáveis removidos ou inexistentes; acrescentar `/api/keys/me` onde a observabilidade é descrita.