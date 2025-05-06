package xyz.thespud.skimap.services

import android.Manifest
import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.app.ActivityCompat
import xyz.thespud.skimap.R
import xyz.thespud.skimap.mapItem.Locations
import xyz.thespud.skimap.mapItem.MapMarker

open class SkierLocationService : Service(), LocationListener {

	private var binder: IBinder? = LocalBinder()

	private var serviceCallbacks: ServiceCallbacks? = null

	inner class LocalBinder: Binder() {
		fun getService(): SkierLocationService = this@SkierLocationService
	}

	private lateinit var locationManager: LocationManager

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
				val notification: Notification = SkiingNotification.createPersistentNotification(
					this, callback.getLaunchingActivity(), "", null, "")
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
					startForeground(SkiingNotification.TRACKING_SERVICE_ID, notification,
						ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
				} else {
					startForeground(SkiingNotification.TRACKING_SERVICE_ID, notification)
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
		SkiingNotification.setupNotificationChannels(this)

		if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 1F, this)
		}

		locationManager = manager

		// FIXME Database setup here?
		/*
		val database = Room.databaseBuilder(this, Database::class.java, Database.NAME)
			.allowMainThreadQueries().build()
		databaseDao = database.skiingActivityDao()

		val todaysDate: String = Database.getTodaysDate()
		val longDate: String = Database.getLongDateFromLong(Date().time)

		var skiingDateAndActivities = databaseDao.getSkiingDateWithActivitiesByShortDate(todaysDate)
		while (skiingDateAndActivities == null) {
			databaseDao.addSkiingDate(LongAndShortDate(longDate, todaysDate))
			skiingDateAndActivities = databaseDao.getSkiingDateWithActivitiesByShortDate(todaysDate)
		}
		skiingDate = skiingDateAndActivities.skiingDate
		 */

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
		SkiingNotification.cancelTrackingNotification(this)

		binder = null
	}

	override fun onLocationChanged(location: Location) {
		val tag = "SkierLocationService"
		Log.v(tag, "Location updated")
		val localServiceCallback = serviceCallbacks ?: return

		// If we are not on the mountain stop the tracking.
		if (!localServiceCallback.isInBounds(location)) {
			Toast.makeText(this, R.string.out_of_bounds,
				Toast.LENGTH_LONG).show()
			SkiingNotification.cancelTrackingNotification(this)
			Log.d(tag, "Stopping location tracking service")
			stopService()
			return
		}

		// FIXME Insert into the database here!
		/*
		val skiingActivity = SkiingActivity(
			location.accuracy,
			location.altitude,
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
				location.mslAltitudeAccuracyMeters
			} else { null },
			location.latitude,
			location.longitude,
			location.speed,
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
				location.speedAccuracyMetersPerSecond
			} else { null },
			location.time,
			skiingDate.id
		)
		databaseDao.addSkiingActivity(skiingActivity)
		 */

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


		val callback = serviceCallbacks
		if (callback != null) {
			SkiingNotification.updateTrackingNotification(this, callback, getString(R.string.tracking_notice),
				null)
		} else {
			Log.w(tag, "Cannot update tracking notification - service callback is null")
		}
	}

	private fun displaySkiingActivity(@StringRes textResource: Int, mapMarker: MapMarker) {
		val localServiceCallback = serviceCallbacks ?: return
		val text: String = getString(textResource, mapMarker.name)
		localServiceCallback.updateMapMarker(text)
		SkiingNotification.updateTrackingNotification(this, localServiceCallback, text, mapMarker.icon)
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

		const val STOP_TRACKING_INTENT = "xyz.thespud.skimap.SkierLocationService.Stop"

		const val START_TRACKING_INTENT = "xyz.thespud.skimap.SkierLocationService.Start"

	}
}
