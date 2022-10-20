package com.varabyte.kotter.foundation

import com.varabyte.kotter.runtime.RunScope
import com.varabyte.kotter.runtime.Session
import com.varabyte.kotter.runtime.Section
import net.jcip.annotations.ThreadSafe
import java.lang.ref.WeakReference
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.reflect.KProperty

/**
 * A special variable which can be used to auto-rerender a target [Section] without needing to call
 * [RunScope.rerender] yourself.
 *
 * The way it works is, when this variable is fetched, it is checked if this has happened while we're in an active
 * block:
 *
 * ```
 * var count by liveVarOf(0)
 * section { <-- active section
 *    for (i in 0 until count) { // <-- getValue happens here, gets associated with active block
 *      text("*")
 *    }
 * }.runUntilFinished {
 *   while (count < 5) {
 *     delay(1000)
 *     ++count // <-- setValue happens here, causes active block to rerun
 *   }
 * }
 *
 * count = 123 // Setting count out of a section is technically fine; nothing is triggered
 * ```
 *
 * This class's value can be queried and set across different values, so it is designed to be thread safe.
 */
@ThreadSafe
class LiveVar<T> internal constructor(private val session: Session, private var value: T) {

    private var associatedBlockRef: WeakReference<Section>? = null
    operator fun getValue(thisRef: Any?, prop: KProperty<*>): T {
        return session.data.lock.read {
            associatedBlockRef = session.activeSection?.let { WeakReference(it) }
            value
        }
    }
    operator fun setValue(thisRef: Any?, prop: KProperty<*>, value: T) {
        session.data.lock.write {
            if (this.value != value) {
                this.value = value
                associatedBlockRef?.get()?.let { associatedBlock ->
                    session.activeSection?.let { activeSection ->
                        if (associatedBlock === activeSection) activeSection.requestRerender()
                    } ?: run {
                        // Our old block is finished, no need to keep a reference around to it anymore.
                        associatedBlockRef = null
                    }
                }
            }
        }
    }
}

/** Create a [LiveVar] whose scope is tied to this session. */
fun <T> Session.liveVarOf(value: T): LiveVar<T> = LiveVar(this, value)