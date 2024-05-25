import korlibs.io.file.*
import korlibs.render.*

actual suspend fun export(gameWindow: GameWindow, file: VfsFile) =
    defaultExport(gameWindow, file)

actual suspend fun CardEditor.save(gameWindow: GameWindow, editor: CardEditor) =
    defaultSave(gameWindow, editor)
