package com.example.posgeneralsdkdemo

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import kotlin.getValue

class MainActivity : AppCompatActivity() {

    private val btnEMV by lazy { findViewById<Button>(R.id.btnEMV) }
    private val btnBeeper by lazy { findViewById<Button>(R.id.btnBeeper) }
    private val btnLed by lazy { findViewById<Button>(R.id.btnLed) }
    private val btnPinpad by lazy { findViewById<Button>(R.id.btnPinpad) }
    private val btnPrinter by lazy { findViewById<Button>(R.id.btnPrinter) }
    private val btnNewSerialPort by lazy { findViewById<Button>(R.id.btnNewSerialPort) }
    private val btnCameraScan by lazy { findViewById<Button>(R.id.btnCameraScan) }
    private val btnMagCardReader by lazy { findViewById<Button>(R.id.btnMagCardReader) }
    private val btnICCardReader by lazy { findViewById<Button>(R.id.btnICCardReader) }
    private val btnOthers by lazy { findViewById<Button>(R.id.btnOthers) }
    private val btnApiTest by lazy { findViewById<Button>(R.id.btnApiTest) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnEMV.setOnClickListener { startActivity(Intent(this, EmvActivity::class.java)) }
        btnBeeper.setOnClickListener { startActivity(Intent(this, BeeperActivity::class.java)) }
        btnLed.setOnClickListener { startActivity(Intent(this, LedActivity::class.java)) }
        btnPinpad.setOnClickListener { startActivity(Intent(this, PinpadActivity::class.java)) }
        btnPrinter.setOnClickListener { startActivity(Intent(this, PrinterActivity::class.java))}
        btnNewSerialPort.setOnClickListener { startActivity(Intent(this, NewSerialPortActivity::class.java)) }
        btnCameraScan.setOnClickListener { startActivity(Intent(this, CameraScanActivity::class.java)) }
        btnMagCardReader.setOnClickListener { startActivity(Intent(this, MagCardReaderActivity::class.java)) }
        btnICCardReader.setOnClickListener { startActivity(Intent(this, ICCardActivity::class.java)) }
        btnOthers.setOnClickListener { startActivity(Intent(this, OthersActivity::class.java)) }
        btnApiTest.setOnClickListener { startActivity(Intent(this, ApiTestActivity::class.java)) }
    }


}