package xyz.thespud.skimap.services

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.fragment.app.FragmentActivity
import xyz.thespud.skimap.R
import kotlin.reflect.KClass
import androidx.core.graphics.createBitmap
import xyz.thespud.skimap.activities.LiveMapActivity
import xyz.thespud.skimap.mapItem.Locations
import xyz.thespud.skimap.mapItem.MapMarker

open class SkierLocationService : Service(), LocationListener {

	private var binder: IBinder? = LocalBinder()

	private var serviceCallbacks: ServiceCallbacks? = null

	inner class LocalBinder: Binder() {
		fun getService(): SkierLocationService = this@SkierLocationService
	}

	private lateinit var locationManager: LocationManager

	private lateinit var notificationManager: NotificationManager

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		val tag = "SkierLocationService"
		Log.v(tag, "onStartCommand called!")
		super.onStartCommand(intent, flags, startId)

		if (intent == null) {
			Log.w(tag, "SkierLocationService started without intent")
			return START_NOT_STICKY
		}

		val action = intent.action
		if (action == null) {
			Log.w(tag, "SkierLocationService intent missing action")
			return START_NOT_STICKY
		}
		Log.v(tag, action)

		val callback = serviceCallbacks
		if (callback == null) {
			Log.w(tag, "Service callback is null!")
			return START_NOT_STICKY
		}

		when (action) {
			START_TRACKING_INTENT -> {
				Log.d(tag, "Starting foreground service")
				val notification: Notification = createPersistentNotification(callback.getLaunchingActivity(),
					"", null, "")
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
					startForeground(TRACKING_SERVICE_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
				} else {
					startForeground(TRACKING_SERVICE_ID, notification)
				}
				callback.setIsTracking(true)
			}
			STOP_TRACKING_INTENT -> {
				Log.d(tag, "Stopping foreground service")
				callback.setManuallyDisabled(true)
				stopService()
			}
			else -> Log.w(tag, "Unknown intent action: $action")
		}

