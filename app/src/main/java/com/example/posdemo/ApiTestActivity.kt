package com.example.posdemo

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.bluetooth.BluetoothClass
import android.device.DeviceManager
import android.device.IccManager
import android.os.Bundle
import android.util.Log
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import com.example.posdemo.databinding.ActivityApiTestBinding
import com.example.posdemo.utils.PermissionUtil
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices

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
            btnTest2.visibility = GONE
            btnTest3.visibility = GONE
            btnTest4.visibility = GONE
            btnTest5.visibility = GONE
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
    }

    private fun onTest3ButtonClicked() {
        Log.e(TAG, "onTest3ButtonClicked")

    }

    private fun onTest4ButtonClicked() {
        Log.e(TAG, "onTest4ButtonClicked")
        Toast.makeText(this, DeviceManager().getSettingProperty("Secure-install_non_market_apps"), Toast.LENGTH_SHORT).show()
    }

    private fun onTest5ButtonClicked() {
        Log.e(TAG, "onTest5ButtonClicked")

    }


}