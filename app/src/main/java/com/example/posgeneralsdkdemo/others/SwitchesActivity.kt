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
import com.example.posgeneralsdkdemo.databinding.ActivitySwitchesBinding

class SwitchesActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySwitchesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySwitchesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSwitchDataSim1.setOnClickListener { onSwitchDataSim1ButtonClicked() }
        binding.btnSwitchDataSim2.setOnClickListener { onSwitchDataSim2ButtonClicked() }
        binding.btnSwitchCallSim1.setOnClickListener { onSwitchCallSim1ButtonClicked() }
        binding.btnSwitchCallSim2.setOnClickListener { onSwitchCallSim2ButtonClicked() }
        binding.btnSwitchSmsSim1.setOnClickListener { onSwitchSmsSim1ButtonClicked() }
        binding.btnSwitchSmsSim2.setOnClickListener { onSwitchSmsSim2ButtonClicked() }

        binding.btnTurnOnWifi.setOnClickListener { onTurnOnWiFiButtonClicked() }
        binding.btnTurnOffWifi.setOnClickListener { onTurnOffWifiButtonClicked() }
        binding.btnEnableWifi.setOnClickListener { onEnableWifiButtonClicked() }
        binding.btnDisableWifi.setOnClickListener { onDisableWifiButtonClicked() }

        binding.btnTurnOnMobileData.setOnClickListener { onTurnOnMobileDataButtonClicked() }
        binding.btnTurnOffMobileData.setOnClickListener { onTurnOffMobileDataButtonClicked() }
        binding.btnEnableMobileData.setOnClickListener { onEnableMobileDataButtonClicked() }
        binding.btnDisableMobileData.setOnClickListener { onDisableMobileDataButtonClicked() }

        binding.btnTurnOnHotspot.setOnClickListener { onTurnOnHotspotButtonClicked() }
        binding.btnTurnOffHotspot.setOnClickListener { onTurnOffHotspotButtonClicked() }
        binding.btnEnableHotspot.setOnClickListener { onEnableHotspotButtonClicked() }
        binding.btnDisableHotspot.setOnClickListener { onDisableHotspotButtonClicked() }

        binding.btnTurnOnBluetooth.setOnClickListener { onTurnOnBluetoothButtonClicked() }
        binding.btnTurnOffBluetooth.setOnClickListener { onTurnOffBluetoothButtonClicked() }
        binding.btnEnableBluetooth.setOnClickListener { onEnableBluetoothButtonClicked() }
        binding.btnDisableBluetooth.setOnClickListener { onDisableBluetoothButtonClicked() }

        binding.btnTurnOnAirplane.setOnClickListener { onTurnOnAirplaneButtonClicked() }
        binding.btnTurnOffAirplane.setOnClickListener { onTurnOffAirplaneButtonClicked() }
        binding.btnEnableAirplane.setOnClickListener { onEnableAirplaneButtonClicked() }
        binding.btnDisableAirplane.setOnClickListener { onDisableAirplaneButtonClicked() }

        binding.btnTurnOnGps.setOnClickListener { onTurnOnGpsButtonClicked() }
        binding.btnTurnOffGps.setOnClickListener { onTurnOffGpsButtonClicked() }
        binding.btnEnableGps.setOnClickListener { onEnableGpsButtonClicked() }
        binding.btnDisableGps.setOnClickListener { onDisableGpsButtonClicked() }

        binding.btnTurnOnAdb.setOnClickListener { onTurnOnAdbButtonClicked() }
        binding.btnTurnOffAdb.setOnClickListener { onTurnOffAdbButtonClicked() }
        binding.btnEnableAdb.setOnClickListener { onEnableAdbButtonClicked() }
        binding.btnDisableAdb.setOnClickListener { onDisableAdbButtonClicked() }

        binding.btnTurnOnDT2W.setOnClickListener { onTurnOnDT2WButtonClicked() }
        binding.btnTurnOffDT2W.setOnClickListener { onTurnOffDT2WButtonClicked() }

        binding.btnTurnOnSoftKeyboard.setOnClickListener { onTurnOnSoftKeyboardButtonClicked() }
        binding.btnTurnOffSoftKeyboard.setOnClickListener { onTurnOffSoftKeyboardButtonClicked() }
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


    private fun onTurnOnMobileDataButtonClicked() {
        runCatching {
            DeviceManager().enableMobileDate(true)
        }.onSuccess {
            Toast.makeText(this, "Turn on Mobile Data successfully", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }

    private fun onTurnOffMobileDataButtonClicked() {
        runCatching {
            DeviceManager().enableMobileDate(false)
        }.onSuccess {
            Toast.makeText(this, "Turn off Mobile Data successfully", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }

    private fun onEnableMobileDataButtonClicked() {
        runCatching {
            DeviceManager().controlMobileDate(true)
        }.onSuccess {
            Toast.makeText(this, "Enable & Turn on Mobile Data successfully", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }

    private fun onDisableMobileDataButtonClicked() {
        runCatching {
            DeviceManager().controlMobileDate(false)
        }.onSuccess {
            Toast.makeText(this, "Disable & Turn off Mobile Data successfully", Toast.LENGTH_SHORT).show()
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



    private fun onTurnOnGpsButtonClicked() {
        runCatching {
            DeviceManager().enableGPS(true)
        }.onSuccess {
            Toast.makeText(this, "Turn on GPS successfully", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }

    private fun onTurnOffGpsButtonClicked() {
        runCatching {
            DeviceManager().enableGPS(false)
        }.onSuccess {
            Toast.makeText(this, "Turn off GPS successfully", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }

    private fun onEnableGpsButtonClicked() {
        runCatching {
            DeviceManager().controlGPS(true)
        }.onSuccess {
            Toast.makeText(this, "Enable & Turn on GPS successfully", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }

    private fun onDisableGpsButtonClicked() {
        runCatching {
            DeviceManager().controlGPS(false)
        }.onSuccess {
            Toast.makeText(this, "Disable & Turn off GPS successfully", Toast.LENGTH_SHORT).show()
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



    private fun onTurnOnSoftKeyboardButtonClicked() {
        runCatching {
            DeviceManager().autoPopInputMethod = true
        }.onSuccess {
            Toast.makeText(this, "Turn on Soft Keyboard successfully", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }


    private fun onTurnOffSoftKeyboardButtonClicked() {
        runCatching {
            DeviceManager().autoPopInputMethod = false
        }.onSuccess {
            Toast.makeText(this, "Turn off Soft Keyboard successfully", Toast.LENGTH_SHORT).show()
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