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

abstract class MapHandler(private val activity: FragmentActivity, private val view: View,
                          private val cameraPosition: CameraPosition, private val cameraBounds: LatLngBounds?,
                          private val skiRuns: SkiRuns, private val icons: CustomIcons,
                          private val drawOpaqueRuns: Boolean, private val showDebug: Boolean): OnMapReadyCallback {

	internal var googleMap: GoogleMap? = null

	abstract val locationManager: LocationManager<*>

	var isNightOnly = false

	abstract val additionalCallback: OnMapReadyCallback

	open fun destroy() {

		for (chairliftPolyline in locationManager.chairliftPolylines) {
			chairliftPolyline.clearPolylines()
		}

		for (greenRunPolyline in locationManager.greenRunPolylines) {
			greenRunPolyline.clearPolylines()
		}

		for (blueRunPolyline in locationManager.blueRunPolylines) {
			blueRunPolyline.clearPolylines()
		}

		for (blackRunPolyline in locationManager.blackRunPolylines) {
			blackRunPolyline.clearPolylines()
		}

		for (doubleBlackRunPolyline in locationManager.doubleBlackRunPolylines) {
			doubleBlackRunPolyline.clearPolylines()
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
		activity.lifecycleScope.launch(Dispatchers.Default) { loadSkiRuns() }

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

	private suspend fun loadSkiRuns() = coroutineScope {
		val tag = "drawPolylines"
		val jobs = mutableListOf<Job>()
		Log.v(tag, "Started drawing polylines and polygons")

		val liftsPolyline = skiRuns.liftsPolyline
		if (liftsPolyline != null) {
			jobs.add(launch {
				Log.d(tag, "Loading chairlift polyline")
				val chairliftColor = if (drawOpaqueRuns) {
					R.color.chairlift_opaque
				} else {
					R.color.chairlift
				}

				val icon = icons.getChairliftIcon()
				locationManager.chairliftPolylines = loadPolylines(liftsPolyline, chairliftColor,
					4f, icon)
				Log.d(tag, "Finished loading chairlift polyline")
			})
		}

		val greenPolylines = skiRuns.greenRunPolylines
		if (greenPolylines != null) {
			jobs.add(launch {
				Log.d(tag, "Loading green run polylines")
				val greenColor = if (drawOpaqueRuns) {
					R.color.green_opaque
				} else {
					R.color.green
				}

				val icon = icons.getEasyIcon()
				locationManager.greenRunPolylines = loadPolylines(greenPolylines, greenColor,
					3f, icon)
				Log.d(tag, "Finished loading green run polylines")
			})
		}

		val bluePolylines = skiRuns.blueRunPolylines
		if (bluePolylines != null) {
			jobs.add(launch {
				Log.d(tag, "Loading blue run polylines")
				val blueColor = if (drawOpaqueRuns) {
					R.color.blue_opaque
				} else {
					R.color.blue
				}

				val icon = icons.getMoreDifficultIcon()
				locationManager.blueRunPolylines = loadPolylines(bluePolylines, blueColor,
					2f, icon)
				Log.d(tag, "Finished loading blue run polylines")
			})
		}

		val blackPolylines = skiRuns.blackRunPolylines
		if (blackPolylines != null) {
			jobs.add(launch {
				Log.d(tag, "Loading black run polylines")
				val blackColor = if (drawOpaqueRuns) {
					R.color.black_opaque
				} else {
					R.color.black
				}

				val icon = icons.getMostDifficultIcon()
				locationManager.blackRunPolylines = loadPolylines(blackPolylines, blackColor,
					1f, icon)
				Log.d(tag, "Finished loading black run polylines")
			})
		}

		val doubleBlackPolylines = skiRuns.doubleBlackRunPolylines
		if (doubleBlackPolylines != null) {
			jobs.add(launch {
				Log.d(tag, "Loading double black run polylines")
				val blackColor = if (drawOpaqueRuns) {
					R.color.black_opaque
				} else {
					R.color.black
				}

				val icon = icons.getExtremelyDifficultIcon()
				locationManager.doubleBlackRunPolylines = loadPolylines(doubleBlackPolylines,
					blackColor, 1f, icon)
				Log.d(tag, "Finished loading double black run polylines")
			})
		}

		val startingLiftBounds = skiRuns.startingLiftBounds
		if (startingLiftBounds != null) {
			jobs.add(launch {
				Log.d(tag, "Adding starting chairlift terminals")

				val icon = icons.getChairliftIcon()
				locationManager.startingChairliftTerminals = loadPolygons(startingLiftBounds,
					R.color.chairlift_polygon, icon)
				Log.d(tag, "Finished adding ending chairlift terminals")
			})
		}

		val endingLiftPolylines = skiRuns.endingLiftPolylines
		if (endingLiftPolylines != null) {
			jobs.add(launch {
				Log.d(tag, "Adding ending chairlift terminals")

				val icon = icons.getChairliftIcon()
				locationManager.endingChairliftTerminals = loadPolygons(endingLiftPolylines,
					R.color.chairlift_polygon, icon)
				Log.d(tag, "Finished adding ending chairlift terminals")
			})
		}

		val greenBounds = skiRuns.greenRunBounds
		if (greenBounds != null) {
			jobs.add(launch {
				Log.d(tag, "Adding green bounds")

				val icon = icons.getEasyIcon()
				locationManager.greenRunBounds = loadPolygons(greenBounds, R.color.green_polygon,
					icon)
				Log.d(tag, "Finished adding green bounds")
			})
		}

		val blueBounds = skiRuns.blueRunBounds
		if (blueBounds != null) {
			jobs.add(launch {
				Log.d(tag, "Adding blue bounds")

				val icon = icons.getMoreDifficultIcon()
				locationManager.blueRunBounds = loadPolygons(blueBounds, R.color.blue_polygon,
					icon)
				Log.d(tag, "Finished adding blue bounds")
			})
		}

		val blackBounds = skiRuns.blackRunBounds
		if (blackBounds != null) {
			jobs.add(launch {
				Log.d(tag, "Adding black bounds")

				val icon = icons.getMostDifficultIcon()
				locationManager.blackRunBounds = loadPolygons(blackBounds, R.color.black_polygon,
					icon)
				Log.d(tag, "Finished adding black bounds")
			})
		}

		val doubleBlackBounds = skiRuns.doubleBlackRunBounds
		if (doubleBlackBounds != null) {
			jobs.add(launch {
				Log.d(tag, "Adding double black bounds")

				val icon = icons.getExtremelyDifficultIcon()
				locationManager.doubleBlackRunBounds = loadPolygons(doubleBlackBounds,
					R.color.black_polygon, icon)
				Log.d(tag, "Finished adding double black bounds")
			})
		}

		// Other bounds are REQUIRED because it contains the ski area bounds
		jobs.add(launch {
			Log.d(tag, "Adding other bounds")
			val sanitizedOtherBounds = mutableListOf<PolygonMapItem>()

			val otherPolygons = loadPolygons(skiRuns.other, R.color.other_polygon_fill,
				R.drawable.ic_missing)
			for (polygon in otherPolygons) {
				if (polygon.name == "Ski Area Bounds") {
					locationManager.skiAreaBounds = polygon
					continue
				}

				// Update the icon based on the name of the other location
				val icon = icons.getOtherIcon(polygon.name)
				val sanitizedOther = PolygonMapItem(polygon.placemark, icon, polygon.points)
				sanitizedOtherBounds.add(sanitizedOther)
			}

			locationManager.otherBounds = sanitizedOtherBounds
			Log.d(tag, "Finished adding other bounds")
		})

		jobs.joinAll()
		System.gc()
		Log.v(tag, "Finished drawing polylines and polygons")
	}

	private fun parseKmlFile(@RawRes file: Int): Iterable<KmlPlacemark> {
		if (googleMap == null) {
			return emptyList()
		}

		val kml = kmlLayer(googleMap!!, file, activity)
		if (kml.placemarks.spliterator().estimateSize() == 0L) {
			Log.w("parseKmlFile", "No placemarks in kml file!")
		}
		return kml.placemarks
	}

	@AnyThread
	private suspend fun loadPolylines(@RawRes fileRes: Int, @ColorRes color: Int, zIndex: Float,
	                                  @DrawableRes icon: Int): List<PolylineMapItem> =
		withContext(Dispatchers.Default) {

			val hashMap: HashMap<String, PolylineMapItem> = HashMap()

			// Load the polyline from the file, and iterate though each placemark.
			for (placemark in parseKmlFile(fileRes)) {

				// Get the LatLng coordinates of the placemark.
				val lineString: KmlLineString = placemark.geometry as KmlLineString
				val coordinates: ArrayList<LatLng> = lineString.geometryObject

				// Get the color of the polyline.
				val argb = activity.getColor(color)

				val polylineMapItem = PolylineMapItem(placemark, icon)

				// Create the polyline using the coordinates and other options.
				val polyline = withContext(Dispatchers.Main) {
					googleMap?.addPolyline {
						addAll(coordinates)
						color(argb)
						if (polylineMapItem.metadata[PolylineMapItem.EASIEST_WAY_DOWN_KEY] != null) {
							pattern(listOf(Gap(2.0F), Dash(8.0F)))
						}
						geodesic(true)
						startCap(RoundCap())
						endCap(RoundCap())
						clickable(false)
						width(8.0F)
						zIndex(zIndex)
						visible(true)
					}
				}

				// Add to the hashmap if the polyline isn't included
				if (hashMap[polylineMapItem.name] == null) {
					hashMap[polylineMapItem.name] = polylineMapItem
				}

				if (polyline != null) {
					hashMap[polylineMapItem.name]!!.polylines.add(polyline)
				}
			}

			return@withContext hashMap.values.toList()
		}

	@AnyThread
	private suspend fun loadPolygons(@RawRes fileRes: Int, @ColorRes color: Int,
	                                 @DrawableRes drawableRes: Int): List<PolygonMapItem> =
		withContext(Dispatchers.Default) {
			val polygonMapItems = mutableListOf<PolygonMapItem>()

			// Load the polygons file.
			for (placemark in parseKmlFile(fileRes)) {

				val kmlPolygon: KmlPolygon = placemark.geometry as KmlPolygon

				activity
				val argb = activity.getColor(color)

				withContext(Dispatchers.Main) {
					googleMap?.addPolygon {
						addAll(kmlPolygon.outerBoundaryCoordinates)
						clickable(false)
						geodesic(true)
						zIndex(0.5F)
						fillColor(argb)
						strokeColor(argb)
						strokeWidth(8.0F)
						visible(showDebug)
					}
				}

				val polygonMapItem = PolygonMapItem(placemark, drawableRes, kmlPolygon.outerBoundaryCoordinates)
				polygonMapItems.add(polygonMapItem)
			}

			return@withContext polygonMapItems
		}

	companion object {
		private const val MINIMUM_ZOOM = 13.0F
		private const val MAXIMUM_ZOOM = 20.0F
	}
}