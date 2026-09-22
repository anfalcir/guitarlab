package studio.guitarlab.app.ui

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import studio.guitarlab.core.audio.RecordingSessionHealthPolicy
import studio.guitarlab.core.audio.RecordingSessionHealthRecord
import studio.guitarlab.core.audio.RecordingTimingEvidenceBasis

/** Small local diagnostic history. No audio content, paths or transient Android device IDs. */
class RecordingSessionHealthStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun history(): List<RecordingSessionHealthRecord> {
        val raw = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    decode(array.optJSONObject(index) ?: continue)?.let(::add)
                }
            }.takeLast(RecordingSessionHealthPolicy.MAX_HISTORY)
        }.getOrDefault(emptyList())
    }

    @Synchronized
    fun append(record: RecordingSessionHealthRecord) {
        val bounded = RecordingSessionHealthPolicy.bounded(history(), record)
        val array = JSONArray()
        bounded.forEach { array.put(encode(it)) }
        prefs.edit().putString(KEY_HISTORY, array.toString()).apply()
    }

    fun latest(): RecordingSessionHealthRecord? = history().lastOrNull()

    private fun encode(record: RecordingSessionHealthRecord): JSONObject = JSONObject().apply {
        put("completedAt", record.completedAtEpochMs)
        putNullable("selectedInput", record.selectedInputIdentity)
        putNullable("effectiveInput", record.effectiveInputIdentity)
        putNullable("selectedOutput", record.selectedOutputIdentity)
        putNullable("effectiveOutput", record.effectiveOutputIdentity)
        put("sampleRate", record.sampleRateHz)
        put("basis", record.timingEvidenceBasis.name)
        putNullable("captureJitter", record.captureAnchorJitterNs)
        putNullable("backingJitter", record.backingAnchorJitterNs)
        putNullable("captureObs", record.captureAnchorObservations)
        putNullable("backingObs", record.backingAnchorObservations)
        put("delta", record.sessionDeltaFrames)
        put("routeLatency", record.acceptedRouteLatencyFrames)
        put("fine", record.residualFineAdjustmentFrames)
        put("fallback", record.outputFallback)
        put("routeChanged", record.routeChanged)
        put("captured", record.capturedFrames)
        putNullable("timelineStart", record.finalTimelineStartFrame)
        putNullable("sourceStart", record.finalSourceStartFrame)
        putNullable("length", record.finalLengthFrames)
        put("zeroReads", record.inputZeroReadEvents)
        putNullable("underruns", record.outputUnderrunCount)
        put("completed", record.completed)
        putNullable("failure", record.failureReason?.take(MAX_FAILURE_CHARS))
    }

    private fun decode(json: JSONObject): RecordingSessionHealthRecord? = runCatching {
        RecordingSessionHealthRecord(
            completedAtEpochMs = json.getLong("completedAt"),
            selectedInputIdentity = json.optNullableString("selectedInput"),
            effectiveInputIdentity = json.optNullableString("effectiveInput"),
            selectedOutputIdentity = json.optNullableString("selectedOutput"),
            effectiveOutputIdentity = json.optNullableString("effectiveOutput"),
            sampleRateHz = json.getInt("sampleRate"),
            timingEvidenceBasis = RecordingTimingEvidenceBasis.valueOf(json.getString("basis")),
            captureAnchorJitterNs = json.optNullableLong("captureJitter"),
            backingAnchorJitterNs = json.optNullableLong("backingJitter"),
            captureAnchorObservations = json.optNullableInt("captureObs"),
            backingAnchorObservations = json.optNullableInt("backingObs"),
            sessionDeltaFrames = json.getLong("delta"),
            acceptedRouteLatencyFrames = json.getLong("routeLatency"),
            residualFineAdjustmentFrames = json.getLong("fine"),
            outputFallback = json.optBoolean("fallback", false),
            routeChanged = json.optBoolean("routeChanged", false),
            capturedFrames = json.getLong("captured"),
            finalTimelineStartFrame = json.optNullableLong("timelineStart"),
            finalSourceStartFrame = json.optNullableLong("sourceStart"),
            finalLengthFrames = json.optNullableLong("length"),
            inputZeroReadEvents = json.optInt("zeroReads", 0),
            outputUnderrunCount = json.optNullableInt("underruns"),
            completed = json.optBoolean("completed", false),
            failureReason = json.optNullableString("failure"),
        )
    }.getOrNull()

    private fun JSONObject.putNullable(key: String, value: Any?) {
        if (value == null) put(key, JSONObject.NULL) else put(key, value)
    }

    private fun JSONObject.optNullableString(key: String): String? =
        if (!has(key) || isNull(key)) null else optString(key).takeIf { it.isNotBlank() }

    private fun JSONObject.optNullableLong(key: String): Long? =
        if (!has(key) || isNull(key)) null else optLong(key)

    private fun JSONObject.optNullableInt(key: String): Int? =
        if (!has(key) || isNull(key)) null else optInt(key)

    private companion object {
        const val PREFS_NAME = "recording-session-health"
        const val KEY_HISTORY = "history-v1"
        const val MAX_FAILURE_CHARS = 240
    }
}
