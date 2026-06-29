package xyz.thespud.skimap.activities

import android.content.Intent
import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLngBounds
import xyz.thespud.skimap.locationmanager.CustomIcons
import xyz.thespud.skimap.locationmanager.LocationManager
import xyz.thespud.skimap.locationmanager.SkiAreaObjects

abstract class MapHandler(val activity: ComponentActivity, private val view: View, private val cameraPosition: CameraPosition,
                          private val cameraBounds: LatLngBounds?, internal val skiAreaObjects: SkiAreaObjects,
                          internal val icons: CustomIcons, private val showDebug: Boolean): OnMapReadyCallback {

	internal var googleMap: GoogleMap? = null

	abstract val locationManager: LocationManager<*>?

	var isNightOnly = false

	abstract val mapReadyBroadcastFilter: String

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

		// Clear the map if it's not null.
		Log.v("MapHandler", "Clearing map.")
		googleMap?.clear()

		// Add broadcast for map event with am intent that has an extra boolean of MAPREADY = FALSE
		val broadcastIntent = Intent(mapReadyBroadcastFilter)
		broadcastIntent.putExtra(MAPREADY, false)
		activity.sendBroadcast(broadcastIntent)

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

		// Add broadcast for map event with am intent that has an extra boolean of MAPREADY = TRUE
		val broadcastIntent = Intent(mapReadyBroadcastFilter)
		broadcastIntent.putExtra(MAPREADY, true)
		activity.sendBroadcast(broadcastIntent)
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

		const val MAPREADY = "MapReady"

		private const val MINIMUM_ZOOM = 13.0F
		private const val MAXIMUM_ZOOM = 20.0F
	}
}