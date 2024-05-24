import korlibs.event.*
import korlibs.korge.ui.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import kotlin.math.*

fun singlelineText(w: Double, h: Double? = null, placeholder: String, init: UITextInput.() -> Unit): UITextInput {
    return UITextInput(placeholder).apply {
        init(this)

        width = w
        skin = ViewRenderer {  }
        padding = Margin.ZERO

        controller.onEscPressed.add {
            it.blur()
        }
        controller.onTextUpdated.add {
            val bounds = it.textView.textBounds
            val newW = w / bounds.width
            val newH = (h ?: height) / bounds.height
            val new = min(min(newW, newH), 1.0)
            scale(new)
            width = max(bounds.width, w)
            height = max(bounds.height, h ?: height)
        }
    }
}

fun multilineText(w: Double, h: Double, placeholder: String, init: UITextInput.() -> Unit): UITextInput {
    return singlelineText(w, h, placeholder) {
        init(this)

        height = h

        controller.eventHandler.onEvent(KeyEvent.Type.DOWN) { key ->
            when (key.key) {
                Key.UP -> {
                    val nl = controller.text.substring(0, controller.cursorIndex).indexOfLast { it == '\n' }
                    if (nl != -1) {
                        controller.moveToIndex(false, nl)
                    }
                }
                Key.DOWN -> {
                    val nl = controller.text.substring(controller.cursorIndex).indexOfFirst { it == '\n' }
                    if (nl != -1) {
                        controller.moveToIndex(false, nl + controller.cursorIndex + 1)
                    }
                }
                else -> {}
            }
        }
        controller.onReturnPressed.add {
            it.insertText("\n")
            it.updateCaretPosition()
            invalidateRender()
        }
    }
}
