package com.example.posgeneralsdkdemo.btprinter

import android.Manifest
import android.R.layout.simple_list_item_1
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
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import com.example.posgeneralsdkdemo.R
import com.example.posgeneralsdkdemo.utils.PermissionUtil
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.getValue


// This is the UUID for SPP(Serial Port Profile) specifically, used to connect RFCOMM printer service
// This is the solution for using Simulate Printer
private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
private const val PERMISSION_BLUETOOTH_SCAN = Manifest.permission.BLUETOOTH_SCAN
private const val PERMISSION_BLUETOOTH_CONNECT = Manifest.permission.BLUETOOTH_CONNECT
private const val PERMISSION_ACCESS_FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION
private const val PERMISSION_ACCESS_COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION
private val PERMISSIONS = arrayOf(
    PERMISSION_BLUETOOTH_SCAN,
    PERMISSION_BLUETOOTH_CONNECT,
    PERMISSION_ACCESS_FINE_LOCATION,
    PERMISSION_ACCESS_COARSE_LOCATION
)
private const val PERMISSION_REQ_BT = 1001

private const val CONTENT =
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

class SppBluetoothPrinterActivity : AppCompatActivity() {

    private val tvResult by lazy { findViewById<TextView>(R.id.tvResult) }
    private val etBluetoothMac by lazy { findViewById<EditText>(R.id.etBluetoothMac) }
    private val btnSelfMac by lazy { findViewById<Button>(R.id.btnSelfMac) }
    private val btnScanBluetooth by lazy { findViewById<Button>(R.id.btnScanBluetooth) }
    private val btnConnectBluetooth by lazy { findViewById<Button>(R.id.btnConnectBluetooth) }
    private val btnDisconnectBluetooth by lazy { findViewById<Button>(R.id.btnDisconnectBluetooth) }
    private val btnPrintText by lazy { findViewById<Button>(R.id.btnPrintText) }

    private var socket: BluetoothSocket? = null
    private val foundDevices = mutableListOf<BluetoothDevice>() // Use to
    private var scanAdapter: ArrayAdapter<String>? = null // The adapter for the List View
    // The mechanism of scanning is that the foundDevices is mapped to scanAdapter index-to-index
    private var scanDialog: AlertDialog? = null

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

