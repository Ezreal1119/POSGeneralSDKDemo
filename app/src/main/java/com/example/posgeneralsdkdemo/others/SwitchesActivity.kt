package com.example.posgeneralsdkdemo.others

import android.device.DeviceManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.posgeneralsdkdemo.R

class SwitchesActivity : AppCompatActivity() {

    private val btnSwitchDataSim1 by lazy { findViewById<Button>(R.id.btnSwitchDataSim1) }
    private val btnSwitchDataSim2 by lazy { findViewById<Button>(R.id.btnSwitchDataSim2) }
    private val btnSwitchCallSim1 by lazy { findViewById<Button>(R.id.btnSwitchCallSim1) }
    private val btnSwitchCallSim2 by lazy { findViewById<Button>(R.id.btnSwitchCallSim2) }
    private val btnSwitchSmsSim1 by lazy { findViewById<Button>(R.id.btnSwitchSmsSim1) }
    private val btnSwitchSmsSim2 by lazy { findViewById<Button>(R.id.btnSwitchSmsSim2) }

    private val btnTurnOnWifi by lazy { findViewById<Button>(R.id.btnTurnOnWifi) }
    private val btnTurnOffWifi by lazy { findViewById<Button>(R.id.btnTurnOffWifi) }
    private val btnEnableWifi by lazy { findViewById<Button>(R.id.btnEnableWifi) }
    private val btnDisableWifi by lazy { findViewById<Button>(R.id.btnDisableWifi) }

    private val btnTurnOnHotspot by lazy { findViewById<Button>(R.id.btnTurnOnHotspot) }
    private val btnTurnOffHotspot by lazy { findViewById<Button>(R.id.btnTurnOffHotspot) }
    private val btnEnableHotspot by lazy { findViewById<Button>(R.id.btnEnableHotspot) }
    private val btnDisableHotspot by lazy { findViewById<Button>(R.id.btnDisableHotspot) }

    private val btnTurnOnBluetooth by lazy { findViewById<Button>(R.id.btnTurnOnBluetooth) }
    private val btnTurnOffBluetooth by lazy { findViewById<Button>(R.id.btnTurnOffBluetooth) }
    private val btnEnableBluetooth by lazy { findViewById<Button>(R.id.btnEnableBluetooth) }
    private val btnDisableBluetooth by lazy { findViewById<Button>(R.id.btnDisableBluetooth) }

    private val btnTurnOnAirplane by lazy { findViewById<Button>(R.id.btnTurnOnAirplane) }
    private val btnTurnOffAirplane by lazy { findViewById<Button>(R.id.btnTurnOffAirplane) }
    private val btnEnableAirplane by lazy { findViewById<Button>(R.id.btnEnableAirplane) }
    private val btnDisableAirplane by lazy { findViewById<Button>(R.id.btnDisableAirplane) }

    private val btnTurnOnAdb by lazy { findViewById<Button>(R.id.btnTurnOnAdb) }
    private val btnTurnOffAdb by lazy { findViewById<Button>(R.id.btnTurnOffAdb) }
    private val btnEnableAdb by lazy { findViewById<Button>(R.id.btnEnableAdb) }
    private val btnDisableAdb by lazy { findViewById<Button>(R.id.btnDisableAdb) }

