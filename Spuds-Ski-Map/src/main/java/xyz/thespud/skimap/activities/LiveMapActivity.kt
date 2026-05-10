package xyz.thespud.skimap.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Process
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLngBounds
import xyz.thespud.skimap.R
import xyz.thespud.skimap.mapItem.Locations
import xyz.thespud.skimap.mapItem.SkiRuns
import xyz.thespud.skimap.services.SkierLocationService
import xyz.thespud.skimap.services.SkiingNotification.NOTIFICATION_PERMISSION

class LiveMapActivity(val activity: FragmentActivity, cameraPosition: CameraPosition, cameraBounds: LatLngBounds?,
                      skiRuns: SkiRuns, otherIconCallback: CustomIcons, showDebug: Boolean = false): MapHandler(activity,
	cameraPosition, cameraBounds, skiRuns, otherIconCallback, false, showDebug), GoogleMap.OnMyLocationClickListener {

	var isMapSetup = false

	var manuallyDisabled = false
	private set
	var isTrackingLocation = false

	private val startTrackingReceiver = object : BroadcastReceiver() {
		override fun onReceive(context: Context?, intent: Intent?) { setIsTracking(true) }
	}

	private val stopTrackingReceiver = object : BroadcastReceiver() {
		override fun onReceive(context: Context?, intent: Intent?) { setIsTracking(false) }
	}

	override val additionalCallback: OnMapReadyCallback = OnMapReadyCallback {
		Log.v("additionalCallback", "additionalCallback called for LiveMapActivity")

		// Determine if the user has enabled location permissions.
		val locationEnabled = activity.checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, Process.myPid(),
			Process.myUid()) == PackageManager.PERMISSION_GRANTED

		// Request location permission, so that we can get the location of the device.
		// The result of the permission request is handled by a callback, onRequestPermissionsResult.
		// If this permission isn't granted then that's fine too.
		Log.v("onMapReady", "Checking location permissions...")
		if (locationEnabled) {
			Log.v("onMapReady", "Location tracking enabled")
			launchLocationService()
		} else {

			// Setup the location popup dialog.
			val alertDialogBuilder: AlertDialog.Builder = AlertDialog.Builder(activity)
			alertDialogBuilder.setTitle(R.string.alert_title)
			alertDialogBuilder.setMessage(R.string.alert_message)
			alertDialogBuilder.setPositiveButton(R.string.alert_ok) { _, _ ->
				ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
					permissionValue)
			}

			// Show the info popup about location.
			alertDialogBuilder.create().show()
		}

		// Apply map insets to fix edge to edge behavior
		applyMapInsets(activity.window.decorView)

		isMapSetup = true
	}

	override fun onMyLocationClick(location: Location) {
		Locations.updateLocations(location)

		var toast = Toast.makeText(activity, R.string.your_location, Toast.LENGTH_LONG)
		var mapMarker = Locations.checkIfIOnChairlift()
		if (mapMarker != null) {
			toast = Toast.makeText(activity, activity.getString(R.string.current_chairlift, mapMarker.name),
				Toast.LENGTH_LONG)
		}

		mapMarker = Locations.checkIfOnRun()
		if (mapMarker != null) {
			toast = Toast.makeText(activity, activity.getString(R.string.current_run, mapMarker.name),
				Toast.LENGTH_LONG)
		}

		mapMarker = Locations.getInLocation()
		if (mapMarker != null) {
			toast = Toast.makeText(activity, activity.getString(R.string.current_other, mapMarker.name),
				Toast.LENGTH_LONG)
		}

		toast.show()
	}

	// This will only get called when we have location permissions.
	@SuppressLint("MissingPermission")
	fun setIsTracking(isTracking: Boolean) {
		Log.d("setIsTracking", "Setting location tracking to $isTracking")
		googleMap?.isMyLocationEnabled = isTracking
		isTrackingLocation = isTracking
	}

	fun setManuallyDisabled(manuallyDisabled: Boolean) {
		Log.d("setManuallyDisabled", "Setting manually disabled to $manuallyDisabled")
		this.manuallyDisabled = manuallyDisabled
	}

	fun launchLocationService() {

		if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS)
			== PackageManager.PERMISSION_DENIED) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
				ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.POST_NOTIFICATIONS),
					NOTIFICATION_PERMISSION
				)
			}
		}

		val locationManager: LocationManager = activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
		if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {

			val serviceIntent = Intent(activity, SkierLocationService::class.java)
			serviceIntent.action = SkierLocationService.START_TRACKING_INTENT

			// Check if the service has already been started and is running...
			val activityManager: ActivityManager = activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

			// As of Build.VERSION_CODES.O, this method is no longer available to third party applications.
			// For backwards compatibility, it will still return the caller's own service.
			// Which is exactly what we want.
			@Suppress("DEPRECATION")
			for (runningServices in activityManager.getRunningServices(Int.MAX_VALUE)) {
				if (SkierLocationService::class.java.name == runningServices.service.className) {
					if (runningServices.foreground) {
						return
					}
				}
			}

			activity.startService(serviceIntent)
		} else {
			Log.w("launchLocationService", "GPS not enabled")
		}
	}

	override fun destroy() {
		super.destroy()
		activity.unregisterReceiver(startTrackingReceiver)
		activity.unregisterReceiver(stopTrackingReceiver)
	}

	init {
		ContextCompat.registerReceiver(activity, startTrackingReceiver,
			IntentFilter(SkierLocationService.START_TRACKING_BROADCAST),
			ContextCompat.RECEIVER_EXPORTED)

		ContextCompat.registerReceiver(activity, stopTrackingReceiver,
			IntentFilter(SkierLocationService.STOP_TRACKING_BROADCAST),
			ContextCompat.RECEIVER_EXPORTED)
	}

	companion object {
		const val permissionValue = 29500
	}
}