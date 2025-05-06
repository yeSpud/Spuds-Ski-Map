package xyz.thespud.skimap.activities

import android.annotation.SuppressLint
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.annotation.AnyThread
import androidx.annotation.RawRes
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.Polyline
import com.google.maps.android.ktx.addCircle
import com.google.maps.android.ktx.addMarker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import xyz.thespud.skimap.R
import xyz.thespud.skimap.mapItem.MapMarker
import kotlin.math.roundToInt

abstract class InfoMapActivity(
	activity: FragmentActivity, val leftPadding: Int, val topPadding: Int, val rightPadding: Int,
	val bottomPadding: Int, cameraBounds: LatLngBounds, @RawRes lifts: Int, @RawRes green: Int,
	@RawRes blue: Int, @RawRes black: Int, @RawRes doubleBlack: Int, @RawRes starting_lifts_bounds: Int,
	@RawRes ending_lifts_bounds: Int, @RawRes green_polygons: Int, @RawRes blue_polygons: Int,
	@RawRes black_polygons: Int, @RawRes double_black_polygons: Int, @RawRes other: Int): MapHandler(
	activity, cameraBounds, lifts, green, blue, black, doubleBlack, starting_lifts_bounds, ending_lifts_bounds,
	green_polygons, blue_polygons, black_polygons, double_black_polygons, other), GoogleMap.InfoWindowAdapter {

	var circles: MutableList<Circle> = mutableListOf()

	var polylines: MutableList<Polyline> = mutableListOf()

	private var runMarker: Marker? = null

	var showDots = false

	var loadedMapMarkers: Array<MapMarker> = emptyArray()

	@SuppressLint("PotentialBehaviorOverride")
	override val additionalCallback: OnMapReadyCallback = OnMapReadyCallback {

		googleMap.setOnCircleClickListener {

			googleMap.setInfoWindowAdapter(this)

			val mapMarker = it.tag as MapMarker
			val location = LatLng(mapMarker.location.latitude, mapMarker.location.longitude)

			if (runMarker == null) {
				runMarker = googleMap.addMarker {
					position(location)
					icon(mapMarker.markerColor)
					title(mapMarker.name)
					zIndex(99.0F)
					visible(true)
				}
			} else {
				runMarker!!.position = location
				runMarker!!.setIcon(mapMarker.markerColor)
				runMarker!!.title = mapMarker.name
				runMarker!!.isVisible = true
			}

			runMarker!!.isVisible = true
			runMarker!!.tag = mapMarker
			runMarker!!.showInfoWindow()
		}

		googleMap.setOnInfoWindowCloseListener { it.isVisible = false }

		// The top and left padding are useless to us
		// because the map is always at the bottom while in portrait and on the right in landscape
		googleMap.setPadding(leftPadding, topPadding, rightPadding, bottomPadding)
	}

	override fun destroy() {
		super.destroy()
		clearMap()
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
		} catch (e: IllegalArgumentException) {
			altitude.text = activity.getString(R.string.marker_altitude, 0)
		}

		val speed: TextView = markerView.findViewById(R.id.marker_speed)

		// Convert from meters per second to miles per hour.
		val speedConversion = 0.44704f

		try {
			speed.text = activity.getString(R.string.marker_speed,
				(markerInfo.location.speed / speedConversion).roundToInt())
		} catch (e: IllegalArgumentException) {
			speed.text = activity.getString(R.string.marker_speed, 0)
		}

		return markerView
	}

	override fun getInfoWindow(marker: Marker): View? {
		Log.v("InfoMapActivity", "getInfoWindow called")
		return null
	}

}