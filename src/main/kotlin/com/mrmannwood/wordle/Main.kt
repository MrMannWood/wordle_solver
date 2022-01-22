package com.mrmannwood.wordle

import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.Exception
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.streams.asSequence

object Main

fun main(args: Array<String>) {
//    runForAllWords()
    runForOneWord("prick")
}

/**
 * Used to get generate guesses for one word
 */
fun runForOneWord(secretWord: String) {
    val words = readWords()

    println("Searching for $secretWord")
    val guesses = guessWord(words, secretWord)

    if (guesses.size > 5) {
        println("Lost :( ${guesses.size}")
    } else {
        println("Won :D ${guesses.size}")
    }
    //println("The word is ${guesses.last().first}")
    guesses.forEach { (guess, result) ->
        println("Guess: $guess, Result: $result")
    }
}

/**
 * Runs the entire dictionary against itself. This is a performance and optimization testing tool that helps me
 * understand how well the algorithm is working
 */
fun runForAllWords() {
    val words = readWords()
    println(words.size)

    val results = mutableMapOf<Int, MutableList<String>>().apply { for(i in 0..21) put(i, mutableListOf()) }

    val executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() - 1)
    val exceptions = mutableMapOf<String, Exception>()

    val startTime = System.currentTimeMillis()
    words.forEachIndexed { idx, word ->
        executor.submit {
            try {
                val result = guessWord(words, word)
                val list = results[result.size]!!
                synchronized(list) {
                    list.add(word)
                }
            } catch (e: Exception) {
                synchronized(exceptions) { exceptions.put(word, e) }
            }
        }
    }

    executor.shutdown()
    try {
        if (!executor.awaitTermination(10, TimeUnit.MINUTES)) {
            println("Got stuck I guess?")
        }
    } catch (e: InterruptedException) {
        println("Interrupted")
    }
    val endTime = System.currentTimeMillis()
    println("Ellapsed Time: ${endTime - startTime}")

    println("e: ${exceptions.size}")
    if (exceptions.isNotEmpty()) {
        exceptions.asSequence().first().let { (word, ex) ->
            println("Exception while handling $word")
            ex.printStackTrace()
        }
    }

    var overThreshold = 0
    var thresholdOrLess = 0
    results.toList().sortedByDescending { it.first }.forEach { (numGuesses, words) ->
        if (words.size == 0) return@forEach
        println("${numGuesses}: ${words.size}")
        if (numGuesses > 5) {
            overThreshold += words.size
        } else {
            thresholdOrLess += words.size
        }
        if (numGuesses >= 11) {
            words.forEach { word ->
                println("\t$word")
            }
        }
    }
    println("Words that took more than threshold: $overThreshold")
    println("Words that took threshold or fewer: $thresholdOrLess")
}

/**
 * The actual solver. Takes the dictionary and the secret word, and returns a list of all the guesses with their results
 */
fun guessWord(allWords: List<String>, secretWord: String): List<Pair<String, String>> {
    var words = allWords
    val guesses = mutableListOf<Pair<String, String>>()
    val knownGoodCharacters = mutableListOf<Char>()
    val eliminatedCharacters = mutableListOf<Char>()

    while (true) {
        val guess = getBestGuess(words, allWords, eliminatedCharacters, knownGoodCharacters)

        val result = scoreGuess(guess, secretWord)
        guesses.add(Pair(guess, result))
        if (result == "ggggg") break
        if (guesses.size > 20) {
            break
        }

        guess.forEachIndexed { idx, letter ->
            if (result[idx] == 'g' || result[idx]== 'y') knownGoodCharacters.addDistinct(letter)
        }
        guess.forEachIndexed { idx, letter ->
            if (result[idx] == 'b' && letter !in knownGoodCharacters) eliminatedCharacters.addDistinct(letter)
        }
        if ('b' !in result) {
            for (c in 'a'..'z') {
                if (c in guess)
                    knownGoodCharacters.addDistinct(c)
                else
                    eliminatedCharacters.addDistinct(c)
            }
        }

        words = words
                .filter { word ->
                    val evalWord = word.toCharArray()

                    for (idx in evalWord.indices) {
                        if (result[idx] == 'g') {
                            if (evalWord[idx] != guess[idx]) return@filter false
                            evalWord[idx] = '-'
                        }
                    }
                    for (idx in evalWord.indices) {
                        if (result[idx] == 'y') {
                            if (guess[idx] == evalWord[idx]) return@filter false
                            if (guess[idx] !in evalWord) return@filter false
                            for (i in evalWord.indices) if (evalWord[i] == guess[idx]) { evalWord[i] = '-'; break }
                        }
                    }
                    for (idx in evalWord.indices) {
                        if (result[idx] == 'b') {
                            if (guess[idx] in evalWord) return@filter false
                        }
                    }
                    return@filter true
                }
                .filter { it != guess }
                .toList()
    }

    return guesses
}

