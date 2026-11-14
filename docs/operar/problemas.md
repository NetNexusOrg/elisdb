# Problemas frequentes

## Na app

| O que aparece | Causa | O que fazer |
|---|---|---|
| **Acesso Negado** no arranque | Falta URL, chave ou nome de utilizador | Importa o `<nome>.config.json` ([ligar um telemóvel](../usar/ligar-telemovel.md)) |
| **Não configurado** | Falta URL ou chave | Importa a credencial, ou emite uma nova no servidor |
| "Servidor rejeitou a sincronização (chave não registada)" | A chave foi revogada ou não existe no servidor | Emite uma chave nova e importa o ficheiro |
| "Relógio do dispositivo dessincronizado" | Mais de ±5 min de diferença | Ativa a hora automática no telemóvel e no servidor; usa **Corrigir** |
| "Falha na sincronização. Verifique a ligação." | Servidor inacessível no URL definido | Confirma a rede e o URL (IP do servidor, não `127.0.0.1`) |
| "Registo «X» foi fundido com um registo existente." | Já havia um cliente com o mesmo nome no servidor | Nada a fazer — o teu registo foi mantido e reenviado |

## No servidor

| Código | Causa | O que fazer |
|---|---|---|
| `401` ao sincronizar | Sem chave, chave revogada ou assinatura inválida | Emite a chave com `elisdb.keys issue` e importa o ficheiro |
| `401` com relógio dessincronizado | Fora da janela de ±300 s | Acerta a hora do telemóvel e do servidor |
| `403` em `/api/keys` | É uma operação de operador | Usa `python -m elisdb.keys list` |
| `404` a enviar foto | O `PUT` chegou antes de o push referenciar a foto | Normal: a app reenvia depois do push |
| `409` ao apagar | A eliminação não é mais recente que o registo | Nada a fazer; o servidor manteve o registo |
| `413` | Foto acima de 20 MiB ou quota excedida | Reduz a foto ou aumenta `ELISDB_MAX_PHOTO_BYTES*` |
| `422` | Lote acima de `ELISDB_MAX_PUSH_RECORDS`/`MAX_PHOTOS_PER_CLIENT` | Divide o envio ou aumenta o limite |
| `429` | Demasiados pedidos do mesmo IP | Espera a janela; atrás de proxy define `ELISDB_TRUST_PROXY=true` |
| `503 database busy` | Escrita em contenção acima do `busy_timeout` | Repetir; se persistir, verifica disco e processos |

## O servidor não arranca

1. `docker compose logs --tail=50` — a mensagem costuma ser explícita.
2. **Permissões:** `elisdb-data/` e `configs/` têm de ser graváveis pelo
   `PUID:PGID` do contentor. O arranque falha (não faz `chown`):
   `sudo chown -R "$(id -u):$(id -g)" elisdb-data configs`.
3. **Porta ocupada:** muda o mapeamento no `compose.yaml` ou liberta a 8000.
4. **`/ready` devolve 503:** base de dados inacessível ou pasta de fotos sem
   escrita.

## Backup/restauro

- `backup verify` falha → a cópia está incompleta; não a uses para restaurar.
- Restaurar uma cópia antiga faz o `instance_id` mudar: a app avisa e preserva os
  dados locais ([detalhe](../operar/backup.md)).

## Ainda não resolve?

```bash
curl http://localhost:8000/health
docker compose ps
docker compose logs --tail=100
python3 start.py --status
```

O `--status` mostra modo, dono e escrita dos diretórios, tamanhos, contagens,
chaves ativas, backups e saúde — resolve a maior parte dos diagnósticos.
