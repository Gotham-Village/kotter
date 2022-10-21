package com.varabyte.kotter.runtime.internal.text

import kotlin.math.max
import kotlin.math.min

/**
 * A class which points at a character index within a text string, where the index can be incremented and decremented.
 *
 * Note that it's valid to point at the index AFTER the last character, which in this case returns a null terminator
 * character to indicate it. That is, for a String of length one, e.g. "a", then textPtr[0] == 'a' and
 * textPtr[1] == '\0'
 */
class TextPtr(val text: CharSequence, charIndex: Int = 0) {
    var charIndex = 0
        set(value) {
            require(value >= 0 && value <= text.length) { "charIndex value is out of bounds. Expected 0 .. ${text.length}, got $value" }
            field = value
        }

    val currChar get() = text.elementAtOrNull(charIndex) ?: Char.MIN_VALUE
    val remainingLength get() = max(0, text.length - charIndex)

    init {
        this.charIndex = charIndex
    }

    /**
     * Increment or decrement the pointer first (based on [forward]), and then keep moving until
     * [keepMoving] stops returning true.
     */
    private fun movePtr(forward: Boolean, keepMoving: (Char) -> Boolean): Boolean {
        val delta = if (forward) 1 else -1

        var newIndex = charIndex
        do {
            newIndex += delta
            if (newIndex < 0) {
                newIndex = 0
                break
            } else if (newIndex >= text.length) {
                newIndex = text.length
                break
            }
        } while (keepMoving(text[newIndex]))

        if (newIndex != charIndex) {
            charIndex = newIndex
            return true
        }
        return false
    }

    fun increment(): Boolean {
        return movePtr(true) { false }
    }

    fun decrement(): Boolean {
        return movePtr(false) { false }
    }

    /**
     * Increment the text pointer at least once, and then keep going as long as the condition is met.
     *
     * @return true if we incremented at least one character (regardless of the condition's effect).
     */
    fun incrementWhile(whileCondition: (Char) -> Boolean) = movePtr(true, whileCondition)

    /**
     * Decrement the text pointer at least once, and then keep going as long as the condition is met.
     *
     * @return true if we decremented at least one character (regardless of the condition's effect).
     */
    fun decrementWhile(whileCondition: (Char) -> Boolean) = movePtr(false, whileCondition)

    /**
     * Like [incrementWhile] with the condition inverted.
     */
    fun incrementUntil(whileCondition: (Char) -> Boolean): Boolean {
        return incrementWhile { !whileCondition(it) }
    }

    /**
     * Like [decrementWhile] with the condition inverted.
     */
    fun decrementUntil(whileCondition: (Char) -> Boolean): Boolean {
        return decrementWhile { !whileCondition(it) }
    }
}

fun TextPtr.substring(length: Int): String {
    val cappedLen = min(length, text.length - charIndex) // In case user passes in Int.MAX_VALUE, this avoids int wrapping
    return text.substring(charIndex, min(charIndex + cappedLen, text.length))
}

fun TextPtr.startsWith(value: Char, ignoreCase: Boolean = false): Boolean {
    return remainingLength > 0 && currChar.equals(value, ignoreCase)
}

fun TextPtr.startsWith(value: String, ignoreCase: Boolean = false): Boolean {
    if (remainingLength < value.length) return false
    return substring(value.length).equals(value, ignoreCase)
}

fun TextPtr.readInt(): Int? {
    if (!currChar.isDigit()) return null

    var intValue = 0
    while (true) {
        val digit = currChar.digitToIntOrNull() ?: break
        increment()
        intValue *= 10
        intValue += digit
    }
    return intValue
}

/**
 * Read the current string up until we reach a point where the condition is no longer true.
 *
 * The character that the text pointer will be on when the condition fails will NOT be included, so for example:
 *
 * ```
 * TextPtr("Hello;World").readUntil { currChar == ';' }
 * ```
 *
 * will return "Hello", not "Hello;"
 */
fun TextPtr.readUntil(condition: TextPtr.() -> Boolean) = buildString {
    while (!condition() && currChar != Char.MIN_VALUE) {
        append(currChar)
        increment()
    }
}