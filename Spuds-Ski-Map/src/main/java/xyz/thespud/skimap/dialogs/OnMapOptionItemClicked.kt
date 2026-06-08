package xyz.thespud.skimap.dialogs

import android.view.View
import xyz.thespud.skimap.activities.MapHandler
import xyz.thespud.skimap.mapItem.PolylineMapItem

class OnMapOptionItemClicked(private val polylineMapItems: List<PolylineMapItem>, private val map: MapHandler): View.OnClickListener {

	override fun onClick(mapOptionItem: View?) {

		if (mapOptionItem == null || mapOptionItem !is MapOptionItem) {
			return
		}

		for (polylineMapItem in polylineMapItems) {
			polylineMapItem.togglePolyLineVisibility(!polylineMapItem.defaultVisibility, map.isNightOnly)
		}

		mapOptionItem.toggleOptionVisibility()
	}
}