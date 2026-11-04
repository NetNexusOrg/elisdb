# Histórico de versões

Notas da página de Releases do GitHub. Cada tag `vX.Y.Z` usa a secção
`## [X.Y.Z]` deste ficheiro — o trabalho `release` do `android-release.yml`
extrai-a e publica-a como descrição do release. Uma tag sem secção falha no CI
antes de gerar o APK, para não sair uma versão sem notas.

Escreve para quem usa a app (ou instala o servidor), agrupado em **Novidades**
e **Correções**. Não incluir secrets, dados de clientes nem fotos.

## [1.11.0] - 2026-11-04

### Novidades

- **Tags nos clientes.** Cada cliente pode ter uma etiqueta livre, editável no formulário e apresentada em maiúsculas na lista, no detalhe e no histórico.
- **Filtrar por tag.** O menu de filtros da lista lista as tags existentes e mostra apenas os clientes com a tag escolhida.
- **Filtro por tag no mapa.** O mapa de clientes ganha o mesmo filtro de tag, independente do filtro da lista.
- **Menus de filtro expansíveis.** Os grupos Tags, Ordenação e Estado aparecem recolhidos e abrem ao tocar.

## [1.10.0] - 2026-11-03

### Novidades

- **Pins adaptativos à densidade.** Os clientes deixam de desaparecer em clusters: cada pin mantém a localização real e encolhe progressivamente até se tornar um ponto quando há sobreposição. O cliente selecionado fica sempre em destaque.
- **Mapa com o tema da app.** O mapa segue o modo claro/escuro do sistema, com o estilo Liberty em ambos os temas.
- **Filtros memorizados.** A lista de clientes e o mapa lembram o filtro e a ordenação escolhidos entre arranques da app.
- **Mapa mais limpo.** Removidos os controlos sobrepostos de logótipo e informação do MapLibre.

### Correções

- **Abertura do mapa.** Corrigido o flicker e o salto de câmara ao abrir o mapa.
- **Pins duplicados.** Clientes na mesma coordenada são separados em leque a partir do zoom de rua.

## [1.9.0] - 2026-11-02

### Novidades

- **Mapa mais detalhado.** Migração para MapLibre Native com tiles vetoriais
  OpenFreeMap, sem chave de API. Os pins, etiquetas e a localização atual
  continuam a funcionar como antes.
- **Clientes agrupados.** Clientes com pins próximos passam a formar um cluster
  com a respetiva contagem. Tocar no cluster aproxima o mapa e revela os pins
  individuais.
- **Etiquetas sem sobreposição.** Quando duas etiquetas colidem, a secundária é
  ocultada ou reposicionada, mantendo o pin sempre visível.
- **Atribuição discreta.** O logótipo e o ícone de informação nativos da MapLibre
  foram substituídos por um controlo compacto `© OpenStreetMap`, que abre os
  créditos e ligações obrigatórias.

### Correções

- **Filtro do mapa.** Alterar o filtro de clientes já não reposiciona a câmara;
  apenas os pins apresentados mudam.

## [1.8.0] - 2026-11-02

### Novidades

- **Mapa vetorial.** O mapa passou a usar MapLibre Native com tiles vetoriais do
  OpenStreetMap servidos pelo OpenFreeMap, mantendo os pins, etiquetas e a
  localização atual sem precisar de chave de API.
- **Mapa de clientes.** A lista passa a ter um atalho para um mapa com todos os
  clientes que têm localização. Os pins ativos são vermelhos, os inativos
  cinzas e cada pin mostra o nome do cliente. Tocar num pin mostra o nome, o
  contrato e o estado, com acesso direto ao detalhe.
- **Filtro do mapa.** O menu (≡) permite mostrar todos os clientes, apenas os
  ativos (predefinição) ou apenas os inativos.
- **A tua localização no mapa.** Ao abrir o mapa, a app pede acesso à localização
  e centra-o na tua posição atual, com um ponto azul. Sem permissão, o mapa
  enquadra os pins dos clientes. No mapa em ecrã inteiro do detalhe, o mapa
  continua centrado no cliente e mostra também a tua localização; afasta o zoom
  para te situares.
- **Navegação mais consistente.** Os menus da lista e do detalhe usam agora o
  ícone hambúrguer (≡). A etiqueta vermelha **Inativo** aparece depois do
  contrato, tanto na lista como na barra superior do detalhe.

### Correções

- **Centragem do mapa.** O enquadramento automático dos pins já não se sobrepõe
  à centragem na localização atual quando esta chega ao mapa.

## [1.7.0] - 2026-11-01

### Novidades

- **Credencial do utilizador.** O ficheiro importado passa a ser apresentado
  como uma credencial (URL do servidor, utilizador e chave), com textos e ação
  de remoção mais claros.
- **Configuração mais simples.** O cartão **Dados** aparece primeiro e o antigo
  cartão **Servidor** foi integrado no cartão **Sincronização**. O estado já não
  aparece duplicado e, sem credencial, só vês a explicação e o botão de
  importação.
- **Contagem do servidor antes do sync.** Ao sincronizar, a app carrega primeiro
  o total do servidor; até chegar, mostra `—`.
- **Sobre mais direto.** A verificação de atualizações é automática ao abrir o
  ecrã, o botão mantém-se sempre visível e o QR do grupo Telegram pode ser
  ampliado.
- **Formulário de cliente mais limpo.** O interruptor **Cliente ativo** é o
  primeiro campo e os textos explicativos redundantes foram removidos.
