package com.example.model

import android.content.Context
import org.json.JSONArray

data class FilterItem(
    val id: String,
    val displayName: String,
    val category: String,
    val colorToneHex: Long,
    val badge: String,
    val tintHex: Long,
    val tintAlpha: Float
)

object FilterRepository {
    private var cachedFilters: List<FilterItem>? = null

    fun getFilters(context: Context): List<FilterItem> {
        cachedFilters?.let { return it }

        val loaded = try {
            val jsonString = context.assets.open("filters.json").bufferedReader().use { it.readText() }
            val array = JSONArray(jsonString)
            val list = mutableListOf<FilterItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val tintHexParsed = try {
                    java.lang.Long.decode(obj.optString("tintHex", "0x00000000"))
                } catch (_: Exception) {
                    0L
                }
                list.add(
                    FilterItem(
                        id = obj.getString("id"),
                        displayName = obj.getString("displayName"),
                        category = obj.getString("category"),
                        colorToneHex = obj.optLong("colorToneHex", 0xFF0D47A1),
                        badge = obj.optString("badge", obj.getString("displayName").take(2).uppercase()),
                        tintHex = tintHexParsed,
                        tintAlpha = obj.optDouble("tintAlpha", 0.2).toFloat()
                    )
                )
            }
            list
        } catch (e: Exception) {
            // Fallback to built-in presets if asset read fails
            StudioPresets.filters.mapIndexed { idx, pair ->
                val (name, cat) = pair
                val words = name.split(" ")
                val badge = if (words.size >= 2) {
                    "${words[0].take(1)}${words[1].take(1)}".uppercase()
                } else {
                    name.take(2).uppercase()
                }
                FilterItem(
                    id = "flt_$idx",
                    displayName = name,
                    category = cat,
                    colorToneHex = 0xFF0D47A1,
                    badge = badge,
                    tintHex = 0xFF00E5FF,
                    tintAlpha = 0.2f
                )
            }
        }

        cachedFilters = loaded
        return loaded
    }

    fun getCategories(context: Context): List<String> {
        val filters = getFilters(context)
        val categories = filters.map { it.category }.distinct().toMutableList()
        if (!categories.contains("All")) {
            categories.add(0, "All")
        }
        return categories
    }
}
