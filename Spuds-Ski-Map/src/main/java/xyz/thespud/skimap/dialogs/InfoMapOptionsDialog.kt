package xyz.thespud.skimap.dialogs

import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import xyz.thespud.skimap.R
import xyz.thespud.skimap.activities.InfoMapActivity

open class InfoMapOptionsDialog(private val infoMapActivity: InfoMapActivity): MapOptionsDialog(
	infoMapActivity.activity.layoutInflater, R.layout.info_map_options_v2, infoMapActivity) {

	private var showDotsImage: MapOptionItem? = null

	@Suppress("DEPRECATION")
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
			var toast = Toast.makeText(infoMapActivity.activity, R.string.toggling_dots, Toast.LENGTH_LONG)
			toast.show()
			infoMapActivity.showDots = !infoMapActivity.showDots

			infoMapActivity.activity.lifecycleScope.launch {
				for (run in infoMapActivity.loadedSkiRuns) {

					if (infoMapActivity.showDots) {
						for (circle in run.circles.value) { circle.isVisible = true }

						run.polyline.value.isVisible = false
					} else {
						if (run.circles.isInitialized()) {
							for (circle in run.circles.value) { circle.isVisible = false }
						}

						run.polyline.value.isVisible = true
					}
				}
				toast.cancel()
				toast = Toast.makeText(infoMapActivity.activity, R.string.done, Toast.LENGTH_SHORT)
				toast.show()
			}

			showDotsButton.toggleOptionVisibility()
		}
		showDotsImage = showDotsButton

		return view
	}
}