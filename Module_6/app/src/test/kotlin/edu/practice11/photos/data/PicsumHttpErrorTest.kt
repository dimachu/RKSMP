package edu.practice11.photos.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PicsumHttpErrorTest {
    @Test
    fun explainsGeoblockOnlyWhenTheProviderReportsIt() {
        val message = picsumHttpMessage(403, "Geoblocked\n")
        assertTrue(message.contains("ограничение доступа", ignoreCase = true))
        assertTrue(message.contains("HTTP 403"))
        assertTrue(message.contains("другую сеть"))
        assertEquals("Picsum отклонил запрос (HTTP 403)", picsumHttpMessage(403, "Forbidden"))
    }

    @Test
    fun keepsOtherStatusesDistinct() {
        assertEquals("Picsum вернул HTTP 503", picsumHttpMessage(503, "Geoblocked"))
    }
}
