## 1. Cache de replay no servidor

- [x] 1.1 Em `app/auth.py`, adicionar um cache (TTL = janela de timestamp) de assinaturas já consumidas
- [x] 1.2 `require_auth` rejeita (401) se a assinatura já estiver no cache; caso contrário insere-a
- [x] 1.3 Garantir que a chave do cache é a assinatura completa (não apenas timestamp) para não rejeitar pedidos legítimos distintos

## 2. Testes e verificação

- [x] 2.1 Em `test_main.py`, repetir exatamente o mesmo pedido assinado dentro da janela → segundo é rejeitado (replay)
- [x] 2.2 Dois pedidos distintos dentro da janela continuam aceites
- [x] 2.3 Correr `pytest`
