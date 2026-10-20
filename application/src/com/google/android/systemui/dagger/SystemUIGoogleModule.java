/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.systemui.dagger;

import com.android.systemui.dagger.SysUISingleton;

import static com.android.systemui.Dependency.ALLOW_NOTIFICATION_LONG_PRESS_NAME;
import static com.android.systemui.Dependency.LEAK_REPORT_EMAIL_NAME;

import android.content.Context;
import android.hardware.SensorPrivacyManager;

import com.android.keyguard.KeyguardViewController;
import com.android.systemui.CoreStartable;
import com.android.systemui.Flags;
import com.android.systemui.ScreenDecorationsModule;
import com.android.systemui.accessibility.AccessibilityModule;
import com.android.systemui.accessibility.SystemActionsModule;
import com.android.systemui.accessibility.data.repository.AccessibilityRepositoryModule;
import com.android.systemui.actioncorner.ActionCornerModule;
import com.android.systemui.clipboardoverlay.dagger.ClipboardOverlayOverrideModule;
import com.android.systemui.communal.posturing.dagger.PosturingModule;
import com.android.systemui.contextualcursor.ContextualCursorModule;
import com.android.systemui.cursorposition.CursorPositionModule;
import com.android.systemui.display.dagger.ReferenceSysUIDisplaySubcomponent;
import com.android.systemui.display.dagger.SystemUIDisplaySubcomponent;
import com.android.systemui.display.data.repository.DisplayPhoneModule;
import com.android.systemui.display.ui.viewmodel.ConnectingDisplayViewModel;
import com.android.systemui.dock.DockManager;
import com.android.systemui.dock.DockManagerImpl;
import com.android.systemui.doze.DozeHost;
import com.android.systemui.dreams.suppression.dagger.NoOpActivityRecognitionModule;
import com.android.systemui.education.dagger.ContextualEducationModule;
import com.android.systemui.emergency.EmergencyGestureModule;
import com.android.systemui.flags.FeatureFlags;
import com.android.systemui.inputdevice.tutorial.KeyboardTouchpadTutorialModule;
import com.android.systemui.inputmethod.ImeSwitcherMenuModule;
import com.android.systemui.keyguard.shared.model.KeyguardSection;
import com.android.systemui.keyboard.shortcut.ShortcutHelperModule;
import com.android.systemui.keyguard.dagger.KeyguardModule;
import com.android.systemui.keyguard.data.quickaffordance.KeyguardQuickAffordanceConfig;
import com.android.systemui.keyguard.domain.interactor.KeyguardInteractor;
import com.android.systemui.keyguard.ui.view.layout.blueprints.KeyguardBlueprintModule;
import com.android.systemui.keyguard.ui.view.layout.sections.KeyguardSectionsModule;
import com.android.systemui.lowlight.dagger.ScreenAwareLightModeMonitorModule;
import com.android.systemui.media.dagger.MediaModule;
import com.android.systemui.media.muteawait.MediaMuteAwaitConnectionCli;
import com.android.systemui.media.nearby.NearbyMediaDevicesManager;
import com.android.systemui.minmode.MinModeManager;
import com.android.systemui.minmode.MinModeManagerImpl;
import com.android.systemui.navigationbar.NavigationBarControllerModule;
import com.android.systemui.plugins.BcSmartspaceDataPlugin;
import com.android.systemui.plugins.keyguard.ui.composable.elements.LockscreenElementProvider;
import com.android.systemui.plugins.qs.QSFactory;
import com.android.systemui.qs.QSFragmentStartableModule;
import com.android.systemui.qs.dagger.QSModule;
import com.android.systemui.qs.tileimpl.QSFactoryImpl;
import com.android.systemui.reardisplay.RearDisplayModule;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.RecentsImplementation;
import com.android.systemui.recents.RecentsModule;
import com.android.systemui.rotationlock.RotationLockModule;
import com.android.systemui.rotationlock.RotationLockNewModule;
import com.android.systemui.scene.SceneContainerFrameworkModule;
import com.android.systemui.screencapture.common.ScreenCaptureModule;
import com.android.systemui.screenshot.ReferenceScreenshotModule;
import com.android.systemui.settings.MultiUserUtilsModule;
import com.android.systemui.settings.UserTracker;
import com.android.systemui.shade.NotificationShadeWindowControllerImpl;
import com.android.systemui.shade.ShadeModule;
import com.android.systemui.smartspace.config.BcSmartspaceConfigProvider;
import com.android.systemui.smartspace.dagger.SmartspaceModule;
import com.android.systemui.startable.Dependencies;
import com.android.systemui.statusbar.CommandQueue;
import com.android.systemui.statusbar.NotificationLockscreenUserManager;
import com.android.systemui.statusbar.NotificationLockscreenUserManagerImpl;
import com.android.systemui.statusbar.NotificationShadeWindowController;
import com.android.systemui.statusbar.SysuiStatusBarStateController;
import com.android.systemui.statusbar.dagger.CentralSurfacesModule;
import com.android.systemui.statusbar.dagger.StartCentralSurfacesModule;
import com.android.systemui.statusbar.notification.dagger.ReferenceNotificationsModule;
import com.android.systemui.statusbar.notification.headsup.HeadsUpModule;
import com.android.systemui.statusbar.phone.CentralSurfaces;
import com.android.systemui.statusbar.phone.DozeServiceHost;
import com.android.systemui.statusbar.phone.StatusBarKeyguardViewManager;
import com.android.systemui.statusbar.phone.dagger.StatusBarPhoneModule;
import com.android.systemui.statusbar.policy.AospPolicyModule;
import com.android.systemui.statusbar.policy.DeviceProvisionedController;
import com.android.systemui.statusbar.policy.DeviceProvisionedControllerImpl;
import com.android.systemui.statusbar.policy.IndividualSensorPrivacyController;
import com.android.systemui.statusbar.policy.IndividualSensorPrivacyControllerImpl;
import com.android.systemui.statusbar.policy.SensorPrivacyController;
import com.android.systemui.statusbar.policy.SensorPrivacyControllerImpl;
import com.android.systemui.statusbar.policy.dagger.SmartRepliesInflationModule;
import com.android.systemui.theme.ThemeOverlayController;
import com.android.systemui.toast.ToastModule;
import com.android.systemui.topwindoweffects.dagger.TopLevelWindowEffectsModule;
import com.android.systemui.touchpad.tutorial.TouchpadTutorialModule;
import com.android.systemui.unfold.SysUIUnfoldStartableModule;
import com.android.systemui.unfold.UnfoldTransitionModule;
import com.android.systemui.util.kotlin.SysUICoroutinesModule;
import com.android.systemui.volume.dagger.VolumeModule;
import com.android.systemui.wallpapers.dagger.WallpaperModule;