    private val btnTurnOnDT2W by lazy { findViewById<Button>(R.id.btnTurnOnDT2W) }
    private val btnTurnOffDT2W by lazy { findViewById<Button>(R.id.btnTurnOffDT2W) }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_switches)

        btnSwitchDataSim1.setOnClickListener { onSwitchDataSim1ButtonClicked() }
        btnSwitchDataSim2.setOnClickListener { onSwitchDataSim2ButtonClicked() }
        btnSwitchCallSim1.setOnClickListener { onSwitchCallSim1ButtonClicked() }
        btnSwitchCallSim2.setOnClickListener { onSwitchCallSim2ButtonClicked() }
        btnSwitchSmsSim1.setOnClickListener { onSwitchSmsSim1ButtonClicked() }
        btnSwitchSmsSim2.setOnClickListener { onSwitchSmsSim2ButtonClicked() }

        btnTurnOnWifi.setOnClickListener { onTurnOnWiFiButtonClicked() }
        btnTurnOffWifi.setOnClickListener { onTurnOffWifiButtonClicked() }
        btnEnableWifi.setOnClickListener { onEnableWifiButtonClicked() }
        btnDisableWifi.setOnClickListener { onDisableWifiButtonClicked() }

        btnTurnOnHotspot.setOnClickListener { onTurnOnHotspotButtonClicked() }
        btnTurnOffHotspot.setOnClickListener { onTurnOffHotspotButtonClicked() }
        btnEnableHotspot.setOnClickListener { onEnableHotspotButtonClicked() }
        btnDisableHotspot.setOnClickListener { onDisableHotspotButtonClicked() }

        btnTurnOnBluetooth.setOnClickListener { onTurnOnBluetoothButtonClicked() }
        btnTurnOffBluetooth.setOnClickListener { onTurnOffBluetoothButtonClicked() }
        btnEnableBluetooth.setOnClickListener { onEnableBluetoothButtonClicked() }
        btnDisableBluetooth.setOnClickListener { onDisableBluetoothButtonClicked() }

        btnTurnOnAirplane.setOnClickListener { onTurnOnAirplaneButtonClicked() }
        btnTurnOffAirplane.setOnClickListener { onTurnOffAirplaneButtonClicked() }
        btnEnableAirplane.setOnClickListener { onEnableAirplaneButtonClicked() }
        btnDisableAirplane.setOnClickListener { onDisableAirplaneButtonClicked() }

        btnTurnOnAdb.setOnClickListener { onTurnOnAdbButtonClicked() }
        btnTurnOffAdb.setOnClickListener { onTurnOffAdbButtonClicked() }
        btnEnableAdb.setOnClickListener { onEnableAdbButtonClicked() }
        btnDisableAdb.setOnClickListener { onDisableAdbButtonClicked() }

        btnTurnOnDT2W.setOnClickListener { onTurnOnDT2WButtonClicked() }
        btnTurnOffDT2W.setOnClickListener { onTurnOffDT2WButtonClicked() }
    }

    private fun onSwitchDataSim1ButtonClicked() {
        runCatching {
            DeviceManager().setPreferData(SimCard.SIM_1.index)
        }.onSuccess {
            Toast.makeText(this, "Switch Data to Sim 1 successfully", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }

    private fun onSwitchDataSim2ButtonClicked() {
        runCatching {
            DeviceManager().setPreferData(SimCard.SIM_2.index)
        }.onSuccess {
            Toast.makeText(this, "Switch Data to Sim 2 successfully", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }

    private fun onSwitchCallSim1ButtonClicked() {
        runCatching {
            val clazz = Class.forName("android.device.DeviceManager")
            val method = clazz.getMethod("setPreferCall", Int::class.java)
            method.invoke(clazz.newInstance(), SimCard.SIM_1.index) as Int
        }.onSuccess {
            Toast.makeText(this, "Switch Call to Sim 1 successfully", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }

    private fun onSwitchCallSim2ButtonClicked() {
        runCatching {
            val clazz = Class.forName("android.device.DeviceManager")
            val method = clazz.getMethod("setPreferCall", Int::class.java)
            method.invoke(clazz.newInstance(), SimCard.SIM_2.index) as Int
        }.onSuccess {
            Toast.makeText(this, "Switch Call to Sim 2 successfully", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }


    private fun onSwitchSmsSim1ButtonClicked() {
        runCatching {
            val clazz = Class.forName("android.device.DeviceManager")
            val method = clazz.getMethod("setPreferSms", Int::class.java)
            method.invoke(clazz.newInstance(), SimCard.SIM_1.index)
        }.onSuccess {
            Toast.makeText(this, "Switch SMS to Sim 1 successfully", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }

    private fun onSwitchSmsSim2ButtonClicked() {
        runCatching {
            val clazz = Class.forName("android.device.DeviceManager")
            val method = clazz.getMethod("setPreferSms", Int::class.java)
            method.invoke(clazz.newInstance(), SimCard.SIM_2.index)
        }.onSuccess {
            Toast.makeText(this, "Switch SMS to Sim 2 successfully", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }

    private fun onTurnOnWiFiButtonClicked() {
        runCatching {
            DeviceManager().switchWifi(true)
        }.onSuccess {
            Toast.makeText(this, "Turn on Wifi successfully", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }


    private fun onTurnOffWifiButtonClicked() {
        runCatching {
            DeviceManager().switchWifi(false)
        }.onSuccess {
            Toast.makeText(this, "Turn off Wifi successfully", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }


    private fun onEnableWifiButtonClicked() {
        runCatching {
            DeviceManager().controlWifi(true)
        }.onSuccess {
            Toast.makeText(this, "WiFi & WiFi Control have been turned on", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }

    private fun onDisableWifiButtonClicked() {
        runCatching {
            DeviceManager().controlWifi(false)
        }.onSuccess {
            Toast.makeText(this, "WiFi & WiFi Control have been turned off", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }

    private fun onTurnOnHotspotButtonClicked() {
        runCatching {
            DeviceManager().enableHotspot(true)
        }.onSuccess {
            Toast.makeText(this, "Turn on Hotspot successfully", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }


    private fun onTurnOffHotspotButtonClicked() {
        runCatching {
            DeviceManager().enableHotspot(false)
        }.onSuccess {
            Toast.makeText(this, "Turn off Hotspot successfully", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }


    private fun onEnableHotspotButtonClicked() {
        runCatching {
            DeviceManager().controlHotspot(true)
        }.onSuccess {
            Toast.makeText(this, "Hotspot Control have been turned on", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }


    private fun onDisableHotspotButtonClicked() {
        runCatching {
            DeviceManager().controlHotspot(false)
        }.onSuccess {
            Toast.makeText(this, "Hotspot Control have been turned off", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }


    private fun onTurnOnBluetoothButtonClicked() {
        runCatching {
            DeviceManager().switchBT(true)
        }.onSuccess {
            Toast.makeText(this, "Turn on Bluetooth successfully", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }


    private fun onTurnOffBluetoothButtonClicked() {
        runCatching {
            DeviceManager().switchBT(false)
        }.onSuccess {
            Toast.makeText(this, "Turn off Bluetooth successfully", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }

    private fun onEnableBluetoothButtonClicked() {
        runCatching {
            DeviceManager().controlBT(true)
        }.onSuccess {
            Toast.makeText(this, "BT Control have been turned on", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }


    private fun onDisableBluetoothButtonClicked() {
        runCatching {
            DeviceManager().controlBT(false)
        }.onSuccess {
            Toast.makeText(this, "BT & BT Control have been turned off", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }


    private fun onTurnOnAirplaneButtonClicked() {
        runCatching {
            DeviceManager().setAirplaneMode(true)
        }.onSuccess {
            Toast.makeText(this, "Turn on Airplane Mode successfully", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }

    private fun onTurnOffAirplaneButtonClicked() {
        runCatching {
            DeviceManager().setAirplaneMode(false)
        }.onSuccess {
            Toast.makeText(this, "Turn off Airplane Mode successfully", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }


    private fun onEnableAirplaneButtonClicked() {
        runCatching {
            DeviceManager().enableAirPlaneMode(true)
        }.onSuccess {
            Toast.makeText(this, "Airplane mode Control have been turned on", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }

    private fun onDisableAirplaneButtonClicked() {
        runCatching {
            DeviceManager().enableAirPlaneMode(false)
        }.onSuccess {
            Toast.makeText(this, "Airplane mode Control have been turned on", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }


    private fun onTurnOnAdbButtonClicked() {
        runCatching {
            DeviceManager().enableAdb(true)
        }.onSuccess {
            Toast.makeText(this, "Turn on ADB successfully", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }


    private fun onTurnOffAdbButtonClicked() {
        runCatching {
            DeviceManager().enableAdb(false)
        }.onSuccess {
            Toast.makeText(this, "Turn off ADB successfully", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }


    private fun onEnableAdbButtonClicked() {
        runCatching {
            DeviceManager().controlAdb(true)
        }.onSuccess {
            Toast.makeText(this, "ADB Control have been turned on", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }


    private fun onDisableAdbButtonClicked() {
        runCatching {
            DeviceManager().controlAdb(false)
        }.onSuccess {
            Toast.makeText(this, "ADB Control have been turned off", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }


    private fun onTurnOnDT2WButtonClicked() {
        runCatching {
            DeviceManager().setSettingProperty("persist-persist.sys.urv.tp.wakeup.gesture", "doubleclick")
        }.onSuccess {
            Toast.makeText(this, "Turn on DT2W successfully", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }


    private fun onTurnOffDT2WButtonClicked() {
        runCatching {
            DeviceManager().setSettingProperty("persist-persist.sys.urv.tp.wakeup.gesture", "")
        }.onSuccess {
            Toast.makeText(this, "Turn off DT2W successfully", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }

}

enum class SimCard(val index: Int) {
    SIM_1(0),
    SIM_2(1)
}