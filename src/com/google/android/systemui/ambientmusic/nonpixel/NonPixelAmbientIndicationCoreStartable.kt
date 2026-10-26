/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (C) 2026 DerpFest AOSP (ported from SystemUIFundamental)
 */
package com.google.android.systemui.ambientmusic.nonpixel

import android.app.AlarmManager
import android.content.Context
import android.content.IntentFilter
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.provider.Settings
import com.android.keyguard.KeyguardUpdateMonitor
import com.android.systemui.CoreStartable
import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.dagger.qualifiers.Application
import com.android.systemui.dagger.qualifiers.Background
import com.android.systemui.user.domain.interactor.SelectedUserInteractor
import com.google.android.systemui.keyguard.domain.interactor.AmbientIndicationInteractor
import java.util.concurrent.Executor
import javax.inject.Inject

@SysUISingleton
class NonPixelAmbientIndicationCoreStartable
@Inject
constructor(
    private val alarmManager: AlarmManager,
    @Application private val context: Context,
    private val keyguardUpdateMonitor: KeyguardUpdateMonitor,
    private val selectedUserInteractor: SelectedUserInteractor,
    private val ambientIndicationInteractor: AmbientIndicationInteractor,
    @Background private val backgroundExecutor: Executor,
    @Background private val backgroundHandler: Handler,
) : CoreStartable {

    override fun start() {
        val isPixel =
            "google".equals(android.os.Build.BRAND, ignoreCase = true) ||
                "google".equals(android.os.Build.MANUFACTURER, ignoreCase = true)

        // On Pixels, only yield to Google AmbientIndication if com.google.android.as is installed (GApps builds)
        if (isPixel) {
            val hasGoogleAs =
                try {
                    context.packageManager.getPackageInfo("com.google.android.as", 0)
                    true
                } catch (e: Exception) {
                    false
                }
            if (hasGoogleAs) {
                return
            }
        }

        val nonPixelAmbientIndicationService =
            NonPixelAmbientIndicationService(
                alarmManager = alarmManager,
                context = context,
                keyguardUpdateMonitor = keyguardUpdateMonitor,
                selectedUserInteractor = selectedUserInteractor,
                ambientIndicationInteractor = ambientIndicationInteractor,
                backgroundExecutor = backgroundExecutor,
            )
        if (nonPixelAmbientIndicationService.started) {
            return
        }
        nonPixelAmbientIndicationService.started = true
        backgroundExecutor.execute { SavedSongs.clearLegacyHistory(context) }

        val intentFilter =
            IntentFilter().apply {
                addAction(NonPixelAmbientIndicationService.ACTION_AMBIENT_INDICATION_SHOW)
                addAction(NonPixelAmbientIndicationService.ACTION_AMBIENT_INDICATION_EXPAND)
                addAction(NonPixelAmbientIndicationService.ACTION_AMBIENT_INDICATION_HIDE)
                addAction(NonPixelAmbientIndicationService.ACTION_UPDATE_QUICK_AFFORDANCE_STATE)
                addAction(NonPixelAmbientIndicationService.ACTION_LOCAL_AMBIENT_INDICATION_SHOW)
                addAction(NonPixelAmbientIndicationService.ACTION_LOCAL_AMBIENT_INDICATION_EXPAND)
                addAction(NonPixelAmbientIndicationService.ACTION_LOCAL_AMBIENT_INDICATION_HIDE)
                addAction(NonPixelAmbientIndicationService.ACTION_LOCAL_UPDATE_QUICK_AFFORDANCE_STATE)
                addAction(NonPixelAmbientIndicationService.ACTION_LOCAL_ON_DEMAND_SEARCH)
                addAction(NonPixelAmbientIndicationService.ACTION_GOOGLE_ON_DEMAND_CLICK)
                addAction(NonPixelAmbientIndicationService.ACTION_AMM_AMBIENT_INDICATION_SHOW)
                addAction(NonPixelAmbientIndicationService.ACTION_AMM_AMBIENT_INDICATION_HIDE)
            }

        context.registerReceiverAsUser(
            nonPixelAmbientIndicationService,
            UserHandle.ALL,
            intentFilter,
            null,
            null,
            Context.RECEIVER_EXPORTED,
        )
        context.registerReceiverAsUser(
            nonPixelAmbientIndicationService.savedSongReceiver,
            UserHandle.ALL,
            IntentFilter(NonPixelAmbientIndicationService.ACTION_LOCAL_TOGGLE_SAVED_SONG),
            null,
            backgroundHandler,
            Context.RECEIVER_NOT_EXPORTED,
        )
        keyguardUpdateMonitor.registerCallback(nonPixelAmbientIndicationService.callback)
        (context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager)
            ?.registerAudioPlaybackCallback(
                nonPixelAmbientIndicationService.playbackCallback,
                Handler(Looper.getMainLooper()),
            )

        val settingsObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                nonPixelAmbientIndicationService.onSettingsChanged()
            }
        }
        context.contentResolver.registerContentObserver(
            Settings.System.getUriFor("now_playing_enabled"),
            false,
            settingsObserver,
            UserHandle.USER_ALL,
        )
        context.contentResolver.registerContentObserver(
            Settings.System.getUriFor("now_playing_on_demand"),
            false,
            settingsObserver,
            UserHandle.USER_ALL,
        )

        nonPixelAmbientIndicationService.onSettingsChanged()
    }
}
