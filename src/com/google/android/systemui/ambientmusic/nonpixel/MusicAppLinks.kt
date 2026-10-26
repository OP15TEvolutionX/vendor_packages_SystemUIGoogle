/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2026 Evolution X
 */
package com.google.android.systemui.ambientmusic.nonpixel

import android.app.PendingIntent
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import com.google.android.systemui.res.R

/**
 * Builds the "play" action shown in the play slot of the expanded ambient
 * indication on non-Pixel devices.
 *
 * The action uses the generic play-from-search intent, so it works with whatever
 * music app the device has. If exactly one app handles it, that app is launched
 * directly and its icon is shown. If several do, the system chooser lets the
 * person pick, and [Target.packageName] is null so a generic icon is shown.
 */
object MusicAppLinks {

    class Target(val pendingIntent: PendingIntent, val packageName: String?)

    private class App(val packageName: String, val searchUri: (String) -> String)

    /** Used only when no installed app handles play-from-search. */
    private val FALLBACK_APPS = listOf(
        App("com.spotify.music") { "spotify:search:$it" },
        App("com.google.android.apps.youtube.music") { "https://music.youtube.com/search?q=$it" },
    )

    fun createPlayIntent(title: CharSequence?, artist: CharSequence?): Intent? {
        val query = listOfNotNull(title, artist)
            .filter { it.isNotEmpty() }
            .joinToString(" ")
        if (query.isEmpty()) return null
        return Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH).apply {
            putExtra(SearchManager.QUERY, query)
            putExtra(MediaStore.EXTRA_MEDIA_FOCUS, MediaStore.Audio.Media.ENTRY_CONTENT_TYPE)
            if (!title.isNullOrEmpty()) {
                putExtra(MediaStore.EXTRA_MEDIA_TITLE, title.toString())
            }
            if (!artist.isNullOrEmpty()) {
                putExtra(MediaStore.EXTRA_MEDIA_ARTIST, artist.toString())
            }
        }
    }

    fun create(context: Context, title: CharSequence?, artist: CharSequence?): Target? {
        val playIntent = createPlayIntent(title, artist) ?: return null
        val packages = context.packageManager
            .queryIntentActivities(playIntent, PackageManager.MATCH_DEFAULT_ONLY)
            .map { it.activityInfo.packageName }
            .distinct()
        return when {
            packages.size == 1 ->
                target(context, Intent(playIntent).setPackage(packages[0]), packages[0])
            packages.size > 1 ->
                target(
                    context,
                    Intent.createChooser(playIntent, context.getString(R.string.ambient_music_play_with)),
                    null,
                )
            else -> createFallback(context, title, artist)
        }
    }

    private fun createFallback(
        context: Context,
        title: CharSequence?,
        artist: CharSequence?,
    ): Target? {
        val encoded = Uri.encode(
            listOfNotNull(title, artist).filter { it.isNotEmpty() }.joinToString(" ")
        )
        for (app in FALLBACK_APPS) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(app.searchUri(encoded)))
                .setPackage(app.packageName)
            if (context.packageManager.resolveActivity(intent, 0) == null) continue
            return target(context, intent, app.packageName)
        }
        return null
    }

    private fun target(context: Context, intent: Intent, packageName: String?): Target {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return Target(pendingIntent, packageName)
    }
}
