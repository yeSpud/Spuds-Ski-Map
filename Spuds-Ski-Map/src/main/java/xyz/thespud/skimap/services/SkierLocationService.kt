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
import androidx.core.app.ActivityCompat
import xyz.thespud.skimap.R
import xyz.thespud.skimap.mapItem.Locations
import java.lang.ref.WeakReference

open class SkierLocationService : Service(), LocationListener {

	// FIXME Memory leak here
	private var binder: WeakReference<IBinder>? = WeakReference(LocalBinder())

	private var serviceCallbacks: WeakReference<ServiceCallbacks>? = null

	inner class LocalBinder: Binder() {
		fun getService(): SkierLocationService = this@SkierLocationService
	}

	private lateinit var locationManager: LocationManager

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		Log.v(TAG, "onStartCommand called!")
		super.onStartCommand(intent, flags, startId)

		if (intent == null) {
			Log.w(TAG, "SkierLocationService started without intent")
			return START_NOT_STICKY
		}

		val action = intent.action
		if (action == null) {
			Log.w(TAG, "SkierLocationService intent missing action")
			return START_NOT_STICKY
		}
		Log.v(TAG, action)

		when (action) {
			START_TRACKING_INTENT -> {
				Log.d(TAG, "Starting foreground service")
				val notification: Notification = SkiingNotification.createTrackingNotification(
					this, null, "", null)
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
					startForeground(SkiingNotification.TRACKING_SERVICE_ID, notification,
						ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
				} else {
					startForeground(SkiingNotification.TRACKING_SERVICE_ID, notification)
				}
			}
			STOP_TRACKING_INTENT -> {
				Log.d(TAG, "Stopping foreground service")
				stopService()
			}
			else -> Log.w(TAG, "Unknown intent action: $action")
		}

		return START_NOT_STICKY
	}

	override fun onCreate() {
		Log.v(TAG, "onCreate called!")
		super.onCreate()

		// If we don't have permission to track user location somehow at this spot just return early.
		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
			!= PackageManager.PERMISSION_GRANTED) {
			Log.w(TAG, "Service started before permissions granted!")
			return
		}

		val manager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
		SkiingNotification.setupNotificationChannels(this)

		if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 1F, this)
		}

		locationManager = manager

		Toast.makeText(this, R.string.starting_tracking, Toast.LENGTH_SHORT).show()
	}

	fun setCallbacks(callbacks: ServiceCallbacks?) {
		Log.v(TAG, "Setting location callback")
		serviceCallbacks = WeakReference(callbacks)
	}

	override fun onDestroy() {
		Log.v(TAG, "onDestroy has been called!")
		super.onDestroy()

		val serviceCallbackReference = serviceCallbacks
		if (serviceCallbackReference != null) {
			val serviceCallback = serviceCallbackReference.get()
			serviceCallback?.setIsTracking(false)
		}

		locationManager.removeUpdates(this)
		SkiingNotification.cancelTrackingNotification(this)

		binder = null
	}

	override fun onLocationChanged(location: Location) {
		Log.v(TAG, "Location updated")
		val serviceCallbackReference = serviceCallbacks
		if (serviceCallbackReference == null) {
			Log.w(TAG, "Service callback reference is null")
			return
		}

		val serviceCallback = serviceCallbackReference.get()
		if (serviceCallback == null) {
			Log.w(TAG, "Service callback is null")
			return
		}

		// If we are not on the mountain stop the tracking.
		if (!serviceCallback.isInBounds(location)) {
			Toast.makeText(this, R.string.out_of_bounds,
				Toast.LENGTH_LONG).show()
			SkiingNotification.cancelTrackingNotification(this)
			Log.d(TAG, "Stopping location tracking service")
			stopService()
			return
		}

		serviceCallback.onLocationUpdated(location)
		Locations.updateLocations(location)

		var mapMarker = serviceCallback.getOnLocation(location)
		if (mapMarker != null) {
			SkiingNotification.displaySkiingActivity(this, serviceCallback, R.string.current_chairlift,
				mapMarker)
			return
		}

		mapMarker = serviceCallback.getInLocation(location)
		if (mapMarker != null) {
			SkiingNotification.displaySkiingActivity(this, serviceCallback, R.string.current_other,
				mapMarker)
			return
		}

		SkiingNotification.updateTrackingNotification(this, serviceCallback, getString(R.string.tracking_notice),
			null)
	}

	fun stopService() {
		serviceCallbacks?.get()?.onTrackingStopped()
		stopForeground(STOP_FOREGROUND_REMOVE)
		stopSelf()
	}

	override fun onBind(intent: Intent?): IBinder? {
		return binder?.get()
	}

	override fun onProviderEnabled(provider: String) {}

	override fun onProviderDisabled(provider: String) {}

	@Deprecated("This callback will never be invoked on Android Q and above.")
	override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

	companion object {

		const val TAG = "SkierLocationService"

		const val STOP_TRACKING_INTENT = "xyz.thespud.skimap.SkierLocationService.Stop"

		const val START_TRACKING_INTENT = "xyz.thespud.skimap.SkierLocationService.Start"

	}
}
