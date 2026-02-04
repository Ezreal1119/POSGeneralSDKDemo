package com.example.posgeneralsdkdemo

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.posgeneralsdkdemo.databinding.ActivityMainBinding
import kotlin.getValue

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnEMV.setOnClickListener { startActivity(Intent(this, EmvActivity::class.java)) }
        binding.btnPinpad.setOnClickListener { startActivity(Intent(this, PinpadActivity::class.java)) }
        binding.btnBeeper.setOnClickListener { startActivity(Intent(this, BeeperActivity::class.java)) }
        binding.btnLed.setOnClickListener { startActivity(Intent(this, LedActivity::class.java)) }
        binding.btnPrinter.setOnClickListener { startActivity(Intent(this, PrinterActivity::class.java))}
        binding.btnNewSerialPort.setOnClickListener { startActivity(Intent(this, NewSerialPortActivity::class.java)) }
        binding.btnCameraScan.setOnClickListener { startActivity(Intent(this, CameraScanActivity::class.java)) }
        binding.btnMagCardReader.setOnClickListener { startActivity(Intent(this, MagCardReaderActivity::class.java)) }
        binding.btnICCardReader.setOnClickListener { startActivity(Intent(this, ICCardActivity::class.java)) }
        binding.btnOthers.setOnClickListener { startActivity(Intent(this, OthersActivity::class.java)) }
        binding.btnApiTest.setOnClickListener { startActivity(Intent(this, ApiTestActivity::class.java)) }
    }


}