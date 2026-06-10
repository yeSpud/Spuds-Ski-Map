package xyz.thespud.skimap.mapItem

import android.util.Log
import androidx.annotation.DrawableRes
import com.google.maps.android.data.kml.KmlPlacemark
import xyz.thespud.skimap.R
import java.util.Collections

abstract class MapItem {

	val name: String

	@DrawableRes
	val icon: Int

	val metadata = HashMap<String, Any>()

	constructor(placemark: KmlPlacemark, icon: Int) {
		name = getPlacemarkName(placemark)
		this.icon = icon

		val properties: List<String> = if (placemark.hasProperty(PROPERTY_KEY)) {
			placemark.getProperty(PROPERTY_KEY).split('\n')
		} else {
			Collections.emptyList()
		}

		parseMetadata(properties)
	}

	constructor(name: String) {
		this.name = name
		this.icon = R.drawable.ic_missing
	}

	abstract fun parseMetadata(properties: List<String>)

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