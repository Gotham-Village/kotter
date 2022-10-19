package com.varabyte.kotterx.test.runtime

import com.varabyte.kotter.runtime.RunScope
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.write

/**
 * Block the current run block until a render has occurred where the passed in [condition] is true.
 *
 * This can be useful to do because, due to the threading nature of Kotter (e.g. one thread gets notified, which
 * notifies another, which causes a render to happen on a third), occasionally it's not possible to predict if logic
 * will all execute in one pass or multiple passes. In that case, if you at least know that renders will eventually
 * settle on some consistent output, you can use this method to wait for that.
 *
 * A very useful pattern is to test the terminal's state:
 *
 * ```
 * testSession { terminal ->
 *   section { ... }.run {
 *     // do some stuff and then...
 *     blockUntilRenderWhen {
 *       terminal.resolveRerenders() == listOf(
 *         "expected line 1",
 *         "expected line 2",
 *         "",
 *       )
 *     }
 *   }
 * }
 * ```
 */
fun RunScope.blockUntilRenderWhen(condition: () -> Boolean) {
    val latch = CountDownLatch(1)

    // Prevent the section from starting a render pass until we're sure we've registered our callback
    // Or, if a render is in progress, this will wait for it to finish first.
    data.lock.write {
        if (condition()) return

        section.onRendered {
            if (condition()) {
                removeListener = true
                latch.countDown()
            }
        }
    }
    latch.await()
}