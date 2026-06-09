package xyz.thespud.skimap.locationmanager

import android.location.Location
import com.google.maps.android.PolyUtil
import xyz.thespud.skimap.mapItem.PolygonMapItem
import xyz.thespud.skimap.mapItem.PolylineMapItem

abstract class LocationManager<T> {

	var previousLocation: Location? = null
		protected set
	var currentLocation: Location? = null
		protected set

	// Cannot be lateinit var because of a race-condition with location updates occurring before this has been set
	var skiAreaBounds: PolygonMapItem? = null

	var chairliftPolylines: List<PolylineMapItem> = emptyList()
	var greenRunPolylines: List<PolylineMapItem> = emptyList()
	var blueRunPolylines: List<PolylineMapItem> = emptyList()
	var blackRunPolylines: List<PolylineMapItem> = emptyList()
	var doubleBlackRunPolylines: List<PolylineMapItem> = emptyList()

	var otherBounds: List<PolygonMapItem> = emptyList()

	var startingChairliftTerminals: List<PolygonMapItem> = emptyList()
	var endingChairliftTerminals: List<PolygonMapItem> = emptyList()

	var greenRunBounds: List<PolygonMapItem> = emptyList()
	var blueRunBounds: List<PolygonMapItem> = emptyList()
	var blackRunBounds: List<PolygonMapItem> = emptyList()
	var doubleBlackRunBounds: List<PolygonMapItem> = emptyList()

	protected var isOnChairlift: PolygonMapItem? = null

	fun updateLocations(newLocation: Location) {
		previousLocation = currentLocation
		currentLocation = newLocation
	}

	fun locationInBounds(location: Location, bounds: List<PolygonMapItem>): PolygonMapItem? {
		for (polygon in bounds) {
			if (PolyUtil.containsLocation(location.latitude, location.longitude, polygon.points,
					true)) {
				return polygon
			}
		}

		return null
	}

	abstract fun checkIfInChairliftTerminal(): T?

	abstract fun checkIfLiftlineRun(run: T): T

	abstract fun checkIfOnRun(): T?

	abstract fun getInLocation(): T?
}