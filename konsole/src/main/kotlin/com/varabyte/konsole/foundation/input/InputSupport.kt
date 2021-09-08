package com.varabyte.konsole.foundation.input

import com.varabyte.konsole.foundation.anim.KonsoleAnim
import com.varabyte.konsole.foundation.runUntilSignal
import com.varabyte.konsole.foundation.text.*
import com.varabyte.konsole.foundation.timer.addTimer
import com.varabyte.konsole.runtime.KonsoleApp
import com.varabyte.konsole.runtime.KonsoleBlock
import com.varabyte.konsole.runtime.RenderScope
import com.varabyte.konsole.runtime.concurrent.ConcurrentScopedData
import com.varabyte.konsole.runtime.internal.ansi.Ansi
import com.varabyte.konsole.runtime.terminal.Terminal
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.mapNotNull
import java.time.Duration

private object KeyFlowKey : ConcurrentScopedData.Key<Flow<Key>> {
    // Once created, we keep it alive for the app, because Flow is designed to be collected multiple times, meaning
    // there's no reason for us to keep recreating it. It's pretty likely that if an app uses input in one block, it
    // will use input again in others. (We can always revisit this decision later and scope this to a KonsoleBlock
    // lifecycle)
    override val lifecycle = KonsoleApp.Lifecycle
}

/**
 * Create a [Flow<Key>] value which converts the ANSI values read from a terminal into Konsole's simpler abstraction
 * (which is a flat collection of keys instead of multi-encoded bytes and other historical legacy).
 */
private fun ConcurrentScopedData.prepareKeyFlow(terminal: Terminal) {
    tryPut(KeyFlowKey) {
        val escSeq = StringBuilder()
        terminal.read().mapNotNull { byte ->
            val c = byte.toChar()
            when {
                escSeq.isNotEmpty() -> {
                    escSeq.append(c)
                    val code = Ansi.EscSeq.toCode(escSeq)
                    if (code != null) {
                        escSeq.clear()
                        when (code) {
                            Ansi.Csi.Codes.Keys.UP -> Keys.UP
                            Ansi.Csi.Codes.Keys.DOWN -> Keys.DOWN
                            Ansi.Csi.Codes.Keys.LEFT -> Keys.LEFT
                            Ansi.Csi.Codes.Keys.RIGHT -> Keys.RIGHT
                            Ansi.Csi.Codes.Keys.HOME -> Keys.HOME
                            Ansi.Csi.Codes.Keys.END -> Keys.END
                            Ansi.Csi.Codes.Keys.DELETE -> Keys.DELETE
                            else -> null
                        }
                    } else {
                        null
                    }
                }
                else -> {
                    when (c) {
                        Ansi.CtrlChars.ESC -> {
                            escSeq.append(c); null
                        }
                        Ansi.CtrlChars.ENTER -> Keys.ENTER
                        Ansi.CtrlChars.BACKSPACE -> Keys.BACKSPACE
                        else -> if (!c.isISOControl()) CharKey(c) else null
                    }
                }
            }
        }
    }
}

/** State needed to support the `input()` function */
private class InputState {
    object Key : ConcurrentScopedData.Key<InputState> {
        override val lifecycle = KonsoleBlock.Lifecycle
    }
    companion object {
        private const val BLINKING_DURATION_MS = 500
    }

    var text = ""
        set(value) {
            if (field != value) {
                field = value
                resetCursor()
            }
        }
    var index = 0
        set(value) {
            if (field != value) {
                field = value
                resetCursor()
            }
        }
    var blinkOn = true
    var blinkElapsedMs = 0

    private fun resetCursor() {
        blinkOn = true
        blinkElapsedMs = 0
    }

    /** Elapse the timer on this input state's cursor animation, returning true if the cursor actually changed. */
    fun elapse(duration: Duration): Boolean {
        val prevBlinkOn = blinkOn
        blinkElapsedMs += duration.toMillis().toInt()
        while (blinkElapsedMs > BLINKING_DURATION_MS) {
            blinkElapsedMs -= BLINKING_DURATION_MS
            blinkOn = !blinkOn
        }
        return prevBlinkOn != blinkOn
    }
}

private object UpdateInputJobKey : ConcurrentScopedData.Key<Job> {
    override val lifecycle = KonsoleBlock.RunScope.Lifecycle
}

private object OnlyCalledOncePerRenderKey : ConcurrentScopedData.Key<Unit> {
    override val lifecycle = RenderScope.Lifecycle
}

