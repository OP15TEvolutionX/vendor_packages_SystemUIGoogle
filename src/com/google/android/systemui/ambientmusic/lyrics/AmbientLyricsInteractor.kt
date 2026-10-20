/*
 * SPDX-FileCopyrightText: Evolution X
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.systemui.ambientmusic.lyrics

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.provider.Settings
import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.dagger.qualifiers.Application
import com.android.systemui.keyguard.domain.interactor.KeyguardInteractor
import com.android.systemui.statusbar.phone.LyricsFetcher
import com.google.android.systemui.keyguard.domain.interactor.AmbientIndicationInteractor
import com.google.android.systemui.keyguard.shared.AmbientIndicationMusic
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Shows the synced lyric line of the music playing on this phone in the ambient indication.
 *
 * Recognition only ever shows songs heard near the phone, and the ambient indication is dropped
 * while the phone itself is playing media. Lyrics mode is the one case where the indication is
 * wanted during playback, so it is exposed as its own state and merged by the view model.
 *
 * The lyric source is only registered while the [SETTING_LYRICS_MODE] system setting is on, as
 * [LyricsFetcher] polls playback position and queries the network while it has a callback.
 */
@SysUISingleton
class AmbientLyricsInteractor
@Inject
constructor(
    @Application private val context: Context,
    @Application scope: CoroutineScope,
    private val ambientIndicationInteractor: AmbientIndicationInteractor,
    private val keyguardInteractor: KeyguardInteractor,
) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val currentLine = MutableStateFlow<String?>(null)

    private var callbackRegistered = false

    private val lyricsFetcher: LyricsFetcher by lazy { LyricsFetcher.getInstance(context) }

    private val callback =
        object : LyricsFetcher.Callback {
            override fun onSyncedLineChanged(
                prevLine: String?,
                currentLine: String?,
                nextLine: String?,
            ) {
                this@AmbientLyricsInteractor.currentLine.value =
                    currentLine?.trim()?.takeIf { it.isNotEmpty() }
            }

            // Unsynced lyrics are the whole song. They cannot follow playback and do not fit
            // the single line pill, so treat them as having no lyrics.
            override fun onPlainLyricsAvailable(plainLyrics: String?) {
                currentLine.value = null
            }

            override fun onLyricsCleared() {
                currentLine.value = null
            }
        }

    /** The current lyric as an indication, while the phone is playing music. Otherwise null. */
    val lyricsState: StateFlow<AmbientIndicationMusic?> =
        combine(currentLine, ambientIndicationInteractor.devicePlayingMusic) { line, playing ->
                if (line != null && playing) AmbientIndicationMusic(text = line) else null
            }
            .stateIn(scope, SharingStarted.Eagerly, null)

    init {
        val observer =
            object : ContentObserver(mainHandler) {
                override fun onChange(selfChange: Boolean) {
                    updateRegistration()
                }
            }
        context.contentResolver.registerContentObserver(
            Settings.System.getUriFor(SETTING_LYRICS_MODE),
            false,
            observer,
            UserHandle.USER_ALL,
        )
        mainHandler.post { updateRegistration() }

        // Keep keyguard layout in step with the indication, as setAmbientMusic does.
        scope.launch {
            lyricsState.collect { lyric ->
                if (lyric != null) {
                    keyguardInteractor.setAmbientIndicationVisible(true)
                } else if (ambientIndicationInteractor.ambientMusicState.value == null) {
                    keyguardInteractor.setAmbientIndicationVisible(false)
                }
            }
        }
    }

    private fun updateRegistration() {
        val enabled =
            Settings.System.getIntForUser(
                context.contentResolver,
                SETTING_LYRICS_MODE,
                0,
                UserHandle.USER_CURRENT,
            ) == 1
        if (enabled && !callbackRegistered) {
            callbackRegistered = true
            lyricsFetcher.addCallback(callback)
        } else if (!enabled && callbackRegistered) {
            callbackRegistered = false
            lyricsFetcher.removeCallback(callback)
            currentLine.value = null
        }
    }

    companion object {
        const val SETTING_LYRICS_MODE = "now_playing_lyrics_mode"
    }
}
