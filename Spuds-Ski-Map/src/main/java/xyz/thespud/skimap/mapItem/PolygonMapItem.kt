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
		val tag = "parseMetadata"
		for (property in properties) {
			Log.v(tag, "Parsing property '$property' for $name")
			if (property.startsWith(LIFTLINE_RUN_KEY)) {
				val allLifts = property.split(":")[1]
				val liftlines = mutableListOf<String>()
				for (lift in allLifts.split(",")) {
					val liftName = lift.trim()
					Log.v(tag, "Adding $name to liftline of $liftName")
					liftlines.add(liftName)
				}
				metadata[LIFTLINE_RUN_KEY] = liftlines.toList()
			}
		}
	}

	fun isLiftlineRun(chairliftName: String): Boolean {
		val liftlines = metadata[LIFTLINE_RUN_KEY]
		if (liftlines == null || liftlines !is List<*>) { return false }

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
