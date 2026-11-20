package com.example.data.model

/** One audio codec the SIP engine can offer, as shown in Settings → Audio Codecs. */
data class AudioCodec(
    /** Stable id stored in settings, e.g. "PCMU/8000" or "opus/48000". */
    val key: String,
    val mimeType: String,
    val clockRate: Int,
    val enabled: Boolean
) {
    /** Human-readable name, e.g. "G.711 µ-law (PCMU)". */
    val displayName: String
        get() = when (mimeType.uppercase()) {
            "PCMU" -> "G.711 µ-law (PCMU)"
            "PCMA" -> "G.711 A-law (PCMA)"
            "G722" -> "G.722 (HD voice)"
            "G729" -> "G.729"
            "OPUS" -> "Opus"
            "SPEEX" -> "Speex"
            "GSM" -> "GSM"
            "ILBC" -> "iLBC"
            "BV16" -> "BroadVoice 16"
            "L16" -> "Linear PCM (L16)"
            "AMR" -> "AMR"
            "AMR-WB" -> "AMR-WB (HD voice)"
            else -> mimeType
        }

    /** Short line under the name: sample rate and typical bandwidth. */
    val details: String
        get() {
            val khz = if (clockRate % 1000 == 0) "${clockRate / 1000} kHz" else "${clockRate / 1000.0} kHz"
            val bandwidth = when (mimeType.uppercase()) {
                "PCMU", "PCMA" -> " • 64 kbps • works with every PBX"
                "G722" -> " • 64 kbps"
                "G729" -> " • 8 kbps • low data"
                "OPUS" -> " • 6–510 kbps • best quality"
                "GSM" -> " • 13 kbps"
                "ILBC" -> " • 13–15 kbps"
                "SPEEX" -> " • low data"
                else -> ""
            }
            return khz + bandwidth
        }

    companion object {
        fun keyOf(mimeType: String, clockRate: Int) = "$mimeType/$clockRate"

        /**
         * G.711 only. iTelSwitchPlus answers re-INVITEs by echoing the whole offer instead of
         * picking one codec, so with more codecs offered a 3-way conference could switch a leg
         * to opus while the carrier kept sending PCMU (noise / no audio).
         */
        val DEFAULT_ENABLED: Set<String> = setOf("PCMU/8000", "PCMA/8000")
    }
}
