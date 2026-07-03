@file:Suppress("DEPRECATION")

package xyz.thespud.skimap.dialogs

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import xyz.thespud.skimap.R
import xyz.thespud.skimap.activities.LiveMapActivity
import xyz.thespud.skimap.services.SkierLocationService

class LocationTrackingToggle: MapOptionItem {

	private lateinit var liveMapActivity: LiveMapActivity

	@Deprecated("When using this constructor be sure to call setLiveMap() in your code")
	constructor(context: Context): this(context, null)
	@Deprecated("When using this constructor be sure to call setLiveMap() in your code")
	constructor(context: Context, attributeSet: AttributeSet?): this(context, attributeSet, 0)
	@Deprecated("When using this constructor be sure to call setLiveMap() in your code")
	constructor(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int) : super(context, attributeSet, defStyleAttr)

	constructor(liveMap: LiveMapActivity): super(liveMap.activity, enabledDrawable, disabledDrawable, enabledText, disabledText, itemEnabled) {

		liveMapActivity = liveMap

		setOnClickListener {

			if (liveMapActivity.isTrackingLocation) {
				liveMapActivity.setManuallyDisabled(true)

				val serviceIntent = Intent(liveMapActivity.activity, SkierLocationService::class.java)
				liveMapActivity.activity.stopService(serviceIntent)
			} else {
				liveMapActivity.setManuallyDisabled(false)
				liveMapActivity.launchLocationService()
			}

			toggleOptionVisibility()
		}
	}

	fun setLiveMap(liveMap: LiveMapActivity) { liveMapActivity = liveMap }

}