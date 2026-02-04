package com.example.posgeneralsdkdemo

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.posgeneralsdkdemo.databinding.ActivityMagCardReaderBinding
import com.urovo.sdk.magcard.MagCardReaderImpl
import com.urovo.sdk.magcard.listener.MagCardListener

private const val TIMEOUT = 30
class MagCardReaderActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMagCardReaderBinding
    private val mMagCardReaderManager = MagCardReaderImpl.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMagCardReaderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnStartSearch.setOnClickListener { onStartSearchButtonClicked() }
        binding.btnStopSearch.setOnClickListener { onStopSearchButtonClicked() }
    }

    override fun onStart() {
        super.onStart()
        binding.btnStartSearch.isEnabled = true
        binding.btnStopSearch.isEnabled = false
        Toast.makeText(this, "Timeout=${TIMEOUT}s", Toast.LENGTH_SHORT).show()
    }

    override fun onStop() {
        super.onStop()
        mMagCardReaderManager.stopSearch()
    }

    private fun onStartSearchButtonClicked() {
        runCatching {
            mMagCardReaderManager.searchCard(TIMEOUT, mMagCardListener)
        }.onSuccess {
            binding.btnStartSearch.isEnabled = false
            binding.btnStopSearch.isEnabled = true
        }.onFailure {
            binding.tvResult.text = it.message
            it.printStackTrace()
        }
    }


    private fun onStopSearchButtonClicked() {
        runCatching {
            mMagCardReaderManager.stopSearch()
        }.onSuccess {
            Toast.makeText(this, "Stop Searching successfully", Toast.LENGTH_SHORT).show()
            binding.tvResult.text = ""
            binding.btnStartSearch.isEnabled = true
            binding.btnStopSearch.isEnabled = false
        }.onFailure {
            binding.tvResult.text = it.message
            it.printStackTrace()
        }
    }

    private val mMagCardListener = object : MagCardListener {
        override fun onSuccess(track: Bundle?) {
            runOnUiThread {
                Toast.makeText(
                    this@MagCardReaderActivity,
                    "Mag Card read successfully",
                    Toast.LENGTH_SHORT
                ).show()
                binding.tvResult.text = buildString {
                    append("Track 1: ${track?.getString(MagCardTag.TRACK1.value)}\n\n")
                    append("Track 2: ${track?.getString(MagCardTag.TRACK2.value)}\n\n")
                    append("Track 3: ${track?.getString(MagCardTag.TRACK3.value)}\n\n")
                    append("PAN: ${track?.getString(MagCardTag.PAN.value)}\n\n")
                    append("Service code: ${track?.getString(MagCardTag.SERVICE_CODE.value)}\n\n")
                    append("Expiry date: ${track?.getString(MagCardTag.EXPIRED_DATE.value)}\n\n")
                }
            }
            onStartSearchButtonClicked()
        }

        override fun onError(error: Int, message: String?) {
            runOnUiThread {
                Toast.makeText(
                    this@MagCardReaderActivity,
                    "Mag Card read failed",
                    Toast.LENGTH_SHORT
                ).show()
                binding.btnStartSearch.isEnabled = true
                binding.btnStopSearch.isEnabled = false
                binding.tvResult.text = "onError: error=$error, message=$message"
            }
        }

        override fun onTimeout() {
            runOnUiThread {
                binding.btnStartSearch.isEnabled = true
                binding.btnStopSearch.isEnabled = false
                Toast.makeText(this@MagCardReaderActivity, "onTimeout", Toast.LENGTH_SHORT)
                    .show()
            }
        }

    }

}

enum class MagCardTag(val value: String) {
    PAN("PAN"),
    TRACK1("TRACK1"),
    TRACK2("TRACK2"),
    TRACK3("TRACK3"),
    SERVICE_CODE("SERVICE_CODE"),
    EXPIRED_DATE("EXPIRED_DATE")
}