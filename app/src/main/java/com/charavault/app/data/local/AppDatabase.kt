package com.charavault.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [CardEntity::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun cardDao(): CardDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "charavault_database"
                )
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

internal val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `cards_new` (
                `id` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `creator` TEXT NOT NULL,
                `description` TEXT NOT NULL,
                `personality` TEXT NOT NULL,
                `scenario` TEXT NOT NULL,
                `firstMes` TEXT NOT NULL,
                `systemPrompt` TEXT NOT NULL,
                `tagsJson` TEXT NOT NULL,
                `alternateGreetingsJson` TEXT NOT NULL,
                `rawJsonData` TEXT NOT NULL,
                `imagePath` TEXT NOT NULL,
                `fileHash` TEXT NOT NULL,
                `semanticHash` TEXT NOT NULL,
                `sortOrder` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO `cards_new` (
                `id`, `name`, `creator`, `description`, `personality`, `scenario`,
                `firstMes`, `systemPrompt`, `tagsJson`, `alternateGreetingsJson`,
                `rawJsonData`, `imagePath`, `fileHash`, `semanticHash`, `sortOrder`,
                `createdAt`, `updatedAt`
            )
            SELECT
                `id`, `name`, `creator`, `description`, `personality`, `scenario`,
                `firstMes`, `systemPrompt`, `tagsJson`, `alternateGreetingsJson`,
                `rawJsonData`, `imagePath`, `fileHash`, `semanticHash`, `sortOrder`,
                `createdAt`, `updatedAt`
            FROM `cards`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `cards`")
        db.execSQL("ALTER TABLE `cards_new` RENAME TO `cards`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_cards_fileHash` ON `cards` (`fileHash`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_cards_semanticHash` ON `cards` (`semanticHash`)")
    }
}

internal val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE `cards` ADD COLUMN `categorySortOrdersJson` TEXT NOT NULL DEFAULT '{}'"
        )
    }
}

internal val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `cards` ADD COLUMN `normalizedJsonHash` TEXT NOT NULL DEFAULT ''")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_cards_normalizedJsonHash` ON `cards` (`normalizedJsonHash`)")
    }
}
