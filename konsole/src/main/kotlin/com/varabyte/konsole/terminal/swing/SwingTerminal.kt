package com.varabyte.konsole.terminal.swing

import com.varabyte.konsole.KonsoleSettings
import com.varabyte.konsole.ansi.AnsiCodes
import com.varabyte.konsole.ansi.AnsiCodes.Sgr.Colors.Bg
import com.varabyte.konsole.ansi.AnsiCodes.Sgr.Colors.Fg
import com.varabyte.konsole.ansi.AnsiCodes.Sgr.Decorations
import com.varabyte.konsole.ansi.AnsiCodes.Sgr.RESET
import com.varabyte.konsole.ansi.AnsiCodes.Sgr.SgrCode
import com.varabyte.konsole.terminal.Terminal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.util.concurrent.CountDownLatch
import javax.swing.JFrame
import javax.swing.JScrollPane
import javax.swing.JTextPane
import javax.swing.text.MutableAttributeSet
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants

class SwingTerminal private constructor(private val pane: SwingTerminalPane) : Terminal {
    companion object {
        /**
         * @param terminalSize Number of characters, so 80x32 will be expanded to fit 80 characters horizontally and
         *   32 lines vertically (before scrolling is needed)
         */
        fun create(
            title: String = KonsoleSettings.VirtualTerminal.title ?: "Konsole Terminal",
            terminalSize: Dimension = KonsoleSettings.VirtualTerminal.size,
            fontSize: Int = KonsoleSettings.VirtualTerminal.fontSize,
            fgColor: Color = KonsoleSettings.VirtualTerminal.fgColor,
            bgColor: Color = KonsoleSettings.VirtualTerminal.bgColor,
        ): SwingTerminal {
            val pane = SwingTerminalPane(fontSize)
            pane.foreground = fgColor
            pane.background = bgColor
            pane.text = buildString {
                // Set initial text to a block of blank characters so pack will set it to the right size
                for (h in 0 until terminalSize.height) {
                    if (h > 0) appendLine()
                    for (w in 0 until terminalSize.width) {
                        append(' ')
                    }
                }
            }

            val terminal = SwingTerminal(pane)
            val framePacked = CountDownLatch(1)
            CoroutineScope((Dispatchers.Swing)).launch {
                val frame = JFrame(title)
                frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE

                frame.contentPane.add(JScrollPane(terminal.pane))
                frame.pack()
                terminal.pane.text = ""
                frame.setLocationRelativeTo(null)

                framePacked.countDown()
                frame.isVisible = true
            }
            framePacked.await()

            return terminal
        }
    }

    override fun write(text: String) {
        pane.processAnsiText(text)
    }

    override fun read(): Flow<Int> = callbackFlow {
        TODO("Not yet implemented")
    }
}

private class TextPtr(val text: String, charIndex: Int = 0) {

    var charIndex = 0
        set(value) {
            require(value >= 0 && value < text.length) { "charIndex value is out of bounds. Expected 0 .. ${text.length - 1}, got $value" }
            field = value
        }

    val currChar get() = text[charIndex]

    init {
        require(text != "") { "TextPtr expects to point at a non-empty string" }
        this.charIndex = charIndex
    }

    fun moveCursor(delta: Int): Boolean {
        val newIndex = (charIndex + delta).coerceIn(0, text.length - 1)
        if (newIndex != charIndex) {
            charIndex = newIndex
            return true
        }
        return false
    }

    fun startsWith(prefix: String): Boolean {
        if (text.length < charIndex + prefix.length) return false

        for (i in prefix.indices) {
            if (text[charIndex + i] != prefix[i]) return false
        }
        return true
    }

    fun substring(length: Int): String = text.substring(charIndex, charIndex + length)
}

