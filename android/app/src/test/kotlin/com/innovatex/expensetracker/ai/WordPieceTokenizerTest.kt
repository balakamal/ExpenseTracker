package com.innovatex.expensetracker.ai

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class WordPieceTokenizerTest {

    @TempDir
    lateinit var tempDir: File

    private lateinit var vocabFile: File
    private lateinit var tokenizer: WordPieceTokenizer

    @BeforeEach
    fun setup() {
        vocabFile = File(tempDir, "vocab.txt")
        // Build a mock vocabulary matching standard BERT tokens
        val sampleVocab = listOf(
            "[PAD]", // 0
            "[UNUSED_1]", // 1
            "[UNUSED_2]", // 2
            "[CLS]", // 101 - we'll populate lines up to standard indices
        )

        vocabFile.printWriter().use { out ->
            for (i in 0 until 100) {
                out.println("[UNUSED_$i]")
            }
            out.println("[UNK]") // 100
            out.println("[CLS]") // 101
            out.println("[SEP]") // 102
            out.println("[MASK]") // 103

            // Common words & subwords
            out.println("inr")
            out.println("debited")
            out.println("credited")
            out.println("swiggy")
            out.println("zomato")
            out.println("amazon")
            out.println("##mart")
            out.println("upi")
            out.println("ref")
            out.println("paid")
            out.println("bill")
            out.println("due")
        }

        tokenizer = WordPieceTokenizer(vocabFile)
    }

    @Test
    fun `test tokenizer initialization`() {
        assertTrue(tokenizer.isInitialized)
    }

    @Test
    fun `test encoding returns exact 128-length tensors`() {
        val text = "INR 450 debited at Swiggy"
        val (inputIds, attentionMask) = tokenizer.encode(text)

        assertEquals(128, inputIds.size)
        assertEquals(128, attentionMask.size)

        // First token must be [CLS] (101)
        assertEquals(101L, inputIds[0])
        assertEquals(1L, attentionMask[0])

        // There should be a [SEP] (102) after the tokens
        val sepIndex = inputIds.indexOf(102L)
        assertTrue(sepIndex > 0)
        assertEquals(1L, attentionMask[sepIndex])

        // After [SEP], everything must be [PAD] (0)
        assertEquals(0L, inputIds[sepIndex + 1])
        assertEquals(0L, attentionMask[sepIndex + 1])
    }

    @Test
    fun `test subword tokenization with double hash`() {
        // "swiggymart" -> "swiggy" + "##mart"
        val text = "swiggymart"
        val (inputIds, _) = tokenizer.encode(text)

        // Index 0 is [CLS] (101), Index 1 is swiggy, Index 2 is ##mart
        assertTrue(inputIds[1] > 103L)
        assertTrue(inputIds[2] > 103L)
    }

    @Test
    fun `test unknown words fallback to UNK token`() {
        val text = "xyz123randomunregisteredword"
        val (inputIds, _) = tokenizer.encode(text)

        // Token 1 should be UNK (100)
        assertEquals(100L, inputIds[1])
    }
}

