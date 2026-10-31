# Proposal

## Why

O ecrã “Sobre” repete a informação em cartões de rótulo/valor e inclui uma descrição
genérica que não acrescenta nada a quem já usa a app. A apresentação fica pesada e
pouco apelativa para um ecrã institucional.

## What Changes

- Remover a descrição da aplicação.
- Substituir o cartão “A aplicação” por um cabeçalho centralizado com o ícone da
  app, o nome e a versão instalada.
- Separar o cabeçalho dos restantes cartões com um divisor subtil.
- Remover o cartão da equipa/desenvolvedor: os contactos foram considerados
  informação desnecessária no ecrã por agora.
- Alinhar a spec `client-about` (deixa de exigir a descrição e a informação do
  desenvolvedor).

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

- `client-about`: o ecrã deixa de mostrar a descrição e a informação do
  desenvolvedor; passa a apresentar um cabeçalho com ícone, nome e versão e o
  cartão de atualizações.

## Impact

- Apenas UI Android: `ui/about/AboutScreen.kt` e o teste instrumentado do ecrã.
- Sem alterações no servidor, base de dados ou sincronização.
