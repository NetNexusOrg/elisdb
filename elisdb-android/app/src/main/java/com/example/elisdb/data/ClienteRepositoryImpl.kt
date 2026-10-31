package com.example.elisdb.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.room.withTransaction
import com.example.elisdb.data.local.AppDatabase
import com.example.elisdb.data.local.Cliente
import com.example.elisdb.data.local.ClienteComFotos
import com.example.elisdb.data.local.ClienteDao
import com.example.elisdb.data.local.Documento
import com.example.elisdb.data.local.DocumentoDao
import com.example.elisdb.data.local.Foto
import com.example.elisdb.data.local.FotoDao
import com.example.elisdb.sync.SyncFault
import com.example.elisdb.sync.SyncManager
import com.example.elisdb.sync.SyncStatus
import java.io.File
import java.io.FileOutputStream
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
  val latitude: Double? = null,
  val longitude: Double? = null,
  val notas: String? = null,
  val fotos: List<Foto> = emptyList(),
  val documentos: List<Documento> = emptyList(),
)

class ClienteRepositoryImpl(
  private val context: Context,
  private val database: AppDatabase,
  private val clienteDao: ClienteDao,
  private val fotoDao: FotoDao,
  private val syncManager: SyncManager? = null,
  private val clockOffsetStore: com.example.elisdb.sync.ClockOffsetStore? = null,
  private val documentoDao: DocumentoDao = database.documentoDao(),
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

  /**
   * Stages a device-map attachment. Images go through [PhotoEncoder] like the
   * client's photos; a PDF is copied byte for byte (re-encoding it would only
   * corrupt it). The file is kept only when it is an accepted type and fits the
   * server's size ceiling, so the form can tell the user why it was refused.
   */
  override suspend fun stageDocument(source: Uri, nome: String): Documento =
    withContext(Dispatchers.IO) {
      val displayName = nome.takeIf { it.isNotBlank() } ?: displayName(source)
      val sourceMime = resolveMime(source, displayName)
      if (!isAcceptedDocumentMime(sourceMime)) {
        throw AttachmentRejectedException(AttachmentRejection.UNSUPPORTED_TYPE)
      }
      val declaredSize = declaredSize(source)
      if (declaredSize != null && declaredSize > MAX_ATTACHMENT_BYTES) {
        throw AttachmentRejectedException(AttachmentRejection.TOO_LARGE)
      }
      val isPdf = sourceMime == "application/pdf"
      val dest = if (isPdf) copyPdf(source) else photoEncoder.encodeToWebp(source, photosDir)
      if (dest.length() > MAX_ATTACHMENT_BYTES) {
        dest.delete()
        throw AttachmentRejectedException(AttachmentRejection.TOO_LARGE)
      }
      Documento(
        id = "",
        clienteId = "",
        nome = displayName ?: if (isPdf) "Documento.pdf" else "Imagem",
        mime = if (isPdf) "application/pdf" else "image/webp",
        uriLocal = dest.absolutePath,
      )
    }

  override suspend fun deleteDocumentoFile(documento: Documento) {
    withContext(Dispatchers.IO) { runCatching { File(documento.uriLocal).delete() } }
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
    val removedFiles = mutableListOf<String>()
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
                latitude = draft.latitude,
                longitude = draft.longitude,
                notas = draft.notas?.takeIf { it.isNotBlank() },
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
                latitude = draft.latitude,
                longitude = draft.longitude,
                notas = draft.notas?.takeIf { it.isNotBlank() },
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
        removedFiles += removed.map { it.uriLocal }
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

        val keptDocumentIds = draft.documentos.filter { it.id.isNotEmpty() }.map { it.id }.toSet()
        val existingDocumentos = documentoDao.getByCliente(clienteId)
        val removedDocumentos = existingDocumentos.filter { it.id !in keptDocumentIds }
        removedFiles += removedDocumentos.map { it.uriLocal }
        if (removedDocumentos.isNotEmpty()) documentoDao.deleteByIds(removedDocumentos.map { it.id })
        draft.documentos
          .forEachIndexed { index, documento ->
            documentoDao.insert(
              documento.copy(
                id = documento.id.ifEmpty { UUID.randomUUID().toString() },
                clienteId = clienteId,
                ordem = index,
              )
            )
          }
        clienteId
      }
    // Removed attachment files are deleted only after the transaction commits, so
    // a rollback cannot leave rows pointing at deleted files.
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

  private fun deleteFile(path: String) {
    runCatching { File(path).delete() }
  }

  private fun displayName(source: Uri): String? =
    runCatching {
        context.contentResolver
          .query(source, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
          ?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
          }
      }
      .getOrNull()
      ?.takeIf { it.isNotBlank() }
      ?: source.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() }

  /**
   * The declared type, falling back to the file extension: a camera capture or a
   * `file://` Uri often has no media type, and refusing it would make the
   * attachment unusable before the type can even be checked.
   */
  private fun resolveMime(source: Uri, nome: String?): String {
    context.contentResolver.getType(source)?.let { return it.lowercase(Locale.ROOT) }
    val name = (nome ?: source.lastPathSegment.orEmpty()).lowercase(Locale.ROOT)
    return when {
      name.endsWith(".pdf") -> "application/pdf"
      name.endsWith(".png") -> "image/png"
      name.endsWith(".webp") -> "image/webp"
      name.endsWith(".jpg") || name.endsWith(".jpeg") -> "image/jpeg"
      else -> "application/octet-stream"
    }
  }

  private fun isAcceptedDocumentMime(mime: String): Boolean =
    mime == "application/pdf" || mime.startsWith("image/")

  private fun declaredSize(source: Uri): Long? =
    runCatching {
        context.contentResolver.openAssetFileDescriptor(source, "r")?.use { it.length }
      }
      .getOrNull()
      ?.takeIf { it >= 0 }

  /** Copies a PDF verbatim into the attachment directory, through a temp file. */
  private fun copyPdf(source: Uri): File {
    photosDir.mkdirs()
    val dest = File(photosDir, "file_${UUID.randomUUID()}.pdf")
    val tmp = File(dest.parentFile, dest.name + ".tmp")
    val input =
      context.contentResolver.openInputStream(source)
        ?: throw AttachmentRejectedException(AttachmentRejection.UNSUPPORTED_TYPE)
    input.use { stream ->
      FileOutputStream(tmp).use { out ->
        stream.copyTo(out)
        out.fd.sync()
      }
    }
    if (!tmp.renameTo(dest)) {
      FileOutputStream(dest).use { out -> tmp.inputStream().use { it.copyTo(out) } }
      tmp.delete()
    }
    return dest
  }

  /**
   * Room's `@Relation` query has no `ORDER BY`, so the photo list comes back in
   * rowid order. Sort it here once, so the list thumbnail, the detail grid and
   * the viewer all show the order the user chose when saving.
   */
  private fun ClienteComFotos.withOrderedPhotos(): ClienteComFotos =
    copy(
      fotos = fotos.sortedWith(compareBy({ it.ordem }, { it.id })),
      documentos = documentos.sortedWith(compareBy({ it.ordem }, { it.id })),
    )

  companion object {
    /** Matches the server's `ELISDB_MAX_PHOTO_BYTES` default. */
    const val MAX_ATTACHMENT_BYTES = 20L * 1024 * 1024
  }
}

/** Why a chosen file could not be attached to the device map. */
enum class AttachmentRejection { UNSUPPORTED_TYPE, TOO_LARGE }

/** The chosen file is not an image/PDF, or exceeds the server's size ceiling. */
class AttachmentRejectedException(val rejection: AttachmentRejection) :
  Exception("attachment rejected: $rejection")

/**
 * The client being saved no longer exists locally: a pull removed it while the
 * form was open. Reported to the user instead of pretending the save worked.
 */
class ClienteRemovidoException(val clienteId: String) :
  Exception("client $clienteId was removed by a sync")