## 1. Configurar logging

- [x] 1.1 Em `app/main.py`, configurar logging estruturado no startup (nível de `ELISDB_LOG_LEVEL`, default INFO)
- [x] 1.2 Registar evento de startup
- [x] 1.3 Intercetar erros não tratados (500) e registá-los sem corpo de foto nem segredos

## 2. Registar rejeições de auth

- [x] 2.1 Registar 401 (assinatura inválida/em falta) com método/path/host, sem a assinatura nem o body
- [x] 2.2 Garantir que seeds/chaves não entram nos logs

## 3. Verificação

- [x] 3.1 Correr o servidor e confirmar que startup/erro/401 aparecem nos logs do stdout
- [x] 3.2 Correr `pytest`
