package xyz.thespud.skimap.dialogs

import android.content.Intent
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import xyz.thespud.skimap.R
import xyz.thespud.skimap.activities.LiveMapActivity
import xyz.thespud.skimap.services.SkierLocationService

open class LiveMapOptionsDialog(private val liveMapActivity: LiveMapActivity,
                                @LayoutRes menu: Int = R.layout.live_map_options_v2): MapOptionsDialog(
	liveMapActivity.activity.layoutInflater, menu, liveMapActivity) {

	private var locationTrackingButton: MapOptionItem? = null

	override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
		val view = super.getView(position, convertView, parent)

		if (locationTrackingButton == null) {
			val toggleLocationTracking: MapOptionItem = view.findViewById(R.id.toggle_location_tracking)
			toggleLocationTracking.setOnClickListener {
				if (liveMapActivity.isTrackingLocation) {
					liveMapActivity.setManuallyDisabled(true)

					val serviceIntent = Intent(liveMapActivity.activity, SkierLocationService::class.java)
					liveMapActivity.activity.stopService(serviceIntent)
				} else {
					liveMapActivity.setManuallyDisabled(false)
					liveMapActivity.launchLocationService()
				}
			}

			if (toggleLocationTracking.itemEnabled != liveMapActivity.isTrackingLocation) {
				toggleLocationTracking.toggleOptionVisibility()
			}

			locationTrackingButton = toggleLocationTracking
		}

		return view
	}
}