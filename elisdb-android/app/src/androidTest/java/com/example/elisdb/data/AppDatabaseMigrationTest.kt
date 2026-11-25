package com.example.elisdb.data

import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.elisdb.data.local.AppDatabase
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test

class AppDatabaseMigrationTest {

  private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
  private val dbName = "migration_test.db"

  @Before
  fun setUp() {
    context.deleteDatabase(dbName)
  }

  @After
  fun tearDown() {
    context.deleteDatabase(dbName)
  }

  @Test
  fun migracao_1_2_preservaClienteEAdicionaContratoNulo() {
    createDatabaseV1WithClient()

    val database =
      Room.databaseBuilder(context, AppDatabase::class.java, dbName)
        .addMigrations(
          AppDatabase.MIGRATION_1_2,
          AppDatabase.MIGRATION_2_3,
          AppDatabase.MIGRATION_3_4,
          AppDatabase.MIGRATION_4_5,
          AppDatabase.MIGRATION_5_6,
          AppDatabase.MIGRATION_6_7,
          AppDatabase.MIGRATION_7_8,
          AppDatabase.MIGRATION_8_9, AppDatabase.MIGRATION_9_10,
          AppDatabase.MIGRATION_10_11, AppDatabase.MIGRATION_11_12, AppDatabase.MIGRATION_12_13, AppDatabase.MIGRATION_13_14, AppDatabase.MIGRATION_14_15,
        )
        .build()

    val cliente = runBlocking { database.clienteDao().getById("1") }
    assertNotNull(cliente)
    assertEquals("Maria", cliente?.nome)
    assertEquals(1.0, cliente?.latitude)
    assertNull(cliente?.contrato)

    database.close()
  }

  @Test
  fun migracao_2_3_removeColunaTelefoneEPreservaCliente() {
    createDatabaseV2WithClient()

    val database =
      Room.databaseBuilder(context, AppDatabase::class.java, dbName)
        .addMigrations(AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5, AppDatabase.MIGRATION_5_6, AppDatabase.MIGRATION_6_7, AppDatabase.MIGRATION_7_8, AppDatabase.MIGRATION_8_9, AppDatabase.MIGRATION_9_10, AppDatabase.MIGRATION_10_11, AppDatabase.MIGRATION_11_12, AppDatabase.MIGRATION_12_13, AppDatabase.MIGRATION_13_14, AppDatabase.MIGRATION_14_15)
        .build()

    val cliente = runBlocking { database.clienteDao().getById("1") }
    assertNotNull(cliente)
    assertEquals("Maria", cliente?.nome)
    assertEquals(1.0, cliente?.latitude)

    // The column is dropped by rebuilding the table; the rebuild must not
    // cascade into the fotos rows (SQLite's DROP COLUMN does not exist before
    // 3.35, i.e. on Android 14 and older releases).
    val fotos = runBlocking { database.fotoDao().getByCliente("1") }
    assertEquals(1, fotos.size)
    assertEquals("/x.jpg", fotos[0].uriLocal)

    val columns =
      database.openHelper.writableDatabase.query("PRAGMA table_info(clientes)").use { cursor ->
        buildSet {
          while (cursor.moveToNext()) add(cursor.getString(1))
        }
      }
    assertTrue(columns.contains("contrato"))
    assertFalse(columns.contains("telefone"))

    database.close()
  }

  @Test
  fun migracao_3_4_converteIdsParaStringEPreservaCliente() {
    createDatabaseV3WithClientAndFoto()

    val database =
      Room.databaseBuilder(context, AppDatabase::class.java, dbName)
        .addMigrations(AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5, AppDatabase.MIGRATION_5_6, AppDatabase.MIGRATION_6_7, AppDatabase.MIGRATION_7_8, AppDatabase.MIGRATION_8_9, AppDatabase.MIGRATION_9_10, AppDatabase.MIGRATION_10_11, AppDatabase.MIGRATION_11_12, AppDatabase.MIGRATION_12_13, AppDatabase.MIGRATION_13_14, AppDatabase.MIGRATION_14_15)
        .build()

    val cliente = runBlocking { database.clienteDao().getById("1") }
    assertNotNull(cliente)
    assertEquals("Maria", cliente?.nome)
    assertEquals(1.0, cliente?.latitude)
    assertEquals(1000L, cliente?.updatedAt)

    val fotos = runBlocking { database.fotoDao().getByCliente("1") }
    assertEquals(1, fotos.size)
    assertEquals("1", fotos[0].id)

    val columns =
      database.openHelper.writableDatabase.query("PRAGMA table_info(clientes)").use { cursor ->
        buildSet {
          while (cursor.moveToNext()) add(cursor.getString(1))
        }
      }
    assertTrue(columns.contains("id"))
    assertTrue(columns.contains("updatedAt"))
    assertTrue(columns.contains("pending"))
    assertTrue(columns.contains("deletePending"))

    database.close()
  }

  @Test
  fun migracao_4_5_adicionaColunaCreatedAtEPreservaCliente() {
    createDatabaseV4WithClient()

    val database =
      Room.databaseBuilder(context, AppDatabase::class.java, dbName)
        .addMigrations(AppDatabase.MIGRATION_4_5, AppDatabase.MIGRATION_5_6, AppDatabase.MIGRATION_6_7, AppDatabase.MIGRATION_7_8, AppDatabase.MIGRATION_8_9, AppDatabase.MIGRATION_9_10, AppDatabase.MIGRATION_10_11, AppDatabase.MIGRATION_11_12, AppDatabase.MIGRATION_12_13, AppDatabase.MIGRATION_13_14, AppDatabase.MIGRATION_14_15)
        .build()

    val cliente = runBlocking { database.clienteDao().getById("1") }
    assertNotNull(cliente)
    assertEquals("Maria", cliente?.nome)
    assertEquals(1000L, cliente?.updatedAt)
    assertNotNull(cliente?.createdAt)
    assertTrue("createdAt back-filled from dataCriacao", (cliente?.createdAt ?: 0L) == 1234L)

    val columns =
      database.openHelper.writableDatabase.query("PRAGMA table_info(clientes)").use { cursor ->
        buildSet {
          while (cursor.moveToNext()) add(cursor.getString(1))
        }
      }
    assertTrue(columns.contains("createdAt"))
    assertTrue(columns.contains("dataCriacao"))
    assertTrue(columns.contains("updatedAt"))

    database.close()
  }

