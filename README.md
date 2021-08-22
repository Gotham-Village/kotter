# Konsole

```kotlin
var wantsToLearn by KonsoleVar(false)
konsole {
    textLine("Would you like to learn Konsole? (Y/n)")
    textLine("> $input")
    if (wantsToLearn) {
        p { textLine("""\(^o^)/""") }
    }
}.runUntilTextEntered {
    onTextEntered { wantsToLearn = input.toLowercase().startsWith("y") }
}
```

---

Konsole aims to be a relatively thin, Kotlin-idiomatic API that provides useful functionality for writing delightful
command line applications. It strives to keep things simple, providing a solution a bit more interesting than making
raw `println` calls but way less featured than something like _Java Curses_.

Specifically, this library helps with:

* Modifying console text in place
* Colors and text decoration (e.g. underline)
* Handling user input
* Support for animations

## Gradle

(To be updated when this project is in a ready state)

## Usage

### Basic

The following is equivalent to `println("Hello, World")`. In this simple case, it's definitely overkill!

```kotlin
konsole {
    textLine("Hello, World")
}.runOnce()
```

`konsole { ... }` defines a `KonsoleBlock` which, on its own, is inert. It needs to be run at least once to output text
to the console. Above, we use the `runOnce` method above to trigger this. As you might expect, `runOnce` will execute
the block once. At this time, the program is blocked until the render is finished running.

`runOnce` consumes the block. If you attempt to call `runOnce` a second time, Konsole will throw an exception.

While the above simple case is a bit verbose, Konsole starts to show its strength when doing background work (or other
async tasks like waiting for user input) that would cause the block to update several times. We'll see many examples of
this in the following sections.

### Background work

For background work, we introduce the `runUntilFinished` method, which takes a callback that is automatically run on a
background thread for you (as a suspend function, so you can call other suspend methods from within it).

The Konsole block, after rendering once, then waits for the background work to finish, blocking the program from
continuing until it is done.

The `runUntilFinished` method includes some properties and functions in its scope, one of which is `rerender`, which you
can call to request another render pass.

```kotlin
var result: Int? = null
konsole {
    text("Calculating... ")
    if (result != null) { text("Done! Result = $result") }
    textLine()
}.runUntilFinished {
    result = doNetworkFetchAndExpensiveCalculation()
    rerender()
}
```

That works! But needing to remembering to call `rerender` yourself is potentially fragile and could be a source of bugs
in the future when trying to figure out why your console doesn't update.

For this purpose, Konsole provides the `KonsoleVar` class, a delegate class which, when updated, will automatically
request a rerender to the last block that referenced it.

To use a `KonsoleVar`, simply change a line like:

```kotlin
var result: Int? = null
```

to

```kotlin
var result by KonsoleVar<Int?>(null)
```

Let's apply that to the above example and remove the `rerender` call:

```kotlin
var result by KonsoleVar<Int?>(null)
konsole {
    text("Calculating... ")
    if (result != null) {
        textLine("Done! Result = $result")
    }
}.runUntilFinished {
    result = doNetworkFetchAndExpensiveCalculation()
}
```

You can use `runUntilFinished` for something like a progress bar:

```kotlin
const val BAR_LENGTH = 100
var percent by KonsoleVar(0)
konsole {
    text("[")
    val numComplete = percent * BAR_LENGTH
    for (val i in 0 until BAR_LENGTH) {
        text(if (i < numComplete) "*" else "-")
    }
    text("]")
}.runUntilFinished {
    // rerenderOnFinished not required in this case because we call it manually
    rerenderOnFinished = false
    while (percent < 100) {
        delay(100)
        percent += 1
    }
}
```

A common pattern is for the `runUntilFinished` block to wait for some sort of signal before finishing, e.g. in response
to some callback. You could always use a general threading trick for this, such as a `CountDownLatch` or a simple
`CompletableDeffered<Unit>` to stop the block from finishing until you're ready:

```kotlin
val fileDownloader by KonsoleVar(FileDownloader("..."))
fileDownloader.start()
konsole {
    /* ... */
}.runUntilFinished {
    val finished = CompletableDeffered<Unit>()
    fileDownloader.onFinished += { finished.complete(Unit) }
    finished.await()
}
```

but, for convenience, Konsole provides the `signal` and `waitForSignal` methods, which do this for you.

```kotlin
val fileDownloader by KonsoleVar(FileDownloader("..."))
konsole {
    /* ... */
}.runUntilFinished {
    fileDownloader.onFinished += { signal() }
    waitForSignal()
}
```

These methods are enough in most cases. Note that if you call `signal` before you reach `waitForSignal`, then
`waitForSignal` will just pass through without stopping.

Finally, there's a `runUntilSignal` method you can use, which acts just like `runUntilFinished` but with a
`waitForSignal` at the end, so you only need to call `signal` at some point to progress:

```kotlin
val fileDownloader by KonsoleVar(FileDownloader("..."))
konsole {
    /* ... */
}.runUntilSignal {
    fileDownloader.onFinished += { signal() }
}
```


### Cursor

