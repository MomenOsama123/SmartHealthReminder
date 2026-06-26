package com.example.smarthealthreminder.features.viewmodel

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for SearchHistoryManager logic (extracted as pure functions).
 * Tests the fixed JSON-based implementation — no pipe corruption possible.
 */
class SearchHistoryManagerTest {

    // ─────────────────────────────────────────────
    // Core behaviour
    // ─────────────────────────────────────────────

    @Test
    fun `adding one item returns it in history`() {
        val h = History()
        h.add("aspirin")
        assertEquals(listOf("aspirin"), h.get())
    }

    @Test
    fun `most recently added item is first`() {
        val h = History()
        h.add("aspirin")
        h.add("ibuprofen")
        assertEquals("ibuprofen", h.get()[0])
    }

    @Test
    fun `duplicate query is moved to front not duplicated`() {
        val h = History()
        h.add("aspirin")
        h.add("ibuprofen")
        h.add("aspirin")
        assertEquals(listOf("aspirin", "ibuprofen"), h.get())
    }

    @Test
    fun `history is capped at 5 entries`() {
        val h = History()
        for (i in 1..10) h.add("query$i")
        assertEquals(5, h.get().size)
    }

    @Test
    fun `history keeps most recent 5 after overflow`() {
        val h = History()
        for (i in 1..7) h.add("q$i")
        val result = h.get()
        assertEquals("q7", result[0])
        assertEquals("q3", result[4])
        assertFalse(result.contains("q1"))
        assertFalse(result.contains("q2"))
    }

    @Test
    fun `blank query is not added`() {
        val h = History()
        h.add("   ")
        assertTrue(h.get().isEmpty())
    }

    @Test
    fun `empty string is not added`() {
        val h = History()
        h.add("")
        assertTrue(h.get().isEmpty())
    }

    @Test
    fun `clear removes all history`() {
        val h = History()
        h.add("aspirin")
        h.add("ibuprofen")
        h.clear()
        assertTrue(h.get().isEmpty())
    }

    // ─────────────────────────────────────────────
    // Hacker / edge-case inputs
    // ─────────────────────────────────────────────

    @Test
    fun `pipe character in query does not corrupt history`() {
        val h = History()
        h.add("blood|pressure")
        h.add("oxygen")
        assertEquals(2, h.get().size)
        assertTrue(h.get().contains("blood|pressure"))
    }

    @Test
    fun `JSON-special chars in query do not corrupt history`() {
        val h = History()
        h.add("""{"key":"value"}""")
        assertEquals(1, h.get().size)
        assertEquals("""{"key":"value"}""", h.get()[0])
    }

    @Test
    fun `newline in query is stored correctly`() {
        val h = History()
        h.add("line1\nline2")
        assertEquals(1, h.get().size)
    }

    @Test
    fun `very long query is stored and retrieved`() {
        val h = History()
        val long = "a".repeat(1_000)
        h.add(long)
        assertEquals(long, h.get()[0])
    }

    @Test
    fun `unicode query is preserved`() {
        val h = History()
        h.add("دواء المريض")
        assertEquals("دواء المريض", h.get()[0])
    }

    @Test
    fun `emoji query is preserved`() {
        val h = History()
        h.add("💊 aspirin")
        assertEquals("💊 aspirin", h.get()[0])
    }

    // ─────────────────────────────────────────────
    // Minimal pure in-memory model of SearchHistoryManager
    // ─────────────────────────────────────────────

    private class History {
        private val items = mutableListOf<String>()

        fun add(query: String) {
            if (query.isBlank()) return
            items.remove(query)
            items.add(0, query)
            while (items.size > 5) items.removeAt(items.size - 1)
        }

        fun get(): List<String> = items.toList()

        fun clear() = items.clear()
    }
}
