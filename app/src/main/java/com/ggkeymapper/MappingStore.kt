package com.ggkeymapper

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class KeyMapping(val keyCode: Int, val x: Float, val y: Float)

object MappingStore {
    private const val PREFS = "gg_mapper_settings"
    private const val KEY_MAPPINGS = "key_mappings"
    private const val KEY_PROFILE = "active_profile"

    fun load(context: Context): MutableList<KeyMapping> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_MAPPINGS, "[]") ?: "[]"
        val result = mutableListOf<KeyMapping>()
        try {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                result.add(KeyMapping(item.optInt("keyCode"), item.optDouble("x", 0.5).toFloat().coerceIn(0f, 1f), item.optDouble("y", 0.5).toFloat().coerceIn(0f, 1f)))
            }
        } catch (_: Exception) {}
        return result
    }

    fun save(context: Context, mappings: List<KeyMapping>) {
        val array = JSONArray()
        mappings.forEach { array.put(JSONObject().put("keyCode", it.keyCode).put("x", it.x).put("y", it.y)) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_MAPPINGS, array.toString()).apply()
    }

    fun saveProfile(context: Context, name: String, mappings: List<KeyMapping>) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val profiles = try { JSONObject(prefs.getString("profiles", "{}") ?: "{}") } catch (_: Exception) { JSONObject() }
        val array = JSONArray()
        mappings.forEach { array.put(JSONObject().put("keyCode", it.keyCode).put("x", it.x).put("y", it.y)) }
        val profileName = name.ifBlank { "Padrão" }
        profiles.put(profileName, array)
        prefs.edit().putString("profiles", profiles.toString()).putString(KEY_PROFILE, profileName).apply()
        save(context, mappings)
    }

    fun loadProfile(context: Context, name: String): MutableList<KeyMapping>? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return try {
            val profiles = JSONObject(prefs.getString("profiles", "{}") ?: "{}")
            val array = profiles.optJSONArray(name) ?: return null
            val result = mutableListOf<KeyMapping>()
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                result.add(KeyMapping(item.optInt("keyCode"), item.optDouble("x", .5).toFloat().coerceIn(0f, 1f), item.optDouble("y", .5).toFloat().coerceIn(0f, 1f)))
            }
            save(context, result)
            result
        } catch (_: Exception) { null }
    }

    fun profileNames(context: Context): List<String> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("profiles", "{}") ?: "{}"
        return try {
            val names = mutableListOf<String>()
            val keys = JSONObject(raw).keys()
            while (keys.hasNext()) names.add(keys.next())
            names.sorted()
        } catch (_: Exception) { emptyList() }
    }
}
