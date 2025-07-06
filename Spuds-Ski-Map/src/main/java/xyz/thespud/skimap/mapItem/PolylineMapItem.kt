package xyz.thespud.skimap.mapItem

import android.util.Log
import com.google.android.gms.maps.model.Polyline
import com.google.maps.android.data.kml.KmlPlacemark
import xyz.thespud.skimap.R

class PolylineMapItem(placemark: KmlPlacemark, icon: Int = R.drawable.ic_missing): MapItem(placemark, icon) {

	val polylines: MutableList<Polyline> = mutableListOf()

	var defaultVisibility = true
		private set

	fun clearPolylines() {
		for (polyline in polylines) {
			polyline.remove()
		}
		polylines.clear()
	}

	/**
	 * Default visibility | Nights Only | Night Run | Output
	 *        0	                 0	         0	        0
	 *        0	                 0	         1	        0
	 *        0	                 1	         0	        0
	 *        0	                 1	         1	        0
	 *        1	                 0	         0	        1
	 *        1	                 0	         1	        1
	 *        1	                 1	         0	        0
	 *        1	                 1	         1	        1
	 */
	fun togglePolyLineVisibility(visible: Boolean, nightRunsOnly: Boolean) {
		defaultVisibility = visible

		for (polyline in polylines) {
			polyline.isVisible = defaultVisibility && (metadata[NIGHT_RUN_KEY] == nightRunsOnly)
		}
	}

	override fun parseMetadata(properties: List<String>): HashMap<String, Any> {
		val metadata = hashMapOf<String, Any>()
		for (property in properties) {
			Log.v("parseMetadata", "Parsing property $property")
			when (property) {
				NIGHT_RUN_KEY -> metadata[NIGHT_RUN_KEY] = true
				EASIEST_WAY_DOWN_KEY -> metadata[EASIEST_WAY_DOWN_KEY] = true
			}
		}
		return metadata
	}

	companion object {
		const val NIGHT_RUN_KEY = "night run"
		const val EASIEST_WAY_DOWN_KEY = "easiest way down"
	}
}