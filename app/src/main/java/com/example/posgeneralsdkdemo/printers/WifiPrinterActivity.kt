package com.example.posgeneralsdkdemo.printers

import android.R.layout.simple_spinner_dropdown_item
import android.R.layout.simple_spinner_item
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doOnTextChanged
import com.example.posgeneralsdkdemo.R
import com.example.posgeneralsdkdemo.btprinter.CONTENT_2_INCH
import com.example.posgeneralsdkdemo.btprinter.CONTENT_3_INCH
import com.example.posgeneralsdkdemo.btprinter.CONTENT_4_INCH
import com.example.posgeneralsdkdemo.databinding.ActivityWifiPrinterBinding
import com.example.posgeneralsdkdemo.others.WifiActivity
import com.example.posgeneralsdkdemo.utils.DeviceInfoUtil
import java.net.Socket
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class WifiPrinterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWifiPrinterBinding

    private val arrayOfSize = arrayOf("2 inch", "3 inch", "4 inch")


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWifiPrinterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.apply {
            btnResetIp.setOnClickListener { etPrinterIp.setText(DeviceInfoUtil.getWifiIpv4(this@WifiPrinterActivity)) }
            btnPrintText.setOnClickListener { onPrintTextButtonClicked() }
            btnWifiSettings.setOnClickListener { onWifiSettingsButtonClicked() }
        }
        binding.etPrinterIp.doOnTextChanged { text, _, _, _ ->
            binding.btnPrintText.isEnabled = isValidIpv4(binding.etPrinterIp.text.toString())
        }
        binding.spPrintSize.adapter = ArrayAdapter(this, simple_spinner_item, arrayOfSize).apply {
            setDropDownViewResource(simple_spinner_dropdown_item)
        }
    }

    override fun onStart() {
        super.onStart()
        binding.btnPrintText.isEnabled = isValidIpv4(binding.etPrinterIp.text.toString())
        if (DeviceInfoUtil.getWifiIpv4(this) == null) {
            Toast.makeText(this, "Please connect to WiFi first", Toast.LENGTH_SHORT).show()
        } else {
            binding.etPrinterIp.setText(DeviceInfoUtil.getWifiIpv4(this))
        }
    }

    private fun onWifiSettingsButtonClicked() {
        startActivity(Intent(this, WifiActivity::class.java))
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun onPrintTextButtonClicked() {
        Thread {
            val time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            runCatching {
                val socket = Socket(binding.etPrinterIp.text.toString().trim(), 9100) // The default port for WiFi Printer
                val os = socket.outputStream
                val contentInBytes: ByteArray = when (binding.spPrintSize.selectedItem as String) { // If wants to support Chinese Characters, use "content.toByteArray(Charset.forName("GBK"))"
                    arrayOfSize[0] -> CONTENT_2_INCH.toByteArray(Charsets.UTF_8)
                    arrayOfSize[1] -> CONTENT_3_INCH.toByteArray(Charsets.UTF_8)
                    arrayOfSize[2] -> CONTENT_4_INCH.toByteArray(Charsets.UTF_8)
                    else -> ByteArray(0)
                }
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

    private fun isValidIpv4(ip: String?): Boolean {
        if (ip.isNullOrBlank()) return false
        val parts = ip.trim().split(".")
        if (parts.size != 4) return false
        return parts.all { p ->
            p.toIntOrNull()?.let { it in 0..255 } == true && (p.length == 1 || !p.startsWith("0"))
        }
    }

}