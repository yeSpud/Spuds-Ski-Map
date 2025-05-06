package xyz.thespud.skimap.activities

import android.os.Build
import android.util.Log
import androidx.annotation.AnyThread
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import androidx.core.content.res.ResourcesCompat
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
import com.google.android.gms.maps.model.Polygon
import com.google.android.gms.maps.model.RoundCap
import com.google.maps.android.BuildConfig
import com.google.maps.android.data.kml.KmlLineString
import com.google.maps.android.data.kml.KmlPlacemark
import com.google.maps.android.data.kml.KmlPolygon
import com.google.maps.android.ktx.addPolygon
import com.google.maps.android.ktx.addPolyline
import com.google.maps.android.ktx.utils.kml.kmlLayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.thespud.skimap.R
import xyz.thespud.skimap.mapItem.Locations
import xyz.thespud.skimap.mapItem.MapItem
import xyz.thespud.skimap.mapItem.PolylineMapItem

abstract class MapHandler(val activity: FragmentActivity, val cameraBounds: LatLngBounds,
                          @RawRes val lifts: Int, @RawRes val green: Int, @RawRes val blue: Int,
                          @RawRes val black: Int, @RawRes val doubleBlack: Int,
                          @RawRes val starting_lifts_bounds: Int, @RawRes val ending_lifts_bounds: Int,
                          @RawRes val green_polygons: Int, @RawRes val blue_polygons: Int,
                          @RawRes val black_polygons: Int, @RawRes val double_black_polygons: Int,
                          @RawRes val other: Int, private val drawOpaqueRuns: Boolean = false): OnMapReadyCallback {

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

	var skiAreaBounds: MapItem? = null
		private set

	var otherBounds: List<MapItem> = emptyList()
		private set
	var startingChairliftTerminals: List<MapItem> = emptyList()
		private set
	var endingChairliftTerminals: List<MapItem> = emptyList()
		private set

	var greenRunBounds: List<MapItem> = emptyList()
		private set
	var blueRunBounds: List<MapItem> = emptyList()
		private set
	var blackRunBounds: List<MapItem> = emptyList()
		private set
	var doubleBlackRunBounds: List<MapItem> = emptyList()
		private set

	open fun destroy() {

		for (chairliftPolyline in chairliftPolylines) {
			chairliftPolyline.destroyUIItems()
		}

		for (greenRunPolyline in greenRunPolylines) {
			greenRunPolyline.destroyUIItems()
		}

		for (blueRunPolyline in blueRunPolylines) {
			blueRunPolyline.destroyUIItems()
		}

		for (blackRunPolyline in blackRunPolylines) {
			blackRunPolyline.destroyUIItems()
		}

		for (doubleBlackRunPolyline in doubleBlackRunPolylines) {
			doubleBlackRunPolyline.destroyUIItems()
		}

		// Clear the map if its not null.
		Log.v("MapHandler", "Clearing map.")
		googleMap.clear()

		// This frees up a bunch of ram, so call the garbage collection to collect the free ram
		System.gc()
	}

	abstract fun getOtherIcon(name: String): Int?

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
		if (BuildConfig.DEBUG) {
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
		googleMap.moveCamera(
			CameraUpdateFactory.newCameraPosition(
				CameraPosition.Builder()
			.target(LatLng(47.92517834073426, -117.10480503737926)).tilt(45F)
			.bearing(317.50552F).zoom(14.414046F).build()))
		googleMap.setLatLngBoundsForCameraTarget(cameraBounds)
		googleMap.setMinZoomPreference(MINIMUM_ZOOM)
		googleMap.setMaxZoomPreference(MAXIMUM_ZOOM)

		googleMap.setIndoorEnabled(false)
		googleMap.mapType = GoogleMap.MAP_TYPE_SATELLITE

		// Load the various polylines onto the map.
		activity.lifecycleScope.launch(Dispatchers.Default) {

			val chairliftPolylineJob = launch {
				Log.d(tag, "Loading chairlift polyline")
				val chairliftColor = if (drawOpaqueRuns) {
					R.color.chairlift_opaque
				} else {
					R.color.chairlift
				}
				chairliftPolylines = loadPolylines(lifts, chairliftColor, 4f, Locations.chairliftIcon)
				Log.d(tag, "Finished loading chairlift polyline")
			}

			val greenRunsPolylineJob = launch {
				Log.d(tag, "Loading green run polylines")
				val greenColor = if (drawOpaqueRuns) {
					R.color.green_opaque
				} else {
					R.color.green
				}
				greenRunPolylines = loadPolylines(green, greenColor, 3f, Locations.greenIcon)
				Log.d(tag, "Finished loading green run polylines")
			}

			val blueRunsPolylineJob = launch {
				Log.d(tag, "Loading blue run polylines")
				val blueColor = if (drawOpaqueRuns) {
					R.color.blue_opaque
				} else {
					R.color.blue
				}
				blueRunPolylines = loadPolylines(blue, blueColor, 2f, Locations.blueIcon)
				Log.d(tag, "Finished loading blue run polylines")
			}

			val blackRunsPolylineJob = launch {
				Log.d(tag, "Loading black run polylines")
				val blackColor = if (drawOpaqueRuns) {
					R.color.black_opaque
				} else {
					R.color.black
				}
				blackRunPolylines = loadPolylines(black, blackColor, 1f, Locations.blackIcon)
				Log.d(tag, "Finished loading black run polylines")
			}

			val doubleBlackRunsPolylineJob = launch {
				Log.d(tag, "Loading double black run polylines")
				val blackColor = if (drawOpaqueRuns) {
					R.color.black_opaque
				} else {
					R.color.black
				}
				doubleBlackRunPolylines = loadPolylines(doubleBlack, blackColor, 1f,
					Locations.doubleBlackIcon)
				Log.d(tag, "Finished loading double black run polylines")
			}

			val startingChairliftJob = launch {
				Log.d(tag, "Adding starting chairlift terminals")
				startingChairliftTerminals = loadMapItems(starting_lifts_bounds, R.color.chairlift_polygon,
					Locations.chairliftIcon)
				Log.d(tag, "Finished adding ending chairlift terminals")
			}

			val endingChairliftJob = launch {
				Log.d(tag, "Adding ending chairlift terminals")
				endingChairliftTerminals = loadMapItems(ending_lifts_bounds, R.color.chairlift_polygon,
					Locations.chairliftIcon)
				Log.d(tag, "Finished adding ending chairlift terminals")
			}

			val greenRunBoundsJob = launch {
				Log.d(tag, "Adding green bounds")
				greenRunBounds = loadMapItems(green_polygons, R.color.green_polygon, Locations.greenIcon)
				Log.d(tag, "Finished adding green bounds")
			}

			val blueRunBoundsJob = launch {
				Log.d(tag, "Adding blue bounds")
				blueRunBounds = loadMapItems(blue_polygons, R.color.blue_polygon, Locations.blueIcon)
				Log.d(tag, "Finished adding blue bounds")
			}

			val blackRunBoundsJob = launch {
				Log.d(tag, "Adding black bounds")
				blackRunBounds = loadMapItems(black_polygons, R.color.black_polygon, Locations.blackIcon)
				Log.d(tag, "Finished adding black bounds")
			}

			val doubleBlackRunBoundsJob = launch {
				Log.d(tag, "Adding double black bounds")
				blackRunBounds = loadMapItems(double_black_polygons, R.color.black_polygon,
					Locations.doubleBlackIcon)
				Log.d(tag, "Finished adding double black bounds")
			}

			val otherBoundsJob = launch {
				Log.d(tag, "Adding other bounds")
				val other = loadPolygons(other, R.color.other_polygon_fill)
				val bounds = mutableListOf<MapItem>()
				for (name in other.keys) {
					val values = other[name]!!
					val polygonPoints: MutableList<List<LatLng>> = mutableListOf()
					for (value in values) {
						withContext(Dispatchers.Main) { polygonPoints.add(value.points) }
					}

					if (name == "Ski Area Bounds") {
						withContext(Dispatchers.Main) { values[0].remove() }
						skiAreaBounds = MapItem(name, polygonPoints)
						continue
					}

					Log.d(tag, "Getting icon for $name")
					val icon = getOtherIcon(name)
					bounds.add(MapItem(name, polygonPoints, icon))
				}
				otherBounds = bounds
				Log.d(tag, "Finished adding other bounds")
			}

			joinAll(chairliftPolylineJob, greenRunsPolylineJob, blueRunsPolylineJob, blackRunsPolylineJob,
				doubleBlackRunsPolylineJob, startingChairliftJob, endingChairliftJob, greenRunBoundsJob,
				blueRunBoundsJob, blackRunBoundsJob, doubleBlackRunBoundsJob, otherBoundsJob)
			System.gc()

			val callbackJob = launch(Dispatchers.Main) {
				Log.d("onMapReady", "Running additional setup steps...")
				additionalCallback.onMapReady(googleMap)
				Log.d("onMapReady", "Finished setting up map.")
			}
			callbackJob.join()
		}
	}

	private fun parseKmlFile(@RawRes file: Int): Iterable<KmlPlacemark> {
		val kml = kmlLayer(googleMap, file, activity)
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && kml.placemarks.spliterator().estimateSize() == 0L) {
			Log.w("parseKmlFile", "No placemarks in kml file!")
		}
		return kml.placemarks
	}

	@AnyThread
	private suspend fun loadPolylines(@RawRes fileRes: Int, @ColorRes color: Int, zIndex: Float,
	                                  @DrawableRes icon: Int? = null): List<PolylineMapItem> =
		withContext(Dispatchers.Default) {

			val hashMap: HashMap<String, PolylineMapItem> = HashMap()

			// Load the polyline from the file, and iterate though each placemark.
			for (placemark in parseKmlFile(fileRes)) {

				// Get the name of the polyline.
				val name: String = getPlacemarkName(placemark)

				// Get the LatLng coordinates of the placemark.
				val lineString: KmlLineString = placemark.geometry as KmlLineString
				val coordinates: ArrayList<LatLng> = lineString.geometryObject

				// Get the color of the polyline.
				val argb = getARGB(color)

				// Get the properties of the polyline.
				val polylineProperties: List<String>? = if (placemark.hasProperty(PROPERTY_KEY)) {
					placemark.getProperty(PROPERTY_KEY).split('\n')
				} else {
					null
				}

				// Check if the polyline is an easy way down polyline.
				val easiestWayDown = polylineHasProperty(polylineProperties, "easiest way down")

				// Create the polyline using the coordinates and other options.
				val polyline = withContext(Dispatchers.Main) {
					googleMap.addPolyline {
						addAll(coordinates)
						color(argb)
						if (easiestWayDown) {
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

				// Check if the map item is already in the hashmap.
				if (hashMap[name] == null) {

					// Check if this is a night item.
					val night = polylineHasProperty(polylineProperties, "night run")

					// Create a new map item for the polyline (since its not in the hashmap).
					val mapItem = PolylineMapItem(name, MutableList(1) { polyline },
						isNightRun = night, icon = icon)

					// Add the map item to the hashmap.
					hashMap[name] = mapItem

				} else {

					// Add the polyline to the map item.
					hashMap[name]!!.polylines.add(polyline)
				}
			}

			return@withContext hashMap.values.toList()
		}

	@AnyThread
	private suspend fun loadPolygons(@RawRes fileRes: Int, @ColorRes color: Int):
			HashMap<String, List<Polygon>> = withContext(Dispatchers.Default) {

		val hashMap: HashMap<String, List<Polygon>> = HashMap()

		// Load the polygons file.
		for (placemark in parseKmlFile(fileRes)) {

			val kmlPolygon: KmlPolygon = placemark.geometry as KmlPolygon

			val argb = getARGB(color)

			val polygon = withContext(Dispatchers.Main) {
				googleMap.addPolygon {
					addAll(kmlPolygon.outerBoundaryCoordinates)
					clickable(false)
					geodesic(true)
					zIndex(0.5F)
					fillColor(argb)
					strokeColor(argb)
					strokeWidth(8.0F)
					visible(BuildConfig.DEBUG)
				}
			}

			val name: String = getPlacemarkName(placemark)
			Log.d("loadPolygons", "Loading polygon for $name")

			if (hashMap[name] == null) {
				hashMap[name] = List(1) { polygon }
			} else {
				val list: MutableList<Polygon> = hashMap[name]!!.toMutableList()
				list.add(polygon)
				hashMap[name] = list.toList()
			}
		}

		return@withContext hashMap
	}

	@AnyThread
	private suspend fun loadMapItems(@RawRes fileRes: Int, @ColorRes color: Int,
	                                 @DrawableRes drawableRes: Int? = null): List<MapItem> =
		withContext(Dispatchers.Default) {
			val mapItems = mutableListOf<MapItem>()
			val polygons = loadPolygons(fileRes, color)
			for (name in polygons.keys) {
				val values = polygons[name]!!
				val polygonPoints: MutableList<List<LatLng>> = mutableListOf()
				for (value in values) {
					withContext(Dispatchers.Main) {
						polygonPoints.add(value.points)
					}
				}
				mapItems.add(MapItem(name, polygonPoints, drawableRes))
			}
			return@withContext mapItems
		}

	private fun getARGB(@ColorRes color: Int): Int {
		return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			activity.getColor(color)
		} else {
			ResourcesCompat.getColor(activity.resources, color, null)
		}
	}

	companion object {

		private const val MINIMUM_ZOOM = 13.0F

		private const val MAXIMUM_ZOOM = 20.0F

		private const val PROPERTY_KEY = "description"

		private fun getPlacemarkName(placemark: KmlPlacemark): String {

			return if (placemark.hasProperty("name")) {
				placemark.getProperty("name")
			} else {

				// If the name wasn't found in the properties return an empty string.
				Log.w("getPlacemarkName", "Placemark is missing name!")
				""
			}
		}

		private fun polylineHasProperty(polylineProperties: List<String>?, propertyKey: String): Boolean {

			if (polylineProperties == null) {
				return false
			}

			for (property in polylineProperties) {
				Log.v("polylineHasProperty", "Checking if property \"$property\" matches property key \"$propertyKey\"")
				if (property.contains(propertyKey)) {
					return true
				}
			}

			return false
		}
	}
}