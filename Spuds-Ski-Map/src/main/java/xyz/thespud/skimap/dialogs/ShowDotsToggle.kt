@file:Suppress("DEPRECATION")

package xyz.thespud.skimap.dialogs

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.widget.Toast
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

	constructor(infoMap: InfoMapActivity, enabledDrawable: Drawable, disabledDrawable: Drawable,
	            enabledText: CharSequence, disabledText: CharSequence, itemEnabled: Boolean):
			super(infoMap.activity, enabledDrawable, disabledDrawable, enabledText, disabledText, itemEnabled) {

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