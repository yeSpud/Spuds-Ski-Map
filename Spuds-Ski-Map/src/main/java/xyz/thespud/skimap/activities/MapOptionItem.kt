package xyz.thespud.skimap.activities

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.UiThread
import androidx.core.graphics.drawable.DrawableCompat
import xyz.thespud.skimap.R

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