private val SGR_CODE_TO_ATTR_MODIFIER = mapOf<SgrCode, MutableAttributeSet.() -> Unit>(
    RESET to { removeAttributes(this) },

    Fg.BLACK to { StyleConstants.setForeground(this, Color.BLACK) },
    Fg.RED to { StyleConstants.setForeground(this, Color.RED) },
    Fg.GREEN to { StyleConstants.setForeground(this, Color.GREEN) },
    Fg.YELLOW to { StyleConstants.setForeground(this, Color.YELLOW) },
    Fg.BLUE to { StyleConstants.setForeground(this, Color.BLUE) },
    Fg.MAGENTA to { StyleConstants.setForeground(this, Color.MAGENTA) },
    Fg.CYAN to { StyleConstants.setForeground(this, Color.CYAN) },
    Fg.WHITE to { StyleConstants.setForeground(this, Color.WHITE) },
    Fg.BLACK_BRIGHT to { StyleConstants.setForeground(this, Color.BLACK) },
    Fg.RED_BRIGHT to { StyleConstants.setForeground(this, Color.RED) },
    Fg.GREEN_BRIGHT to { StyleConstants.setForeground(this, Color.GREEN) },
    Fg.YELLOW_BRIGHT to { StyleConstants.setForeground(this, Color.YELLOW) },
    Fg.BLUE_BRIGHT to { StyleConstants.setForeground(this, Color.BLUE) },
    Fg.MAGENTA_BRIGHT to { StyleConstants.setForeground(this, Color.MAGENTA) },
    Fg.CYAN_BRIGHT to { StyleConstants.setForeground(this, Color.CYAN) },
    Fg.WHITE_BRIGHT to { StyleConstants.setForeground(this, Color.WHITE) },

    Bg.BLACK to { StyleConstants.setBackground(this, Color.BLACK) },
    Bg.RED to { StyleConstants.setBackground(this, Color.RED) },
    Bg.GREEN to { StyleConstants.setBackground(this, Color.GREEN) },
    Bg.YELLOW to { StyleConstants.setBackground(this, Color.YELLOW) },
    Bg.BLUE to { StyleConstants.setBackground(this, Color.BLUE) },
    Bg.MAGENTA to { StyleConstants.setBackground(this, Color.MAGENTA) },
    Bg.CYAN to { StyleConstants.setBackground(this, Color.CYAN) },
    Bg.WHITE to { StyleConstants.setBackground(this, Color.WHITE) },
    Bg.BLACK_BRIGHT to { StyleConstants.setBackground(this, Color.BLACK) },
    Bg.RED_BRIGHT to { StyleConstants.setBackground(this, Color.RED) },
    Bg.GREEN_BRIGHT to { StyleConstants.setBackground(this, Color.GREEN) },
    Bg.YELLOW_BRIGHT to { StyleConstants.setBackground(this, Color.YELLOW) },
    Bg.BLUE_BRIGHT to { StyleConstants.setBackground(this, Color.BLUE) },
    Bg.MAGENTA_BRIGHT to { StyleConstants.setBackground(this, Color.MAGENTA) },
    Bg.CYAN_BRIGHT to { StyleConstants.setBackground(this, Color.CYAN) },
    Bg.WHITE_BRIGHT to { StyleConstants.setBackground(this, Color.WHITE) },

    Decorations.BOLD to { StyleConstants.setBold(this, true) },
    Decorations.ITALIC to { StyleConstants.setItalic(this, true) },
    Decorations.UNDERLINE to { StyleConstants.setUnderline(this, true) },
    Decorations.STRIKETHROUGH to { StyleConstants.setStrikeThrough(this, true) },
)


class SwingTerminalPane(fontSize: Int) : JTextPane() {
    init {
        isEditable = false
        font = Font(Font.MONOSPACED, Font.PLAIN, fontSize)
    }

    private fun processEscapeCode(textPtr: TextPtr, attrs: MutableAttributeSet): Boolean {
        if (!textPtr.moveCursor(1)) return false
        return when (textPtr.currChar) {
            AnsiCodes.EscSeq.CSI -> processCsiCode(textPtr, attrs)
            else -> false
        }
    }

    private fun processCsiCode(textPtr: TextPtr, attrs: MutableAttributeSet): Boolean {
        if (!textPtr.moveCursor(1)) return false

        for (entry in SGR_CODE_TO_ATTR_MODIFIER) {
            val code = entry.key
            if (textPtr.startsWith(code.value)) {
                val modifyAttributes = entry.value
                modifyAttributes(attrs)
                textPtr.moveCursor(code.value.length - 1)
                return true
            }
        }
        return false
    }

    fun processAnsiText(text: String) {
        if (text.isEmpty()) return

        val doc = styledDocument
        val attrs = SimpleAttributeSet()
        val stringBuilder = StringBuilder()
        fun flush() {
            doc.insertString(doc.length, stringBuilder.toString(), attrs)
            stringBuilder.clear()
        }

        val textPtr = TextPtr(text)
        do {
            when (textPtr.currChar) {
                AnsiCodes.Ctrl.ESC -> {
                    flush()
                    val prevCharIndex = textPtr.charIndex
                    if (!processEscapeCode(textPtr, attrs)) {
                        // Skip over escape byte or else error message will be interpreted as an ANSI command!
                        textPtr.charIndex = prevCharIndex + 1
                        val peekLen = 7
                        throw IllegalArgumentException(
                            "Unknown escape sequence starting here (plus next $peekLen characters): ${
                                textPtr.substring(peekLen + 1)
                            }..."
                        )
                    }
                }

                else -> stringBuilder.append(textPtr.currChar)
            }
        } while (textPtr.moveCursor(1))
        flush()
    }
}
