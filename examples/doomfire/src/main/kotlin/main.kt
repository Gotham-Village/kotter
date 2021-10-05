import com.varabyte.konsole.foundation.input.Keys
import com.varabyte.konsole.foundation.input.onKeyPressed
import com.varabyte.konsole.foundation.input.runUntilKeyPressed
import com.varabyte.konsole.foundation.konsoleApp
import com.varabyte.konsole.foundation.text.*
import com.varabyte.konsole.foundation.timer.addTimer
import java.time.Duration
import kotlin.random.Random

// Fire renderer ported from https://fabiensanglard.net/doom_fire_psx/

// Size chosen to produce a ~16:9 final gif render (in my terminal, the line height is higher than char width)
private const val VIEW_WIDTH = 70
private const val VIEW_HEIGHT = 17
private const val MAX_X = VIEW_WIDTH - 1
private const val MAX_Y = VIEW_HEIGHT - 1

@Suppress("unused")
enum class FireColor(val ansiColor: Color) {
    WHITE_BRIGHT(Color.BRIGHT_WHITE),
    WHITE(Color.WHITE),
    YELLOW_BRIGHT(Color.BRIGHT_YELLOW),
    YELLOW(Color.YELLOW),
    RED_BRIGHT(Color.BRIGHT_RED),
    RED(Color.RED),
    BRIGHT_BLACK(Color.BRIGHT_BLACK),
    BLACK(Color.BLACK);

    fun cooler(): FireColor {
        return when (this) {
            BLACK -> BLACK
            else -> values()[ordinal + 1]
        }
    }
}

operator fun Array<FireColor>.get(x: Int, y: Int) = this[y * VIEW_WIDTH + x]
operator fun Array<FireColor>.set(x: Int, y: Int, value: FireColor) {
    this[y * VIEW_WIDTH + x] = value
}

class DoomFireModel {
    val buffer = Array(VIEW_WIDTH * VIEW_HEIGHT) { FireColor.BLACK }
    private var isFireOn = false

    init {
        assert(!isFireOn)
        toggleFire()
    }

    fun update() {
        if (!isFireOn) {
            // Decaying the source of the fire will eventually starve the rest of it
            for (x in 0..MAX_X) {
                buffer[x, MAX_Y] = buffer[x, MAX_Y].cooler()
            }
        }

        for (y in 0 until MAX_Y) { // until: Always leave the last Y line alone, it's the source of the fire
            for (x in 0..MAX_X) {
                val srcColor = buffer[x, y + 1]
                var dstColor = FireColor.BLACK
                var xFinal = x
                if (srcColor != FireColor.BLACK) {
                    val shouldDecay = (Random.nextFloat() > 0.4)
                    val xOffsetRandomness = (Random.nextFloat() * 3.0).toInt() - 1 // Windy to the left
                    dstColor = if (shouldDecay) srcColor.cooler() else srcColor
                    xFinal = (x - xOffsetRandomness + VIEW_WIDTH) % VIEW_WIDTH // Wrap x
                }
                buffer[xFinal, y] = dstColor
            }
        }

    }

    fun toggleFire() {
        isFireOn = !isFireOn
        if (isFireOn) {
            for (x in 0..MAX_X) {
                buffer[x, MAX_Y] = FireColor.WHITE_BRIGHT
            }
        }
    }
}

fun main() = konsoleApp {
    konsole {
        p {
            textLine("Press SPACE to toggle fire on and off")
            textLine("Press Q to quit")
        }
    }.run()

    val doomFire = DoomFireModel()
    konsole {
        for (y in 0..MAX_Y) {
            for (x in 0..MAX_X) {
                val fireColor = doomFire.buffer[x, y]
                color(fireColor.ansiColor)
                if (fireColor == FireColor.BLACK) {
                    text(" ")
                } else {
                    text("*")
                }
            }
            textLine()
        }
    }.runUntilKeyPressed(Keys.Q) {
        onKeyPressed {
            if (key == Keys.SPACE) {
                doomFire.toggleFire()
            }
        }
        addTimer(Duration.ofMillis(50), repeat = true) {
            doomFire.update()
            rerender()
        }
    }
}