import korlibs.image.color.*
import korlibs.image.font.*
import korlibs.image.format.*
import korlibs.io.async.*
import korlibs.io.file.*
import korlibs.io.file.std.*
import korlibs.io.lang.*
import korlibs.io.stream.*
import korlibs.korge.*
import korlibs.korge.scene.*
import korlibs.korge.ui.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import korlibs.render.*
import kotlinx.serialization.Serializable

suspend fun main() = Korge(windowSize = Size(900, 800), backgroundColor = Colors["#2b2b2b"]) {
	val sceneContainer = sceneContainer()
	sceneContainer.changeTo { MyScene() }
}

data class FontRef(
    val thin: TtfFont,
    val regular: TtfFont,
)

@Serializable
data class HP(
    val infinite: Boolean,
    val atk: Double?,
    val hp: Double?,
) {
    companion object {
        fun infinite(): HP =
            HP(true, null, null)

        fun none(): HP =
            HP(false, null, null)

        fun part(a: Double? = null, b: Double? = null): HP =
            HP(false, a, b)
    }
}

fun Double.goodToString(): String {
    val v = toString()
    if (v.endsWith(".0"))
        return v.substring(0, v.length - 2)
    return v
}

class MyScene : Scene() {
	override suspend fun SContainer.sceneMain() {
        addChild(uiHorizontalStack {
            val cv = cardView(gameWindow) {
                hp = HP.part(a = 12.0, b = 0.0)
                scale(2.0)
            }

            addChild(uiVerticalStack {
                addChild(uiHorizontalStack {
                    val top = uiVerticalStack {
                        visible(!cv.hp.infinite)
                        val num = uiEditableNumber(min = 0.0, max = 99.9, decimals = 1) {
                            onSetValue.add {
                                cv.hp = cv.hp.copy(atk = it.value)
                            }
                        }
                        val enable = uiCheckBox(text = "Top", checked = cv.hp.atk != null) {
                            onChange.add {
                                num.visible(it.checked)
                                cv.hp = if (it.checked) cv.hp.copy(atk = num.value)
                                else cv.hp.copy(atk = null)
                            }
                        }
                        addChild(enable)
                        addChild(num)
                    }

                    val bottom = uiVerticalStack {
                        visible(!cv.hp.infinite)
                        val num = uiEditableNumber(min = 0.0, max = 99.9, decimals = 1) {
                            onSetValue.add {
                                cv.hp = cv.hp.copy(hp = it.value)
                            }
                        }
                        val enable = uiCheckBox(text = "Bottom", checked = cv.hp.hp != null) {
                            onChange.add {
                                num.visible(it.checked)
                                cv.hp = if (it.checked) cv.hp.copy(hp = num.value)
                                else cv.hp.copy(hp = null)
                            }
                        }
                        addChild(enable)
                        addChild(num)
                    }

                    val inf = uiCheckBox(text = "Infinite") {
                        onChange.add {
                            top.visible(!it.checked)
                            bottom.visible(!it.checked)
                            cv.hp = cv.hp.copy(infinite = it.checked)
                        }
                    }

                    addChild(inf)
                    addChild(top)
                    addChild(bottom)
                })

                val creatureKw = listOf(
                    "Flying",
                    "Immobile",
                    "Slow",
                    "Ranged",
                    "Useless",
                    "Chaotic",
                    "Worker",
                )

                val structureKw = listOf(
                    "Area",
                    "NonProfit"
                )

                val artifactKw = listOf(
                    "Held",
                    "Built",
                )

                val typeKws = mapOf(
                    "Creature" to creatureKw,
                    "Structure" to structureKw,
                    "Artifact" to artifactKw,
                )

                val personality = UIHorizontalStack().apply {
                    addChild(uiVerticalStack {
                        forcedWidth = width * 2
                        CardView.PERSONALITIES.forEach {
                            addChild(uiCheckBox(text = it) {
                                onChange.add {
                                    if (it.checked) {
                                        cv.personalities.add(it.text[0])
                                    } else {
                                        cv.personalities.remove(it.text[0])
                                    }
                                    cv.updatePersonalitiesAndPredictability()
                                }
                            })
                        }
                    })
                    addChild(uiVerticalStack {
                        addChild(uiText("Unpredictability:"))
                        addChild(uiEditableNumber(min = 0.0, max = 99.9, decimals = 0) {
                            onSetValue.add {
                                cv.unpredictability = it.value.toInt()
                                cv.updatePersonalitiesAndPredictability()
                            }
                        })
                    })
                }

                addChild(uiHorizontalStack {
                    var kw: View

                    fun buildKw(): View =
                        UIVerticalStack().apply {
                            forcedWidth = width * 2
                            typeKws[cv.type]?.forEach {
                                addChild(uiCheckBox(text = it) {
                                    onChange.add {
                                        if (it.checked) {
                                            cv.keywords.add(it.text)
                                        } else {
                                            cv.keywords.remove(it.text)
                                        }
                                        cv.updateKeywords()
                                    }
                                })
                            }
                        }

                    kw = buildKw()

                    val pa = this
                    addChild(uiComboBox(items = CardView.TYPES) {
                        onSelectionUpdate.add {
                            cv.type = it.selectedItem ?: "Creature"
                            personality.visible(cv.type == "Creature")

                            cv.keywords.clear()
                            cv.updateKeywords()

                            pa.removeChild(kw)
                            val new = buildKw()
                            pa.addChild(new)
                            kw = new
                        }
                    })
                    addChild(kw)
                })

                addChild(personality)

                addChild(uiHorizontalStack {
                    addChild(uiButton("Save") {
                        onPress.add {
                            runBlockingNoSuspensions {
                                gameWindow.openFileDialog(FileFilter("ZIP files" to listOf("*.zip")), write = true).firstOrNull()?.let { dest ->
                                    val tmp = tempVfs["CWSimTmp"]
                                    tmp.mkdirs()

                                    tmp["img.png"].writeBitmap(cv.image, PNG)
                                    tmp["data.json"].writeString(cv.encodeToJson())

                                    tmp.createZipFromTreeTo(dest)

                                    tmp.deleteRecursively()

                                    gameWindow.alert("saved successfully!")
                                }
                            }
                        }
                    })

                    addChild(uiButton("Export") {
                        onPress.add {
                            runBlockingNoSuspensions {
                                gameWindow.openFileDialog(FileFilter("PNG files" to listOf("*.png")), write = true).firstOrNull()?.let { file ->
                                    val bmp = cv.renderToBitmap()
                                    file.writeBitmap(bmp, PNG)
                                }
                            }
                        }
                    })

                    addChild(uiButton("Load") {
                        onPress.add {
                            runBlockingNoSuspensions {
                                gameWindow.openFileDialog(FileFilter("ZIP files" to listOf("*.zip"))).firstOrNull()?.let { file ->
                                    try {
                                        println("loading $file")
                                        val zip = file.openAsZip().vfs
                                        cv.image = zip.open("img.png", VfsOpenMode.READ)
                                            .readBitmap()
                                        val json = zip.open("data.json", VfsOpenMode.READ)
                                            .readAllAsFastStream()
                                            .readStringz()
                                        cv.decodeFromJson(json)
                                    } catch (e: Exception) {
                                        println("fail: $e")
                                        gameWindow.alert("failed to load card")
                                    }
                                }
                            }
                        }
                    })
                })
            })

            addChild(cv)
        })
	}
}
