package xyz.thespud.skimap.mapItem

import android.location.Location
import androidx.annotation.DrawableRes
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.RoundCap
import com.google.maps.android.ktx.addCircle
import com.google.maps.android.ktx.addPolyline

class SkiRun(name: String, @DrawableRes icon: Int, private val color: Int, val locations: List<Location>,
             private val googleMap: GoogleMap): MapItemBase(name, icon) {

	val startTime: Long
	val endTime: Long

	val averageSpeed: Float
	val maxSpeed: Float

	val circles = lazy {
		locations.map { location ->
			googleMap.addCircle {
				center(LatLng(location.latitude, location.longitude))
				strokeColor(this@SkiRun.color)
				fillColor(this@SkiRun.color)
				clickable(true)
				radius(3.0)
				zIndex(50.0F)
			}
		}
	}

	val polyline = lazy {
		val latLngs = locations.map { location -> LatLng(location.latitude, location.longitude) }
		googleMap.addPolyline {
			addAll(latLngs)
			color(this@SkiRun.color)
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