import com.varabyte.konsole.foundation.input.Keys
import com.varabyte.konsole.foundation.input.input
import com.varabyte.konsole.foundation.input.onInputEntered
import com.varabyte.konsole.foundation.input.onKeyPressed
import com.varabyte.konsole.foundation.input.runUntilKeyPressed
import com.varabyte.konsole.foundation.konsoleApp
import com.varabyte.konsole.foundation.konsoleVarOf
import com.varabyte.konsole.foundation.text.HSV
import com.varabyte.konsole.foundation.text.hsv
import com.varabyte.konsole.foundation.text.text
import com.varabyte.konsole.foundation.text.textLine
import kotlin.math.roundToInt

const val W = 40
const val H = 15

private fun Int.toHexStr() = this.toString(16).padStart(2, '0')
private fun Float.toPercentStr() = "${(this * 100).roundToInt()}%"

fun main() = konsoleApp {
    // Instructions never need to change; output them first
    konsole {
        textLine()
        textLine("h: enter hue")
        textLine("arrows: move picker (changes saturation and value)")
        textLine("home/end: saturation to 0/100%")
        textLine("pg up/down: value to 0/100%")
        textLine("Press Q to quit")
        textLine()
        textLine("NOTE: If colors look chunky, your terminal may not support true colors")
        textLine()
    }.run()

    // Instructions never need to change; output them first
    var xCurr by konsoleVarOf(0)
    var yCurr by konsoleVarOf(0)
    var h by konsoleVarOf(0)
    var enteringHue by konsoleVarOf(false)
    konsole {
        val sCurr = (xCurr / W.toFloat())
        val vCurr = 1f - (yCurr / H.toFloat())

        for (y in 0..H) {
            for (x in 0..W) {
                val s = x / W.toFloat()
                val v = 1f - (y / H.toFloat())

                if (xCurr == x && yCurr == y) {
                    text("░")
                }
                else {
                    hsv(h, s, v) {
                        text("█")
                    }
                }
            }
            textLine()
        }

        val rgb = HSV(h, sCurr, vCurr).toRgb()
        textLine()
        if (enteringHue) {
            text("Enter hue (0 - 360): "); input(); textLine()
        }
        else {
            textLine("HSV: $h°, ${sCurr.toPercentStr()}, ${vCurr.toPercentStr()}")
            textLine("RGB: ${rgb.r}, ${rgb.g}, ${rgb.b} (0x${rgb.r.toHexStr()}${rgb.g.toHexStr()}${rgb.b.toHexStr()})")
        }
        textLine()
    }.runUntilKeyPressed(Keys.Q) {
        onKeyPressed {
            if (!enteringHue) {
                when (key) {
                    Keys.LEFT -> xCurr = (xCurr - 1).coerceAtLeast(0)
                    Keys.RIGHT -> xCurr = (xCurr + 1).coerceAtMost(W)
                    Keys.UP -> yCurr = (yCurr - 1).coerceAtLeast(0)
                    Keys.DOWN -> yCurr = (yCurr + 1).coerceAtMost(H)
                    Keys.HOME -> xCurr = 0
                    Keys.END -> xCurr = W
                    Keys.PAGE_UP -> yCurr = 0
                    Keys.PAGE_DOWN -> yCurr = H
                    Keys.H -> enteringHue = true
                }
            }
        }

        onInputEntered {
            enteringHue = false
            input.toIntOrNull()?.let { hIn ->
                if (hIn in (0 .. 360)) {
                    h = hIn
                }
            }
        }
    }
}