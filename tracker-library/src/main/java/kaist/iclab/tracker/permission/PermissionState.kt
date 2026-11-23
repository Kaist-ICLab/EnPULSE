package kaist.iclab.tracker.permission

enum class PermissionState {
    NOT_REQUESTED,
    RATIONALE_REQUIRED, // The permission requires rationale
    GRANTED, // The permission is granted
    PERMANENTLY_DENIED, // The permission is permanently denied
    UNSUPPORTED, // The permission is not supported by the device hardware
    APP_NOT_INSTALLED, // An app the permission depends on (Samsung Health) is missing or too old
}
