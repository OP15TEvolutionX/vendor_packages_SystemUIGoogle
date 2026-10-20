/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2026 DerpFest AOSP (ported from SystemUIFundamental)
 */
package com.google.android.systemui.ambientmusic.nonpixel.engine

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class HybridMusicRecognitionEngine(private val context: Context) {

    companion object {
        private const val TAG = "HybridMusicEngine"
    }

    private val recorder = AmbientAudioRecorder()
    private val onlineEngine = OnlineSoundSearchEngine(context)
    private val mutex = Mutex()

    suspend fun recognize(
        durationSeconds: Int = 8,
        abortOnSilence: Boolean = true,
    ): SongResult? = withContext(Dispatchers.Default) {
        mutex.withLock {
            Log.d(TAG, "Starting audio capture for recognition (${durationSeconds}s)...")
            val audioBytes = recorder.recordSample(durationSeconds, abortOnSilence) ?: run {
                Log.w(TAG, "No audio sample captured")
                return@withLock null
            }

            val onlineResult = onlineEngine.recognizeOnline(audioBytes)
            if (onlineResult != null) {
                Log.d(TAG, "Matched song online: ${onlineResult.title} - ${onlineResult.artist}")
                return@withLock onlineResult
            }

            Log.d(TAG, "No match found for ambient audio sample")
            return@withLock null
        }
    }
}
