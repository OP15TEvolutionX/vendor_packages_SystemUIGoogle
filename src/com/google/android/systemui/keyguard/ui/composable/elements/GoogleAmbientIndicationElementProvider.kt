package com.google.android.systemui.keyguard.ui.composable.elements

import android.content.Context
import android.view.LayoutInflater
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.compose.animation.scene.ElementContentScope
import com.android.compose.animation.scene.ElementKey
import com.android.compose.theme.PlatformTheme
import com.android.keyguard.KeyguardUpdateMonitor
import com.android.systemui.axdynamicbar.ui.AxDynamicBarChipViewModel
import com.android.systemui.axdynamicbar.ui.compose.AxDynamicBarKeyguardChip
import com.android.systemui.common.ui.domain.interactor.ConfigurationInteractor
import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.dagger.qualifiers.Application
import com.android.systemui.dagger.qualifiers.Background
import com.android.systemui.dagger.qualifiers.Main
import com.android.systemui.graphics.ImageLoader
import com.android.systemui.keyguard.domain.interactor.KeyguardInteractor
import com.android.systemui.keyguard.ui.viewmodel.KeyguardRootViewModel
import com.android.systemui.plugins.ActivityStarter
import com.android.systemui.plugins.FalsingManager
import com.android.systemui.plugins.keyguard.ui.composable.elements.BaseLockscreenElement.ElementSource
import com.android.systemui.plugins.keyguard.ui.composable.elements.LockscreenElement
import com.android.systemui.plugins.keyguard.ui.composable.elements.LockscreenElementKeys
import com.android.systemui.plugins.keyguard.ui.composable.elements.LockscreenElementProvider
import com.android.systemui.plugins.keyguard.ui.composable.elements.LockscreenScope
import com.android.systemui.power.domain.interactor.PowerInteractor
import com.android.systemui.util.concurrency.DelayableExecutor
import com.android.systemui.util.wakelock.DelayedWakeLock
import com.google.android.systemui.ambientmusic.AmbientIndicationContainer
import com.google.android.systemui.keyguard.ui.binder.KeyguardAmbientIndicationAreaViewBinder
import com.google.android.systemui.keyguard.ui.viewmodel.KeyguardAmbientIndicationViewModel
import com.google.android.systemui.res.R
import java.util.concurrent.Executor
import javax.inject.Inject
import kotlinx.coroutines.DisposableHandle

@SysUISingleton
class GoogleAmbientIndicationElementProvider
@Inject
constructor(
    @Application val context: Context,
    val viewModel: KeyguardAmbientIndicationViewModel,
    val powerInteractor: PowerInteractor,
    val keyguardUpdateMonitor: KeyguardUpdateMonitor,
    val activityStarter: ActivityStarter,
    val delayedWakeLockFactory: DelayedWakeLock.Factory,
    val keyguardInteractor: KeyguardInteractor,
    val configurationInteractor: ConfigurationInteractor,
    val imageLoader: ImageLoader,
    val keyguardRootViewModel: KeyguardRootViewModel,
    val falsingManager: FalsingManager,
    @Main val mainDelayableExecutor: DelayableExecutor,
    @Background val backgroundExecutor: Executor,
    val axDynamicBarChipViewModel: AxDynamicBarChipViewModel,
) : LockscreenElementProvider {

    override val elements: List<LockscreenElement> by lazy { listOf(IndicationAreaElement()) }

    @Composable
    fun AmbientIndication(modifier: Modifier = Modifier) {
        var bindingHandle by remember { mutableStateOf<DisposableHandle?>(null) }
        var chipHeight by remember { mutableIntStateOf(0) }
        val expanded by axDynamicBarChipViewModel.isKeyguardExpanded.collectAsStateWithLifecycle()
        val isLowUdfps by axDynamicBarChipViewModel.isLowUdfps.collectAsStateWithLifecycle()
        val ambientMusic by viewModel.ambientIndicationMusicState.collectAsStateWithLifecycle()
        val devicePlayingMusic by viewModel.devicePlayingMusic.collectAsStateWithLifecycle()
        val lyricsShowing by viewModel.lyricsShowing.collectAsStateWithLifecycle()

        // A search or a recognized song has to be visible even while the low-UDFPS chip is
        // using this slot. Playback on the phone is not that song, but its lyrics are shown.
        val canShow = !devicePlayingMusic || lyricsShowing
        val showingNowPlaying = ambientMusic != null && canShow
        val showNowPlayingView =
            if (isLowUdfps) showingNowPlaying || chipHeight == 0 else canShow

        Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            // On a low fingerprint sensor the lower-region row covers the sensor, so the
            // dynamic bar chip is drawn here, above the lock icon, instead.
            if (isLowUdfps && !showingNowPlaying) {
                PlatformTheme {
                    AxDynamicBarKeyguardChip(
                        viewModel = axDynamicBarChipViewModel,
                        modifier =
                            Modifier.onSizeChanged { chipHeight = it.height }
                                .then(
                                    if (expanded) Modifier.fillMaxSize()
                                    else Modifier.widthIn(max = 260.dp)
                                ),
                    )
                }
            }
            if (showNowPlayingView) {
                AndroidView(
                    modifier = Modifier.fillMaxWidth(),
                    factory = { ctx ->
                        val view =
                            LayoutInflater.from(ctx).inflate(R.layout.ambient_indication, null)
                                as AmbientIndicationContainer
                        bindingHandle =
                            KeyguardAmbientIndicationAreaViewBinder.bind(
                                viewGroup = view,
                                viewModel = viewModel,
                                powerInteractor = powerInteractor,
                                keyguardUpdateMonitor = keyguardUpdateMonitor,
                                activityStarter = activityStarter,
                                delayedWakeLockFactory = delayedWakeLockFactory,
                                keyguardInteractor = keyguardInteractor,
                                configurationInteractor = configurationInteractor,
                                imageLoader = imageLoader,
                                keyguardRootViewModel = keyguardRootViewModel,
                                falsingManager = falsingManager,
                                mainDelayableExecutor = mainDelayableExecutor,
                                backgroundExecutor = backgroundExecutor,
                            )
                        view
                    },
                    onRelease = {
                        bindingHandle?.dispose()
                        bindingHandle = null
                    },
                )
            }
        }
    }

    inner class IndicationAreaElement : LockscreenElement {
        override val context: Context = this@GoogleAmbientIndicationElementProvider.context
        override val key: ElementKey = LockscreenElementKeys.AmbientIndicationArea
        override val source: ElementSource = ElementSource.VENDOR

        @Composable
        override fun LockscreenScope<ElementContentScope>.LockscreenElement() {
            AmbientIndication()
        }
    }
}
