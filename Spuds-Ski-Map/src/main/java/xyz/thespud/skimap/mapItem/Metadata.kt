package xyz.thespud.skimap.mapItem

enum class MetadataType {
	EASIEST_WAY_DOWN,
	LIFTLINE_OF,
	NIGHT_RUN

}

open class Metadata(val type: MetadataType)

class EasiestWayDown: Metadata(MetadataType.EASIEST_WAY_DOWN)

class Liftline(val skiliftNames: List<String>): Metadata(MetadataType.LIFTLINE_OF)

class NightRun: Metadata(MetadataType.NIGHT_RUN)

inline fun <reified M : Metadata> List<Metadata>.get(): M? { return this.firstOrNull { it is M } as? M }