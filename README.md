![version: 0.9.3](https://img.shields.io/badge/kotter-v0.9.3-blue)
<a href="https://discord.gg/5NZ2GKV5Cs">
  <img alt="Varabyte Discord" src="https://img.shields.io/discord/886036660767305799.svg?label=&logo=discord&logoColor=ffffff&color=7389D8&labelColor=6A7EC2" />
</a>
[![Follow @bitspittle](https://img.shields.io/twitter/follow/bitspittle.svg?style=social)](https://twitter.com/intent/follow?screen_name=bitspittle)

# Kotter 🦦

```kotlin
session {
  var wantsToLearn by liveVarOf(false)
  section {
    text("Would you like to learn "); cyan { text("Kotter") }; textLine("? (Y/n)")
    text("> "); input(Completions("yes", "no"))

    if (wantsToLearn) {
      yellow(isBright = true) { p { textLine("""\(^o^)/""") } }
    }
  }.runUntilInputEntered {
    onInputEntered { wantsToLearn = "yes".startsWith(input.lowercase()) }
  }
}
```

![Code sample in action](https://github.com/varabyte/media/raw/main/kotter/screencasts/kotter-input.gif)

*See also: [the game of life](examples/life), [snake](examples/snake), and [doom fire](examples/doomfire) implemented in Kotter!*

---

Kotter (a **KOT**lin **TER**minal library) aims to be a relatively thin, declarative, Kotlin-idiomatic API that provides
useful functionality for writing delightful console applications. It strives to keep things simple, providing a solution
a bit more opinionated than making raw `println` calls but way less featured than something like _Java Curses_.

Specifically, this library helps with:

* Setting colors and text decorations (e.g. underline, bold)
* Handling user input
* Creating timers and animations
* Seamlessly repainting terminal text when values change

## Gradle

### Dependency

The artifact for this project is hosted in our own artifact repository (*), so to include Kotter in your project, modify
your Gradle build file as follows:

```groovy
repositories {
  /* ... */
  maven { url 'https://us-central1-maven.pkg.dev/varabyte-repos/public' }
}

dependencies {
  /* ... */
  implementation 'com.varabyte.kotter:kotter:0.9.3'
}
```

(* To be hosted in `mavenCentral` eventually)

### Running examples

If you've cloned this repository, examples are located under the [examples](examples) folder. To try one of them, you
can navigate into it on the command line and run it via Gradle.

```bash
$ cd examples/life
$ ../../gradlew run
```

However, because Gradle itself has taken over the terminal to do its own fancy command line magic, the example will
actually open up and run inside a virtual terminal.

If you want to run the program directly inside your system terminal, which is hopefully the way most users will see your
application, you should use the `installDist` task to accomplish this:

```bash
$ cd examples/life
$ ../../gradlew installDist
$ cd build/install/life/bin
$ ./life
```

***Note:** If your terminal does not support features needed by Kotter, then this still may end up running inside a
virtual terminal.*

## Usage

### Basic

The following is equivalent to `println("Hello, World")`. In this simple case, it's definitely overkill!

```kotlin
session {
  section { textLine("Hello, World") }.run()
}
```

`section { ... }` defines a `Section` which, on its own, is inert. It needs to be run to output text to the
console. Above, we use the `run` method to trigger this. The method blocks until the render (i.e. text printing to the
console) is finished (which, for console text, probably won't be very long).

`session { ... }` sets the outer scope for your whole program (e.g. it specifies the lifetime of some data). While we're
just calling it with default arguments here, you can also pass in parameters that apply to the entire application.
A Kotter `session` can contain one or more `section`s. 

While the above simple case is a bit verbose for what it's doing, Kotter starts to show its strength when doing
background work (or other async tasks like waiting for user input) during which time the block may update several times.
We'll see many examples throughout this document later.

### Text Effects

You can call color methods directly, which remain in effect until the next color method is called:

```kotlin
section {
  green(layer = BG)
  red() // defaults to FG layer if no layer specified
  textLine("Red on green")
  blue()
  textLine("Blue on green")
}.run()
```

or, if you only want the color effect to live for a limited time, you can use scoped helper versions that handle
clearing colors for you automatically at the end of their block:

```kotlin
section {
  green(layer = BG) {
    red {
      textLine("Red on green")
    }
    textLine("Default on green")
    blue {
      textLine("Blue on green")
    }
  }
}.run()
```

If the user's terminal supports truecolor mode, you can specify rgb (or hsv) values directly:

```kotlin
section {
  rgb(0xFFFF00) { textLine("Yellow!") }
  hsv(35, 1.0, 1.0) { textLine("Orange!") }
}.run()
```

***Note:** If truecolor is not supported, terminals may attempt to emulate it by falling back to a nearby color, which
may look decent! However, to be safe, you may want to avoid subtle gradient tricks, as they may come out clumped for
some users.*

Various text effects (like bold) are also available:

```kotlin
section {
  bold {
    textLine("Title")
  }

  p {
    textLine("This is the first paragraph of text")
  }

  p {
    text("This paragraph has an ")
    underline { text("underlined") }
    textLine(" word in it")
  }
}.run()
```

***Note:** Italics support is not currently exposed, as it is not a standard feature and is inconsistently supported.*

### State and scopedState

To reduce the chance of introducing unexpected bugs later, state changes (like colors) will be localized to the current
`section` block only:

```kotlin
section {
  blue(BG)
  red()
  text("This text is red on blue")
}.run()

section {
  text("This text is rendered using default colors")
}.run()
```

Within a section, you can also use the `scopedState` method. This creates a new scope within which any state will be
automatically discarded after it ends.

```kotlin
section {
  scopedState {
    red()
    blue(BG)
    underline()
    text("Underlined red on blue")
  }
  text("Text without color or decorations")
}.run()
```

***Note:** This is what the scoped text effect methods (like `red { ... }`) are doing for you under the hood, actually.*

### Dynamic sections

The `section` block is designed to be run one _or more_ times. That is, you can write logic inside it which may not get
executed on the first run but will be on a followup run.

Here, we pass in a callback to the `run` method which updates a value referenced by the `section` block (the `result`
integer). This example will run the section twice - once when `run` is first called and again when it calls
`rerender`:

```kotlin
var result: Int? = null
section {
  text("Calculating... ")
  if (result != null) {
    text("Done! Result = $result")
  }
}.run {
  result = doNetworkFetchAndExpensiveCalculation()
  rerender()
}
```

The `run` callback automatically runs on a background thread for you (as a suspend function, so you can call other
suspend methods from within it).

Unlike using `run` without a callback (i.e. simply `run()`), here your program will be blocked until the callback has
finished (or, if it has triggered a rerender, until the last rerender finishes after your callback is done).

#### LiveVar

As you can see above, the `run` callback uses a `rerender` method, which you can call to request another render pass.

However, remembering to call `rerender` yourself is potentially fragile and could be a source of bugs in the future when
trying to figure out why your console isn't updating.

For this purpose, Kotter provides the `LiveVar` class, which, when modified, will automatically request a rerender.
An example will demonstrate this in action shortly.

To create a `LiveVar`, simply change a normal variable declaration line like:

```kotlin
session {
  var result: Int? = null
  /* ... */
}
```

to:

```kotlin
session {
  var result by liveVarOf<Int?>(null)
  /* ... */
}
```

***Note:** The `liveVarOf` method is actually scoped to the `session` block. For many remaining examples, we'll elide
the `session` boilerplate, but that doesn't mean you can omit it in your own program!*

Let's apply `liveVarOf` to our earlier example in order to remove the `rerender` call:

```kotlin
var result by liveVarOf<Int?>(null)
section {
  /* ... no changes ... */
}.run {
  result = doNetworkFetchAndExpensiveCalculation()
}
```

And done! Fewer lines and less error pone.

Here's another example, showing how you can use `run` for something like a progress bar:

```kotlin
// Prints something like: [****------]
val BAR_LENGTH = 10
var numFilledSegments by liveVarOf(0)
section {
  text("[")
  for (i in 0 until BAR_LENGTH) {
    text(if (i < numFilledSegments) "*" else "-")
  }
  text("]")
}.run {
  var percent = 0
  while (percent < 100) {
    delay(Random.nextLong(10, 100))
    percent += Random.nextInt(1,5)
    numFilledSegments = ((percent / 100f) * BAR_LENGTH).roundToInt()
  }
}
```

#### LiveList

Similar to `LiveVar`, a `LiveList` is a reactive primitive which, when modified by having elements added to or
removed from it, causes a rerender to happen automatically. You don't need to use the `by` keyword with `LiveList`.
Instead, within a `session`, use the `liveListOf` method:

```kotlin
val fileWalker = FileWalker(".") // This class doesn't exist but just pretend for this example...
val fileMatches = liveListOf<String>()
section {
  textLine("Matches found so far: ")
  if (fileMatches.isNotEmpty()) {
    for (match in fileMatches) {
      textLine(" - $match")
    }
  }
  else {
    textLine("No matches so far...")
  }
}.run {
  fileWalker.findFiles("*.txt") { file ->
    fileMatches += file.name
  }
  /* ... */
}
```

The `LiveList` class is thread safe, but you can still run into trouble if you access multiple values on the list one
after the other, as a lock is released between each check. It's always possible that modifying the first property will
kick off a new render which will start before the additional values are set, in other words.

To handle this, you can use the `LiveList#withWriteLock` method:

```kotlin
val fileWalker = FileWalker(".")
val last10Matches = liveListOf<String>()
section {
  ...
}.run {
  fileWalker.findFiles("*.txt") { file ->
    last10Matches.withWriteLock {
      add(file.name)
      if (size > 10) { removeAt(0) }
    }
  }
  /* ... */

}
```

The general rule of thumb is: use `withWriteLock` if you want to access or modify more than one property from the list
at the same time within your `run` block.

Note that you don't have to worry about locking within a `section { ... }` block. Data access is already locked for you
in that context.

#### Signals and waiting

A common pattern is for the `run` block to wait for some sort of signal before finishing, e.g. in response to some
event. You could always use a general threading trick for this, such as a `CountDownLatch` or a
`CompletableDeffered<Unit>` to stop the block from finishing until you're ready:

```kotlin
val fileDownloader = FileDownloader("...")
fileDownloader.start()
section {
  /* ... */
}.run {
  val finished = CompletableDeffered<Unit>()
  fileDownloader.onFinished += { finished.complete(Unit) }
  finished.await()
}
```

but, for convenience, Kotter provides the `signal` and `waitForSignal` methods, which do this for you.

```kotlin
val fileDownloader = FileDownloader("...")
section {
  /* ... */
}.run {
  fileDownloader.onFinished += { signal() }
  waitForSignal()
}
```

These methods are enough in most cases. Note that if you call `signal` before you reach `waitForSignal`, then
`waitForSignal` will just pass through without stopping.

There's also a convenience `runUntilSignal` method you can use, within which you don't need to call `waitForSignal` yourself, since
this case is so common:

```kotlin
val fileDownloader = FileDownloader("...")
section {
  /* ... */
}.runUntilSignal {
  fileDownloader.onFinished += { signal() }
}
```

### User input

#### Typed input

Kotter consumes keypresses, so as the user types into the console, nothing will show up unless you intentionally print
it. You can easily do this using the `input` method, which handles listening to kepresses and adding text into your
section at that location:

```kotlin
section {
  // `input` is a method that appends the user's input typed so far in this
  // Once your section references it, the block is automatically rerendered when its value changes.
  text("Please enter your name: "); input()
}.run { /* ... */ }
```

Note that the input method automatically adds a cursor for you. This also handles keys like LEFT/RIGHT and HOME/END,
moving the cursor back and forth between the bounds of the input string.

You can intercept input as it is typed using the `onInputChanged` event:

```kotlin
section {
  text("Please enter your name: "); input()
}.run {
  onInputChanged {
    input = input.toUpperCase()
  }
  /* ... */
}
```

You can also use the `rejectInput` method to return your input to the previous (presumably valid) state.

```kotlin
section {
  text("Please enter your name: "); input()
}.run {
  onInputChanged {
    if (input.any { !it.isLetter() }) { rejectInput() }
    // Would also work: input = input.filter { it.isLetter() }
  }
  /* ... */
}
```

You can also use `onInputEntered`. This will be triggered whenever the user presses the ENTER key.

```kotlin
var name = ""
section {
  text("Please enter your name: "); input()
}.runUntilSignal {
  onInputChanged { input = input.filter { it.isLetter() } }
  onInputEntered { name = input; signal() }
}
```

There's actually a shortcut for cases like the above, since they're pretty common: `runUntilInputEntered`.
Using it, we can slightly simplify the above example, typing fewer characters for identical behavior:

```kotlin
var name = ""
section {
  text("Please enter your name: "); input()
}.runUntilInputEntered {
  onInputChanged { input = input.filter { it.isLetter() } }
  onInputEntered { name = input }
}
```

#### Keypresses

If you're interested in specific keypresses and not simply input that's been typed in, you can register a listener to
the `onKeyPressed` event:

```kotlin
section {
  textLine("Press Q to quit")
  /* ... */
}.run {
  var quit = false
  onKeyPressed {
    when(key) {
      Keys.Q -> quit = true
    }
  }

  while (!quit) {
    delay(16)
    /* ... */
  }
}
```

For convenience, there's also a `runUntilKeyPressed` method you can use to help with patterns like the above.

```kotlin
section {
  textLine("Press Q to quit")
  /* ... */
}.runUntilKeyPressed(Keys.Q) {
  while (true) {
    delay(16)
    /* ... */
  }
}
```

### Timers

Kotter can manage a set of timers for you. Use the `addTimer` method in your `run` block to add some:

```kotlin
section {
  /* ... */
}.runUntilSignal {
  addTimer(Duration.ofMillis(500)) {
    println("500ms passed!")
    signal()
  }
}
```

You can create a repeating timer by passing in `repeat = true` to the method. And if you want to stop it from repeating
at some point, set `repeat = false` inside the timer block when it is triggered:

```kotlin
val BLINK_TOTAL_LEN = Duration.ofSeconds(5)
val BLINK_LEN = Duration.ofMillis(250)
var blinkOn by liveVarOf(false)
section {
  scopedState {
    if (blinkOn) invert()
    textLine("This line will blink for ${BLINK_TOTAL_LEN.toSeconds()} seconds")
  }

}.run {
  var blinkCount = BLINK_TOTAL_LEN.toMillis() / BLINK_LEN.toMillis()
  addTimer(BLINK_LEN, repeat = true) {
    blinkOn = !blinkOn
    blinkCount--
    if (blinkCount == 0L) {
      repeat = false
    }
  }
  /* ... */
}
```

![Code sample in action](https://github.com/varabyte/media/raw/main/kotter/screencasts/kotter-blink.gif)

It's possible your block will exit while things are in a bad state due to running timers, so you can use the
`onFinishing` callback to handle this:

```kotlin
var blinkOn by liveVarOf(false)
section {
  /* ... */
}.onFinishing {
  blinkOn = false
}.run {
  addTimer(Duration.ofMillis(250), repeat = true) { blinkOn = !blinkOn }
  /* ... */
}
```

***Note:** Unlike other callbacks, `onFinishing` is registered directly against the underlying `section`, because it is
actually triggered AFTER the run pass is finished but before the block is torn down.*

`onFinishing` will only run after all timers are stopped, so you don't have to worry about setting a value that an
errant timer will clobber later.

### Animations

You can easily create custom animations, by calling `animOf`:

```kotlin
var finished = false
val spinnerAnim = animOf(listOf("\\", "|", "/", "-"), Duration.ofMillis(125))
val thinkingAnim = animOf(listOf("", ".", "..", "..."), Duration.ofMillis(500))
section {
  if (!finished) { text(spinnerAnim) } else { text("✓") }
  text(" Searching for files")
  if (!finished) { text(thinkingAnim) } else { text("... Done!") }
}.run {
  doExpensiveFileSearching()
  finished = true
}
```

![Code sample in action](https://github.com/varabyte/media/raw/main/kotter/screencasts/kotter-spinner.gif)

When you reference an animation in a render for the first time, it kickstarts a timer automatically for you. In other
words, all you have to do is treat your animation instance as if it were a string, and Kotter takes care of the rest!

#### Animation templates

If you have an animation that you want to share in a bunch of places, you can create a template for it and instantiate
instances from the template. `Anim.Template` takes exactly the same arguments as the `animOf` method.

This may be useful if you have a single animation that you want to run in many places at the same time but all slightly
off from one another. For example, if you were processing 10 threads at a time, you may want the spinner for each thread
to start spinning whenever its thread activates:

```kotlin
val SPINNER_TEMPATE = Anim.Template(listOf("\\", "|", "/", "-"), Duration.ofMillis(250))

val spinners = (1..10).map { animOf(SPINNER_TEMPLATE) }
/* ... */
```

## Advanced

### Thread Affinity

Setting aside the fact that the `run` block runs in a background thread, sections themselves are rendered sequentially
on a single thread. Anytime you make a call to run a section, no matter which thread it is called from, a single thread
ultimately handles it. At the same time, if you attempt to run one section while another is already running, an
exception is thrown.

I made this decision so that:

* I don't have to worry about multiple sections `println`ing at the same time - who likes clobbered text?
* Kotter handles repainting by moving the terminal cursor around, which would fail horribly if multiple sections tried
doing this at the same time.
* Kotter embraces the idea of a dynamic, active section preceded by a bunch of static history. If two dynamic blocks
wanted to be active at the same time, what would that even mean?

In practice, I expect this decision won't be an issue for most users. Command line apps are expected to have a main flow
anyway -- ask the user a question, do some work, then ask another question, etc. It is expected that a user won't ever
even need to call `section` from more than one thread. It is hoped that the
`section { ... main thread ... }.run { ... background thread ... }` pattern is powerful enough for most (all?) cases.

### Virtual Terminal

It's not guaranteed that every user's command line setup supports ANSI codes. For example, debugging this project with
IntelliJ as well as running within Gradle are two such environments where functionality isn't available! According to
many online reports, Windows is also a big offender here.

Kotter will attempt to detect if your console does not support the features it uses, and if not, it will open up a
virtual terminal. This fallback gives your application better cross-platform support.

To modify the logic to ALWAYS open the virtual terminal, you can set the `terminal` parameter in `session` like
this:

```kotlin
session(terminal = VirtualTerminal.create()) {
  section { /* ... */ }
  /* ... */
}
```

or you can chain multiple factory methods together using the `runUntilSuccess` method, which will try to start each
terminal type in turn. If you want to mimic the current behavior where you try to run a system terminal first and fall
back to a virtual terminal later, but perhaps you want to customize the virtual terminal with different parameters,
you can write code like so:

```kotlin
session(
  terminal = listOf(
    { SystemTerminal() },
    { VirtualTerminal.create(title = "My App", terminalSize = Dimension(30, 30)) },
  ).runUntilSuccess()
) {
  /* ... */
}
```

### Why Not Compose / Mosaic?

Kotter's API is inspired by Compose, which astute readers may have already noticed -- it has a core block which gets
rerun for you automatically as necessary without you having to worry about it, and special state variables which, when
modified, automatically "recompose" the current console block. Why not just use Compose directly?

In fact, this is exactly what [Jake Wharton's Mosaic](https://github.com/JakeWharton/mosaic) is doing. Actually, I tried
using it first but ultimately decided against it before deciding to write Kotter, for the following reasons:

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
that I envisioned Kotter.
  * For a concrete example, see the [compiler demo](examples/compiler).

* Compose encourages using a set of powerful layout primitives, namely `Box`, `Column`, and `Row`, with margins and
  shapes and layers. Command line apps don't really need this level of power, however.

* Compose has a lot of strengths built around, well, composing methods! And to enable this, it makes heavy use of
  features like `remember` blocks, which you can call inside a composable method and it gets treated in a special way.
  But for a simple CLI library, being able to focus on render blocks that don't nest too deeply and not worrying as much
  about performance allowed a more pared down API to emerge.

* Compose does a lot of nice tricks due to the fact it is ultimately a compiler plugin, but it is nice to see what the
  API would kind of look like with no magic at all (although, admittedly, with some features sacrificed).

* Mosaic doesn't support input well yet (at the time of writing this README, maybe this has changed in the future).
  For example, compare [Mosaic](https://github.com/JakeWharton/mosaic/blob/fd213711ce2b828a6436a61d6d345692222bdb95/samples/robot/src/main/kotlin/example/robot.kt#L45)
  to [Kotter](https://github.com/varabyte/kotter/blob/main/examples/mosaic/robot/src/main/kotlin/main.kt#L27).

#### Mosaic comparison

```kotlin
// Mosaic
runMosaic {
  var count by remember { mutableStateOf(0) }
  Text("The count is: $count")

  LaunchedEffect(null) {
    for (i in 1..20) {
      delay(250)
      count++
    }
  }
}

// Kotter
session {
  var count by liveVarOf(0)
  section {
    textLine("The count is: $count")
  }.run {
    for (i in 1..20) {
      delay(250)
      count++
    }
  }
}
```

Comparisons with Mosaic are included in the [examples/mosaic](examples/mosaic) folder.