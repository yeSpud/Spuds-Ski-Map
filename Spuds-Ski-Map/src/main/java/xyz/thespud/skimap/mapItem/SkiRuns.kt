package xyz.thespud.skimap.mapItem

import androidx.annotation.RawRes

data class SkiRuns(@RawRes val other: Int) {

	@RawRes
	var liftsPolyline: Int? = null

	@RawRes
	var greenRunPolylines: Int? = null

	@RawRes
	var blueRunPolylines: Int? = null

	@RawRes
	var blackRunPolylines: Int? = null

	@RawRes
	var doubleBlackRunPolylines: Int? = null

	@RawRes
	var startingLiftBounds: Int? = null

	@RawRes
	var endingLiftPolylines: Int? = null

	@RawRes
	var greenRunBounds: Int? = null

	@RawRes
	var blueRunBounds: Int? = null

	@RawRes
	var blackRunBounds: Int? = null

	@RawRes
	var doubleBlackRunBounds: Int? = null
}
