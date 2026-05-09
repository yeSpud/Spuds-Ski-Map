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


	var chairliftPolylines: List<PolylineMapItem> = emptyList()
	var greenRunPolylines: List<PolylineMapItem> = emptyList()
	var blueRunPolylines: List<PolylineMapItem> = emptyList()
	var blackRunPolylines: List<PolylineMapItem> = emptyList()
	var doubleBlackRunPolylines: List<PolylineMapItem> = emptyList()

	lateinit var skiAreaBounds: PolygonMapItem

	var otherBounds: List<PolygonMapItem> = emptyList()
	var startingChairliftTerminals: List<PolygonMapItem> = emptyList()
	var endingChairliftTerminals: List<PolygonMapItem> = emptyList()

	var greenRunBounds: List<PolygonMapItem> = emptyList()
	var blueRunBounds: List<PolygonMapItem> = emptyList()
	var blackRunBounds: List<PolygonMapItem> = emptyList()
	var doubleBlackRunBounds: List<PolygonMapItem> = emptyList()

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

	private fun isInStartingTerminal(): String? {
		val location = currentLocation
		if (location == null) {
			Log.w("isInStartingTerminal", "Location has not been set")
			return null
		}

		for (startingChairlift in startingChairliftTerminals) {
			if (PolyUtil.containsLocation(location.latitude, location.longitude,
					startingChairlift.points, true)) {
				return startingChairlift.name
			}
		}

		return null
	}

	private fun isInEndingTerminal(): String? {
		val location = currentLocation
		if (location == null) {
			Log.w("isInEndingTerminal", "Location has not been set")
			return null
		}

		for (endingChairlift in endingChairliftTerminals) {
			if (PolyUtil.containsLocation(location.latitude, location.longitude,
					endingChairlift.points, true)) {
				return endingChairlift.name
			}
		}

		return null
	}

	fun checkIfIOnChairlift(): MapMarker? {
		val location = currentLocation
		if (location == null) {
			Log.w("checkIfIOnChairlift", "Location has not been set")
			return null
		}

		val startingTerminal = isInStartingTerminal()
		if (startingTerminal != null) {
			isOnChairlift = startingTerminal
			return MapMarker(startingTerminal, location, chairliftIcon,
				BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED), Color.RED)
		}

		val endingTerminal = isInEndingTerminal()
		if (endingTerminal != null) {
			isOnChairlift = null
			return MapMarker(endingTerminal, location, chairliftIcon,
				BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED), Color.RED)
		}

		if (isOnChairlift != null) {

			val liftlineRun = checkIfOnRun()
			if (liftlineRun != null) {

				val allRuns = mutableListOf<PolygonMapItem>()
				allRuns.addAll(greenRunBounds)
				allRuns.addAll(blueRunBounds)
				allRuns.addAll( blueRunBounds)
				allRuns.addAll(doubleBlackRunBounds)

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
				isOnChairlift = null
			}
		}

		return null
	}

	fun checkIfOnRun(): MapMarker? {
		val location = currentLocation
		if (location == null) {
			Log.w("checkIfOnRun", "Location has not been set")
			return null
		}

		val greenRun = getRunMarker(location, greenRunBounds, greenIcon,
			BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN),
			Color.GREEN)
		if (greenRun != null) {
			return greenRun
		}

		val blueRun = getRunMarker(location, blueRunBounds, blueIcon,
			BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE),
			Color.BLUE)
		if (blueRun != null) {
			return blueRun
		}

		val blackRun = getRunMarker(location, blackRunBounds, blackIcon,
			BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE),
			Color.BLACK)
		if (blackRun != null) {
			return blackRun
		}

		val doubleBlackRun = getRunMarker(location, doubleBlackRunBounds, doubleBlackIcon,
			BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE),
			Color.BLACK)
		if (doubleBlackRun != null) {
			return doubleBlackRun
		}

		return null
	}

	fun getOnLocation(): MapMarker? {
		var mapMarker = checkIfIOnChairlift()
		if (mapMarker != null) {
			return mapMarker
		}

		mapMarker = checkIfOnRun()
		if (mapMarker != null) {
			return mapMarker
		}

		return null
	}

	fun getInLocation(): MapMarker? {
		val location = currentLocation
		if (location == null) {
			Log.w("getInLocation", "Location has not been set")
			return null
		}

		for (other: PolygonMapItem in otherBounds) {
			if (PolyUtil.containsLocation(location.latitude, location.longitude,
					other.points, true)) {
				return MapMarker(other.name, location, other.icon,
					BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA),
					Color.MAGENTA)
			}
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
