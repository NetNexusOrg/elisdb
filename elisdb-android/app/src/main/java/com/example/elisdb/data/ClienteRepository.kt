package com.example.elisdb.data

import android.net.Uri
import com.example.elisdb.data.local.CatalogoItem
import com.example.elisdb.data.local.CategoriaCatalogo
import com.example.elisdb.data.local.Cliente
import com.example.elisdb.data.local.ClienteComFotos
import com.example.elisdb.data.local.ClienteServicoComItem
import com.example.elisdb.data.local.Foto
import com.example.elisdb.sync.SyncFault
import com.example.elisdb.sync.SyncStatus
import java.io.File
import kotlinx.coroutines.flow.Flow

interface ClienteRepository {
  val clientes: Flow<List<Cliente>>

  val clientesComFotos: Flow<List<ClienteComFotos>>

  val syncStatus: Flow<SyncStatus>

  val syncFault: Flow<SyncFault?>

  fun observeCliente(id: String): Flow<ClienteComFotos?>

  suspend fun getCliente(id: String): ClienteComFotos?

  suspend fun stagePhoto(source: Uri): Foto

  fun createPhotoFile(): File

  suspend fun deletePhotoFile(foto: Foto)

  suspend fun save(draft: ClienteDraft): String

  /** Catalog models, including inactive ones, in display order. */
  val catalogo: Flow<List<CatalogoItem>>

  fun observeServicos(clienteId: String): Flow<List<ClienteServicoComItem>>

  suspend fun getCatalogo(): List<CatalogoItem>

  suspend fun getServicos(clienteId: String): List<ClienteServicoComItem>

  /** Creates or renames a catalog item; throws [CatalogoNomeDuplicadoException]. */
  suspend fun saveCatalogoItem(
    id: String?,
    nome: String,
    categoria: CategoriaCatalogo,
  ): String

  suspend fun setCatalogoAtivo(id: String, ativo: Boolean)

  suspend fun deleteCliente(id: String)

  suspend fun syncNow(): SyncStatus
}