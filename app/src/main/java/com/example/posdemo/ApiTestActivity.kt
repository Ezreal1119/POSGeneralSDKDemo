package com.example.posdemo

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.bluetooth.BluetoothClass
import android.content.Intent
import android.device.DeviceManager
import android.device.IccManager
import android.device.UFSManager
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.format.DateFormat
import android.util.Base64
import android.util.Log
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import com.example.posdemo.databinding.ActivityApiTestBinding
import com.example.posdemo.others.SimCard
import com.example.posdemo.utils.ImageUtil
import com.example.posdemo.utils.PermissionUtil
import com.example.posdemo.utils.UStageCrypto
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.urovo.utils.BytesUtil
import java.util.Date
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

@Suppress("DEPRECATION")
class ApiTestActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "ApiTestActivity_TAG"
    }

    private lateinit var binding: ActivityApiTestBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityApiTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnTest1.setOnClickListener @androidx.annotation.RequiresPermission(allOf = [android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION]) { onTest1ButtonClicked() }
        binding.btnTest2.setOnClickListener { onTest2ButtonClicked() }
        binding.btnTest3.setOnClickListener { onTest3ButtonClicked() }
        binding.btnTest4.setOnClickListener { onTest4ButtonClicked() }
        binding.btnTest5.setOnClickListener { onTest5ButtonClicked() }

    }

    override fun onStart() {
        super.onStart()
        binding.apply {
            btnTest1.visibility = VISIBLE
            btnTest2.visibility = VISIBLE
            btnTest3.visibility = VISIBLE
            btnTest4.visibility = VISIBLE
            btnTest5.visibility = VISIBLE
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun onTest1ButtonClicked() {
        Log.e(TAG, "onTest1ButtonClicked")
        getLocationOnce()




    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun getLocationOnce() {
        if (!PermissionUtil.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION), 1001)) {
            Toast.makeText(this, "Please grant permission first", Toast.LENGTH_SHORT).show()
            return
        }

        val availability =
            com.google.android.gms.common.GoogleApiAvailability
                .getInstance()
                .isGooglePlayServicesAvailable(this)

        Log.e(TAG, "GMS availability = $availability")
        val lm = getSystemService(LOCATION_SERVICE) as android.location.LocationManager

        Log.e(TAG, "GPS enabled = ${lm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)}")
        Log.e(TAG, "Network enabled = ${lm.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)}")
        Log.e(TAG, "Passive enabled = ${lm.isProviderEnabled(android.location.LocationManager.PASSIVE_PROVIDER)}")


        val client = LocationServices.getFusedLocationProviderClient(this)

        val request = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 500L           // 0.5 秒
            fastestInterval = 200L    // 200 ms
            numUpdates = 10
        }

        client.requestLocationUpdates(
            request,
            object : LocationCallback() {
                override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                    Log.e(TAG, "onLocationResult: ", )
                    val loc = result.lastLocation
                    if (loc != null) {
                        Log.e(TAG, "onLocationResult: Lat=${loc.latitude}, Lng=${loc.longitude}", )
                        runOnUiThread {
                            binding.tvResult.text = "Lat=${loc.latitude}, Lng=${loc.longitude}"
                        }
                    }
                    client.removeLocationUpdates(this)
                }
            },
            mainLooper
        )
    }

    private fun onTest2ButtonClicked() {
        Log.e(TAG, "onTest2ButtonClicked")

        val list = UStageCrypto.decryptAndUnzipMulti("DBV3Naf+O0h8kCF5YzlDphil7RAbGEExegGxWWFECD525EAp+CwbaWOKUSurhQ5JDwycyTnGI8y9hhFA8oOKc+wSi1TXJTL+FlmDy1FV99GMjA1CFB5IKTd\\/AngFtsBYm\\/bK6PHhis1O5Tl\\/e1QlLiEDCqbCilC4bIpjg6DsiO4SwyzHOs9T87\\/hO8XdUxVNVhuA7gWfDTPzV1KlsBwh+u6clrYN8u2FuokXhmNilVucVoMc6Ls+KatMUr4KB2rWnvRGnBse06ezfV0qB1hMQ09a9fU+rOqFHmR+WEa7uVn4tUSSH0xDZfaOLDCrtiOcJ8QOEQAGpg1h9ntWbod83184zLdT+bJzwhC8xR5NUbEGGVuhW81rPQrVENd2xDepLvjoQ0kn5DA6b0\\/jNfiiqY2e1JesBAC38zO0M3+LXOFuYqQOspiCgNuvO10tLHXjIpXEXKX5lg6sd0CGVfRoI1ORuif2Y5Pl2sPW1PUan5Yo58Ab8ZhPlzuWneA7GKNs")
        list.forEachIndexed { i, plain ->
            Log.e("USTAGE", plain)
        }
    }

    private fun onTest3ButtonClicked() {
        Log.e(TAG, "onTest3ButtonClicked")


        fun encryptUBrowserPassword(plain: String): String {
            val keyInHex = "6836686e6269646e736d7a7776776d77"
            val iv = ByteArray(16)
            val key = SecretKeySpec(BytesUtil.hexString2Bytes(keyInHex), "AES")
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(iv))
            val ct = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
            return Base64.encodeToString(ct, Base64.NO_WRAP).replace("=", "")
        }

        fun decryptUBrowserPassword(b64NoPad: String): String {
            val keyInHex = "6836686e6269646e736d7a7776776d77"
            val iv = ByteArray(16)
            val padded = buildString {
                append(b64NoPad)
                while (length % 4 != 0) append('=')
            }

            val key = SecretKeySpec(BytesUtil.hexString2Bytes(keyInHex), "AES")
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))

            val ct = Base64.decode(padded, Base64.NO_WRAP)
            val pt = cipher.doFinal(ct)
            return String(pt, Charsets.UTF_8)
        }
        Log.e(TAG, "onTest3ButtonClicked: ${encryptUBrowserPassword("111111")}", )
    }

    private fun onTest4ButtonClicked() {
        Log.e(TAG, "onTest4ButtonClicked")

        UFSManager().setBootanimation("/sdcard/Download/bootanimation_720_1440.zip")
    }

    private fun onTest5ButtonClicked() {
        Log.e(TAG, "onTest5ButtonClicked")
        DeviceManager().setSettingProperty("persist-persist.sys.urv.all.settings.password", "")

//        runCatching {
//            val clazz = Class.forName("android.device.DeviceManager")
//            val method = clazz.getMethod("setPreferCall", Int::class.java)
//            method.invoke(clazz.newInstance(), SimCard.SIM_1.index) as Int
//        }.onSuccess {
//            Toast.makeText(this, "Switch Call to Sim 1 successfully", Toast.LENGTH_SHORT).show()
//        }.onFailure {
//            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
//            it.printStackTrace()
//        }



//        val clazz = Class.forName("android.device.UFSManager")
//        val method = clazz.getMethod("setWallpaper", Bitmap::class.java, Int::class.java)
//        method.invoke(clazz.newInstance(), ImageUtil.pngToBitmap(resources, R.drawable.wallpaper), 2)
//

    }


}