/**
 * If necessary, instantiate data that the [input] method expects to exist.
 *
 * Is a no-op after the first time.
 */
private fun ConcurrentScopedData.prepareInput(scope: RenderScope) {
    if (!tryPut(OnlyCalledOncePerRenderKey) { }) {
        throw IllegalStateException("Calling `input` more than once in a render pass is not supported")
    }

    prepareKeyFlow(scope.block.app.terminal)
    if (tryPut(InputState.Key) { InputState() }) {
        val state = get(InputState.Key)!!
        addTimer(KonsoleAnim.ONE_FRAME_60FPS, repeat = true) {
            if (state.elapse(elapsed)) {
                scope.block.requestRerender()
            }
        }
        scope.block.onFinishing {
            if (state.blinkOn) {
                state.blinkOn = false
                scope.block.requestRerender()
            }
        }
    }
    tryPut(
        UpdateInputJobKey,
        provideInitialValue = {
            CoroutineScope(Dispatchers.IO).launch {
                getValue(KeyFlowKey).collect { key ->
                    get(InputState.Key) {
                        val prevText = text
                        val prevIndex = index
                        var proposedText: String? = null
                        var proposedIndex: Int? = null
                        when (key) {
                            Keys.LEFT -> index = (index - 1).coerceAtLeast(0)
                            Keys.RIGHT -> {
                                if (index < text.length) {
                                    index++
                                }
                                else {
                                    get(CompleterKey) {
                                        complete(text)?.let { completion ->
                                            val finalText = text + completion
                                            proposedText = finalText
                                            proposedIndex = finalText.length
                                        }
                                    }
                                }
                            }
                            Keys.HOME -> index = 0
                            Keys.END -> index = text.length
                            Keys.DELETE -> {
                                if (index <= text.lastIndex) {
                                    proposedText = text.removeRange(index, index + 1)
                                }
                            }

                            Keys.BACKSPACE -> {
                                if (index > 0) {
                                    proposedText = text.removeRange(index - 1, index)
                                    proposedIndex = index - 1
                                }
                            }

                            Keys.ENTER -> {
                                get(InputEnteredCallbackKey) { this.invoke(OnInputEnteredScope(text)) }
                                get(SystemInputEnteredCallbackKey) { this.invoke() }
                            }
                            else ->
                                if (key is CharKey) {
                                    proposedText = "${text.take(index)}${key.code}${text.takeLast(text.length - index)}"
                                    proposedIndex = index + 1
                                }
                        }

                        if (proposedText != null) {
                            get(InputChangedCallbacksKey) {
                                val scope = OnInputChangedScope(input = proposedText!!, prevInput = text)
                                forEach { callback -> scope.callback() }

                                proposedText = if (!scope.rejected) scope.input else scope.prevInput
                            }

                            text = proposedText!!
                            index = (proposedIndex ?: index).coerceIn(0, text.length)
                        }

                        if (text != prevText || index != prevIndex) {
                            scope.block.requestRerender()
                        }
                    }
                }
            }
        },
        dispose = { job -> job.cancel() }
    )
}

interface InputCompleter {
    /**
     * Given some [input], return a suffix that should complete it, or null if the string does not have a matching
     * completion.
     *
     * For example, for "y", you might return "es"
     */
    fun complete(input: String): String?

    val color: Color get() = Color.BRIGHT_BLACK
}

/**
 * A default [InputCompleter] that provides completions given a list of values.
 *
 * If there are multiple matches, e.g. "Colorado" and "Connecticut" for "co", the item earlier in the list will be
 * suggested as the completion.
 */
open class Completions(private vararg val values: String, private val ignoreCase: Boolean = true) : InputCompleter {
    override fun complete(input: String): String? {
        return values.firstOrNull { value ->
            value.startsWith(input, ignoreCase)
        }?.substring(input.length)
    }
}

private object CompleterKey : ConcurrentScopedData.Key<InputCompleter> {
    override val lifecycle = KonsoleBlock.Lifecycle
}


fun RenderScope.input(completer: InputCompleter? = null) {
    data.prepareInput(this)
    completer?.let { data[CompleterKey] = it }

    data.get(InputState.Key) {
        val completion = completer?.complete(text) ?: ""
        // Note: Trailing space as cursor can be put AFTER last character
        val finalText = "$text$completion "

        scopedState { // Make sure color changes don't leak
            for (i in finalText.indices) {
                if (i == text.length && completer != null && completion.isNotEmpty()) {
                    color(completer.color)
                }
                if (i == index && blinkOn) {
                    invert()
                }
                text(finalText[i])
                if (i == index && blinkOn) {
                    clearInvert()
                }
            }
        }
    }
}

