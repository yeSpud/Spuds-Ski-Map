@file:Suppress("DEPRECATION")

package xyz.thespud.skimap.dialogs

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import xyz.thespud.skimap.activities.MapHandler

abstract class PolylineToggle: MapOptionItem {

	lateinit var mapHandler: MapHandler
		private set

	abstract val clickListener: OnClickListener

	@Deprecated("When using this constructor be sure to call setMapHandler() in your code")
	constructor(context: Context): this(context, null)
	@Deprecated("When using this constructor be sure to call setMapHandler() in your code")
	constructor(context: Context, attributeSet: AttributeSet?): this(context, attributeSet, 0)
	@Deprecated("When using this constructor be sure to call setMapHandler() in your code")
	constructor(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int) : super(context, attributeSet, defStyleAttr)

	constructor(context: Context, enabledDrawable: Drawable, disabledDrawable: Drawable,
	            enabledText: CharSequence, disabledText: CharSequence, itemEnabled: Boolean):
			super(context, enabledDrawable, disabledDrawable, enabledText, disabledText, itemEnabled) {

				setOnClickListener(clickListener)
			}

	fun setMapHandler(mapHandler: MapHandler) {
		this.mapHandler = mapHandler
	}

}