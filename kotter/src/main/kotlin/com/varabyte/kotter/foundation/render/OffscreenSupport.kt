package com.varabyte.kotter.foundation.render

import com.varabyte.kotter.runtime.SectionState
import com.varabyte.kotter.runtime.internal.TerminalCommand
import com.varabyte.kotter.runtime.internal.ansi.commands.NEWLINE_COMMAND
import com.varabyte.kotter.runtime.render.RenderScope
import com.varabyte.kotter.runtime.render.Renderer

class OffscreenBuffer(private val parentScope: RenderScope, render: RenderScope.() -> Unit) {
    private val offscreenRenderer = Renderer(parentScope.renderer.app, autoAppendNewline = false)
    private val offscreenScope = RenderScope(offscreenRenderer).apply(render)

    val lineLengths = offscreenScope.renderer.textArea.lineLengths

    fun width(row: Int): Int {
        require(row in lineLengths.indices) { "Row out of bounds. Expected in [0, ${lineLengths.size}), got $row" }
        return lineLengths[row]
    }

    fun createRenderer() = CommandRenderer(parentScope, offscreenRenderer.commands)
}

class CommandRenderer internal constructor(
    private val targetScope: RenderScope,
    private val commands: List<TerminalCommand>
) {
    private var commandIndex = 0
    private var lastState: SectionState? = null

    /**
     * Render a single row of commands.
     *
     * The reason to render a single row instead of all at once is because it's expected that you are wrapping
     * this content with some external output, e.g. an outer border around some inner text. Here, you'd render the
     * left wall of the border, for example, a row of content, the right wall of the border, etc.
     */
    fun renderNextRow(): Boolean {
        if (commandIndex == commands.size) {
            return false
        }

        targetScope.scopedState {
            lastState?.let { targetScope.state = it }
            while (commandIndex < commands.size) {
                if (commands[commandIndex] === NEWLINE_COMMAND) {
                    ++commandIndex
                    break
                }

                if (commands[commandIndex] !== NEWLINE_COMMAND) {
                    targetScope.applyCommand(commands[commandIndex])
                    ++commandIndex
                }
            }
            lastState = targetScope.state
        }

        return true
    }
}


/**
 * An offscreen block lets you create a temporary internal section that doesn't render until you're ready for it to.
 *
 * This method returns an intermediate object which can then render a row at a time using a buffer renderer:
 *
 * ```
 * val buffer = offscreen { ... }
 * val renderer = buffer.createRenderer()
 * while (true) {
 *   renderer.renderNextRow() // Adds offscreen commands to our current render scope
 *   textLine()
 * }
 * ```
 *
 * This is mainly useful for layout purposes, where you can calculate the size of what the render area will be, e.g.
 * to wrap things with a border.
 */
fun RenderScope.offscreen(render: RenderScope.() -> Unit): OffscreenBuffer {
    return OffscreenBuffer(this, render)
}