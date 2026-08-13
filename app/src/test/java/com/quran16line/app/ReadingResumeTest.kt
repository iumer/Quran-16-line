package com.quran16line.app

import com.quran16line.app.data.HighlightPoint
import com.quran16line.app.viewmodel.resumeReaderPage
import org.junit.Assert.assertEquals
import org.junit.Test

class ReadingResumeTest {
    @Test
    fun prefersHighlightPageOverLastScrolledPage() {
        val highlight = HighlightPoint(page = 221, lineIndex = 4)
        assertEquals(221, resumeReaderPage(lastPage = 3, highlight = highlight, pageCount = 559))
    }

    @Test
    fun fallsBackToLastPageWhenNoHighlight() {
        assertEquals(358, resumeReaderPage(lastPage = 358, highlight = null, pageCount = 559))
    }

    @Test
    fun clampsOutOfRangeHighlightPage() {
        val highlight = HighlightPoint(page = 9999, lineIndex = 0)
        assertEquals(559, resumeReaderPage(lastPage = 3, highlight = highlight, pageCount = 559))
    }
}