By default, Konsole doesn't display a cursor indicator, but you can easily do it yourself by calling `blinkingCursor`:

```kotlin
konsole {
    text("Cursor is here >>> ")
    blinkingCursor()
    text(" <<<")
}.runUntilFinished { /* ... */ }
```

When referenced, the cursor will cause the block to auto-render itself occasionally in order to repaint its state.

### User input

Konsole consumes keypresses, so as the user types into the console, nothing will show up unless you intentionally print
it. You can easily do this using the `input` property, which contains the user's input typed so far (excluding control
characters):

```kotlin
konsole {
    // `input` is a property that contains the user's input typed so far in
    // this konsole block. If your block references it, the block is
    // automatically rerendered when it changes.
    text("Please enter your name: $input")
}.runUntilFinished { /* ... */ }
```

Note that the input property automatically adds a blinking cursor automatically for you. This also handles keys like
LEFT, HOME, and DEL, moving the cursor back and forth between the bounds of the input string.

You can intercept input as it is typed in using the `onInputChanged` event:

```kotlin
konsole {
    text("Please enter your name: $input")
}.runUntilFinished {
    onInputChanged {
        // Reject invalid characters!
        input = input.filter { it.isLetter() }
    }
    /* ... */
}
```

You can also use the `lastInput` property to return your input to the previous (presumably valid) state.

```kotlin
konsole {
    text("Please enter your name: $input")
}.runUntilFinished {
    onInputChanged {
        if (input.any { !it.isLetter() }) {
            input = lastInput
        }
    }
    /* ... */
}
```

You can also use `onInputEntered`. This will be triggered whenever the user presses the ENTER key.

```kotlin
lateinit var name: String
konsole {
    text("Please enter your name: $input")
}.runUntilSignal {
    onInputChanged { input = input.filter { it.isLetter() } }
    onInputEntered { name = input; signal() }
}
```

There's actually a shortcut for cases where you want to automatically signal the handler block when text is entered,
`runUntilTextEntered`, as this is pretty common. Using it, we can slightly simplify the above example:

```kotlin
lateinit var name: String
konsole {
    text("Please enter your name: $input")
}.runUntilTextEntered {
    onInputChanged { input = input.filter { it.isLetter() } }
    onInputEntered { name = input }
}
```

Putting everything together, let's tweak the example from the beginning of this README:

```kotlin
var wantsToLearn by KonsoleVar(false)
konsole {
    textLine("Would you like to learn Konsole? (Y/n)")
    text("> $input")
    grey { textLine("yes".substringAfter(input)) }
    if (wantsToLearn) {
        p { textLine("""\(^o^)/""") }
    }
}.runUntilTextEntered {
    onInputChanged {
        val isValid = ("yes".startsWith(input) || "no".startsWithInput(input))
        if (!isValid) { input = lastInput }
    }
    onInputEntered {
        if ("yes".startsWith(input)) {
            input = "yes" // Update the input to make it feel like we autocompleted their answer
            wantsToLearn = true
        }
    }
}
```

This will cause the following to be printed to the console:

```bash
Would you like to learn Konsole? (Y/n)
> |y|es
```

After the user presses "ye":

```bash
Would you like to learn Konsole? (Y/n)
> ye|s|
```

And after the user presses enter:

```bash
Would you like to learn Konsole? (Y/n)
> yes

\(^o^)/

(Next line of text would go here...)
```

A common situation in a console is responding to bad input.

```kotlin
val VALID_ANSWERS = setOf("yes", "no")
var errorMessage by KonsoleVar<String>(null)
konsole {
    text("Would you like to learn Konsole? (Y/n)")
    textLine("> $input")
    if (errorMessage != null) {
        textLine()
        red { textLine(errorMessage) }
    }
}.runUntilSignal {
    onInputEntered {
        if (!VALID_ANSWERS.any { it.startsWith(input) }) {
            errorMessage = "Please try again. Reason: \"$input\" was invalid"
            rerender()
        }
        else if ("yes".startsWith(input)) { signal() }
    }
}
```

### Text Effects

You can call color methods directly:

```kotlin
konsole {
    green(layer = BG)
    red() // defaults to FG layer
    text("Hello")
    clearColor() // defaults to FG layer
    text(" ")
    blue()
    text("World")
    clearColor()
    clearColor(layer = BG)
    // ^ You could also have used clearColors() instead of calling clearColor twice
}.runOnce()
```

or you can use scoped helper versions that handle clearing colors for you automatically at the end of their block:

```kotlin
konsole {
    green(layer = BG) {
        red {
            text("Hello")
        }
        text(" ")
        blue {
            text("World")
        }
    }
}.runOnce()
```

### State

To reduce the chance of introducing unexpected bugs later, state changes (like colors) will be localized to the current
`konsole` block only:

```kotlin
konsole {
    blue(BG)
    red()
    text("This text is red on blue")
}.runOnce()

konsole {
    text("This text is rendered using default colors")
}.runOnce()
```

Konsole blocks also introduce the `scopedState` method. What this does is creates a new scope within which any state
will be automatically discarded after it ends. This is what the scoped color methods are doing for you under the hood,
actually.

