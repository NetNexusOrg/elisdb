package com.example.elisdb.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.example.elisdb.data.local.AppDatabase
import com.example.elisdb.data.local.CatalogoDao
import com.example.elisdb.data.local.CatalogoItem
import com.example.elisdb.data.local.CategoriaCatalogo
import com.example.elisdb.data.local.Cliente
import com.example.elisdb.data.local.ClienteComFotos
import com.example.elisdb.data.local.ClienteDao
import com.example.elisdb.data.local.ClienteServico
import com.example.elisdb.data.local.ClienteServicoComItem
import com.example.elisdb.data.local.ClienteServicoDao
import com.example.elisdb.data.local.Foto
import com.example.elisdb.data.local.FotoDao
import com.example.elisdb.data.local.joinTags
import com.example.elisdb.sync.StagedPhotoRegistry
import com.example.elisdb.sync.SyncFault
import com.example.elisdb.sync.SyncManager
import com.example.elisdb.sync.SyncStatus
import java.io.File
import java.text.Normalizer
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

data class ServicoDraft(val itemId: String, val quantidade: Int? = null)

data class ClienteDraft(
  val id: String? = null,
  val nome: String,
  val contrato: String? = null,
  val tags: List<String> = emptyList(),
  val latitude: Double? = null,
  val longitude: Double? = null,
  val notas: String? = null,
  val ativo: Boolean = true,
  val fotos: List<Foto> = emptyList(),
  val servicos: List<ServicoDraft> = emptyList(),
)

