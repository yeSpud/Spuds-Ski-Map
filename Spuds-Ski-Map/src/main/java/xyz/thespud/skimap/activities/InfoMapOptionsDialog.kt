package xyz.thespud.skimap.activities

import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import xyz.thespud.skimap.R

class InfoMapOptionsDialog(private val infoMapActivity: InfoMapActivity): MapOptionsDialog(
	infoMapActivity.activity.layoutInflater, R.layout.info_map_options, infoMapActivity) {

	private var showDotsImage: MapOptionItem? = null

	override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
		val view = super.getView(position, convertView, parent)

		if (showDotsImage != null) {
			return view
		}

		val showDotsButton: MapOptionItem? = view.findViewById(R.id.show_circles)
		if (showDotsButton == null) {
			Log.w("getView", "Unable to find show dots button")
			return view
		}

		showDotsButton.setOnClickListener {
			infoMapActivity.showDots = !infoMapActivity.showDots

			if (infoMapActivity.showDots) {
				infoMapActivity.activity.lifecycleScope.launch { infoMapActivity.addCirclesToMap() }
			} else {
				infoMapActivity.removeCircles()
			}

			showDotsButton.toggleOptionVisibility()
		}
		showDotsImage = showDotsButton

		return view
	}
}
