package com.example.posdemo

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.posdemo.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnEMV.setOnClickListener { startActivity(Intent(this, EmvActivity::class.java)) }
        binding.btnPinpad.setOnClickListener { startActivity(Intent(this, PinpadActivity::class.java)) }
        binding.btnCardReader.setOnClickListener { startActivity(Intent(this, CardReaderActivity::class.java)) }
        binding.btnPrinter.setOnClickListener { startActivity(Intent(this, PrinterActivity::class.java))}
        binding.btnUtilities.setOnClickListener { startActivity(Intent(this, UtilitiesActivity::class.java)) }
        binding.btnOthers.setOnClickListener { startActivity(Intent(this, OthersActivity::class.java)) }
        binding.btnApiTest.setOnClickListener { startActivity(Intent(this, ApiTestActivity::class.java)) }
    }


}