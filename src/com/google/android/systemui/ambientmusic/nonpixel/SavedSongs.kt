/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2026 Evolution X
 */
package com.google.android.systemui.ambientmusic.nonpixel

import android.content.Context
import android.os.UserHandle
import android.provider.Settings
import android.util.Log
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

/**
 * Local list of songs the person hearted on non-Pixel devices.
 *
 * Stored as a JSON array in [Settings.System] under [KEY], newest first, holding the title,
 * artist, the time it was saved and, when the recognition returned an https cover, its URL.
 * Songs are matched on normalized (lowercase, whitespace-collapsed) title and artist so the same
 * song is recognized again later.
 */
object SavedSongs {
    private const val TAG = "SavedSongs"
    const val KEY = "now_playing_saved"
    const val KEY_ALBUM_ART = "albumArt"
    const val MAX_ENTRIES = 100

    /**
     * Settings.System caps everything a package writes at 40000 bytes, counted as two bytes per
     * character, so about 20000 characters shared by every setting SystemUI owns. Writes past that
     * throw, so the list gets its own smaller budget and drops its oldest entries to fit.
     */
    const val MAX_SAVED_CHARS = 8000

    /**
     * Earlier builds kept a recognition history under this key that nothing ever showed. Empties
     * it so the space goes back to the saved songs list.
     */
    fun clearLegacyHistory(context: Context) {
        try {
            val resolver = context.contentResolver
            val key = "now_playing_history"
            if (!Settings.System.getStringForUser(resolver, key, UserHandle.USER_CURRENT).isNullOrEmpty()) {
                Settings.System.putStringForUser(resolver, key, "", UserHandle.USER_CURRENT)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to clear the old recognition history", e)
        }
    }

    private val WHITESPACE = Regex("\\s+")

    /** Lowercase, whitespace-collapsed form used to tell whether two songs are the same. */
    internal fun normalize(value: String?): String =
        value.orEmpty().trim().replace(WHITESPACE, " ").lowercase(Locale.ROOT)

    /**
     * Returns [entries] limited to [maxEntries] and to [maxChars] once serialized, dropping from
     * the end so the newest entries, which come first, are kept.
     */
    internal fun fitToBudget(entries: List<JSONObject>, maxEntries: Int, maxChars: Int): JSONArray {
        var count = entries.size.coerceAtMost(maxEntries)
        while (count > 0 && toArray(entries, count).toString().length > maxChars) {
            count--
        }
        return toArray(entries, count)
    }

    /**
     * Like [fitToBudget], but a cover URL is much longer than the rest of an entry, so the URLs of
     * the oldest entries are dropped first and an entry is only dropped when that was not enough.
     */
    internal fun fitSavedToBudget(entries: List<JSONObject>): JSONArray {
        val limited = entries.take(MAX_ENTRIES)
        var oldest = limited.size - 1
        while (oldest >= 0 && toArray(limited, limited.size).toString().length > MAX_SAVED_CHARS) {
            limited[oldest].remove(KEY_ALBUM_ART)
            oldest--
        }
        return fitToBudget(limited, MAX_ENTRIES, MAX_SAVED_CHARS)
    }

    private fun toArray(entries: List<JSONObject>, count: Int): JSONArray {
        val array = JSONArray()
        for (i in 0 until count) array.put(entries[i])
        return array
    }

    private fun read(context: Context): JSONArray {
        val json =
            Settings.System.getStringForUser(
                context.contentResolver,
                KEY,
                UserHandle.USER_CURRENT,
            )
        if (json.isNullOrEmpty()) return JSONArray()
        return try {
            JSONArray(json)
        } catch (e: Exception) {
            Log.w(TAG, "Ignoring unreadable saved songs list", e)
            JSONArray()
        }
    }

    private fun matches(entry: JSONObject, title: String, artist: String): Boolean =
        normalize(entry.optString("title")) == title &&
            normalize(entry.optString("artist")) == artist

    @Synchronized
    fun isSaved(context: Context, title: CharSequence?, artist: CharSequence?): Boolean {
        val t = normalize(title?.toString())
        if (t.isEmpty()) return false
        val a = normalize(artist?.toString())
        return try {
            val saved = read(context)
            (0 until saved.length()).any { matches(saved.getJSONObject(it), t, a) }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check saved songs", e)
            false
        }
    }

    /**
     * Saves the song if it is not in the list, removes it if it is. [albumArt] is kept only when it
     * is an https URL: other cover URIs are temporary and could not be opened again later.
     *
     * @return whether the song is saved after the call.
     */
    @Synchronized
    fun toggle(
        context: Context,
        title: CharSequence?,
        artist: CharSequence?,
        albumArt: CharSequence? = null,
    ): Boolean {
        val titleText = title?.toString()?.trim().orEmpty()
        val artistText = artist?.toString()?.trim().orEmpty()
        val t = normalize(titleText)
        if (t.isEmpty()) return false
        val a = normalize(artistText)
        val artText = albumArt?.toString()?.trim().orEmpty()
        return try {
            val saved = read(context)
            val kept = ArrayList<JSONObject>()
            var wasSaved = false
            for (i in 0 until saved.length()) {
                val entry = saved.getJSONObject(i)
                if (matches(entry, t, a)) {
                    wasSaved = true
                } else {
                    kept.add(entry)
                }
            }
            if (!wasSaved) {
                kept.add(
                    0,
                    JSONObject().apply {
                        put("title", titleText)
                        put("artist", artistText)
                        put("timestamp", System.currentTimeMillis())
                        if (artText.startsWith("https://")) put(KEY_ALBUM_ART, artText)
                    },
                )
            }
            val result = fitSavedToBudget(kept)
            Settings.System.putStringForUser(
                context.contentResolver,
                KEY,
                result.toString(),
                UserHandle.USER_CURRENT,
            )
            // A single entry too large for the budget is dropped, so report what was written.
            (0 until result.length()).any { matches(result.getJSONObject(it), t, a) }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to toggle saved song", e)
            isSaved(context, title, artist)
        }
    }
}
