package edu.console

import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DuplicatesTest {
    @Test
    fun groupsOnlyEqualJsonBytesAcrossDirectories() = runBlocking {
        val directory = Files.createTempDirectory("json-duplicates").toFile()
        try {
            directory.resolve("a.json").writeText("{\"a\":1}")
            directory.resolve("nested").mkdir()
            directory.resolve("nested/b.JSON").writeText("{\"a\":1}")
            directory.resolve("different.json").writeText("{\"a\":2}")
            directory.resolve("not-json.txt").writeText("{\"a\":1}")
            val groups = assertNotNull(duplicates(directory, 10)).values
            assertEquals(1, groups.size)
            assertEquals(setOf("a.json", "b.JSON"), groups.single().map { it.name }.toSet())
            assertNull(duplicates(directory, 0))
        } finally {
            directory.deleteRecursively()
        }
    }
}
