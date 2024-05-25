import korlibs.io.file.VfsFile
import korlibs.render.*

actual suspend fun save(gameWindow: GameWindow, file: VfsFile) =
    defaultSave(gameWindow, file)
