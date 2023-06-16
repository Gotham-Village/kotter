package com.varabyte.kotterx.test.foundation

import com.varabyte.kotter.foundation.session
import com.varabyte.kotter.runtime.Session
import com.varabyte.kotterx.test.terminal.TestTerminal

/**
 * Like a [session] block but backed by a [TestTerminal], which is provides as a lambda argument.
 *
 * The [TestTerminal] is a useful way to assert that various Kotter commands resulted in expected behavior.
 */
fun testSession(
    provideHeight: (() -> Int)? = null,
    block: Session.(TestTerminal) -> Unit
) {
    val terminal = TestTerminal(provideHeight)
    session(terminal = terminal) {
        this.block(terminal)
    }
}
