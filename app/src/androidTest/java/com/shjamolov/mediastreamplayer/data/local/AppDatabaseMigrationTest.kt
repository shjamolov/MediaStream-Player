package com.shjamolov.mediastreamplayer.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.shjamolov.mediastreamplayer.data.local.migration.MIGRATION_1_2
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
    )

    @Test
    fun migrate1To2_replacesBootstrapWithProductionTables() {
        helper.createDatabase(TEST_DATABASE, 1).use { database ->
            database.execSQL(
                "INSERT INTO bootstrap_records (id, schemaVersion) VALUES (1, 1)",
            )
        }

        helper.runMigrationsAndValidate(
            TEST_DATABASE,
            2,
            true,
            MIGRATION_1_2,
        ).close()
    }

    private companion object {
        const val TEST_DATABASE = "migration-test"
    }
}
