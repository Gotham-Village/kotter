package com.varabyte.kotter.foundation.text

import com.varabyte.kotter.runtime.internal.ansi.Ansi.Csi.Codes
import com.varabyte.kotterx.test.foundation.testSession
import com.varabyte.kotterx.test.terminal.lines
import com.varabyte.truthish.assertThat
import kotlin.test.Test

class TextSupportTest {
    @Test
    fun `trivial textLine calls in section`() = testSession { terminal ->
        section {
            textLine("Line 1")
            textLine("Line 2")
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "Line 1",
            "Line 2",
            Codes.Sgr.RESET.toFullEscapeCode(),
        ).inOrder()
    }

    @Test
    fun `trivial text calls in section`() = testSession { terminal ->
        section {
            text("Line 1")
            text("Line 2")
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "Line 1Line 2${Codes.Sgr.RESET}",
            "", // Newline always added at the end of a section
        ).inOrder()
    }

    @Test
    fun `simple text effects work`() = testSession { terminal ->
        section {
            bold { red { text("Text") } }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            // Note: The order of the codes don't match the declaration above, because internally all states are
            // collected and applied in a somewhat arbitrary order that is implementation specific.
            "${Codes.Sgr.Colors.Fg.RED}${Codes.Sgr.Decorations.BOLD}Text${Codes.Sgr.RESET}",
            "", // Newline always added at the end of a section
        ).inOrder()
    }

    @Test
    fun `color arguments work`() = testSession { terminal ->
        section {
            green(ColorLayer.BG) {
                red(isBright = true) {
                    text("Text")
                }
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            // Note: The order of the codes don't match the declaration above, because internally all states are
            // collected and applied in a somewhat arbitrary order that is implementation specific.
            "${Codes.Sgr.Colors.Fg.RED_BRIGHT}${Codes.Sgr.Colors.Bg.GREEN}Text${Codes.Sgr.RESET}",
            "", // Newline always added at the end of a section
        ).inOrder()
    }

    @Test
    fun `intermediate text effects are discarded`() = testSession { terminal ->
        section {
            bold {
                red {
                    green {
                        blue {
                            clearBold()
                            text("Text")
                        }
                    }
                }
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "${Codes.Sgr.Colors.Fg.BLUE}Text${Codes.Sgr.RESET}",
            "", // Newline always added at the end of a section
        ).inOrder()
    }

    @Test
    fun `paragraph adds space around contents if necessary`() = testSession { terminal ->
        section {
            p {
                textLine("First paragraph")
            }

            p {
                textLine("Middle paragraph A")
            }

            p {
                text("Middle paragraph B")
            }

            p {
                text("Last paragraph")
            }
        }.run()

        assertThat(terminal.lines()).containsExactly(
            "First paragraph", // No space above the first paragraph
            "",
            "Middle paragraph A", // No extra space added above when one paragraph follows a newline
            "",
            "Middle paragraph B",
            "",
            "Last paragraph",
            "",
            "${Codes.Sgr.RESET}",
        ).inOrder()
    }
}