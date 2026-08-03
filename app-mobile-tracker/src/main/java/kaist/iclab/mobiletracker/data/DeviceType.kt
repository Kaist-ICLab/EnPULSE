package kaist.iclab.mobiletracker.data

/**
 * Enum representing the type of device that collected the sensor data.
 * Used to differentiate between phone and watch sensor data.
 */
enum class DeviceType(val value: Int) {
    PHONE(0),
    WATCH(1);

    companion object {
        /**
         * Get DeviceType from integer value
         */
        fun fromInt(value: Int): DeviceType {
            return entries.find { it.value == value } ?: PHONE
        }
    }
}
