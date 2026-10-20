package com.google.android.systemui.keyguard.ui.viewmodel

import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.dagger.qualifiers.Application
import com.android.systemui.dagger.qualifiers.Background
import com.android.systemui.dagger.qualifiers.Main
import com.android.systemui.keyguard.domain.interactor.BurnInInteractor
import com.android.systemui.keyguard.domain.interactor.KeyguardTransitionInteractor
import com.android.systemui.keyguard.shared.model.BurnInModel
import com.android.systemui.keyguard.shared.model.KeyguardState
import com.android.systemui.res.R as AR
import com.google.android.systemui.ambientmusic.lyrics.AmbientLyricsInteractor
import com.google.android.systemui.keyguard.domain.interactor.AmbientIndicationInteractor
import com.google.android.systemui.keyguard.shared.AmbientIndicationMusic
import com.google.android.systemui.res.R
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@SysUISingleton
class KeyguardAmbientIndicationViewModel
@Inject
constructor(
    ambientIndicationInteractor: AmbientIndicationInteractor,
    ambientLyricsInteractor: AmbientLyricsInteractor,
    burnInInteractor: BurnInInteractor,
    keyguardTransitionInteractor: KeyguardTransitionInteractor,
    @Background private val bgCoroutineContext: CoroutineContext,
    @Main private val mainDispatcher: CoroutineDispatcher,
    @Application scope: CoroutineScope,
) {
    /** A recognized song or search, otherwise the lyric of the music playing on this phone. */
    val ambientIndicationMusicState: StateFlow<AmbientIndicationMusic?> =
        combine(ambientIndicationInteractor.ambientMusicState, ambientLyricsInteractor.lyricsState) {
                music,
                lyric ->
                music ?: lyric
            }
            .stateIn(scope, SharingStarted.Eagerly, null)

    /** True while the indication is a lyric line, which is shown during local playback. */
    val lyricsShowing: StateFlow<Boolean> =
        ambientLyricsInteractor.lyricsState
            .map { it != null }
            .stateIn(scope, SharingStarted.Eagerly, false)

    val devicePlayingMusic: StateFlow<Boolean> = ambientIndicationInteractor.devicePlayingMusic

    private val burnInFlow: Flow<BurnInModel> =
        combine(
                burnInInteractor.burnIn(
                    xDimenResourceId = AR.dimen.burn_in_prevention_offset_x,
                    yDimenResourceId = AR.dimen.default_burn_in_prevention_offset,
                ),
                keyguardTransitionInteractor.transitionValue(KeyguardState.AOD),
            ) { burnIn, aodFactor ->
                BurnInModel(
                    translationX = (burnIn.translationX * aodFactor).toInt(),
                    translationY = (burnIn.translationY * aodFactor).toInt(),
                    scale = burnIn.scale,
                    scaleClockOnly = burnIn.scaleClockOnly,
                )
            }
            .distinctUntilChanged()
            .flowOn(bgCoroutineContext)

    val indicationAreaTranslationX: Flow<Float> =
        burnInFlow.map { it.translationX.toFloat() }.flowOn(mainDispatcher)

    val indicationAreaTranslationY: Flow<Float> =
        burnInFlow.map { it.translationY.toFloat() }.flowOn(mainDispatcher)
}
