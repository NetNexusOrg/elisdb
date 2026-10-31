package com.example.elisdb.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
  entities = [Cliente::class, Foto::class, SyncMeta::class],
  version = 10,
  exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
  abstract fun clienteDao(): ClienteDao

  abstract fun fotoDao(): FotoDao

  abstract fun syncMetaDao(): SyncMetaDao

  companion object {
    val MIGRATION_1_2: Migration =
      object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL("ALTER TABLE clientes ADD COLUMN contrato TEXT")
        }
      }

    val MIGRATION_2_3: Migration =
      object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
          // `ALTER TABLE ... DROP COLUMN` needs SQLite 3.35 (Android 14) and the
          // app supports API 24, so rebuild the table instead (same pattern as
          // 3→4). The supported SQLite versions only see create + copy + rename.
          db.execSQL(
            """
            CREATE TABLE clientes_new (
              id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
              nome TEXT NOT NULL,
              contrato TEXT,
              latitude REAL NOT NULL,
              longitude REAL NOT NULL,
              notas TEXT,
              dataCriacao INTEGER NOT NULL
            )
            """
          )
          db.execSQL(
            """
            INSERT INTO clientes_new (id, nome, contrato, latitude, longitude, notas, dataCriacao)
            SELECT id, nome, contrato, latitude, longitude, notas, dataCriacao FROM clientes
            """
          )
          db.execSQL("DROP TABLE clientes")
          db.execSQL("ALTER TABLE clientes_new RENAME TO clientes")
        }
      }

    val MIGRATION_3_4: Migration =
      object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL(
            """
            CREATE TABLE clientes_new (
              id TEXT NOT NULL PRIMARY KEY,
              nome TEXT NOT NULL,
              contrato TEXT,
              latitude REAL NOT NULL,
              longitude REAL NOT NULL,
              notas TEXT,
              dataCriacao INTEGER NOT NULL,
              updatedAt INTEGER NOT NULL,
              pending INTEGER NOT NULL DEFAULT 0,
              deletePending INTEGER NOT NULL DEFAULT 0
            )
            """
          )
          db.execSQL(
            """
            INSERT INTO clientes_new (id, nome, contrato, latitude, longitude, notas, dataCriacao, updatedAt, pending, deletePending)
            SELECT id, nome, contrato, latitude, longitude, notas, dataCriacao, dataCriacao, 0, 0 FROM clientes
            """
          )
          db.execSQL("DROP TABLE clientes")
          db.execSQL("ALTER TABLE clientes_new RENAME TO clientes")

          db.execSQL(
            """
            CREATE TABLE fotos_new (
              id TEXT NOT NULL PRIMARY KEY,
              clienteId TEXT NOT NULL,
              uriLocal TEXT NOT NULL,
              mime TEXT NOT NULL DEFAULT 'image/jpeg',
              ordem INTEGER NOT NULL DEFAULT 0,
              deletePending INTEGER NOT NULL DEFAULT 0,
              FOREIGN KEY(clienteId) REFERENCES clientes(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """
          )
          db.execSQL(
            """
            INSERT INTO fotos_new (id, clienteId, uriLocal, mime, ordem, deletePending)
            SELECT id, clienteId, uriLocal, 'image/jpeg', 0, 0 FROM fotos
            """
          )
          db.execSQL("DROP TABLE fotos")
          db.execSQL("ALTER TABLE fotos_new RENAME TO fotos")
          db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_fotos_clienteId ON fotos (clienteId)"
          )
        }
      }

    val MIGRATION_4_5: Migration =
      object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL("ALTER TABLE clientes ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
          db.execSQL(
            "UPDATE clientes SET createdAt = dataCriacao WHERE createdAt = 0"
          )
        }
      }

    val MIGRATION_5_6: Migration =
      object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL(
            "CREATE TABLE IF NOT EXISTS sync_meta (" +
              "`key` TEXT NOT NULL PRIMARY KEY, " +
              "value INTEGER NOT NULL)"
          )
        }
      }

    val MIGRATION_6_7: Migration =
      object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL(
            """
            CREATE TABLE clientes_new (
              id TEXT NOT NULL PRIMARY KEY,
              nome TEXT NOT NULL,
              contrato TEXT,
              latitude REAL,
              longitude REAL,
              notas TEXT,
              dataCriacao INTEGER NOT NULL,
              updatedAt INTEGER NOT NULL,
              createdAt INTEGER NOT NULL,
              pending INTEGER NOT NULL DEFAULT 0,
              deletePending INTEGER NOT NULL DEFAULT 0
            )
            """
          )
          db.execSQL(
            """
            INSERT INTO clientes_new (id, nome, contrato, latitude, longitude, notas, dataCriacao, updatedAt, createdAt, pending, deletePending)
            SELECT id, nome, contrato, latitude, longitude, notas, dataCriacao, updatedAt, createdAt, pending, deletePending FROM clientes
            """
          )
          db.execSQL("DROP TABLE clientes")
          db.execSQL("ALTER TABLE clientes_new RENAME TO clientes")
        }
      }

    val MIGRATION_7_8: Migration =
      object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
          // Normalize legacy millisecond timestamps to epoch seconds. Values
          // above 1e11 are milliseconds (epoch ~1.7e12 in 2026); second-scale
          // values (~1.7e9) are left untouched.
          db.execSQL(
            "UPDATE clientes SET updatedAt = updatedAt / 1000 WHERE updatedAt > 100000000000"
          )
          db.execSQL(
            "UPDATE clientes SET createdAt = createdAt / 1000 WHERE createdAt > 100000000000"
          )
          db.execSQL(
            "UPDATE clientes SET dataCriacao = dataCriacao / 1000 WHERE dataCriacao > 100000000000"
          )
          db.execSQL(
            "UPDATE sync_meta SET value = value / 1000 WHERE `key` = 'last_pull_watermark' AND value > 100000000000"
          )
        }
      }

    val MIGRATION_8_9: Migration =
      object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
          // Local write counter used by the push acknowledgement: `updatedAt` has
          // second resolution, so an edit made in the same second as the push
          // looked identical to the snapshot that was sent.
          db.execSQL("ALTER TABLE clientes ADD COLUMN revision INTEGER NOT NULL DEFAULT 0")
        }
      }

    val MIGRATION_9_10: Migration =
      object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
          // Active/inactive status. Existing clients default to active, so the
          // migration never changes what the user already sees.
          db.execSQL("ALTER TABLE clientes ADD COLUMN ativo INTEGER NOT NULL DEFAULT 1")
        }
      }
  }
}