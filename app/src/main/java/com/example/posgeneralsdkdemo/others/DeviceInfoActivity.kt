package com.example.posgeneralsdkdemo.others

import android.content.pm.PackageManager
import android.device.DeviceManager
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.posgeneralsdkdemo.R
import com.urovo.sdk.utils.SystemProperties.getSystemProperty
import java.nio.charset.StandardCharsets
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.cert.X509Certificate
import java.security.spec.ECGenParameterSpec
import java.util.Arrays
import java.util.Date
import java.util.regex.Matcher
import java.util.regex.Pattern

// <uses-permission android:name="android.permission.ACCESS_WIFI_STATE"/>
// <uses-permission android:name="android.permission.CHANGE_WIFI_STATE"/>
// <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>

private const val GOOGLE_ROOT_PUBLIC_KEY = "MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAr7bHgiuxpwHsK7Qui8xUFmOr75gvMsd/dTEDDJdSSxtf6An7xyqpRR90PL2abxM1dEqlXnf2tqw1Ne4Xwl5jlRfdnJLmN0pTy/4lj4/7tv0Sk3iiKkypnEUtR6WfMgH0QZfKHM1+di+y9TFRtv6y//0rb+T+W8a9nsNL/ggjnar86461qO0rOs2cXjp3kOG1FEJ5MVmFmBGtnrKpa73XpXyTqRxB/M0n1n/W9nGqC4FSYa04T6N5RIZGBN2z2MT5IKGbFlbC8UrW0DxW7AYImQQcHtGl/m00QLVWutHQoVJYnFPlXTcHYvASLu+RhhsbDmxMgJJ0mcDpvsC4PjvB+TxywElgS70vE0XmLD+OJtvsBslHZvPBKCOdT0MS+tgSOIfga+z1Z1g7+DVagf7quvmag8jfPioyKvxnK/EgsTUVi2ghzq8wm27ud/mIM7AY2qEORR8Go3TVB4HzWQgpZrt3i5MIlCaY504LzSRiigHCzAPlHws+W0rB5N+er5/2pJKnfBSDiCiFAVtCLOZ7gLiMm0jhO2B6tUXHI/+MRPjy02i59lINMRRev56GKtcd9qO/0kUJWdZTdA2XoS82ixPvZtXQpUpuL12ab+9EaDK8Z4RHJYYfCT3Q5vNAXaiWQ+8PTWm2QgBR/bkwSWc+NpUFgNPN9PvQi8WEg5UmAGMCAwEAAQ=="
private const val TAG = "Patrick_DeviceInfoActivity"
const val PACKAGE_COMPONENT_INFO = "com.example.posgeneralsdkdemo/com.example.posgeneralsdkdemo.others.DeviceInfoActivity"
class DeviceInfoActivity : AppCompatActivity() {

    private val googleKey: ByteArray? = Base64.decode(GOOGLE_ROOT_PUBLIC_KEY, Base64.DEFAULT)

    private val tvResult by lazy { findViewById<TextView>(R.id.tvResult) }

    private lateinit var wifiManager: WifiManager

