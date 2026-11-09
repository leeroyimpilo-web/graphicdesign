package za.co.graphicdesign.pageharvest

import org.junit.Assert.assertEquals
import org.junit.Test

class PaginationModeTest {
    @Test fun detectsSavedModes() {
        assertEquals(PaginationMode.SCROLL, PaginationMode.fromSaved("SCROLL"))
        assertEquals(PaginationMode.CSS_NEXT, PaginationMode.fromSaved("CSS_NEXT"))
        assertEquals(PaginationMode.AUTO, PaginationMode.fromSaved("unknown"))
    }
}
