package com.example.posgeneralsdkdemo.btprinter

import android.Manifest
import android.R.layout.simple_list_item_1
import android.R.layout.simple_spinner_dropdown_item
import android.R.layout.simple_spinner_item
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import com.example.posgeneralsdkdemo.R
import com.example.posgeneralsdkdemo.databinding.ActivitySppBluetoothPrinterBinding
import com.example.posgeneralsdkdemo.utils.PermissionUtil
import com.urovo.utils.BytesUtil
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.getValue
import kotlin.math.log


// This is the UUID for SPP(Serial Port Profile) specifically, used to connect RFCOMM printer service
// This is the solution for using Simulate Printer
private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
private const val PERMISSION_BLUETOOTH_SCAN = Manifest.permission.BLUETOOTH_SCAN
private const val PERMISSION_BLUETOOTH_CONNECT = Manifest.permission.BLUETOOTH_CONNECT
private const val PERMISSION_ACCESS_FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION
private const val PERMISSION_ACCESS_COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION
val PERMISSIONS_BT = arrayOf(
    PERMISSION_BLUETOOTH_SCAN,
    PERMISSION_BLUETOOTH_CONNECT,
    PERMISSION_ACCESS_FINE_LOCATION,
    PERMISSION_ACCESS_COARSE_LOCATION
)
const val PERMISSION_REQ_BT = 1001

const val CONTENT_2_INCH =
            "       WALMART SUPERCENTER     \n" +
            " 1234 MAIN STREET, ANYTOWN, USA\n" +
            "       TEL: (555) 123-4567     \n" +
            "-------------------------------\n" +
            "QTY  ITEM         PRICE   TOTAL\n" +
            "-------------------------------\n" +
            " 1   MILK 1 GAL   $3.49   $3.49\n" +
            " 2   BREAD LOAF   $1.99   $3.98\n" +
            " 1   EGGS DOZEN   $2.79   $2.79\n" +
            " 3   APPLES       $0.99   $2.97\n" +
            "-------------------------------\n" +
            "SUBTOTAL                 $13.23\n" +
            "TAX (8.25%)               $1.09\n" +
            "-------------------------------\n" +
            "TOTAL                    $14.32\n" +
            "-------------------------------\n" +
            "CASH                     $20.00\n" +
            "CHANGE                    $5.68\n" +
            "-------------------------------\n" +
            "    THANK YOU FOR SHOPPING     \n" +
            "       PLEASE VISIT AGAIN      "
const val CONTENT_3_INCH =
    "              WALMART SUPERCENTER              \n" +
            "        1234 MAIN STREET, ANYTOWN, USA         \n" +
            "              TEL: (555) 123-4567              \n" +
            "------------------------------------------------\n" +
            "QTY   ITEM                     PRICE      TOTAL\n" +
            "------------------------------------------------\n" +
            " 1    MILK 1 GAL                $3.49      $3.49\n" +
            " 2    BREAD LOAF                $1.99      $3.98\n" +
            " 1    EGGS DOZEN                $2.79      $2.79\n" +
            " 3    APPLES                    $0.99      $2.97\n" +
            "------------------------------------------------\n" +
            "SUBTOTAL                                  $13.23\n" +
            "TAX (8.25%)                                $1.09\n" +
            "------------------------------------------------\n" +
            "TOTAL                                     $14.32\n" +
            "------------------------------------------------\n" +
            "CASH                                      $20.00\n" +
            "CHANGE                                     $5.68\n" +
            "------------------------------------------------\n" +
            "               THANK YOU FOR SHOPPING            \n" +
            "                  PLEASE VISIT AGAIN             "
