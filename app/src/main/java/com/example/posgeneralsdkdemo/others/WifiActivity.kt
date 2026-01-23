package com.example.posgeneralsdkdemo.others

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.device.DeviceManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.posgeneralsdkdemo.R
import com.example.posgeneralsdkdemo.utils.PermissionUtil
import kotlin.getValue

// <uses-permission android:name="android.permission.ACCESS_WIFI_STATE"/>
// <uses-permission android:name="android.permission.CHANGE_WIFI_STATE"/>
// <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>

private const val PERMISSION_ACCESS_WIFI_STATE = Manifest.permission.ACCESS_WIFI_STATE
private const val PERMISSION_CHANGE_WIFI_STATE = Manifest.permission.CHANGE_WIFI_STATE
private const val PERMISSION_ACCESS_FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION
private const val PERMISSION_ACCESS_COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION
private val ARRAY_OF_PERMISSIONS = arrayOf(
    PERMISSION_ACCESS_WIFI_STATE,
    PERMISSION_CHANGE_WIFI_STATE,
    PERMISSION_ACCESS_FINE_LOCATION,
    PERMISSION_ACCESS_COARSE_LOCATION
)
private const val REQ_PERMISSION_WIFI = 1001

class WifiActivity : AppCompatActivity() {

    private val etSSID by lazy { findViewById<EditText>(R.id.etSSID) }
    private val etWifiPassword by lazy { findViewById<EditText>(R.id.etWifiPassword) }
    private val btnConnectWifi by lazy { findViewById<Button>(R.id.btnConnectWifi) }
    private val btnSelectWifi by lazy { findViewById<Button>(R.id.btnSelectWifi) }
    private val btnForgetAllWifi by lazy { findViewById<Button>(R.id.btnForgetAllWifi) }
    private val btnTurnOnWifi by lazy { findViewById<Button>(R.id.btnTurnOnWifi) }
    private val btnTurnOffWifi by lazy { findViewById<Button>(R.id.btnTurnOffWifi) }
    private val btnEnableWifi by lazy { findViewById<Button>(R.id.btnEnableWifi) }
    private val btnDisableWifi by lazy { findViewById<Button>(R.id.btnDisableWifi) }

    private val etSSIDForWhitelist by lazy { findViewById<EditText>(R.id.etSSIDForWhitelist) }
    private val btnAddToWhitelist by lazy { findViewById<Button>(R.id.btnAddToWhitelist) }
    private val btnRemoveFromWhitelist by lazy { findViewById<Button>(R.id.btnRemoveFromWhitelist) }

    private val tvResult by lazy { findViewById<TextView>(R.id.tvResult) }



