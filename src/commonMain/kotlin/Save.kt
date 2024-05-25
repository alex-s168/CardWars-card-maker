import korlibs.io.file.*
import korlibs.render.*

expect suspend fun save(gameWindow: GameWindow, file: VfsFile)

suspend fun defaultSave(gameWindow: GameWindow, file: VfsFile) {
    gameWindow.openFileDialog(write = true).firstOrNull()?.let {
        file.copyTo(it)
    }
}
