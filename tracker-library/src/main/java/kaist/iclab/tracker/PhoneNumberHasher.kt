package kaist.iclab.tracker

import android.content.Context
import android.telephony.PhoneNumberUtils
import android.telephony.TelephonyManager
import java.util.Locale
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.text.ifEmpty

/**
 * Encrypts phone number and pseudonymizes it.
 * The hashing is "participant-specific"; Same number in different participants result in different hashes.
 * */
class PhoneNumberHasher(
    context: Context
) {
    private val numberHasher = Mac.getInstance("HmacSHA256").apply {
        init(SecretKeySpec(MetaData(context).deviceUuid.toByteArray(), "HmacSHA256"))
    }

    private val countryIso: String = context.getSystemService(TelephonyManager::class.java)
        ?.let { it.simCountryIso.ifEmpty { it.networkCountryIso } }
        ?.ifEmpty { null }
        ?.uppercase(Locale.ROOT)
        ?: Locale.getDefault().country

    /**
     * Normalize to E.164 so that differently formatted forms of the same number
     * (e.g. "010-1234-5678" and "+821012345678") hash identically.
     * If the given string is not a valid phone number, it returns an identical string.
     */
    private fun normalizeNumber(number: String): String =
        PhoneNumberUtils.formatNumberToE164(number, countryIso) ?: number

    fun hash(number: String): String = synchronized(numberHasher) {
        numberHasher.doFinal(normalizeNumber(number).toByteArray()).joinToString("") { "%02x".format(it) }
    }
}