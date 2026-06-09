package xyz.thespud.skimap.dialogs

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import xyz.thespud.skimap.R
import xyz.thespud.skimap.activities.MapHandler
import xyz.thespud.skimap.mapItem.PolylineMapItem

open class MapOptionsDialog(private val layoutInflater: LayoutInflater, @LayoutRes private val menu: Int,
                            private val map: MapHandler) : BaseAdapter() {

	private var showChairliftImage: MapOptionItem? = null

	private var showGreenRunsImage: MapOptionItem? = null

	private var showBlueRunsImage: MapOptionItem? = null

	private var showBlackRunsImage: MapOptionItem? = null

	private var showDoubleBlackRunsImage: MapOptionItem? = null

	private var showTerrainParksImage: MapOptionItem? = null

	private var showNightRunsImage: MapOptionItem? = null

	override fun getCount(): Int {
		return 1
	}

	override fun getItem(position: Int): Any {
		return position // Todo properly implement me?
	}

	override fun getItemId(position: Int): Long {
		return position.toLong() // Todo properly implement me?
	}

	override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {

		val view: View = convertView ?: layoutInflater.inflate(menu, parent, false)

		val locationManager = map.locationManager
		if (locationManager != null) {
			if (showChairliftImage == null) {
				showChairliftImage =
					getRunOption(view, R.id.show_chairlift, locationManager.chairliftPolylines, map)
			}

			if (showGreenRunsImage == null) {
				showGreenRunsImage =
					getRunOption(view, R.id.show_green_runs, locationManager.greenRunPolylines, map)

			}

			if (showBlueRunsImage == null) {
				showBlueRunsImage =
					getRunOption(view, R.id.show_blue_runs, locationManager.blueRunPolylines, map)
			}

			if (showBlackRunsImage == null) {
				showBlackRunsImage =
					getRunOption(view, R.id.show_black_runs, locationManager.blackRunPolylines, map)
			}

			if (showDoubleBlackRunsImage == null) {
				showDoubleBlackRunsImage = getRunOption(view, R.id.show_double_black_runs,
					locationManager.doubleBlackRunPolylines, map)
			}

			if (showTerrainParksImage == null) {
				showTerrainParksImage = getRunOption(
					view,
					R.id.show_terrain_parks,
					emptyList(),
					map
				) // FIXME Add terrain park
			}
		}

		if (showNightRunsImage == null) {
			val nightRunImage: MapOptionItem? = view.findViewById(R.id.show_night_runs)
			if (nightRunImage == null) {
				Log.w("getView", "Unable to find night run option")
				return view
			}

			nightRunImage.setOnClickListener {
				if (it == null || it !is MapOptionItem) {
					return@setOnClickListener
				}

				with(map) {

					isNightOnly = !isNightOnly

					if (locationManager != null) {
						for (chairliftPolyline in locationManager.chairliftPolylines) {
							chairliftPolyline.togglePolyLineVisibility(chairliftPolyline.defaultVisibility,
								isNightOnly)
						}

						for (greenRunPolyline in locationManager.greenRunPolylines) {
							greenRunPolyline.togglePolyLineVisibility(greenRunPolyline.defaultVisibility,
								isNightOnly)
						}

						for (blueRunPolyline in locationManager.blueRunPolylines) {
							blueRunPolyline.togglePolyLineVisibility(blueRunPolyline.defaultVisibility,
								isNightOnly)
						}

						for (blackRunPolyline in locationManager.blackRunPolylines) {
							blackRunPolyline.togglePolyLineVisibility(blackRunPolyline.defaultVisibility,
								isNightOnly)
						}

						for (doubleBlackRunPolyline in locationManager.doubleBlackRunPolylines) {
							doubleBlackRunPolyline.togglePolyLineVisibility(doubleBlackRunPolyline.defaultVisibility,
								isNightOnly)
						}
					}
				}

				it.toggleOptionVisibility()
			}
			showNightRunsImage = nightRunImage
		}

		return view
	}

	companion object {
		private fun getRunOption(view: View, @IdRes resId: Int, runs: List<PolylineMapItem>,
		                         map: MapHandler): MapOptionItem? {

			val optionsView: MapOptionItem? = view.findViewById(resId)
			if (optionsView == null) {
				Log.w("getRunObject", "Unable to find that option!")
				return null
			}

			optionsView.setOnClickListener(OnMapOptionItemClicked(runs, map))
			return optionsView
		}
	}
}