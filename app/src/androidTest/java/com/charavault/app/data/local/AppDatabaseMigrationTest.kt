package com.charavault.app.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val databaseName = "favorite-removal-migration-test.db"
    private val openHelpers = mutableListOf<SupportSQLiteOpenHelper>()

    @Before
    fun setUp() {
        context.deleteDatabase(databaseName)
    }

    @After
    fun tearDown() {
        openHelpers.forEach(SupportSQLiteOpenHelper::close)
        context.deleteDatabase(databaseName)
    }

    @Test
    fun migrationFrom2To4_preservesCardRemovesFavoriteAndAddsCategoryOrder() {
        createVersion2Database()

        val migratedDatabase = openVersion4Database()
        val columns = migratedDatabase.query("PRAGMA table_info(`cards`)").use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            buildSet {
                while (cursor.moveToNext()) add(cursor.getString(nameIndex))
            }
        }

        assertFalse(columns.contains("isFavorite"))
        assertTrue(columns.contains("tagsJson"))
        assertTrue(columns.contains("categorySortOrdersJson"))

        migratedDatabase.query(
            "SELECT `name`, `tagsJson`, `sortOrder`, `categorySortOrdersJson` FROM `cards` WHERE `id` = 'card-1'"
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Alice", cursor.getString(0))
            assertEquals("[\"冒险\"]", cursor.getString(1))
            assertEquals(7, cursor.getInt(2))
            assertEquals("{}", cursor.getString(3))
        }
    }

    private fun createVersion2Database() {
        val helper = createOpenHelper(
            version = 2,
            onCreate = { db ->
                db.execSQL(CREATE_VERSION_2_CARDS_TABLE)
                db.execSQL("CREATE INDEX `index_cards_fileHash` ON `cards` (`fileHash`)")
                db.execSQL("CREATE INDEX `index_cards_semanticHash` ON `cards` (`semanticHash`)")
                db.execSQL(INSERT_VERSION_2_CARD)
            }
        )
        helper.writableDatabase.close()
    }

    private fun openVersion4Database(): SupportSQLiteDatabase {
        val helper = createOpenHelper(
            version = 4,
            onCreate = { error("Version 2 database should already exist") },
            onUpgrade = { db, oldVersion, newVersion ->
                assertEquals(2, oldVersion)
                assertEquals(4, newVersion)
                MIGRATION_2_3.migrate(db)
                MIGRATION_3_4.migrate(db)
            }
        )
        return helper.writableDatabase
    }

    private fun createOpenHelper(
        version: Int,
        onCreate: (SupportSQLiteDatabase) -> Unit,
        onUpgrade: (SupportSQLiteDatabase, Int, Int) -> Unit = { _, _, _ -> }
    ): SupportSQLiteOpenHelper {
        val callback = object : SupportSQLiteOpenHelper.Callback(version) {
            override fun onCreate(db: SupportSQLiteDatabase) = onCreate.invoke(db)

            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) =
                onUpgrade.invoke(db, oldVersion, newVersion)
        }
        return FrameworkSQLiteOpenHelperFactory()
            .create(
                SupportSQLiteOpenHelper.Configuration.builder(context)
                    .name(databaseName)
                    .callback(callback)
                    .build()
            )
            .also(openHelpers::add)
    }

    private companion object {
        const val CREATE_VERSION_2_CARDS_TABLE = """
            CREATE TABLE `cards` (
                `id` TEXT NOT NULL PRIMARY KEY,
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
                `isFavorite` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )
        """

        const val INSERT_VERSION_2_CARD = """
            INSERT INTO `cards` VALUES (
                'card-1', 'Alice', 'Author', 'Description', 'Personality', 'Scenario',
                'Hello', 'System', '["冒险"]', '[]', '{}', '/cards/alice.png',
                'file-hash', 'semantic-hash', 7, 1, 1000, 2000
            )
        """
    }
}
