# Histórico de versões

Notas da página de Releases do GitHub. Cada tag `vX.Y.Z` usa a secção
`## [X.Y.Z]` deste ficheiro — o trabalho `release` do `android-release.yml`
extrai-a e publica-a como descrição do release. Uma tag sem secção falha no CI
antes de gerar o APK, para não sair uma versão sem notas.

Escreve para quem usa a app (ou instala o servidor), agrupado em **Novidades**
e **Correções**. Não incluir secrets, dados de clientes nem fotos.

## [Não publicado]

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
