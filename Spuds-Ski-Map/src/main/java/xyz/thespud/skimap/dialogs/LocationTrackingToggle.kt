@file:Suppress("DEPRECATION")

package xyz.thespud.skimap.dialogs

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import androidx.appcompat.content.res.AppCompatResources
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

	constructor(liveMap: LiveMapActivity): super(liveMap.activity,
		AppCompatResources.getDrawable(liveMap.activity, R.drawable.ic_pause)!!,
		AppCompatResources.getDrawable(liveMap.activity, R.drawable.ic_play)!!,
		liveMap.activity.getString(R.string.stop_tracking),
		liveMap.activity.getString(R.string.start_tracking), true) {

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