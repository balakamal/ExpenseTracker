package com.innovatex.expensetracker.data.remote

import kotlinx.coroutines.Dispatchers
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GithubReleaseCheckerTest {

    private lateinit var checker: GithubReleaseChecker

    @BeforeEach
    fun setup() {
        checker = GithubReleaseChecker(Dispatchers.Unconfined)
    }

    @Test
    fun `test newer patch version detected`() {
        assertTrue(checker.isNewerVersion(remote = "1.0.1", local = "1.0.0"))
    }

    @Test
    fun `test newer minor version detected`() {
        assertTrue(checker.isNewerVersion(remote = "1.1.0", local = "1.0.9"))
    }

    @Test
    fun `test newer major version detected`() {
        assertTrue(checker.isNewerVersion(remote = "2.0.0", local = "1.9.9"))
    }

    @Test
    fun `test identical version returns false`() {
        assertFalse(checker.isNewerVersion(remote = "1.0.0", local = "1.0.0"))
    }

    @Test
    fun `test older remote version returns false`() {
        assertFalse(checker.isNewerVersion(remote = "1.0.0", local = "1.0.1"))
    }

    @Test
    fun `test version with prerelease suffix parsed correctly`() {
        assertTrue(checker.isNewerVersion(remote = "1.1.0-rc1", local = "1.0.0"))
    }

    @Test
    fun `test monotonic build run numbers detected correctly`() {
        assertTrue(checker.isNewerVersion(remote = "1.0.12", local = "1.0.2"))
        assertTrue(checker.isNewerVersion(remote = "1.0.5", local = "1.0.4"))
        assertTrue(checker.isNewerVersion(remote = "1.0.100", local = "1.0.99"))
        assertFalse(checker.isNewerVersion(remote = "1.0.10", local = "1.0.10"))
        assertFalse(checker.isNewerVersion(remote = "1.0.2", local = "1.0.12"))
    }
}
