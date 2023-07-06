package com.varabyte.kotterx.text

import com.varabyte.kotter.foundation.render.*
import com.varabyte.kotter.foundation.text.*
import com.varabyte.kotter.runtime.render.*
import com.varabyte.kotter.runtime.render.OffscreenRenderScope

/**
 * Shift the contents of this buffer over to the right by some [amount].
 */
fun RenderScope.shiftRight(
    amount: Int = 0, render: OffscreenRenderScope.() -> Unit
) {
    val content = offscreen(render)
    val renderer = content.createRenderer()
    while (renderer.hasNextRow()) {
        repeat(amount) { text(' ') }
        renderer.renderNextRow()
        textLine()
    }
}
