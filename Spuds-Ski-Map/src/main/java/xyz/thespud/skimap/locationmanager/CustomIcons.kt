package xyz.thespud.skimap.locationmanager

import xyz.thespud.skimap.R

interface CustomIcons {

	fun getOtherIcon(name: String): Int

	fun getChairliftIcon(name: String): Int { return R.drawable.ic_chairlift }

	fun getEasyIcon(): Int { return R.drawable.ic_green }

	fun getMoreDifficultIcon(): Int { return R.drawable.ic_blue }

	fun getMostDifficultIcon(): Int { return R.drawable.ic_black }

	// TODO: Create icon for double black diamond runs
	fun getExtremelyDifficultIcon(): Int { return R.drawable.ic_missing }

	fun getTerrainParkIcon(): Int { return R.drawable.ic_terrain_park }

}