/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2026 DerpFest AOSP (ported from SystemUIFundamental)
 */
package com.google.android.systemui.ambientmusic.nonpixel.engine

import android.net.Uri

data class SongResult(
    val title: String,
    val artist: String,
    val albumArtUri: Uri? = null,
    val source: RecognitionSource = RecognitionSource.OFFLINE_NEURAL,
    val timestampMillis: Long = System.currentTimeMillis(),
) {
    enum class RecognitionSource {
        OFFLINE_NEURAL,
        ONLINE_FALLBACK,
    }
}
