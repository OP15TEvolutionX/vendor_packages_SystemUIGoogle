package com.google.android.systemui.keyguard.domain.interactor

import android.app.PendingIntent
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.os.Handler
import android.os.Looper
import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.keyguard.domain.interactor.KeyguardInteractor
import com.google.android.systemui.keyguard.data.repository.AmbientIndicationRepository
import com.google.android.systemui.keyguard.shared.AmbientIndicationMusic
import com.google.android.systemui.keyguard.shared.ExtendedIndication
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@SysUISingleton
class AmbientIndicationInteractor
@Inject
constructor(
    val ambientIndicationRepository: AmbientIndicationRepository,
    val keyguardInteractor: KeyguardInteractor,
    private val audioManager: AudioManager,
) {

    val ambientMusicState: StateFlow<AmbientIndicationMusic?> =
        ambientIndicationRepository.ambientMusic.asStateFlow()

    private val _devicePlayingMusic = MutableStateFlow(audioManager.isMusicActive)

    /** True while this phone is outputting media. */
    val devicePlayingMusic: StateFlow<Boolean> = _devicePlayingMusic.asStateFlow()

    init {
        audioManager.registerAudioPlaybackCallback(
            object : AudioManager.AudioPlaybackCallback() {
                override fun onPlaybackConfigChanged(configs: List<AudioPlaybackConfiguration>) {
                    val playing = audioManager.isMusicActive
                    _devicePlayingMusic.value = playing
                    // Speakers (or a cast from this phone) are not music near the phone.
                    // Drop the indication so the lockscreen island can use the slot.
                    if (playing) {
                        hideAmbientMusic()
                    }
                }
            },
            Handler(Looper.getMainLooper()),
        )
    }

    fun hideAmbientMusic() {
        ambientIndicationRepository.ambientMusic.value = null
        keyguardInteractor.setAmbientIndicationVisible(false)
    }

    fun setAmbientMusic(
        text: CharSequence?,
        openIntent: PendingIntent?,
        favoritingIntent: PendingIntent?,
        iconOverride: Int?,
        skipUnlock: Boolean?,
        iconDescription: String?,
        extendedIndication: ExtendedIndication?,
    ) {
        if (audioManager.isMusicActive) {
            hideAmbientMusic()
            return
        }
        ambientIndicationRepository.ambientMusic.value =
            AmbientIndicationMusic(
                text = text,
                openIntent = openIntent,
                favoritingIntent = favoritingIntent,
                iconOverride = iconOverride,
                skipUnlock = skipUnlock,
                iconDescription = iconDescription,
                extendedIndication = extendedIndication,
            )
        keyguardInteractor.setAmbientIndicationVisible(true)
    }
}