  @Test
  fun migracao_5_6_criaTabelaSyncMetaEPreservaCliente() {
    createDatabaseV5WithClient()

    val database =
      Room.databaseBuilder(context, AppDatabase::class.java, dbName)
        .addMigrations(AppDatabase.MIGRATION_5_6, AppDatabase.MIGRATION_6_7, AppDatabase.MIGRATION_7_8, AppDatabase.MIGRATION_8_9, AppDatabase.MIGRATION_9_10, AppDatabase.MIGRATION_10_11, AppDatabase.MIGRATION_11_12, AppDatabase.MIGRATION_12_13, AppDatabase.MIGRATION_13_14, AppDatabase.MIGRATION_14_15)
        .build()

    val cliente = runBlocking { database.clienteDao().getById("1") }
    assertNotNull(cliente)
    assertEquals("Maria", cliente?.nome)

    val meta = runBlocking { database.syncMetaDao().get("last_pull_watermark") }
    assertNull(meta)

    val tables =
      database.openHelper.writableDatabase.query("SELECT name FROM sqlite_master WHERE type='table'").use { cursor ->
        buildSet {
          while (cursor.moveToNext()) add(cursor.getString(0))
        }
      }
    assertTrue(tables.contains("sync_meta"))

    database.close()
  }

  @Test
  fun migracao_6_7_tornaCoordenadasNulasEPreservaCliente() {
    createDatabaseV6WithClient()

    val database =
      Room.databaseBuilder(context, AppDatabase::class.java, dbName)
        .addMigrations(AppDatabase.MIGRATION_6_7, AppDatabase.MIGRATION_7_8, AppDatabase.MIGRATION_8_9, AppDatabase.MIGRATION_9_10, AppDatabase.MIGRATION_10_11, AppDatabase.MIGRATION_11_12, AppDatabase.MIGRATION_12_13, AppDatabase.MIGRATION_13_14, AppDatabase.MIGRATION_14_15)
        .build()

    val cliente = runBlocking { database.clienteDao().getById("1") }
    assertNotNull(cliente)
    assertEquals("Maria", cliente?.nome)
    assertEquals(1.0, cliente?.latitude)
    assertEquals(2.0, cliente?.longitude)
    assertEquals(1000L, cliente?.updatedAt)

    val notNulls =
      database.openHelper.writableDatabase.query("PRAGMA table_info(clientes)").use { cursor ->
        buildMap {
          while (cursor.moveToNext()) {
            val name = cursor.getString(1)
            val notNull = cursor.getInt(3)
            if (name == "latitude" || name == "longitude") put(name, notNull == 1)
          }
        }
      }
    assertEquals(false, notNulls["latitude"])
    assertEquals(false, notNulls["longitude"])

    database.close()
  }

  @Test
  fun migracao_7_8_converteTimestampsMsParaSegundos() {
    createDatabaseV7WithClient()

    val database =
      Room.databaseBuilder(context, AppDatabase::class.java, dbName)
        .addMigrations(AppDatabase.MIGRATION_7_8, AppDatabase.MIGRATION_8_9, AppDatabase.MIGRATION_9_10, AppDatabase.MIGRATION_10_11, AppDatabase.MIGRATION_11_12, AppDatabase.MIGRATION_12_13, AppDatabase.MIGRATION_13_14, AppDatabase.MIGRATION_14_15)
        .build()

    val cliente = runBlocking { database.clienteDao().getById("1") }
    assertNotNull(cliente)
    assertEquals("Maria", cliente?.nome)
    assertEquals(1758000000L, cliente?.updatedAt)
    assertEquals(1758000000L, cliente?.createdAt)
    assertEquals(1758000000L, cliente?.dataCriacao)

    val meta = runBlocking { database.syncMetaDao().get("last_pull_watermark") }
    assertEquals(1758000000L, meta)

    database.close()
  }

  @Test
  fun migracao_8_9_adicionaRevisionEPreservaCliente() {
    createDatabaseV8WithClient()

    val database =
      Room.databaseBuilder(context, AppDatabase::class.java, dbName)
        .addMigrations(AppDatabase.MIGRATION_8_9, AppDatabase.MIGRATION_9_10, AppDatabase.MIGRATION_10_11, AppDatabase.MIGRATION_11_12, AppDatabase.MIGRATION_12_13, AppDatabase.MIGRATION_13_14, AppDatabase.MIGRATION_14_15)
        .build()

    val cliente = runBlocking { database.clienteDao().getById("1") }
    assertNotNull(cliente)
    assertEquals("Maria", cliente?.nome)
    assertEquals(1758000000L, cliente?.updatedAt)
    // Rows written before the column existed start at revision 0; the next
    // local write bumps it, which is what the push acknowledgement compares.
    assertEquals(0L, cliente?.revision)

    runBlocking {
      database.clienteDao().markSynced("1", seenRevision = 0)
    }
    assertFalse(runBlocking { database.clienteDao().getById("1") }!!.pending)

    database.close()
  }

  @Test
  fun migracao_9_10_adicionaAtivoEPreservaCliente() {
    createDatabaseV9WithClient()

    val database =
      Room.databaseBuilder(context, AppDatabase::class.java, dbName)
        .addMigrations(AppDatabase.MIGRATION_9_10, AppDatabase.MIGRATION_10_11, AppDatabase.MIGRATION_11_12, AppDatabase.MIGRATION_12_13, AppDatabase.MIGRATION_13_14, AppDatabase.MIGRATION_14_15)
        .build()

    val cliente = runBlocking { database.clienteDao().getById("1") }
    assertNotNull(cliente)
    assertEquals("Maria", cliente?.nome)
    assertEquals(1758000000L, cliente?.updatedAt)
    // The column default keeps every client that existed before the migration
    // in service, so nothing disappears from the list after upgrading.
    assertTrue(cliente!!.ativo)
    assertNull(cliente.tag)

    runBlocking { database.clienteDao().update(cliente.copy(ativo = false)) }
    assertFalse(runBlocking { database.clienteDao().getById("1") }!!.ativo)

    database.close()
  }

