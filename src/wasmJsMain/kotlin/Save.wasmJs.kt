import korlibs.io.file.*
import korlibs.io.lang.*
import korlibs.io.net.*
import korlibs.io.net.http.*
import korlibs.render.*
import kotlinx.browser.*

actual suspend fun save(gameWindow: GameWindow, file: VfsFile) {
    val url = createHttpClient().post(
        "http://207.180.202.42:7070/save",
        HttpBodyContent(
            MimeType.APPLICATION_OCTET_STREAM.mime,
            file.readAll()
        )
    ).checkErrors().readAllString(UTF8)
    window.open(url, "_blank")
}
