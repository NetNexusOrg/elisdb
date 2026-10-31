package com.example.elisdb.data

import android.net.Uri
import com.example.elisdb.data.local.Cliente
import com.example.elisdb.data.local.ClienteComFotos
import com.example.elisdb.data.local.Documento
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

  suspend fun stageDocument(source: Uri, nome: String): Documento

  fun createPhotoFile(): File

  suspend fun deletePhotoFile(foto: Foto)

  suspend fun deleteDocumentoFile(documento: Documento)

  suspend fun save(draft: ClienteDraft): String

  suspend fun deleteCliente(id: String)

  suspend fun syncNow(): SyncStatus
}