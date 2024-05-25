import korlibs.image.format.*
import korlibs.io.file.*
import korlibs.io.lang.*
import korlibs.io.net.*
import korlibs.io.net.http.*
import korlibs.render.*
import kotlinx.browser.*

actual suspend fun export(gameWindow: GameWindow, file: VfsFile) {
    val url = createHttpClient().post(
        "http://207.180.202.42:7070/export?ext=${file.extension}",
        HttpBodyContent(
            MimeType.APPLICATION_OCTET_STREAM.mime,
            file.readAll()
        )
    ).checkErrors().readAllString(UTF8)
    window.open(url, "_blank")
}

actual suspend fun CardEditor.save(gameWindow: GameWindow, editor: CardEditor) {
    val json = editor.cv.encodeToJson().toByteArray(UTF8)
    val img = editor.cv.image.encode(PNG)
    val url = createHttpClient().post(
        "http://207.180.202.42:7070/save",
        HttpBodyContent(
            MimeType.APPLICATION_OCTET_STREAM.mime,
            json + byteArrayOf(0) + img,
        )
    ).checkErrors().readAllString(UTF8)
    window.open(url, "_blank")
}