class OnKeyPressedScope(val key: Key)

private object KeyPressedJobKey : ConcurrentScopedData.Key<Job> {
    override val lifecycle = KonsoleBlock.RunScope.Lifecycle
}
private object KeyPressedCallbackKey : ConcurrentScopedData.Key<OnKeyPressedScope.() -> Unit> {
    override val lifecycle = KonsoleBlock.RunScope.Lifecycle
}
// Note: We create a separate key here from above to ensure we can trigger the system callback only AFTER the user
// callback was triggered. That's because the system handler may fire a signal which, if sent out too early, could
// result in the user callback not getting a chance to run.
private object SystemKeyPressedCallbackKey : ConcurrentScopedData.Key<OnKeyPressedScope.() -> Unit> {
    override val lifecycle = KonsoleBlock.RunScope.Lifecycle
}

/**
 * Start running a job that collects keypresses and sends them to callbacks.
 *
 * This is a no-op when called after the first time.
 */
private fun ConcurrentScopedData.prepareOnKeyPressed(terminal: Terminal) {
    prepareKeyFlow(terminal)
    tryPut(
        KeyPressedJobKey,
        provideInitialValue = {
            CoroutineScope(Dispatchers.IO).launch {
                getValue(KeyFlowKey).collect { key ->
                    val scope = OnKeyPressedScope(key)
                    get(KeyPressedCallbackKey) { this.invoke(scope) }
                    get(SystemKeyPressedCallbackKey) { this.invoke(scope) }
                }
            }
        },
        dispose = { job -> job.cancel() }
    )
}

fun KonsoleBlock.RunScope.onKeyPressed(listener: OnKeyPressedScope.() -> Unit) {
    data.prepareOnKeyPressed(block.app.terminal)
    if (!data.tryPut(KeyPressedCallbackKey) { listener }) {
        throw IllegalStateException("Currently only one `onKeyPressed` callback at a time is supported.")
    }
}

fun KonsoleBlock.runUntilKeyPressed(vararg keys: Key, block: suspend KonsoleBlock.RunScope.() -> Unit = {}) {
    runUntilSignal {
        data.prepareOnKeyPressed(this.block.app.terminal)
        data[SystemKeyPressedCallbackKey] = { if (keys.contains(key)) signal() }
        block()
    }
}

class OnInputChangedScope(var input: String, val prevInput: String) {
    internal var rejected = false
    fun rejectInput() { rejected = true }
}
private object InputChangedCallbacksKey : ConcurrentScopedData.Key<MutableList<OnInputChangedScope.() -> Unit>> {
    override val lifecycle = KonsoleBlock.RunScope.Lifecycle
}

fun KonsoleBlock.RunScope.onInputChanged(listener: OnInputChangedScope.() -> Unit) {
    data.putIfAbsent(InputChangedCallbacksKey, provideInitialValue = { mutableListOf() }) { add(listener) }
}

class OnInputEnteredScope(val input: String)
private object InputEnteredCallbackKey : ConcurrentScopedData.Key<OnInputEnteredScope.() -> Unit> {
    override val lifecycle = KonsoleBlock.RunScope.Lifecycle
}

// Note: We create a separate key here from above to ensure we can trigger the system callback only AFTER the user
// callback was triggered. That's because the system handler may fire a signal which, if sent out too early, could
// result in the user callback not getting a chance to run.
private object SystemInputEnteredCallbackKey : ConcurrentScopedData.Key<() -> Unit> {
    override val lifecycle = KonsoleBlock.RunScope.Lifecycle
}

fun KonsoleBlock.RunScope.onInputEntered(listener: OnInputEnteredScope.() -> Unit) {
    if (!data.tryPut(InputEnteredCallbackKey) { listener }) {
        throw IllegalStateException("Currently only one `onInputEntered` callback at a time is supported.")
    }
}

fun KonsoleBlock.runUntilInputEntered(block: suspend KonsoleBlock.RunScope.() -> Unit = {}) {
    runUntilSignal {
        data[SystemInputEnteredCallbackKey] = { signal() }
        block()
    }
}