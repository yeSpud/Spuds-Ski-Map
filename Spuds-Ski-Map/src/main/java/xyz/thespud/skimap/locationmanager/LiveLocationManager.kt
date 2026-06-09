package xyz.thespud.skimap.locationmanager

import android.util.Log
import xyz.thespud.skimap.mapItem.PolygonMapItem

object LiveLocationManager: LocationManager<PolygonMapItem>() {

	override fun checkIfInChairliftTerminal(): PolygonMapItem? {
		val location = currentLocation
		if (location == null) {
			Log.w("checkIfInChairliftTerminal", "Location has not been set")
			return null
		}

		var chairlift = locationInBounds(location, startingChairliftTerminals)
		if (chairlift != null) {
			isOnChairlift = chairlift
			return chairlift
		}

		chairlift = locationInBounds(location, endingChairliftTerminals)
		if (chairlift != null) {
			isOnChairlift = chairlift
			return chairlift
		}

		return null
	}

	override fun checkIfLiftlineRun(run: PolygonMapItem): PolygonMapItem {
		val liftline = isOnChairlift ?: return run

		if (run.isLiftlineRun(liftline.name)) {

			// Since this is a liftline run AND we are "on a chairlift" return that chairlift
			return liftline
		}

		// To get here we are either NOT on a chairlift,
		// OR the run were on is not in the potential liftline runs,
		// so just return the run
		Log.i("checkIfLiftlineRun", "Determined to no longer be on ${liftline.name} - resetting")
		isOnChairlift = null
		return run
	}

	override fun checkIfOnRun(): PolygonMapItem? {
		val location = currentLocation
		if (location == null) {
			Log.w("checkIfOnRun", "Location has not been set")
			return null
		}

		var run = locationInBounds(location, greenRunBounds)
		if (run != null) {
			return checkIfLiftlineRun(run)
		}

		run = locationInBounds(location, blueRunBounds)
		if (run != null) {
			return checkIfLiftlineRun(run)
		}

		run = locationInBounds(location, blackRunBounds)
		if (run != null) {
			return checkIfLiftlineRun(run)
		}

		run = locationInBounds(location, doubleBlackRunBounds)
		if (run != null) {
			return checkIfLiftlineRun(run)
		}

		return null
	}

	override fun getInLocation(): PolygonMapItem? {
		val location = currentLocation
		if (location == null) {
			Log.w("getInLocation", "Location has not been set")
			return null
		}

		return locationInBounds(location, otherBounds)
	}
}