package com.callbackdev.thatstheword.core.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.callbackdev.thatstheword.core.data.db.ThatsTheWordDatabase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The database is a contract (PLANNING.md §5): the schema committed in `core/data/schemas/` is the
 * one the code opens. Every migration adds its test here, and the upgrade from the first release's
 * schema (version 1) is tested from it.
 */
@RunWith(AndroidJUnit4::class)
class SchemaTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), ThatsTheWordDatabase::class.java)

    @Test
    fun `the first schema is the code's`() {
        helper.createDatabase(NAME, 1).close()
        helper.runMigrationsAndValidate(NAME, 1, true).close()
    }

    private companion object {
        const val NAME = "schema-test.db"
    }
}
