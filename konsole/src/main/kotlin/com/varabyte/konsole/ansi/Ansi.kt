package com.varabyte.konsole.ansi

/**
 * A collection of common ANSI codes and other related constants which power the features of
 * Konsole.
 *
 * See also:
 * https://en.wikipedia.org/wiki/ANSI_escape_code
 * https://www.lihaoyi.com/post/BuildyourownCommandLinewithANSIescapecodes.html
 */
object Ansi {
    // https://en.wikipedia.org/wiki/ANSI_escape_code#Control_characters
    object CtrlChars {
        const val ESC = '\u001B'
    }

    // https://en.wikipedia.org/wiki/ANSI_escape_code#Fe_Escape_sequences
    object EscSeq {
        const val CSI = '['
    }

    // https://en.wikipedia.org/wiki/ANSI_escape_code#CSI_(Control_Sequence_Introducer)_sequences
    object Csi {
        /** The single byte identifier at the end of a code, e.g. 'm' in "ESC[31;1m" */
        abstract class Identifier(val code: Char) {
            companion object {
                private val identifierObjects = mutableMapOf<Char, Identifier>()
                fun fromCode(code: Char) = identifierObjects[code]
            }

            init {
                @Suppress("LeakingThis") // We aren't using "this" in a dangerous way
                identifierObjects[code] = this
            }

            final override fun toString() = code.toString()
        }

        object Identifiers {
            object CURSOR_UP : Identifier('A')
            object CURSOR_DOWN : Identifier('B')
            object CURSOR_RIGHT : Identifier('C')
            object CURSOR_LEFT : Identifier('D')
            object CURSOR_PREV_LINE : Identifier('F')
            object ERASE_LINE : Identifier('K')
            object SGR : Identifier('m')
            object KEYCODE : Identifier('~')
        }

        /** The full code for this command, e.g. the "31;1m" part of "ESC[31;1m" */
        open class Code(val value: String) {
            fun toFullEscapeCode(): String = "${CtrlChars.ESC}${EscSeq.CSI}${value}"
            override fun equals(other: Any?): Boolean {
                return other is Code && other.value == value
            }

            override fun hashCode(): Int = value.hashCode()
        }

        object Codes {
            object Cursor {
                object MOVE_TO_PREV_LINE : Code("1${Identifiers.CURSOR_PREV_LINE}")
            }

            object Erase {
                object CURSOR_TO_LINE_END : Code("0${Identifiers.ERASE_LINE}")
            }

            // https://en.wikipedia.org/wiki/ANSI_escape_code#SGR_(Select_Graphic_Rendition)_parameters
            object Sgr {
                object RESET : Code("0${Identifiers.SGR}")

                object Decorations {
                    object BOLD : Code("1${Identifiers.SGR}")
                    object UNDERLINE : Code("4${Identifiers.SGR}")
                    object STRIKETHROUGH : Code("9${Identifiers.SGR}")
                }

                object Colors {
                    object Fg {
                        object BLACK : Code("30${Identifiers.SGR}")
                        object RED : Code("31${Identifiers.SGR}")
                        object GREEN : Code("32${Identifiers.SGR}")
                        object YELLOW : Code("33${Identifiers.SGR}")
                        object BLUE : Code("34${Identifiers.SGR}")
                        object MAGENTA : Code("35${Identifiers.SGR}")
                        object CYAN : Code("36${Identifiers.SGR}")
                        object WHITE : Code("37${Identifiers.SGR}")

                        object BLACK_BRIGHT : Code("30;1${Identifiers.SGR}")
                        object RED_BRIGHT : Code("31;1${Identifiers.SGR}")
                        object GREEN_BRIGHT : Code("32;1${Identifiers.SGR}")
                        object YELLOW_BRIGHT : Code("33;1${Identifiers.SGR}")
                        object BLUE_BRIGHT : Code("34;1${Identifiers.SGR}")
                        object MAGENTA_BRIGHT : Code("35;1${Identifiers.SGR}")
                        object CYAN_BRIGHT : Code("36;1${Identifiers.SGR}")
                        object WHITE_BRIGHT : Code("37;1${Identifiers.SGR}")
                    }

                    object Bg {
                        object BLACK : Code("40${Identifiers.SGR}")
                        object RED : Code("41${Identifiers.SGR}")
                        object GREEN : Code("42${Identifiers.SGR}")
                        object YELLOW : Code("43${Identifiers.SGR}")
                        object BLUE : Code("44${Identifiers.SGR}")
                        object MAGENTA : Code("45${Identifiers.SGR}")
                        object CYAN : Code("46${Identifiers.SGR}")
                        object WHITE : Code("47${Identifiers.SGR}")

                        object BLACK_BRIGHT : Code("40;1${Identifiers.SGR}")
                        object RED_BRIGHT : Code("41;1${Identifiers.SGR}")
                        object GREEN_BRIGHT : Code("42;1${Identifiers.SGR}")
                        object YELLOW_BRIGHT : Code("43;1${Identifiers.SGR}")
                        object BLUE_BRIGHT : Code("44;1${Identifiers.SGR}")
                        object MAGENTA_BRIGHT : Code("45;1${Identifiers.SGR}")
                        object CYAN_BRIGHT : Code("46;1${Identifiers.SGR}")
                        object WHITE_BRIGHT : Code("47;1${Identifiers.SGR}")
                    }
                }
            }

            object Keys {
                object HOME : Code("1${Identifiers.KEYCODE}")
                object END : Code("4${Identifiers.KEYCODE}")
                object DELETE : Code("3${Identifiers.KEYCODE}")

                object UP : Code("${Identifiers.CURSOR_UP}")
                object DOWN : Code("${Identifiers.CURSOR_DOWN}")
                object LEFT : Code("${Identifiers.CURSOR_LEFT}")
                object RIGHT : Code("${Identifiers.CURSOR_RIGHT}")
            }
        }
    }
}