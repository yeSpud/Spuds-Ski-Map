package xyz.thespud.skimap.activities

import android.annotation.SuppressLint
import android.location.Location
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.maps.android.ktx.addMarker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import xyz.thespud.skimap.R
import xyz.thespud.skimap.locationmanager.CustomIcons
import xyz.thespud.skimap.locationmanager.InfoLocationManager
import xyz.thespud.skimap.locationmanager.SkiAreaObjects
import xyz.thespud.skimap.mapItem.InfoMapMarker
import xyz.thespud.skimap.mapItem.SkiRun
import kotlin.math.roundToInt

class InfoMapActivity(val activity: ComponentActivity, view: View, cameraPosition: CameraPosition,
                      cameraBounds: LatLngBounds?, skiAreaObjects: SkiAreaObjects, icons: CustomIcons,
                      showDebug: Boolean = false): MapHandler(view, cameraPosition, cameraBounds, skiAreaObjects,
	icons, showDebug), GoogleMap.InfoWindowAdapter {

	override var locationManager: InfoLocationManager? = null

	private var runMarker: Marker? = null

	var showDots = false

	var loadedSkiRuns: List<SkiRun> = emptyList()
	private set

	override fun onMapReady(map: GoogleMap) {
		super.onMapReady(map)

		map.setOnCircleClickListener {
			Log.v("onCircleClicked", "Circle clicked!")
			map.setInfoWindowAdapter(this)

			val mapMarker = it.tag
			if (mapMarker !is InfoMapMarker) {
				val unknownClassName = if (mapMarker != null) {
					"(${mapMarker.javaClass.name})"
				} else {
					""
				}
				Log.w("onCircleClick", "Circle tag class is not a InfoMapMarker $unknownClassName")
				return@setOnCircleClickListener
			}

			val location = LatLng(mapMarker.location.latitude, mapMarker.location.longitude)

			var marker = runMarker
			if (marker == null) {
				marker = map.addMarker {
					position(location)
					icon(mapMarker.markerColor)
					title(mapMarker.mapItem.name)
					zIndex(99.0F)
					visible(true)
				}
			} else {
				marker.position = location
				marker.setIcon(mapMarker.markerColor)
				marker.title = mapMarker.mapItem.name
				marker.isVisible = true
			}

			marker!!.isVisible = true
			marker.tag = mapMarker
			marker.showInfoWindow()

			runMarker = marker
		}

		activity.lifecycleScope.launch(Dispatchers.Main) {
			locationManager = InfoLocationManager(skiAreaObjects, icons, map, activity)
		}

		map.setOnInfoWindowCloseListener { it.isVisible = false }
	}

	override fun destroy() {
		super.destroy()
		clearMap()
	}

	fun loadSkiRuns(mapMarkers: List<InfoMapMarker>) {
		val map = googleMap
		if (map == null) {
			Log.w("loadSkiRuns", "Map is not yet set up")
			return
		}

		val parsedSkiRuns = mutableListOf<SkiRun>()

		val runPoints = mutableListOf<Location>()
		var previousMapMarker: InfoMapMarker? = null
		for (mapMarker in mapMarkers) {

			// If our previous marker has a different name its likely because it's a different run,
			// so commit the run points up to this point with the previous run name and begin anew
			if (previousMapMarker != null && mapMarker.mapItem.name != previousMapMarker.mapItem.name) {
				val skiRun = SkiRun(previousMapMarker, runPoints.toList(), map)
				parsedSkiRuns.add(skiRun)

				// Reset
				runPoints.clear()
			}

			runPoints.add(mapMarker.location)
			previousMapMarker = mapMarker
		}

		// Commit the final run location
		if (previousMapMarker != null) {
			val skiRun = SkiRun(previousMapMarker, runPoints.toList(), map)
			parsedSkiRuns.add(skiRun)
		}

		loadedSkiRuns = parsedSkiRuns.toList()
	}

	@Suppress("DEPRECATION")
	fun clearMap() {
		for (skiRun in loadedSkiRuns) {
			if (skiRun.circles.isInitialized()) {
				for (circle in skiRun.circles.value) {
					circle.remove()
				}
			}

			if (skiRun.polyline.isInitialized()) {
				skiRun.polyline.value.remove()
			}
		}

		loadedSkiRuns = emptyList()
		System.gc()
	}

	override fun getInfoContents(marker: Marker): View? {
		Log.v("CustomInfoWindow", "getInfoContents called")

		val markerInfo = marker.tag
		if (markerInfo !is InfoMapMarker) {
			val unknownClassName = if (markerInfo != null) {
				"(${markerInfo.javaClass.name})"
			} else {
				""
			}
			Log.w("CustomInfoWindow", "Marker tag isn't an InfoMapMarker $unknownClassName")
			return null
		}

		val markerView: View = activity.layoutInflater.inflate(R.layout.info_window, null)
		val name: TextView = markerView.findViewById(R.id.marker_name)

		name.text = markerInfo.mapItem.name

		val altitude: TextView = markerView.findViewById(R.id.marker_altitude)

		// Convert from meters to feet.
		val altitudeConversion = 3.280839895f

		try {
			altitude.text = activity.getString(R.string.marker_altitude, (markerInfo.location.altitude * altitudeConversion).roundToInt())
		} catch (_: IllegalArgumentException) {
			altitude.text = activity.getString(R.string.marker_altitude, 0)
		}

		val speed: TextView = markerView.findViewById(R.id.marker_speed)

		// Convert from meters per second to miles per hour.
		val speedConversion = 0.44704f

		try {
			speed.text = activity.getString(R.string.marker_speed, (markerInfo.location.speed / speedConversion).roundToInt())
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