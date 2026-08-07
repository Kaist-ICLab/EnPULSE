package kaist.iclab.mobiletracker.data.sensors

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Supabase data class representing campaign table data.
 *
 * @property id Unique identifier for the campaign table entry (integer primary key).
 * @property campaignId Campaign identifier.
 * @property name Name of the campaign table — doubles as the sensor id this row gates/configures,
 *   e.g. `"timing_sensor"` (see [kaist.iclab.mobiletracker.utils.toCampaignSensorName]).
 * @property description Description of the campaign table.
 * @property dailyCountMax Maximum daily count allowed for the table.
 * @property config Optional free-form JSON configuration for the sensor named by [name]. Only a
 *   handful of sensors read this today (e.g. `timing_sensor` — see
 *   [kaist.iclab.tracker.sensor.phone.TimingSensor.Config.fromJson]); most rows leave it null and
 *   are only used for the active-sensor allowlist.
 */
@Serializable
data class CampaignTableData(
    val id: Long? = null,
    @SerialName("campaign_id")
    val campaignId: Long,
    val name: String,
    val description: String? = null,
    @SerialName("daily_count_max")
    val dailyCountMax: Int? = null,
    @SerialName("is_custom")
    val isCustom: Boolean,
    @SerialName("display_name")
    val displayName: String,
    val config: JsonElement? = null,
)
