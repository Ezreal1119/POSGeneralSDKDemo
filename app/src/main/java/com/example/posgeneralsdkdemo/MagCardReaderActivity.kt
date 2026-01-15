package com.example.posgeneralsdkdemo

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.urovo.sdk.magcard.MagCardReaderImpl
import com.urovo.sdk.magcard.listener.MagCardListener

private const val TIMEOUT = 30
class MagCardReaderActivity : AppCompatActivity() {

    private val tvResult by lazy { findViewById<TextView>(R.id.tvResult) }
    private val btnStartSearch by lazy { findViewById<Button>(R.id.btnStartSearch) }
    private val btnStopSearch by lazy { findViewById<Button>(R.id.btnStopSearch) }
    private val mMagCardReaderManager = MagCardReaderImpl.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mag_card_reader)

        btnStartSearch.setOnClickListener { onStartSearchButtonClicked() }
        btnStopSearch.setOnClickListener { onStopSearchButtonClicked() }
    }

    override fun onStart() {
        super.onStart()
        btnStartSearch.isEnabled = true
        btnStopSearch.isEnabled = false
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
            btnStartSearch.isEnabled = false
            btnStopSearch.isEnabled = true
        }.onFailure {
            tvResult.text = it.message
            it.printStackTrace()
        }
    }


    private fun onStopSearchButtonClicked() {
        runCatching {
            mMagCardReaderManager.stopSearch()
        }.onSuccess {
            Toast.makeText(this, "Stop Searching successfully", Toast.LENGTH_SHORT).show()
            tvResult.text = ""
            btnStartSearch.isEnabled = true
            btnStopSearch.isEnabled = false
        }.onFailure {
            tvResult.text = it.message
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
                tvResult.text = buildString {
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
                btnStartSearch.isEnabled = true
                btnStopSearch.isEnabled = false
                tvResult.text = "onError: error=$error, message=$message"
            }
        }

        override fun onTimeout() {
            runOnUiThread {
                btnStartSearch.isEnabled = true
                btnStopSearch.isEnabled = false
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