    private val deviceManager = DeviceManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_info)

        wifiManager = getSystemService(WIFI_SERVICE) as WifiManager
    }



    override fun onStart() {
        super.onStart()
        runCatching {
            tvResult.text = buildString {
                append("SN: ${deviceManager.deviceId}\n")
                append("Model: ${getDevType()}\n")
                append("Firmware: \n - OS: ${Build.ID}\n")
                append(" - UFS: ${deviceManager.getSettingProperty("ro.ufs.custom")}-${deviceManager.getSettingProperty("ro.ufs.build.version")}\n")
                append(" - SE: ${deviceManager.getSettingProperty("persist-urv.se.version")}\n\n")

                append("Attestation Key: ${keyAttestationTest()}\n")
                append("GMS: ${isPackageInstalled("com.google.android.gms")}\n")
                append("GSF: ${isPackageInstalled("com.google.android.gsf")}\n")
                append("Chrome: ${isPackageInstalled("com.android.chrome")}\n")
                append("PlayStore: ${isPackageInstalled("com.android.vending")}\n\n")

                append("OTA Firmware version:\n")
                append("OS version: \n - ${getOSVersion()}\n")
                append("UFS version: \n - ${getUFSVersion()}\n")
                append("SE version: \n - ${getSEVersion()}\n\n")

                append("Network type: ${getNetworkType()}\n")
                if ("unknown" in wifiManager.connectionInfo.ssid) {
                    append("WiFi status: Not connection to WiFi!\n")
                } else {
                    append("SSID: ${wifiManager.connectionInfo.ssid}\n")
                }
                append("WiFi MAC: ${DeviceManager().getSettingProperty("persist.sys.device.wifimac")}\n")
                append("WiFi Whitelist: ${DeviceManager().wifiWhiteList}\n")
                append("Device type: ${getModelType()}\n\n")

                append("Language: ${getLanguageType()}\n")
                append("IMEI1: ${deviceManager.getImei(1)}\n")
                append("IMEI2: ${deviceManager.getImei(2)}\n")
                append("Batter Percentage: ${deviceManager.batteryInfo.getInt("level")}\n")
                append("Batter plugged: ${deviceManager.batteryInfo.getInt("plugged")}\n")
                append("NTP server: ${deviceManager.getSettingProperty("Global-ntp_server")}\n")
                append("TimeZone: ${deviceManager.getSettingProperty("persist-persist.sys.timezone")} - ${deviceManager.getSettingProperty("persist-persist.sys.settimezone")}\n\n")

                append("UMS: ${isPackageInstalled("com.urovo.uhome")}\n")
                append("AppMarket_UMS: ${isPackageInstalled("com.urovo.appmarket")}\n")
                append("UTMS: ${isPackageInstalled("com.urovo.utms")}\n")
                append("AppMarket_UTMS: ${isPackageInstalled("com.urovo.utms.appmarket")}\n")
            }
        }.onFailure {
            tvResult.text = it.message
            it.printStackTrace()
        }
    }


    private fun getDevType(): String {
        return getSystemProperty("pwv.project", "no result found!")
    }


    private fun getModelType(): String {
        val devType: String? = getDevType()
        if (devType == "SQ52T" || devType == "SQ27T" || devType == "SQ27TE" || devType == "SQ27TD" || devType == "SQ42T" || devType == "SQ27TC" || devType == "SQ43T" || devType == "SQ46" || devType == "SQ51" || devType == "SQ52" || devType == "SQ31T" || devType == "SQ51C" || devType == "SQ51CW" || devType == "SQ46W") {
            val str = getSystemProperty("pwv.rf.type", "WE")
            return str
        }
        val str2: String = getSystemProperty("persist.radio.multisim.config", "")
        Log.i("patrick", str2)
        return if (str2 == "dsds") "DS (Dual SIM)" else "WE (Without Extra SIM)"
    }


    private fun getNetworkType(): String? {
        val activeNetworkInfo = (this
            .getSystemService("connectivity") as ConnectivityManager).activeNetworkInfo
        if (activeNetworkInfo == null) {
            return null
        }
        return if (activeNetworkInfo.type == 1) "Wifi" else "4G"
    }

    private fun getLanguageType(): String {
        return if (getSystemProperty(
                "pwv.custom.enbuild",
                "false"
            ) == "true"
        ) "english" else "chinese"
    }

    private fun getOSVersion(): String {
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

    private fun getUFSVersion(): String {
        val strArr = arrayOf<String?>("0.0.0.0", "PKG-XX")
        strArr[0] = getSystemProperty("ro.ufs.build.version", "0.0.0.0")
        strArr[1] = "PKG-" + getSystemProperty("ro.ufs.custom", "XX")
        return strArr[1] + " - " + strArr[0]
    }

    private fun getSEVersion(): String {
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

    private fun keyAttestationTest(): Boolean {
        try {
            // 1. Generate an EC attestation key in AndroidKeyStore
            val keyPairGenerator =
                KeyPairGenerator.getInstance("EC", "AndroidKeyStore")

            val now = Date()

            val spec = KeyGenParameterSpec.Builder("Key1", KeyProperties.PURPOSE_SIGN)
                .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                .setDigests(
                    KeyProperties.DIGEST_SHA256,
                    KeyProperties.DIGEST_SHA384,
                    KeyProperties.DIGEST_SHA512
                )
                .setKeyValidityStart(now)
                .setAttestationChallenge("hello world".toByteArray(StandardCharsets.UTF_8))
                .build()

            keyPairGenerator.initialize(spec)
            keyPairGenerator.generateKeyPair()

            // 2. Get attestation Cert Chain from AndroidKeyStore
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            val certificateChain = keyStore.getCertificateChain("Key1")

            if (certificateChain == null || certificateChain.size == 0) {
                return false
            }

            Log.d(TAG, "Key Attestion CODE_SUPPORT, chain length=" + certificateChain.size)

            // 3. Find if there's any Certificate that its publicKey == GOOGLE_ROOT_PUBLIC_KEY
            for (i in certificateChain.indices.reversed()) {
                val x509Certificate: X509Certificate = certificateChain[i] as X509Certificate
                val pubEncoded: ByteArray? = x509Certificate.getPublicKey().getEncoded()
                if (Arrays.equals(pubEncoded, googleKey)) {
                    Log.d(TAG, "cert[" + i + "] matches GOOGLE_ROOT_PUBLIC_KEY")
                    return true
                }
            }
            return false
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    private fun covertInt(str: String?): Int {
        return str?.toInt() ?: 0
    }

    private fun isPackageInstalled(packageName: String): Boolean {
        return try {
            this.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }
}