import com.google.android.systemui.battery.BatterySaverModuleGoogle;
import com.google.android.systemui.gesture.GestureModuleGoogle;
import com.google.android.systemui.ambientmusic.nonpixel.NonPixelAmbientIndicationCoreStartable;
import com.google.android.systemui.keyguard.AmbientIndicationCoreStartable;
import com.google.android.systemui.keyguard.data.quickaffordance.CalculatorQuickAffordanceConfig;
import com.google.android.systemui.keyguard.data.quickaffordance.CalendarQuickAffordanceConfig;
import com.google.android.systemui.keyguard.data.quickaffordance.NowPlayingQuickAffordanceConfig;
import com.google.android.systemui.keyguard.data.repository.AmbientIndicationRepository;
import com.google.android.systemui.keyguard.domain.interactor.AmbientIndicationInteractor;
import com.google.android.systemui.keyguard.ui.composable.elements.GoogleAmbientIndicationElementProvider;
import com.google.android.systemui.keyguard.ui.sections.DefaultAmbientIndicationAreaSection;
import com.google.android.systemui.power.dagger.PowerModuleGoogle;
import com.google.android.systemui.smartspace.BcSmartspaceDataProvider;
import com.google.android.systemui.smartspace.DateSmartspaceDataProvider;
import com.google.android.systemui.smartspace.KeyguardSmartspaceStartable;
import com.google.android.systemui.smartspace.WeatherSmartspaceDataProvider;
import com.google.android.systemui.smartspace.dagger.SmartspaceGoogleModule;
import com.google.android.systemui.theme.ThemeOverlayControllerGoogle;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.ClassKey;
import dagger.multibindings.IntoMap;
import dagger.multibindings.IntoSet;

import java.util.Optional;
import java.util.Set;

import javax.inject.Named;
import javax.inject.Provider;

