package com.example.posdemo.others

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.posdemo.databinding.ActivityNewSerialPortBinding
import com.urovo.serial.utils.SerialPortListener
import com.urovo.serial.utils.SerialPortTool
import com.urovo.utils.BytesUtil

/**
 * Please note: The version of Serial Port is always reading Data from the Serial Port buffer once opened
 * For receivedBuffer: this version won't buffer the previously received Data, only real-time data will be received
 * The Baud Rate doesn't really matter, since this is USB CDC/ACM not traditional UART.
 */

class NewSerialPortActivity : AppCompatActivity() {

    companion object {
        private const val SERIAL_PORT_PATH_PREFIX = "/dev/ttyGS"
        private const val GENERAL_BAUD_RATE_115200 = 115200
        private val baudRateArray = arrayOf(1200, 2400, 4800, 9600, 14400, 19200, 28800, 38400, 57600, 115200)
    }

    private lateinit var binding: ActivityNewSerialPortBinding
    private var mSerialPortManager: SerialPortTool? = null
    private val mSerialPortListener = object: SerialPortListener {
        override fun onReceive(dataReceived: ByteArray?) {
            runOnUiThread {
                if (dataReceived == null) {
                    Toast.makeText(this@NewSerialPortActivity, "Received null data", Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
                }
                binding.tvResult.apply {
                    append("RX: \n")
                    append(" - HEX: ${BytesUtil.bytes2HexString(dataReceived)}\n")
                    append(" - HEX_ASCII: ${BytesUtil.bcd2Ascii(dataReceived)}\n")
                    append(" - HEX_UTF8: ${String(dataReceived, Charsets.UTF_8)}\n")
                }
            }
        }
        override fun onFail(errorCode: String?, errorMessage: String?) {
            runOnUiThread {
                binding.tvResult.text = "onFail: errorCode=$errorCode\n"
            }
        }
    }

    private val serialPortList: List<String> = List(10) { i ->
        SERIAL_PORT_PATH_PREFIX + i
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewSerialPortBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnOpenSerialPort.setOnClickListener { onOpenSerialPortButtonClicked() }
        binding.btnCloseSerialPort.setOnClickListener { onCloseSerialPortButtonClicked() }
        binding.btnSendHexData.setOnClickListener { onSendHexDataButtonClicked() }
        binding.btnSendUtf8Data.setOnClickListener { onSendUtf8ButtonClicked() }
        binding.btnClearConsole.setOnClickListener { onClearConsoleButtonClicked() }
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
            binding.tvResult.text = it.message
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
            binding.tvResult.text = it.message
            it.printStackTrace()
        }
    }

    private fun onSendHexDataButtonClicked() {
        val dataAsString = binding.etSerialPortData.text.toString().trim()
        val dataAsBytes = BytesUtil.hexString2Bytes(dataAsString)
        runCatching {
            if (mSerialPortManager == null) {
                Toast.makeText(this, "Please open Serial Port first", Toast.LENGTH_SHORT).show()
                return
            }
            mSerialPortManager?.sendData(dataAsBytes, dataAsBytes.size)
        }.onSuccess {
            binding.tvResult.append("TX(HEX): \n${BytesUtil.bytes2HexString(dataAsBytes)}\n\n")
        }.onFailure {
            binding.tvResult.text = it.message
            it.printStackTrace()
        }
    }

    private fun onSendUtf8ButtonClicked() {
        val dataAsString = binding.etSerialPortData.text.toString().trim()
        val dataAsBytes = dataAsString.toByteArray(Charsets.UTF_8)
        runCatching {
            if (mSerialPortManager == null) {
                Toast.makeText(this, "Please open Serial Port first", Toast.LENGTH_SHORT).show()
                return
            }
            mSerialPortManager?.sendData(dataAsBytes, dataAsBytes.size)
        }.onSuccess {
            binding.tvResult.append("TX(UTF-8): \n${BytesUtil.bytes2HexString(dataAsBytes)}\n\n")
        }.onFailure {
            binding.tvResult.text = it.message
            it.printStackTrace()
        }
    }

    private fun onClearConsoleButtonClicked() {
        binding.tvResult.text = ""
    }

    // <------------------UI helper methods----------------->

    private fun uiRefreshOnPortOpen() {
        binding.btnOpenSerialPort.isEnabled = false
        binding.btnCloseSerialPort.isEnabled = true
        binding.btnSendHexData.isEnabled = true
        binding.btnSendUtf8Data.isEnabled = true
        binding.btnClearConsole.isEnabled = true
        binding.tvResult.text = ""
        binding.tvIntro.setTextColor(Color.GREEN)
    }

    private fun uiRefreshOnPortClose() {
        binding.btnOpenSerialPort.isEnabled = true
        binding.btnCloseSerialPort.isEnabled = false
        binding.btnSendHexData.isEnabled = false
        binding.btnSendUtf8Data.isEnabled = false
        binding.btnClearConsole.isEnabled = false
        binding.tvResult.text = ""
        binding.tvIntro.setTextColor(Color.RED)
    }
}