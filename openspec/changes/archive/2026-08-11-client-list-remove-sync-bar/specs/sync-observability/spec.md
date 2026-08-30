## REMOVED Requirements

### Requirement: Sync observable from the client list

**Reason**: O estado de sincronização e o gatilho manual deixam de pertencer à lista
de clientes; a observabilidade da sincronização passa a residir exclusivamente nas
Definições, deixando a lista de clientes dedicada à gestão de clientes.

**Migration**: Utilizadores que antes viam o estado de sincronização ou disparavam a
sincronização a partir da lista devem usar a ecrã de Definições para consultar o
estado e acionar a sincronização.