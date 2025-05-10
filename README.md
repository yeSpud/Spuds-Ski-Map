# Spud's Ski Map
This is an android library used for creating ski area maps in apps.

## Adding to project
todo

If you want to use the included skier location service for tracking the user's location in the background add the following under the application tag in the android manifest:
```xml
<service android:name="xyz.thespud.skimap.services.SkierLocationService"
    android:foregroundServiceType="location" />
```

## Main Map activity
In order to use the LiveMapActivity you will need to have a class that implements the `getOtherIcon` function.
The easiest way to do this is to declare a private inner class within your maps activity such as the following:
```kotlin
private inner class MyActiveMap(leftPadding: Int, topPadding: Int, rightPadding: Int, bottomPadding: Int):
	LiveMapActivity(
		this@MyMapActivity,
		leftPadding, topPadding, rightPadding, bottomPadding,
		CameraPosition.Builder().target(/*LatLng Here*/).tilt(/*Tilt here*/).bearing(/*Bearing here*/).zoom(/*Zoom here*/).build(),
		LatLngBounds(/*Your pair of LatLng-s here*/),
		R.raw.lifts, R.raw.green, R.raw.blue, R.raw.black, R.raw.double_black,
		R.raw.starting_lift_polygons, R.raw.ending_lift_polygons, R.raw.green_polygons,
		R.raw.blue_polygons, R.raw.black_polygons, R.raw.double_black_polygons, R.raw.other) {

	override fun getOtherIcon(name: String): Int? {
		// Use this function to assign icons to "other" locations on the map (such as lodges, parking lots, etc.)
		// It returns a drawable resource ID, or null if none exists
		return null
	}

	override fun onLocationUpdated(location: Location) {
		// Use this function for when you want to do something with the user's new location
	}
}
```

```kotlin
class MyMapActivity : FragmentActivity() {
	// ...
	
	private lateinit var map: MyActiveMap

	// ...

	override fun onCreate(savedInstanceState: Bundle?) {
		// ...

		// Get the padding for a full screen map if desired - this setup assumes your using databinding 
		ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			lpad = systemBars.left
			tpad = systemBars.top
			rpad = systemBars.right
			bpad = systemBars.bottom

			// Additional padding adjustments here...

			insets
		}

		// ...

		// Setup the map handler.
		map = MyActiveMap(lpad, tpad, rpad, bpad)

		//...

	}

	// ...

	override fun onDestroy() {
		// ...
		map.destroy()
		// ...
		super.onDestroy()
	}

	// ...

	override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
	                                        grantResults: IntArray) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults)

		when (requestCode) {

			// If request is cancelled, the result arrays are empty.
			LiveMapActivity.permissionValue -> {
				if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					map.launchLocationService()
				}
			}
		}
	}

	// ...

}
```

## Info Map activity

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
```