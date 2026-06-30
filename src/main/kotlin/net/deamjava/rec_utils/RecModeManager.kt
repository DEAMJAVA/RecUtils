package net.deamjava.rec_utils

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import net.fabricmc.loader.api.FabricLoader
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

enum class RecFeature(val id: String, val defaultValue: Boolean) {
    NO_SPECTATORS("no_spectators", true),
    NO_ADMIN_COMMAND("no_admin_command", true);

    companion object {
        fun fromId(id: String): RecFeature? = entries.find { it.id.equals(id, ignoreCase = true) }
    }
}

data class PlayerRecData(
    var recMode: Boolean = false,
    val features: MutableMap<String, Boolean> = mutableMapOf()
) {
    fun featureValue(feature: RecFeature): Boolean =
        features[feature.id] ?: feature.defaultValue
}

object RecModeManager {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val data = ConcurrentHashMap<UUID, PlayerRecData>()
    private lateinit var saveFile: File

    fun init() {
        val dir = FabricLoader.getInstance().configDir.resolve(RecUtils.MOD_ID).toFile()
        if (!dir.exists()) dir.mkdirs()
        saveFile = File(dir, "playerdata.json")
        load()
    }

    private fun load() {
        if (!saveFile.exists()) return
        try {
            saveFile.reader().use { reader ->
                val type = object : TypeToken<Map<String, PlayerRecData>>() {}.type
                val raw: Map<String, PlayerRecData>? = gson.fromJson(reader, type)
                raw?.forEach { (uuidStr, value) ->
                    runCatching { UUID.fromString(uuidStr) }.getOrNull()?.let { uuid ->
                        data[uuid] = value
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Synchronized
    fun save() {
        try {
            saveFile.writer().use { writer ->
                val asStrings = data.entries.associate { it.key.toString() to it.value }
                gson.toJson(asStrings, writer)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun dataFor(uuid: UUID): PlayerRecData = data.getOrPut(uuid) { PlayerRecData() }

    @JvmStatic
    fun isRecMode(uuid: UUID): Boolean = data[uuid]?.recMode ?: false

    fun toggleRecMode(uuid: UUID): Boolean {
        val entry = dataFor(uuid)
        entry.recMode = !entry.recMode
        save()
        return entry.recMode
    }

    @JvmStatic
    fun isFeatureEnabled(uuid: UUID, feature: RecFeature): Boolean =
        dataFor(uuid).featureValue(feature)

    fun setFeature(uuid: UUID, feature: RecFeature, value: Boolean) {
        dataFor(uuid).features[feature.id] = value
        save()
    }
}