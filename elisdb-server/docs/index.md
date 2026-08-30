# Documentação do servidor ElisDB

O servidor ElisDB é o programa que **guarda os teus dados** (clientes e fotos)
e os mantém em sincronia com a app Android. Quando a app está no mesmo servidor,
os dados são partilhados entre todos os dispositivos ligados.

## O que vais fazer com este servidor

1. **Instalar** o servidor no teu computador (uma vez).
2. **Correr** o servidor.
3. **Emitir uma chave** para cada telemóvel que quiseres ligar.
4. **Importar a chave na app** — e a app fica a sincronizar automaticamente.

O passo 3 gera um único ficheiro de configuração por telemóvel. É o único
ficheiro que a app precisa para ficar ligada ao servidor.

## Guias

1. [Instalação](instalacao.md) — o que precisas de ter e como preparar o servidor
2. [Execução](execucao.md) — como correr o servidor (normal ou em Docker)
3. [Configuração](configuracao.md) — como ligar cada telemóvel (emitir a chave)
4. [Utilização](uso.md) — como funciona a sincronização e como resolver problemas
5. [Nginx Proxy Manager](nginx-proxy-manager.md) — expor o servidor na internet com um domínio HTTPS

## Início rápido (resumo)

Para primeiro contacto, em 3 comandos:

```bash
python3 -m venv .venv
.venv/bin/pip install -r requirements.txt
.venv/bin/uvicorn app.main:app
```

O servidor fica disponível em `http://127.0.0.1:8000`. Antes de conseguires
sincronizar, tens de emitir a chave do telemóvel — vai a
[Configuração](configuracao.md).

> **Referência técnica:** a lista completa de variáveis de ambiente e a descrição
> da API HTTP estão na parte final de [Configuração](configuracao.md) e de
> [Utilização](uso.md), para consulta.