package kaist.iclab.tracker.permission

enum class PermissionState {
    NOT_REQUESTED,
    RATIONALE_REQUIRED, // The permission requires rationale
    GRANTED, // The permission is granted
    PERMANENTLY_DENIED, // The permission is permanently denied
    UNSUPPORTED, // The permission is not supported by the device hardware
}
