package com.varabyte.konsole.foundation.text

import com.varabyte.konsole.runtime.KonsoleBlock
import com.varabyte.konsole.runtime.RenderScope
import com.varabyte.konsole.runtime.internal.ansi.Ansi
import com.varabyte.konsole.runtime.internal.ansi.commands.*

enum class ColorLayer {
    FG,
    BG
}

enum class Color {
    BLACK,
    RED,
    GREEN,
    YELLOW,
    BLUE,
    MAGENTA,
    CYAN,
    WHITE,
    BRIGHT_BLACK,
    BRIGHT_RED,
    BRIGHT_GREEN,
    BRIGHT_YELLOW,
    BRIGHT_BLUE,
    BRIGHT_MAGENTA,
    BRIGHT_CYAN,
    BRIGHT_WHITE;
}

private fun toBlackCommand(colorLayer: ColorLayer, isBright: Boolean) = when(colorLayer) {
    ColorLayer.FG -> if (isBright) FG_BLACK_BRIGHT_COMMAND else FG_BLACK_COMMAND
    ColorLayer.BG -> if (isBright) BG_BLACK_BRIGHT_COMMAND else BG_BLACK_COMMAND
}

private fun toRedCommand(colorLayer: ColorLayer, isBright: Boolean) = when(colorLayer) {
    ColorLayer.FG -> if (isBright) FG_RED_BRIGHT_COMMAND else FG_RED_COMMAND
    ColorLayer.BG -> if (isBright) BG_RED_BRIGHT_COMMAND else BG_RED_COMMAND
}

private fun toGreenCommand(colorLayer: ColorLayer, isBright: Boolean) = when(colorLayer) {
    ColorLayer.FG -> if (isBright) FG_GREEN_BRIGHT_COMMAND else FG_GREEN_COMMAND
    ColorLayer.BG -> if (isBright) BG_GREEN_BRIGHT_COMMAND else BG_GREEN_COMMAND
}

private fun toYellowCommand(colorLayer: ColorLayer, isBright: Boolean) = when(colorLayer) {
    ColorLayer.FG -> if (isBright) FG_YELLOW_BRIGHT_COMMAND else FG_YELLOW_COMMAND
    ColorLayer.BG -> if (isBright) BG_YELLOW_BRIGHT_COMMAND else BG_YELLOW_COMMAND
}

private fun toBlueCommand(colorLayer: ColorLayer, isBright: Boolean) = when(colorLayer) {
    ColorLayer.FG -> if (isBright) FG_BLUE_BRIGHT_COMMAND else FG_BLUE_COMMAND
    ColorLayer.BG -> if (isBright) BG_BLUE_BRIGHT_COMMAND else BG_BLUE_COMMAND
}

private fun toMagentaCommand(colorLayer: ColorLayer, isBright: Boolean) = when(colorLayer) {
    ColorLayer.FG -> if (isBright) FG_MAGENTA_BRIGHT_COMMAND else FG_MAGENTA_COMMAND
    ColorLayer.BG -> if (isBright) BG_MAGENTA_BRIGHT_COMMAND else BG_MAGENTA_COMMAND
}

private fun toCyanCommand(colorLayer: ColorLayer, isBright: Boolean) = when(colorLayer) {
    ColorLayer.FG -> if (isBright) FG_CYAN_BRIGHT_COMMAND else FG_CYAN_COMMAND
    ColorLayer.BG -> if (isBright) BG_CYAN_BRIGHT_COMMAND else BG_CYAN_COMMAND
}

private fun toWhiteCommand(colorLayer: ColorLayer, isBright: Boolean) = when(colorLayer) {
    ColorLayer.FG -> if (isBright) FG_WHITE_BRIGHT_COMMAND else FG_WHITE_COMMAND
    ColorLayer.BG -> if (isBright) BG_WHITE_BRIGHT_COMMAND else BG_WHITE_COMMAND
}

fun RenderScope.black(colorLayer: ColorLayer = ColorLayer.FG, isBright: Boolean = false) {
    applyCommand(toBlackCommand(colorLayer, isBright))
}

fun RenderScope.red(colorLayer: ColorLayer = ColorLayer.FG, isBright: Boolean = false) {
    applyCommand(toRedCommand(colorLayer, isBright))
}

fun RenderScope.green(colorLayer: ColorLayer = ColorLayer.FG, isBright: Boolean = false) {
    applyCommand(toGreenCommand(colorLayer, isBright))
}

fun RenderScope.yellow(colorLayer: ColorLayer = ColorLayer.FG, isBright: Boolean = false) {
    applyCommand(toYellowCommand(colorLayer, isBright))
}

fun RenderScope.blue(colorLayer: ColorLayer = ColorLayer.FG, isBright: Boolean = false) {
    applyCommand(toBlueCommand(colorLayer, isBright))
}

fun RenderScope.magenta(colorLayer: ColorLayer = ColorLayer.FG, isBright: Boolean = false) {
    applyCommand(toMagentaCommand(colorLayer, isBright))
}

