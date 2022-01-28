package com.mrmannwood.wordle

import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.*

val wordleSetupDiv = document.getElementById("wordle_setup_div") as HTMLDivElement
val wordleDiv = document.getElementById("wordle_game_div") as HTMLDivElement
val secretWordInput = document.getElementById("secret_word_input") as HTMLInputElement
val secretWordResult = document.getElementById("secret_word_result") as HTMLParagraphElement
val guessInput = document.getElementById("guess_input") as HTMLInputElement
val wordleTable = document.getElementById("wordle_table") as HTMLTableElement
val remainingWordsTable = document.getElementById("remaining_words") as HTMLTableElement
val bestGuessTable = document.getElementById("best_guess") as HTMLTableElement

fun main() {

    lateinit var secretWord: String
    secretWordInput.addEventListener("input", { event ->
        val input = secretWordInput.value.toLowerCase()
        if (input.length == 5) {
            if (!WORDLE_LIST.contains(input)) {
                window.alert("$input is not a legal word")
                secretWordInput.value = ""
            } else {
                secretWord = input
                wordleDiv.style.display = "block"
                wordleSetupDiv.style.display = "none"
                secretWordResult.innerText = "Secret Word: $input"
            }
        }
    })

    val guesses: MutableList<Guess> = mutableListOf()
    guessInput.addEventListener("input", { event ->
        val input = guessInput.value.toLowerCase()
        if (input.length == 5) {
            if (!DICTIONARY.contains(input)) {
                window.alert("$input is not a legal word")
            } else {
                guesses.add(Guess.score(input, secretWord))
                handleGuess(guesses)
            }
            guessInput.value = ""
        }
    })
}

private fun handleGuess(guesses: List<Guess>) {
    val bestGuesses = getTop10Guesses(guesses)
    val viableWords = getRemainingWords(guesses)

    wordleTable.insertRow(-1).apply {
        val guess = guesses.last()
        guess.guess.forEachIndexed { idx, char ->
            (insertCell(-1) as HTMLTableCellElement).apply {
                innerText = char.toString().toUpperCase()
                bgColor = when(guess.score[idx]) {
                    'g' -> "#618b55"
                    'y' -> "#b29f4c"
                    else -> "#121213"
                }
                style.fontSize = "20px"
                style.color = "#d7dadc"
                style.fontWeight = "bold"
            }
        }
        (insertCell(-1) as HTMLTableCellElement).apply {
            innerText = "${viableWords.size} viable words remain"
        }
    }

    while(remainingWordsTable.rows.length > 0) { remainingWordsTable.deleteRow(0) }

    var i = 1
    while (i * i < viableWords.size) { i++ }

    lateinit var row: HTMLTableRowElement
    viableWords.forEachIndexed { idx, word ->
        if (idx % i == 0) { row = remainingWordsTable.insertRow(-1) }
        (row.insertCell(-1) as HTMLTableCellElement).apply {
            innerText = word
            style.color = "#d7dadc"

            val index = bestGuesses.indexOf(word)
            bgColor = when {
                index < 0 -> {
                    "#121213"
                }
                index > 5 -> {
                    "#b29f4c"
                }
                else -> {
                    "#618b55"
                }
            }
        }
    }

    while(bestGuessTable.rows.length > 0) { bestGuessTable.deleteRow(0) }

    i = 1
    while (i * i < bestGuesses.size) { i++ }

    bestGuesses.forEachIndexed { idx, word ->
        if (idx % i == 0) { row = bestGuessTable.insertRow(-1) }
        (row.insertCell(-1) as HTMLTableCellElement).apply {
            innerText = word
            style.color = "#d7dadc"

            val index = bestGuesses.indexOf(word)
            bgColor = when {
                index > 7 -> {
                    "#121213"
                }
                index > 3 -> {
                    "#b29f4c"
                }
                else -> {
                    "#618b55"
                }
            }
        }
    }
}