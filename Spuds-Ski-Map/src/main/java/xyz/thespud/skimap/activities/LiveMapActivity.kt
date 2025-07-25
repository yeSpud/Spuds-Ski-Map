package xyz.thespud.skimap.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.os.Process
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.maps.android.PolyUtil
import com.google.maps.android.ktx.addMarker
import xyz.thespud.skimap.R
import xyz.thespud.skimap.mapItem.Locations
import xyz.thespud.skimap.mapItem.Locations.chairliftIcon
import xyz.thespud.skimap.mapItem.MapMarker
import xyz.thespud.skimap.mapItem.PolygonMapItem
import xyz.thespud.skimap.mapItem.SkiRuns
import xyz.thespud.skimap.services.SkierLocationService
import xyz.thespud.skimap.services.SkiingNotification.NOTIFICATION_PERMISSION

abstract class LiveMapActivity(cameraPosition: CameraPosition, cameraBounds: LatLngBounds?, skiRuns: SkiRuns,
                               showDebug: Boolean = false): MapHandler(cameraPosition, cameraBounds,
	skiRuns, false, showDebug), GoogleMap.OnMyLocationClickListener {

	var isMapSetup = false

	var manuallyDisabled = false
	private set
	var isTrackingLocation = false
	private var locationTrackingButton: MapOptionItem? = null

	private val serviceConnection = object : ServiceConnection {

		override fun onServiceConnected(name: ComponentName?, service: IBinder) {
			setIsTracking(true)
		}

		override fun onServiceDisconnected(name: ComponentName?) {
			setIsTracking(false)
		}
	}

	override val additionalCallback: OnMapReadyCallback = OnMapReadyCallback {
		Log.v("additionalCallback", "additionalCallback called for LiveMapActivity")

		// Determine if the user has enabled location permissions.
		val locationEnabled = checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, Process.myPid(),
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
			val alertDialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this)
			alertDialogBuilder.setTitle(R.string.alert_title)
			alertDialogBuilder.setMessage(R.string.alert_message)
			alertDialogBuilder.setPositiveButton(R.string.alert_ok) { _, _ ->
				ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
					permissionValue)
			}

			// Show the info popup about location.
			alertDialogBuilder.create().show()
		}

		// googleMap.setPadding(leftPadding, topPadding, rightPadding, bottomPadding)
		isMapSetup = true
	}

	override fun onMyLocationClick(location: Location) {
		Locations.updateLocations(location)

		var toast = Toast.makeText(this, R.string.your_location, Toast.LENGTH_LONG)
		var mapMarker = Locations.checkIfIOnChairlift()
		if (mapMarker != null) {
			toast = Toast.makeText(this, getString(R.string.current_chairlift, mapMarker.name),
				Toast.LENGTH_LONG)
		}

		mapMarker = Locations.checkIfOnRun()
		if (mapMarker != null) {
			toast = Toast.makeText(this, getString(R.string.current_run, mapMarker.name),
				Toast.LENGTH_LONG)
		}

		mapMarker = Locations.getInLocation()
		if (mapMarker != null) {
			toast = Toast.makeText(this, getString(R.string.current_other, mapMarker.name),
				Toast.LENGTH_LONG)
		}

		toast.show()
	}

	// This will only get called when we have location permissions.
	@SuppressLint("MissingPermission")
	fun setIsTracking(isTracking: Boolean) {
		Log.d("setIsTracking", "Setting location tracking to $isTracking")
		googleMap.isMyLocationEnabled = isTracking
		isTrackingLocation = isTracking

		val button = locationTrackingButton ?: return
		if (button.itemEnabled != isTracking) {
			button.toggleOptionVisibility()
		}
	}

	fun setManuallyDisabled(manuallyDisabled: Boolean) {
		Log.d("setManuallyDisabled", "Setting manually disabled to $manuallyDisabled")
		this.manuallyDisabled = manuallyDisabled
	}

	fun launchLocationService() {

		if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
			== PackageManager.PERMISSION_DENIED) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
				ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS),
					NOTIFICATION_PERMISSION
				)
			}
		}

		val locationManager: LocationManager = getSystemService(LOCATION_SERVICE) as LocationManager
		if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {

			val serviceIntent = Intent(this, SkierLocationService::class.java)
			serviceIntent.action = SkierLocationService.START_TRACKING_INTENT

			// Check if the service has already been started and is running...
			val activityManager: ActivityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager

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

			bindService(serviceIntent, serviceConnection, BIND_NOT_FOREGROUND)
			startService(serviceIntent)
		} else {
			Log.w("launchLocationService", "GPS not enabled")
		}
	}

	companion object {
		const val permissionValue = 29500
	}

}