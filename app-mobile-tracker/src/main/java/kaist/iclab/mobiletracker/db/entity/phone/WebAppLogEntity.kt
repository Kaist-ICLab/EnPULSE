package kaist.iclab.mobiletracker.db.entity.phone

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index

/**
 * A generic event logged by a third-party webapp via the `log` bridge action
 * ([kaist.iclab.mobiletracker.webapp.bridge.LogBridgeHandler]).
 *
 * Deliberately **not** a sensor: it does not extend
 * [kaist.iclab.mobiletracker.db.entity.BaseEntity], is absent from
 * [kaist.iclab.mobiletracker.db.obx.SensorStores]/[kaist.iclab.mobiletracker.di.SensorRegistry],
 * and never rides the campaign-gated sensor upload path. It follows the
 * [MicroEmaResponseEntity] pattern instead — a small dedicated store keyed on [isSynced], drained
 * by [kaist.iclab.mobiletracker.services.upload.WebAppLogUploader] into the `web_app_log` table
 * regardless of whether sensor collection is running.
 */
@Entity
data class WebAppLogEntity(
    @Id var id: Long = 0,

    /** When the webapp emitted the event (epoch millis); stamped natively on receipt. */
    var timestamp: Long = 0,

    var webAppId: String = "",

    var eventType: String = "",

    /** Valid JSON text for the `properties` jsonb column; null when the webapp sent none. */
    var propertiesJson: String? = null,

    @Index var isSynced: Boolean = false
)
