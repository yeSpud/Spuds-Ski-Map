package xyz.thespud.skimap.mapItem

import android.graphics.Color
import android.location.Location
import android.util.Log
import androidx.annotation.DrawableRes
import com.google.android.gms.maps.model.BitmapDescriptorFactory
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

	fun checkIfOnOther(map: MapHandler): MapMarker? {
		val location = currentLocation
		if (location == null) {
			Log.w("checkIfOnOther", "Other map items have not been set up")
			return null
		}

		for (other in map.otherBounds) {
			if (other.locationInsidePoints(location)) {

				return when (other.icon) {
					/*
					R.drawable.ic_parking -> MapMarker(other.name, location, other.icon,
						BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA),
						Color.GRAY)
					R.drawable.ic_ski_patrol_icon -> MapMarker(other.name, location, other.icon,
						BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA),
						Color.WHITE)
					R.drawable.ic_ski_school -> MapMarker(other.name, location, other.icon,
						BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA),
						Color.GRAY)
					R.drawable.ic_chairlift -> MapMarker(other.name, location, other.icon,
						BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA),
						Color.TRANSPARENT)*/ // FIXME How should I be set?
					else -> MapMarker(other.name, location, other.icon ?: R.drawable.ic_missing,
						BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA),
						Color.MAGENTA)
				}
			}
		}

		return null
	}

	private fun isInStartingTerminal(map: MapHandler): String? {
		for (startingChairlift in map.startingChairliftTerminals) {
			if (startingChairlift.locationInsidePoints(currentLocation!!)) {
				return startingChairlift.name
			}
		}

		return null
	}

	private fun isInEndingTerminal(map: MapHandler): String? {
		for (endingChairlift in map.endingChairliftTerminals) {
			if (endingChairlift.locationInsidePoints(currentLocation!!)) {
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

		var name: String? = null

		val startingTerminal = isInStartingTerminal(map)
		if (startingTerminal != null) {
			isOnChairlift = startingTerminal
			name = startingTerminal
		}

		val endingTerminal = isInEndingTerminal(map)
		if (endingTerminal != null) {
			isOnChairlift = null
			name = endingTerminal
		}

		if (isOnChairlift != null) {
			name = isOnChairlift!!
		}

		if (name == null) {
			return null
		}

		return MapMarker(name, location, chairliftIcon,
			BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED), Color.RED)
	}

	fun checkIfOnRun(map: MapHandler): MapMarker? {
		val location = currentLocation
		if (location == null) {
			Log.w("checkIfOnRun", "Ski runs have not been set up")
			return null
		}

		for (greenRun in map.greenRunBounds) {
			if (greenRun.locationInsidePoints(location)) {
				return MapMarker(greenRun.name, location, greenIcon,
					BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN),
					Color.GREEN)
			}
		}

		for (blueRun in map.blueRunBounds) {
			if (blueRun.locationInsidePoints(location)) {
				return MapMarker(blueRun.name, location, blueIcon,
					BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE),
					Color.BLUE)
			}
		}

		for (blackRun in map.blackRunBounds) {
			if (blackRun.locationInsidePoints(location)) {
				return MapMarker(blackRun.name, location, blackIcon,
					BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE),
					Color.BLACK)
			}
		}

		for (doubleBlackRun in map.doubleBlackRunBounds) {
			if (doubleBlackRun.locationInsidePoints(location)) {
				return MapMarker(doubleBlackRun.name, location, doubleBlackIcon,
					BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE),
					Color.BLACK)
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
