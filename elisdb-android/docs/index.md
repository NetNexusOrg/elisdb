# Documentação da app ElisDB

A app **ElisDB** é um caderno de clientes para o telemóvel: cria e edita
clientes, tira fotos, marca localizações no mapa — e **sincroniza** tudo com o
servidor ElisDB (no teu servidor/computador), para os dados ficarem acessíveis
do mesmo telemóvel ou de vários.

Funciona **mesmo sem servidor** (offline): podes usar a app sozinho; a
sincronização só lida com o servidor quando ele existir e estiver configurado.

## Para começares a usar

1. **Instala a app** no telemóvel ([Instalação](instalacao.md) /
   [Execução](execucao.md)).
2. **Prepara o servidor** (instala, corre e emite a chave do telemóvel — guias
   em `../elisdb-server/docs/`).
3. **Na app:** abra **Definições** → **Importar configuração do servidor** e
   escolhe o ficheiro `<nome>.config.json` emitido no servidor ([Configuração](configuracao.md)).
4. **Sincroniza**: volta à lista de clientes e carrega no botão de sincronizar.
   Deve aparecer "Sincronizado".

Este fluxo está explicado passo a passo em [Utilização](uso.md).

## Guias

1. [Instalação](instalacao.md) — o que precisas no computador para construir a app
2. [Execução](execucao.md) — construir e instalar a app no telemóvel
3. [Configuração](configuracao.md) — quantidade de dados, assinatura e ligação ao servidor
4. [Utilização](uso.md) — ligar a app ao servidor e usar no dia a dia

> **Referência técnica:** a stack (Kotlin, Compose, Room, Ktor, etc.) e os
> detalhes de build estão descritos nas secções finais de
> [Configuração](configuracao.md) e [Execução](execucao.md), para consulta.

## Referência cruzada

O servidor tem a sua própria documentação em `../elisdb-server/docs/`
(instalação, execução, emissão de chaves e API).