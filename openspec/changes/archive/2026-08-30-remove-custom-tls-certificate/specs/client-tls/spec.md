## REMOVED Requirements

### Requirement: User-configured trust anchor

**Reason**: A funcionalidade de importar e confiar num certificado/CA HTTPS auto-hospedado deixa de interessar; a app passa a validar o certificado do servidor apenas contra o armazenamento de confiança do sistema.

**Migration**: Nenhuma. Qualquer certificado anteriormente importado deixa de ser usado; as ligações HTTPS passam a usar a validação padrão do sistema.

### Requirement: Trust anchor scoped to the configured host

**Reason**: A capability de confiança customizada é removida na íntegra.

**Migration**: Nenhuma.
