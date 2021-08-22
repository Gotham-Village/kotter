package com.varabyte.konsole.ansi.commands

import com.varabyte.konsole.core.KonsoleCommand
import com.varabyte.konsole.core.KonsoleScope
import com.varabyte.konsole.core.internal.MutableKonsoleTextArea

private class CharCommand(private val char: Char) : KonsoleCommand {
    override fun applyTo(textArea: MutableKonsoleTextArea) {
        textArea.append(char)
    }
}

private class TextCommand(private val text: String) : KonsoleCommand {
    override fun applyTo(textArea: MutableKonsoleTextArea) {
        textArea.append(text)
    }
}

private val NewlineCommand = CharCommand('\n')

fun KonsoleScope.text(text: String) {
    block.applyCommand(TextCommand(text))
}

fun KonsoleScope.textLine(text: String) {
    block.applyCommand(TextCommand(text))
    newLine()
}

fun KonsoleScope.newLine() {
    block.applyCommand(NewlineCommand)
}

/**
 * Create a "paragraph" for text.
 *
 * This is a convenience function for wrapping a block with newlines above and below it, which is a common enough
 * pattern that it's nice to shorten it.
 */
fun KonsoleScope.p(block: KonsoleScope.() -> Unit) {
    newLine()
    block()
    newLine()
}

