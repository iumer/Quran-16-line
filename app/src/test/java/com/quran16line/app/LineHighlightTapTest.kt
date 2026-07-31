package com.quran16line.app

import com.quran16line.app.ui.reader.lineIndexForTap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LineHighlightTapTest {
    @Test
    fun mapsTapInsideTextBandToLine() {
        // With default insets 0.105 top / 0.065 bottom on height 1000:
        // text runs from 105..935 → band height 51.875
        assertEquals(0, lineIndexForTap(tapY = 110f, pageHeight = 1000f, linesPerPage = 16))
        assertEquals(15, lineIndexForTap(tapY = 930f, pageHeight = 1000f, linesPerPage = 16))
    }

    @Test
    fun ignoresTapsOnHeaderAndFooter() {
        assertNull(lineIndexForTap(tapY = 20f, pageHeight = 1000f, linesPerPage = 16))
        assertNull(lineIndexForTap(tapY = 980f, pageHeight = 1000f, linesPerPage = 16))
    }
}
