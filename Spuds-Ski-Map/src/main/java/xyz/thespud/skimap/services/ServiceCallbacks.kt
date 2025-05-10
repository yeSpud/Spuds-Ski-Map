package xyz.thespud.skimap.services

import android.location.Location
import androidx.fragment.app.FragmentActivity
import xyz.thespud.skimap.mapItem.MapMarker

interface ServiceCallbacks {
	fun isInBounds(location: Location): Boolean

	fun getOnLocation(location: Location): MapMarker?

	fun getInLocation(location: Location): MapMarker?

	fun updateMapMarker(locationString: String)

	fun setIsTracking(isTracking: Boolean)

	fun setManuallyDisabled(manuallyDisabled: Boolean)

	fun getLaunchingActivity(): FragmentActivity

	fun onLocationUpdated(location: Location)

	fun onTrackingStopped()

}