import Cells.State
import com.varabyte.konsole.foundation.input.Keys
import com.varabyte.konsole.foundation.input.onKeyPressed
import com.varabyte.konsole.foundation.input.runUntilKeyPressed
import com.varabyte.konsole.foundation.konsoleApp
import com.varabyte.konsole.foundation.text.*
import kotlinx.coroutines.delay
import kotlin.random.Random

private const val WIDTH = 60
private const val HEIGHT = 30

// The rules of life: https://en.wikipedia.org/wiki/Conway%27s_Game_of_Life
class Cells {
    enum class State {
        DEAD,
        ALIVE,
        BORN,
        DYING;

        fun isAlive(): Boolean = (this === BORN || this === ALIVE)
        fun isDead() = !isAlive()
    }

    private val buffer = Array(WIDTH * HEIGHT) { State.DEAD }
    private operator fun Array<State>.get(x: Int, y: Int) = buffer[y * WIDTH + x]
    private operator fun Array<State>.set(x: Int, y: Int, state: State) { buffer[y * WIDTH + x] = state }
    operator fun get(x: Int, y: Int) = buffer[x, y]

    var onChanged: () -> Unit = {}

    init {
        randomize()
    }

    fun randomize() {
        for (i in buffer.indices) { buffer[i] = if (Random.nextFloat() > 0.9) State.ALIVE else State.DEAD }
        // The first frame is chaotic but settles after the first few steps
        step()
        step()
    }

    private fun Array<State>.countNeighbors(x: Int, y: Int): Int {
        var count = 0
        for (dx in listOf(-1, 0, 1)) {
            for (dy in listOf(-1, 0, 1)) {
                if (dx != 0 || dy != 0) {
                    val neighborX = x + dx
                    val neighborY = y + dy
                    if (neighborX in 0 until WIDTH && neighborY in 0 until HEIGHT && this[neighborX, neighborY].isAlive()) {
                        ++count
                    }
                }
            }
        }
        return count
    }

    fun step() {
        // Make a copy because we're going to modify `buffer` in place but need to compare against the old values
        val bufferCurr = buffer.copyOf()
        for (x in 0 until WIDTH) {
            for (y in 0 until HEIGHT) {
                val neighborCount = bufferCurr.countNeighbors(x, y)
                val alive = when(bufferCurr[x, y].isAlive()) {
                    true -> neighborCount == 2 || neighborCount == 3
                    false -> neighborCount == 3
                }
                if (alive) {
                    buffer[x, y] = if (bufferCurr[x, y].isDead()) State.BORN else State.ALIVE
                }
                else {
                    buffer[x, y] = if (bufferCurr[x, y].isAlive()) State.DYING else State.DEAD
                }
            }
        }

        onChanged()
    }
}

fun main() = konsoleApp {
    val cells = Cells()

    // Instructions never need to change; output them first
    konsole {
        p {
            textLine("space: play/pause")
            textLine("right: step one frame")
            textLine("r: randomize")
            textLine("q: quit")
        }
    }.run()

    konsole {
        text("+")
        text("-".repeat(WIDTH))
        textLine("+")

        for (y in 0 until HEIGHT) {
            text("|")
            for (x in 0 until WIDTH) {
                val state = cells[x, y]
                scopedState {
                    @Suppress("NON_EXHAUSTIVE_WHEN")
                    when (state) {
                        State.BORN -> green()
                        State.DYING -> red()
                    }
                    text(when (state) {
                        State.DEAD -> ' '
                        else -> '*'
                    })
                }
            }
            textLine("|")
        }

        text("+")
        text("-".repeat(WIDTH))
        textLine("+")
        textLine()
    }.runUntilKeyPressed(Keys.Q) {
        var paused = false
        cells.onChanged = { rerender() }

        onKeyPressed {
            when (key) {
                Keys.SPACE -> paused = !paused
                Keys.R -> cells.randomize()
                Keys.RIGHT -> {
                    paused = true
                    cells.step()
                }
            }
        }

        while (true) {
            delay(100)
            if (!paused) {
                cells.step()
            }
        }
    }
}