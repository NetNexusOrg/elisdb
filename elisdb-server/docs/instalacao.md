# Instalação do servidor

Aqui está como preparar o servidor no teu computador. Depois de fazer isto uma
vez, segue para a [Execução](execucao.md) para o correr.

## O que precisas de ter

- **Python 3.10 ou superior** instalado no computador (testado com 3.12).
- Acesso à internet, para instalar as dependências na primeira vez.

Confirma a versão do Python:

```bash
python3 --version
```

Deve mostrar algo como `Python 3.12.x`. Se o comando não existir, instala o
Python antes de continuar.

## Passo 1 — obter o código do servidor

O servidor vive na pasta `elisdb-server/` do projeto ElisDB, junto à app Android
(`elisdb-android/`). Obtém o projeto a partir do repositório:

```bash
git clone <URL-do-repositorio-do-projeto>
cd elisdb/elisdb-server
```

## Passo 2 — criar o ambiente e instalar as dependências

```bash
python3 -m venv .venv
.venv/bin/pip install -r requirements.txt
```

Isto cria uma pasta `.venv` com tudo o que o servidor precisa, sem mexer no
resto do computador. A primeira instalação pode demorar um pouco.

> Em Debian/Ubuntu, se o comando `python3 -m venv` falhar, instala o pacote
> `python3-venv` e repete o comando.

## Passo 3 — verificar que ficou tudo certo

Corre os testes incluídos no projeto:

```bash
.venv/bin/python -m pytest
```

Se todos os testes passarem (sem mensagens de erro), a instalação está
concluída.

## O que saber antes da primeira execução

- O servidor **não vem com nenhuma chave pré-registada**: até emitires a chave
  de um telemóvel, os pedidos da app são recusados. Isso é normal — é a medida
  de segurança.
- Os dados ficam guardados num único ficheiro: `elisdb.db` (na pasta onde o
  servidor corre), ou no caminho que definires na variável `ELISDB_DB`.

## Passo seguinte

[Execução](execucao.md) — como correr o servidor.