package com.mrmannwood.wordle

fun getRemainingWords(guesses: List<Guess>): List<String> {
    val words = DICTIONARY.toMutableList()
    val knownGoodCharacters = mutableSetOf<Char>()
    val eliminatedCharacters = mutableSetOf<Char>()

    guesses.forEach { guessScore ->
        handleGuess(
                guess = guessScore,
                words = words,
                goodChars = knownGoodCharacters,
                badChars = eliminatedCharacters
        )
    }
    return words
}

fun getTop10Guesses(guesses: List<Guess>): List<String> {
    val dictionary = DICTIONARY.toMutableList()
    val words = DICTIONARY.toMutableList()
    val knownGoodCharacters = mutableSetOf<Char>()
    val eliminatedCharacters = mutableSetOf<Char>()

    guesses.forEach { guessScore ->
        handleGuess(
            guess = guessScore,
            words = words,
            goodChars = knownGoodCharacters,
            badChars = eliminatedCharacters
        )
    }

    val generatedGuesses = mutableListOf<String>()
    for (i in 0..9) {
        if (words.isNotEmpty()) {
            val guess = getBestGuess(words, dictionary, eliminatedCharacters, knownGoodCharacters) ?: break
            generatedGuesses.add(guess)
            words.remove(guess)
            dictionary.remove(guess)
        }
    }
    return generatedGuesses
}

fun guessWord(secretWord: String): List<Guess> {
    val words = DICTIONARY.toMutableList()
    val guesses = mutableListOf<Guess>()
    val knownGoodCharacters = mutableSetOf<Char>()
    val eliminatedCharacters = mutableSetOf<Char>()

    while (true) {
        val guess = Guess.score(
            getBestGuess(words, DICTIONARY, eliminatedCharacters, knownGoodCharacters) ?: break,
            secretWord
        )
        handleGuess(
            guess = guess,
            words = words,
            goodChars = knownGoodCharacters,
            badChars = eliminatedCharacters
        )
        guesses.add(guess)
        if (guess.isCorrect()) break
        if (guesses.size > 20) {
            //prevents infinite loops
            break
        }
    }

    return guesses
}

fun handleGuess(guess: Guess, words: MutableList<String>, goodChars: MutableSet<Char>, badChars: MutableSet<Char>) {
    goodChars.addAll(guess.getKnownGoodCharacters())
    badChars.addAll(guess.getKnownBadCharacters().filter { !goodChars.contains(it) })

    words.removeAll(words.filter { !guess.isValid(it) })
}

fun getBestGuess(words: List<String>, allWords: List<String>, eliminatedCharacters: Set<Char>, usedCharacters: Set<Char>): String? {
    GuessStrategy.getStrategies().forEach {
        val candidate = it.getBestGuesses(1, words, allWords, eliminatedCharacters, usedCharacters).firstOrNull()
        if (candidate != null) {
            return candidate
        }
    }
    return null
}
