package xyz.thespud.skimap.mapItem

import android.location.Location
import com.google.android.gms.maps.model.BitmapDescriptor

data class InfoMapMarker(val mapItem: PolygonMapItem, val location: Location, val markerColor: BitmapDescriptor, val color: Int)