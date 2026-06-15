package xyz.thespud.skimap.mapItem

import android.location.Location
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.RoundCap
import com.google.maps.android.ktx.addCircle
import com.google.maps.android.ktx.addPolyline

class SkiRun(private val infoMapMarker: InfoMapMarker, val locations: List<Location>, private val googleMap: GoogleMap): MapItemBase(infoMapMarker.mapItem.name, infoMapMarker.mapItem.icon) {

	val startTime: Long
	val endTime: Long

	val averageSpeed: Float
	val maxSpeed: Float

	// fixme this is using too much RAM & causes too much lag
	@Deprecated("Adding circles to the map yields high memory usage - consider not using it")
	val circles = lazy {
		locations.map { location ->
			val circle = googleMap.addCircle {
				center(LatLng(location.latitude, location.longitude))
				strokeColor(infoMapMarker.color)
				fillColor(infoMapMarker.color)
				clickable(true)
				radius(3.0)
				zIndex(50.0F)
			}

			circle.tag = infoMapMarker
			circle
		}
	}

	val polyline = lazy {
		val latLngs = locations.map { location -> LatLng(location.latitude, location.longitude) }
		googleMap.addPolyline {
			addAll(latLngs)
			color(infoMapMarker.color)
			zIndex(10.0F)
			geodesic(true)
			startCap(RoundCap())
			endCap(RoundCap())
			clickable(false)
			width(8.0F)
		}
	}

	init {

		var localStartTime: Long = Long.MAX_VALUE
		var localEndTime: Long = Long.MIN_VALUE

		var speedSum: Float = 0.0F
		var localMaxSpeed: Float = 0.0F

		for (location in locations) {
			if (location.time < localStartTime) {
				localStartTime = location.time
			}

			if (location.time > localEndTime) {
				localEndTime = location.time
			}


			if (location.speed > localMaxSpeed) {
				localMaxSpeed = location.speed
			}
			speedSum+=location.speed
		}

		startTime = localStartTime
		endTime = localEndTime
		averageSpeed = speedSum / locations.size
		maxSpeed = localMaxSpeed
	}
}