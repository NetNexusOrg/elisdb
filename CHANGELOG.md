# Histórico de versões

Notas da página de Releases do GitHub. Cada tag `vX.Y.Z` usa a secção
`## [X.Y.Z]` deste ficheiro — o trabalho `release` do `android-release.yml`
extrai-a e publica-a como descrição do release. Uma tag sem secção falha no CI
antes de gerar o APK, para não sair uma versão sem notas.

Escreve para quem usa a app (ou instala o servidor), agrupado em **Novidades**
e **Correções**. Não incluir secrets, dados de clientes nem fotos.

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