const val CONTENT_4_INCH =
    "                        WALMART SUPERCENTER                        \n" +
            "                  1234 MAIN STREET, ANYTOWN, USA                   \n" +
            "                        TEL: (555) 123-4567                        \n" +
            "----------------------------------------------------------------\n" +
            "QTY     ITEM                                   PRICE        TOTAL\n" +
            "----------------------------------------------------------------\n" +
            " 1      MILK 1 GAL                              $3.49        $3.49\n" +
            " 2      BREAD LOAF                              $1.99        $3.98\n" +
            " 1      EGGS DOZEN                              $2.79        $2.79\n" +
            " 3      APPLES                                  $0.99        $2.97\n" +
            "----------------------------------------------------------------\n" +
            "SUBTOTAL                                                     $13.23\n" +
            "TAX (8.25%)                                                   $1.09\n" +
            "----------------------------------------------------------------\n" +
            "TOTAL                                                        $14.32\n" +
            "----------------------------------------------------------------\n" +
            "CASH                                                         $20.00\n" +
            "CHANGE                                                        $5.68\n" +
            "----------------------------------------------------------------\n" +
            "                     THANK YOU FOR SHOPPING                      \n" +
            "                        PLEASE VISIT AGAIN                        "

class SppBluetoothPrinterActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySppBluetoothPrinterBinding

    private var socket: BluetoothSocket? = null
    private val foundDevices = mutableListOf<BluetoothDevice>() // Use to
    private var scanAdapter: ArrayAdapter<String>? = null // The adapter for the List View
    // The mechanism of scanning is that the foundDevices is mapped to scanAdapter index-to-index
    private var scanDialog: AlertDialog? = null

    private val arrayOfSize = arrayOf("2 inch", "3 inch", "4 inch")

    private val btScanReceiver = object : BroadcastReceiver() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> { // Will trigger this when start scanning(discovering)
                    runOnUiThread { Toast.makeText(this@SppBluetoothPrinterActivity, "Scanning...", Toast.LENGTH_SHORT).show() }
                }

                BluetoothDevice.ACTION_FOUND -> { // Will trigger this whenever a BT device is found(discovered)
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE) ?: return
                    if (device.address == null) return // Ignore the device that doesn't have MAC address
                    if (!foundDevices.contains(device)) {
                        foundDevices.add(device) // One new Device added to the foundDevices
                        runOnUiThread {
                            scanAdapter?.add("${device.name ?: "Unknown"} - ${device.address}") // One same new Device added to the scanAdapter
                            scanAdapter?.notifyDataSetChanged() // After found, the ListView should be refreshed. Very much like Compose
                        }
                    }
                }
            }
        }

    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySppBluetoothPrinterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.apply {
            btnSelfMac.setOnClickListener { onSelfMacButtonClicked() }
            btnScanBluetooth.setOnClickListener { onScanBluetoothButtonClicked() }
            btnConnectBluetooth.setOnClickListener { onConnectBluetoothButtonClicked() }
            btnDisconnectBluetooth.setOnClickListener { onDisconnectBluetoothButtonClicked() }
            btnPrintText.setOnClickListener { onPrintTextButtonClicked() }
        }

        binding.etBluetoothMac.doOnTextChanged { text, _, _, _ ->
            binding.btnConnectBluetooth.isEnabled = isValidMacAddress(binding.etBluetoothMac.text.toString())
        }

        binding.spPrintSize.adapter = ArrayAdapter(this, simple_spinner_item, arrayOfSize).apply {
            setDropDownViewResource(simple_spinner_dropdown_item)
        }
    }

    override fun onStart() {
        super.onStart()
        if(!PermissionUtil.requestPermissions(this, PERMISSIONS_BT, PERMISSION_REQ_BT)) return
        binding.btnConnectBluetooth.isEnabled = isValidMacAddress(binding.etBluetoothMac.text.toString())
        binding.btnDisconnectBluetooth.isEnabled = false
        binding.btnPrintText.isEnabled = false
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothDevice.ACTION_FOUND)
        }
        registerReceiver(btScanReceiver, filter)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    override fun onStop() {
        super.onStop()
        unregisterReceiver(btScanReceiver)
        if(BluetoothAdapter.getDefaultAdapter().isDiscovering) BluetoothAdapter.getDefaultAdapter()?.cancelDiscovery()
        scanDialog?.dismiss()
        socket?.close() // Make sure the connect with the BT device is disconnected
        socket = null // Clean up the handle, release the memory
        isValidMacAddress(binding.etBluetoothMac.text.toString())
        binding.btnDisconnectBluetooth.isEnabled = false
        binding.btnPrintText.isEnabled = false
    }

    private fun onSelfMacButtonClicked() {
        binding.etBluetoothMac.setText("FF:FF:FF:FF:FF:FF")
        Toast.makeText(this, "Simulate Printer Selected", Toast.LENGTH_SHORT).show()
    }

    @SuppressLint("MissingPermission")
    private fun onScanBluetoothButtonClicked() {

        runCatching {
            val adapter = BluetoothAdapter.getDefaultAdapter() ?: throw Exception("No BluetoothAdapter") // Make sure Device supports BT
            foundDevices.clear()
            val displayList = mutableListOf<String>() // For display devices that have been saved in the adapter before on the BT List
            adapter.bondedDevices?.forEach { device ->
                if (device.address == null) return@forEach
                foundDevices.add(device) // After this, foundDevices contains all the saved devices
                displayList.add("⭐ ${device.name ?: "Unknown"} - ${device.address}")
            }
            scanAdapter = ArrayAdapter(this, simple_list_item_1, displayList) // After this, scanAdapter contains all the saved devices as well
            val listView = ListView(this).apply {
                this.adapter = scanAdapter
                setOnItemClickListener { _, _, position, _ ->
                    val device = foundDevices[position]
                    binding.etBluetoothMac.setText(device.address)
                    Toast.makeText(this@SppBluetoothPrinterActivity, "Device selected: ${device.address}", Toast.LENGTH_SHORT).show()
                    BluetoothAdapter.getDefaultAdapter()?.cancelDiscovery() // Terminate discovery if select any device
                    scanDialog?.dismiss() // Cancel the dialog if select any device
                }
            }
            scanDialog = AlertDialog.Builder(this)
                .setTitle("Select Bluetooth Device")
                .setView(listView)
                .setNegativeButton("Cancel") { _, _ ->
                    adapter.cancelDiscovery() // Cancel the Dialog manually -> Cancel discovery
                }
                .setOnDismissListener {
                    adapter.cancelDiscovery() // Select any device -> Cancel the Dialog -> Cancel discovery
                }
                .show()
            if (adapter.isDiscovering) adapter.cancelDiscovery()
            adapter.startDiscovery()
        }
    }


    @SuppressLint("MissingPermission")
    private fun onConnectBluetoothButtonClicked() {
        val mac = binding.etBluetoothMac.text.toString().trim()
        Thread {
            runCatching {
                val adapter = BluetoothAdapter.getDefaultAdapter() ?: throw Exception("No BluetoothAdapter") // Make sure Device supports BT
                if (adapter.isDiscovering) adapter.cancelDiscovery() // Make sure BT adapter is not searching for new BT device
                val device = adapter.getRemoteDevice(mac) // Get the device of specific BT MAC
                val s = device.createRfcommSocketToServiceRecord(SPP_UUID) // Create a socket used to connect to the SPP Service of BT device
                s.connect() // Start connecting...
                socket = s // Make sure socket is only evaluated when s has successfully connected to the BT device
            }.onSuccess {
                runOnUiThread {
                    Toast.makeText(this, "Connected to: $mac", Toast.LENGTH_SHORT).show()
                }
                runOnUiThread {
                    binding.btnConnectBluetooth.isEnabled = false
                    binding.btnDisconnectBluetooth.isEnabled = true
                    binding.btnPrintText.isEnabled = true
                }
            }.onFailure {
                runOnUiThread {
                    binding.tvResult.text = it.message
                    it.printStackTrace()
                }
                socket?.close() // Make sure the connect with the BT device is disconnected
                socket = null // Clean up the handle, release the memory
            }
        }.start()
    }


    private fun onDisconnectBluetoothButtonClicked() {
        runCatching {
            socket?.close() // Make sure the connect with the BT device is disconnected
            socket = null // Clean up the handle, release the memory
        }.onSuccess {
            Toast.makeText(this, "Disconnected successfully", Toast.LENGTH_SHORT).show()
            binding.tvResult.text = ""
            binding.btnConnectBluetooth.isEnabled = isValidMacAddress(binding.etBluetoothMac.text.toString())
            binding.btnDisconnectBluetooth.isEnabled = false
            binding.btnPrintText.isEnabled = false
        }.onFailure {
            binding.tvResult.text = it.message
            it.printStackTrace()
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun onPrintTextButtonClicked() {
        Thread {
            val time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            runCatching {
                val s = socket ?: throw Exception("Not connected yet")
                val os = s.outputStream
                val contentInBytes: ByteArray = when (binding.spPrintSize.selectedItem as String) { // If wants to support Chinese Characters, use "content.toByteArray(Charset.forName("GBK"))"
                    arrayOfSize[0] -> CONTENT_2_INCH.toByteArray(Charsets.UTF_8)
                    arrayOfSize[1] -> CONTENT_3_INCH.toByteArray(Charsets.UTF_8)
                    arrayOfSize[2] -> CONTENT_4_INCH.toByteArray(Charsets.UTF_8)
                    else -> ByteArray(0)
                }
                // Using ESC/POS 1B401B61011B45012020202020202057414C4D41525420535550455243454E54455220202020200A2031323334204D41494E205354524545542C20414E59544F574E2C205553410A2020202020202054454C3A202835353529203132332D3435363720202020200A2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D0A51545920204954454D2020202020202020205052494345202020544F54414C0A2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D0A20312020204D494C4B20312047414C20202024332E343920202024332E34390A20322020204252454144204C4F414620202024312E393920202024332E39380A20312020204547475320444F5A454E20202024322E373920202024322E37390A20332020204150504C45532020202020202024302E393920202024322E39370A2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D0A535542544F54414C20202020202020202020202020202020202431332E32330A5441582028382E3235252920202020202020202020202020202024312E30390A2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D0A544F54414C20202020202020202020202020202020202020202431342E33320A2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D0A434153482020202020202020202020202020202020202020202432302E30300A4348414E4745202020202020202020202020202020202020202024352E36380A2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D0A202020205448414E4B20594F5520464F522053484F5050494E4720202020200A20202020202020504C4541534520564953495420414741494E2020202020200A1B45001B61005369676E61747572653A205061747269636B2058750A1B6102446174653A20323032362D30322D30370A0A0A
                os.write(byteArrayOf(0x1B, 0x40)) // Initialize
                os.write(byteArrayOf(0x1B, 0x61, 0x01)) // Align Center
                os.write(byteArrayOf(0x1B, 0x45, 0x01)) // Bold On
                os.write(contentInBytes)
                os.write(byteArrayOf(0x0A)) // feed line
                os.write(byteArrayOf(0x1B, 0x45, 0x00)) // Bold off
                os.write(byteArrayOf(0x1B, 0x61, 0x00)) // Align Left
                os.write("Signature: Patrick Xu\n".toByteArray(Charsets.UTF_8))
                os.write(byteArrayOf(0x1B, 0x61, 0x02)) // Align Right
                os.write("Date: $time".toByteArray(Charsets.UTF_8))
                os.write(byteArrayOf(0x0A, 0x0A, 0x0A)) // feed line
                os.flush() // Start printing
            }.onSuccess {
                runOnUiThread {
                    Toast.makeText(this, "Print sent", Toast.LENGTH_SHORT).show()
                }
            }.onFailure {
                runOnUiThread {
                    binding.tvResult.text = it.message
                    it.printStackTrace()
                }
            }
        }.start()
    }

    private fun isValidMacAddress(mac: String): Boolean {
        return mac.trim().matches(Regex("^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$"))
    }

}

/*
Understanding ESC/POS:
 - Initialize: 0x1B, 0x40
 - feedLine: 0x0A (0x0A, 0x0a means feed line twice)
 - Align_left: 0x1B, 0x61, 0x00; Align_middle: 0x1B, 0x61, 0x01; Align_right: 0x1B, 0x61, 0x02;
 - Bold_on: 0x1B, 0x45, 0x01; Bold_off: 0x1B, 0x45, 0x00
 - Text_size_normal: 0x1D, 0x21, 0x11; Text_size_large: 0x1D, 0x21, 0x22; Text_size_extra_large: 0x1D, 0x21, 0x33
 */