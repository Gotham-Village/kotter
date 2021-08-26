package com.varabyte.konsole.core

import com.varabyte.konsole.ansi.Ansi
import com.varabyte.konsole.ansi.commands.INVERT_COMMAND
import com.varabyte.konsole.core.internal.MutableKonsoleTextArea
import com.varabyte.konsole.terminal.Terminal
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.max
import kotlin.math.min


class KonsoleBlock internal constructor(
    private val executor: ExecutorService,
    private val terminal: Terminal,
    private val block: KonsoleScope.() -> Unit) {

    companion object {
        private val activeReference = AtomicReference<KonsoleBlock?>(null)
        internal val active get() = activeReference.get()
    }

    class RunScope(
        private val rerenderRequested: () -> Unit
    ) {
        private val waitLatch = CountDownLatch(1)
        fun rerender() = rerenderRequested()
        fun waitForSignal() { waitLatch.await() }
        fun signal() { waitLatch.countDown() }
    }

    private val textArea = MutableKonsoleTextArea()

    private val renderLock = ReentrantLock()
    private var renderRequested = false

    /**
     * A collection of values that might be accessed both within a konsole block AND a run block at the same time, so
     * their access should be guarded behind a lock.
     */
    private class ThreadSafeData(private val terminal: Terminal, private val requestRerender: () -> Unit) {
        /** State needed to support the special `$input` property */
        class InputState {
            var textBuilder = StringBuilder()
            var index = 0
        }

        private val lock = ReentrantLock()

        private lateinit var terminalReaderJob: Job
        private lateinit var _inputState: InputState

        fun <T> inputState(withLock: InputState.() -> T): T {
            return lock.withLock {
                if (!::_inputState.isInitialized) {
                    _inputState = InputState()
                    startReading()
                }
                _inputState.withLock()
            }
        }

        private fun startReading() {
            lock.withLock {
                if (!::terminalReaderJob.isInitialized) {
                    terminalReaderJob = CoroutineScope(Dispatchers.IO).launch {
                        val escSeq = StringBuilder()
                        terminal.read().collect { byte ->
                            val c = byte.toChar()
                            when {
                                escSeq.isNotEmpty() -> {
                                    escSeq.append(c)
                                    val code = Ansi.EscSeq.toCode(escSeq)
                                    if (code != null) {
                                        when(code) {
                                            Ansi.Csi.Codes.Keys.LEFT -> {
                                                inputState { index = (index - 1).coerceAtLeast(0) }
                                            }
                                            Ansi.Csi.Codes.Keys.RIGHT -> {
                                                inputState { index = (index + 1).coerceAtMost(textBuilder.length) }
                                            }
                                            Ansi.Csi.Codes.Keys.HOME -> {
                                                inputState { index = 0 }
                                            }
                                            Ansi.Csi.Codes.Keys.END -> {
                                                inputState { index = textBuilder.length }
                                            }
                                            Ansi.Csi.Codes.Keys.DELETE -> {
                                                inputState {
                                                    if (textBuilder.isNotEmpty()) {
                                                        textBuilder.deleteAt(index)
                                                        index = max(0, min(index, textBuilder.lastIndex))
                                                    }
                                                }
                                            }
                                        }
                                        escSeq.clear()
                                    }
                                }
                                else -> when (byte) {
                                    Ansi.CtrlChars.ESC.code -> escSeq.append(c)
                                    Ansi.CtrlChars.ENTER.code -> {
                                        inputState {
                                            textBuilder.clear()
                                            index = 0
                                        }
                                    }
                                    Ansi.CtrlChars.BACKSPACE.code -> {
                                        inputState {
                                            if (index > 0) {
                                                textBuilder.deleteAt(index - 1)
                                                index--
                                            }
                                        }
                                    }
                                    else -> {
                                        inputState {
                                            textBuilder.insert(index, c)
                                            index += 1 // Otherwise, already index = 0, first char
                                        }
                                    }
                                }
                            }

                            requestRerender() // TODO: Only run this if necessary
                        }
                    }
                }
            }
        }
    }

    private val threadSafeData = ThreadSafeData(terminal) { requestRerender() }

    fun input() {
        threadSafeData.inputState {
            val text = textBuilder.toString()
            for (i in 0 until index) {
                textArea.append(text[i])
            }
            applyCommand(INVERT_COMMAND)
            textArea.append(text.elementAtOrNull(index) ?: ' ')
            applyCommand(INVERT_COMMAND)
            for (i in (index + 1)..text.lastIndex) {
                textArea.append(text[i])
            }
        }
    }

    internal fun applyCommand(command: KonsoleCommand) {
        command.applyTo(textArea)
    }

    internal fun requestRerender() {
        renderLock.withLock {
            // If we get multiple render requests in a short period of time, we only need to handle one of them - the
            // remaining requests are redundant and will be covered by the initial one.
            if (!renderRequested) {
                renderRequested = true
                renderOnceAsync()
            }
        }
    }

    private fun renderOnceAsync(): Job {
        val self = this
        return CoroutineScope(executor.asCoroutineDispatcher()).launch {
            renderLock.withLock { renderRequested = false }

            val clearBlockCommand = buildString {
                if (!textArea.isEmpty()) {
                    // To clear an existing block of 'n' lines, completely delete all but one of them, and then delete the
                    // last one down to the beginning (in other words, don't consume the \n of the previous line)
                    for (i in 0 until textArea.numLines) {
                        append('\r')
                        append(Ansi.Csi.Codes.Erase.CURSOR_TO_LINE_END.toFullEscapeCode())
                        if (i < textArea.numLines - 1) {
                            append(Ansi.Csi.Codes.Cursor.MOVE_TO_PREV_LINE.toFullEscapeCode())
                        }
                    }
                }
            }

            textArea.clear()
            KonsoleScope(self).block()
            // Send the whole set of instructions through "write" at once so the clear and updates are processed
            // in one pass.
            terminal.write(clearBlockCommand + textArea.toString())
        }
    }
    private fun renderOnce() = runBlocking {
        renderOnceAsync().join()
    }

    /** Mark this block as active. Only one block can be active at a time! */
    private fun activate(block: () -> Unit) {
        if (!activeReference.compareAndSet(null, this)) {
            throw IllegalStateException("Cannot run this Konsole block while another block is already running")
        }
        block()
        activeReference.set(null)
    }

    fun run(block: (suspend RunScope.() -> Unit)? = null) {
        activate {
            activeReference.set(this)
            renderOnce()
            if (block != null) {
                val job = CoroutineScope(Dispatchers.Default).launch {
                    val scope = RunScope(rerenderRequested = { requestRerender() })
                    scope.block()
                }

                runBlocking { job.join() }
            }
        }
    }
}

fun KonsoleBlock.runUntilSignal(block: suspend KonsoleBlock.RunScope.() -> Unit) {
    run {
        block()
        waitForSignal()
    }
}

// TODO: fun KonsoleBlock.runUntilTextEntered(block: suspend RunUntilScope.() -> Unit) { ... }