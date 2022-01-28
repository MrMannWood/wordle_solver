package com.mrmannwood.wordle

data class Guess private constructor(val guess: String, val score: String) {
    companion object {
        fun guess(guess: String, score: String): Guess {
            return Guess(guess, score)
        }
        fun score(guess: String, secretWord: String): Guess {
            val result = CharArray(5) {'b'}
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
            return Guess(guess, result.concatToString())
        }
    }

    fun isCorrect(): Boolean = score.firstOrNull { it != 'g' }?.let { false } ?: run { true }

    fun getKnownGoodCharacters(): List<Char> =
        guess.asSequence().filterIndexed { idx, _ -> score[idx] == 'g' || score[idx] == 'y' }.toList()

    fun getKnownBadCharacters(): List<Char> {
        return if ('b' !in score) {
            ('a'..'z').asSequence().filter { it !in guess }.toList()
        } else {
            guess.asSequence().filterIndexed { idx, _ -> score[idx] == 'b' }.toList()
        }
    }

    fun isValid(word: String): Boolean {
        val evalWord = word.toCharArray()

        for (idx in evalWord.indices) {
            if (score[idx] == 'g') {
                if (evalWord[idx] != guess[idx]) return false
                evalWord[idx] = '-'
            }
        }
        for (idx in evalWord.indices) {
            if (score[idx] == 'y') {
                if (guess[idx] == evalWord[idx]) return false
                if (guess[idx] !in evalWord) return false
                for (i in evalWord.indices) if (evalWord[i] == guess[idx]) { evalWord[i] = '-'; break }
            }
        }
        for (idx in evalWord.indices) {
            if (score[idx] == 'b') {
                if (guess[idx] in evalWord) return false
            }
        }
        return true
    }
}