@file:Suppress("DEPRECATION")

package xyz.thespud.skimap.dialogs

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.content.res.AppCompatResources
import xyz.thespud.skimap.R
import xyz.thespud.skimap.activities.MapHandler

class SkiliftsToggle: PolylineToggle {

	/*
	override val clickListener by lazy {
		val locationManager = mapHandler.locationManager ?: throw IllegalStateException("Location manager is null for ski option toggle")

		OnMapOptionItemClicked(locationManager.chairliftPolylines, mapHandler)
	}
	 */

	@Deprecated("When using this constructor be sure to call setMapHandler() in your code")
	constructor(context: Context): this(context, null)
	@Deprecated("When using this constructor be sure to call setMapHandler() in your code")
	constructor(context: Context, attributeSet: AttributeSet?): this(context, attributeSet, 0)
	@Deprecated("When using this constructor be sure to call setMapHandler() in your code")
	constructor(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int) : super(context, attributeSet, defStyleAttr)

	constructor(context: Context, mapHandler: MapHandler): super(context, AppCompatResources.getDrawable(context,
		R.drawable.ic_chairlift)!!, AppCompatResources.getDrawable(context,
		R.drawable.ic_chairlift_disabled)!!, context.getString(R.string.show_chairlifts),
		context.getString(R.string.hide_chairlifts), true) {

		setMapHandler(mapHandler)

		setOnClickListener {
			val locationManager = mapHandler.locationManager ?: throw IllegalStateException("Location manager is null for ski option toggle")

			OnMapOptionItemClicked(locationManager.chairliftPolylines, mapHandler)
		}
	}
}