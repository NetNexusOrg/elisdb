## 1. Normalização e parsing

- [x] 1.1 Usar em `migrar_memento.py` a mesma normalização de nomes do servidor (`trim`, colapsar espaços, maiúsculas)
- [x] 1.2 Não gerar cliente para linhas/grupos cujo campo de coordenadas é um erro de parsing; continuar a reportar o erro
- [x] 1.3 Guardar as notas originais e os contratos dos casos TIPO A/B para permitir revisão e aviso de contratos diferentes
- [x] 1.4 Atualizar os testes de `migrar_memento.py`
- [x] 1.5 Adicionar um safeguard que valida o cabeçalho, os limites de tamanho e as coordenadas do CSV antes de contactar o servidor

## 2. Importador e configuração

- [x] 2.1 Criar `importar_memento.py` com caminhos fixos e sem flags
- [x] 2.2 Ler e validar `memento-import.config.json`; aplicar permissões `600` e nunca imprimir a private seed
- [x] 2.3 Criar a chave dedicada via Docker na primeira execução local e mostrar instruções de setup único quando não for possível
- [x] 2.4 Implementar pedidos assinados ed25519 para `/api/time`, `/api/clients` e `/api/push`
- [x] 2.5 Criar o wrapper executável `elisdb-memento/importar`

## 3. Comparação e revisão

- [x] 3.1 Ignorar clientes existentes por nome normalizado; nunca atualizar nem apagar
- [x] 3.2 Detetar possíveis duplicados/renomeações por nome sem acentos/pontuação e por semelhança com o mesmo contrato
- [x] 3.3 Implementar revisão opcional de TIPO A/B e escolha de coordenadas
- [x] 3.4 Implementar revisão de possíveis duplicados, com omissão por defeito
- [x] 3.5 Implementar confirmação 1 a 1 com `s`, `n`, `v`, `f` e `q`

## 4. Envio e relatório

- [x] 4.1 Enviar apenas os confirmados, em lotes de 200, com `updated_at` da hora do servidor
- [x] 4.2 Tratar `accepted`, `duplicate`, `stale` e `error`; devolver exit code não-zero em erros
- [x] 4.3 Escrever relatório de pré-visualização e final, incluindo decisões por cliente
- [x] 4.4 Ler o ficheiro opcional de exclusões
- [x] 4.5 Atualizar `.gitignore` para o relatório e as exclusões

## 5. Testes e documentação

- [x] 5.1 Criar testes unitários do importador com HTTP/Docker mockados
- [x] 5.2 Correr `pytest` em `elisdb-memento/` e confirmar que passa
- [x] 5.3 Fazer um teste de integração real com servidor local, chave ed25519 e CSV pequeno
- [x] 5.4 Criar `elisdb-memento/README.md` com o fluxo operacional, limitações e segurança
- [x] 5.5 Atualizar `elisdb-memento/CHANGELOG.md`
- [x] 5.6 Validar a change com `openspec validate importar-clientes-memento`
