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

	override fun parseMetadata(properties: List<String>): List<Metadata> {
		val tag = "parseMetadata"
		val mutableMetadata = mutableListOf<Metadata>()

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
				mutableMetadata.add(Liftline(liftlines))
			}
		}

		return mutableMetadata
	}

	fun isLiftlineRun(skilift: PolygonMapItem): Boolean {
		val liftlines = metadata.get<Liftline>() ?: return false

		for (skiliftName in liftlines.skiliftNames) {
			if (skiliftName == skilift.name) { return true }
		}

		return false
	}

	companion object {
		const val LIFTLINE_RUN_KEY = "liftline"
	}

}