/**
 * A dagger module for injecting default implementations of components of System UI.
 *
 * Variants of SystemUI should make a copy of this, include it in their component, and customize it
 * as needed.
 *
 * This module might alternatively be named `AospSystemUIModule`, `PhoneSystemUIModule`,
 * or `BasicSystemUIModule`.
 *
 * Nothing in the module should be strictly required. Each piece should either be swappable with
 * a different implementation or entirely removable.
 *
 * This is different from {@link SystemUIModule} which should be used for pieces of required
 * SystemUI code that variants of SystemUI _must_ include to function correctly.
 */
@Module(includes = {
        AccessibilityModule.class,
        AccessibilityRepositoryModule.class,
        AospPolicyModule.class,
        BatterySaverModuleGoogle.class,
        CentralSurfacesModule.class,
        ClipboardOverlayOverrideModule.class,
        ConnectingDisplayViewModel.StartableModule.class,
        ImeSwitcherMenuModule.class,
        DisplayPhoneModule.class,
        EmergencyGestureModule.class,
        GestureModuleGoogle.class,
        HeadsUpModule.class,
        KeyguardModule.class,
        KeyguardBlueprintModule.class,
        KeyguardSectionsModule.class,
        KeyboardTouchpadTutorialModule.class,
        MediaModule.class,
        MediaMuteAwaitConnectionCli.StartableModule.class,
        MultiUserUtilsModule.class,
        NavigationBarControllerModule.class,
        NearbyMediaDevicesManager.StartableModule.class,
        NoOpActivityRecognitionModule.class,
        PowerModuleGoogle.class,
        QSFragmentStartableModule.class,
        QSModule.class,
        RearDisplayModule.class,
        RecentsModule.class,
        ReferenceNotificationsModule.class,
        PosturingModule.class,
        ReferenceScreenshotModule.class,
        RotationLockModule.class,
        RotationLockNewModule.class,
        ScreenCaptureModule.class,
        ScreenDecorationsModule.class,
        StatusBarPhoneModule.class,
        SystemActionsModule.class,
        ShadeModule.class,
        StartCentralSurfacesModule.class,
        SceneContainerFrameworkModule.class,
        ScreenAwareLightModeMonitorModule.class,
        SysUICoroutinesModule.class,
        SysUIUnfoldStartableModule.class,
        UnfoldTransitionModule.Startables.class,
        ToastModule.class,
        TopLevelWindowEffectsModule.class,
        TouchpadTutorialModule.class,
        VolumeModule.class,
        WallpaperModule.class,
        ShortcutHelperModule.class,
        SmartRepliesInflationModule.class,
        ContextualEducationModule.class,
        ActionCornerModule.class,
        CursorPositionModule.class,
        ContextualCursorModule.class,
        SmartspaceGoogleModule.class,
}, subcomponents = {
        ReferenceSysUIDisplaySubcomponent.class
})
public abstract class SystemUIGoogleModule {

    @Binds
    abstract SystemUIDisplaySubcomponent.Factory systemUIDisplaySubcomponentFactory(
            ReferenceSysUIDisplaySubcomponent.Factory factory);

    @SysUISingleton
    @Provides
    @Named(LEAK_REPORT_EMAIL_NAME)
    static String provideLeakReportEmail() {
        return "";
    }

    @Binds
    abstract NotificationLockscreenUserManager bindNotificationLockscreenUserManager(
            NotificationLockscreenUserManagerImpl notificationLockscreenUserManager);

    @Provides
    @SysUISingleton
    static SensorPrivacyController provideSensorPrivacyController(
            SensorPrivacyManager sensorPrivacyManager) {
        SensorPrivacyController spC = new SensorPrivacyControllerImpl(sensorPrivacyManager);
        spC.init();
        return spC;
    }

    @Provides
    @SysUISingleton
    static IndividualSensorPrivacyController provideIndividualSensorPrivacyController(
            SensorPrivacyManager sensorPrivacyManager, UserTracker userTracker) {
        IndividualSensorPrivacyController spC = new IndividualSensorPrivacyControllerImpl(
                sensorPrivacyManager, userTracker);
        spC.init();
        return spC;
    }

    /**  */
    @Binds
    @SysUISingleton
    public abstract QSFactory bindQSFactory(QSFactoryImpl qsFactoryImpl);

    @Binds
    abstract DockManager bindDockManager(DockManagerImpl dockManager);

    @Provides
    @SysUISingleton
    static Optional<MinModeManager> bindMinModeManager(
            Provider<MinModeManagerImpl> minModeManager) {
        if (Flags.enableMinmode()) {
            return Optional.of(minModeManager.get());
        } else {
            return Optional.empty();
        }
    }

