package xyz.thespud.skimap.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.NotificationCompat
import androidx.core.graphics.createBitmap
import androidx.fragment.app.FragmentActivity
import xyz.thespud.skimap.R
import xyz.thespud.skimap.mapItem.MapMarker
import kotlin.reflect.KClass

object SkiingNotification {

	const val TRACKING_SERVICE_ID = 69

	const val ACTIVITY_SUMMARY_ID = 420

	const val NOTIFICATION_PERMISSION = 1231

	const val ACTIVITY_SUMMARY_LAUNCH_DATE = "activitySummaryLaunchDate"

	const val TRACKING_SERVICE_CHANNEL_ID = "skiAppTracker"

	const val ACTIVITY_SUMMARY_CHANNEL_ID = "skiAppProgress"


	fun setupNotificationChannels(context: Context) {
		val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

		val trackingNotificationChannel = NotificationChannel(
			TRACKING_SERVICE_CHANNEL_ID,
			context.getString(R.string.tracking_notification_channel_name), NotificationManager.IMPORTANCE_LOW)

		val progressNotificationChannel = NotificationChannel(
			ACTIVITY_SUMMARY_CHANNEL_ID,
			context.getString(R.string.activity_summary_notification_channel_name), NotificationManager.IMPORTANCE_DEFAULT)

		notificationManager.createNotificationChannels(listOf(trackingNotificationChannel, progressNotificationChannel))
		Log.v("setupNotificationChannels", "Created new notification channel")
	}

	fun cancelTrackingNotification(context: Context) {
		val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
		notificationManager.cancel(TRACKING_SERVICE_ID)
	}

	fun displaySkiingActivity(context: Context, serviceCallbacks: ServiceCallbacks, @DrawableRes appIcon: Int,
	                          @StringRes textResource: Int, mapMarker: MapMarker) {
		val text: String = context.getString(textResource, mapMarker.name)
		serviceCallbacks.updateMapMarker(text)
		updateTrackingNotification(context, serviceCallbacks, appIcon, text, mapMarker.icon)
	}

	fun updateTrackingNotification(context: Context, serviceCallbacks: ServiceCallbacks, @DrawableRes appIcon: Int,
	                               title: String, @DrawableRes icon: Int?) {
		Log.v("updateTrackingNotification", "updateTrackingNotification called!")
		val bitmap: Bitmap? = if (icon != null) {
			drawableToBitmap(AppCompatResources.getDrawable(context, icon)!!)
		} else {
			null
		}

		val activity = serviceCallbacks.getLaunchingActivity()
		val notification: Notification = createTrackingNotification(context, activity, appIcon, title, bitmap)

		val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

		// Make sure we arent setting the same notification
		for (shownNotification in notificationManager.activeNotifications) {
			val shownNotificationText = shownNotification.notification.extras.getString(Notification.EXTRA_TEXT)
			val notificationText = notification.extras.getString(Notification.EXTRA_TEXT)
			if (shownNotificationText == notificationText) {
				return
			}
			Log.d("updateTrackingNotification", "Setting notification text to: $notificationText")
		}
		notificationManager.notify(TRACKING_SERVICE_ID, notification)
	}

	fun createActivityNotification(context: Context, activityToLaunch: KClass<out FragmentActivity>,
	                               @DrawableRes icon: Int, fileToOpen: String): Notification {
		val notificationIntent = Intent(context, activityToLaunch::class.java)
		notificationIntent.putExtra(ACTIVITY_SUMMARY_LAUNCH_DATE, fileToOpen)

		val pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent,
			PendingIntent.FLAG_IMMUTABLE)

		val builder: NotificationCompat.Builder = getNotificationBuilder(context, ACTIVITY_SUMMARY_CHANNEL_ID,
			icon, true, R.string.activity_notification_text, pendingIntent)

		return builder.build()
	}

	fun createActivityNotification(context: Context, activityToLaunch: KClass<out FragmentActivity>,
	                               @DrawableRes icon: Int, fileToOpen: Int): Notification {
		val notificationIntent = Intent(context, activityToLaunch::class.java)
		notificationIntent.putExtra(ACTIVITY_SUMMARY_LAUNCH_DATE, fileToOpen)

		val pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent,
			PendingIntent.FLAG_IMMUTABLE)

		val builder: NotificationCompat.Builder = getNotificationBuilder(context, ACTIVITY_SUMMARY_CHANNEL_ID,
			icon, true, R.string.activity_notification_text, pendingIntent)

		return builder.build()
	}

	fun createTrackingNotification(context: Context, activityToLaunch: FragmentActivity?, @DrawableRes appIcon: Int,
	                               title: String, iconBitmap: Bitmap?): Notification {
		var pendingIntent: PendingIntent? = null
		if (activityToLaunch != null) {
			val notificationIntent = Intent(context, activityToLaunch::class.java)
			pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent,
				PendingIntent.FLAG_IMMUTABLE)
		}

		val builder: NotificationCompat.Builder = getNotificationBuilder(context, TRACKING_SERVICE_CHANNEL_ID,
			appIcon, false, R.string.tracking_notice, pendingIntent)
			.setContentText(title)
			.addAction(getStopButton(context))

		if (iconBitmap != null) {
			builder.setLargeIcon(iconBitmap)
		}

		return builder.build()
	}

	private fun getStopButton(context: Context): NotificationCompat.Action {
		val stopIntent = Intent(context, SkierLocationService::class.java)
		stopIntent.action = SkierLocationService.STOP_TRACKING_INTENT

		val pendingIntent = PendingIntent.getService(context, 0, stopIntent,
			PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE)
		return NotificationCompat.Action(0, "Stop Tracking", pendingIntent)
	}

	private fun getNotificationBuilder(context: Context, channelId: String, @DrawableRes icon: Int,
	                                   showTime: Boolean, @StringRes titleText: Int, pendingIntent: PendingIntent?):
			NotificationCompat.Builder {

		return NotificationCompat.Builder(context, channelId)
			.setSmallIcon(icon) // context.applicationInfo.icon
			.setShowWhen(showTime)
			.setContentTitle(context.getString(titleText))
			.setContentIntent(pendingIntent)
	}

	/**
	 * @author https://studiofreya.com/2018/08/15/android-notification-large-icon-from-vector-xml/
	 */
	private fun drawableToBitmap(drawable: Drawable): Bitmap? {

		if (drawable is BitmapDrawable) {
			return drawable.bitmap
		}

		val bitmap: Bitmap = createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight)

		val canvas = Canvas(bitmap)
		drawable.setBounds(0, 0, canvas.width, canvas.height)
		drawable.draw(canvas)

		return bitmap
	}

}