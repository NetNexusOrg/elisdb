package com.example.elisdb.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.elisdb.data.local.AppDatabase
import com.example.elisdb.data.local.Cliente
import com.example.elisdb.data.local.Foto
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class HistoryQueriesTest {

  @Test
  fun queriesOnlyTheRequestedRowsThatStillExist() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    try {
      database.clienteDao().insert(cliente("c1", deletePending = false))
      database.clienteDao().insert(cliente("c2", deletePending = true))
      database.fotoDao().insert(
        Foto(id = "f1", clienteId = "c1", uriLocal = "/tmp/f1.webp")
      )
      database.fotoDao().insert(
        Foto(id = "f2", clienteId = "c1", uriLocal = "/tmp/f2.webp", ordem = 1)
      )

      val uris = database.fotoDao().getUrisByIds(listOf("f1", "missing"))
      assertEquals(listOf("f1" to "/tmp/f1.webp"), uris.map { it.id to it.uriLocal })
      assertEquals(setOf("c1"), database.clienteDao().getExistingIds(listOf("c1", "c2", "gone")).toSet())
    } finally {
      database.close()
    }
  }

  @Test
  fun emptyListsReturnNothing() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    try {
      assertEquals(emptyList<Any>(), database.fotoDao().getUrisByIds(emptyList()))
      assertEquals(emptyList<String>(), database.clienteDao().getExistingIds(emptyList()))
    } finally {
      database.close()
    }
  }

  private fun cliente(id: String, deletePending: Boolean) =
    Cliente(
      id = id,
      nome = "Cliente $id",
      dataCriacao = 0,
      updatedAt = 0,
      deletePending = deletePending,
    )
}
