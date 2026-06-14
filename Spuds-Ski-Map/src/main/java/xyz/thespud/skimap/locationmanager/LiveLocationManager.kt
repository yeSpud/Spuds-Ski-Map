package xyz.thespud.skimap.locationmanager

import android.content.Context
import android.util.Log
import com.google.android.gms.maps.GoogleMap
import com.google.maps.android.data.kml.KmlPolygon
import xyz.thespud.skimap.R
import xyz.thespud.skimap.mapItem.PolygonMapItem
import kotlin.jvm.Throws

class LiveLocationManager private constructor(skiAreaObjects: SkiAreaObjects, icons: CustomIcons, googleMap: GoogleMap,
                                              context: Context, drawOpaqueRuns: Boolean):
	LocationManager<PolygonMapItem>(skiAreaObjects, icons, googleMap, context, drawOpaqueRuns) {

	// Cannot be lateinit var because of a race-condition with location updates occurring before this has been set
	val skiAreaBounds: PolygonMapItem

	override fun checkIfInChairliftTerminal(): PolygonMapItem? {
		val location = currentLocation
		if (location == null) {
			Log.w("checkIfInChairliftTerminal", "Location has not been set")
			return null
		}

		val chairlift = locationInBounds(location, chairliftTerminals)
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
		if (run != null) { return checkIfLiftlineRun(run) }

		run = locationInBounds(location, blueRunBounds)
		if (run != null) { return checkIfLiftlineRun(run) }

		run = locationInBounds(location, blackRunBounds)
		if (run != null) { return checkIfLiftlineRun(run) }

		run = locationInBounds(location, doubleBlackRunBounds)
		if (run != null) { return checkIfLiftlineRun(run) }

		// Since were not in a chairlift terminal, and not on a ski run,
		// just check to see if were only on the chairlift polygon
		run = locationInBounds(location, chairliftBounds)
		if (run != null) {
			isOnChairlift = run
			return run
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

	companion object {

		@Volatile
		private var instance: LiveLocationManager? = null

		fun getInstance(skiAreaObjects: SkiAreaObjects, icons: CustomIcons, googleMap: GoogleMap, context: Context,
		                drawOpaqueRuns: Boolean): LiveLocationManager {
			return instance ?: synchronized(this) {
				instance ?: LiveLocationManager(skiAreaObjects, icons, googleMap, context, drawOpaqueRuns)
					.also { instance = it }
			}
		}

		@Throws(IllegalStateException::class)
		fun getInstance(): LiveLocationManager {
			return instance ?: throw IllegalStateException("LiveLocationManager not initialized")
		}
	}

	init {
		val placemark = parseKmlFile(googleMap, skiAreaObjects.skiAreaBounds, context).first()
		val kmlPolygon = placemark.geometry as KmlPolygon
		skiAreaBounds = PolygonMapItem(placemark, R.drawable.ic_missing, kmlPolygon.outerBoundaryCoordinates)
	}
}