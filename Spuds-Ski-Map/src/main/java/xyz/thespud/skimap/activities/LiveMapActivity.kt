package xyz.thespud.skimap.activities

import android.Manifest
import android.app.ActivityManager
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.IBinder
import android.os.Process
import android.util.Log
import androidx.annotation.RawRes
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.maps.android.ktx.addMarker
import xyz.thespud.skimap.R
import xyz.thespud.skimap.mapItem.Locations
import xyz.thespud.skimap.mapItem.MapMarker
import xyz.thespud.skimap.services.ServiceCallbacks
import xyz.thespud.skimap.services.SkierLocationService

abstract class LiveMapActivity(
	activity: FragmentActivity, val leftPadding: Int, val topPadding: Int, val rightPadding: Int,
	val bottomPadding: Int, cameraPosition: CameraPosition, cameraBounds: LatLngBounds, @RawRes lifts: Int?,
	@RawRes green: Int?, @RawRes blue: Int?, @RawRes black: Int?, @RawRes doubleBlack: Int?,
	@RawRes starting_lifts_bounds: Int?, @RawRes ending_lifts_bounds: Int?, @RawRes green_polygons: Int?,
	@RawRes blue_polygons: Int?, @RawRes black_polygons: Int?, @RawRes double_black_polygons: Int?,
	@RawRes other: Int): MapHandler(activity, cameraPosition, cameraBounds, lifts, green, blue, black,
	doubleBlack, starting_lifts_bounds, ending_lifts_bounds, green_polygons, blue_polygons, black_polygons,
	double_black_polygons, other), ServiceCallbacks {

	private var locationMarker: Marker? = null

	var isMapSetup = false

	var skierLocationService: SkierLocationService? = null
	private set
	private var bound = false

	var manuallyDisabled = false
	private set
	var isTrackingLocation = false
	private var locationTrackingButton: MapOptionItem? = null

	private val serviceConnection = object : ServiceConnection {

		override fun onServiceConnected(name: ComponentName?, service: IBinder) {
			val binder = service as SkierLocationService.LocalBinder
			skierLocationService = binder.getService()
			bound = true
			skierLocationService!!.setCallbacks(this@LiveMapActivity)
			setIsTracking(true)
		}

		override fun onServiceDisconnected(name: ComponentName?) {
			skierLocationService!!.setCallbacks(null)
			activity.unbindService(this)
			skierLocationService = null
			setIsTracking(false)
			bound = false
		}
	}

	override val additionalCallback: OnMapReadyCallback = OnMapReadyCallback {

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

		googleMap.setPadding(leftPadding, topPadding, rightPadding, bottomPadding)
		isMapSetup = true
	}

	override fun destroy() {

		if (locationMarker != null) {
			Log.v("LiveMapActivity", "Removing location marker")
			locationMarker!!.remove()
			locationMarker = null
		}

		if (bound) {
			skierLocationService!!.setCallbacks(null)
			activity.unbindService(serviceConnection)
			skierLocationService = null
			isTrackingLocation = false
			bound = false
		}

		super.destroy()
	}

	override fun isInBounds(location: Location): Boolean {
		if (skiAreaBounds != null) {
			return skiAreaBounds!!.locationInsidePoints(location)
		}
		return false
	}

	override fun getOnLocation(location: Location): MapMarker? {

		var mapMarker = Locations.checkIfIOnChairlift(startingChairliftTerminals, endingChairliftTerminals)
		if (mapMarker != null) {
			return mapMarker
		}

		mapMarker = Locations.checkIfOnRun(greenRunBounds, blueRunBounds, blackRunBounds, doubleBlackRunBounds)
		if (mapMarker != null) {
			return mapMarker
		}

		return null
	}

	override fun getInLocation(location: Location): MapMarker? {
		return Locations.checkIfOnOther(otherBounds)
	}

	override fun updateMapMarker(locationString: String) {
		val location = Locations.currentLocation
		if (location != null) {
			if (locationMarker == null) {
				locationMarker = googleMap.addMarker {
					position(LatLng(location.latitude, location.longitude))
					title(activity.resources.getString(R.string.your_location))
				}
			} else {

				// Otherwise just update the LatLng location.
				locationMarker!!.position = LatLng(location.latitude, location.longitude)
			}
		}
	}

	override fun setIsTracking(isTracking: Boolean) {
		isTrackingLocation = isTracking

		val button = locationTrackingButton ?: return
		if (button.itemEnabled != isTracking) {
			button.toggleOptionVisibility()
		}
	}

	override fun setManuallyDisabled(manuallyDisabled: Boolean) {
		this.manuallyDisabled = manuallyDisabled
	}

	override fun getLaunchingActivity(): FragmentActivity {
		return activity
	}

	fun launchLocationService() {

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

			activity.bindService(serviceIntent, serviceConnection, Context.BIND_NOT_FOREGROUND)
			activity.startService(serviceIntent)
		} else {
			Log.w("launchLocationService", "GPS not enabled")
		}
	}

	companion object {
		const val permissionValue = 29500
	}

}