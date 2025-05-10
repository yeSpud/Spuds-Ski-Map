package xyz.thespud.skimap.activities

import android.util.Log
import android.view.View
import android.view.ViewGroup
import xyz.thespud.skimap.R

open class LiveMapOptionsDialog(private val liveMapActivity: LiveMapActivity): MapOptionsDialog(
	liveMapActivity.activity.layoutInflater, R.layout.live_map_options_v2, liveMapActivity) {

	private var locationTrackingButton: MapOptionItem? = null

	override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
		val view = super.getView(position, convertView, parent)

		if (locationTrackingButton == null) {
			val toggleLocationTracking: MapOptionItem = view.findViewById(R.id.toggle_location_tracking)
			toggleLocationTracking.setOnClickListener {
				if (liveMapActivity.isTrackingLocation) {
					liveMapActivity.setManuallyDisabled(true)
					liveMapActivity.skierLocationService?.stopService() ?: Log.w("onClick",
						"Unable to stop location tracking")
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
