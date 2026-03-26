package com.example.posdemo.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.widget.Toast
import com.urovo.sdk.utils.SystemProperties.getSystemProperty

import org.bouncycastle.asn1.ASN1Boolean
import org.bouncycastle.asn1.ASN1Enumerated
import org.bouncycastle.asn1.ASN1InputStream
import org.bouncycastle.asn1.ASN1Integer
import org.bouncycastle.asn1.ASN1OctetString
import org.bouncycastle.asn1.ASN1Sequence
import org.bouncycastle.asn1.ASN1TaggedObject
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.cert.X509Certificate
import java.util.regex.Matcher
import java.util.regex.Pattern

object DeviceInfoUtil {

    fun getWifiIpv4(context: Context): String? {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val ipInt = wifiManager.connectionInfo.ipAddress
        if (ipInt == 0) return null

        return "${ipInt and 0xff}.${ipInt shr 8 and 0xff}.${ipInt shr 16 and 0xff}.${ipInt shr 24 and 0xff}"
    }

    fun checkDeviceAttestation(keyAlias: String): String {
        var result = ""
        runCatching {
            // Generate a key with attestation
            val keyPairGenerator = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_EC,
                "AndroidKeyStore"
            )

            val challenge = "test_challenge".toByteArray()

            val spec = KeyGenParameterSpec.Builder(keyAlias, KeyProperties.PURPOSE_SIGN)
                .setAlgorithmParameterSpec(java.security.spec.ECGenParameterSpec("secp256r1"))
                .setDigests(KeyProperties.DIGEST_SHA256)
                .setAttestationChallenge(challenge)
                .build()

            keyPairGenerator.initialize(spec)
            keyPairGenerator.generateKeyPair()

            // Get the certificate chain
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            val certs = keyStore.getCertificateChain(keyAlias)

            if (certs.isNullOrEmpty()) {
                return "Attestation FAIL: No Cert Chain found!"
            }

            val attestationCert = certs[0] as X509Certificate
            val extensionData = attestationCert.getExtensionValue("1.3.6.1.4.1.11129.2.1.17") // ATTESTATION_EXTENSION_OID

            if (extensionData == null) {
                return "Attestation FAIL: No Extension Found!"
            }

            // Parse the attestation extension
            result = parseAttestationExtension(extensionData)

            // Cleanup
            keyStore.deleteEntry(keyAlias)
        }.onFailure {
            return "No have Attestation Key"
        }
        return result
    }

    fun getLanguageType(): String {
        return if (getSystemProperty(
                "pwv.custom.enbuild",
                "false"
            ) == "true"
        ) "english" else "chinese"
    }

    fun getModelType(): String {
        val devType: String? = getDevType()
        if (devType == "SQ52T" || devType == "SQ27T" || devType == "SQ27TE" || devType == "SQ27TD" || devType == "SQ42T" || devType == "SQ27TC" || devType == "SQ43T" || devType == "SQ46" || devType == "SQ51" || devType == "SQ52" || devType == "SQ31T" || devType == "SQ51C" || devType == "SQ51CW" || devType == "SQ46W") {
            val str = getSystemProperty("pwv.rf.type", "WE")
            return str
        }
        val str2: String = getSystemProperty("persist.radio.multisim.config", "")
        return if (str2 == "dsds") "DS (Dual SIM)" else "WE (Without Extra SIM)"
    }


    fun getOSVersion(): String {
        var strGroup: String?
        if (Build.VERSION.SDK_INT >= 31) {
            strGroup = getSystemProperty("ro.build.display.id", Build.ID)
        } else {
            strGroup = getSystemProperty("ro.vendor.build.id", Build.ID)
        }
        val matcher: Matcher = Pattern.compile(".*(\\d{6}_\\d{2}).*").matcher(strGroup)
        if (matcher.find()) {
            strGroup = matcher.group(1)
        }
        val strArrSplit: Array<String?> =
            strGroup.split("_".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        var strSubstring = Build.VERSION.RELEASE
        if (strSubstring.contains(".")) {
            strSubstring = strSubstring.substring(0, strSubstring.indexOf("."))
        }
        val sb = java.lang.StringBuilder(strSubstring)
        val iCovertInt: Int = covertInt(strArrSplit[strArrSplit.size - 2])
        sb.append(".")
        sb.append(iCovertInt / 10000)
        val i = iCovertInt % 10000
        if (i >= 1000) {
            sb.append(".")
            sb.append(i)
        } else if (100 <= i) {
            sb.append(".0")
            sb.append(i)
        } else if (10 <= i) {
            sb.append(".00")
            sb.append(i)
        } else if (i >= 0) {
            sb.append(".000")
            sb.append(i)
        }
        val iCovertInt2: Int = covertInt(strArrSplit[strArrSplit.size - 1])
        if (iCovertInt2 >= 0 && iCovertInt2 < 10) {
            sb.append(".0")
            sb.append(iCovertInt2)
        } else {
            sb.append(".")
            sb.append(iCovertInt2)
        }
        return getCustomName() + " - " + sb.toString()
    }

    private fun getCustomName(): String {
        val pwvCustom = getPWVCUSTOM()
        val pwvCustomAttach = getPWVCUSTOMATTACH()
        if (pwvCustom == "XX") {
            val signed = getSystemProperty("pwv.custom.sign", "false")
            return if (signed == "true") {
                "StandardOS-S"
            } else {
                "StandardOS-N"
            }
        }
        val candidate: String? = if (pwvCustomAttach.equals("XX", ignoreCase = true)) {
            pwvCustom
        } else {
            pwvCustomAttach
        }
        return candidate ?: ""
    }

    private fun getPWVCUSTOM(): String? {
        var str: String?
        try {
            str = getSystemProperty("pwv.custom.custom", "")
        } catch (e: java.lang.Exception) {
            str = ""
        }
        return str
    }

    private fun getPWVCUSTOMATTACH(): String? {
        var str: String?
        try {
            str = getSystemProperty("pwv.custom.custom.attach", "")
        } catch (e: java.lang.Exception) {
            str = ""
        }
        return str
    }

    fun getUFSVersion(): String {
        val strArr = arrayOf<String?>("0.0.0.0", "PKG-XX")
        strArr[0] = getSystemProperty("ro.ufs.build.version", "0.0.0.0")
        strArr[1] = "PKG-" + getSystemProperty("ro.ufs.custom", "XX")
        return strArr[1] + " - " + strArr[0]
    }

    fun getSEVersion(): String {
        val str: String? = getSystemProperty("urv.se.version", "")
        if (str == null || str == "") {
            return Build.VERSION.RELEASE + ".0.0.0"
        }
        val iIndexOf = str.indexOf("V")
        if (iIndexOf == -1) {
            return Build.VERSION.RELEASE + ".0.0.0"
        }
        return getSECustomName() + " - " + Build.VERSION.RELEASE + "." + str.substring(iIndexOf + 1, iIndexOf + 6)
    }

    fun getSECustomName(): String {
        return if (getSystemProperty("pwv.custom.sign", "false") == "true") "SEFW-S" else "SEFW-N"
    }

    private fun covertInt(str: String?): Int {
        return str?.toInt() ?: 0
    }

    fun getDevType(): String {
        return getSystemProperty("pwv.project", "no result found!")
    }

    fun getNetworkType(context: Context): String? {
        val activeNetworkInfo = (context
            .getSystemService("connectivity") as ConnectivityManager).activeNetworkInfo
        if (activeNetworkInfo == null) {
            return null
        }
        return if (activeNetworkInfo.type == 1) "Wifi" else "4G"
    }

    private fun parseAttestationExtension(extensionData: ByteArray): String {
        val asn1 = ASN1InputStream(extensionData).readObject()
        val extensionSeq = ASN1InputStream((asn1 as ASN1OctetString).octets)
            .readObject() as ASN1Sequence

        // Attestation version is at index 0
        val attestationVersion = (extensionSeq.getObjectAt(0) as ASN1Integer).value.toInt()

        // RootOfTrust is at index 6 in KeyDescription
        val teeEnforced = extensionSeq.getObjectAt(7) as ASN1Sequence

        for (i in 0 until teeEnforced.size()) {
            val item = teeEnforced.getObjectAt(i)
            if (item is ASN1TaggedObject) {
                when (item.tagNo) {
                    704 -> { // RootOfTrust
                        val rootOfTrust = item.baseObject as ASN1Sequence
                        return parseRootOfTrust(rootOfTrust)
                    }
                }
            }
        }
        return "???"
    }

    private fun parseRootOfTrust(rootOfTrust: ASN1Sequence): String {
        // RootOfTrust structure:
        // [0] verifiedBootKey OCTET STRING
        // [1] deviceLocked BOOLEAN
        // [2] verifiedBootState VerifiedBootState (ENUMERATED)
        // [3] verifiedBootHash OCTET STRING (optional)

        if (rootOfTrust.size() >= 3) {
            val deviceLocked = (rootOfTrust.getObjectAt(1) as ASN1Boolean).isTrue
            val verifiedBootState = (rootOfTrust.getObjectAt(2) as ASN1Enumerated).value.toInt()

            val bootStateString = when (verifiedBootState) {
                0 -> "VERIFIED"
                1 -> "SELF_SIGNED"
                2 -> "UNVERIFIED"
                3 -> "FAILED"
                else -> "UNKNOWN($verifiedBootState)"
            }
            return buildString {
                append(" - HAVE ATTESTATION(GOOGLE) KEY\n")
                append(" - DeviceLocked: $deviceLocked\n")
                append(" - VerifiedBootState: $bootStateString\n")
                if (deviceLocked && bootStateString == "VERIFIED") {
                    append("This terminal CAN pass Play Integrity")
                }
            }
        }
        return "???"
    }


}