# Documentação do servidor ElisDB

O servidor ElisDB é o programa que **guarda os teus dados** (clientes e fotos)
e os mantém em sincronia com a app Android. Corre **apenas** em Docker
(Docker-only) — quando a app está ligada ao servidor, os dados são partilhados
entre todos os dispositivos.

## O que vais fazer com este servidor

1. **Instalar** Docker e Docker Compose (uma vez).
2. **Subir** o servidor com `docker compose`.
3. **Emitir uma chave** para cada telemóvel que quiseres ligar.
4. **Importar a chave na app** — e a app fica pronta para sincronizar (ao
   abrir, ao guardar um cliente ou no botão **Sincronizar**, no ecrã
   **Configuração**).

O passo 3 gera um único ficheiro de configuração por telemóvel. É o único
ficheiro que a app precisa para ficar ligada ao servidor.

## Guias

1. [Instalação](instalacao.md) — o que precisas de ter e como preparar o servidor
2. [Execução](execucao.md) — como subir o servidor em Docker
3. [Configuração](configuracao.md) — como ligar cada telemóvel (emitir a chave)
4. [Utilização](uso.md) — como funciona a sincronização e como resolver problemas
5. [Cópia de segurança e restauro](backup.md) — proteger e recuperar a base de dados e as fotos
6. [Nginx Proxy Manager](nginx-proxy-manager.md) — expor o servidor na internet com um domínio HTTPS
7. [Segurança e modelo de confiança](seguranca.md) — quem pode ler e alterar os dados (leitura recomendada antes de partilhar o servidor)

## Início rápido (resumo)

Para primeiro contacto, em 2 comandos:

```bash
docker compose up -d
curl http://localhost:8000/health
# → {"status":"ok","version":"...","api_version":...}
```

O servidor fica disponível em `http://localhost:8000`. Antes de conseguires
sincronizar, tens de emitir a chave do telemóvel — vai a
[Configuração](configuracao.md).

> **Referência técnica:** a lista completa de variáveis de ambiente e a descrição
> da API HTTP estão na parte final de [Configuração](configuracao.md) e de
> [Utilização](uso.md), para consulta.
