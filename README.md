# Spud's Ski Map
This is an android library used for creating ski area maps in apps.

## Adding to project
To get started add this library to your android app.
```kotlin
implementation("xyz.thespud:spuds-ski-map:2025.06.01")
```
You'll also want the various google maps libraries
```kotlin
implementation("com.google.android.gms:play-services-maps:19.2.0")
implementation("com.google.maps.android:android-maps-utils:3.13.0")
implementation("com.google.maps.android:maps-ktx:5.2.0")
implementation("com.google.maps.android:maps-utils-ktx:5.2.0")
```

If you want to use the included skier location service for tracking the user's location in the background add the following under the application tag in the android manifest:
```xml
<service android:name="xyz.thespud.skimap.services.SkierLocationService"
    android:foregroundServiceType="location" />
```

You'll also want to include the following permissions:
```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>
```

## Live tracking map activity
To use the live tracking map activity for tracking and showing the user's current location on the mountain 
simply declare the LiveMapActivty class with the following:
```kotlin
LiveMapActivity(this, binding.root, 
	CameraPosition.Builder()
		.target(/*LatLng Here*/)
		.tilt(/*Tilt here*/)
		.bearing(/*Bearing here*/)
		.zoom(/*Zoom level here*/).build(),
	LatLngBounds(/*Your pair of LatLng-s here - or null if there are no camera bounds*/), 
	skiRuns, MyCustomIconsClass())
```
The first argument is the context, the second is the view the map will be bound to, 
then followed by the initial camera position for when the activity starts and the map is ready.
This is then followed by the LatLngBounds for the map - 
essentially making sure the user cant scroll too far away (though this can be null if you don't want bounds to be set).

The SkiRuns dataclass is next, and holds all the polylines and polygons for the map. 
This must be initialized with the miscellaneous polygons 
(which contain parking lots, lodging, but most importantly the ski area bounds). 
Following that any additional polylines and polygons may be set like the following:
```kotlin
val skiRuns = SkiRuns(R.raw.misc)
skiRuns.liftsPolyline = R.raw.lifts
skiRuns.greenRunPolylines = ...
skiRuns.blueRunPolylines = ...
skiRuns.blackRunPolylines = ...
skiRuns.doubleBlackRunPolylines = ...
skiRuns.startingLiftBounds = ...
skiRuns.endingLiftPolylines = ...
skiRuns.greenRunBounds = ...
skiRuns.blueRunBounds = ...
skiRuns.blackRunBounds = ...
skiRuns.doubleBlackRunBounds = ...
```

THe final required argument is a class that implements the CustomIcons interface. 
This class is used for setting custom icons for the miscellaneous areas (such as parking lots, lodges, etc.), 
and will eventually be extended to include custom icons for ski lifts.

For now the class only needs to consist of a single overridden function `getOtherIcon` 
which distinguishes between different areas by name, and as such a switch statement can be used to set the icon for each area:
```kotlin
class MyCustomIconsClass: CustomIcons {

	override fun getOtherIcon(name: String): Int {
		Log.d("getOtherIcon", "Getting icon for $name")
		val icon: Int = when (name) {
			"Parking Lot A" -> R.drawable.ic_parking
			"Lodge 1" -> R.drawable.ic_lodge
			// ...
			else -> {
				Log.w("getOtherIcon", "$name does not have an icon")
				R.drawable.ic_missing
			}
		}

		return icon
	}
}
```

### Receiving location update broadcasts
The live location tracking is done though the SkierLocationService class which is started automatically by the LiveMapActivity class.
If you wish to have something happen when the location tracking starts, stops, or updates (such as writing the location to a database)
then declare these BroadcastReceivers:
```kotlin
private val startTrackingReceiver = object : BroadcastReceiver() {
	override fun onReceive(context: Context?, intent: Intent?) {
		Log.d("startTrackingReceiver", "Received broadcast to start tracking")
		
		// ...
		
	}
}

private val updateTrackingReceiver = object : BroadcastReceiver() {
	override fun onReceive(context: Context?, intent: Intent?) {
		Log.d("updateTrackingReceiver", "Received broadcast to update tracking")
		
		// ...
		
	}
}

private val stopTrackingReceiver = object : BroadcastReceiver() {
	override fun onReceive(context: Context?, intent: Intent?) {
		Log.d("stopTrackingReceiver", "Received broadcast to stop tracking")
		
		// ...
		
	}
}

```

Be sure to follow this up by registering the receivers within the `onCreate` method of your activity:
```kotlin
ContextCompat.registerReceiver(context, startTrackingReceiver,
	IntentFilter(SkierLocationService.START_TRACKING_BROADCAST),
	ContextCompat.RECEIVER_EXPORTED)

ContextCompat.registerReceiver(context, updateTrackingReceiver,
	IntentFilter(SkierLocationService.UPDATE_TRACKING_BROADCAST),
	ContextCompat.RECEIVER_EXPORTED)

ContextCompat.registerReceiver(thicontexts, stopTrackingReceiver,
	IntentFilter(SkierLocationService.STOP_TRACKING_BROADCAST),
	ContextCompat.RECEIVER_EXPORTED)
```
and unregister them within the `onDestroy` method:
```kotlin
unregisterReceiver(startTrackingReceiver)
unregisterReceiver(updateTrackingReceiver)
unregisterReceiver(stopTrackingReceiver)
```

### Location tracking permissions
Location tracking requires location permissions to be grated. 
This permission request can be handled in your activity with the following code:
```kotlin
override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
	super.onRequestPermissionsResult(requestCode, permissions, grantResults)

	when (requestCode) {

		// If request is canceled, the result arrays are empty.
		permissionValue -> {
			if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				launchLocationService()
			}
		}
	}
}
```

All-in-all a prospective activity would look something like this:
```kotlin
class MyMapActivity : FragmentActivity() {
	// ...

	private lateinit var map: LiveMapActivity

	// ...

	private val startTrackingReceiver = object : BroadcastReceiver() {
		override fun onReceive(context: Context?, intent: Intent?) {
			Log.d("startTrackingReceiver", "Received broadcast to start tracking")

			// ...

		}
	}

	private val updateTrackingReceiver = object : BroadcastReceiver() {
		override fun onReceive(context: Context?, intent: Intent?) {
			Log.d("updateTrackingReceiver", "Received broadcast to update tracking")

			// ...

		}
	}

	private val stopTrackingReceiver = object : BroadcastReceiver() {
		override fun onReceive(context: Context?, intent: Intent?) {
			Log.d("stopTrackingReceiver", "Received broadcast to stop tracking")

			// ...

		}
	}
	
	// ...

	override fun onCreate(savedInstanceState: Bundle?) {
		enableEdgeToEdge()
		super.onCreate(savedInstanceState)
		
		// ...

		// Load the map polylines and polygons.
		// This class REQUIRES the misc file in order to get the ski area bounds, 
		// but the rest are optional and are set after the fact. 
		// If there are no bounds or polylines simply dont add it.
		val skiRuns = SkiRuns(R.raw.other)
		skiRuns.liftsPolyline = R.raw.lifts
		skiRuns.greenRunPolylines = ...
		skiRuns.blueRunPolylines = ...
		skiRuns.blackRunPolylines = ...
		skiRuns.doubleBlackRunPolylines = ...
		skiRuns.startingLiftBounds = ...
		skiRuns.endingLiftPolylines = ...
		skiRuns.greenRunBounds = ...
		skiRuns.blueRunBounds = ...
		skiRuns.blackRunBounds = ...
		skiRuns.doubleBlackRunBounds = ...

		//...

		map = LiveMapActivity(this, binding.root, CameraPosition.Builder()
			.target(/*LatLng Here*/)
			.tilt(/*Tilt here*/)
			.bearing(/*Bearing here*/)
			.zoom(/*Zoom level here*/).build(),
			null, skiRuns, MyCustomIconsClass())

		// Obtain the SupportMapFragment and get notified when the map is ready to be used.
		val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
		mapFragment.getMapAsync(map)
		
		// ...
		
		ContextCompat.registerReceiver(this, startTrackingReceiver,
			IntentFilter(SkierLocationService.START_TRACKING_BROADCAST),
			ContextCompat.RECEIVER_EXPORTED)
		
		ContextCompat.registerReceiver(this, updateTrackingReceiver,
			IntentFilter(SkierLocationService.UPDATE_TRACKING_BROADCAST),
			ContextCompat.RECEIVER_EXPORTED)
		
		ContextCompat.registerReceiver(this, stopTrackingReceiver,
			IntentFilter(SkierLocationService.STOP_TRACKING_BROADCAST),
			ContextCompat.RECEIVER_EXPORTED)
		
		// ...
		
	}

	// ...

	override fun onDestroy() {
		// ...

		unregisterReceiver(startTrackingReceiver)
		unregisterReceiver(updateTrackingReceiver)
		unregisterReceiver(stopTrackingReceiver)
		
		// ...
		map.destroy()
		// ...
		super.onDestroy()
	}

	// ...

	override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults)

		when (requestCode) {

			// If request is canceled, the result arrays are empty.
			permissionValue -> {
				if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					launchLocationService()
				}
			}
		}
	}
	
	// ...

}
```

## Info Map activity
The InfoMapActivity is less intense than the LiveMapActivity as it doesn't have to deal with tracking 
and displaying the user's live location. As such it just needs to be declared with the following:
```kotlin
InfoMapActivity(this, binding.map,
	CameraPosition.Builder()
		.target(/*LatLng Here*/)
		.tilt(/*Tilt here*/)
		.bearing(/*Bearing here*/)
		.zoom(/*Zoom level here*/).build(),
	LatLngBounds(/*Your pair of LatLng-s here - or null if there are no camera bounds*/),
	skiRuns, MyCustomIconsClass())
```
The arguments from the LiveMapActivity also apply for the InfoMapActivity.

As such a prospective info map activity should look something like this:
```kotlin
class MyInfoMapActivity : FragmentActivity() {
	// ...
	
	private lateinit var map: InfoMapActivity
	
	// ...
	
	override fun onCreate(savedInstanceState: Bundle?) {
		enableEdgeToEdge()
		super.onCreate(savedInstanceState)

		// ...

		// Load the map polylines and polygons.
		// This class REQUIRES the misc file in order to get the ski area bounds, 
		// but the rest are optional and are set after the fact. 
		// If there are no bounds or polylines simply dont add it.
		val skiRuns = SkiRuns(R.raw.other)
		skiRuns.liftsPolyline = R.raw.lifts
		skiRuns.greenRunPolylines = ...
		skiRuns.blueRunPolylines = ...
		skiRuns.blackRunPolylines = ...
		skiRuns.doubleBlackRunPolylines = ...
		skiRuns.startingLiftBounds = ...
		skiRuns.endingLiftPolylines = ...
		skiRuns.greenRunBounds = ...
		skiRuns.blueRunBounds = ...
		skiRuns.blackRunBounds = ...
		skiRuns.doubleBlackRunBounds = ...

		//...

		// Setup the map handler.
		map = InfoMapActivity(this, binding.map, CameraPosition.Builder()
			.target(/*LatLng Here*/)
			.tilt(/*Tilt here*/)
			.bearing(/*Bearing here*/)
			.zoom(/*Zoom level here*/).build(),
			null, skiRuns, MyCustomIconsClass())

		// Obtain the SupportMapFragment and get notified when the map is ready to be used.
		val mapFragment = supportFragmentManager.findFragmentById(R.id.activity_map) as SupportMapFragment
		mapFragment.getMapAsync(map)
		
		// ...
				
	}
	
	// ...
	
	override fun onDestroy() {
		// ...
		map.destroy()
		// ...
		super.onDestroy()
	}
}
```


## Included options dialog
There are 2 included dialogs corresponding to each included map activity.

### Live Map Options dialog
Allows for starting and stopping location tracking

### Info Map Options dialog
Allows for hiding and showing location dots on the final map