    private lateinit var wifiManager: WifiManager
    private lateinit var connectivityManager: ConnectivityManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wifi)

        btnConnectWifi.setOnClickListener { onConnectWifiButtonClicked() }
        btnSelectWifi.setOnClickListener { onSelectWifiButtonClicked() }
        btnForgetAllWifi.setOnClickListener { onForgetAllWifiButtonClicked() }
        btnTurnOnWifi.setOnClickListener { onTurnOnWifiButtonClicked() }
        btnTurnOffWifi.setOnClickListener { onTurnOffWifiButtonClicked() }
        btnEnableWifi.setOnClickListener { onEnableWifiButtonClicked() }
        btnDisableWifi.setOnClickListener { onDisableWifiButtonClicked() }
        btnAddToWhitelist.setOnClickListener { onAddToWhitelistButtonClicked() }
        btnRemoveFromWhitelist.setOnClickListener { onRemoveFromWhitelistButtonClicked() }

        wifiManager = getSystemService(WIFI_SERVICE) as WifiManager
        connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager

    }

    override fun onStart() {
        super.onStart()
        uiRefreshOnWifiStatus()
        uiRefreshOnEvent()

        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        connectivityManager.registerNetworkCallback(request, wifiConnectCallback)
    }

    override fun onStop() {
        super.onStop()
        connectivityManager.unregisterNetworkCallback(wifiConnectCallback)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_PERMISSION_WIFI) {
            val granted = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (granted) {
                tvResult.text = buildString {
                    if ("unknown" in wifiManager.connectionInfo.ssid) {
                        append("Not connection to WiFi!\n\n")
                    } else {
                        append("SSID: ${wifiManager.connectionInfo.ssid}\n\n")
                    }
                    append("WiFi MAC: ${DeviceManager().getSettingProperty("persist.sys.device.wifimac")}\n\n")
                    if (DeviceManager().wifiWhiteList.isEmpty()) {
                        append("No WiFi Whitelist set")
                    } else {
                        append("Whitelist: ${DeviceManager().wifiWhiteList}")
                    }
                }
            } else {
                Toast.makeText(this, "Please grant permission for Wifi manually!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun onConnectWifiButtonClicked() {
        runCatching {
            DeviceManager().connectWifi(etSSID.text.toString().trim(), etWifiPassword.text.toString(), WifiType.WPA.type)
        }.onSuccess {
            Toast.makeText(this, "Connecting to ${etSSID.text.toString().trim()}", Toast.LENGTH_SHORT).show()
            btnTurnOnWifi.isEnabled = false
            btnTurnOffWifi.isEnabled = true
            uiRefreshOnEvent()
        }.onFailure {
            tvResult.text = it.message
            it.printStackTrace()
        }
    }


    private fun onSelectWifiButtonClicked() {
        runCatching {
            startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
        }.onSuccess {
            Toast.makeText(this, "Please select a WiFi to connect", Toast.LENGTH_SHORT).show()
        }.onFailure {
            tvResult.text = it.message
            it.printStackTrace()
        }
    }


    private fun onForgetAllWifiButtonClicked() {
        runCatching {
            DeviceManager().forgetAllWifi()
        }.onSuccess {
            Toast.makeText(this, "Forgot all WiFi and disconnected from any WiFi successfully", Toast.LENGTH_SHORT).show()

        }.onFailure {
            tvResult.text = it.message
            it.printStackTrace()
        }
    }



    private fun onTurnOnWifiButtonClicked() {
        runCatching {
            DeviceManager().switchWifi(true)
        }.onSuccess {
            Toast.makeText(this, "Turn on Wifi successfully", Toast.LENGTH_SHORT).show()
            btnTurnOnWifi.isEnabled = false
            btnTurnOffWifi.isEnabled = true
            uiRefreshOnEvent()
        }.onFailure {
            tvResult.text = it.message
            it.printStackTrace()
        }
    }


    private fun onTurnOffWifiButtonClicked() {
        runCatching {
            DeviceManager().switchWifi(false)
        }.onSuccess {
            Toast.makeText(this, "Turn off Wifi successfully", Toast.LENGTH_SHORT).show()
            btnTurnOnWifi.isEnabled = true
            btnTurnOffWifi.isEnabled = false
            uiRefreshOnEvent()
        }.onFailure {
            tvResult.text = it.message
            it.printStackTrace()
        }
    }

    private fun onEnableWifiButtonClicked() {
        runCatching {
            DeviceManager().controlWifi(true)
        }.onSuccess {
            Toast.makeText(this, "WiFi & WiFi Control have been turned on", Toast.LENGTH_SHORT).show()
        }.onFailure {
            tvResult.text = it.message
            it.printStackTrace()
        }
    }


    private fun onDisableWifiButtonClicked() {
        runCatching {
            DeviceManager().controlWifi(false)
        }.onSuccess {
            Toast.makeText(this, "WiFi & WiFi Control have been turned off", Toast.LENGTH_SHORT).show()
        }.onFailure {
            tvResult.text = it.message
            it.printStackTrace()
        }
    }

    private fun onAddToWhitelistButtonClicked() {
        runCatching {
            DeviceManager().insertToWifiWhiteList(etSSIDForWhitelist.text.toString())
        }.onSuccess {
            Toast.makeText(this, "Added ${etSSIDForWhitelist.text} to whitelist successfully", Toast.LENGTH_SHORT).show()
            uiRefreshOnEvent()
        }.onFailure {
            tvResult.text = it.message
            it.printStackTrace()
        }
    }

    private fun onRemoveFromWhitelistButtonClicked() {
        runCatching {
            DeviceManager().removeFromWifiWhiteList(etSSIDForWhitelist.text.toString())
        }.onSuccess {
            Toast.makeText(this, "Removed ${etSSIDForWhitelist.text} from whitelist successfully", Toast.LENGTH_SHORT).show()
            uiRefreshOnEvent()
        }.onFailure {
            tvResult.text = it.message
            it.printStackTrace()
        }
    }


    private val wifiConnectCallback = object: ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            runOnUiThread {
                uiRefreshOnEvent()
                Toast.makeText(this@WifiActivity, "Wifi connected", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            runOnUiThread {
                uiRefreshOnEvent()
            }
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            super.onCapabilitiesChanged(network, networkCapabilities)
            runOnUiThread {
                uiRefreshOnEvent()
            }
        }
    }

    // <--------------------UI helper methods--------------------> //

    private fun uiRefreshOnWifiStatus() {
        if (!PermissionUtil.requestPermissions(this, ARRAY_OF_PERMISSIONS, REQ_PERMISSION_WIFI)) {
            return
        }
        if (wifiManager.isWifiEnabled) {
            btnTurnOnWifi.isEnabled = false
            btnTurnOffWifi.isEnabled = true
        } else {
            btnTurnOnWifi.isEnabled = true
            btnTurnOffWifi.isEnabled = false
        }
    }

    private fun uiRefreshOnEvent() {
        if (!PermissionUtil.requestPermissions(this, ARRAY_OF_PERMISSIONS, REQ_PERMISSION_WIFI)) {
            return
        }
        tvResult.text = buildString {
            if ("unknown" in wifiManager.connectionInfo.ssid) {
                append("Not connection to WiFi!\n\n")
            } else {
                append("SSID: ${wifiManager.connectionInfo.ssid}\n\n")
            }
            append("WiFi MAC: ${DeviceManager().getSettingProperty("persist.sys.device.wifimac")}\n\n")
            if (DeviceManager().wifiWhiteList.isEmpty()) {
                append("No WiFi Whitelist set")
            } else {
                append("Whitelist: ${DeviceManager().wifiWhiteList}")
            }
        }

    }

}

enum class WifiType(val type: Int) {  // 0(none) 1(WEP) 2(WPA) 3(SAE)
    NONE(0), // No need password to connect to WiFi
    WEP(1), // Deprecated WiFi connection encryption
    WPA(2), // WPA/WPA2: Most commonly used currently
    SAE(3) // aka. WPA3: only new AP supports
}