class ClienteRepositoryImpl(
  private val context: Context,
  private val database: AppDatabase,
  private val clienteDao: ClienteDao,
  private val fotoDao: FotoDao,
  private val catalogoDao: CatalogoDao,
  private val clienteServicoDao: ClienteServicoDao,
  private val syncManager: SyncManager? = null,
  private val clockOffsetStore: com.example.elisdb.sync.ClockOffsetStore? = null,
  private val stagedPhotoRegistry: StagedPhotoRegistry? = null,
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
    // The staged registry plus the sweeper's age grace protect the new file,
    // without holding the pull/materialization guard during encoding.
    val dest = photoEncoder.encodeToWebp(source, photosDir)
    stagedPhotoRegistry?.add(dest.absolutePath)
    Foto(id = "", clienteId = "", uriLocal = dest.absolutePath)
  }

  override fun createPhotoFile(): File {
    photosDir.mkdirs()
    val file = File(photosDir, "foto_${UUID.randomUUID()}.jpg")
    stagedPhotoRegistry?.add(file.absolutePath)
    return file
  }

  override suspend fun deletePhotoFile(foto: Foto) {
    stagedPhotoRegistry?.remove(foto.uriLocal)
    withContext(Dispatchers.IO) { runCatching { File(foto.uriLocal).delete() } }
  }

  override suspend fun save(draft: ClienteDraft): String {
    val now = nowEpochSeconds()
    val tag = joinTags(draft.tags)
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
                tag = tag,
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
                tag = tag,
                latitude = draft.latitude,
                longitude = draft.longitude,
                notas = draft.notas?.takeIf { it.isNotBlank() },
                ativo = draft.ativo,
                updatedAt = now,
                pending = true,
                localOnly = false,
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
        // Exactly one photo is the profile photo: the first one the user marked
        // or, when nothing is marked (including photos saved before the flag
        // existed), the first of the gallery.
        val preferredIndex =
          draft.fotos.indexOfFirst { it.preferida }.takeIf { it >= 0 }
            ?: draft.fotos.indices.firstOrNull()
        draft.fotos
          .forEachIndexed { index, foto ->
            fotoDao.insert(
              foto.copy(
                id = foto.id.ifEmpty { UUID.randomUUID().toString() },
                clienteId = clienteId,
                ordem = index,
                preferida = index == preferredIndex,
              )
            )
          }
        clienteServicoDao.replaceForCliente(
          clienteId,
          draft.servicos.map {
            ClienteServico(clienteId = clienteId, itemId = it.itemId, quantidade = it.quantidade)
          },
        )
        clienteId
      }
    // The photos in the draft now have rows, so a sweep may reclaim them if the
    // client is later removed. Removed files are deleted only after the
    // transaction commits, so a rollback cannot leave rows pointing at deleted
    // files.
    draft.fotos.forEach { stagedPhotoRegistry?.remove(it.uriLocal) }
    removedFiles.forEach { deleteFile(it) }
    return clienteId
  }

  override val catalogo: Flow<List<CatalogoItem>> = catalogoDao.observeAll()

  override fun observeServicos(clienteId: String): Flow<List<ClienteServicoComItem>> =
    clienteServicoDao.observeForCliente(clienteId)

  override suspend fun getCatalogo(): List<CatalogoItem> = catalogoDao.getAll()

  override suspend fun getServicos(clienteId: String): List<ClienteServicoComItem> =
    clienteServicoDao.getForCliente(clienteId).mapNotNull { servico ->
      catalogoDao.getById(servico.itemId)?.let { item ->
        ClienteServicoComItem(servico, item)
      } ?: ClienteServicoComItem(servico, null)
    }

  override suspend fun saveCatalogoItem(
    id: String?,
    nome: String,
    categoria: CategoriaCatalogo,
  ): String {
    val clean = nome.trim().replace(Regex("\\s+"), " ").uppercase(Locale.ROOT)
    if (clean.isBlank() || clean.length > 80) throw CatalogoNomeInvalidoException()
    val normalized = normalizarNomeCatalogo(clean)
    val duplicate = catalogoDao.getByCategoria(categoria.slug, id ?: "").any {
      it.nomeNormalizado == normalized
    }
    if (duplicate) throw CatalogoNomeDuplicadoException()
    val now = nowEpochSeconds()
    val itemId =
      if (id == null) {
        UUID.randomUUID().toString()
      } else {
        val existing = catalogoDao.getById(id) ?: throw CatalogoItemRemovidoException(id)
        if (existing.categoria != categoria.slug) throw CatalogoCategoriaImutavelException()
        id
      }
    val existing = id?.let { catalogoDao.getById(it) }
    catalogoDao.upsert(
      CatalogoItem(
        id = itemId,
        categoria = categoria.slug,
        nome = clean,
        nomeNormalizado = normalized,
        ativo = existing?.ativo ?: true,
        pending = true,
        syncError = null,
        createdAt = existing?.createdAt ?: now,
        updatedAt = now,
        revision = (existing?.revision ?: 0) + 1,
      )
    )
    return itemId
  }

  override suspend fun setCatalogoAtivo(id: String, ativo: Boolean) {
    val existing = catalogoDao.getById(id) ?: throw CatalogoItemRemovidoException(id)
    catalogoDao.upsert(
      existing.copy(
        ativo = ativo,
        pending = true,
        syncError = null,
        updatedAt = nowEpochSeconds(),
        revision = existing.revision + 1,
      )
    )
  }

  override suspend fun deleteCliente(id: String) {
    clienteDao.getById(id)?.let { existing ->
      clienteDao.update(existing.copy(deletePending = true, revision = existing.revision + 1))
    }
  }

  override suspend fun syncNow(): SyncStatus =
    syncManager?.syncNow() ?: SyncStatus.IDLE

  private fun deleteFile(foto: Foto) {
    stagedPhotoRegistry?.remove(foto.uriLocal)
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

class CatalogoNomeDuplicadoException :
  Exception("catalog item name already exists in this category")

class CatalogoNomeInvalidoException : Exception("catalog item name is blank")

class CatalogoCategoriaImutavelException :
  Exception("catalog item category cannot be changed")

class CatalogoItemRemovidoException(val itemId: String) :
  Exception("catalog item $itemId does not exist locally")

/** Duplicate detection only: trim, collapse spaces, drop diacritics, uppercase. */
internal fun normalizarNomeCatalogo(nome: String): String {
  val decomposed = Normalizer.normalize(nome, Normalizer.Form.NFD)
  return decomposed
    .replace(Regex("\\p{M}+"), "")
    .replace(Regex("\\s+"), " ")
    .trim()
    .uppercase(Locale.ROOT)
}
