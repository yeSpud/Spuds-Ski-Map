package xyz.thespud.skimap.mapItem

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.data.kml.KmlPlacemark
import xyz.thespud.skimap.R

class PolygonMapItem: MapItem {

	val points: List<LatLng>

	constructor(placemark: KmlPlacemark, icon: Int = R.drawable.ic_missing, points: List<LatLng>):
			super(placemark, icon) { this.points = points }

	constructor(name: String): super(name) { this.points = emptyList() }

	override fun parseMetadata(properties: List<String>) {
		//val metadata = hashMapOf<String, Any>()
		for (property in properties) {
			Log.v("parseMetadata", "Parsing property $property")
			when (property) {
				LIFTLINE_RUN_KEY -> {
					val allLifts = property.split(":")[1]
					metadata[LIFTLINE_RUN_KEY] = allLifts.split(",").forEach { it.trim() }
				}
			}
		}
		//return metadata
	}

	fun isLiftlineRun(chairliftName: String): Boolean {
		val liftlines = metadata[LIFTLINE_RUN_KEY] as? List<String>? ?: return false

		for (liftline in liftlines) {
			if (liftline == chairliftName) {
				return true
			}
		}

		return false
	}

	companion object {
		const val LIFTLINE_RUN_KEY = "liftline"
	}

}
