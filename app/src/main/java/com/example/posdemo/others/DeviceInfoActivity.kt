package com.example.posdemo.others

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.device.DeviceManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import com.example.posdemo.databinding.ActivityDeviceInfoBinding
import com.example.posdemo.maps.LocationActivity
import com.example.posdemo.utils.DeviceInfoUtil
import com.example.posdemo.utils.PermissionUtil
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.play.core.integrity.IntegrityManager
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import com.urovo.sdk.utils.SystemProperties.getSystemProperty
import java.nio.charset.StandardCharsets
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.cert.X509Certificate
import java.security.spec.ECGenParameterSpec
import java.util.Arrays
import java.util.Date
import java.util.UUID
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.math.log

// <uses-permission android:name="android.permission.ACCESS_WIFI_STATE"/>
// <uses-permission android:name="android.permission.CHANGE_WIFI_STATE"/>
// <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>

class DeviceInfoActivity : AppCompatActivity() {

    companion object {
        private const val GOOGLE_ROOT_PUBLIC_KEY = "MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAr7bHgiuxpwHsK7Qui8xUFmOr75gvMsd/dTEDDJdSSxtf6An7xyqpRR90PL2abxM1dEqlXnf2tqw1Ne4Xwl5jlRfdnJLmN0pTy/4lj4/7tv0Sk3iiKkypnEUtR6WfMgH0QZfKHM1+di+y9TFRtv6y//0rb+T+W8a9nsNL/ggjnar86461qO0rOs2cXjp3kOG1FEJ5MVmFmBGtnrKpa73XpXyTqRxB/M0n1n/W9nGqC4FSYa04T6N5RIZGBN2z2MT5IKGbFlbC8UrW0DxW7AYImQQcHtGl/m00QLVWutHQoVJYnFPlXTcHYvASLu+RhhsbDmxMgJJ0mcDpvsC4PjvB+TxywElgS70vE0XmLD+OJtvsBslHZvPBKCOdT0MS+tgSOIfga+z1Z1g7+DVagf7quvmag8jfPioyKvxnK/EgsTUVi2ghzq8wm27ud/mIM7AY2qEORR8Go3TVB4HzWQgpZrt3i5MIlCaY504LzSRiigHCzAPlHws+W0rB5N+er5/2pJKnfBSDiCiFAVtCLOZ7gLiMm0jhO2B6tUXHI/+MRPjy02i59lINMRRev56GKtcd9qO/0kUJWdZTdA2XoS82ixPvZtXQpUpuL12ab+9EaDK8Z4RHJYYfCT3Q5vNAXaiWQ+8PTWm2QgBR/bkwSWc+NpUFgNPN9PvQi8WEg5UmAGMCAwEAAQ=="
        private const val TAG = "Patrick_DeviceInfoActivity"
        private val PERMISSION_LOCATION = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        private const val REQ_PERMISSION_LOCATION = 1001

        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "attestation_key_alias"
    }

    private lateinit var binding: ActivityDeviceInfoBinding


    private lateinit var wifiManager: WifiManager

    private val deviceManager = DeviceManager()

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnRunAttestation.setOnClickListener { onRunAttestationButtonClicked() }
        binding.btnRunPlayIntegrity.setOnClickListener { onRunPlayIntegrityButtonClicked() }

