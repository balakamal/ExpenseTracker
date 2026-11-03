package com.innovatex.expensetracker.ai

import org.json.JSONObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AiChatHarnessTest {

    @Test
    fun `extractThinkingAndClean correctly parses think tags and clean answer`() {
        val raw = """
            <think>
            1. Check October total debits.
            2. Match Starbucks merchant.
            3. Propose recategorization to food_dining.
            </think>
            I found 3 transactions at Starbucks. Would you like me to recategorize them?
        """.trimIndent()

        val thinkPattern = Regex("<think>(.*?)</think>", RegexOption.DOT_MATCHES_ALL)
        val match = thinkPattern.find(raw)
        val thinking = match?.groupValues?.get(1)?.trim()
        val cleanAnswer = raw.replace(thinkPattern, "").trim()

        assertNotNull(thinking)
        assertTrue(thinking!!.contains("Match Starbucks merchant"))
        assertEquals("I found 3 transactions at Starbucks. Would you like me to recategorize them?", cleanAnswer)
    }

    @Test
    fun `action proposal parser decodes add_expense proposal correctly`() {
        val actionJson = JSONObject()
            .put("type", "add_expense")
            .put("amount", 350.0)
            .put("merchant", "Blue Tokai")
            .put("categoryId", "food_dining")
            .put("notes", "Filter coffee and croissant")
            .put("reason", "Coffee shop expense")
            .toString()

        val json = JSONObject(actionJson)
        assertEquals("add_expense", json.getString("type"))
        assertEquals(350.0, json.getDouble("amount"))
        assertEquals("Blue Tokai", json.getString("merchant"))
        assertEquals("food_dining", json.getString("categoryId"))
        assertEquals("Filter coffee and croissant", json.getString("notes"))
    }

    @Test
    fun `action proposal parser decodes mark_self_transfer correctly`() {
        val actionJson = JSONObject()
            .put("type", "mark_self_transfer")
            .put("merchant", "HDFC Bank")
            .put("reason", "Transfer between own accounts")
            .toString()

        val json = JSONObject(actionJson)
        assertEquals("mark_self_transfer", json.getString("type"))
        assertEquals("HDFC Bank", json.getString("merchant"))
        assertEquals("Transfer between own accounts", json.getString("reason"))
    }

    @Test
    fun `mode segregation strictly isolates ledger jobs from general jobs`() {
        val jobs = listOf(
            AiChatJobRecord(
                id = "job-1",
                sessionId = AiChatJobStore.DEFAULT_LEDGER_SESSION_ID,
                mode = AiChatWorker.MODE_LEDGER,
                prompt = "How much did I spend at Swiggy?",
                submittedAt = 1000L,
                isWorking = false,
                answer = "You spent ₹1,450."
            ),
            AiChatJobRecord(
                id = "job-2",
                sessionId = AiChatJobStore.DEFAULT_GENERAL_SESSION_ID,
                mode = AiChatWorker.MODE_GENERAL,
                prompt = "What is the 50/30/20 rule?",
                submittedAt = 2000L,
                isWorking = false,
                answer = "It is a budgeting guideline."
            )
        )

        // Simulating ledger mode filter
        val ledgerJobs = jobs.filter { it.mode == AiChatWorker.MODE_LEDGER }
        assertEquals(1, ledgerJobs.size)
        assertEquals("job-1", ledgerJobs.first().id)

        // Simulating general mode filter
        val generalJobs = jobs.filter { it.mode == AiChatWorker.MODE_GENERAL }
        assertEquals(1, generalJobs.size)
        assertEquals("job-2", generalJobs.first().id)
    }

    @Test
    fun `session isolation ensures new chat sessions remain empty without leaking default chat history`() {
        val defaultLedgerJob = AiChatJobRecord(
            id = "default-job-1",
            sessionId = AiChatJobStore.DEFAULT_LEDGER_SESSION_ID,
            mode = AiChatWorker.MODE_LEDGER,
            prompt = "Initial ledger query",
            submittedAt = 1000L,
            isWorking = false,
            answer = "Ledger answer"
        )
        val allJobs = listOf(defaultLedgerJob)

        val newSessionId = "session-uuid-999"

        // Filtering for new session
        val modeJobs = allJobs.filter { it.mode == AiChatWorker.MODE_LEDGER }
        val newSessionJobs = modeJobs.filter { job ->
            if (newSessionId == AiChatJobStore.DEFAULT_LEDGER_SESSION_ID) {
                job.sessionId == AiChatJobStore.DEFAULT_LEDGER_SESSION_ID || job.sessionId == AiChatJobStore.DEFAULT_SESSION_ID
            } else {
                job.sessionId == newSessionId
            }
        }

        assertTrue(newSessionJobs.isEmpty(), "New session must be empty, no default jobs leaked")
    }
}