  @Test
  fun migracao_10_11_adicionaTagEPreservaCliente() {
    createDatabaseV10WithClient()

    val database =
      Room.databaseBuilder(context, AppDatabase::class.java, dbName)
        .addMigrations(AppDatabase.MIGRATION_10_11, AppDatabase.MIGRATION_11_12, AppDatabase.MIGRATION_12_13, AppDatabase.MIGRATION_13_14, AppDatabase.MIGRATION_14_15)
        .build()

    val cliente = runBlocking { database.clienteDao().getById("1") }
    assertNotNull(cliente)
    assertEquals("Maria", cliente?.nome)
    assertNull(cliente?.tag)

    runBlocking { database.clienteDao().update(cliente!!.copy(tag = "Premium")) }
    assertEquals("Premium", runBlocking { database.clienteDao().getById("1") }?.tag)

    database.close()
  }


  @Test
  fun migracao_11_12_adicionaLocalOnlyEPreservaCliente() {
    createDatabaseV11WithClient()

    val database =
      Room.databaseBuilder(context, AppDatabase::class.java, dbName)
        .addMigrations(AppDatabase.MIGRATION_11_12, AppDatabase.MIGRATION_12_13, AppDatabase.MIGRATION_13_14, AppDatabase.MIGRATION_14_15)
        .build()

    val cliente = runBlocking { database.clienteDao().getById("1") }
    assertNotNull(cliente)
    assertEquals("Maria", cliente?.nome)
    assertFalse("existing records are not local-only", cliente!!.localOnly)

    runBlocking { database.clienteDao().update(cliente.copy(localOnly = true)) }
    assertTrue(runBlocking { database.clienteDao().getById("1") }!!.localOnly)

    database.close()
  }


  @Test
  fun migracao_12_13_adicionaPreferidaEPreservaFotos() {
    createDatabaseV12WithPhoto()

    val database =
      Room.databaseBuilder(context, AppDatabase::class.java, dbName)
        .addMigrations(AppDatabase.MIGRATION_12_13, AppDatabase.MIGRATION_13_14, AppDatabase.MIGRATION_14_15)
        .build()

    val fotos = runBlocking { database.fotoDao().getByCliente("1") }
    assertEquals(1, fotos.size)
    assertFalse("existing photos are not the profile photo", fotos[0].preferida)

    runBlocking { database.fotoDao().update(fotos[0].copy(preferida = true)) }
    assertTrue(runBlocking { database.fotoDao().getByCliente("1") }[0].preferida)

    database.close()
  }



  @Test
  fun migracao_13_14_criaCatalogoEPreservaCliente() {
    createDatabaseV13WithClient()

    val database =
      Room.databaseBuilder(context, AppDatabase::class.java, dbName)
        .addMigrations(AppDatabase.MIGRATION_13_14, AppDatabase.MIGRATION_14_15)
        .build()

    val cliente = runBlocking { database.clienteDao().getById("1") }
    assertNotNull(cliente)
    assertEquals("Maria", cliente?.nome)

    val tables =
      database.openHelper.writableDatabase.query("SELECT name FROM sqlite_master WHERE type='table'").use { cursor ->
        buildSet {
          while (cursor.moveToNext()) add(cursor.getString(0))
        }
      }
    assertTrue(tables.contains("catalogo_itens"))

    val item =
      com.example.elisdb.data.local.CatalogoItem(
        id = "c1",
        categoria = "roedores",
        nome = "Ratoeira",
        nomeNormalizado = "RATOEIRA",
      )
    runBlocking { database.catalogoDao().insert(item) }
    assertEquals("Ratoeira", runBlocking { database.catalogoDao().getById("c1") }?.nome)

    database.close()
  }

  @Test
  fun migracao_14_15_criaServicosComCascata() {
    createDatabaseV14WithCatalog()

    val database =
      Room.databaseBuilder(context, AppDatabase::class.java, dbName)
        .addMigrations(AppDatabase.MIGRATION_14_15)
        .build()

    runBlocking {
      database.clienteServicoDao().upsertAll(
        listOf(
          com.example.elisdb.data.local.ClienteServico(
            clienteId = "1",
            itemId = "c1",
            quantidade = 4,
          )
        )
      )
    }
    assertEquals(4, runBlocking { database.clienteServicoDao().getForCliente("1") }.single().quantidade)

    runBlocking { database.clienteDao().delete(runBlocking { database.clienteDao().getById("1") }!!) }
    assertTrue(runBlocking { database.clienteServicoDao().getForCliente("1") }.isEmpty())

    database.close()
  }

