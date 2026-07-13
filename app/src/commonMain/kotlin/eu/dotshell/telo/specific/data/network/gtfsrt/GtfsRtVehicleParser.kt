package eu.dotshell.telo.specific.data.network.gtfsrt

/**
 * A vehicle entry decoded from a GTFS-RT VehiclePosition feed.
 * Only vehicles carrying a position survive parsing.
 */
data class GtfsRtVehicle(
    val entityId: String,
    val tripId: String?,
    val routeId: String?,
    val directionId: Int?,
    val latitude: Double,
    val longitude: Double,
    val bearing: Double?,
    val speedMps: Double?,
    val timestamp: Long?,
    val vehicleId: String?,
    val vehicleLabel: String?
)

data class GtfsRtFeed(
    val headerTimestamp: Long?,
    val vehicles: List<GtfsRtVehicle>
)

/**
 * Hand-rolled protobuf wire-format reader for the GTFS-RT FeedMessage subset
 * the app needs (no protobuf dependency can be added on this project):
 *
 * ```
 * FeedMessage    { 1: FeedHeader header, 2: repeated FeedEntity entity }
 * FeedHeader     { 3: uint64 timestamp }
 * FeedEntity     { 1: string id, 4: VehiclePosition vehicle }
 * VehiclePosition{ 1: TripDescriptor trip, 2: Position position,
 *                  5: uint64 timestamp, 8: VehicleDescriptor vehicle }
 * TripDescriptor { 1: string trip_id, 5: string route_id, 6: uint32 direction_id }
 * Position       { 1: float latitude, 2: float longitude, 3: float bearing,
 *                  5: float speed }
 * VehicleDescriptor { 1: string id, 2: string label }
 * ```
 *
 * Unknown fields of every message are skipped by wire type, so feed
 * extensions (trip updates interleaved, OccupancyStatus, …) are harmless.
 */
object GtfsRtVehicleParser {

    fun parse(bytes: ByteArray): GtfsRtFeed {
        val reader = ProtoReader(bytes, 0, bytes.size)
        var headerTimestamp: Long? = null
        val vehicles = mutableListOf<GtfsRtVehicle>()

        while (reader.hasMore()) {
            val tag = reader.readTag()
            when (tag.fieldNumber) {
                1 -> headerTimestamp = readHeaderTimestamp(reader.subMessage())
                2 -> readEntity(reader.subMessage())?.let(vehicles::add)
                else -> reader.skip(tag.wireType)
            }
        }
        return GtfsRtFeed(headerTimestamp, vehicles)
    }

    private fun readHeaderTimestamp(reader: ProtoReader): Long? {
        var timestamp: Long? = null
        while (reader.hasMore()) {
            val tag = reader.readTag()
            when {
                tag.fieldNumber == 3 && tag.wireType == 0 -> timestamp = reader.readVarint()
                else -> reader.skip(tag.wireType)
            }
        }
        return timestamp
    }

    private fun readEntity(reader: ProtoReader): GtfsRtVehicle? {
        var entityId = ""
        var vehicle: GtfsRtVehicle? = null
        while (reader.hasMore()) {
            val tag = reader.readTag()
            when (tag.fieldNumber) {
                1 -> entityId = reader.readString()
                4 -> vehicle = readVehiclePosition(reader.subMessage())
                else -> reader.skip(tag.wireType)
            }
        }
        return vehicle?.copy(entityId = entityId)
    }

