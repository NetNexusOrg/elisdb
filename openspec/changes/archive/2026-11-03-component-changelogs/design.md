# Design

## Context

O repositório está organizado por componente, mas só `elisdb-android/` tem um
`CHANGELOG.md`. Esse ficheiro é específico da app: o job `release` de
`.github/workflows/android-release.yml` extrai dele a secção `## [X.Y.Z]` para
publicar as notas de uma tag `v*`. O servidor tem a versão em
`elisdb-server/VERSION`, o bot Telegram é um componente separado e a migração
Memento é um conjunto de scripts autónomo — nenhum deles tinha um registo de
mudanças próprio.

## Decisions

### Um changelog na raiz de cada componente

Cada componente passa a ter `CHANGELOG.md` na sua própria raiz. Isto segue o
layout já fixado em `repository-layout`: ficheiros específicos de um componente
vivem dentro desse componente, e a raiz do repositório só contém documentação
transversal, CI e tooling. Não é criado um changelog na raiz.

### Conteúdo inicial honesto

Os três changelogs novos começam em `## [Unreleased]` com uma nota de changelog
inicial. Não são inventadas versões nem entradas de release que não existam. O
do servidor aponta para `elisdb-server/VERSION` como fonte da versão de release,
sem duplicar o número.

### O changelog Android fica inalterado

`elisdb-android/CHANGELOG.md` continua a ser a fonte das notas de release
Android e não é tocado. O requisito novo apenas torna explícito que os
changelogs dos outros componentes não alimentam o workflow de release.

## Risks / trade-offs

- Changelogs por componente podem ficar desatualizados. Mitigação: a spec
  exige a existência do ficheiro; mantê-lo atualizado é responsabilidade de
  quem altera o componente, tal como já acontece na app.
- Duplicar a versão do servidor no changelog criaria divergência; por isso o
  changelog remete para `elisdb-server/VERSION`.

## Alternatives considered

- **Um changelog único na raiz**: contraria o layout por componente e mistura
  históricos com ritmos e públicos diferentes.
- **Reutilizar `elisdb-android/CHANGELOG.md`**: acoplaria as notas de release
  Android a mudanças de servidor, bot ou migração, e o workflow deixaria de ter
  uma fonte limpa.
- **Não documentar a regra na spec**: o layout ficaria apenas implícito e
  voltaria a divergir com o tempo.