    @SysUISingleton
    @Provides
    @Named(ALLOW_NOTIFICATION_LONG_PRESS_NAME)
    static boolean provideAllowNotificationLongPress() {
        return true;
    }

    @Provides
    @SysUISingleton
    static Recents provideRecents(Context context, RecentsImplementation recentsImplementation,
            CommandQueue commandQueue) {
        return new Recents(context, recentsImplementation, commandQueue);
    }

    @SysUISingleton
    @Provides
    static DeviceProvisionedController bindDeviceProvisionedController(
            DeviceProvisionedControllerImpl deviceProvisionedController) {
        deviceProvisionedController.init();
        return deviceProvisionedController;
    }

    @Binds
    abstract KeyguardViewController bindKeyguardViewController(
            StatusBarKeyguardViewManager statusBarKeyguardViewManager);

    @Binds
    abstract NotificationShadeWindowController bindNotificationShadeController(
            NotificationShadeWindowControllerImpl notificationShadeWindowController);

    @Binds
    abstract DozeHost provideDozeHost(DozeServiceHost dozeServiceHost);

    /**  */
    @Provides
    @IntoMap
    @Dependencies
    @ClassKey(SysuiStatusBarStateController.class)
    static Set<Class<? extends CoreStartable>> providesStatusBarStateControllerDeps() {
        return Set.of(CentralSurfaces.class);
    }

    @Provides
    @SysUISingleton
    static BcSmartspaceDataPlugin provideBcSmartspaceDataPlugin() {
        return new BcSmartspaceDataProvider();
    }

    @Provides
    @SysUISingleton
    @Named(SmartspaceModule.DATE_SMARTSPACE_DATA_PLUGIN)
    static BcSmartspaceDataPlugin provideDateSmartspaceDataPlugin() {
        return new DateSmartspaceDataProvider();
    }

    @Provides
    @SysUISingleton
    @Named(SmartspaceModule.WEATHER_SMARTSPACE_DATA_PLUGIN)
    static BcSmartspaceDataPlugin provideWeatherSmartspaceDataPlugin() {
        return new WeatherSmartspaceDataProvider();
    }

    @Provides
    @SysUISingleton
    static BcSmartspaceConfigProvider provideBcSmartspaceConfigPlugin(FeatureFlags featureFlags) {
        return new BcSmartspaceConfigProvider(featureFlags);
    }

    @Binds
    @IntoMap
    @ClassKey(KeyguardSmartspaceStartable.class)
    abstract CoreStartable bindKeyguardSmartspaceStartable(
            KeyguardSmartspaceStartable startable);

    @Binds
    @IntoMap
    @ClassKey(AmbientIndicationCoreStartable.class)
    abstract CoreStartable bindAmbientIndicationCoreStartable(
            AmbientIndicationCoreStartable impl);

    @Binds
    @IntoMap
    @ClassKey(NonPixelAmbientIndicationCoreStartable.class)
    abstract CoreStartable bindNonPixelAmbientIndicationCoreStartable(
            NonPixelAmbientIndicationCoreStartable impl);

    @Provides
    @SysUISingleton
    static AmbientIndicationRepository provideAmbientIndicationRepository() {
        return new AmbientIndicationRepository();
    }

    @Binds
    @Named(KeyguardSectionsModule.KEYGUARD_AMBIENT_INDICATION_AREA_SECTION)
    abstract KeyguardSection bindDefaultAmbientIndicationAreaSection(
            DefaultAmbientIndicationAreaSection impl);

    @Binds
    @IntoSet
    abstract LockscreenElementProvider bindGoogleAmbientIndicationElementProvider(
            GoogleAmbientIndicationElementProvider impl);

    @Binds
    @IntoSet
    abstract KeyguardQuickAffordanceConfig bindNowPlayingQuickAffordanceConfig(
            NowPlayingQuickAffordanceConfig impl);

    @Binds
    @IntoSet
    abstract KeyguardQuickAffordanceConfig bindCalendarQuickAffordanceConfig(
            CalendarQuickAffordanceConfig impl);

    @Binds
    @IntoSet
    abstract KeyguardQuickAffordanceConfig bindCalculatorQuickAffordanceConfig(
            CalculatorQuickAffordanceConfig impl);

    @Binds
    abstract ThemeOverlayController bindThemeOverlayController(
            ThemeOverlayControllerGoogle impl);
}
