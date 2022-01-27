package com.mrmannwood.wordle

import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

fun main(args: Array<String>) {
    //playWordle()
    worldSolver()
}

private fun playWordle() {
    val secretWord = WORDLE_LIST.random()
    val scanner = Scanner(System.`in`)
    val guesses = mutableListOf<Guess>()
    for (i in 5 downTo 0) {
        val guess = readGuess(scanner)
        guesses.add(Guess.score(guess, secretWord))
        println(guesses.last().score)
        if (guesses.last().isCorrect()) break
    }
    if (guesses.last().isCorrect()) {
        println("Congrats, you won!")
    } else {
        println("Sorry! The word was $secretWord")
    }
}

private fun worldSolver() {
    println("Wordle Solver")
    val scanner = Scanner(System.`in`)
    val guesses = mutableListOf<Guess>()
    while (true) {
        guesses.add(Guess.guess(
            readGuess(scanner),
            readResult(scanner)
        ))
        val words = getRemainingWords(guesses)
        println(words.joinToString(", ") { it })
        if (words.size == 1) {
            println("There is 1 viable word remaining")
            break
        } else {
            println("There are ${words.size} viable words remaining")
        }
    }
}

private fun readGuess(scanner: Scanner): String {
    while(true) {
        print("Enter Guess: ")
        val guess = scanner.nextLine().toLowerCase()
        if (guess.length == 5 && guess.matches(Regex("[a-z]*"))) {
            return guess
        }
        println("Guess is not valid. Must be 5 letters.")
    }
}

private fun readResult(scanner: Scanner): String {
    while(true) {
        print("Enter Result: ")
        val result = scanner.nextLine().toLowerCase()
        if (result.length == 5 && result.matches(Regex("[bgy]*"))) {
            return result
        }
        println("Result is not valid. Must be 5 of [g, y, b].")
    }
}

fun getTop10Guesses() {
    getTop10Guesses(
        listOf(
            Guess.score("tears", "prick"),
            Guess.score("prong", "prick")
        )
    ).forEach {
        println(it)
    }
}

/**
 * Used to get generate guesses for one word
 */
fun runForOneWord(secretWord: String) {
    println("Searching for $secretWord")
    val guesses = guessWord(secretWord)

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

    val executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() - 1)
    val futures = mutableListOf<Future<Pair<String, Int>>>()
    secretWords.forEach { word ->
        futures.add(
            executor.submit(Callable {
                try {
                    word to guessWord(word).size
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
    var sum = 0
    println("|Guesses|Words|")
    println("|---|---|")
    results.toList().sortedByDescending { it.first }.forEach { (numGuesses, words) ->
        println("|$numGuesses|${words.size}|")
        if (numGuesses > 6) {
            overThreshold += words.size
        } else {
            thresholdOrLess += words.size
        }
        sum += words.size
    }
    println("Words that took 6 or more guesses: $overThreshold")
    println("Words that took 6 guesses or fewer: $thresholdOrLess")
}