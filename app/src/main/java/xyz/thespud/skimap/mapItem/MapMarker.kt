package xyz.thespud.skimap.mapItem

import android.location.Location
import androidx.annotation.DrawableRes
import com.google.android.gms.maps.model.BitmapDescriptor

// todo Add equals operator(?)
data class MapMarker(val name: String, val location: Location, @DrawableRes val icon: Int,
                     val markerColor: BitmapDescriptor, val color: Int)