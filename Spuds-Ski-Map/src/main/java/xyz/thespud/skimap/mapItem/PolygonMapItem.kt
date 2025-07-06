package xyz.thespud.skimap.mapItem

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.data.kml.KmlPlacemark
import xyz.thespud.skimap.R

class PolygonMapItem(
	placemark: KmlPlacemark,
	icon: Int = R.drawable.ic_missing,
	val points: List<LatLng>,
): MapItem(placemark, icon) {

	override fun parseMetadata(properties: List<String>): HashMap<String, Any> {
		val metadata = hashMapOf<String, Any>()
		for (property in properties) {
			Log.v("parseMetadata", "Parsing property $property")
			when (property) {

			}
		}
		return metadata
	}

}