    private val enableBtLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            Toast.makeText(this, "Bluetooth has been Enabled", Toast.LENGTH_SHORT).show()
            onConnectBluetoothButtonClicked()
        } else {
            Toast.makeText(this, "Bluetooth still NOT enabled", Toast.LENGTH_SHORT).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spp_bluetooth_printer)

        btnSelfMac.setOnClickListener { onSelfMacButtonClicked() }
        btnScanBluetooth.setOnClickListener { onScanBluetoothButtonClicked() }
        btnConnectBluetooth.setOnClickListener { onConnectBluetoothButtonClicked() }
        btnDisconnectBluetooth.setOnClickListener { onDisconnectBluetoothButtonClicked() }
        btnPrintText.setOnClickListener { onPrintTextButtonClicked() }

        etBluetoothMac.doOnTextChanged { text, _, _, _ ->
            btnConnectBluetooth.isEnabled = isValidMacAddress(etBluetoothMac.text.toString())
        }
    }

    override fun onStart() {
        super.onStart()
        if(!PermissionUtil.requestPermissions(this, PERMISSIONS, PERMISSION_REQ_BT)) return
        btnConnectBluetooth.isEnabled = isValidMacAddress(etBluetoothMac.text.toString())
        btnDisconnectBluetooth.isEnabled = false
        btnPrintText.isEnabled = false
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
        isValidMacAddress(etBluetoothMac.text.toString())
        btnDisconnectBluetooth.isEnabled = false
        btnPrintText.isEnabled = false
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQ_BT) {
            if (PermissionUtil.checkPermissions(this, PERMISSIONS)) {
                Toast.makeText(this, "Bluetooth permission granted. Please tap again to scan.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Bluetooth permission denied.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun onSelfMacButtonClicked() {
        if(!PermissionUtil.requestPermissions(this, PERMISSIONS, PERMISSION_REQ_BT)) return
        etBluetoothMac.setText("FF:FF:FF:FF:FF:FF")
        Toast.makeText(this, "Simulate Printer Selected", Toast.LENGTH_SHORT).show()
    }

    @SuppressLint("MissingPermission")
    private fun onScanBluetoothButtonClicked() {
        if(!PermissionUtil.requestPermissions(this, PERMISSIONS, PERMISSION_REQ_BT)) return

        runCatching {
            val adapter = BluetoothAdapter.getDefaultAdapter() ?: throw Exception("No BluetoothAdapter") // Make sure Device supports BT
            if (!adapter.isEnabled) { // Make sure BT is turned on first
                enableBtLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)) // Use Intent(Settings.ACTION_BLUETOOTH_SETTINGS) to jump to BT settings instead
                return
            }
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
                    etBluetoothMac.setText(device.address)
                    Toast.makeText(this@SppBluetoothPrinterActivity, "Device selected: ${device.address                                                                                                                                                                                                                                                                                                                           }", Toast.LENGTH_SHORT).show()
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
        if(!PermissionUtil.requestPermissions(this, PERMISSIONS, PERMISSION_REQ_BT)) return
        val mac = etBluetoothMac.text.toString().trim()
        Thread {
            runCatching {
                val adapter = BluetoothAdapter.getDefaultAdapter() ?: throw Exception("No BluetoothAdapter") // Make sure Device supports BT
                if (!adapter.isEnabled) { // Make sure BT is turned on first
                    enableBtLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)) // Use Intent(Settings.ACTION_BLUETOOTH_SETTINGS) to jump to BT settings instead
                    return@Thread
                }
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
                    btnConnectBluetooth.isEnabled = false
                    btnDisconnectBluetooth.isEnabled = true
                    btnPrintText.isEnabled = true
                }
            }.onFailure {
                runOnUiThread {
                    tvResult.text = it.message
                    it.printStackTrace()
                }
                socket?.close() // Make sure the connect with the BT device is disconnected
                socket = null // Clean up the handle, release the memory
            }
        }.start()
    }


    private fun onDisconnectBluetoothButtonClicked() {
        if(!PermissionUtil.requestPermissions(this, PERMISSIONS, PERMISSION_REQ_BT)) return
        runCatching {
            socket?.close() // Make sure the connect with the BT device is disconnected
            socket = null // Clean up the handle, release the memory
        }.onSuccess {
            Toast.makeText(this, "Disconnected successfully", Toast.LENGTH_SHORT).show()
            tvResult.text = ""
            btnConnectBluetooth.isEnabled                                                                                                                                                                                                                         = isValidMacAddress(etBluetoothMac.text.toString())
            btnDisconnectBluetooth.isEnabled = false
            btnPrintText.isEnabled = false
        }.onFailure {
            tvResult.text = it.message
            it.printStackTrace()
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun onPrintTextButtonClicked() {
        if(!PermissionUtil.requestPermissions(this, PERMISSIONS, PERMISSION_REQ_BT)) return
        Thread {
            val time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            runCatching {
                val s = socket ?: throw Exception("Not connected yet")
                val os = s.outputStream
                val contentInBytes = CONTENT.toByteArray(Charsets.UTF_8) // If wants to support Chinese Characters, use "content.toByteArray(Charset.forName("GBK"))"
                // Using ESC/POS
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
                os.write(byteArrayOf(0x0A, 0x0A)) // feed line
                os.flush() // Start printing
            }.onSuccess {
                runOnUiThread {
                    Toast.makeText(this, "Print sent", Toast.LENGTH_SHORT).show()
                }
            }.onFailure {
                runOnUiThread {
                    tvResult.text = it.message
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