/**
 * Ceremony to read the dictionary
 */
fun readWords(): List<String> {
    return BufferedReader(InputStreamReader(Main::class.java.getResourceAsStream("/words.txt"))).lines()
        .asSequence()
        .filter { it.length == 5 }
        .filter { word ->
            word.forEach { c ->
                if (c !in 'a'..'z' && c !in 'A'..'Z') {
                    return@filter false
                }
            }
            return@filter true
        }
        .map { it.toLowerCase() }
        .toList()
}

/**
 * Uses the different strategies to make a guess. This was tuned with experimentation.
 */
fun getBestGuess(words: List<String>, allWords: List<String>, eliminatedCharacters: List<Char>, usedCharacters: List<Char>): String {
    if (words.size == 1) { return words[0] }
    if (words.size in 3..10000) { // 10000 is a magic number. No idea why it works, but experimentation shows that it does
        val candidate = CharacterEliminationStrategy.getBestGuess(words, allWords, eliminatedCharacters, usedCharacters)
        if (candidate != null) {
            return candidate
        }
    }
    return HighestCharacterFrequencyStrategy.getBestGuess(words, allWords, eliminatedCharacters, usedCharacters)
}

/**
 * This is the part that is "wordle." Takes a guess word and a secret word and returns a string representing the wordle
 * color result
 * e.x. Guess: pines, SecretWord: prick, Result: gybbb
 */
fun scoreGuess(guess: String, secretWord: String): String {
    val result = charArrayOf('b','b','b','b','b')
    val gEval = guess.toCharArray()
    val sWord = secretWord.toCharArray()

    for (idx in result.indices) {
        if (sWord[idx] == gEval[idx]) {
            result[idx] = 'g'
            sWord[idx] = '-'
            gEval[idx] = '-'
        }
    }
    for (idx in result.indices) {
        if (gEval[idx] == '-') continue
        if (gEval[idx] in sWord) {
            result[idx] = 'y'
            for (i in sWord.indices) {
                if (gEval[idx] == sWord[i]) {
                    sWord[i] = '-'
                    break
                }
            }
            gEval[idx] = '-'
        }
    }
    return String(result)
}

/**
 * Just some ceremony
 */
fun <E> MutableList<E>.addDistinct(element: E) {
    if (!contains(element)) add(element)
}

/**
 * I experimented with enough strategies that I decided to make an interface. Turns out that (of the ones I tried) only
 * these two were any good
 */
interface GuessStrategy {
    fun getBestGuess(words: List<String>, allWords: List<String>, eliminatedCharacters: List<Char>, usedCharacters: List<Char>): String?
}

/**
 * Analyze the remaining words and find the characters that aren't common, then choose a word from allWords that has the
 * most of them. This is handle the "shill, skill, spill" case, but it turns out it's also really effective generally
 */
object CharacterEliminationStrategy: GuessStrategy {
    override fun getBestGuess(words: List<String>, allWords: List<String>, eliminatedCharacters: List<Char>, usedCharacters: List<Char>): String? {
        val positions = mutableListOf<MutableMap<Char, Int>>().apply {
            for(idx in 0..4) { add(mutableMapOf()) }
        }
        words.forEach { word ->
            word.toCharArray().forEachIndexed { idx, letter ->
                positions[idx][letter] = positions[idx].getOrElse(letter) { 0 } + 1
            }
        }
        val letters = mutableListOf<Char>()
        positions.forEach { position ->
            if (position.values.size != 1) {
                position.keys.forEach {
                    if (it !in usedCharacters && it !in eliminatedCharacters) {
                        letters.addDistinct(it)
                    }
                }
            }
        }
        if (letters.size == 0) return null

        return allWords
                .map { word ->
                    var count = 0
                    letters.forEach { if(word.contains(it)) count++ }
                    word to count
                }.maxBy { it.second }?.first
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
object HighestCharacterFrequencyStrategy: GuessStrategy {
    override fun getBestGuess(words: List<String>, allWords: List<String>, eliminatedCharacters: List<Char>, usedCharacters: List<Char>): String {
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

        return sorted.first().first
    }
}