		return START_NOT_STICKY
	}

	override fun onCreate() {
		Log.v("SkierLocationService", "onCreate called!")
		super.onCreate()

		// If we don't have permission to track user location somehow at this spot just return early.
		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
			!= PackageManager.PERMISSION_GRANTED) {
			Log.w("SkierLocationService", "Service started before permissions granted!")
			return
		}

		val manager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
		notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			val trackingNotificationChannel = NotificationChannel(
				TRACKING_SERVICE_CHANNEL_ID,
				getString(R.string.tracking_notification_channel_name), NotificationManager.IMPORTANCE_LOW)

			val progressNotificationChannel = NotificationChannel(
				ACTIVITY_SUMMARY_CHANNEL_ID,
				getString(R.string.activity_summary_notification_channel_name), NotificationManager.IMPORTANCE_DEFAULT)

			val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
			notificationManager.createNotificationChannels(listOf(trackingNotificationChannel, progressNotificationChannel))
			Log.v("onCreate", "Created new notification channel")
		}

		if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 1F, this)
		}

		locationManager = manager

		Toast.makeText(this, R.string.starting_tracking, Toast.LENGTH_SHORT).show()
	}

	fun setCallbacks(callbacks: ServiceCallbacks?) {
		serviceCallbacks = callbacks
	}

	override fun onDestroy() {
		Log.v("SkierLocationService", "onDestroy has been called!")
		super.onDestroy()

		serviceCallbacks?.setIsTracking(false)
		locationManager.removeUpdates(this)
		notificationManager.cancel(TRACKING_SERVICE_ID)

		binder = null
	}

	override fun onLocationChanged(location: Location) {
		Log.v("SkierLocationService", "Location updated")
		val localServiceCallback = serviceCallbacks ?: return

		// If we are not on the mountain stop the tracking.
		if (!localServiceCallback.isInBounds(location)) {
			Toast.makeText(this, R.string.out_of_bounds, Toast.LENGTH_LONG).show()
			notificationManager.cancel(TRACKING_SERVICE_ID)
			Log.d("SkierLocationService", "Stopping location tracking service")
			stopService()
			return
		}

		Locations.updateLocations(location)

		var mapMarker = localServiceCallback.getOnLocation(location)
		if (mapMarker != null) {
			displaySkiingActivity(R.string.current_chairlift, mapMarker)
			return
		}

		mapMarker = localServiceCallback.getInLocation(location)
		if (mapMarker != null) {
			displaySkiingActivity(R.string.current_other, mapMarker)
			return
		}

		updateTrackingNotification(getString(R.string.tracking_notice), null)
	}

	private fun displaySkiingActivity(@StringRes textResource: Int, mapMarker: MapMarker) {
		val localServiceCallback = serviceCallbacks ?: return
		val text: String = getString(textResource, mapMarker.name)
		localServiceCallback.updateMapMarker(text)
		updateTrackingNotification(text, mapMarker.icon)
	}

	private fun updateTrackingNotification(title: String, @DrawableRes icon: Int?) {

		val callback = serviceCallbacks
		if (callback == null) {
			Log.w("updateTrackingNtifictin", "Service callback is null")
			return
		}

		val bitmap: Bitmap? = if (icon != null) {
			drawableToBitmap(AppCompatResources.getDrawable(this, icon)!!)
		} else {
			null
		}

		val notification: Notification = createPersistentNotification(callback.getLaunchingActivity(),
			title, bitmap, "")

		// Make sure we arent setting the same notification
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			for (shownNotification in notificationManager.activeNotifications) {
				val shownNotificationText = shownNotification.notification.extras.getString(
					Notification.EXTRA_TEXT)
				val notificationText = notification.extras.getString(Notification.EXTRA_TEXT)
				if (shownNotificationText == notificationText) {
					return
				}
			}
		}
		notificationManager.notify(TRACKING_SERVICE_ID, notification)
	}


	private fun createPersistentNotification(activityToLaunch: FragmentActivity, title: String,
	                                         iconBitmap: Bitmap?, fileToOpen: String): Notification {

		val stopIntent = Intent(this, SkierLocationService::class.java)
		stopIntent.action = STOP_TRACKING_INTENT

		val notificationIntent = Intent(this, activityToLaunch::class.java)
		notificationIntent.putExtra(ACTIVITY_SUMMARY_LAUNCH_DATE, fileToOpen)

		val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0,
			notificationIntent, PendingIntent.FLAG_IMMUTABLE)

		val builder: NotificationCompat.Builder = getNotificationBuilder(TRACKING_SERVICE_CHANNEL_ID,
			false, R.string.tracking_notice, pendingIntent)
			.setContentText(title)
			.addAction(0, "Stop Tracking", PendingIntent.getService(this,
				0, stopIntent,
				PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE))

		if (iconBitmap != null) {
			builder.setLargeIcon(iconBitmap)
		}

		return builder.build()
	}

	private fun getNotificationBuilder(channelId: String, showTime: Boolean, @StringRes titleText: Int,
	                                   pendingIntent: PendingIntent): NotificationCompat.Builder {

		return NotificationCompat.Builder(this, channelId)
			.setSmallIcon(applicationInfo.icon)
			.setShowWhen(showTime)
			.setContentTitle(getString(titleText))
			.setContentIntent(pendingIntent)
	}

	fun stopService() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			stopForeground(STOP_FOREGROUND_REMOVE)
		} else {
			@Suppress("DEPRECATION")
			stopForeground(true)
		}
		stopSelf()
	}

	override fun onBind(intent: Intent?): IBinder? {
		return binder
	}

	override fun onProviderEnabled(provider: String) {}

	override fun onProviderDisabled(provider: String) {}

	@Deprecated("This callback will never be invoked on Android Q and above.")
	override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

	companion object {

		const val TRACKING_SERVICE_ID = 69

		const val ACTIVITY_SUMMARY_ID = 420

		const val TRACKING_SERVICE_CHANNEL_ID = "skiAppTracker"

		const val ACTIVITY_SUMMARY_CHANNEL_ID = "skiAppProgress"

		const val ACTIVITY_SUMMARY_LAUNCH_DATE = "activitySummaryLaunchDate"

		const val STOP_TRACKING_INTENT = "com.mtspokane.skiapp.SkierLocationService.Stop"

		const val START_TRACKING_INTENT = "com.mtspokane.skiapp.SkierLocationService.Start"

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
}
