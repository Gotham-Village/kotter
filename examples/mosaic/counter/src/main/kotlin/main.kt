import com.varabyte.konsole.core.konsoleApp
import com.varabyte.konsole.core.text.textLine
import kotlinx.coroutines.delay

// https://github.com/JakeWharton/mosaic/tree/trunk/samples/counter
fun main() = konsoleApp {
    var count by KonsoleVar(0)
    konsole {
        textLine("The count is: $count")
    }.run {
        for (i in 1..20) {
            delay(250)
            count++
        }
    }
}