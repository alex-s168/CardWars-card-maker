import korlibs.io.async.*
import korlibs.io.file.*
import korlibs.io.util.*
import korlibs.render.*
import kotlinx.coroutines.flow.*

data class CardPack(
    val name: String,
    val source: VfsFile?,
    var cards: List<Pair<CardEditor, CardEditor.LoadResult>>
) {
    val changed = Signal<CardPack>()

    suspend fun reload(gameWindow: GameWindow) {
        requireNotNull(source)
        cards = source.list().map {
            val ce = CardEditor(gameWindow, true)
            ce to ce.loadFromZip(it)
        }.toList()
        changed(this)
    }

    suspend fun saveAll() {
        requireNotNull(source)
        cards.forEach { (ce, _) ->
            val path = source[ce.cv.vname.escape()]
            ce.saveToZip(path)
        }
    }

    companion object {
        suspend fun load(dir: VfsFile, gameWindow: GameWindow): CardPack {
            return CardPack(dir.baseName, dir, listOf()).also {
                it.reload(gameWindow)
            }
        }
    }
}
