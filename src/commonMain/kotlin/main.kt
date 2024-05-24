import korlibs.image.color.*
import korlibs.image.font.*
import korlibs.image.format.*
import korlibs.io.async.*
import korlibs.io.file.*
import korlibs.io.file.std.*
import korlibs.io.lang.*
import korlibs.io.stream.*
import korlibs.korge.*
import korlibs.korge.scene.*
import korlibs.korge.ui.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import korlibs.render.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

suspend fun main() = Korge(windowSize = Size(900, 800), backgroundColor = Colors["#2b2b2b"]) {
	val sceneContainer = sceneContainer()
	sceneContainer.changeTo { MyScene() }
}

data class FontRef(
    val thin: TtfFont,
    val regular: TtfFont,
)

@Serializable
data class HP(
    val infinite: Boolean,
    val atk: Double?,
    val hp: Double?,
) {
    companion object {
        fun infinite(): HP =
            HP(true, null, null)

        fun none(): HP =
            HP(false, null, null)

        fun part(a: Double? = null, b: Double? = null): HP =
            HP(false, a, b)
    }
}

fun Double.goodToString(): String {
    val v = toString()
    if (v.endsWith(".0"))
        return v.substring(0, v.length - 2)
    return v
}

class MyScene : Scene() {
	override suspend fun SContainer.sceneMain() {
        addChild(CardEditor(gameWindow, true).build())
	}
}
