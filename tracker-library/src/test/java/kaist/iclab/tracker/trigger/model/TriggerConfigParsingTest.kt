package kaist.iclab.tracker.trigger.model

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class TriggerConfigParsingTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun action(raw: String): TriggerActionConfig =
        json.decodeFromString(TriggerActionConfigSerializer, raw)

    private fun condition(raw: String): ConditionNode =
        json.decodeFromString(ConditionNodeSerializer, raw)

    // --- Actions ---

    @Test
    fun `watch_ema parses survey id and cooldown`() {
        val parsed = action("""{"kind":"watch_ema","surveyId":34,"minIntervalMillis":60000}""")
        assertEquals(TriggerActionConfig.WatchEma(surveyId = 34, minIntervalMillis = 60_000), parsed)
    }

    @Test
    fun `missing minIntervalMillis means no cooldown`() {
        val parsed = action("""{"kind":"watch_ema","surveyId":34}""")
        assertEquals(0L, parsed.minIntervalMillis)
    }

    @Test
    fun `decimal minIntervalMillis is accepted instead of dropping the trigger`() {
        val parsed = action("""{"kind":"ema","surveyId":7,"minIntervalMillis":60000.0}""")
        assertEquals(60_000L, parsed.minIntervalMillis)
    }

    @Test
    fun `snake_case survey_id is accepted`() {
        val parsed = action("""{"kind":"ema","survey_id":7}""")
        assertEquals(TriggerActionConfig.Ema(surveyId = 7, minIntervalMillis = 0), parsed)
    }

    @Test
    fun `notification with null url has no url, not the string null`() {
        val parsed = action(
            """{"kind":"notification","title":"T","description":"D","url":null,"minIntervalMillis":0}"""
        ) as TriggerActionConfig.Notification
        assertNull(parsed.url)
        assertEquals(0, parsed.deviceType)
    }

    @Test
    fun `notification device_type alias selects the watch`() {
        val parsed = action(
            """{"kind":"notification","title":"T","description":"D","device_type":1}"""
        ) as TriggerActionConfig.Notification
        assertEquals(1, parsed.deviceType)
    }

    @Test
    fun `broadcast extras default their value type to String`() {
        val parsed = action(
            """{"kind":"broadcast","action":"a.b.C","extras":[{"key":"k","value":"v"}]}"""
        ) as TriggerActionConfig.Broadcast
        assertEquals(listOf(BroadcastExtra("k", "v", "String")), parsed.extras)
    }

    @Test
    fun `unknown action kind is rejected`() {
        assertThrows(SerializationException::class.java) { action("""{"kind":"teleport"}""") }
    }

    @Test
    fun `watch_ema without a survey id is rejected`() {
        assertThrows(SerializationException::class.java) { action("""{"kind":"watch_ema"}""") }
    }

    // --- Conditions ---

    @Test
    fun `nested condition tree parses`() {
        val parsed = condition(
            """
            {"type":"and","children":[
              {"type":"detection","sensor":"gesture","value":"Clapping"},
              {"type":"not","child":{"type":"or","children":[
                {"type":"detection","sensor":"stress","value":"High"}
              ]}}
            ]}
            """
        )
        val expected = ConditionNode.And(
            listOf(
                ConditionNode.Detection("gesture", "Clapping"),
                ConditionNode.Not(ConditionNode.Or(listOf(ConditionNode.Detection("stress", "High"))))
            )
        )
        assertEquals(expected, parsed)
    }

    @Test
    fun `condition survives a serialize and parse round trip`() {
        val original = ConditionNode.Or(
            listOf(ConditionNode.Detection("gesture", "Knocking"), ConditionNode.Detection("gesture", "Drinking"))
        )
        val roundTripped = condition(json.encodeToString(ConditionNodeSerializer, original))
        assertEquals(original, roundTripped)
    }

    @Test
    fun `unknown condition type is rejected`() {
        assertThrows(SerializationException::class.java) { condition("""{"type":"xor","children":[]}""") }
    }

    @Test
    fun `detection without a value is rejected`() {
        assertThrows(SerializationException::class.java) { condition("""{"type":"detection","sensor":"gesture"}""") }
    }

    // --- Which sensors a condition reads (the engine only re-checks a trigger for these) ---

    @Test
    fun `referenced sensors cover every branch of the tree`() {
        val tree = condition(
            """
            {"type":"and","children":[
              {"type":"detection","sensor":"gesture","value":"Clapping"},
              {"type":"not","child":{"type":"or","children":[
                {"type":"detection","sensor":"stress","value":"High"},
                {"type":"detection","sensor":"gesture","value":"Knocking"}
              ]}}
            ]}
            """
        )
        assertEquals(setOf("gesture", "stress"), tree.referencedSensors())
    }

    @Test
    fun `a gesture-only trigger does not reference activity or stress`() {
        val tree = condition("""{"type":"detection","sensor":"gesture","value":"Clapping"}""")
        assertEquals(setOf("gesture"), tree.referencedSensors())
    }

    // --- Whole trigger ---

    @Test
    fun `full trigger parses with condition and actions`() {
        val parsed = json.decodeFromString(
            ParsedCampaignTrigger.serializer(),
            """
            {"id":5,"campaignId":1,"name":"Clap",
             "condition":{"type":"detection","sensor":"gesture","value":"Clapping"},
             "actions":[{"kind":"watch_ema","surveyId":34,"minIntervalMillis":30000}]}
            """
        )
        assertEquals(5, parsed.id)
        assertEquals(ConditionNode.Detection("gesture", "Clapping"), parsed.condition)
        assertTrue(parsed.actions.single() is TriggerActionConfig.WatchEma)
    }
}
