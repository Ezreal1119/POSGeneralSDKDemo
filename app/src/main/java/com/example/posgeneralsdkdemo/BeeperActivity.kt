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
import com.example.posgeneralsdkdemo.databinding.ActivityBeeperBinding
import com.google.android.material.slider.Slider
import com.urovo.sdk.beeper.BeeperImpl

private val durationArray = arrayOf(100, 200, 500, 1000, 2000)
class BeeperActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBeeperBinding

    private val mBeeperManager = BeeperImpl.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBeeperBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnStartBeeper.setOnClickListener { onStartBeeperButtonClicked() }
        binding.btnStopBeeper.setOnClickListener { onStopBeeperButtonClicked() }
        binding.spDuration.adapter = ArrayAdapter(this, simple_spinner_item, durationArray).apply {
            setDropDownViewResource(simple_spinner_dropdown_item)
        }
    }


    private fun onStartBeeperButtonClicked() {
        runCatching {
            val count = binding.sliderBeeperCount.value.toInt()
            val duration = binding.spDuration.selectedItem as Int
            mBeeperManager.startBeep(count, duration)
            return@runCatching Pair(count, duration)
        }.onSuccess { (count, duration) ->
            binding.btnStartBeeper.isEnabled = false
            Handler(Looper.getMainLooper()).postDelayed({
                binding.btnStartBeeper.isEnabled = true
            }, (count * duration * 2).toLong())
        }.onFailure {
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }


    private fun onStopBeeperButtonClicked() {
        runCatching {
            mBeeperManager.stopBeep()
        }.onFailure {
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }
}