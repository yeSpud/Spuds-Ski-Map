package xyz.thespud.skimap.services

import android.Manifest
import android.app.Notification
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.maps.android.PolyUtil
import xyz.thespud.skimap.R
import xyz.thespud.skimap.activities.LiveMapActivity
import xyz.thespud.skimap.mapItem.Locations

class SkierLocationService : Service(), LocationListener {

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

				val notification: Notification = SkiingNotification.createTrackingNotification(this,
					null, applicationInfo.icon, "", null)

				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
					startForeground(SkiingNotification.TRACKING_SERVICE_ID, notification,
						ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
				} else {
					startForeground(SkiingNotification.TRACKING_SERVICE_ID, notification)
				}

				sendBroadcast(Intent(START_TRACKING_BROADCAST))
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

		val manager = getSystemService(LOCATION_SERVICE) as LocationManager
		SkiingNotification.setupNotificationChannels(this)

		if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 1F, this)
		}

		locationManager = manager

		Toast.makeText(this, R.string.starting_tracking, Toast.LENGTH_SHORT).show()
	}

	override fun onDestroy() {
		Log.v(TAG, "onDestroy has been called!")
		super.onDestroy()

		locationManager.removeUpdates(this)
		SkiingNotification.cancelTrackingNotification(this)
	}

	override fun onLocationChanged(location: Location) {
		Log.v(TAG, "Location updated")

		val bounds = Locations.skiAreaBounds
		if (bounds == null) {
			Log.w(TAG, "Bounds not set before update")
			return
		}

		// If we are not on the mountain stop the tracking.
		if (!PolyUtil.containsLocation(location.latitude, location.longitude,
				bounds.points, true)) {
			Toast.makeText(this, R.string.out_of_bounds,
				Toast.LENGTH_LONG).show()
			SkiingNotification.cancelTrackingNotification(this)
			Log.d(TAG, "Stopping location tracking service")
			stopService()
			return
		}

		Locations.updateLocations(location)

		sendBroadcast(Intent(UPDATE_TRACKING_BROADCAST))

		val intent = Intent(this, LiveMapActivity::class.java)

		var mapMarker = Locations.getOnLocation()
		if (mapMarker != null) {
			SkiingNotification.displaySkiingActivity(this, intent,
				applicationInfo.icon, R.string.current_chairlift, mapMarker)
			return
		}

		mapMarker = Locations.getInLocation()
		if (mapMarker != null) {
			SkiingNotification.displaySkiingActivity(this, intent,
				applicationInfo.icon, R.string.current_other, mapMarker)
			return
		}

		SkiingNotification.updateTrackingNotification(this, intent,
			applicationInfo.icon, getString(R.string.tracking_notice), null)
	}

	fun stopService() {
		sendBroadcast(Intent(STOP_TRACKING_BROADCAST))
		stopForeground(STOP_FOREGROUND_REMOVE)
		stopSelf()
	}

	override fun onBind(intent: Intent?): IBinder? { return null }

	override fun onProviderEnabled(provider: String) {}

	override fun onProviderDisabled(provider: String) {}

	@Deprecated("This callback will never be invoked on Android Q and above.")
	override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

	companion object {

		const val TAG = "SkierLocationService"

		const val STOP_TRACKING_INTENT = "xyz.thespud.skimap.SkierLocationService.Stop"
		const val START_TRACKING_INTENT = "xyz.thespud.skimap.SkierLocationService.Start"

		const val START_TRACKING_BROADCAST = "xyz.thespud.skimap.SkierLocationService.Broadcast.Start"
		const val UPDATE_TRACKING_BROADCAST = "xyz.thespud.skimap.SkierLocationService.Broadcast.Update"
		const val STOP_TRACKING_BROADCAST = "xyz.thespud.skimap.SkierLocationService.Broadcast.Stop"
	}
}
