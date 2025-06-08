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

## Main Map activity
In order to use the LiveMapActivity you will need to have a class that implements the `getOtherIcon` function.
The easiest way to do this is to declare a private inner class within your maps activity such as the following:
```kotlin
private inner class MyActiveMap(leftPadding: Int, topPadding: Int, rightPadding: Int, bottomPadding: Int, skiRuns: SkiRuns): 
	LiveMapActivity(this@MyMapActivity, leftPadding, topPadding, rightPadding, bottomPadding, 
	CameraPosition.Builder().target(/*LatLng Here*/).tilt(/*Tilt here*/).bearing(/*Bearing here*/).zoom(/*Zoom here*/).build(), 
	LatLngBounds(/*Your pair of LatLng-s here - or null if there are no camera bounds*/), 
	skiRuns) {

	override val additionalCallback: OnMapReadyCallback = OnMapReadyCallback {
		super.additionalCallback
		// Override this callback to run any additional steps when the map is ready.
		// Otherwise this can be excluded.
	}
	
	override fun getOtherIcon(name: String): Int? {
		// Use this function to assign icons to "other" locations on the map (such as lodges, parking lots, etc.)
		// It returns a drawable resource ID, or null if none exists
		return null
	}

	override fun onLocationUpdated(location: Location) {
		// Use this function for when you want to do something with the user's new location
	}

	override fun onTrackingStopped() {
		// Use this function for when you want to do something once location tracking has stopped.
	}
}
```

```kotlin
class MyMapActivity : FragmentActivity() {
	// ...
	
	private var map: MyActiveMap? = null

	// ...

	override fun onCreate(savedInstanceState: Bundle?) {
		enableEdgeToEdge()
		super.onCreate(savedInstanceState)
		
		// ...

		// Load the map polylines and polygons.
		// This object REQUIRES the other object in order to get the ski area bounds, 
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
				

		// Get the padding for a full screen map if desired - this setup assumes your using databinding 
		ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view: View, insets: WindowInsetsCompat ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			lpad = systemBars.left
			tpad = systemBars.top
			rpad = systemBars.right
			bpad = systemBars.bottom

			// Additional padding adjustments here...

			// Setup the map handler.
			map = MyActiveMap(lpad, tpad, rpad, bpad, skiRuns)

			// ...

			// Obtain the SupportMapFragment and get notified when the map is ready to be used.
			val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
			mapFragment.getMapAsync(map!!)
			
			// ...
			
			insets
		}

		//...

	}

	// ...

	override fun onDestroy() {
		// ...
		map?.destroy()
		// ...
		super.onDestroy()
	}

	// ...

}
```

## Info Map activity
```kotlin
private inner class MyInfoMap(lpad: Int, tpad: Int, rpad: Int, bpad: Int, skiRuns: SkiRuns) : 
	InfoMapActivity(this@MyInfoMapActivity, lpad, tpad, rpad, bpad,
		CameraPosition.Builder().target(/*LatLng Here*/).tilt(/*Tilt here*/).bearing(/*Bearing here*/).zoom(/*Zoom here*/).build(),
		LatLngBounds(/*Your pair of LatLng-s here - or null if there are no camera bounds*/),
		skiRuns) {

	override val additionalCallback: OnMapReadyCallback = OnMapReadyCallback {
		super.additionalCallback
		// Override this callback to run any additional steps when the map is ready.
		// Otherwise this can be excluded.
	}

	override fun getOtherIcon(name: String): Int? {
		// Use this function to assign icons to "other" locations on the map (such as lodges, parking lots, etc.)
		// It returns a drawable resource ID, or null if none exists
		return null
	}
}
```

```kotlin
class MyInfoMapActivity : FragmentActivity() {
	// ...
	
	private lateinit var map: MyInfoMap
	
	// ...
	
	override fun onCreate(savedInstanceState: Bundle?) {
		enableEdgeToEdge()
		super.onCreate(savedInstanceState)

		// ...

		// Load the map polylines and polygons.
		// This object REQUIRES the other object in order to get the ski area bounds, 
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

		// Get the padding for a full screen map if desired - this setup assumes your using databinding 
		ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view: View, insets: WindowInsetsCompat ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			lpad = systemBars.left
			tpad = systemBars.top
			rpad = systemBars.right
			bpad = systemBars.bottom

			// Additional padding adjustments here...

			// Setup the map handler.
			map = MyInfoMap(lpad, tpad, rpad, bpad, skiRuns)

			// ...

			// Obtain the SupportMapFragment and get notified when the map is ready to be used.
			val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
			mapFragment.getMapAsync(map!!)

			// ...

			insets
		}

		//...
				
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


## Location tracking service
The location tracking service is called SkierLocationService, 
and requires location services to be grated. This permission request can be done in the main activity with the following code:
```kotlin
class MyMapActivity : FragmentActivity() {
	// ... 

	override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults)

		when (requestCode) {

			// If request is cancelled, the result arrays are empty.
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