        wifiManager = getSystemService(WIFI_SERVICE) as WifiManager
        binding.btnGetLocationBaidu.setOnClickListener {
            if (!PermissionUtil.requestPermissions(this, PERMISSION_LOCATION,REQ_PERMISSION_LOCATION)) {
                Toast.makeText(this, "Please grant Location Permission first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startActivity(Intent(this, LocationActivity::class.java))
        }
        binding.btnGetLocationGoogle.setOnClickListener { onGetLocationGoogleButtonClicked() }

        runCatching {
            binding.tvResult.text = buildString {
                append("SN: ${deviceManager.deviceId}\n")
                append("Model(Ext): ${DeviceManager().getSettingProperty("ro.product.vendor.model")}\n")
                append("Model: ${getDevType()}\n")
                append("Firmware: \n - OS: ${Build.ID}\n")
                append(" - UFS: ${deviceManager.getSettingProperty("ro.ufs.custom")}-${deviceManager.getSettingProperty("ro.ufs.build.version")}\n")
                append(" - SE: ${deviceManager.getSettingProperty("persist-urv.se.version")}\n\n")

                append("GSF: ${isPackageInstalled("com.google.android.gsf")}\n")
                append("GMS: ${isPackageInstalled("com.google.android.gms")} (available: ${GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this@DeviceInfoActivity) == ConnectionResult.SUCCESS})\n")
                append("PlayStore: ${isPackageInstalled("com.android.vending")}\n")
                append("Chrome: ${isPackageInstalled("com.android.chrome")}\n")
                append("Maps: ${isPackageInstalled("com.google.android.apps.maps")}\n\n")

                append("OTA Firmware version:\n")
                append("OS version: \n - ${getOSVersion()}\n")
                append("UFS version: \n - ${getUFSVersion()}\n")
                append("SE version: \n - ${getSEVersion()}\n\n")

                append("Network type: ${getNetworkType()}\n")
                if ("unknown" in wifiManager.connectionInfo.ssid) {
                    append("WiFi status: Not connection to WiFi!\n")
                } else {
                    append("SSID: ${wifiManager.connectionInfo.ssid}\n")
                    append("WiFi IP: ${DeviceInfoUtil.getWifiIpv4(this@DeviceInfoActivity)}\n")
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
                append("TimeZone: ${deviceManager.getSettingProperty("persist-persist.sys.timezone")} - ${deviceManager.getSettingProperty("persist-persist.sys.settimezone")}\n")
                append("Top APP: ${deviceManager.topPackageName}\n\n")

                append("UMS: ${isPackageInstalled("com.urovo.uhome")}\n")
                append("AppMarket_UMS: ${isPackageInstalled("com.urovo.appmarket")}\n")
                append("UTMS: ${isPackageInstalled("com.urovo.utms")}\n")
                append("AppMarket_UTMS: ${isPackageInstalled("com.urovo.utms.appmarket")}\n\n")

                append("Location Providers: \n${getLocationProviders()}\n")
                append("GPS enabled = ${(getSystemService(LOCATION_SERVICE) as LocationManager).isProviderEnabled(LocationManager.GPS_PROVIDER)}\n")
                append("Network enabled = ${(getSystemService(LOCATION_SERVICE) as LocationManager).isProviderEnabled(LocationManager.NETWORK_PROVIDER)}\n\n")
                append("Attestation Status:\n")
                append(DeviceInfoUtil.checkDeviceAttestation(KEY_ALIAS))
            }
        }.onFailure {
            binding.tvResult.text = it.message
            it.printStackTrace()
        }
    }


    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun onGetLocationGoogleButtonClicked() {
        if (!PermissionUtil.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION), 1001)) {
            Toast.makeText(this, "Please grant permission first", Toast.LENGTH_SHORT).show()
            return
        }
        if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this) != ConnectionResult.SUCCESS) {
            Toast.makeText(this, "GMS not available", Toast.LENGTH_SHORT).show()
            return
        }

        val lm = getSystemService(LOCATION_SERVICE) as LocationManager
        val msg = buildString {
            append("GPS enabled = ${lm.isProviderEnabled(LocationManager.GPS_PROVIDER)}\n")
            append("High Accuracy = ${lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)}\n")
        }
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

        val client = LocationServices.getFusedLocationProviderClient(this)
        val tokenSource = CancellationTokenSource()
        client.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            tokenSource.token
        ).addOnSuccessListener { loc ->
            if (loc != null) {
                binding.tvResult.text = "Lat=${loc.latitude}, Lng=${loc.longitude}, acc=${loc.accuracy}"
            }
        }.addOnFailureListener {
            binding.tvResult.text = "Failed: ${it.message}"
        }
    }


    /*
        1. Attestation process:
         - Google Backend sends a Challenge(Random Number / Nonce)
         - Terminal generates a new temporary EC KeyPair inside the AndroidKeyStore
         - Terminal requests Attestation with that Challenge (using AndroidKeyStore API)
         - TEE/SE uses Device's AttestationKey to Sign a Certificate for that new Key(using Challenge)
         - Terminals sends that Certificate to Google Backend for verification
            a. Validity of Certificate Chain (e.g. Expiry Date; Verify Signature...)
            b. Google Root Certificate (Google Self-signed)
            c. Challenge
         - Terminal invalidate that temporary EC KeyPair

        2. Understanding AndroidKeyStore:
         - AndroidKeyStore is a set of APIs provided by Android Native SDK
         - It's used by APP to communicate with TEE/SE
         - It could be TEE or SE under the hood up to the actual implementation
         - APP <---using APIs---> AndroidKeyStore <------> TEE/SE
     */
    private fun onRunAttestationButtonClicked() {
        runCatching {
            // 1. Generate a Random Number. In practice, this will be generate by Google and send to Terminal for Play Integrity
            val challenge = UUID.randomUUID().toString()

            // 2. Create an new Temporary EC KeyPair in KeyStore.
            val spec = KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_SIGN)
                // This new Temporary could be used for signing in the future.
                .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                // This EC Key is used for EC Encryption in the future
                .setDigests(KeyProperties.DIGEST_SHA256)
                // Hashing Algorithm of this EC key when signing
                .setAttestationChallenge(challenge.toByteArray(Charsets.UTF_8))
                // The Challenge to be put in the Certificate of this EC KeyPair
                .build()

            // 3. Generate the Key Pair specified above, and sign its Certificate using AttestationKey of the Terminal.
            KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, ANDROID_KEY_STORE).apply {
                initialize(spec)
                generateKeyPair()
            }

            // 4. Get CertificateChain(w/ Challenge in the Cert Extension) of that EC KeyPair just generated
            val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE)
            keyStore.load(null)
            val certificateChain = keyStore.getCertificateChain(KEY_ALIAS)
            if (certificateChain.isNullOrEmpty()) {
                Toast.makeText(this, "No Certificate Chain found", Toast.LENGTH_SHORT).show()
            }

            // 5. Check if the Root Cert is GOOGLE_ROOT_PUBLIC_KEY or not
            for (i in certificateChain.indices.reversed()) {
                val x509Certificate = certificateChain[i] as X509Certificate
                val publicKeyEncoded = x509Certificate.publicKey.encoded
                if (publicKeyEncoded.contentEquals(Base64.decode(GOOGLE_ROOT_PUBLIC_KEY, Base64.DEFAULT))) {
                    Toast.makeText(this, "Attestation success(Google Root Found)", Toast.LENGTH_SHORT).show()
                    return
                }
            }
            Toast.makeText(this, "Attestation failed!(No Google Root)", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            Log.e(TAG, "onRunAttestationButtonClicked: ${it.message}")
        }
    }


    private fun onRunPlayIntegrityButtonClicked() {
        runCatching {
            Toast.makeText(this, "Checking Play Integrity, please wait...", Toast.LENGTH_SHORT).show()
            // 0. Check if have Google(Attestation) Key, if no have, then Play Store will throw Integrity Exception
            if ("HAVE ATTESTATION(GOOGLE) KEY" !in DeviceInfoUtil.checkDeviceAttestation(KEY_ALIAS)) {
                throw Exception("Play Integrity FAIL: No have Google Key")
            }

            // 1. Create request to be sent to Google Server
            val integrityManager = IntegrityManagerFactory.create(this)
            val nonce = UUID.randomUUID().toString() // The Nonce is generated on the APP side
            val request = IntegrityTokenRequest.builder().setNonce(nonce).build()

            // 2. Send the request(including Nonce & other Terminal Info)
            integrityManager.requestIntegrityToken(request)
                .addOnSuccessListener { resp ->
                    val token = resp.token()
                    runOnUiThread {
                        Toast.makeText(this, "Play Integrity SUCCESS: len=${token.length}]", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "onRunPlayIntegrityButtonClicked: ${e.message.toString()}")
                    if ("Play Store" in e.message.toString()) {
                        Toast.makeText(this, "Play Integrity FAIL: No Play Store / GMS", Toast.LENGTH_LONG).show()
                    } else if ("Network error" in e.message.toString()) {
                        Toast.makeText(this, "Play Integrity FAIL: No Network", Toast.LENGTH_LONG).show()
                    } else if("cloud project" in e.message.toString()) {
                        Toast.makeText(this, "Play Integrity FAIL: No Cloud Project", Toast.LENGTH_LONG).show()
                    } else if ("-12" in e.message.toString()){
                        Toast.makeText(this, "Play Integrity FAIL: Google ERROR. Make sure PlayStore&GMS are up-to-date.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, e.message.toString(), Toast.LENGTH_LONG).show()
                    }
                }
        }.onFailure {
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            Log.e(TAG, "onRunPlayIntegrityButtonClicked: ${it.message}")
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

    private fun getLocationProviders(): String {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        return locationManager.allProviders.toString()
    }

}