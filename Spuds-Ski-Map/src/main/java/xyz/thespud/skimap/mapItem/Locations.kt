package xyz.thespud.skimap.mapItem

import android.graphics.Color
import android.location.Location
import android.util.Log
import androidx.annotation.DrawableRes
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.maps.android.PolyUtil
import xyz.thespud.skimap.R
import xyz.thespud.skimap.activities.MapHandler

object Locations {

	var previousLocation: Location? = null
		private set
	var currentLocation: Location? = null
		private set

	@DrawableRes
	var chairliftIcon: Int = R.drawable.ic_chairlift
	@DrawableRes
	var greenIcon: Int = R.drawable.ic_green
	@DrawableRes
	var blueIcon: Int = R.drawable.ic_blue
	@DrawableRes
	var blackIcon: Int = R.drawable.ic_black
	@DrawableRes
	var doubleBlackIcon: Int = R.drawable.ic_missing // FIXME I need an icon ;w;

	private var isOnChairlift: String? = null

	// TODO Add helper function to convert from resource color to actual color

	fun updateLocations(newLocation: Location) {
		previousLocation = currentLocation
		currentLocation = newLocation
	}

	private fun isInStartingTerminal(map: MapHandler): String? {
		val location = currentLocation
		if (location == null) {
			Log.w("isInStartingTerminal", "Other map items have not been set up")
			return null
		}

		for (startingChairlift in map.startingChairliftTerminals) {
			if (PolyUtil.containsLocation(location.latitude, location.longitude,
					startingChairlift.points, true)) {
				return startingChairlift.name
			}
		}

		return null
	}

	private fun isInEndingTerminal(map: MapHandler): String? {
		val location = currentLocation
		if (location == null) {
			Log.w("isInEndingTerminal", "Other map items have not been set up")
			return null
		}

		for (endingChairlift in map.endingChairliftTerminals) {
			if (PolyUtil.containsLocation(location.latitude, location.longitude,
					endingChairlift.points, true)) {
				return endingChairlift.name
			}
		}

		return null
	}

	fun checkIfIOnChairlift(map: MapHandler): MapMarker? {
		val location = currentLocation
		if (location == null) {
			Log.w("checkIfIOnChairlift", "Chairlifts have not been set up")
			return null
		}

		val startingTerminal = isInStartingTerminal(map)
		if (startingTerminal != null) {
			isOnChairlift = startingTerminal
			return MapMarker(startingTerminal, location, chairliftIcon,
				BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED), Color.RED)
		}

		val endingTerminal = isInEndingTerminal(map)
		if (endingTerminal != null) {
			isOnChairlift = null
			return MapMarker(endingTerminal, location, chairliftIcon,
				BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED), Color.RED)
		}

		if (isOnChairlift != null) {

			val liftlineRun = checkIfOnRun(map)
			if (liftlineRun != null) {

				val allRuns = mutableListOf<PolygonMapItem>()
				allRuns.addAll(map.greenRunBounds)
				allRuns.addAll(map.blueRunBounds)
				allRuns.addAll( map.blueRunBounds)
				allRuns.addAll(map.doubleBlackRunBounds)

				for (runs in allRuns) {
					val liftlineRuns = runs.metadata[PolygonMapItem.LIFTLINE_RUN_KEY]
					if (liftlineRuns != null) {
						for (liftlineRun in liftlineRuns as List<*>) {
							if (liftlineRun == isOnChairlift!!) {
								return MapMarker(isOnChairlift!!, location, chairliftIcon,
									BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED),
									Color.RED)
							}
						}
					}
				}
			}
		}

		return null
	}

	fun checkIfOnRun(map: MapHandler): MapMarker? {
		val location = currentLocation
		if (location == null) {
			Log.w("checkIfOnRun", "Ski runs have not been set up")
			return null
		}

		val greenRun = getRunMarker(location, map.greenRunBounds, greenIcon,
			BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN),
			Color.GREEN)
		if (greenRun != null) {
			return greenRun
		}

		val blueRun = getRunMarker(location, map.blueRunBounds, blueIcon,
			BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE),
			Color.BLUE)
		if (blueRun != null) {
			return blueRun
		}

		val blackRun = getRunMarker(location, map.blackRunBounds, blackIcon,
			BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE),
			Color.BLACK)
		if (blackRun != null) {
			return blackRun
		}

		val doubleBlackRun = getRunMarker(location, map.doubleBlackRunBounds, doubleBlackIcon,
			BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE),
			Color.BLACK)
		if (doubleBlackRun != null) {
			return doubleBlackRun
		}

		return null
	}

	private fun getRunMarker(location: Location, polygonMapItems: List<PolygonMapItem>, icon: Int,
	                         markerColor: BitmapDescriptor, color: Int): MapMarker? {
		for (polygonMapItem in polygonMapItems) {
			if (PolyUtil.containsLocation(location.latitude, location.longitude,
					polygonMapItem.points, true)) {
				Log.d("getRunMarker", "On run ${polygonMapItem.name}")
				isOnChairlift = null
				return MapMarker(polygonMapItem.name, location, icon, markerColor, color)
			}
		}
		return null
	}

	fun resetLocations() {
		currentLocation = null
		previousLocation = null
		isOnChairlift = null
	}
}