  private fun createDatabaseV13WithClient() {
    val configuration =
      SupportSQLiteOpenHelper.Configuration.builder(context)
        .name(dbName)
        .callback(
          object : SupportSQLiteOpenHelper.Callback(13) {
            override fun onCreate(db: SupportSQLiteDatabase) {
              db.execSQL(
                "CREATE TABLE IF NOT EXISTS clientes (" +
                  "id TEXT NOT NULL PRIMARY KEY, " +
                  "nome TEXT NOT NULL, contrato TEXT, tag TEXT, " +
                  "latitude REAL, longitude REAL, " +
                  "notas TEXT, dataCriacao INTEGER NOT NULL, " +
                  "createdAt INTEGER NOT NULL DEFAULT 0, " +
                  "updatedAt INTEGER NOT NULL, " +
                  "pending INTEGER NOT NULL DEFAULT 0, " +
                  "deletePending INTEGER NOT NULL DEFAULT 0, " +
                  "revision INTEGER NOT NULL DEFAULT 0, " +
                  "ativo INTEGER NOT NULL DEFAULT 1, " +
                  "localOnly INTEGER NOT NULL DEFAULT 0)"
              )
              db.execSQL(
                "CREATE TABLE IF NOT EXISTS fotos (" +
                  "id TEXT NOT NULL PRIMARY KEY, " +
                  "clienteId TEXT NOT NULL, uriLocal TEXT NOT NULL, " +
                  "mime TEXT NOT NULL DEFAULT 'image/jpeg', " +
                  "ordem INTEGER NOT NULL DEFAULT 0, " +
                  "deletePending INTEGER NOT NULL DEFAULT 0, " +
                  "preferida INTEGER NOT NULL DEFAULT 0, " +
                  "FOREIGN KEY(clienteId) REFERENCES clientes(id) " +
                  "ON UPDATE NO ACTION ON DELETE CASCADE)"
              )
              db.execSQL("CREATE INDEX IF NOT EXISTS index_fotos_clienteId ON fotos (clienteId)")
              db.execSQL(
                "CREATE TABLE IF NOT EXISTS sync_meta (" +
                  "`key` TEXT NOT NULL PRIMARY KEY, " +
                  "value INTEGER NOT NULL)"
              )
            }

            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
          }
        )
        .build()

    val helper = FrameworkSQLiteOpenHelperFactory().create(configuration)
    helper.writableDatabase.use { db ->
      db.execSQL(
        "INSERT INTO clientes (id, nome, contrato, latitude, longitude, dataCriacao, updatedAt, pending, deletePending, revision, ativo, localOnly) " +
          "VALUES ('1', 'Maria', 'CT-1', 1.0, 2.0, 1758000000, 1758000000, 0, 0, 0, 1, 0)"
      )
    }
    helper.close()
  }

