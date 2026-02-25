package com.example.posdemo.utils

import android.content.Context
import android.net.wifi.WifiManager
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.widget.Toast
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