    private fun readVehiclePosition(reader: ProtoReader): GtfsRtVehicle? {
        var tripId: String? = null
        var routeId: String? = null
        var directionId: Int? = null
        var latitude: Double? = null
        var longitude: Double? = null
        var bearing: Double? = null
        var speed: Double? = null
        var timestamp: Long? = null
        var vehicleId: String? = null
        var vehicleLabel: String? = null

        while (reader.hasMore()) {
            val tag = reader.readTag()
            when (tag.fieldNumber) {
                1 -> {
                    val trip = reader.subMessage()
                    while (trip.hasMore()) {
                        val t = trip.readTag()
                        when (t.fieldNumber) {
                            1 -> tripId = trip.readString()
                            5 -> routeId = trip.readString()
                            6 -> if (t.wireType == 0) directionId = trip.readVarint().toInt() else trip.skip(t.wireType)
                            else -> trip.skip(t.wireType)
                        }
                    }
                }
                2 -> {
                    val position = reader.subMessage()
                    while (position.hasMore()) {
                        val t = position.readTag()
                        when {
                            t.wireType == 5 && t.fieldNumber == 1 -> latitude = position.readFloat().toDouble()
                            t.wireType == 5 && t.fieldNumber == 2 -> longitude = position.readFloat().toDouble()
                            t.wireType == 5 && t.fieldNumber == 3 -> bearing = position.readFloat().toDouble()
                            t.wireType == 5 && t.fieldNumber == 5 -> speed = position.readFloat().toDouble()
                            else -> position.skip(t.wireType)
                        }
                    }
                }
                5 -> if (tag.wireType == 0) timestamp = reader.readVarint() else reader.skip(tag.wireType)
                8 -> {
                    val descriptor = reader.subMessage()
                    while (descriptor.hasMore()) {
                        val t = descriptor.readTag()
                        when (t.fieldNumber) {
                            1 -> vehicleId = descriptor.readString()
                            2 -> vehicleLabel = descriptor.readString()
                            else -> descriptor.skip(t.wireType)
                        }
                    }
                }
                else -> reader.skip(tag.wireType)
            }
        }

        val lat = latitude ?: return null
        val lon = longitude ?: return null
        return GtfsRtVehicle(
            entityId = "",
            tripId = tripId,
            routeId = routeId,
            directionId = directionId,
            latitude = lat,
            longitude = lon,
            bearing = bearing,
            speedMps = speed,
            timestamp = timestamp,
            vehicleId = vehicleId,
            vehicleLabel = vehicleLabel
        )
    }

    private val Int.fieldNumber: Int get() = this ushr 3
    private val Int.wireType: Int get() = this and 0x7

    /** Bounded cursor over the wire bytes; sub-messages share the array. */
    private class ProtoReader(
        private val bytes: ByteArray,
        private var pos: Int,
        private val end: Int
    ) {
        fun hasMore(): Boolean = pos < end

        fun readTag(): Int = readVarint().toInt()

        fun readVarint(): Long {
            var result = 0L
            var shift = 0
            while (true) {
                check(pos < end) { "varint runs past message end at $pos" }
                val b = bytes[pos++].toInt()
                result = result or ((b and 0x7F).toLong() shl shift)
                if (b and 0x80 == 0) return result
                shift += 7
                check(shift < 64) { "malformed varint at $pos" }
            }
        }

        private fun readLength(): Int {
            val length = readVarint().toInt()
            check(length >= 0 && pos + length <= end) { "length $length runs past message end at $pos" }
            return length
        }

        fun subMessage(): ProtoReader {
            val length = readLength()
            val sub = ProtoReader(bytes, pos, pos + length)
            pos += length
            return sub
        }

        fun readString(): String {
            val length = readLength()
            val value = bytes.decodeToString(pos, pos + length)
            pos += length
            return value
        }

        fun readFloat(): Float {
            check(pos + 4 <= end) { "fixed32 runs past message end at $pos" }
            val bits = (bytes[pos].toInt() and 0xFF) or
                ((bytes[pos + 1].toInt() and 0xFF) shl 8) or
                ((bytes[pos + 2].toInt() and 0xFF) shl 16) or
                ((bytes[pos + 3].toInt() and 0xFF) shl 24)
            pos += 4
            return Float.fromBits(bits)
        }

        fun skip(wireType: Int) {
            when (wireType) {
                0 -> readVarint()
                1 -> { check(pos + 8 <= end); pos += 8 }
                // NOT `pos += readLength()`: the left operand would be read
                // before readLength() advances pos past the length varint.
                2 -> { val length = readLength(); pos += length }
                5 -> { check(pos + 4 <= end); pos += 4 }
                else -> error("unsupported wire type $wireType at $pos")
            }
        }
    }
}