  private fun createDatabaseV14WithCatalog() {
    createDatabaseV13WithClient()
    val helper =
      FrameworkSQLiteOpenHelperFactory()
        .create(
          SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(14) {
              override fun onCreate(db: SupportSQLiteDatabase) = Unit
              override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()
        )
    helper.writableDatabase.use { db ->
      db.execSQL(
        "CREATE TABLE IF NOT EXISTS catalogo_itens (" +
          "id TEXT NOT NULL PRIMARY KEY, categoria TEXT NOT NULL, nome TEXT NOT NULL, " +
          "nomeNormalizado TEXT NOT NULL, ativo INTEGER NOT NULL DEFAULT 1, " +
          "pending INTEGER NOT NULL DEFAULT 0, syncError TEXT, " +
          "createdAt INTEGER NOT NULL DEFAULT 0, updatedAt INTEGER NOT NULL DEFAULT 0, " +
          "revision INTEGER NOT NULL DEFAULT 0)"
      )
      db.execSQL("CREATE INDEX IF NOT EXISTS index_catalogo_itens_categoria ON catalogo_itens (categoria)")
      db.execSQL(
        "INSERT INTO catalogo_itens (id, categoria, nome, nomeNormalizado, ativo, pending, createdAt, updatedAt, revision) " +
          "VALUES ('c1', 'roedores', 'Ratoeira', 'RATOEIRA', 1, 0, 1, 1, 0)"
      )
    }
    helper.close()
  }

  private fun createDatabaseV12WithPhoto() {
    val configuration =
      SupportSQLiteOpenHelper.Configuration.builder(context)
        .name(dbName)
        .callback(
          object : SupportSQLiteOpenHelper.Callback(12) {
            override fun onCreate(db: SupportSQLiteDatabase) {
              db.execSQL(
                "CREATE TABLE IF NOT EXISTS clientes (" +
                  "id TEXT NOT NULL PRIMARY KEY, " +
                  "nome TEXT NOT NULL, contrato TEXT, tag TEXT, " +
                  "latitude REAL, longitude REAL, " +
                  "notas TEXT, dataCriacao INTEGER NOT NULL, " +
                  "createdAt INTEGER NOT NULL DEFAULT 0, " +
                  "updatedAt INTEGER NOT NULL, " +
                  "pending INTEGER NOT NULL DEFAULT 0, " +
                  "deletePending INTEGER NOT NULL DEFAULT 0, " +
                  "revision INTEGER NOT NULL DEFAULT 0, " +
                  "ativo INTEGER NOT NULL DEFAULT 1, " +
                  "localOnly INTEGER NOT NULL DEFAULT 0)"
              )
              db.execSQL(
                "CREATE TABLE IF NOT EXISTS fotos (" +
                  "id TEXT NOT NULL PRIMARY KEY, " +
                  "clienteId TEXT NOT NULL, uriLocal TEXT NOT NULL, " +
                  "mime TEXT NOT NULL DEFAULT 'image/jpeg', " +
                  "ordem INTEGER NOT NULL DEFAULT 0, " +
                  "deletePending INTEGER NOT NULL DEFAULT 0, " +
                  "FOREIGN KEY(clienteId) REFERENCES clientes(id) " +
                  "ON UPDATE NO ACTION ON DELETE CASCADE)"
              )
              db.execSQL("CREATE INDEX IF NOT EXISTS index_fotos_clienteId ON fotos (clienteId)")
              db.execSQL(
                "CREATE TABLE IF NOT EXISTS sync_meta (" +
                  "`key` TEXT NOT NULL PRIMARY KEY, " +
                  "value INTEGER NOT NULL)"
              )
            }

            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
          }
        )
        .build()

    val helper = FrameworkSQLiteOpenHelperFactory().create(configuration)
    helper.writableDatabase.use { db ->
      db.execSQL(
        "INSERT INTO clientes (id, nome, contrato, tag, latitude, longitude, notas, dataCriacao, createdAt, updatedAt, pending, deletePending, revision, ativo, localOnly) " +
          "VALUES ('1', 'Maria', 'CT-1', NULL, 1.0, 2.0, NULL, 1758000000, 1758000000, 1758000000, 0, 0, 0, 1, 0)"
      )
      db.execSQL(
        "INSERT INTO fotos (id, clienteId, uriLocal, mime, ordem, deletePending) " +
          "VALUES ('p1', '1', '/tmp/p1.webp', 'image/webp', 0, 0)"
      )
    }
    helper.close()
  }

  private fun createDatabaseV11WithClient() {
    val configuration =
      SupportSQLiteOpenHelper.Configuration.builder(context)
        .name(dbName)
        .callback(
          object : SupportSQLiteOpenHelper.Callback(11) {
            override fun onCreate(db: SupportSQLiteDatabase) {
              db.execSQL(
                "CREATE TABLE IF NOT EXISTS clientes (" +
                  "id TEXT NOT NULL PRIMARY KEY, " +
                  "nome TEXT NOT NULL, contrato TEXT, tag TEXT, " +
                  "latitude REAL, longitude REAL, " +
                  "notas TEXT, dataCriacao INTEGER NOT NULL, " +
                  "createdAt INTEGER NOT NULL DEFAULT 0, " +
                  "updatedAt INTEGER NOT NULL, " +
                  "pending INTEGER NOT NULL DEFAULT 0, " +
                  "deletePending INTEGER NOT NULL DEFAULT 0, " +
                  "revision INTEGER NOT NULL DEFAULT 0, " +
                  "ativo INTEGER NOT NULL DEFAULT 1)"
              )
              db.execSQL(
                "CREATE TABLE IF NOT EXISTS fotos (" +
                  "id TEXT NOT NULL PRIMARY KEY, " +
                  "clienteId TEXT NOT NULL, uriLocal TEXT NOT NULL, " +
                  "mime TEXT NOT NULL DEFAULT 'image/jpeg', " +
                  "ordem INTEGER NOT NULL DEFAULT 0, " +
                  "deletePending INTEGER NOT NULL DEFAULT 0, " +
                  "FOREIGN KEY(clienteId) REFERENCES clientes(id) " +
                  "ON UPDATE NO ACTION ON DELETE CASCADE)"
              )
              db.execSQL("CREATE INDEX IF NOT EXISTS index_fotos_clienteId ON fotos (clienteId)")
              db.execSQL(
                "CREATE TABLE IF NOT EXISTS sync_meta (" +
                  "`key` TEXT NOT NULL PRIMARY KEY, " +
                  "value INTEGER NOT NULL)"
              )
            }

            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
          }
        )
        .build()

    val helper = FrameworkSQLiteOpenHelperFactory().create(configuration)
    helper.writableDatabase.use { db ->
      db.execSQL(
        "INSERT INTO clientes (id, nome, contrato, tag, latitude, longitude, notas, dataCriacao, createdAt, updatedAt, pending, deletePending, revision, ativo) " +
          "VALUES ('1', 'Maria', 'CT-1', NULL, 1.0, 2.0, NULL, 1758000000, 1758000000, 1758000000, 0, 0, 0, 1)"
      )
    }
    helper.close()
  }

  private fun createDatabaseV10WithClient() {
    val configuration =
      SupportSQLiteOpenHelper.Configuration.builder(context)
        .name(dbName)
        .callback(
          object : SupportSQLiteOpenHelper.Callback(10) {
            override fun onCreate(db: SupportSQLiteDatabase) {
              db.execSQL(
                "CREATE TABLE IF NOT EXISTS clientes (" +
                  "id TEXT NOT NULL PRIMARY KEY, " +
                  "nome TEXT NOT NULL, contrato TEXT, " +
                  "latitude REAL, longitude REAL, " +
                  "notas TEXT, dataCriacao INTEGER NOT NULL, " +
                  "createdAt INTEGER NOT NULL DEFAULT 0, " +
                  "updatedAt INTEGER NOT NULL, " +
                  "pending INTEGER NOT NULL DEFAULT 0, " +
                  "deletePending INTEGER NOT NULL DEFAULT 0, " +
                  "revision INTEGER NOT NULL DEFAULT 0, " +
                  "ativo INTEGER NOT NULL DEFAULT 1)"
              )
              db.execSQL(
                "CREATE TABLE IF NOT EXISTS fotos (" +
                  "id TEXT NOT NULL PRIMARY KEY, " +
                  "clienteId TEXT NOT NULL, uriLocal TEXT NOT NULL, " +
                  "mime TEXT NOT NULL DEFAULT 'image/jpeg', " +
                  "ordem INTEGER NOT NULL DEFAULT 0, " +
                  "deletePending INTEGER NOT NULL DEFAULT 0, " +
                  "FOREIGN KEY(clienteId) REFERENCES clientes(id) " +
                  "ON UPDATE NO ACTION ON DELETE CASCADE)"
              )
              db.execSQL("CREATE INDEX IF NOT EXISTS index_fotos_clienteId ON fotos (clienteId)")
              db.execSQL(
                "CREATE TABLE IF NOT EXISTS sync_meta (" +
                  "`key` TEXT NOT NULL PRIMARY KEY, " +
                  "value INTEGER NOT NULL)"
              )
            }

            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
          }
        )
        .build()

    val helper = FrameworkSQLiteOpenHelperFactory().create(configuration)
    helper.writableDatabase.use { db ->
      db.execSQL(
        "INSERT INTO clientes (id, nome, contrato, latitude, longitude, notas, dataCriacao, createdAt, updatedAt, pending, deletePending, revision, ativo) " +
          "VALUES ('1', 'Maria', 'CT-1', 1.0, 2.0, NULL, 1758000000, 1758000000, 1758000000, 0, 0, 0, 1)"
      )
    }
    helper.close()
  }

  private fun createDatabaseV9WithClient() {
    val configuration =
      SupportSQLiteOpenHelper.Configuration.builder(context)
        .name(dbName)
        .callback(
          object : SupportSQLiteOpenHelper.Callback(9) {
            override fun onCreate(db: SupportSQLiteDatabase) {
              db.execSQL(
                "CREATE TABLE IF NOT EXISTS clientes (" +
                  "id TEXT NOT NULL PRIMARY KEY, " +
                  "nome TEXT NOT NULL, contrato TEXT, " +
                  "latitude REAL, longitude REAL, " +
                  "notas TEXT, dataCriacao INTEGER NOT NULL, " +
                  "createdAt INTEGER NOT NULL DEFAULT 0, " +
                  "updatedAt INTEGER NOT NULL, " +
                  "pending INTEGER NOT NULL DEFAULT 0, " +
                  "deletePending INTEGER NOT NULL DEFAULT 0, " +
                  "revision INTEGER NOT NULL DEFAULT 0)"
              )
              db.execSQL(
                "CREATE TABLE IF NOT EXISTS fotos (" +
                  "id TEXT NOT NULL PRIMARY KEY, " +
                  "clienteId TEXT NOT NULL, uriLocal TEXT NOT NULL, " +
                  "mime TEXT NOT NULL DEFAULT 'image/jpeg', " +
                  "ordem INTEGER NOT NULL DEFAULT 0, " +
                  "deletePending INTEGER NOT NULL DEFAULT 0, " +
                  "FOREIGN KEY(clienteId) REFERENCES clientes(id) " +
                  "ON UPDATE NO ACTION ON DELETE CASCADE)"
              )
              db.execSQL("CREATE INDEX IF NOT EXISTS index_fotos_clienteId ON fotos (clienteId)")
              db.execSQL(
                "CREATE TABLE IF NOT EXISTS sync_meta (" +
                  "`key` TEXT NOT NULL PRIMARY KEY, " +
                  "value INTEGER NOT NULL)"
              )
            }

            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
          }
        )
        .build()

    val helper = FrameworkSQLiteOpenHelperFactory().create(configuration)
    helper.writableDatabase.use { db ->
      db.execSQL(
        "INSERT INTO clientes (id, nome, contrato, latitude, longitude, notas, dataCriacao, createdAt, updatedAt, pending, deletePending, revision) " +
          "VALUES ('1', 'Maria', 'CT-1', 1.0, 2.0, NULL, 1758000000, 1758000000, 1758000000, 0, 0, 0)"
      )
    }
    helper.close()
  }

  private fun createDatabaseV8WithClient() {
    val configuration =
      SupportSQLiteOpenHelper.Configuration.builder(context)
        .name(dbName)
        .callback(
          object : SupportSQLiteOpenHelper.Callback(8) {
            override fun onCreate(db: SupportSQLiteDatabase) {
              db.execSQL(
                "CREATE TABLE IF NOT EXISTS clientes (" +
                  "id TEXT NOT NULL PRIMARY KEY, " +
                  "nome TEXT NOT NULL, contrato TEXT, " +
                  "latitude REAL, longitude REAL, " +
                  "notas TEXT, dataCriacao INTEGER NOT NULL, " +
                  "createdAt INTEGER NOT NULL DEFAULT 0, " +
                  "updatedAt INTEGER NOT NULL, " +
                  "pending INTEGER NOT NULL DEFAULT 0, " +
                  "deletePending INTEGER NOT NULL DEFAULT 0)"
              )
              db.execSQL(
                "CREATE TABLE IF NOT EXISTS fotos (" +
                  "id TEXT NOT NULL PRIMARY KEY, " +
                  "clienteId TEXT NOT NULL, uriLocal TEXT NOT NULL, " +
                  "mime TEXT NOT NULL DEFAULT 'image/jpeg', " +
                  "ordem INTEGER NOT NULL DEFAULT 0, " +
                  "deletePending INTEGER NOT NULL DEFAULT 0, " +
                  "FOREIGN KEY(clienteId) REFERENCES clientes(id) " +
                  "ON UPDATE NO ACTION ON DELETE CASCADE)"
              )
              db.execSQL("CREATE INDEX IF NOT EXISTS index_fotos_clienteId ON fotos (clienteId)")
              db.execSQL(
                "CREATE TABLE IF NOT EXISTS sync_meta (" +
                  "`key` TEXT NOT NULL PRIMARY KEY, " +
                  "value INTEGER NOT NULL)"
              )
            }

            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
          }
        )
        .build()

    val helper = FrameworkSQLiteOpenHelperFactory().create(configuration)
    helper.writableDatabase.use { db ->
      db.execSQL(
        "INSERT INTO clientes (id, nome, contrato, latitude, longitude, notas, dataCriacao, createdAt, updatedAt, pending, deletePending) " +
          "VALUES ('1', 'Maria', 'CT-1', 1.0, 2.0, NULL, 1758000000, 1758000000, 1758000000, 1, 0)"
      )
    }
    helper.close()
  }

  private fun createDatabaseV7WithClient() {
    val configuration =
      SupportSQLiteOpenHelper.Configuration.builder(context)
        .name(dbName)
        .callback(
          object : SupportSQLiteOpenHelper.Callback(7) {
            override fun onCreate(db: SupportSQLiteDatabase) {
              db.execSQL(
                "CREATE TABLE IF NOT EXISTS clientes (" +
                  "id TEXT NOT NULL PRIMARY KEY, " +
                  "nome TEXT NOT NULL, contrato TEXT, " +
                  "latitude REAL, longitude REAL, " +
                  "notas TEXT, dataCriacao INTEGER NOT NULL, " +
                  "createdAt INTEGER NOT NULL DEFAULT 0, " +
                  "updatedAt INTEGER NOT NULL, " +
                  "pending INTEGER NOT NULL DEFAULT 0, " +
                  "deletePending INTEGER NOT NULL DEFAULT 0)"
              )
              db.execSQL(
                "CREATE TABLE IF NOT EXISTS fotos (" +
                  "id TEXT NOT NULL PRIMARY KEY, " +
                  "clienteId TEXT NOT NULL, uriLocal TEXT NOT NULL, " +
                  "mime TEXT NOT NULL DEFAULT 'image/jpeg', " +
                  "ordem INTEGER NOT NULL DEFAULT 0, " +
                  "deletePending INTEGER NOT NULL DEFAULT 0, " +
                  "FOREIGN KEY(clienteId) REFERENCES clientes(id) " +
                  "ON UPDATE NO ACTION ON DELETE CASCADE)"
              )
              db.execSQL("CREATE INDEX IF NOT EXISTS index_fotos_clienteId ON fotos (clienteId)")
              db.execSQL(
                "CREATE TABLE IF NOT EXISTS sync_meta (" +
                  "`key` TEXT NOT NULL PRIMARY KEY, " +
                  "value INTEGER NOT NULL)"
              )
            }

            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
          }
        )
        .build()

    val helper = FrameworkSQLiteOpenHelperFactory().create(configuration)
    helper.writableDatabase.use { db ->
      db.execSQL(
        "INSERT INTO clientes (id, nome, contrato, latitude, longitude, notas, dataCriacao, createdAt, updatedAt, pending, deletePending) " +
          "VALUES ('1', 'Maria', 'CT-1', 1.0, 2.0, NULL, 1758000000000, 1758000000000, 1758000000000, 0, 0)"
      )
      db.execSQL(
        "INSERT INTO sync_meta (`key`, value) VALUES ('last_pull_watermark', 1758000000000)"
      )
    }
    helper.close()
  }

  private fun createDatabaseV6WithClient() {
    val configuration =
      SupportSQLiteOpenHelper.Configuration.builder(context)
        .name(dbName)
        .callback(
          object : SupportSQLiteOpenHelper.Callback(6) {
            override fun onCreate(db: SupportSQLiteDatabase) {
              db.execSQL(
                "CREATE TABLE IF NOT EXISTS clientes (" +
                  "id TEXT NOT NULL PRIMARY KEY, " +
                  "nome TEXT NOT NULL, contrato TEXT, " +
                  "latitude REAL NOT NULL, longitude REAL NOT NULL, " +
                  "notas TEXT, dataCriacao INTEGER NOT NULL, " +
                  "createdAt INTEGER NOT NULL DEFAULT 0, " +
                  "updatedAt INTEGER NOT NULL, " +
                  "pending INTEGER NOT NULL DEFAULT 0, " +
                  "deletePending INTEGER NOT NULL DEFAULT 0)"
              )
              db.execSQL(
                "CREATE TABLE IF NOT EXISTS fotos (" +
                  "id TEXT NOT NULL PRIMARY KEY, " +
                  "clienteId TEXT NOT NULL, uriLocal TEXT NOT NULL, " +
                  "mime TEXT NOT NULL DEFAULT 'image/jpeg', " +
                  "ordem INTEGER NOT NULL DEFAULT 0, " +
                  "deletePending INTEGER NOT NULL DEFAULT 0, " +
                  "FOREIGN KEY(clienteId) REFERENCES clientes(id) " +
                  "ON UPDATE NO ACTION ON DELETE CASCADE)"
              )
              db.execSQL("CREATE INDEX IF NOT EXISTS index_fotos_clienteId ON fotos (clienteId)")
              db.execSQL(
                "CREATE TABLE IF NOT EXISTS sync_meta (" +
                  "`key` TEXT NOT NULL PRIMARY KEY, " +
                  "value INTEGER NOT NULL)"
              )
            }

            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
          }
        )
        .build()

    val helper = FrameworkSQLiteOpenHelperFactory().create(configuration)
    helper.writableDatabase.use { db ->
      db.execSQL(
        "INSERT INTO clientes (id, nome, contrato, latitude, longitude, notas, dataCriacao, createdAt, updatedAt, pending, deletePending) " +
          "VALUES ('1', 'Maria', 'CT-1', 1.0, 2.0, NULL, 1234, 1234, 1000, 0, 0)"
      )
    }
    helper.close()
  }

  private fun createDatabaseV5WithClient() {
    val configuration =
      SupportSQLiteOpenHelper.Configuration.builder(context)
        .name(dbName)
        .callback(
          object : SupportSQLiteOpenHelper.Callback(5) {
            override fun onCreate(db: SupportSQLiteDatabase) {
              db.execSQL(
                "CREATE TABLE IF NOT EXISTS clientes (" +
                  "id TEXT NOT NULL PRIMARY KEY, " +
                  "nome TEXT NOT NULL, contrato TEXT, " +
                  "latitude REAL NOT NULL, longitude REAL NOT NULL, " +
                  "notas TEXT, dataCriacao INTEGER NOT NULL, " +
                  "createdAt INTEGER NOT NULL DEFAULT 0, " +
                  "updatedAt INTEGER NOT NULL, " +
                  "pending INTEGER NOT NULL DEFAULT 0, " +
                  "deletePending INTEGER NOT NULL DEFAULT 0)"
              )
              db.execSQL(
                "CREATE TABLE IF NOT EXISTS fotos (" +
                  "id TEXT NOT NULL PRIMARY KEY, " +
                  "clienteId TEXT NOT NULL, uriLocal TEXT NOT NULL, " +
                  "mime TEXT NOT NULL DEFAULT 'image/jpeg', " +
                  "ordem INTEGER NOT NULL DEFAULT 0, " +
                  "deletePending INTEGER NOT NULL DEFAULT 0, " +
                  "FOREIGN KEY(clienteId) REFERENCES clientes(id) " +
                  "ON UPDATE NO ACTION ON DELETE CASCADE)"
              )
              db.execSQL("CREATE INDEX IF NOT EXISTS index_fotos_clienteId ON fotos (clienteId)")
            }

            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
          }
        )
        .build()

    val helper = FrameworkSQLiteOpenHelperFactory().create(configuration)
    helper.writableDatabase.use { db ->
      db.execSQL(
        "INSERT INTO clientes (id, nome, contrato, latitude, longitude, notas, dataCriacao, createdAt, updatedAt, pending, deletePending) " +
          "VALUES ('1', 'Maria', 'CT-1', 1.0, 2.0, NULL, 1234, 1234, 1000, 0, 0)"
      )
    }
    helper.close()
  }

  private fun createDatabaseV4WithClient() {
    val configuration =
      SupportSQLiteOpenHelper.Configuration.builder(context)
        .name(dbName)
        .callback(
          object : SupportSQLiteOpenHelper.Callback(4) {
            override fun onCreate(db: SupportSQLiteDatabase) {
              db.execSQL(
                "CREATE TABLE IF NOT EXISTS clientes (" +
                  "id TEXT NOT NULL PRIMARY KEY, " +
                  "nome TEXT NOT NULL, contrato TEXT, " +
                  "latitude REAL NOT NULL, longitude REAL NOT NULL, " +
                  "notas TEXT, dataCriacao INTEGER NOT NULL, " +
                  "updatedAt INTEGER NOT NULL, " +
                  "pending INTEGER NOT NULL DEFAULT 0, " +
                  "deletePending INTEGER NOT NULL DEFAULT 0)"
              )
              db.execSQL(
                "CREATE TABLE IF NOT EXISTS fotos (" +
                  "id TEXT NOT NULL PRIMARY KEY, " +
                  "clienteId TEXT NOT NULL, uriLocal TEXT NOT NULL, " +
                  "mime TEXT NOT NULL DEFAULT 'image/jpeg', " +
                  "ordem INTEGER NOT NULL DEFAULT 0, " +
                  "deletePending INTEGER NOT NULL DEFAULT 0, " +
                  "FOREIGN KEY(clienteId) REFERENCES clientes(id) " +
                  "ON UPDATE NO ACTION ON DELETE CASCADE)"
              )
              db.execSQL("CREATE INDEX IF NOT EXISTS index_fotos_clienteId ON fotos (clienteId)")
            }

            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
          }
        )
        .build()

    val helper = FrameworkSQLiteOpenHelperFactory().create(configuration)
    helper.writableDatabase.use { db ->
      db.execSQL(
        "INSERT INTO clientes (id, nome, contrato, latitude, longitude, notas, dataCriacao, updatedAt, pending, deletePending) " +
          "VALUES ('1', 'Maria', 'CT-1', 1.0, 2.0, NULL, 1234, 1000, 0, 0)"
      )
    }
    helper.close()
  }

  private fun createDatabaseV3WithClientAndFoto() {
    val configuration =
      SupportSQLiteOpenHelper.Configuration.builder(context)
        .name(dbName)
        .callback(
          object : SupportSQLiteOpenHelper.Callback(3) {
            override fun onCreate(db: SupportSQLiteDatabase) {
              db.execSQL(
                "CREATE TABLE IF NOT EXISTS clientes (" +
                  "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                  "nome TEXT NOT NULL, contrato TEXT, " +
                  "latitude REAL NOT NULL, longitude REAL NOT NULL, " +
                  "notas TEXT, dataCriacao INTEGER NOT NULL)"
              )
              db.execSQL(
                "CREATE TABLE IF NOT EXISTS fotos (" +
                  "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                  "clienteId INTEGER NOT NULL, uriLocal TEXT NOT NULL, " +
                  "FOREIGN KEY(clienteId) REFERENCES clientes(id) " +
                  "ON UPDATE NO ACTION ON DELETE CASCADE)"
              )
              db.execSQL("CREATE INDEX IF NOT EXISTS index_fotos_clienteId ON fotos (clienteId)")
            }

            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
          }
        )
        .build()

    val helper = FrameworkSQLiteOpenHelperFactory().create(configuration)
    helper.writableDatabase.use { db ->
      db.execSQL(
        "INSERT INTO clientes (nome, contrato, latitude, longitude, notas, dataCriacao) " +
          "VALUES ('Maria', 'CT-1', 1.0, 2.0, NULL, 1000)"
      )
      db.execSQL(
        "INSERT INTO fotos (clienteId, uriLocal) VALUES (1, '/x.jpg')"
      )
    }
    helper.close()
  }

  private fun createDatabaseV1WithClient() {
    val configuration =
      SupportSQLiteOpenHelper.Configuration.builder(context)
        .name(dbName)
        .callback(
          object : SupportSQLiteOpenHelper.Callback(1) {
            override fun onCreate(db: SupportSQLiteDatabase) {
              db.execSQL(
                "CREATE TABLE IF NOT EXISTS clientes (" +
                  "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                  "nome TEXT NOT NULL, telefone TEXT, " +
                  "latitude REAL NOT NULL, longitude REAL NOT NULL, " +
                  "notas TEXT, dataCriacao INTEGER NOT NULL)"
              )
              db.execSQL(
                "CREATE TABLE IF NOT EXISTS fotos (" +
                  "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                  "clienteId INTEGER NOT NULL, uriLocal TEXT NOT NULL, " +
                  "FOREIGN KEY(clienteId) REFERENCES clientes(id) " +
                  "ON UPDATE NO ACTION ON DELETE CASCADE)"
              )
              db.execSQL("CREATE INDEX IF NOT EXISTS index_fotos_clienteId ON fotos (clienteId)")
            }

            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
          }
        )
        .build()

    val helper = FrameworkSQLiteOpenHelperFactory().create(configuration)
    helper.writableDatabase.use { db ->
      db.execSQL(
        "INSERT INTO clientes (nome, telefone, latitude, longitude, notas, dataCriacao) " +
          "VALUES ('Maria', NULL, 1.0, 2.0, NULL, 1000)"
      )
    }
    helper.close()
  }

  private fun createDatabaseV2WithClient() {
    val configuration =
      SupportSQLiteOpenHelper.Configuration.builder(context)
        .name(dbName)
        .callback(
          object : SupportSQLiteOpenHelper.Callback(2) {
            override fun onCreate(db: SupportSQLiteDatabase) {
              db.execSQL(
                "CREATE TABLE IF NOT EXISTS clientes (" +
                  "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                  "nome TEXT NOT NULL, telefone TEXT, contrato TEXT, " +
                  "latitude REAL NOT NULL, longitude REAL NOT NULL, " +
                  "notas TEXT, dataCriacao INTEGER NOT NULL)"
              )
              db.execSQL(
                "CREATE TABLE IF NOT EXISTS fotos (" +
                  "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                  "clienteId INTEGER NOT NULL, uriLocal TEXT NOT NULL, " +
                  "FOREIGN KEY(clienteId) REFERENCES clientes(id) " +
                  "ON UPDATE NO ACTION ON DELETE CASCADE)"
              )
              db.execSQL("CREATE INDEX IF NOT EXISTS index_fotos_clienteId ON fotos (clienteId)")
            }

            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
          }
        )
        .build()

    val helper = FrameworkSQLiteOpenHelperFactory().create(configuration)
    helper.writableDatabase.use { db ->
      db.execSQL(
        "INSERT INTO clientes (nome, telefone, contrato, latitude, longitude, notas, dataCriacao) " +
          "VALUES ('Maria', NULL, 'CT-1', 1.0, 2.0, NULL, 1000)"
      )
      db.execSQL("INSERT INTO fotos (clienteId, uriLocal) VALUES (1, '/x.jpg')")
    }
    helper.close()
  }
}
