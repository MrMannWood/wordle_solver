package com.mrmannwood.wordle

import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import kotlin.Exception

fun main(args: Array<String>) {
//    runForAllWords()
//    runForOneWord("maker")
    getTop10GuessesForEachStrategy(
        GuessStrategy.getStrategies(),
        DICTIONARY,
        listOf(
            GuessScore.score("tears", "prick"),
            GuessScore.score("prong", "prick")
//            GuessScore.score("primy", "prick")
        )
    )
}

fun getTop10GuessesForEachStrategy(
    strategies: List<GuessStrategy>,
    dictionary: List<String>,
    guesses: List<GuessScore>
) {
    var words = dictionary
    val knownGoodCharacters = mutableSetOf<Char>()
    val eliminatedCharacters = mutableSetOf<Char>()

    guesses.forEach { guessScore ->
        if (guessScore.isCorrect()) return@forEach

        knownGoodCharacters.addAll(guessScore.getKnownGoodCharacters())
        eliminatedCharacters.addAll(guessScore.getKnownBadCharacters().filter { !knownGoodCharacters.contains(it) })

        words = words.filter { guessScore.isValid(it) }
    }

    strategies.forEach { strategy ->
        val g = strategy.getBestGuesses(10, words, dictionary, eliminatedCharacters, knownGoodCharacters)
        if (g.isNotEmpty()) {
            println(strategy.name)
            g.forEach { println("\t$it") }
        }
    }
}

/**
 * Used to get generate guesses for one word
 */
fun runForOneWord(secretWord: String) {
    val words = DICTIONARY

    println("Searching for $secretWord")
    val guesses = guessWord(words, secretWord)

    if (guesses.size > 6) {
        println("Lost :( ${guesses.size}")
    } else {
        println("Won :D ${guesses.size}")
    }
    guesses.forEach { (guess, result) ->
        println("Guess: $guess, Result: $result")
    }
}

/**
 * Runs the entire dictionary against itself. This is a performance and optimization testing tool that helps me
 * understand how well the algorithm is working
 */
fun runForAllWords() {
    val secretWords = WORDLE_LIST
    val dictionary = DICTIONARY

    val executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() - 1)
    val futures = mutableListOf<Future<Pair<String, Int>>>()
    secretWords.forEach { word ->
        futures.add(
            executor.submit(Callable {
                try {
                    word to guessWord(dictionary, word).size
                } catch (e: Exception) {
                    throw Exception("Error for word: $word", e)
                }
            })
        )
    }
    executor.shutdown()

    val results = mutableMapOf<Int, MutableList<String>>()
    futures.forEach {
        val result = it.get(10, TimeUnit.MINUTES)
        results.getOrPut(result.second) { mutableListOf() }.add(result.first)
    }

    var overThreshold = 0
    var thresholdOrLess = 0
    println("|Guesses|Words|")
    println("|---|---|")
    results.toList().sortedByDescending { it.first }.forEach { (numGuesses, words) ->
        println("|$numGuesses|${words.size}|")
        if (numGuesses > 6) {
            overThreshold += words.size
        } else {
            thresholdOrLess += words.size
        }
        if (numGuesses > 6) {
            words.forEach { println("\t$it") }
        }
    }
    println("Words that took 6 or more guesses: $overThreshold")
    println("Words that took 6 guesses or fewer: $thresholdOrLess")
}

/**
 * The actual solver. Takes the dictionary and the secret word, and returns a list of all the guesses with their results
 */
fun guessWord(allWords: List<String>, secretWord: String): List<GuessScore> {
    var words = allWords
    val guesses = mutableListOf<GuessScore>()
    val knownGoodCharacters = mutableSetOf<Char>()
    val eliminatedCharacters = mutableSetOf<Char>()

    while (true) {
        val guess = getBestGuess(words, allWords, eliminatedCharacters, knownGoodCharacters)

        val guessScore = GuessScore.score(guess, secretWord)
        guesses.add(guessScore)
        if (guessScore.isCorrect()) break
        if (guesses.size > 20) {
            //prevents infinite loops
            break
        }

        knownGoodCharacters.addAll(guessScore.getKnownGoodCharacters())
        eliminatedCharacters.addAll(guessScore.getKnownBadCharacters().filter { !knownGoodCharacters.contains(it) })

        words = words.filter { guessScore.isValid(it) }
    }

    return guesses
}

/**
 * Uses the different strategies to make a guess. This was tuned with experimentation.
 */
fun getBestGuess(words: List<String>, allWords: List<String>, eliminatedCharacters: Set<Char>, usedCharacters: Set<Char>): String {
    GuessStrategy.getStrategies().forEach {
        val candidate = it.getBestGuesses(1, words, allWords, eliminatedCharacters, usedCharacters).firstOrNull()
        if (candidate != null) {
            return candidate
        }
    }
    throw Exception("No strategy returned a word for $words")
}
