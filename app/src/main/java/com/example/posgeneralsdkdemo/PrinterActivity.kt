package com.example.posgeneralsdkdemo

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.posgeneralsdkdemo.btprinter.PERMISSIONS_BT
import com.example.posgeneralsdkdemo.btprinter.PERMISSION_REQ_BT
import com.example.posgeneralsdkdemo.btprinter.SppBluetoothPrinterActivity
import com.example.posgeneralsdkdemo.databinding.ActivityPrinterBinding
import com.example.posgeneralsdkdemo.printers.WifiPrinterActivity

import com.example.posgeneralsdkdemo.utils.ImageUtil
import com.example.posgeneralsdkdemo.utils.PermissionUtil
import com.google.android.material.slider.Slider
import com.google.zxing.BarcodeFormat
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.MqttGlobalPublishFilter
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import com.urovo.sdk.print.PrintFormat
import com.urovo.sdk.print.PrinterProviderImpl
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

// implementation("com.google.zxing:core:3.5.3")

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
        "       PLEASE VISIT AGAIN      \n"
private const val MQTT_URL = "39.101.193.145"

class PrinterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPrinterBinding
    private val mPrinterManager = PrinterProviderImpl.getInstance(this)

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val bmp = contentResolver.openInputStream(uri).use { input ->
                BitmapFactory.decodeStream(input)
            }
            mPrinterManager.addImage(
                imageFormat,
                ImageUtil.bitmapToBytes(bmp)
            )
            mPrinterManager.feedLine(1) // If you pass -1, then negative line will be fed
            mPrinterManager.startPrint()
            runOnUiThread {
                Toast.makeText(this@PrinterActivity, "Printing successfully", Toast.LENGTH_SHORT).show()
                uiRefreshOnStopPrinting()
            }
        }
    }

    private val textFormat = Bundle().apply {
        putInt(ContentFormat.FONT.value, 1)
        putBoolean(ContentFormat.FONT_BOLD.value, false)
        putInt(ContentFormat.ALIGN.value, 1)
        putInt(ContentFormat.LINE_HEIGHT.value, 0)
    }
    private val barcodeFormat = Bundle().apply {
        putInt(ContentFormat.ALIGN.value, 1)
        putInt(ContentFormat.WIDTH.value, 400)
        putInt(ContentFormat.HEIGHT.value, 100)
        putSerializable(ContentFormat.BARCODE_TYPE.value, BarcodeFormat.CODE_128)
    }
    private val qrcodeFormat = Bundle().apply {
        putInt(ContentFormat.ALIGN.value, 1)
        putInt(ContentFormat.EXPECTED_HEIGHT.value, 300)
    }
    private val imageFormat = Bundle().apply {
        putInt(ContentFormat.HEIGHT.value, 300)
        putInt(ContentFormat.WIDTH.value, 300)
        putInt(ContentFormat.OFFSET.value, 35)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPrinterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.apply {
            btnPrintText.setOnClickListener { onPrintTextButtonClicked() }
            btnPrintTextLeftRight.setOnClickListener { onPrintTextLeftRightButtonClicked() }
            btnPrintTextLeftMiddleRight.setOnClickListener { onPrintTextLeftMiddleRightButtonClicked() }
            btnPrintBarcode.setOnClickListener { onPrintBarcodeButtonClicked() }
            btnPrintQrcode.setOnClickListener { onPrintQrcodeButtonClicked() }
            btnPrintImage.setOnClickListener { onPrintImageButtonClicked() }
            btnPrintImageFromPhoto.setOnClickListener { onPrintImageFromPhotoButtonClicked() }
            btnPrintBitmapCanvas.setOnClickListener { onPrintBitmapCanvasButtonClicked() }
            btnLineFeed.setOnClickListener { onLineFeedButtonClicked() }
            btnSppBluetoothPrinter.setOnClickListener { onSppBluetoothPrinterButtonClicked() }
            btnWifiPrinter.setOnClickListener { onWifiPrinterButtonClicked() }
        }
    }

    override fun onStart() {
        super.onStart()
        mqttSubscribeAndConnect(MQTT_URL)
    }

    override fun onStop() {
        super.onStop()
        mqttDisconnect()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String?>,
        grantResults: IntArray,
        deviceId: Int
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults, deviceId)
        if (requestCode == PERMISSION_REQ_BT) {
            if (PermissionUtil.checkPermissions(this, PERMISSIONS_BT)) {
                Toast.makeText(this, "Bluetooth permission granted. Please tap again to scan.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Bluetooth permission denied.", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun onPrintTextButtonClicked() {
        // Based on PrinterManager().drawTextEx() under the hood, with formatting encapsulation.
        mPrinterManager.initPrint()
        uiRefreshOnStartPrinting()
        Thread {
            runCatching {
                if (mPrinterManager.status != 0x00) {
                    throw Exception("onPrintTextButtonClicked: Printer not ready - statusCode=${mPrinterManager.status}")
                }
                mPrinterManager.setGray(binding.sliderGray.value.toInt())
                val retOnSetSpeed = mPrinterManager.setSpeed(binding.sliderPrintSpeed.value.toInt() + 10)
                if (retOnSetSpeed != 0x00) throw Exception("Set Print Speed failed")
                mPrinterManager.addText(textFormat, CONTENT)
                val ret = mPrinterManager.startPrint()
                mPrinterManager.feedLine(1) // If you pass -1, then negative line will be fed
                if (ret != 0x00) throw Exception("startPrint(): Printing failed")
            }.onSuccess { ret ->
                runOnUiThread {
                    Toast.makeText(this@PrinterActivity, "Printing successfully", Toast.LENGTH_SHORT).show()
                    uiRefreshOnStopPrinting()
                }
            }.onFailure {
                runOnUiThread { Toast.makeText(this, "onFailure: ${mPrinterManager.status}", Toast.LENGTH_SHORT).show() }
                it.printStackTrace()
            }
            mPrinterManager.close()
        }.start()
    }

    private fun onPrintTextLeftRightButtonClicked() {
        // Based on PrinterManager().drawTextEx() under the hood, with formatting encapsulation.
        mPrinterManager.initPrint()
        uiRefreshOnStartPrinting()
        Thread {
            runCatching {
                if (mPrinterManager.status != 0x00) {
                    throw Exception("onPrintTextLeftRightButtonClicked: Printer not ready - statusCode=${mPrinterManager.status}")
                }
                mPrinterManager.setGray(binding.sliderGray.value.toInt())
                val retOnSetSpeed = mPrinterManager.setSpeed(binding.sliderPrintSpeed.value.toInt() + 10)
                if (retOnSetSpeed != 0x00) throw Exception("Set Print Speed failed")
                mPrinterManager.addTextLeft_Right(textFormat, "Patrick", "Urovo")
                val ret = mPrinterManager.startPrint()
                if (ret != 0x00) throw Exception("startPrint(): Printing failed")
            }.onSuccess {
                runOnUiThread {
                    Toast.makeText(this@PrinterActivity, "Printing successfully", Toast.LENGTH_SHORT).show()
                    uiRefreshOnStopPrinting()
                }
            }.onFailure {
                runOnUiThread { Toast.makeText(this, "onFailure: ${mPrinterManager.status}", Toast.LENGTH_SHORT).show() }
                it.printStackTrace()
            }
            mPrinterManager.close()
        }.start()
    }


    private fun onPrintTextLeftMiddleRightButtonClicked() {
        // Based on PrinterManager().drawTextEx() under the hood, with formatting encapsulation.
        mPrinterManager.initPrint()
        uiRefreshOnStartPrinting()
        Thread {
            runCatching {
                if (mPrinterManager.status != 0x00) {
                    throw Exception("onPrintTextLeftMiddleRightButtonClicked: Printer not ready - statusCode=${mPrinterManager.status}")
                }
                mPrinterManager.setGray(binding.sliderGray.value.toInt())
                val retOnSetSpeed = mPrinterManager.setSpeed(binding.sliderPrintSpeed.value.toInt() + 10)
                if (retOnSetSpeed != 0x00) throw Exception("Set Print Speed failed")
                mPrinterManager.addTextLeft_Center_Right(
                    textFormat,
                    "Patrick",
                    "works in",
                    "Urovo"
                )
                val ret = mPrinterManager.startPrint()
                if (ret != 0x00) throw Exception("startPrint(): Printing failed")
            }.onSuccess {
                runOnUiThread {
                    Toast.makeText(this@PrinterActivity, "Printing successfully", Toast.LENGTH_SHORT).show()
                    uiRefreshOnStopPrinting()
                }
            }.onFailure {
                runOnUiThread { Toast.makeText(this, "onFailure: ${mPrinterManager.status}", Toast.LENGTH_SHORT).show() }
                it.printStackTrace()
            }
            mPrinterManager.close()
        }.start()
    }


    private fun onPrintBarcodeButtonClicked() {
        // Based on Create Barcode Util and PrinterManager().drawBitmap() under the hood, with formatting encapsulation.
        mPrinterManager.initPrint()
        uiRefreshOnStartPrinting()
        Thread {
            runCatching {
                if (mPrinterManager.status != 0x00) {
                    throw Exception("onPrintBarcodeButtonClicked: Printer not ready - statusCode=${mPrinterManager.status}")
                }
                mPrinterManager.setGray(binding.sliderGray.value.toInt())
                val retOnSetSpeed = mPrinterManager.setSpeed(binding.sliderPrintSpeed.value.toInt() + 10)
                if (retOnSetSpeed != 0x00) throw Exception("Set Print Speed failed")
                mPrinterManager.addBarCode(barcodeFormat, "8618807737955Patrick")
                mPrinterManager.feedLine(1) // If you pass -1, then negative line will be fed
                val ret = mPrinterManager.startPrint()
                if (ret != 0x00) throw Exception("startPrint(): Printing failed")
            }.onSuccess {
                runOnUiThread {
                    Toast.makeText(this@PrinterActivity, "Printing successfully", Toast.LENGTH_SHORT).show()
                    uiRefreshOnStopPrinting()
                }
            }.onFailure {
                runOnUiThread { Toast.makeText(this, "onFailure: ${mPrinterManager.status}", Toast.LENGTH_SHORT).show() }
                it.printStackTrace()
            }
            mPrinterManager.close()
        }.start()
    }


    private fun onPrintQrcodeButtonClicked(content: String = "https://www.urovo.com/") {
        // Based on Create QRCode Util and PrinterManager().drawBitmap() under the hood, with formatting encapsulation.
        mPrinterManager.initPrint()
        uiRefreshOnStartPrinting()
        Thread {
            runCatching {
                if (mPrinterManager.status != 0x00) {
                    throw Exception("onPrintQrcodeButtonClicked: Printer not ready - statusCode=${mPrinterManager.status}")
                }
                mPrinterManager.setGray(binding.sliderGray.value.toInt())
                val retOnSetSpeed = mPrinterManager.setSpeed(binding.sliderPrintSpeed.value.toInt() + 10)
                if (retOnSetSpeed != 0x00) throw Exception("Set Print Speed failed")
                mPrinterManager.addQrCode(qrcodeFormat, content)
                mPrinterManager.feedLine(1) // If you pass -1, then negative line will be fed
                val ret = mPrinterManager.startPrint()
                if (ret != 0x00) throw Exception("startPrint(): Printing failed")
            }.onSuccess {
                runOnUiThread {
                    Toast.makeText(this@PrinterActivity, "Printing successfully", Toast.LENGTH_SHORT).show()
                    uiRefreshOnStopPrinting()
                }
            }.onFailure {
                runOnUiThread { Toast.makeText(this, "onFailure: ${mPrinterManager.status}", Toast.LENGTH_SHORT).show() }
                it.printStackTrace()
            }
            mPrinterManager.close()
        }.start()
    }


    private fun onPrintImageButtonClicked() {
        // Based on Converting Byte[] to Bitmap and PrinterManager().drawBitmap() under the hood, with formatting encapsulation.
        mPrinterManager.initPrint()
        uiRefreshOnStartPrinting()
        Thread {
            runCatching {
                if (mPrinterManager.status != 0x00) {
                    throw Exception("onPrintImageButtonClicked: Printer not ready - statusCode=${mPrinterManager.status}")
                }
                mPrinterManager.setGray(binding.sliderGray.value.toInt())
                val retOnSetSpeed = mPrinterManager.setSpeed(binding.sliderPrintSpeed.value.toInt() + 10)
                if (retOnSetSpeed != 0x00) throw Exception("Set Print Speed failed")
                mPrinterManager.addImage(
                    imageFormat,
                    ImageUtil.bitmapToBytes(ImageUtil.pngToBitmap(resources, R.drawable.pikachu))
                )
                mPrinterManager.feedLine(1) // If you pass -1, then negative line will be fed
                val ret = mPrinterManager.startPrint()
                if (ret != 0x00) throw Exception("startPrint(): Printing failed")
            }.onSuccess {
                runOnUiThread {
                    Toast.makeText(this@PrinterActivity, "Printing successfully", Toast.LENGTH_SHORT).show()
                    uiRefreshOnStopPrinting()
                }
            }.onFailure {
                runOnUiThread { Toast.makeText(this, "onFailure: ${mPrinterManager.status}", Toast.LENGTH_SHORT).show() }
                it.printStackTrace()
            }
            mPrinterManager.close()
        }.start()
    }


    private fun onPrintImageFromPhotoButtonClicked() {
        // Based on Converting Byte[] to Bitmap and PrinterManager().drawBitmap() under the hood, with formatting encapsulation.
        mPrinterManager.initPrint()
        uiRefreshOnStartPrinting()
        runCatching {
            if (mPrinterManager.status != 0x00) {
                throw Exception("onPrintImageFromPhotoButtonClicked: Printer not ready - statusCode=${mPrinterManager.status}")
            }
            mPrinterManager.setGray(binding.sliderGray.value.toInt())
            val retOnSetSpeed = mPrinterManager.setSpeed(binding.sliderPrintSpeed.value.toInt() + 10)
            if (retOnSetSpeed != 0x00) throw Exception("Set Print Speed failed")
            pickImageLauncher.launch(arrayOf("image/*"))
        }.onFailure {
            runOnUiThread { Toast.makeText(this, "onFailure: ${mPrinterManager.status}", Toast.LENGTH_SHORT).show() }
            it.printStackTrace()
        }
    }

    private fun onPrintBitmapCanvasButtonClicked() {
        // Based on PrinterManager().drawBitmap() under the hood, you need to format&draw the Bitmap(Text + Image) yourself. With the help of Canvas. This is more recommended if you want to draw the Bitmap by yourself
        mPrinterManager.initPrint()
        uiRefreshOnStartPrinting()
        Thread {
            runCatching {
                if (mPrinterManager.status != 0x00) {
                    throw Exception("onPrintImageButtonClicked: Printer not ready - statusCode=${mPrinterManager.status}")
                }
                mPrinterManager.setGray(binding.sliderGray.value.toInt())
                val retOnSetSpeed = mPrinterManager.setSpeed(binding.sliderPrintSpeed.value.toInt() + 10)
                if (retOnSetSpeed != 0x00) throw Exception("Set Print Speed failed")
                val textBitmap = ImageUtil.textToBitmap(
                    lines = CONTENT.replace("$", "₦").split("\n").dropLast(3), // Testing special symbol
                    textSizePx = 20, //
                    paddingPx = 0, // Offset from x = 0;
                    lineGapPx = 2
                )
                // Please note: offset only has effect only when the sizeX is less than the width of the PrintPaper
                val scaledLogoBitmap = ImageUtil.scaleBitmap(ImageUtil.pngToBitmap(resources, R.drawable.unipay), 220)
                mPrinterManager.addBitmap(scaledLogoBitmap, 80)
                val scaledTextBitmap = ImageUtil.scaleBitmap(textBitmap)
                mPrinterManager.addBitmap(scaledTextBitmap, 0)
                val qrBitmap = ImageUtil.stringToQrBitmap("I have a dream that one day I can play basketball without considering the need of eating anti-sharpie planet, but still having the same honor of joining the esteemed League for caring sloth.", 350)
                mPrinterManager.addBitmap(qrBitmap, 12)
                val scaledLastTextBitmap = ImageUtil.scaleBitmap(ImageUtil.textToBitmap(listOf("    THANK YOU FOR SHOPPING     ", "       PLEASE VISIT AGAIN      "), 20, 0, 2))
                mPrinterManager.addBitmap(scaledLastTextBitmap, 0)
                mPrinterManager.feedLine(1) // If you pass -1, then negative line will be fed
                val ret = mPrinterManager.startPrint()
                if (ret != 0x00) throw Exception("startPrint(): Printing failed")
            }.onSuccess {
                runOnUiThread {
                    Toast.makeText(this@PrinterActivity, "Printing successfully", Toast.LENGTH_SHORT).show()
                    uiRefreshOnStopPrinting()
                }
            }.onFailure {
                runOnUiThread { Toast.makeText(this, "onFailure: ${mPrinterManager.status}", Toast.LENGTH_SHORT).show() }
                it.printStackTrace()
            }
            mPrinterManager.close()
        }.start()
    }


    private fun onLineFeedButtonClicked() {
        // if you feedLine(-1), then no line will be fed at all
        mPrinterManager.initPrint()
        uiRefreshOnStartPrinting()
        Thread {
            runCatching {
                if (mPrinterManager.status != 0x00) {
                    throw Exception("onLineFeedButtonClicked: Printer not ready - statusCode=${mPrinterManager.status}")
                }
                val retOnSetSpeed = mPrinterManager.setSpeed(binding.sliderPrintSpeed.value.toInt())
                if (retOnSetSpeed != 0x00) throw Exception("Set Print Speed failed")
                mPrinterManager.feedLine(1) // If you pass -1, then negative line will be fed
                val ret = mPrinterManager.startPrint()
                if (ret != 0x00) throw Exception("startPrint(): Printing failed")
            }.onSuccess {
                runOnUiThread {
                    Toast.makeText(this@PrinterActivity, "Line Feed successfully", Toast.LENGTH_SHORT).show()
                    uiRefreshOnStopPrinting()
                }
            }.onFailure {
                runOnUiThread { Toast.makeText(this, "onFailure: ${mPrinterManager.status}", Toast.LENGTH_SHORT).show() }
                it.printStackTrace()
            }
            mPrinterManager.close()
        }.start()
    }


    private fun onSppBluetoothPrinterButtonClicked() {
        if(!PermissionUtil.requestPermissions(this, PERMISSIONS_BT, PERMISSION_REQ_BT)) {
            Toast.makeText(this, "Please grant BT Permission first", Toast.LENGTH_SHORT).show()
            return
        }
        if (!(BluetoothAdapter.getDefaultAdapter()?.isEnabled ?: false)) {
            enableBtLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)) // Use Intent(Settings.ACTION_BLUETOOTH_SETTINGS) to jump to BT settings instead
            Toast.makeText(this, "Please turn on BT first", Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(Intent(this, SppBluetoothPrinterActivity::class.java))
    }

    private val enableBtLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            Toast.makeText(this, "Bluetooth has been Enabled", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Bluetooth still NOT enabled", Toast.LENGTH_SHORT).show()
        }
    }


    private fun onWifiPrinterButtonClicked() {
        val wifiManager = getSystemService(WIFI_SERVICE) as WifiManager
        if (!wifiManager.isWifiEnabled) {
            Toast.makeText(this, "Please turn on WiFi first", Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(Intent(this, WifiPrinterActivity::class.java))
    }




    // <-----------------------MQTT print-----------------------> //

    private var mqttClient: Mqtt3AsyncClient? = null

    private fun mqttSubscribeAndConnect(host: String, port: Int = 1883, topic: String = "patrick/print/qrcode") {
        val client = MqttClient.builder()
            .useMqttVersion3() // Need to specify the version, otherwise V5 mighe be used
            .identifier("patrick_${UUID.randomUUID()}")
            .serverHost(host)
            .serverPort(port)
            .buildAsync() // Connecting in another Thread, not blocking UI Thread
        mqttClient = client

        client.connect().whenComplete { _, error -> // To connect the MQTT server (Long session)
            if (error != null) {
                runOnUiThread { Toast.makeText(this, "Connected to MQTT failed", Toast.LENGTH_SHORT).show() }
                return@whenComplete
            }
            client.subscribeWith() // To subscribe to certain Topic
                .topicFilter(topic)
                .qos(MqttQos.AT_LEAST_ONCE) // Make sure the APP gets the message
                .send()
            client.publishes(MqttGlobalPublishFilter.SUBSCRIBED) { publish -> // Register the Callback method, not publishing message. SUBSCRIBED means only callback for the topic subscribed.
                val message = publish.payload // payload is "Optional" type, it might exist or not exist
                    .map { content -> // In the case of Optional, if content exists then do the conversion. Keep empty otherwise
                        StandardCharsets.UTF_8.decode(content).toString()
                    }.orElse("") // If the Optional is still empty, then return the default value ""
                runOnUiThread {
                    onPrintQrcodeButtonClicked(message)
                }
            }
            runOnUiThread {
                Toast.makeText(this, "Connected to MQTT successfully", Toast.LENGTH_SHORT).show()
                binding.tvMqttInfo.text = buildString {
                    append("Mqtt: $host:$port - topic: $topic\n\n")
                    append("\"mosquitto_pub -h $host -p $port -t patrick/print/qrcode -m '<message>'\"")
                }
            }

        }
    }

    private fun mqttDisconnect() {
        mqttClient?.disconnect()
        mqttClient = null
        binding.tvMqttInfo.text = ""
    }

    // <--------------------UI Helper methods--------------------> //

    private fun uiRefreshOnStartPrinting() {
        binding.apply {
            btnPrintText.isEnabled = false
            btnPrintTextLeftRight.isEnabled = false
            btnPrintTextLeftMiddleRight.isEnabled = false
            btnPrintBarcode.isEnabled = false
            btnPrintQrcode.isEnabled = false
            btnPrintImage.isEnabled = false
            btnPrintImageFromPhoto.isEnabled = false
            btnPrintBitmapCanvas.isEnabled = false
            btnLineFeed.isEnabled = false
            tvPrinterTitle.visibility = View.INVISIBLE
            pbWaiting.visibility = View.VISIBLE
        }
    }

    private fun uiRefreshOnStopPrinting() {
        binding.apply {
            btnPrintText.isEnabled = true
            btnPrintTextLeftRight.isEnabled = true
            btnPrintTextLeftMiddleRight.isEnabled = true
            btnPrintBarcode.isEnabled = true
            btnPrintQrcode.isEnabled = true
            btnPrintImage.isEnabled = true
            btnPrintImageFromPhoto.isEnabled = true
            btnPrintBitmapCanvas.isEnabled = true
            btnLineFeed.isEnabled = true
            tvPrinterTitle.visibility = View.VISIBLE
            pbWaiting.visibility = View.INVISIBLE
        }
    }
}

enum class ContentFormat(val value: String) {
    FONT("font"),
    FONT_BOLD("fontBold"),
    ALIGN("align"),
    LINE_HEIGHT("lineHeight"),
    WIDTH("width"),
    HEIGHT("height"),
    BARCODE_TYPE("barcode_type"),
    EXPECTED_HEIGHT("expectedHeight"),
    OFFSET("offset")
}