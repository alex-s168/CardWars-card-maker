import korlibs.io.file.*
import korlibs.render.*

actual suspend fun save(gameWindow: GameWindow, file: VfsFile) =
    defaultSave(gameWindow, file)
