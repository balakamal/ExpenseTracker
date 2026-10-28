package com.innovatex.expensetracker.ai

import java.io.File

class WordPieceTokenizer(vocabFile: File) {
    private val vocab = mutableMapOf<String, Long>()
    private val maxSeqLength = 128
    private val padId = 0L
    private val unkId = 100L
    private val clsId = 101L
    private val sepId = 102L

    init {
        if (vocabFile.exists()) {
            vocabFile.inputStream().bufferedReader().useLines { lines ->
                lines.forEachIndexed { index, line ->
                    val token = line.trim()
                    if (token.isNotEmpty()) {
                        vocab[token] = index.toLong()
                    }
                }
            }
        }
    }

    val isInitialized: Boolean
        get() = vocab.isNotEmpty()

    fun encode(text: String): Pair<LongArray, LongArray> {
        val clean = text.lowercase().replace(Regex("[^a-z0-9\\s₹$€£.,-]"), " ")
        val words = clean.split("\\s+".toRegex()).filter { it.isNotBlank() }

        val inputIds = mutableListOf<Long>()
        inputIds.add(clsId)

        for (word in words) {
            val subTokens = tokenizeWord(word)
            for (sub in subTokens) {
                if (inputIds.size >= maxSeqLength - 1) break
                inputIds.add(sub)
            }
            if (inputIds.size >= maxSeqLength - 1) break
        }
        inputIds.add(sepId)

        val attentionMask = MutableList(inputIds.size) { 1L }

        while (inputIds.size < maxSeqLength) {
            inputIds.add(padId)
            attentionMask.add(0L)
        }

        return Pair(inputIds.toLongArray(), attentionMask.toLongArray())
    }

    private fun tokenizeWord(word: String): List<Long> {
        val tokens = mutableListOf<Long>()
        var start = 0
        while (start < word.length) {
            var end = word.length
            var matchedToken: Long? = null
            var matchedEnd = -1

            while (start < end) {
                val substr = if (start == 0) word.substring(start, end) else "##" + word.substring(start, end)
                if (vocab.containsKey(substr)) {
                    matchedToken = vocab[substr]
                    matchedEnd = end
                    break
                }
                end--
            }

            if (matchedToken == null) {
                tokens.add(unkId)
                break
            } else {
                tokens.add(matchedToken)
                start = matchedEnd
            }
        }
        return tokens
    }
}

