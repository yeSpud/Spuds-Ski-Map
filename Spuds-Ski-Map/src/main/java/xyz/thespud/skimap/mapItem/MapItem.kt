package xyz.thespud.skimap.mapItem

import android.util.Log
import androidx.annotation.DrawableRes
import com.google.maps.android.data.kml.KmlPlacemark
import java.util.Collections

abstract class MapItem(placemark: KmlPlacemark, @DrawableRes val icon: Int) {

	val name: String

	val metadata: HashMap<String, Any>

	init {
		name = getPlacemarkName(placemark)

		val properties: List<String> = if (placemark.hasProperty(PROPERTY_KEY)) {
			placemark.getProperty(PROPERTY_KEY).split('\n')
		} else {
			Collections.emptyList()
		}

		metadata = parseMetadata(properties)
	}

	abstract fun parseMetadata(properties: List<String>): HashMap<String, Any>

	fun getPlacemarkName(placemark: KmlPlacemark): String {

		return if (placemark.hasProperty("name")) {
			placemark.getProperty("name")
		} else {

			// If the name wasn't found in the properties return an empty string.
			Log.w("getPlacemarkName", "Placemark is missing name!")
			""
		}
	}

	companion object {
		private const val PROPERTY_KEY = "description"
	}

}