```kotlin
konsole {
    scopedState {
        green(layer = BG)
        scopedState {
            red()
            text("Hello")
        }
        text(" ")
        scopedState {
            blue()
            text("World")
        }
    }
}.runOnce()
```

While `scopedState` is quite verbose for the single color case, it can be useful if you want to change the foreground
and background colors at the same time:

```kotlin
konsole {
    scopedState {
        red()
        blue(BG)
        text("Hello world")
    }
}.runOnce()
```


### Animations

You can easily create custom animations, by implementing the `KonsoleAnimation` interface and then instantiating an
animation instance from it:

```kotlin
val SPINNER = object : KonsoleAnimation(Duration.ofMillis(250)) {
    override val frames = arrayOf("\\", "|", "/", "-")
}

var finished = false
val spinner = SPINNER.createInstance()
konsole {
    if (!finished) {
        animation(spinner)
    }
    else {
        text("✓")
    }
    text(" Searching for files...")
    if (finished) {
        text(" Done!")
    }
    testLine()
}.runUntilFinished {
    doExpensiveFileSearching()
    finished = true
}
```

If it's a one-use animation that you don't want to share as a template, you can create the instance directly of course:

```kotlin
val spinner = object : KonsoleAnimation(Duration.ofMillis(250)) {
    override val frames = arrayOf("\\", "|", "/", "-")
}.createInstance()
```

Here's a way to create the progress bar from earlier using animations:

```kotlin
val PROGRESS_BAR = object : KonsoleAnimation(Duration.MAX_VALUE) {
    override val frames = arrayOf(
        "[----------]",
        "[*---------]",
        "[**--------]",
        "[***-------]",
        "[****------]",
        "[*****-----]",
        "[******----]",
        "[*******---]",
        "[********--]",
        "[*********-]",
        "[**********]",
    )
}

val progressBar = PROGRESS_BAR.createInstance()
konsole {
    animation(progressBar)
}.runUntilFinished {
    while (progressBar.advance()) {
        delay(100)
    }
}
```

## Advanced

### Thread Affinity

The experience of using Konsole is essentially single-threaded. Anytime you make a call to run a Konsole block, no
matter which thread it is called from, a single thread ultimately handles the work of rendering the block. At the same
time, if you attempt to call `konsole` while another Konsole block is already running, an exception is thrown.

I made this decision so that:

* I don't have to worry about multiple Konsole blocks println'ing at the same time - who likes clobbered text?
* Konsole does a bunch of tricks moving the terminal cursor around and setting color, etc. states, which would fail
horribly if multiple Konsole blocks tried doing this at the same time.
* Konsole embraces the idea of a dynamic, active block trailed by a bunch of static history. If two dynamic blocks
wanted to be active at the same time, what would that even mean?

In practice, I expect this decision won't be an issue for most users. Command line apps are expected to have a main flow
anyway -- ask the user a question, do some work, then ask another question, etc. It is expected that a user won't ever
even need to call `konsole` from more than one thread. It is hoped that the `konsole { ... }.runUntilFinished { ... }`
pattern is powerful enough for most (all?) cases.

### Virtual Terminal

It's not guaranteed that every user's command line setup supports ANSI codes. For example, debugging this project with
IntelliJ as well as Gradle are two such environments where functionality is limited! According to many online reports,
Windows is also a big offender here.

Konsole will attempt to detect if your console does not support the features it uses, and if not, it will open up a
fake virtual terminal backed by Swing. This workaround gives us better cross-platform support.

To modify the logic to ALWAYS open the virtual terminal, you can write the following somewhere in your program before
you start using Konsole:

```kotlin
DefaultTerminalIoProvider = { VirtualTerminalIo() }
```

### Why Not Compose?

Konsole's API is inspired by Compose, which astute readers may have already noticed -- it has a core block which gets
rerun for you automatically as necessary without you having to worry about it, and special state variables which, when
modified, automatically "recompose" the current console block. Why not just use Compose directly?

In fact, this is exactly what [Jake Wharton's Mosaic](https://github.com/JakeWharton/mosaic) is doing. I tried using it
first but ultimately decided against it before deciding to write Konsole, for the following reasons:

* Compose is tightly tied to the current Kotlin compiler version, which means if you are targeting a particular
version of the Kotlin language, you can easily see the dreaded error message: `This version (x.y.z) of the Compose
Compiler requires Kotlin version a.b.c but you appear to be using Kotlin version d.e.f which is not known to be
compatible.`
  * Using Kotlin v1.3 or older for some reason? You're out of luck.
  * I suspect this issue with Compose will improve over time, but for the present, it still seems like a non-Compose
  approach could be useful to many.

* Compose is great for rendering a whole, interactive UI, but console printing is often two parts: the active part that
the user is interacting with, and the history, which is static. To support this with Compose, you'd need to manage the
history list yourself and keep appending to it, and it was while thinking about an API that addressed this limitation
that I envisioned Konsole.

### Tested Platforms

* [x] Linux
* [ ] Mac
* [ ] Windows