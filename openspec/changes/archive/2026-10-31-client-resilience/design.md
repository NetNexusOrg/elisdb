# Design

## Context

O sync é manual e concorrente com a edição: o utilizador pode gravar um cliente enquanto um push está a caminho do servidor. O ack não pode, por isso, escrever o conteúdo que foi enviado — só pode confirmar que **aquilo que foi enviado** ainda é a versão atual. Depois de `sync-integrity` a comparação é feita por `updatedAt`, um inteiro em segundos.

## Decisions

### Revisão local em vez de timestamp

`updatedAt` vem do relógio do dispositivo e é ajustado pelo offset do servidor; dois saves no mesmo segundo produzem o mesmo valor, e um ack atrasado limpa `pending` de uma versão que nunca foi enviada. Uma coluna `revision INTEGER NOT NULL DEFAULT 0` incrementada em cada gravação local é monotónica por definição e independente de relógios.

O ack passa a ser `UPDATE clientes SET pending = 0 WHERE id = :id AND revision = :seen`; zero linhas significa "mudou durante o sync" e o registo fica pendente, com aviso na UI. O repositório também incrementa `revision` quando altera `deletePending`, para que apagar durante o sync não seja confundido com o snapshot anterior.

### O watermark não pode ser refém de um registo

O clamp de `sync-integrity` (`min(saltado) - 1`) protege contra saltar a versão do servidor, mas um registo que nunca consegue ser enviado — nome inválido, `status = error` persistente — ficaria a segurar o watermark para sempre, e cada pull voltaria a trazer todos os registos posteriores.

O limite é o tempo: só seguram o watermark os registos pendentes cuja última alteração é recente (7 dias). Passado esse prazo assume-se que o registo está avariado; a sincronização segue, o registo continua pendente e é tentado de novo, e a UI já mostra o aviso de falha. Não é preciso coluna nova: `updatedAt` do registo pendente é o instante em que a edição ficou à espera.

### Reset explícito ao mudar de servidor

`importKey`/`clearConfig` passam a limpar `sync_meta` (watermark), os hashes de fotos (`PhotoPushState`), o offset de relógio e o último estado de sync. Sem isto, um servidor novo com timestamps menores que o watermark antigo nunca devolve nada, e o cliente ainda empurra para lá os dados do servidor anterior. Os registos locais **não** são apagados: o full pull seguinte reconcilia-os (os que não existirem no servidor novo desaparecem, os que existirem são adotados).

### Migração do seed: cifrado primeiro

A ordem atual (`clear()` do texto simples e só depois `EncryptedSharedPreferences.create`) perde a chave se o Keystore falhar. Passa a: criar a loja cifrada → gravar os valores → confirmar → limpar o texto simples; qualquer falha mantém o texto simples e é registada, sem propagar a exceção para `Application.onCreate`.

### Timeouts por pedido

O `HttpTimeout` global (30 s) é razoável para JSON e absurdo para um APK de 20 MB. O download da release usa `timeout { requestTimeoutMillis = 10 min; socketTimeoutMillis = 30 s }`, o envio e a descarga de fotos 2 min. O resto mantém-se.

## Risks / trade-offs

- **Registo avariado fica invisível**: depois de 7 dias o watermark passa-lhe à frente, portanto a versão do servidor nunca é aplicada localmente enquanto ele estiver pendente. É o preço de não paralisar a sincronização, e o aviso de erro continua visível.
- **Reset ao mudar de servidor**: quem troca de servidor por engano e volta atrás paga um full pull (tráfego), mas não perde dados.
- **`revision` é local**: o servidor não a conhece, pelo que um pull que aplica um registo do servidor põe `pending = false` diretamente (não há nada pendente a confirmar).

## Alternatives considered

- **Hash do conteúdo no ack**: funciona, mas obriga a ler todas as colunas e comparar strings em SQL; a revisão é mais barata e igualmente correta.
- **Contador de tentativas por registo**: mais preciso para o watermark, mas exige coluna nova, migração e reset; o limite temporal resolve o mesmo problema com uma regra que se explica numa frase.
- **Apagar os registos locais ao mudar de servidor**: garantia mais forte, mas destrutiva e desnecessária — o full pull já reconcilia.
