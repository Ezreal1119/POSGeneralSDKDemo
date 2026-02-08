package com.example.posdemo

import android.os.Bundle
import android.util.Log
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.app.AppCompatActivity
import com.example.posdemo.databinding.ActivityApiTestBinding

class ApiTestActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "ApiTestActivity_TAG"
    }

    private lateinit var binding: ActivityApiTestBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityApiTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnTest1.setOnClickListener { onTest1ButtonClicked() }
        binding.btnTest2.setOnClickListener { onTest2ButtonClicked() }
        binding.btnTest3.setOnClickListener { onTest3ButtonClicked() }
        binding.btnTest4.setOnClickListener { onTest4ButtonClicked() }
        binding.btnTest5.setOnClickListener { onTest5ButtonClicked() }

    }

    override fun onStart() {
        super.onStart()
        binding.apply {
            btnTest1.visibility = VISIBLE
            btnTest2.visibility = GONE
            btnTest3.visibility = GONE
            btnTest4.visibility = GONE
            btnTest5.visibility = GONE
        }
    }

    private fun onTest1ButtonClicked() {
        Log.e(TAG, "onTest1ButtonClicked")
    }

    private fun onTest2ButtonClicked() {
        Log.e(TAG, "onTest2ButtonClicked")

    }

    private fun onTest3ButtonClicked() {
        Log.e(TAG, "onTest3ButtonClicked")

    }

    private fun onTest4ButtonClicked() {
        Log.e(TAG, "onTest4ButtonClicked")

    }

    private fun onTest5ButtonClicked() {
        Log.e(TAG, "onTest5ButtonClicked")

    }
}