fun RenderScope.cyan(colorLayer: ColorLayer = ColorLayer.FG, isBright: Boolean = false) {
    applyCommand(toCyanCommand(colorLayer, isBright))
}

fun RenderScope.white(colorLayer: ColorLayer = ColorLayer.FG, isBright: Boolean = false) {
    applyCommand(toWhiteCommand(colorLayer, isBright))
}

fun RenderScope.color(color: Color, colorLayer: ColorLayer = ColorLayer.FG) {
    applyCommand(when (color) {
        Color.BLACK -> toBlackCommand(colorLayer, false)
        Color.RED -> toRedCommand(colorLayer, false)
        Color.GREEN -> toGreenCommand(colorLayer, false)
        Color.YELLOW -> toYellowCommand(colorLayer, false)
        Color.BLUE -> toBlueCommand(colorLayer, false)
        Color.MAGENTA -> toMagentaCommand(colorLayer, false)
        Color.CYAN -> toCyanCommand(colorLayer, false)
        Color.WHITE -> toWhiteCommand(colorLayer, false)
        Color.BRIGHT_BLACK -> toBlackCommand(colorLayer, true)
        Color.BRIGHT_RED -> toRedCommand(colorLayer, true)
        Color.BRIGHT_GREEN -> toGreenCommand(colorLayer, true)
        Color.BRIGHT_YELLOW -> toYellowCommand(colorLayer, true)
        Color.BRIGHT_BLUE -> toBlueCommand(colorLayer, true)
        Color.BRIGHT_MAGENTA -> toMagentaCommand(colorLayer, true)
        Color.BRIGHT_CYAN -> toCyanCommand(colorLayer, true)
        Color.BRIGHT_WHITE -> toWhiteCommand(colorLayer, true)
    })
}

fun RenderScope.clearColor(colorLayer: ColorLayer = ColorLayer.FG) {
    applyCommand(when(colorLayer) {
        ColorLayer.FG -> FG_CLEAR_COMMAND
        ColorLayer.BG -> BG_CLEAR_COMMAND
    })
}

fun RenderScope.clearColors() {
    applyCommand(FG_CLEAR_COMMAND)
    applyCommand(BG_CLEAR_COMMAND)
}

fun RenderScope.invert() {
    applyCommand(INVERT_COMMAND)
}

fun RenderScope.clearInvert() {
    applyCommand(CLEAR_INVERT_COMMAND)
}

fun RenderScope.black(
    colorLayer: ColorLayer = ColorLayer.FG,
    isBright: Boolean = false,
    scopedBlock: KonsoleBlock.() -> Unit
) {
    scopedState {
        black(colorLayer, isBright)
        scopedBlock()
    }
}

fun RenderScope.red(
    colorLayer: ColorLayer = ColorLayer.FG,
    isBright: Boolean = false,
    scopedBlock: KonsoleBlock.() -> Unit
) {
    scopedState {
        red(colorLayer, isBright)
        scopedBlock()
    }
}

fun RenderScope.green(
    colorLayer: ColorLayer = ColorLayer.FG,
    isBright: Boolean = false,
    scopedBlock: KonsoleBlock.() -> Unit
) {
    scopedState {
        green(colorLayer, isBright)
        scopedBlock()
    }
}

fun RenderScope.yellow(
    colorLayer: ColorLayer = ColorLayer.FG,
    isBright: Boolean = false,
    scopedBlock: KonsoleBlock.() -> Unit
) {
    scopedState {
        yellow(colorLayer, isBright)
        scopedBlock()
    }
}

fun RenderScope.blue(
    colorLayer: ColorLayer = ColorLayer.FG,
    isBright: Boolean = false,
    scopedBlock: KonsoleBlock.() -> Unit
) {
    scopedState {
        blue(colorLayer, isBright)
        scopedBlock()
    }
}

fun RenderScope.cyan(
    colorLayer: ColorLayer = ColorLayer.FG,
    isBright: Boolean = false,
    scopedBlock: KonsoleBlock.() -> Unit
) {
    scopedState {
        cyan(colorLayer, isBright)
        scopedBlock()
    }
}

fun RenderScope.magenta(
    colorLayer: ColorLayer = ColorLayer.FG,
    isBright: Boolean = false,
    scopedBlock: KonsoleBlock.() -> Unit
) {
    scopedState {
        magenta(colorLayer, isBright)
        scopedBlock()
    }
}

fun RenderScope.white(
    colorLayer: ColorLayer = ColorLayer.FG,
    isBright: Boolean = false,
    scopedBlock: KonsoleBlock.() -> Unit
) {
    scopedState {
        white(colorLayer, isBright)
        scopedBlock()
    }
}

fun RenderScope.invert(scopedBlock: KonsoleBlock.() -> Unit) {
    scopedState {
        invert()
        scopedBlock()
    }
}