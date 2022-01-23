package com.mrmannwood.wordle

import kotlin.math.min

sealed class GuessStrategy {
    companion object {
        fun getStrategies() = listOf(
            FirstGuessStrategy,
            FirstWordStrategy,
            CharacterEliminationStrategy,
            HighestCharacterFrequencyStrategy
        )
    }

    abstract val name: String

    abstract fun getBestGuesses(
        numGuesses: Int,
        words: List<String>,
        allWords: List<String>,
        eliminatedCharacters: Set<Char>,
        usedCharacters: Set<Char>
    ): List<String>

    object FirstGuessStrategy: GuessStrategy() {
        override val name = "First Guess"
        override fun getBestGuesses(
            numGuesses: Int,
            words: List<String>,
            allWords: List<String>,
            eliminatedCharacters: Set<Char>,
            usedCharacters: Set<Char>
        ): List<String> = if (words.size == allWords.size) listOf("tares") else emptyList()
    }

    object FirstWordStrategy: GuessStrategy() {
        override val name = "First Word"
        override fun getBestGuesses(
            numGuesses: Int,
            words: List<String>,
            allWords: List<String>,
            eliminatedCharacters: Set<Char>,
            usedCharacters: Set<Char>
        ): List<String> = if (words.size <= 2) words else emptyList()
    }

    /**
     * Analyze the remaining words and find the characters that aren't common, then choose a word from allWords that has the
     * most of them. This is handle the "shill, skill, spill" case, but it turns out it's also really effective generally
     */
    object CharacterEliminationStrategy: GuessStrategy() {

        override val name = "CharacterEliminationStrategy"

        override fun getBestGuesses(
            numGuesses: Int,
            words: List<String>,
            allWords: List<String>,
            eliminatedCharacters: Set<Char>,
            usedCharacters: Set<Char>
        ): List<String> {
            if (words.size == 2) return words
            val a = min(2, words.size / 10)

            val letters = Array(26) { 0 }
            words.forEach { word ->
                word.toCharArray().distinct().forEach { letter ->
                    letters[letter - 'a'] = letters[letter - 'a'] + 1
                }
            }

            var numZero = 0
            for (idx in letters.indices) {
                if (letters[idx] >= words.size - a) letters[idx] = 0
                if (letters[idx] == 0) numZero++
            }
            if (numZero == letters.size) return emptyList()

            return allWords.asSequence()
                .map { word ->
                    val wordLetters = word.toCharArray().distinct()
                    var count = 0
                    wordLetters.forEach { c ->
                        count += letters[c - 'a']
                    }
                    word to count
                }
                .sortedByDescending { it.second }
                .map { it.first }
                .take(numGuesses)
                .toList()
        }
    }

    /**
     * Analyzes the remaining words for the characters that occur the most often (ignoring duplicates) and chooses the word
     * with the most of them.
     * As a secondary step, analyzes the positions of the most common letters, and chooses a word where the most common
     * letters are in their most common positions.
     * One of my strategies was just this, but ignoring vowels (since they can be figured out from context clues, and the
     * first guess will always just be mostly vowels anyway), but it did significantly worse
     */
    object HighestCharacterFrequencyStrategy: GuessStrategy() {

        override val name = "HighestCharacterFrequencyStrategy"

        override fun getBestGuesses(
            numGuesses: Int,
            words: List<String>,
            allWords: List<String>,
            eliminatedCharacters: Set<Char>,
            usedCharacters: Set<Char>
        ): List<String> {
            val letters = mutableMapOf<Char, Int>().apply { for (c in 'a'..'z') put(c, 0) }
            val positions = mutableListOf<MutableMap<Char, Int>>().apply {
                for (idx in 0..4) add(mutableMapOf<Char, Int>().apply { for (c in 'a'..'z') put(c, 0) })
            }
            words.forEach { word ->
                word.toCharArray().distinct().forEach { letter -> letters[letter] = letters[letter]!! + 1 }
                word.toCharArray().forEachIndexed { idx, letter -> positions[idx][letter] = positions[idx][letter]!! + 1 }
            }

            var sorted = words.asSequence()
                .map { word ->
                    var total = 0
                    word.toCharArray().distinct().forEach { letter ->
                        total += letters[letter]!!
                    }
                    Pair(word, total)
                }
                .toList()
                .sortedByDescending { it.second }

            var minScore = sorted.first().second - ((sorted.first().second - sorted.last().second) / 10)

            sorted = sorted.asSequence()
                .filter { it.second >= minScore }
                .map { it.first }
                .map { word ->
                    var total = 0
                    word.forEachIndexed { idx, letter ->
                        total += positions[idx][letter]!!
                    }
                    Pair(word, total)
                }
                .toList()
                .sortedByDescending { it.second }

            minScore = sorted.first().second - ((sorted.first().second - sorted.last().second) / 10)
            sorted = sorted.filter { it.second >= minScore }

            return sorted.map { it.first }.take(numGuesses)
        }
    }
}