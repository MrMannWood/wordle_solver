package com.mrmannwood.wordle

import kotlin.collections.ArrayList

/* Kotlin appears to have a compiler issue when defining this as a single list in a single file. Splitting it like this
 * appears to fix the problem */
val DICTIONARY: List<String> by lazy {
    ArrayList<String>(DICT1.size + DICT2.size).apply {
        addAll(DICT1)
        addAll(DICT2)
    }
}