package com.example.posgeneralsdkdemo

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.urovo.serial.utils.SerialPortListener
import com.urovo.serial.utils.SerialPortTool
import com.urovo.smartpos.device.core.SerialPortDriverImpl
import com.urovo.utils.BytesUtil
import kotlin.text.Charsets.UTF_8

/**
 * Please note: The version of Serial Port is always reading Data from the Serial Port buffer once opened
 * For receivedBuffer: this version won't buffer the previously received Data, only real-time data will be received
 * The Baud Rate doesn't really matter, since this is USB CDC/ACM not traditional UART.
 */
private const val SERIAL_PORT_PATH_PREFIX = "/dev/ttyGS"
private const val GENERAL_BAUD_RATE_115200 = 115200
class NewSerialPortActivity : AppCompatActivity() {

    private val tvIntro by lazy { findViewById<TextView>(R.id.tvIntro) }
    private val tvResult by lazy { findViewById<TextView>(R.id.tvResult) }
    private val etSerialPortData by lazy { findViewById<EditText>(R.id.etSerialPortData) }
    private val btnOpenSerialPort by lazy { findViewById<Button>(R.id.btnOpenSerialPort) }
    private val btnCloseSerialPort by lazy { findViewById<Button>(R.id.btnCloseSerialPort) }
    private val btnSendHexData by lazy { findViewById<Button>(R.id.btnSendHexData) }
    private val btnSendUtf8Data by lazy { findViewById<Button>(R.id.btnSendUtf8Data) }
    private val btnClearConsole by lazy { findViewById<Button>(R.id.btnClearConsole) }

    private var mSerialPortManager: SerialPortTool? = null
    private val mSerialPortListener = object: SerialPortListener {
        override fun onReceive(dataReceived: ByteArray?) {
            runOnUiThread {
                if (dataReceived == null) {
                    Toast.makeText(this@NewSerialPortActivity, "Received null data", Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
                }
                tvResult.apply {
                    append("RX: \n")
                    append(" - HEX: ${BytesUtil.bytes2HexString(dataReceived)}\n")
                    append(" - HEX_ASCII: ${BytesUtil.bcd2Ascii(dataReceived)}\n")
                    append(" - HEX_UTF8: ${String(dataReceived, UTF_8)}\n")
                }
            }
        }
        override fun onFail(errorCode: String?, errorMessage: String?) {
            runOnUiThread {
                tvResult.text = "onFail: errorCode=$errorCode\n"
            }
        }
    }

    private val serialPortList: List<String> = List(10) { i ->
        SERIAL_PORT_PATH_PREFIX + i
    }
    private val baudRateArray = arrayOf(1200, 2400, 4800, 9600, 14400, 19200, 28800, 38400, 57600, 115200)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_serial_port)

        btnOpenSerialPort.setOnClickListener { onOpenSerialPortButtonClicked() }
        btnCloseSerialPort.setOnClickListener { onCloseSerialPortButtonClicked() }
        btnSendHexData.setOnClickListener { onSendHexDataButtonClicked() }
        btnSendUtf8Data.setOnClickListener { onSendUtf8ButtonClicked() }
        btnClearConsole.setOnClickListener { onClearConsoleButtonClicked() }
    }

    override fun onStart() {
        super.onStart()
        uiRefreshOnPortClose()
    }

    override fun onStop() {
        super.onStop()
        if (mSerialPortManager != null) {
            mSerialPortManager?.close()
            mSerialPortManager = null
        }
    }

    private fun onOpenSerialPortButtonClicked() {
        runCatching {
            if (mSerialPortManager == null) {
                mSerialPortManager = SerialPortTool()
            }
            mSerialPortManager?.setOnListener(mSerialPortListener)
            // Will find the first Serial Port that canWrite & canRead and open it from the List
            // Normally, /dev/ttyGS0 is the only Serial Port canWrite & canRead
            val ret = mSerialPortManager?.openSerialPort(serialPortList, GENERAL_BAUD_RATE_115200)
            if (ret != 0) throw Exception("Serial open failed: ret=$ret\n")
        }.onSuccess {
            uiRefreshOnPortOpen()
            Toast.makeText(this, "Serial Port opened successfully", Toast.LENGTH_SHORT).show()
        }.onFailure {
            tvResult.text = it.message
            it.printStackTrace()
        }
    }

    private fun onCloseSerialPortButtonClicked() {
        runCatching {
            if (mSerialPortManager != null) {
                mSerialPortManager?.close()
                mSerialPortManager = null
            }
        }.onSuccess {
            uiRefreshOnPortClose()
            Toast.makeText(this, "Serial Port closed successfully", Toast.LENGTH_SHORT).show()
        }.onFailure {
            tvResult.text = it.message
            it.printStackTrace()
        }
    }

    private fun onSendHexDataButtonClicked() {
        val dataAsString = etSerialPortData.text.toString().trim()
        val dataAsBytes = BytesUtil.hexString2Bytes(dataAsString)
        runCatching {
            if (mSerialPortManager == null) {
                Toast.makeText(this, "Please open Serial Port first", Toast.LENGTH_SHORT).show()
                return
            }
            mSerialPortManager?.sendData(dataAsBytes, dataAsBytes.size)
        }.onSuccess {
            tvResult.append("TX(HEX): \n${BytesUtil.bytes2HexString(dataAsBytes)}\n\n")
        }.onFailure {
            tvResult.text = it.message
            it.printStackTrace()
        }
    }

    private fun onSendUtf8ButtonClicked() {
        val dataAsString = etSerialPortData.text.toString().trim()
        val dataAsBytes = dataAsString.toByteArray(UTF_8)
        runCatching {
            if (mSerialPortManager == null) {
                Toast.makeText(this, "Please open Serial Port first", Toast.LENGTH_SHORT).show()
                return
            }
            mSerialPortManager?.sendData(dataAsBytes, dataAsBytes.size)
        }.onSuccess {
            tvResult.append("TX(UTF-8): \n${BytesUtil.bytes2HexString(dataAsBytes)}\n\n")
        }.onFailure {
            tvResult.text = it.message
            it.printStackTrace()
        }
    }

    private fun onClearConsoleButtonClicked() {
        tvResult.text = ""
    }

    // <------------------UI helper methods----------------->

    private fun uiRefreshOnPortOpen() {
        btnOpenSerialPort.isEnabled = false
        btnCloseSerialPort.isEnabled = true
        btnSendHexData.isEnabled = true
        btnSendUtf8Data.isEnabled = true
        btnClearConsole.isEnabled = true
        tvResult.text = ""
        tvIntro.setTextColor(Color.GREEN)
    }

    private fun uiRefreshOnPortClose() {
        btnOpenSerialPort.isEnabled = true
        btnCloseSerialPort.isEnabled = false
        btnSendHexData.isEnabled = false
        btnSendUtf8Data.isEnabled = false
        btnClearConsole.isEnabled = false
        tvResult.text = ""
        tvIntro.setTextColor(Color.RED)
    }
}