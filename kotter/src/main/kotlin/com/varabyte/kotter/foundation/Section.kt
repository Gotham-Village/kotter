package com.varabyte.kotter.foundation

import com.varabyte.kotter.runtime.RunScope
import com.varabyte.kotter.runtime.Section

/**
 * Create a [Section.run] block that blocks until [RunScope.signal] is called.
 *
 * This is a convenience function for shortening the common pattern:
 *
 * ```
 * section { /* ... */ }.run {
 *   onSomeEvent { signal() }
 *   waitForSignal()
 * }
 * ```
 *
 * into just
 *
 * ```
 * section { /* ... */ }.runUntilSignal {
 *   onSomeEvent { signal() }
 * }
 * ```
 */
fun Section.runUntilSignal(block: suspend RunScope.() -> Unit) {
    run {
        block()
        waitForSignal()
    }
}