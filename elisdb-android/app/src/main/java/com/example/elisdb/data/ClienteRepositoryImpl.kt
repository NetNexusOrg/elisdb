package com.example.elisdb.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.example.elisdb.data.local.AppDatabase
import com.example.elisdb.data.local.Cliente
import com.example.elisdb.data.local.ClienteComFotos
import com.example.elisdb.data.local.ClienteDao
import com.example.elisdb.data.local.Foto
import com.example.elisdb.data.local.FotoDao
import com.example.elisdb.sync.SyncFault
import com.example.elisdb.sync.SyncManager
import com.example.elisdb.sync.SyncStatus
import java.io.File
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

data class ClienteDraft(
  val id: String? = null,
  val nome: String,
  val contrato: String? = null,
  val tag: String? = null,
  val latitude: Double? = null,
  val longitude: Double? = null,
  val notas: String? = null,
  val ativo: Boolean = true,
  val fotos: List<Foto> = emptyList(),
)

class ClienteRepositoryImpl(
  private val context: Context,
  private val database: AppDatabase,
  private val clienteDao: ClienteDao,
  private val fotoDao: FotoDao,
  private val syncManager: SyncManager? = null,
  private val clockOffsetStore: com.example.elisdb.sync.ClockOffsetStore? = null,
) : ClienteRepository {

  /** Device clock (epoch seconds) shifted by the last-known server offset, when available. */
  private fun nowEpochSeconds(): Long =
    System.currentTimeMillis() / 1000 + (clockOffsetStore?.getOffsetSeconds() ?: 0L)
  private val photosDir: File
    get() = File(context.filesDir, "photos")

  private val photoEncoder = PhotoEncoder(context)

  override val clientes: Flow<List<Cliente>> = clienteDao.observeAll()

  override val clientesComFotos: Flow<List<ClienteComFotos>> =
    clienteDao.observeAllComFotos().map { list -> list.map { it.withOrderedPhotos() } }

  override val syncStatus: Flow<SyncStatus> =
    syncManager?.status ?: kotlinx.coroutines.flow.flowOf(SyncStatus.IDLE)

  override val syncFault: Flow<SyncFault?> =
    syncManager?.fault ?: kotlinx.coroutines.flow.flowOf(null)

  override fun observeCliente(id: String): Flow<ClienteComFotos?> =
    clienteDao.observeClienteComFotos(id).map { it?.withOrderedPhotos() }

  override suspend fun getCliente(id: String): ClienteComFotos? =
    clienteDao.getClienteComFotos(id)?.withOrderedPhotos()

  override suspend fun stagePhoto(source: Uri): Foto = withContext(Dispatchers.IO) {
    val dest = photoEncoder.encodeToWebp(source, photosDir)
    Foto(id = "", clienteId = "", uriLocal = dest.absolutePath)
  }

  override fun createPhotoFile(): File {
    photosDir.mkdirs()
    return File(photosDir, "foto_${UUID.randomUUID()}.jpg")
  }

  override suspend fun deletePhotoFile(foto: Foto) {
    withContext(Dispatchers.IO) { runCatching { File(foto.uriLocal).delete() } }
  }

  override suspend fun save(draft: ClienteDraft): String {
    val now = nowEpochSeconds()
    val removedFiles = mutableListOf<Foto>()
    val clienteId =
      database.withTransaction {
        val clienteId =
          if (draft.id == null) {
            val newId = UUID.randomUUID().toString()
            clienteDao.insert(
              Cliente(
                id = newId,
                nome = draft.nome.trim().uppercase(Locale.ROOT),
                contrato = draft.contrato?.takeIf { it.isNotBlank() },
                tag = draft.tag?.trim()?.uppercase(Locale.ROOT)?.takeIf { it.isNotBlank() },
                latitude = draft.latitude,
                longitude = draft.longitude,
                notas = draft.notas?.takeIf { it.isNotBlank() },
                ativo = draft.ativo,
                dataCriacao = now,
                updatedAt = now,
                createdAt = now,
                pending = true,
                revision = 1,
              )
            )
            newId
          } else {
            val existing =
              clienteDao.getById(draft.id) ?: throw ClienteRemovidoException(draft.id)
            clienteDao.update(
              existing.copy(
                nome = draft.nome.trim().uppercase(Locale.ROOT),
                contrato = draft.contrato?.takeIf { it.isNotBlank() },
                tag = draft.tag?.trim()?.uppercase(Locale.ROOT)?.takeIf { it.isNotBlank() },
                latitude = draft.latitude,
                longitude = draft.longitude,
                notas = draft.notas?.takeIf { it.isNotBlank() },
                ativo = draft.ativo,
                updatedAt = now,
                pending = true,
                revision = existing.revision + 1,
              )
            )
            draft.id
          }

        val keptIds = draft.fotos.filter { it.id.isNotEmpty() }.map { it.id }.toSet()
        val existingFotos = fotoDao.getByCliente(clienteId)
        val removed = existingFotos.filter { it.id !in keptIds }
        removedFiles += removed
        if (removed.isNotEmpty()) fotoDao.deleteByIds(removed.map { it.id })
        draft.fotos
          .forEachIndexed { index, foto ->
            fotoDao.insert(
              foto.copy(
                id = foto.id.ifEmpty { UUID.randomUUID().toString() },
                clienteId = clienteId,
                ordem = index,
              )
            )
          }
        clienteId
      }
    // Removed photo files are deleted only after the transaction commits, so a
    // rollback cannot leave rows pointing at deleted files.
    removedFiles.forEach { deleteFile(it) }
    return clienteId
  }

  override suspend fun deleteCliente(id: String) {
    clienteDao.getById(id)?.let { existing ->
      clienteDao.update(existing.copy(deletePending = true, revision = existing.revision + 1))
    }
  }

  override suspend fun syncNow(): SyncStatus =
    syncManager?.syncNow() ?: SyncStatus.IDLE

  private fun deleteFile(foto: Foto) {
    runCatching { File(foto.uriLocal).delete() }
  }

  /**
   * Room's `@Relation` query has no `ORDER BY`, so the photo list comes back in
   * rowid order. Sort it here once, so the list thumbnail, the detail grid and
   * the viewer all show the order the user chose when saving.
   */
  private fun ClienteComFotos.withOrderedPhotos(): ClienteComFotos =
    copy(fotos = fotos.sortedWith(compareBy({ it.ordem }, { it.id })))
}

/**
 * The client being saved no longer exists locally: a pull removed it while the
 * form was open. Reported to the user instead of pretending the save worked.
 */
class ClienteRemovidoException(val clienteId: String) :
  Exception("client $clienteId was removed by a sync")