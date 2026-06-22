package xyz.thespud.skimap.mapItem

import android.util.Log
import com.google.maps.android.data.kml.KmlPlacemark
import xyz.thespud.skimap.R
import java.util.Collections

abstract class MapItem: MapItemBase {

	val metadata: List<Metadata>

	constructor(placemark: KmlPlacemark, icon: Int): super(getPlacemarkName(placemark), icon) {

		val properties: List<String> = if (placemark.hasProperty(PROPERTY_KEY)) {
			placemark.getProperty(PROPERTY_KEY).split('\n')
		} else {
			Collections.emptyList()
		}

		metadata = parseMetadata(properties)
	}

	constructor(name: String): super(name, R.drawable.ic_missing) { metadata = emptyList() }

	abstract fun parseMetadata(properties: List<String>): List<Metadata>

	companion object {
		private const val PROPERTY_KEY = "description"

		fun getPlacemarkName(placemark: KmlPlacemark): String {

			return if (placemark.hasProperty("name")) {
				placemark.getProperty("name")
			} else {

				// If the name wasn't found in the properties return an empty string.
				Log.w("getPlacemarkName", "Placemark is missing name!")
				""
			}
		}
	}
}