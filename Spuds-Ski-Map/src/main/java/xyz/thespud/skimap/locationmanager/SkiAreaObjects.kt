package xyz.thespud.skimap.locationmanager

import androidx.annotation.RawRes

data class SkiAreaObjects(@RawRes val skiAreaBounds: Int) {

	@RawRes
	var chairliftsPolyline: Int? = null

	@RawRes
	var greenRunPolylines: Int? = null

	@RawRes
	var blueRunPolylines: Int? = null

	@RawRes
	var blackRunPolylines: Int? = null

	@RawRes
	var doubleBlackRunPolylines: Int? = null

	@RawRes
	var chairliftTerminals: Int? = null

	@RawRes
	var greenRunBounds: Int? = null

	@RawRes
	var blueRunBounds: Int? = null

	@RawRes
	var blackRunBounds: Int? = null

	@RawRes
	var doubleBlackRunBounds: Int? = null

	@RawRes
	var other: Int? = null
}