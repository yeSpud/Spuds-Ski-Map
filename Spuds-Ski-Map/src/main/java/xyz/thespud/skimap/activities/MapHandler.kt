package xyz.thespud.skimap.activities

import android.util.Log
import androidx.annotation.AnyThread
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.thespud.skimap.R
import xyz.thespud.skimap.mapItem.Locations
import xyz.thespud.skimap.mapItem.PolygonMapItem
import xyz.thespud.skimap.mapItem.PolylineMapItem
import xyz.thespud.skimap.mapItem.SkiRuns
import java.util.Collections.emptyList

abstract class MapHandler(val activity: FragmentActivity,
                          private val leftPadding: Int, private val topPadding: Int,
                          private val rightPadding: Int, private val bottomPadding: Int,
                          private val cameraPosition: CameraPosition,
                          private val cameraBounds: LatLngBounds?, private val skiRuns: SkiRuns,
                          private val drawOpaqueRuns: Boolean, private val showDebug: Boolean): OnMapReadyCallback {

	internal lateinit var googleMap: GoogleMap

	var isNightOnly = false

	abstract val additionalCallback: OnMapReadyCallback

	var chairliftPolylines: List<PolylineMapItem> = emptyList()
		private set
	var greenRunPolylines: List<PolylineMapItem> = emptyList()
		private set
	var blueRunPolylines: List<PolylineMapItem> = emptyList()
		private set
	var blackRunPolylines: List<PolylineMapItem> = emptyList()
		private set
	var doubleBlackRunPolylines: List<PolylineMapItem> = emptyList()
		private set

	var skiAreaBounds: PolygonMapItem? = null
		private set

	var otherBounds: List<PolygonMapItem> = emptyList()
		private set
	var startingChairliftTerminals: List<PolygonMapItem> = emptyList()
		private set
	var endingChairliftTerminals: List<PolygonMapItem> = emptyList()
		private set

	var greenRunBounds: List<PolygonMapItem> = emptyList()
		private set
	var blueRunBounds: List<PolygonMapItem> = emptyList()
		private set
	var blackRunBounds: List<PolygonMapItem> = emptyList()
		private set
	var doubleBlackRunBounds: List<PolygonMapItem> = emptyList()
		private set

	open fun destroy() {

		for (chairliftPolyline in chairliftPolylines) {
			chairliftPolyline.clearPolylines()
		}

		for (greenRunPolyline in greenRunPolylines) {
			greenRunPolyline.clearPolylines()
		}

		for (blueRunPolyline in blueRunPolylines) {
			blueRunPolyline.clearPolylines()
		}

		for (blackRunPolyline in blackRunPolylines) {
			blackRunPolyline.clearPolylines()
		}

		for (doubleBlackRunPolyline in doubleBlackRunPolylines) {
			doubleBlackRunPolyline.clearPolylines()
		}

		// Clear the map if its not null.
		Log.v("MapHandler", "Clearing map.")
		googleMap.clear()

		// This frees up a bunch of ram, so call the garbage collection to collect the free ram
		System.gc()
	}

	abstract fun getOtherIcon(name: String): Int

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
		googleMap = map

		// Setup camera view logging.
		if (showDebug) {
			googleMap.setOnCameraIdleListener {
				val cameraPosition: CameraPosition = googleMap.cameraPosition

				val cameraTag = "OnCameraIdle"
				Log.d(cameraTag, "Bearing: ${cameraPosition.bearing}")
				Log.d(cameraTag, "Target: ${cameraPosition.target}")
				Log.d(cameraTag, "Tilt: ${cameraPosition.tilt}")
				Log.d(cameraTag, "Zoom: ${cameraPosition.zoom}")
			}
		}

		// Move the map camera view and set the view restrictions.
		googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
		googleMap.setLatLngBoundsForCameraTarget(cameraBounds)
		googleMap.setMinZoomPreference(MINIMUM_ZOOM)
		googleMap.setMaxZoomPreference(MAXIMUM_ZOOM)

		googleMap.isIndoorEnabled = false
		googleMap.mapType = GoogleMap.MAP_TYPE_SATELLITE

		// Load the various polylines onto the map.
		activity.lifecycleScope.launch(Dispatchers.Default) { drawPolylines() }

		googleMap.setPadding(leftPadding, topPadding, rightPadding, bottomPadding)

		Log.d("onMapReady", "Running additional setup steps...")
		additionalCallback.onMapReady(googleMap)
		Log.d("onMapReady", "Finished setting up map.")
	}

	private suspend fun drawPolylines() = coroutineScope {
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
				chairliftPolylines = loadPolylines(liftsPolyline, chairliftColor, 4f, Locations.chairliftIcon)
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
				greenRunPolylines = loadPolylines(greenPolylines, greenColor, 3f, Locations.greenIcon)
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
				blueRunPolylines = loadPolylines(bluePolylines, blueColor, 2f, Locations.blueIcon)
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
				blackRunPolylines = loadPolylines(blackPolylines, blackColor, 1f, Locations.blackIcon)
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
				doubleBlackRunPolylines = loadPolylines(doubleBlackPolylines, blackColor, 1f,
					Locations.doubleBlackIcon)
				Log.d(tag, "Finished loading double black run polylines")
			})
		}

		val startingLiftBounds = skiRuns.startingLiftBounds
		if (startingLiftBounds != null) {
			jobs.add(launch {
				Log.d(tag, "Adding starting chairlift terminals")
				startingChairliftTerminals = loadPolygons(startingLiftBounds, R.color.chairlift_polygon,
					Locations.chairliftIcon)
				Log.d(tag, "Finished adding ending chairlift terminals")
			})
		}

		val endingLiftPolylines = skiRuns.endingLiftPolylines
		if (endingLiftPolylines != null) {
			jobs.add(launch {
				Log.d(tag, "Adding ending chairlift terminals")
				endingChairliftTerminals = loadPolygons(endingLiftPolylines, R.color.chairlift_polygon,
					Locations.chairliftIcon)
				Log.d(tag, "Finished adding ending chairlift terminals")
			})
		}

		val greenBounds = skiRuns.greenRunBounds
		if (greenBounds != null) {
			jobs.add(launch {
				Log.d(tag, "Adding green bounds")
				greenRunBounds = loadPolygons(greenBounds, R.color.green_polygon, Locations.greenIcon)
				Log.d(tag, "Finished adding green bounds")
			})
		}

		val blueBounds = skiRuns.blueRunBounds
		if (blueBounds != null) {
			jobs.add(launch {
				Log.d(tag, "Adding blue bounds")
				blueRunBounds = loadPolygons(blueBounds, R.color.blue_polygon, Locations.blueIcon)
				Log.d(tag, "Finished adding blue bounds")
			})
		}

		val blackBounds = skiRuns.blackRunBounds
		if (blackBounds != null) {
			jobs.add(launch {
				Log.d(tag, "Adding black bounds")
				blackRunBounds = loadPolygons(blackBounds, R.color.black_polygon, Locations.blackIcon)
				Log.d(tag, "Finished adding black bounds")
			})
		}

		val doubleBlackBounds = skiRuns.doubleBlackRunBounds
		if (doubleBlackBounds != null) {
			jobs.add(launch {
				Log.d(tag, "Adding double black bounds")
				blackRunBounds = loadPolygons(doubleBlackBounds, R.color.black_polygon, Locations.doubleBlackIcon)
				Log.d(tag, "Finished adding double black bounds")
			})
		}

		// Other bounds are REQUIRED because it contains the ski area bounds
		jobs.add(launch {
			Log.d(tag, "Adding other bounds")
			val sanitizedOtherBounds = mutableListOf<PolygonMapItem>()

			val iconRes = R.drawable.ic_missing // getOtherIcon("") // FIXME

			val otherPolygons = loadPolygons(skiRuns.other, R.color.other_polygon_fill, iconRes)
			for (polygon in otherPolygons) {
				if (polygon.name == "Ski Area Bounds") {
					skiAreaBounds = polygon
					continue
				}
				sanitizedOtherBounds.add(polygon)
			}

			otherBounds = sanitizedOtherBounds
			Log.d(tag, "Finished adding other bounds")
		})

		jobs.forEach { it.join() }
		System.gc()
		Log.v(tag, "Finished drawing polylines and polygons")
	}

	private fun parseKmlFile(@RawRes file: Int): Iterable<KmlPlacemark> {
		val kml = kmlLayer(googleMap, file, activity)
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
					googleMap.addPolyline {
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

				hashMap[polylineMapItem.name]!!.polylines.add(polyline)
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

				val argb = activity.getColor(color)

				/*val polygon = */withContext(Dispatchers.Main) {
					googleMap.addPolygon {
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