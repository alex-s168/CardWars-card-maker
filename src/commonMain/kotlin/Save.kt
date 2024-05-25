import korlibs.io.file.*
import korlibs.render.*

expect suspend fun export(gameWindow: GameWindow, file: VfsFile)

suspend fun defaultExport(gameWindow: GameWindow, file: VfsFile) {
    gameWindow.openFileDialog(write = true).firstOrNull()?.let {
        file.copyTo(it)
    }
}

suspend fun CardEditor.defaultSave(gameWindow: GameWindow, editor: CardEditor) {
    gameWindow.openFileDialog(write = true).firstOrNull()?.let { file ->
        editor.saveToZip(file)
    }
}

expect suspend fun CardEditor.save(gameWindow: GameWindow, editor: CardEditor)
