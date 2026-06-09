package xyz.thespud.skimap.locationmanager

import android.content.Context
import android.graphics.Color
import android.location.Location
import android.util.Log
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import xyz.thespud.skimap.mapItem.InfoMapMarker
import xyz.thespud.skimap.mapItem.PolygonMapItem

class InfoLocationManager(skiRuns: SkiRuns, icons: CustomIcons, googleMap: GoogleMap, context: Context):
	LocationManager<InfoMapMarker>(skiRuns, icons, googleMap, context, true) {

	fun resetLocations() {
		currentLocation = null
		previousLocation = null
		isOnChairlift = null
	}

	fun getRunMarker(polygonMapItems: List<PolygonMapItem>, location: Location,
	                         markerColor: BitmapDescriptor, color: Int): InfoMapMarker? {
		val mapItem = locationInBounds(location, polygonMapItems)
		if (mapItem != null) { return InfoMapMarker(mapItem, location, markerColor, color) }
		return null
	}

	override fun checkIfInChairliftTerminal(): InfoMapMarker? {
		val location = currentLocation
		if (location == null) {
			Log.w("checkIfInChairliftTerminal", "Location has not been set")
			return null
		}

		val chairlift = getRunMarker(chairliftTerminals, location,
			RED_MARKER, Color.RED)
		if (chairlift != null) {
			isOnChairlift = chairlift.mapItem
			return chairlift
		}

		return null
	}

	override fun checkIfLiftlineRun(run: InfoMapMarker): InfoMapMarker {
		val liftline = isOnChairlift ?: return run

		if (run.mapItem.isLiftlineRun(liftline.name)) {

			// Since this is a liftline run AND we are "on a chairlift" return that chairlift
			return InfoMapMarker(liftline, run.location, RED_MARKER, Color.RED)
		}

		// To get here we are either NOT on a chairlift,
		// OR the run were on is not in the potential liftline runs,
		// so just return the run
		Log.i("checkIfLiftlineRun", "Determined to no longer be on ${liftline.name} - resetting")
		isOnChairlift = null
		return run
	}

	override fun checkIfOnRun(): InfoMapMarker? {
		val location = currentLocation
		if (location == null) {
			Log.w("checkIfIOnRun", "Location has not been set")
			return null
		}

		var run = getRunMarker(greenRunBounds, location, GREEN_MARKER, Color.GREEN)
		if (run != null) {
			return checkIfLiftlineRun(run)
		}

		run = getRunMarker(blueRunBounds, location, BLUE_MARKER, Color.BLUE)
		if (run != null) {
			return checkIfLiftlineRun(run)
		}

		run = getRunMarker(blackRunBounds, location, BLACK_MARKER, Color.BLACK)
		if (run != null) {
			return checkIfLiftlineRun(run)
		}

		run = getRunMarker(doubleBlackRunBounds, location, BLACK_MARKER, Color.BLACK)
		if (run != null) {
			return checkIfLiftlineRun(run)
		}

		return null
	}

	override fun getInLocation(): InfoMapMarker? {
		val location = currentLocation
		if (location == null) {
			Log.w("getInLocation", "Location has not been set")
			return null
		}

		return getRunMarker(otherBounds, location, OTHER_MARKER, Color.MAGENTA)
	}

	companion object {
		val RED_MARKER = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
		val GREEN_MARKER = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
		val BLUE_MARKER = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)
		val BLACK_MARKER = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
		val OTHER_MARKER = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA)
	}
}