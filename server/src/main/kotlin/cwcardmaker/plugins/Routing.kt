package cwcardmaker.plugins

import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.date.*
import io.ktor.utils.io.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.*
import java.io.*
import java.nio.file.*
import java.util.zip.*

fun zipFiles(files: List<File>, out: File) {
    val fos = FileOutputStream(out)
    val zipOut = ZipOutputStream(fos)

    for (srcFile in files) {
        val fis = FileInputStream(srcFile)
        val zipEntry = ZipEntry(srcFile.name)
        zipOut.putNextEntry(zipEntry)

        val bytes = ByteArray(1024)
        var length: Int
        while ((fis.read(bytes).also { length = it }) >= 0) {
            zipOut.write(bytes, 0, length)
        }
        fis.close()
    }

    zipOut.close()
    fos.close()
}

fun Application.configureRouting() {
    routing {
        get("/file") {
            withContext(Dispatchers.IO) {
                call.request.queryParameters["name"]?.let {
                    val fn = it.replace(File.separatorChar, '_')
                    if (fn.startsWith("file-")) {
                        val file = File(fn)
                        call.respondFile(file)
                    }
                }
            }
        }
        post("/export") {
            withContext(Dispatchers.IO) {
                val ext = call.request.queryParameters["ext"] ?: "bin"

                val bytes = call.receiveStream().readAllBytes()
                val name = "file-${getTimeMillis()}.$ext"
                val file = File(name)
                file.writeBytes(bytes)
                call.respondText("http://207.180.202.42:7070/file?name=$name")
            }
        }
        post("/save") {
            withContext(Dispatchers.IO) {
                val stream = call.receiveChannel()

                val dir = Files.createTempDirectory("cwcardmaker").toFile()
                val jsonFile = File(dir, "data.json")
                val pngFile = File(dir, "img.png")

                // read from stream until 0 into jsonFile and rest into png file

                val jsonLen = stream.readIntLittleEndian()
                jsonFile.outputStream().use {
                    stream.copyTo(it, limit = jsonLen.toLong())
                }

                pngFile.outputStream().use {
                    stream.copyTo(it)
                }

                // pack to zip

                val outFileName = "file-${getTimeMillis()}.zip"
                val outFile = File(outFileName)
                zipFiles(listOf(jsonFile, pngFile), outFile)

                call.respondText("http://207.180.202.42:7070/file?name=$outFileName")
            }
        }
        staticFiles("/",
            File("../build/dist/js/productionExecutable/")
        )
    }
}
