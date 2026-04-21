package com.example.posdemo

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.posdemo.databinding.ActivityMainBinding
import com.example.posdemo.services.ConfigWatcherService
import com.urovo.sdk.utils.SystemProperties.getSystemProperty

class MainActivity : AppCompatActivity() {

    companion object {
        private val listOfPos = listOf("SQ68", "SQ29M", "SQ29MB", "SQ29WR", "SQ65A", "SQ65B")
    }

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnEMV.setOnClickListener {
            if (getDevType() !in BuildConfig.LIST_OF_POS) {
                Toast.makeText(this, "PDA doesn't support this feature!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startActivity(Intent(this, EmvActivity::class.java))
        }
        binding.btnPinpad.setOnClickListener {
            if (getDevType() !in BuildConfig.LIST_OF_POS) {
                Toast.makeText(this, "PDA doesn't support this feature!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startActivity(Intent(this, PinpadActivity::class.java))
        }
        binding.btnCardReader.setOnClickListener {
            if (getDevType() !in BuildConfig.LIST_OF_POS) {
                Toast.makeText(this, "PDA doesn't support this feature!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startActivity(Intent(this, CardReaderActivity::class.java))
        }
        binding.btnPrinter.setOnClickListener {
            if (getDevType() !in BuildConfig.LIST_OF_POS) {
                Toast.makeText(this, "PDA doesn't support this feature!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startActivity(Intent(this, PrinterActivity::class.java))
        }
        binding.btnUtilities.setOnClickListener {
            if (getDevType() !in BuildConfig.LIST_OF_POS) {
                Toast.makeText(this, "PDA doesn't support this feature!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startActivity(Intent(this, UtilitiesActivity::class.java))
        }
        binding.btnOthers.setOnClickListener { startActivity(Intent(this, OthersActivity::class.java)) }
        binding.btnApiTest.setOnClickListener { startActivity(Intent(this, ApiTestActivity::class.java)) }

        binding.tvAppVersion.text = buildString {
            append("https://github.com/Ezreal1119/POSGeneralSDKDemo\n")
            append("Version: ${BuildConfig.VERSION_NAME}")
        }

    }

    private fun getDevType(): String {
        return getSystemProperty("pwv.project", "no result found!")
    }
}
