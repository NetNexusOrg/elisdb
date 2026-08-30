## 1. Restringir listagem de chaves

- [x] 1.1 Em `app/main.py`, `GET /api/keys` passa a devolver 403 (ou ser removido) para dispositivos cliente autenticados
- [x] 1.2 Manter a listagem disponível para o operador via CLI (`python -m app.keys`)
- [x] 1.3 `GET /api/keys/me` mantém-se funcional para dispositivos

## 2. Testes e verificação

- [x] 2.1 Em `test_main.py`, dispositivo autenticado a `/api/keys` → 403 (ou erro); `/api/keys/me` → 200 com a própria identidade
- [x] 2.2 CLI continua a listar chaves
- [x] 2.3 Correr `pytest`
