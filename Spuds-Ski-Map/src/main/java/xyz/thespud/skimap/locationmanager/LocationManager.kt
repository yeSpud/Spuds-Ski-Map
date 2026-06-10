package xyz.thespud.skimap.locationmanager

import android.content.Context
import android.location.Location
import android.util.Log
import androidx.annotation.AnyThread
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import androidx.annotation.UiThread
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Dash
import com.google.android.gms.maps.model.Gap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.RoundCap
import com.google.maps.android.PolyUtil
import com.google.maps.android.data.kml.KmlLineString
import com.google.maps.android.data.kml.KmlPlacemark
import com.google.maps.android.data.kml.KmlPolygon
import com.google.maps.android.ktx.addPolygon
import com.google.maps.android.ktx.addPolyline
import com.google.maps.android.ktx.utils.kml.kmlLayer
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import xyz.thespud.skimap.R
import xyz.thespud.skimap.mapItem.MapItem
import xyz.thespud.skimap.mapItem.PolygonMapItem
import xyz.thespud.skimap.mapItem.PolylineMapItem

abstract class LocationManager<T>(skiRuns: SkiRuns, private val icons: CustomIcons, googleMap: GoogleMap,
                                  context: Context, drawOpaqueRuns: Boolean) {

	val chairliftPolylines: List<PolylineMapItem>
	val greenRunPolylines: List<PolylineMapItem>
	val blueRunPolylines: List<PolylineMapItem>
	val blackRunPolylines: List<PolylineMapItem>
	val doubleBlackRunPolylines: List<PolylineMapItem>

	val chairliftTerminals: List<PolygonMapItem>
	val greenRunBounds: List<PolygonMapItem>
	val blueRunBounds: List<PolygonMapItem>
	val blackRunBounds: List<PolygonMapItem>
	val doubleBlackRunBounds: List<PolygonMapItem>
	val otherBounds: List<PolygonMapItem>

	var previousLocation: Location? = null
		protected set
	var currentLocation: Location? = null
		protected set

	protected var isOnChairlift: PolygonMapItem? = null

	fun updateLocations(newLocation: Location) {
		previousLocation = currentLocation
		currentLocation = newLocation
	}

	fun locationInBounds(location: Location, bounds: List<PolygonMapItem>): PolygonMapItem? {
		for (polygon in bounds) {
			if (PolyUtil.containsLocation(location.latitude, location.longitude, polygon.points,
					true)) {
				return polygon
			}
		}

		return null
	}

	abstract fun checkIfInChairliftTerminal(): T?

	abstract fun checkIfLiftlineRun(run: T): T

	abstract fun checkIfOnRun(): T?

	abstract fun getInLocation(): T?

	protected fun parseKmlFile(googleMap: GoogleMap, @RawRes file: Int, context: Context): Iterable<KmlPlacemark> {
		val kml = kmlLayer(googleMap, file, context)
		if (kml.placemarks.spliterator().estimateSize() == 0L) {
			Log.w("parseKmlFile", "No placemarks in kml file!")
		}
		return kml.placemarks
	}

	private fun getIcon(iconType: IconType, name: String): Int {
		return when (iconType) {
			IconType.CHAIRLIFT -> icons.getChairliftIcon(name)
			IconType.EASY -> icons.getEasyIcon()
			IconType.MORE_DIFFICULT -> icons.getMoreDifficultIcon()
			IconType.MOST_DIFFICULT -> icons.getMostDifficultIcon()
			IconType.EXTREMELY_DIFFICULT -> icons.getExtremelyDifficultIcon()
			IconType.TERRAIN_PARK -> icons.getTerrainParkIcon()
			IconType.OTHER -> icons.getOtherIcon(name)
		}
	}

	@UiThread
	private fun loadPolylines(googleMap: GoogleMap, @RawRes fileRes: Int, context: Context,
	                          @ColorRes color: Int, zIndex: Float, iconType: IconType): List<PolylineMapItem> {

		val hashMap: HashMap<String, PolylineMapItem> = HashMap()

		// Load the polyline from the file, and iterate though each placemark.
		for (placemark in parseKmlFile(googleMap, fileRes, context)) {

			// Get the LatLng coordinates of the placemark.
			val lineString = placemark.geometry as KmlLineString
			val coordinates: ArrayList<LatLng> = lineString.geometryObject

			val name = MapItem.getPlacemarkName(placemark)
			val icon = getIcon(iconType, name)

			// Get the color of the polyline.
			val argb = context.getColor(color)

			val polylineMapItem = PolylineMapItem(placemark, icon)

			// Create the polyline using the coordinates and other options.
			val polyline = googleMap.addPolyline {
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

			// Add to the hashmap if the polyline isn't included
			if (hashMap[polylineMapItem.name] == null) {
				hashMap[polylineMapItem.name] = polylineMapItem
			}

			hashMap[polylineMapItem.name]!!.polylines.add(polyline)
		}

		return hashMap.values.toList()
	}

	@UiThread
	protected fun loadPolygons(googleMap: GoogleMap, @RawRes fileRes: Int, context: Context,
	                           @ColorRes color: Int, iconType: IconType, showDebug: Boolean = false):
			List<PolygonMapItem> {

		val polygonMapItems = mutableListOf<PolygonMapItem>()

		// Load the polygons file.
		for (placemark in parseKmlFile(googleMap, fileRes, context)) {

			val name = MapItem.getPlacemarkName(placemark)
			val icon = getIcon(iconType, name)

			val kmlPolygon = placemark.geometry as KmlPolygon

			val argb = context.getColor(color)

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

			val polygonMapItem = PolygonMapItem(placemark, icon, kmlPolygon.outerBoundaryCoordinates)
			polygonMapItems.add(polygonMapItem)
		}

		return polygonMapItems
	}

	init {
		val tag = "LocationManager"
		Log.v(tag, "Started drawing polylines and polygons")

		val liftsPolylineFile = skiRuns.chairliftsPolyline
		if (liftsPolylineFile != null) {
			Log.d(tag, "Loading chairlift polyline")
			val chairliftColor = if (drawOpaqueRuns) { R.color.chairlift_opaque } else { R.color.chairlift }

			chairliftPolylines = loadPolylines(googleMap, liftsPolylineFile, context, chairliftColor,
				4f, IconType.CHAIRLIFT)

			Log.d(tag, "Finished loading chairlift polyline")
		} else { chairliftPolylines = emptyList() }

		val greenPolylines = skiRuns.greenRunPolylines
		if (greenPolylines != null) {
			Log.d(tag, "Loading green run polylines")
			val greenColor = if (drawOpaqueRuns) { R.color.green_opaque } else { R.color.green }

			greenRunPolylines = loadPolylines(googleMap, greenPolylines, context, greenColor,
				3f, IconType.EASY)
			Log.d(tag, "Finished loading green run polylines")
		} else { greenRunPolylines = emptyList() }

		val bluePolylines = skiRuns.blueRunPolylines
		if (bluePolylines != null) {
			Log.d(tag, "Loading blue run polylines")
			val blueColor = if (drawOpaqueRuns) { R.color.blue_opaque } else { R.color.blue }

			blueRunPolylines = loadPolylines(googleMap, bluePolylines, context, blueColor,
				2f, IconType.MORE_DIFFICULT)
			Log.d(tag, "Finished loading blue run polylines")
		} else { blueRunPolylines = emptyList() }

		val blackPolylines = skiRuns.blackRunPolylines
		if (blackPolylines != null) {
			Log.d(tag, "Loading black run polylines")
			val blackColor = if (drawOpaqueRuns) { R.color.black_opaque } else { R.color.black }

			blackRunPolylines = loadPolylines(googleMap, blackPolylines, context, blackColor,
				1f, IconType.MOST_DIFFICULT)
			Log.d(tag, "Finished loading black run polylines")
		} else { blackRunPolylines = emptyList() }

		val doubleBlackPolylines = skiRuns.doubleBlackRunPolylines
		if (doubleBlackPolylines != null) {
			Log.d(tag, "Loading double black run polylines")
			val blackColor = if (drawOpaqueRuns) { R.color.black_opaque } else { R.color.black }

			doubleBlackRunPolylines = loadPolylines(googleMap, doubleBlackPolylines, context,
				blackColor, 1f, IconType.EXTREMELY_DIFFICULT)
			Log.d(tag, "Finished loading double black run polylines")
		} else { doubleBlackRunPolylines = emptyList() }

		// var terminals = mutableListOf<PolygonMapItem>()
		val terminals = skiRuns.chairliftTerminals
		if (terminals != null) {
			Log.d(tag, "Adding starting chairlift terminals")

			chairliftTerminals = loadPolygons(googleMap,terminals, context, R.color.chairlift_polygon,
				IconType.CHAIRLIFT)
			Log.d(tag, "Finished adding ending chairlift terminals")
		} else { chairliftTerminals = emptyList() }


		val greenBounds = skiRuns.greenRunBounds
		if (greenBounds != null) {
			Log.d(tag, "Adding green bounds")

			greenRunBounds = loadPolygons(googleMap, greenBounds, context, R.color.green_polygon,
				IconType.EASY)
			Log.d(tag, "Finished adding green bounds")
		} else { greenRunBounds = emptyList() }

		val blueBounds = skiRuns.blueRunBounds
		if (blueBounds != null) {
			Log.d(tag, "Adding blue bounds")

			blueRunBounds = loadPolygons(googleMap, blueBounds, context,R.color.blue_polygon,
				IconType.MORE_DIFFICULT)
			Log.d(tag, "Finished adding blue bounds")
		} else { blueRunBounds = emptyList() }

		val blackBounds = skiRuns.blackRunBounds
		if (blackBounds != null) {
			Log.d(tag, "Adding black bounds")

			blackRunBounds = loadPolygons(googleMap, blackBounds, context, R.color.black_polygon,
				IconType.MOST_DIFFICULT)
			Log.d(tag, "Finished adding black bounds")
		} else { blackRunBounds = emptyList() }

		val doubleBlackBounds = skiRuns.doubleBlackRunBounds
		if (doubleBlackBounds != null) {
			Log.d(tag, "Adding double black bounds")

			doubleBlackRunBounds = loadPolygons(googleMap, doubleBlackBounds, context,
				R.color.black_polygon, IconType.EXTREMELY_DIFFICULT)
			Log.d(tag, "Finished adding double black bounds")
		} else { doubleBlackRunBounds = emptyList() }

		val other = skiRuns.other
		if (other != null) {
			Log.d(tag, "Adding other bounds")

			otherBounds = loadPolygons(googleMap, other, context, R.color.other_polygon_fill,
				IconType.OTHER)
			Log.d(tag, "Finished adding other bounds")
		} else { otherBounds = emptyList() }
	}
}