package com.google.android.systemui.keyguard.ui.sections

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.android.keyguard.KeyguardUpdateMonitor
import com.android.systemui.axdynamicbar.ui.AxDynamicBarChipViewModel
import com.android.systemui.common.ui.domain.interactor.ConfigurationInteractor
import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.dagger.qualifiers.Application
import com.android.systemui.dagger.qualifiers.Background
import com.android.systemui.dagger.qualifiers.Main
import com.android.systemui.graphics.ImageLoader
import com.android.systemui.keyguard.domain.interactor.KeyguardInteractor
import com.android.systemui.keyguard.shared.model.KeyguardSection
import com.android.systemui.keyguard.ui.viewmodel.KeyguardRootViewModel
import com.android.systemui.plugins.ActivityStarter
import com.android.systemui.plugins.FalsingManager
import com.android.systemui.power.domain.interactor.PowerInteractor
import com.android.systemui.res.R as AR
import com.android.systemui.util.concurrency.DelayableExecutor
import com.android.systemui.util.wakelock.DelayedWakeLock
import com.google.android.systemui.keyguard.ui.binder.KeyguardAmbientIndicationAreaViewBinder
import com.google.android.systemui.keyguard.ui.viewmodel.KeyguardAmbientIndicationViewModel
import com.google.android.systemui.res.R
import java.util.concurrent.Executor
import javax.inject.Inject
import kotlinx.coroutines.DisposableHandle

@SysUISingleton
class DefaultAmbientIndicationAreaSection
@Inject
constructor(
    @Application private val context: Context,
    private val keyguardAmbientIndicationViewModel: KeyguardAmbientIndicationViewModel,
    private val powerInteractor: PowerInteractor,
    private val keyguardUpdateMonitor: KeyguardUpdateMonitor,
    private val activityStarter: ActivityStarter,
    private val delayedWakeLockFactory: DelayedWakeLock.Factory,
    private val keyguardInteractor: KeyguardInteractor,
    private val configurationInteractor: ConfigurationInteractor,
    private val imageLoader: ImageLoader,
    private val keyguardRootViewModel: KeyguardRootViewModel,
    private val falsingManager: FalsingManager,
    @Main private val mainDelayableExecutor: DelayableExecutor,
    @Background private val backgroundExecutor: Executor,
    private val axDynamicBarChipViewModel: AxDynamicBarChipViewModel,
) : KeyguardSection() {

    private var ambientIndicationAreaHandle: DisposableHandle? = null

    override fun addViews(constraintLayout: ConstraintLayout) {
        val view =
            LayoutInflater.from(constraintLayout.context)
                .inflate(R.layout.ambient_indication, constraintLayout, false)
        constraintLayout.addView(view)
    }

    override fun applyConstraints(constraintSet: ConstraintSet) {
        constraintSet.constrainWidth(
            R.id.ambient_indication_container,
            ConstraintLayout.LayoutParams.MATCH_PARENT,
        )
        // A sensor in the bottom strip of the display leaves no room under it, so the container
        // sits above the icon exactly like on non-UDFPS devices.
        val belowSensor =
            keyguardUpdateMonitor.isUdfpsSupported() && !axDynamicBarChipViewModel.isLowUdfps.value
        if (belowSensor) {
            constraintSet.constrainHeight(R.id.ambient_indication_container, 0)
            // The layout's bottom margin only makes sense when sitting above the icon (non-UDFPS).
            // Under the sensor it eats the whole band on devices with a low sensor and the
            // container collapses to 0px, so lyrics/Now Playing get cut off.
            constraintSet.setMargin(R.id.ambient_indication_container, ConstraintSet.BOTTOM, 0)
            // Never collapse below the pill height; if the band is smaller, grow downward from
            // the sensor instead of being squeezed.
            constraintSet.constrainMinHeight(
                R.id.ambient_indication_container,
                context.resources.getDimensionPixelSize(R.dimen.ambient_indication_udfps_min_height),
            )
            constraintSet.setVerticalBias(R.id.ambient_indication_container, 0f)
            constraintSet.connect(
                R.id.ambient_indication_container,
                ConstraintSet.TOP,
                AR.id.device_entry_icon_view,
                ConstraintSet.BOTTOM,
            )
            constraintSet.connect(
                R.id.ambient_indication_container,
                ConstraintSet.BOTTOM,
                AR.id.keyguard_indication_area,
                ConstraintSet.TOP,
            )
            constraintSet.connect(
                R.id.ambient_indication_container,
                ConstraintSet.START,
                ConstraintSet.PARENT_ID,
                ConstraintSet.START,
            )
            constraintSet.connect(
                R.id.ambient_indication_container,
                ConstraintSet.END,
                ConstraintSet.PARENT_ID,
                ConstraintSet.END,
            )
        } else {
            constraintSet.constrainHeight(
                R.id.ambient_indication_container,
                ConstraintSet.WRAP_CONTENT,
            )
            constraintSet.connect(
                R.id.ambient_indication_container,
                ConstraintSet.BOTTOM,
                AR.id.device_entry_icon_view,
                ConstraintSet.TOP,
            )
            constraintSet.connect(
                R.id.ambient_indication_container,
                ConstraintSet.START,
                ConstraintSet.PARENT_ID,
                ConstraintSet.START,
            )
            constraintSet.connect(
                R.id.ambient_indication_container,
                ConstraintSet.END,
                ConstraintSet.PARENT_ID,
                ConstraintSet.END,
            )
        }
    }

    override fun bindData(constraintLayout: ConstraintLayout) {
        ambientIndicationAreaHandle =
            KeyguardAmbientIndicationAreaViewBinder.bind(
                viewGroup = constraintLayout,
                viewModel = keyguardAmbientIndicationViewModel,
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
    }

    override fun removeViews(constraintLayout: ConstraintLayout) {
        ambientIndicationAreaHandle?.dispose()
        ambientIndicationAreaHandle = null
        val view = constraintLayout.findViewById<View>(R.id.ambient_indication_container)
        if (view != null) {
            constraintLayout.removeView(view)
        }
    }
}
