package cwcardmaker.plugins

import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.date.*
import java.io.File

fun Application.configureRouting() {
    routing {
        get("/file") {
            call.request.queryParameters["name"]?.let {
                val fn = it.replace(File.separatorChar, '_')
                if (fn.startsWith("file-")) {
                    val file = File(fn)
                    call.respondFile(file)
                }
            }
        }
        post("/save") {
            val bytes = call.receiveStream().readAllBytes()
            val name = "file-${getTimeMillis()}.zip"
            val file = File(name)
            file.writeBytes(bytes)
            call.respondText("http://207.180.202.42:7070/file?name=$name")
        }
        staticFiles("/",
            File("../build/dist/js/productionExecutable/")
        )
    }
}
