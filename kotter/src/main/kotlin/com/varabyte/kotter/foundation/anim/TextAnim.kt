package com.varabyte.kotter.foundation.anim

import com.varabyte.kotter.foundation.text.text
import com.varabyte.kotter.foundation.text.textLine
import com.varabyte.kotter.runtime.Section
import com.varabyte.kotter.runtime.Session
import com.varabyte.kotter.runtime.render.RenderScope
import java.time.Duration

/**
 * An [Anim] that renders some text, which gets updated on each new frame.
 *
 * To reference a text animation, just convert it to a string inside a [Section].
 *
 * Using one looks like this:
 *
 * ```
 * session {
 *   val waitingAnim = textAnimOf(listOf("", ".", "..", "..."), frameDuration = Duration.ofMillis(250))
 *   section {
 *     text("Thinking$waitingAnim")
 *   }.runUntilSignal { ... }
 * }
 * ```
 */
class TextAnim internal constructor(session: Session, private val template: Template)
    : Anim(session, template.frames.size, template.frameDuration) {

    /**
     * A template for a text animation, useful if you want to define an animation once but instantiate several copies of
     * it throughout your program.
     */
    class Template(val frames: List<String>, val frameDuration: Duration) {
        init {
            require(!frameDuration.isNegative && !frameDuration.isZero) { "Invalid animation created with non-positive frame length" }
            require(frames.isNotEmpty()) { "Invalid animation created with no frames" }
        }
    }

    private val currText get() = template.frames[currFrame]

    /**
     * We wrap all animation property accesses in this special block which kickstarts the timer for this animation if
     * it hasn't already been done so.
     */
    private fun <R> readProperty(block: () -> R): R {
        requestAnimate()
        return block()
    }

    /** The current frame of text. */
    override fun toString() = readProperty { currText }
}

fun Session.textAnimOf(template: TextAnim.Template) = TextAnim(this, template)
/** Instantiate a [TextAnim] tied to the current [Session]. */
fun Session.textAnimOf(frames: List<String>, frameDuration: Duration) =
    TextAnim(this, TextAnim.Template(frames, frameDuration))

/**
 * Append the current frame of text animation to the current section.
 */
fun RenderScope.text(anim: TextAnim) {
    text(anim.toString())
}

/**
 * Append the current frame of text animation to the current section, followed by a newline.
 */
fun RenderScope.textLine(anim: TextAnim) {
    textLine(anim.toString())
}
