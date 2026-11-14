# Segurança

## O modelo, em duas frases

O ElisDB é para **um grupo de confiança**. Qualquer chave registada lê, edita e
apaga **todos** os clientes e fotos do servidor; a auditoria identifica o autor
das alterações, mas não impede nada.

Detalhe: [Segurança e modelo de confiança](docs/operar/seguranca.md).

## Reportar um problema

Abre um *security advisory* privado em
<https://github.com/NetNexusOrg/elisdb/security/advisories/new> ou contacta o
mantenedor por mensagem direta. Não abras uma issue pública com detalhes
exploráveis.

Inclui: versão (`/health` e `gradle.properties`), passos para reproduzir e o
impacto esperado.

## Fora de âmbito

- Servidores expostos sem TLS: é responsabilidade do operador — ver
  [Expor com HTTPS](docs/operar/nginx.md).
- Acesso total por parte de uma chave legítima (é o modelo, não um bug).
- Perda de dados por falta de backups — ver [Backups](docs/operar/backup.md).

## Segredos no repositório

Nunca commites `*.config.json` (têm a seed privada), `*.db`, `.env`,
`keystore.properties`, keystores ou as credenciais de release. O `.gitignore`
cobre estes casos; se encontrares um ficheiro destes no histórico, avisa em
privado.
