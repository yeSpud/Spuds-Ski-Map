package xyz.thespud.skimap.activities

import android.annotation.SuppressLint
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.annotation.AnyThread
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.RoundCap
import com.google.maps.android.ktx.addCircle
import com.google.maps.android.ktx.addMarker
import com.google.maps.android.ktx.addPolyline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import xyz.thespud.skimap.R
import xyz.thespud.skimap.mapItem.MapMarker
import xyz.thespud.skimap.mapItem.SkiRuns
import kotlin.math.roundToInt

abstract class InfoMapActivity(activity: FragmentActivity,
                               leftPadding: Int, topPadding: Int, rightPadding: Int, bottomPadding: Int,
                               cameraPosition: CameraPosition, cameraBounds: LatLngBounds?, skiRuns: SkiRuns):
	MapHandler(activity, leftPadding, topPadding, rightPadding, bottomPadding, cameraPosition,
		cameraBounds, skiRuns), GoogleMap.InfoWindowAdapter {

	var circles: MutableList<Circle> = mutableListOf()

	var polylines: MutableList<Polyline> = mutableListOf()

	private var runMarker: Marker? = null

	var showDots = false

	var loadedMapMarkers: Array<MapMarker> = emptyArray()

	// FIXME Not being called when overridden
	@SuppressLint("PotentialBehaviorOverride")
	override val additionalCallback: OnMapReadyCallback = OnMapReadyCallback {
		Log.v("additionalCallback", "additionalCallback called for InfoMapActivity")

		googleMap.setOnCircleClickListener {
			Log.v("onCircleClicked", "Circle clicked!")
			googleMap.setInfoWindowAdapter(this)

			val mapMarker = it.tag as MapMarker
			val location = LatLng(mapMarker.location.latitude, mapMarker.location.longitude)

			var marker = runMarker
			if (marker == null) {
				marker = googleMap.addMarker {
					position(location)
					icon(mapMarker.markerColor)
					title(mapMarker.name)
					zIndex(99.0F)
					visible(true)
				}
			} else {
				marker.position = location
				marker.setIcon(mapMarker.markerColor)
				marker.title = mapMarker.name
				marker.isVisible = true
			}

			marker!!.isVisible = true
			marker.tag = mapMarker
			marker.showInfoWindow()

			runMarker = marker
		}

		googleMap.setOnInfoWindowCloseListener { it.isVisible = false }
	}

	override fun destroy() {
		super.destroy()
		clearMap()
	}

	@AnyThread
	suspend fun addPolylinesToMap() = withContext(Dispatchers.Default) {
		Log.d("addPolylinesToMap", "Started adding polylines to map")
		var previousMapMarker: MapMarker? = null
		val polylinePoints: MutableList<LatLng> = mutableListOf()

		for (mapMarker in loadedMapMarkers) {
			val location = LatLng(mapMarker.location.latitude, mapMarker.location.longitude)
			polylinePoints.add(location)

			if (previousMapMarker != null) {
				if (previousMapMarker.color != mapMarker.color) {

					val polyline = withContext(Dispatchers.Main) {
						googleMap.addPolyline {
							addAll(polylinePoints)
							color(previousMapMarker.color)
							zIndex(10.0F)
							geodesic(true)
							startCap(RoundCap())
							endCap(RoundCap())
							clickable(false)
							width(8.0F)
							visible(true)
						}
					}
					polylines.add(polyline)
					polylinePoints.clear()
					polylinePoints.add(location)
				}
			}

			previousMapMarker = mapMarker
		}

		System.gc()
		Log.d("addPolylinesToMap", "Finished adding polylines to map")
	}

	/**
	 * WARNING: This runs on the UI thread so it'll freeze the app while adding all the circles
	 */
	@AnyThread
	suspend fun addCirclesToMap() = withContext(Dispatchers.Main) {
		Log.d("addCirclesToMap", "Started adding circles to map")
		for (mapMarker in loadedMapMarkers) {
			val location = LatLng(mapMarker.location.latitude, mapMarker.location.longitude)

			val circle = googleMap.addCircle { // FIXME this is using too much RAM & causes too much lag
				center(location)
				strokeColor(mapMarker.color)
				fillColor(mapMarker.color)
				clickable(true)
				radius(3.0)
				zIndex(50.0F)
				visible(showDots)
			}
			circle.tag = mapMarker
			circles.add(circle)
		}

		System.gc()
		Log.d("addCirclesToMap", "Finished adding circles to map")
	}

	fun removeCircles() {
		for (circle in circles) {
			circle.remove()
		}
		circles.clear()
	}

	fun clearMap() {
		removeCircles()

		for (polyline in polylines) {
			polyline.remove()
		}
		polylines.clear()
	}

	override fun getInfoContents(marker: Marker): View? {
		Log.v("CustomInfoWindow", "getInfoContents called")

		if (marker.tag !is MapMarker) {
			return null
		}

		val markerView: View = activity.layoutInflater.inflate(R.layout.info_window, null)
		val name: TextView = markerView.findViewById(R.id.marker_name)

		val markerInfo: MapMarker = marker.tag as MapMarker
		name.text = markerInfo.name

		val altitude: TextView = markerView.findViewById(R.id.marker_altitude)

		// Convert from meters to feet.
		val altitudeConversion = 3.280839895f

		try {
			altitude.text = activity.getString(R.string.marker_altitude,
				(markerInfo.location.altitude * altitudeConversion).roundToInt())
		} catch (_: IllegalArgumentException) {
			altitude.text = activity.getString(R.string.marker_altitude, 0)
		}

		val speed: TextView = markerView.findViewById(R.id.marker_speed)

		// Convert from meters per second to miles per hour.
		val speedConversion = 0.44704f

		try {
			speed.text = activity.getString(R.string.marker_speed,
				(markerInfo.location.speed / speedConversion).roundToInt())
		} catch (_: IllegalArgumentException) {
			speed.text = activity.getString(R.string.marker_speed, 0)
		}

		return markerView
	}

	override fun getInfoWindow(marker: Marker): View? {
		Log.v("InfoMapActivity", "getInfoWindow called")
		return null
	}

}