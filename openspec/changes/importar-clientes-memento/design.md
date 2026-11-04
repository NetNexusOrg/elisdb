## Context

O export do Memento é um CSV com 507 linhas e 498 clientes após colapsar nomes
repetidos. O servidor ElisDB já resolve duplicados por nome normalizado, aplica
last-write-wins e atribui `delivery_ts`; por isso a importação deve usar a API
em vez de escrever diretamente no SQLite. O `PDL` do Memento é o `contrato` da
ElisDB e não é único (há contratos repetidos em dezenas de clientes), pelo que a
chave de comparação é sempre o nome normalizado.

## Decisions

### Safeguard do CSV antes de qualquer contacto
O CSV é lido e validado antes de carregar/criar a chave ou de falar com o
servidor. O safeguard exige as colunas `Nome`, `PDL`, `Coordenadas` e `Notas`,
aplica os limites do servidor (nome até 200 caracteres normalizados; contrato e
notas até 100 000) e valida as coordenadas (vazias permitidas, pares completos,
numéricos, latitude -90..90, longitude -180..180). Erros estruturais abortam
com exit code 1 sem contactar o servidor; coordenadas inválidas numa linha são
reportadas e essa linha é saltada, permitindo importar as restantes.

### API em vez de acesso direto à base de dados
A importação usa pedidos assinados com ed25519 para `/api/time`,
`/api/clients` e `/api/push`. Assim o servidor mantém a autoridade sobre
duplicados, timestamps, auditoria e `delivery_ts`, e a app Android recebe os
clientes no próximo sync sem qualquer alteração.

### Importação apenas aditiva
Clientes cujo nome normalizado já existe no servidor são ignorados, mesmo que o
contrato ou as coordenadas sejam diferentes. Nunca há update nem delete. Isto
torna a reexecução idempotente e evita que um export antigo do Memento
sobrescreva edições feitas na ElisDB.

### Hora do servidor
`updated_at` é preenchido com a hora obtida de `/api/time`, não com o relógio
local. O servidor continua a atribuir `delivery_ts`; a hora do servidor evita
rejeições por desvio de relógio e mantém o comportamento de LWW previsível.

### Confirmação 1 a 1
Depois do resumo e da revisão de casos especiais, cada cliente a importar é
mostrado individualmente. `s` confirma, `n`/Enter salta, `v` mostra detalhes,
`f` termina a revisão e envia os já confirmados, `q` aborta tudo. Não há uma
segunda pergunta "Aplicar todos?". O envio é feito em lotes de 200 para não
gerar um pedido por cliente.

### Configuração dedicada e secreta
O config tem `server_url`, `private_seed` e `user_name`. É criado com permissões
`600` e nunca é impresso. Na primeira execução local o script tenta criar a
chave `memento-import` via `docker compose exec`; se não conseguir, mostra o
comando único de setup para o operador. Para um servidor remoto, o config é
criado no servidor e copiado para `elisdb-memento/memento-import.config.json`.

### Duplicados e renomeações
Além da igualdade exata de nome normalizado, o script assinala nomes iguais
ignorando acentos/pontuação e nomes com semelhança `>= 0.92` quando o contrato
é igual e não vazio. Por omissão, esses casos são saltados; o operador pode
rever um a um e incluir explicitamente. Isto reduz o risco de renomeações no
Memento criarem clientes duplicados na ElisDB.

### TIPO A/B
TIPO A (vários pares no mesmo campo) usa o primeiro par e guarda os restantes
nas notas; TIPO B (nome repetido) colapsa num cliente e junta as notas. O
operador pode rever estes casos e escolher outro par. Se os contratos do TIPO B
diferirem, o script avisa e mantém o da primeira linha.

### Erros de parsing
Linhas cujo campo de coordenadas tem um número ímpar de valores são reportadas
e não geram cliente, alinhado com a spec de migração. Coordenadas vazias são
válidas: o cliente é importado com `latitude`/`longitude` NULL.

### Ficheiros locais
Caminhos fixos, sem flags: `ELIS.csv`, `memento-import.config.json`,
`memento-import-report.txt` e `memento-import-exclude.txt`. O relatório e o
ficheiro de exclusões contêm nomes de clientes e são ignorados pelo Git. O CSV
e o config já estavam ignorados.

## Risks / Trade-offs

- **Sem deteção de eliminações:** o CSV não tem tombstones. Um cliente apagado
  na ElisDB mas presente no Memento volta a ser criado. O ficheiro de exclusões
  é a salvaguarda manual.
- **Renomeações:** sem ID estável, uma renomeação no Memento parece um cliente
  novo; a heurística de semelhança apenas avisa.
- **Fotos e Rota:** não são importadas nesta fase; os URLs de fotos do Memento
  não são transferidos.
- **Confirmação longa:** com centenas de novos clientes, a revisão 1 a 1 é
  demorada; `f` permite terminá-la a qualquer momento.
- **Docker na primeira execução:** se o servidor for remoto ou o Docker não
  estiver disponível, é necessário o setup manual único do config.
