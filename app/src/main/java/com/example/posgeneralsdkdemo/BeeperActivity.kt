package com.example.posgeneralsdkdemo

import android.R.layout.simple_spinner_dropdown_item
import android.R.layout.simple_spinner_item
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.slider.Slider
import com.urovo.sdk.beeper.BeeperImpl

class BeeperActivity : AppCompatActivity() {

    private val mBeeperManager = BeeperImpl.getInstance()

    private val sliderBeeperCount by lazy { findViewById<Slider>(R.id.sliderBeeperCount) }
    private val spDuration by lazy { findViewById<Spinner>(R.id.spDuration) }
    private val btnStartBeeper by lazy { findViewById<Button>(R.id.btnStartBeeper) }
    private val btnStopBeeper by lazy { findViewById<Button>(R.id.btnStopBeeper) }

    private val durationArray = arrayOf(100, 200, 500, 1000, 2000)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_beeper)

        btnStartBeeper.setOnClickListener { onStartBeeperButtonClicked() }
        btnStopBeeper.setOnClickListener { onStopBeeperButtonClicked() }
        spDuration.adapter = ArrayAdapter(this, simple_spinner_item, durationArray).apply {
            setDropDownViewResource(simple_spinner_dropdown_item)
        }
    }


    private fun onStartBeeperButtonClicked() {
        try {
            val count = sliderBeeperCount.value.toInt()
            val duration = spDuration.selectedItem as Int
            mBeeperManager.startBeep(count, duration)
            btnStartBeeper.isEnabled = false
            Handler(Looper.getMainLooper()).postDelayed({
                btnStartBeeper.isEnabled = true
            }, (count * duration * 2).toLong())
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error on onStartBeeperButtonClicked()", Toast.LENGTH_SHORT).show()
        }
    }


    private fun onStopBeeperButtonClicked() {
        try {
            mBeeperManager.stopBeep()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error on onStopBeeperButtonClicked()", Toast.LENGTH_SHORT).show()
        }
    }
}