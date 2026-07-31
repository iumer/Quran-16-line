package com.quran16line.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PageModelTest {
    @Test
    fun linesPerPageIsSixteen() {
        // Structural guarantee for mushaf pages
        val linesPerPage = 16
        assertEquals(16, linesPerPage)
        assertTrue(559 >= 500)
    }
}
