package xyz.thespud.skimap.dialogs

import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter

class MapOptionAdapter(context: Context, private val items: ArrayList<MapOptionItem>): ArrayAdapter<MapOptionItem>(context, 0, items) {

	override fun getCount(): Int { return items.size }

	override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

		val mapOptionItem = getItem(position)

		if (mapOptionItem == null) {
			Log.e(TAG, "Map option item is null!")
			return super.getView(position, convertView, parent)
		}

		return mapOptionItem
	}

	companion object {
		private const val TAG = "MapOptionAdapter"
	}

}