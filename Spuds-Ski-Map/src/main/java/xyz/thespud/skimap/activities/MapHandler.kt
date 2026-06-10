package xyz.thespud.skimap.activities

import android.util.Log
import android.view.View
import androidx.annotation.AnyThread
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.Dash
import com.google.android.gms.maps.model.Gap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.RoundCap
import com.google.maps.android.data.kml.KmlLineString
import com.google.maps.android.data.kml.KmlPlacemark
import com.google.maps.android.data.kml.KmlPolygon
import com.google.maps.android.ktx.addPolygon
import com.google.maps.android.ktx.addPolyline
import com.google.maps.android.ktx.utils.kml.kmlLayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.thespud.skimap.R
import xyz.thespud.skimap.locationmanager.CustomIcons
import xyz.thespud.skimap.locationmanager.LocationManager
import xyz.thespud.skimap.mapItem.PolygonMapItem
import xyz.thespud.skimap.mapItem.PolylineMapItem
import xyz.thespud.skimap.locationmanager.SkiRuns

abstract class MapHandler(private val view: View, private val cameraPosition: CameraPosition,
                          private val cameraBounds: LatLngBounds?, private val showDebug: Boolean): OnMapReadyCallback {

	internal var googleMap: GoogleMap? = null

	abstract val locationManager: LocationManager<*>?

	var isNightOnly = false

	abstract val additionalCallback: OnMapReadyCallback

	open fun destroy() {

		val _locationManager = locationManager
		if (_locationManager != null) {

			for (chairliftPolyline in _locationManager.chairliftPolylines) {
				chairliftPolyline.clearPolylines()
			}

			for (greenRunPolyline in _locationManager.greenRunPolylines) {
				greenRunPolyline.clearPolylines()
			}

			for (blueRunPolyline in _locationManager.blueRunPolylines) {
				blueRunPolyline.clearPolylines()
			}

			for (blackRunPolyline in _locationManager.blackRunPolylines) {
				blackRunPolyline.clearPolylines()
			}

			for (doubleBlackRunPolyline in _locationManager.doubleBlackRunPolylines) {
				doubleBlackRunPolyline.clearPolylines()
			}
		}

		// Clear the map if its not null.
		Log.v("MapHandler", "Clearing map.")
		googleMap?.clear()

		// This frees up a bunch of ram, so call the garbage collection to collect the free ram
		System.gc()
	}

	/**
	 * Manipulates the map once available.
	 * This callback is triggered when the map is ready to be used.
	 * This is where we can add markers or lines, add listeners or move the camera. In this case,
	 * we just add a marker near Sydney, Australia.
	 * If Google Play services is not installed on the device, the user will be prompted to install
	 * it inside the SupportMapFragment. This method will only be triggered once the user has
	 * installed Google Play services and returned to the app.
	 */
	override fun onMapReady(map: GoogleMap) {
		val tag = "onMapReady"
		Log.v(tag, "Setting up map for the first time...")

		// Setup camera view logging.
		if (showDebug) {
			map.setOnCameraIdleListener {
				val cameraPosition: CameraPosition = map.cameraPosition

				val cameraTag = "OnCameraIdle"
				Log.d(cameraTag, "Bearing: ${cameraPosition.bearing}")
				Log.d(cameraTag, "Target: ${cameraPosition.target}")
				Log.d(cameraTag, "Tilt: ${cameraPosition.tilt}")
				Log.d(cameraTag, "Zoom: ${cameraPosition.zoom}")
			}
		}

		// Move the map camera view and set the view restrictions.
		map.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
		map.setLatLngBoundsForCameraTarget(cameraBounds)
		map.setMinZoomPreference(MINIMUM_ZOOM)
		map.setMaxZoomPreference(MAXIMUM_ZOOM)

		map.isIndoorEnabled = false
		map.mapType = GoogleMap.MAP_TYPE_SATELLITE

		// Load the various polylines and polygons onto the map.
		// activity.lifecycleScope.launch(Dispatchers.Default) { loadSkiRuns() }

		applyMapInsets(view, map)

		googleMap = map

		Log.d("onMapReady", "Running additional setup steps...")
		additionalCallback.onMapReady(googleMap!!)
		Log.d("onMapReady", "Finished setting up map.")
	}

	// For fixing edge to edge behavior
	fun applyMapInsets(view: View, map: GoogleMap) {
		ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
			Log.v("applyMapInsets", "Applying map insets...")

			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			map.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)

			insets
		}

		// Request the insets be applied again since they may have already been applied to the view,
		// and we want our newly set listener to run
		view.requestApplyInsets()
	}

	companion object {
		private const val MINIMUM_ZOOM = 13.0F
		private const val MAXIMUM_ZOOM = 20.0F
	}
}