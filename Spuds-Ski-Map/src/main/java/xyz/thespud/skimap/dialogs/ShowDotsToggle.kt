@file:Suppress("DEPRECATION")

package xyz.thespud.skimap.dialogs

import android.content.Context
import android.util.AttributeSet
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import xyz.thespud.skimap.R
import xyz.thespud.skimap.activities.InfoMapActivity

class ShowDotsToggle: MapOptionItem {

	private lateinit var infoMapActivity: InfoMapActivity

	@Deprecated("When using this constructor be sure to call setInfoMap() in your code")
	constructor(context: Context): this(context, null)
	@Deprecated("When using this constructor be sure to call setInfoMap() in your code")
	constructor(context: Context, attributeSet: AttributeSet?): this(context, attributeSet, 0)
	@Deprecated("When using this constructor be sure to call setInfoMap() in your code")
	constructor(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int) : super(context, attributeSet, defStyleAttr)

	constructor(infoMap: InfoMapActivity): super(infoMap.activity,
		AppCompatResources.getDrawable(infoMap.activity, R.drawable.ic_hide_dots)!!,
		AppCompatResources.getDrawable(infoMap.activity, R.drawable.ic_show_dots)!!,
		infoMap.activity.getString(R.string.show_dots), infoMap.activity.getString(R.string.hide_dots),
		false) {

		infoMapActivity = infoMap

		setOnClickListener {

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

			toggleOptionVisibility()
		}
	}

	fun setInfoMap(infoMap: InfoMapActivity) { infoMapActivity = infoMap }
}