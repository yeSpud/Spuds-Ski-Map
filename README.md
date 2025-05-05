# Spud's Ski Map
This is an android library used for creating ski area maps in apps.

## Adding to project
todo

## Main map activity
todo

### Options Dialog

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