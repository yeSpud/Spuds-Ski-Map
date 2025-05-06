package xyz.thespud.skimap.activities

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.annotation.UiThread
import androidx.core.graphics.drawable.DrawableCompat
import xyz.thespud.skimap.R
import xyz.thespud.skimap.mapItem.PolylineMapItem

class MapOptionItem: LinearLayout {

	private val icon: ImageView
	private val text: TextView

	private val enabledDrawable: Drawable
	private val disabledDrawable: Drawable

	private val enabledText: CharSequence
	private val disabledText: CharSequence

	var itemEnabled: Boolean
		private set

	constructor(context: Context): this(context, null)

	constructor(context: Context, attributeSet: AttributeSet?): this(context, attributeSet, 0)

	constructor(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int) : super(context,
		attributeSet, defStyleAttr) {

		inflate(context, R.layout.menu_dialog_entry, this)

		icon = findViewById(R.id.menu_entry_icon)
		text = findViewById(R.id.menu_entry_text)

		context.theme.obtainStyledAttributes(attributeSet, R.styleable.MapOptions, 0,
			0).apply {

			try {

				enabledText = getText(R.styleable.MapOptions_enabled_menu_title)
				disabledText = getText(R.styleable.MapOptions_disabled_menu_title)

				enabledDrawable = getDrawable(R.styleable.MapOptions_enabled_menu_icon)!!
				disabledDrawable = getDrawable(R.styleable.MapOptions_disabled_menu_icon)!!

				itemEnabled = !getBoolean(R.styleable.MapOptions_menu_enable_by_default, true)
				toggleOptionVisibility()
			} finally {
				recycle()
			}
		}
	}

	constructor(context: Context, enabledDrawable: Drawable, disabledDrawable: Drawable,
	            enabledText: CharSequence, disabledText: CharSequence, itemEnabled: Boolean = true): super(context, null,
		0) {
		inflate(context, R.layout.menu_dialog_entry, this)

		icon = findViewById(R.id.menu_entry_icon)
		text = findViewById(R.id.menu_entry_text)

		this.enabledDrawable = enabledDrawable
		this.disabledDrawable = disabledDrawable
		this.enabledText = enabledText
		this.disabledText = disabledText
		this.itemEnabled = itemEnabled
		toggleOptionVisibility()
	}

	@UiThread
	fun toggleOptionVisibility() {

		if (itemEnabled) {
			text.text = disabledText
			icon.setImageDrawable(DrawableCompat.wrap(disabledDrawable))
		} else {
			text.text = enabledText
			icon.setImageDrawable(DrawableCompat.wrap(enabledDrawable))
		}

		itemEnabled = !itemEnabled
		invalidate()
	}
}

class OnMapItemClicked(private val polylineMapItems: List<PolylineMapItem>, private val map: MapHandler): View.OnClickListener {

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

		if (showChairliftImage == null) {
			showChairliftImage = getRunOption(view, R.id.show_chairlift, map.chairliftPolylines, map)
		}

		if (showGreenRunsImage == null) {
			showGreenRunsImage = getRunOption(view, R.id.show_green_runs, map.greenRunPolylines, map)

		}

		if (showBlueRunsImage == null) {
			showBlueRunsImage = getRunOption(view, R.id.show_blue_runs, map.blueRunPolylines, map)
		}

		if (showBlackRunsImage == null) {
			showBlackRunsImage = getRunOption(view, R.id.show_black_runs, map.blackRunPolylines, map)
		}

		if (showDoubleBlackRunsImage == null) {
			showDoubleBlackRunsImage = getRunOption(view, R.id.show_double_black_runs, map.doubleBlackRunPolylines,
				map)
		}

		if (showTerrainParksImage == null) {
			showTerrainParksImage = getRunOption(view, R.id.show_terrain_parks, emptyList(), map) // FIXME Add terrain park
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

					for (chairliftPolyline in chairliftPolylines) {
						chairliftPolyline.togglePolyLineVisibility(chairliftPolyline.defaultVisibility,
							isNightOnly)
					}

					for (greenRunPolyline in greenRunPolylines) {
						greenRunPolyline.togglePolyLineVisibility(greenRunPolyline.defaultVisibility,
							isNightOnly)
					}

					for (blueRunPolyline in blueRunPolylines) {
						blueRunPolyline.togglePolyLineVisibility(blueRunPolyline.defaultVisibility,
							isNightOnly)
					}

					for (blackRunPolyline in blackRunPolylines) {
						blackRunPolyline.togglePolyLineVisibility(blackRunPolyline.defaultVisibility,
							isNightOnly)
					}

					for (doubleBlackRunPolyline in doubleBlackRunPolylines) {
						doubleBlackRunPolyline.togglePolyLineVisibility(doubleBlackRunPolyline.defaultVisibility,
							isNightOnly)
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

			optionsView.setOnClickListener(OnMapItemClicked(runs, map))
			return optionsView
		}
	}
}
