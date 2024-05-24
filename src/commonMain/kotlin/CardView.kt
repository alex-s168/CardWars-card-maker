import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.image.font.*
import korlibs.image.format.*
import korlibs.io.async.*
import korlibs.io.file.std.*
import korlibs.korge.input.*
import korlibs.korge.ui.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import korlibs.render.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlin.math.*

class CardView(
    val rootWindow: GameWindow,
    val moreSugar: FontRef,
    template: Bitmap,
    imageIn: Bitmap,
): Container() {
    var type = "Creature"
    val keywords = mutableSetOf<String>()
    val personalities = mutableListOf<Char>()
    var unpredictability = 0

    companion object {
        val TYPES = listOf(
            "Creature",
            "Structure",
            "Artifact",
        )

        val PERSONALITIES = listOf(
            "Simple",
            "Aggressive",
            "Monster",
            "Intelligent",
            "Friendly",
        )
    }

    var hp: HP = HP.infinite()
        set(value) {
            field = value
            val old = hpView
            hpView = if (value.infinite) {
                infHpView()
            } else {
                partHpView(
                    value.atk?.goodToString() ?: "-",
                    value.hp?.goodToString() ?: "-",
                )
            }
            replaceChild(old, hpView)
        }

    var image: Bitmap = imageIn
        set(value) {
            val new = mkImgView(value)
            replaceChild(imgView, new)
            imgView = new
            field = value
        }

    private var imgView: View = mkImgView(imageIn)
    private var hpView: View = infHpView()

    private fun mkImgView(bm: Bitmap): View =
        Container().apply {
            val wantWidth = 227.0
            val wantHeight = 270.0

            addChild(SolidRect(wantWidth, wantHeight, Colors.WHITE).apply {
                position(15, 15)
            })

            addChild(Image(bm).apply {
                position(15, 15)
                val wscale = wantWidth / width
                val hscale = wantHeight / height
                scale(min(wscale, hscale))
            })

            onDropFile { drop ->
                runBlockingNoSuspensions {
                    drop.files?.firstOrNull()?.let { file ->
                        runCatching {
                            val bmp = file.readBitmap()
                            image = bmp
                        }
                    }
                }
            }

            onClick {
                rootWindow.openFileDialog().firstOrNull()?.let { file ->
                    println("loading $file")
                    runCatching {
                        val bmp = file.readBitmap()
                        image = bmp
                    }
                }
            }
        }

    private fun infHpView(): View =
        Text("8").apply {
            font = moreSugar.thin
            textSize = 33.0
            rotation = Angle.QUARTER
            color = Colors.BLACK
            position(65, 305)
        }

    private fun partHpView(top: String, bot: String): View =
        Container().apply {
            addChild(text(top) {
                font = moreSugar.thin
                textSize = 17.0
                color = Colors.BLACK
                position(5, 0)
            })
            addChild(text(bot) {
                font = moreSugar.thin
                textSize = 17.0
                color = Colors.BLACK
                position(25, 25)
            })
            addChild(text("/") {
                font = moreSugar.regular
                textSize = 17.0
                rotation = Angle.fromDegrees(30.0)
                color = Colors.BLACK
                position(25, 14)
            })
            position(20, 295)
        }

    private fun keywordsListView(): View =
        Text(keywords.joinToString("\n")).apply {
            font = moreSugar.thin
            textSize = 7.0
            color = Colors.BLACK
            height = min(height, 50.0)
            position(200, 280)
        }

    private var kwListView = keywordsListView()

    private fun statsView(): View =
        let {
            var pers = personalities.joinToString("")

            if (unpredictability != 0) {
                if (pers.length > 1)
                    pers += " "
                pers += unpredictability.toString()
            }
            Text(pers).apply {
                font = moreSugar.thin
                textSize = 7.0
                color = Colors.BLACK
                position(210, 333)
            }
        }

    private var statsViewV = statsView()

    private val nameView: UITextInput
    private val descView: UITextInput
    private val tagsView: UITextInput
    private val authorView: UITextInput

    init {
        addChild(imgView)
        addChild(image(template) {
            scale(0.5)
        })
        addChild(singlelineText(140.0, placeholder = "Enter name here") {
            font = moreSugar.thin
            textSize = 15.0
            position(60, 275)
        }.also { nameView = it })
        addChild(multilineText(120.0, 40.0, placeholder = "Enter description here") {
            font = moreSugar.thin
            textSize = 11.0
            position(80, 294)
        }.also { descView = it })
        addChild(singlelineText(120.0, placeholder = "Creature") {
            font = moreSugar.thin
            textSize = 8.0
            position(80, 331)
        }.also { tagsView = it })
        addChild(singlelineText(60.0, placeholder = "~ author") {
            font = moreSugar.thin
            textSize = 9.0
            position(185, 345)
        }.also { authorView = it })
        addChild(hpView)
        addChild(kwListView)
        addChild(statsViewV)
    }

    var vname: String by nameView::text
    var vdesc: String by descView::text
    var vtags: String by tagsView::text
    var vauthor: String by authorView::text

    fun updateKeywords() {
        val new = keywordsListView()
        replaceChild(kwListView, new)
        kwListView = new
    }

    fun updatePersonalitiesAndPredictability() {
        val new = statsView()
        replaceChild(statsViewV, new)
        statsViewV = new
    }

    @Serializable
    data class Stats(
        val name: String,
        val desc: String,
        val tags: String,
        val author: String,
        val hp: HP,
        val type: String,
        val keywords: Set<String>,
        val personalities: List<Char>,
        val unpredictability: Int,
    )

    fun encodeTo(): Stats =
        Stats(
            vname,
            vdesc,
            vtags,
            vauthor,
            hp,
            type,
            keywords,
            personalities,
            unpredictability
        )

    fun encodeToJson(): String =
        Json.encodeToString(encodeTo())

    fun decodeFrom(stats: Stats) {
        vname = stats.name
        vdesc = stats.desc
        vtags = stats.tags
        vauthor = stats.author
        hp = stats.hp
        type = stats.type
        keywords.clear()
        keywords.addAll(stats.keywords)
        personalities.clear()
        personalities.addAll(stats.personalities)
        unpredictability = stats.unpredictability

        updateKeywords()
        updatePersonalitiesAndPredictability()
    }

    fun decodeFromJson(json: String) {
        decodeFrom(Json.decodeFromString(json))
    }
}

suspend fun cardView(root: GameWindow, init: CardView.() -> Unit = {}): CardView {
    val moreSugar = FontRef(
        resourcesVfs["more-sugar.thin.ttf"].readTtfFont(),
        resourcesVfs["more-sugar.regular.ttf"].readTtfFont()
    )

    val template = resourcesVfs["CW_TEMP_FINAL.png"].readBitmap()

    val img = resourcesVfs["img.png"].readBitmap()

    return CardView(root, moreSugar, template, img).apply(init)
}
