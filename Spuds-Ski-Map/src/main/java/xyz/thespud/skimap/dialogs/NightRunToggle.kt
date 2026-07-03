@file:Suppress("DEPRECATION")

package xyz.thespud.skimap.dialogs

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.content.res.AppCompatResources
import xyz.thespud.skimap.R
import xyz.thespud.skimap.activities.MapHandler

class NightRunToggle: PolylineToggle {

	override val clickListener by lazy {

		OnClickListener {

			val locationManager = mapHandler.locationManager
				?: throw IllegalStateException("Location manager is null for night runs toggle")

			mapHandler.isNightOnly = !mapHandler.isNightOnly

			for (chairliftPolyline in locationManager.chairliftPolylines) {
				chairliftPolyline.togglePolyLineVisibility(chairliftPolyline.defaultVisibility,
					mapHandler.isNightOnly)
			}

			for (greenRunPolyline in locationManager.greenRunPolylines) {
				greenRunPolyline.togglePolyLineVisibility(greenRunPolyline.defaultVisibility,
					mapHandler.isNightOnly)
			}

			for (blueRunPolyline in locationManager.blueRunPolylines) {
				blueRunPolyline.togglePolyLineVisibility(blueRunPolyline.defaultVisibility,
					mapHandler.isNightOnly)
			}

			for (blackRunPolyline in locationManager.blackRunPolylines) {
				blackRunPolyline.togglePolyLineVisibility(blackRunPolyline.defaultVisibility,
					mapHandler.isNightOnly)
			}

			for (doubleBlackRunPolyline in locationManager.doubleBlackRunPolylines) {
				doubleBlackRunPolyline.togglePolyLineVisibility(doubleBlackRunPolyline.defaultVisibility,
					mapHandler.isNightOnly)
			}

			toggleOptionVisibility()
		}
	}

	@Deprecated("When using this constructor be sure to call setMapHandler() in your code")
	constructor(context: Context): this(context, null)
	@Deprecated("When using this constructor be sure to call setMapHandler() in your code")
	constructor(context: Context, attributeSet: AttributeSet?): this(context, attributeSet, 0)
	@Deprecated("When using this constructor be sure to call setMapHandler() in your code")
	constructor(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int) : super(context, attributeSet, defStyleAttr)

	constructor(context: Context, mapHandler: MapHandler): super(context, AppCompatResources.getDrawable(context,
		R.drawable.ic_night)!!, AppCompatResources.getDrawable(context,
		R.drawable.ic_sun)!!, context.getString(R.string.night_runs),
		context.getString(R.string.all_runs), true) {

		setMapHandler(mapHandler)
	}
}