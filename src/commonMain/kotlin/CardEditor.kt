import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.image.format.*
import korlibs.io.async.*
import korlibs.io.file.*
import korlibs.io.file.std.*
import korlibs.io.stream.*
import korlibs.korge.ui.*
import korlibs.korge.view.*
import korlibs.render.*
import kotlinx.serialization.json.*

class CardEditor(
    val gameWindow: GameWindow,
    val showSaveLoadExport: Boolean,
): Container() {
    private val onLoad = Signal<CardView.Stats>()
    private val onSave = Signal<VfsFile>()

    lateinit var cv: CardView

    var changed: Boolean = false
        private set

    suspend fun build(): CardEditor {
        cv = cardView(gameWindow) {
            hp = HP.part(a = 12.0, b = 0.0)
            scale(2.0)

            onLoad.add {
                decodeFrom(it)
            }
        }

        cv.onChanged.add {
            changed = true
        }

        addChild(uiHorizontalStack {
            addChild(uiVerticalStack {
                addChild(uiHorizontalStack {
                    val top = uiVerticalStack {
                        visible(!cv.hp.infinite)
                        val num = uiEditableNumber(min = 0.0, max = 99.9, decimals = 1) {
                            onSetValue.add {
                                cv.hp = cv.hp.copy(atk = it.value)
                            }
                            onLoad.add {
                                if (it.hp.atk == null) {
                                    visible(false)
                                } else {
                                    visible(true)
                                    value = it.hp.atk
                                }
                            }
                        }
                        val enable = uiCheckBox(text = "Top", checked = cv.hp.atk != null) {
                            onChange.add {
                                num.visible(it.checked)
                                cv.hp = if (it.checked) cv.hp.copy(atk = num.value)
                                else cv.hp.copy(atk = null)
                            }
                            onLoad.add {
                                checked = it.hp.atk != null
                            }
                        }
                        onLoad.add {
                            visible(!it.hp.infinite)
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
                            onLoad.add {
                                if (it.hp.hp == null) {
                                    visible(false)
                                } else {
                                    visible(true)
                                    value = it.hp.hp
                                }
                            }
                        }
                        val enable = uiCheckBox(text = "Bottom", checked = cv.hp.hp != null) {
                            onChange.add {
                                num.visible(it.checked)
                                cv.hp = if (it.checked) cv.hp.copy(hp = num.value)
                                else cv.hp.copy(hp = null)
                            }
                            onLoad.add {
                                checked = it.hp.hp != null
                            }
                        }
                        onLoad.add {
                            visible(!it.hp.infinite)
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
                        onLoad.add {
                            checked = it.hp.infinite
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
                        CardView.PERSONALITIES.forEach { p ->
                            addChild(uiCheckBox(text = p) {
                                onChange.add {
                                    if (it.checked) {
                                        cv.personalities.add(p[0])
                                    } else {
                                        cv.personalities.remove(p[0])
                                    }
                                    cv.updatePersonalitiesAndPredictability()
                                }
                                onLoad.add {
                                    checked = p[0] in it.personalities
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
                            onLoad.add {
                                value = it.unpredictability.toDouble()
                            }
                        })
                    })
                }

                addChild(uiHorizontalStack {
                    var kw: View

                    fun buildKw(): View =
                        UIVerticalStack().apply {
                            forcedWidth = width * 2
                            typeKws[cv.type]?.forEach { kw ->
                                addChild(uiCheckBox(text = kw) {
                                    onChange.add {
                                        if (it.checked) {
                                            cv.keywords.add(it.text)
                                        } else {
                                            cv.keywords.remove(it.text)
                                        }
                                        cv.updateKeywords()
                                    }
                                    onLoad.add {
                                        checked = kw in it.keywords
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
                        onLoad.add {
                            selectedItem = it.type
                            personality.visible(it.type == "Creature")
                        }
                    })
                    addChild(kw)
                })

                addChild(personality)

                if (showSaveLoadExport) {
                    addChild(uiHorizontalStack {
                        addChild(uiButton("Save") {
                            onPress.add {
                                launch(gameWindow.coroutineDispatcher) {
                                    val temp = tempVfs["CWCardMakerTemp.zip"]
                                    saveToZip(temp)
                                    save(gameWindow, temp)
                                    gameWindow.alert("saved successfully!")
                                }
                            }
                        })

                        addChild(uiButton("Export") {
                            onPress.add {
                                launch(gameWindow.coroutineDispatcher) {
                                    val temp = tempVfs["CWCardMakerTemp.png"]
                                    exportAsPNG(temp)
                                    save(gameWindow, temp)
                                    gameWindow.alert("exported successfully!")
                                }
                            }
                        })

                        addChild(uiButton("Load") {
                            onPress.add {
                                launch(gameWindow.coroutineDispatcher) {
                                    gameWindow.openFileDialog(FileFilter("ZIP files" to listOf("*.zip"))).firstOrNull()?.let { file ->
                                        try {
                                            loadFromZip(file)
                                        } catch (e: Exception) {
                                            println("fail: $e")
                                            gameWindow.alert("failed to load card")
                                        }
                                    }
                                }
                            }
                        })
                    })
                }

                addChild(text("Unsaved changes") {
                    color = Colors.RED
                    fontSize *= 2
                    visible = false

                    cv.onChanged.add {
                        visible = true
                    }
                    onSave.add {
                        visible = false
                    }
                })
            })

            addChild(cv)
        })

        return this
    }

    suspend fun saveToZip(file: VfsFile) {
        val tmp = tempVfs["CWSimTmp"]
        tmp.mkdirs()

        try {
            tmp["img.png"].writeBitmap(cv.image, PNG)
            tmp["data.json"].writeString(cv.encodeToJson())
            kotlin.runCatching {
                exportAsPNG(tmp["rendering.png"])
            }
            tmp.createZipFromTreeTo(file)

            changed = false
            onSave(file)
        } finally {
            tmp.deleteRecursively()
        }
    }

    data class LoadResult(
        val rendering: Bitmap?
    )

    suspend fun loadFromZip(file: VfsFile): LoadResult {
        println("loading $file")
        val zip = file.openAsZip().vfs
        cv.image = zip.open("img.png", VfsOpenMode.READ)
            .readBitmap()
        val json = zip.open("data.json", VfsOpenMode.READ)
            .readAllAsFastStream()
            .readStringz()
        val data = Json.decodeFromString<CardView.Stats>(json)
        onLoad(data)
        val rendering = kotlin.runCatching {
            zip.open("rendering.png", VfsOpenMode.READ)
            .readBitmap()
        }.getOrNull()
        return LoadResult(rendering)
    }

    suspend fun exportAsPNG(file: VfsFile) {
        val bmp = cv.renderToBitmap()
        if (file.exists()) file.delete()
        file.writeBitmap(bmp, PNG)
    }
}