- **Mapas sem botões de zoom.** O zoom passa a ser feito apenas por pinça, tanto
  no mapa em ecrã inteiro como na escolha de localização.

### Correções

- **As fotos deixam de se perder quando o servidor não tem o ficheiro.** A app
  preserva a cópia local, avisa e volta a enviar a foto na sincronização seguinte.
- **Uma lista de fotos incompleta já não apaga fotos locais.** Se o servidor
  devolver uma lista incompleta, a app mantém as fotos que já tinha.
- **Sincronizações grandes são feitas por páginas**, em vez de carregar todos os
  registos de uma vez.
- **Depois de um restauro do servidor**, a app deteta que os dados do servidor
  estão atrás dos locais e faz uma sincronização completa, em vez de assumir que
  já está tudo em dia.
- **Uma edição rejeitada como antiga volta a buscar a versão do servidor**, para
  não ficar indefinidamente com dados desatualizados.
- **Fotos tiradas com o telemóvel rodado ficam com a orientação correta.**

## [1.6.2] - 2026-10-31

### Novidades

- **Lista por ordem à escolha.** No menu de controlos da pesquisa podes agora
  ordenar os clientes por **Ordem alfabética** (predefinição), por **Mais
  recentes primeiro** ou por **Última edição primeiro**, sem perder o filtro por
  estado nem a pesquisa.
- **Configuração mais clara.** O ecrã passou a mostrar o estado da sincronização
  como uma etiqueta no cartão, a última sincronização com data e hora e, nos
  dados, se a app e o servidor estão em sincronia. Sem servidor configurado, a
  importação do ficheiro aparece logo como a primeira ação, em vez de repetir
  avisos.
- **Ecrã Sobre renovado.** A informação da app aparece agora como um cabeçalho
  com o ícone, o nome e a versão, em vez de um cartão com uma descrição longa, e
  o ecrã inclui o QR code do grupo de Telegram da app.
- **Abrir a app sincroniza.** Ao arrancar, a app faz uma sincronização em segundo
  plano (uma vez por arranque e apenas se houver servidor configurado), para
  receber novidades de outros dispositivos e enviar o que ficou pendente. Sem
  configuração nada é tentado e não aparece erro.
- **Guardar sincroniza logo.** Ao guardar um cliente novo ou uma edição, a app
  começa a sincronizar em segundo plano, sem teres de abrir a Configuração. A
  navegação continua imediata e, se não houver rede, o cliente fica guardado e
  pendente como antes.
- **Clientes ativos e inativos.** Cada cliente tem agora um interruptor
  **Cliente ativo** no formulário. Os inativos continuam guardados e
  sincronizados, mas aparecem com a etiqueta **Inativo** na lista e no detalhe,
  para distinguir quem já não está em serviço sem apagar o histórico. A lista
  pode ser filtrada por estado através do ícone de controlos na caixa de
  pesquisa. O servidor guarda o estado e regista a ativação/desativação no
  histórico de alterações.

## [1.6.1] - 2026-10-26

### Novidades

- **Aviso quando um registo muda durante a sincronização.** Se editares um
  cliente enquanto a sincronização está a decorrer, a app mantém a alteração
  pendente e avisa que vai ser reenviada, em vez de a substituir pela versão
  antiga.

### Correções

- **Clientes eliminados noutro telemóvel desaparecem de facto.** As eliminações
  passam a ser entregues segundo o relógio do servidor, por isso um apagar feito
  num telemóvel com o relógio atrasado já chega aos restantes.
- **Edições de telemóveis com o relógio atrasado deixam de ser rejeitadas** como
  desatualizadas e passam a ser guardadas com o seu próprio horário.
- **Atualizar a app em Android 7 a 13 já não falha ao abrir.** A migração da base
  de dados deixou de depender de uma instrução de SQLite que só existe em
  Android 14.
- **Fotos que o servidor não tem voltam a ser enviadas**, em vez de ficarem
  dadas como sincronizadas.
- **Servidor: atualizar uma base de dados antiga já não apaga as fotos**, e a
  limpeza de ficheiros órfãos deixou de poder apagar fotos válidas quando a
  pasta está configurada com um caminho relativo.

## [1.6.0] - 2026-10-26

### Novidades

- **Atualizar a app pelo próprio ecrã Sobre.** "Procurar atualizações" pergunta
  ao servidor qual é a versão publicada; se for mais recente, "Transferir"
  descarrega o pacote com progresso e "Instalar" abre o instalador do Android.
  O ficheiro é verificado (tamanho e SHA-256) antes de ser oferecido, e um
  download corrompido é apagado em vez de instalado.
- **O servidor passa a publicar a versão atual.** Os endpoints
  `/api/app/latest` e `/api/app/download` são autenticados como o resto da API;
  basta colocar o APK na pasta de atualizações e o pedido seguinte já o
  encontra — sem reiniciar o servidor.
- **Publicação automática.** Criar a tag `v*` assina o APK, anexa-o a esta
  página de Releases e copia-o para o servidor, sem passos manuais.
- **Histórico do cliente em página própria**, com os campos e as fotos que cada
  alteração mudou.
- **Sincronização com o relógio do servidor**, para que a hora dos registos
  coincida entre dispositivos.

### Correções

- Registos com data anterior ao cursor de sincronização passam a ser entregues
  aos outros dispositivos em vez de ficarem retidos.
- O release deixa de falhar quando o armazenamento de artefactos do GitHub está
  cheio: o upload de conveniência é best effort — o que conta é o APK anexado
  ao release e a cópia no servidor.
