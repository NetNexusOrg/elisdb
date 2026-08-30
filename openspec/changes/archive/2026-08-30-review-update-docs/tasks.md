## 1. Review e atualização da documentação da app

- [x] 1.1 Rever `elisdb-android/README.md` e `docs/` (index, instalacao, execucao, configuracao, uso) contra o comportamento real
- [x] 1.2 Corrigir comandos de emissão de chave para `docker compose exec elisdb-server python -m app.keys issue ...` em `README.md` e `docs/uso.md`
- [x] 1.3 Uniformizar o nome visível da app como "ElisDB" onde ainda não estiver (mantendo `com.example.elisdb` e restantes identificadores técnicos)
- [x] 1.4 Verificar ligações cruzadas para `../elisdb-server/docs/` e corrigir referências quebradas/obsoletas

## 2. Review e atualização da documentação do servidor

- [x] 2.1 Rever `elisdb-server/README.md` e `docs/` (index, instalacao, execucao, configuracao, uso, nginx-proxy-manager) contra o comportamento real
- [x] 2.2 Confirmar que toda a documentação do servidor é Docker-first e sem instruções `venv`/`uvicorn`/`pytest` residuais
- [x] 2.3 Verificar coerência dos passos de provisionamento (`ELISDB_USERS`) e emissão de chave entre os guias

## 3. Review do README raiz e coerência global

- [x] 3.1 Rever `README.md` (raiz) para refletir o fluxo atual (app "ElisDB", servidor Docker-first)
- [x] 3.2 Confirmar que app e servidor se referem mutuamente de forma coerente e sem contradições
- [x] 3.3 Varrer o repo por referências obsoletas a `venv`, `uvicorn`, `pytest` e execução manual no contexto de documentação de utilizador e corrigir
