import java.io.ByteArrayInputStream
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

object CertUtil {

    fun parseCert(certBytes: ByteArray): String {
        return try {
            val cf = CertificateFactory.getInstance("X.509")
            val cert = cf.generateCertificate(
                ByteArrayInputStream(certBytes)
            ) as X509Certificate

            buildString {
                appendLine("Subject: ${cert.subjectX500Principal.name}")
                appendLine("Issuer: ${cert.issuerX500Principal.name}")
                appendLine("Serial Number: ${cert.serialNumber}")
                appendLine("Valid From: ${cert.notBefore}")
                appendLine("Valid To: ${cert.notAfter}")
                appendLine("Signature Algorithm: ${cert.sigAlgName}")
                appendLine("Public Key Algorithm: ${cert.publicKey.algorithm}")
            }

        } catch (e: Exception) {
            "Parse certificate failed: ${e.message}"
        }
    }
}