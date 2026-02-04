package com.example.posgeneralsdkdemo.iccard

import android.device.IccManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.posgeneralsdkdemo.R
import com.example.posgeneralsdkdemo.databinding.ActivitySle4442Binding
import com.urovo.sdk.utils.BytesUtil


private const val ICC_SLOT = 0x00
private const val SLE4428_CARD_TYPE = 0x02
private const val VOLTAGE_3V = 0x01
private const val INDEX_0 = 0
private const val INDEX_100 = 100

// [0, 31] is Protected; [32 - 255] is Non-Protected
// Must verifyPassword before Writing and Changing password
class Sle4442Activity : AppCompatActivity() {

    private lateinit var binding: ActivitySle4442Binding

    private val mIccManager = IccManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySle4442Binding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.apply {
            btnPowerUpSle4442.setOnClickListener { onPowerUpSle4442ButtonClicked() }
            btnPowerDownSle4442.setOnClickListener { onPowerDownSle4442ButtonClicked() }
            btnResetSle4442.setOnClickListener { onResetSle4442ButtonClicked() }
            btnDeactivateSle4442.setOnClickListener { onDeactivateSle4442ButtonClicked() }
            btnWriteSle4442.setOnClickListener { onWriteSle4442ButtonClicked() }
            btnReadSle4442.setOnClickListener { onReadSle4442ButtonClicked() }
            btnWriteSle4442Protected.setOnClickListener { onWriteSle4442ProtectedButtonClicked() }
            btnReadSle4442Protected.setOnClickListener { onReadSle4442ProtectedButtonClicked() }
        }
    }

    override fun onStart() {
        super.onStart()
        binding.apply {
            btnPowerUpSle4442.isEnabled = true
            btnPowerDownSle4442.isEnabled = false
            btnResetSle4442.isEnabled = false
            btnDeactivateSle4442.isEnabled = false
            btnReadSle4442.isEnabled = false
            btnWriteSle4442.isEnabled = false
            btnReadSle4442Protected.isEnabled = false
            btnWriteSle4442Protected.isEnabled = false
        }
    }

    override fun onStop() {
        super.onStop()
        runCatching {
            mIccManager.close()
        }
    }

    private fun onPowerUpSle4442ButtonClicked() {
        runCatching {
            val ret = mIccManager.open(ICC_SLOT.toByte(), SLE4428_CARD_TYPE.toByte(), VOLTAGE_3V.toByte())
            if (ret < 0) throw Exception("SLE4442 powered up failed")
        }.onSuccess {
            Toast.makeText(this, "Powered up SLE4442 successfully", Toast.LENGTH_SHORT).show()
            binding.tvResult.text = "Powered up SLE4442 successfully"
            binding.btnResetSle4442.isEnabled = true
            binding.btnPowerUpSle4442.isEnabled = false
        }.onFailure {
            binding.tvResult.text = it.message
            it.printStackTrace()
        }
    }


    private fun onPowerDownSle4442ButtonClicked() {
        runCatching {
            val ret = mIccManager.close()
            if (ret < 0) throw Exception("SLE4442 powered down failed")
        }.onSuccess {
            Toast.makeText(this, "Powered down SLE4442 successfully", Toast.LENGTH_SHORT).show()
            binding.tvResult.text = "Powered down SLE4442 successfully"
            binding.btnPowerUpSle4442.isEnabled = true
            binding.btnPowerDownSle4442.isEnabled = false
        }.onFailure {
            binding.tvResult.text = it.message
            it.printStackTrace()
        }
    }


    private fun onResetSle4442ButtonClicked() {
        val atrBuffer = ByteArray(64)
        runCatching {
            val outputLen = mIccManager.sle4442_reset(atrBuffer)
            if (outputLen <= 0) throw Exception("SLE4442 activated(Reset) failed")
            val ret = mIccManager.sle4442_verifyPassword(BytesUtil.hexString2Bytes("FFFFFF"))
            if (ret != 0x00) throw Exception("Verify Password failed: FFFFFF")
            return@runCatching outputLen
        }.onSuccess { outputLen -> // outputLen
            val data = atrBuffer.copyOf(outputLen)
            binding.tvResult.text = buildString {
                append("Power on successfully! ATR: \n")
                append(BytesUtil.bytes2HexString(data))
                append("\n\n")
                append("Note: if any ATR(Answer to Reset) returns, then means SLE4442 is powered on successfully.\n\n")
                append("Verify Password successfully: FFFFFF")
            }
            binding.apply {
                btnResetSle4442.isEnabled = false
                btnDeactivateSle4442.isEnabled = true
                btnReadSle4442.isEnabled = true
                btnWriteSle4442.isEnabled = true
                btnReadSle4442Protected.isEnabled = true
                btnWriteSle4442Protected.isEnabled = true
            }
        }.onFailure {
            binding.tvResult.text = it.message
            it.printStackTrace()
        }
    }

    private fun onDeactivateSle4442ButtonClicked() {
        runCatching {
            val ret = mIccManager.deactivate()
            if (ret != 0) throw Exception("SLE4442 deactivation failed")
        }.onSuccess {
            Toast.makeText(this, "SLE4442 deactivation successfully", Toast.LENGTH_SHORT).show()
            binding.apply {
                tvResult.text = "SLE4442 deactivation successfully"
                btnDeactivateSle4442.isEnabled = false
                btnPowerDownSle4442.isEnabled = true
                btnReadSle4442.isEnabled = false
                btnWriteSle4442.isEnabled = false
            }
        }.onFailure {
            binding.tvResult.text = it.message
            it.printStackTrace()
        }
    }


    private fun onWriteSle4442ButtonClicked() {
        val data = BytesUtil.hexString2Bytes(binding.etSle4442Data.text.toString())
        runCatching {
            val ret = mIccManager.sle4442_writeMainMemory(INDEX_100, data, data.size)
            if (ret != 0) throw Exception("Wrote Data to SLE4442 failed")
        }.onSuccess {
            binding.tvResult.text = buildString {
                append("HEX Data written successfully:\n")
                append("${binding.etSle4442Data.text}\n")
                append("from Index = 100 - [32,255]\n")
                append("Data size: ${data.size}")
            }
        }.onFailure {
            binding.tvResult.text = it.message
            it.printStackTrace()
        }
    }

    private fun onReadSle4442ButtonClicked() {
        val dataLen = BytesUtil.hexString2Bytes(binding.etSle4442Data.text.toString()).size
        runCatching {
            val data = mIccManager.sle4442_readMainMemory(INDEX_100, dataLen)
            if (data == null || data.isEmpty()) throw Exception("Read Data from SLE4442 failed")
            return@runCatching data
        }.onSuccess { data ->
            binding.tvResult.text = buildString {
                append("HEX Data read successfully:\n")
                append("${BytesUtil.bytes2HexString(data)}\n")
                append("from Index = 100 - [32,255]\n")
                append("Data size: ${data.size}")
            }
        }.onFailure {
            binding.tvResult.text = it.message
            it.printStackTrace()
        }
    }

    private fun onWriteSle4442ProtectedButtonClicked() {
        val data = BytesUtil.hexString2Bytes(binding.etSle4442Data.text.toString())
        runCatching {
            val ret = mIccManager.sle4442_writeProtectionMemory(INDEX_0, data, 4)
            if (ret != 0) throw Exception("Wrote Data to SLE4442 failed")
        }.onSuccess {
            binding.tvResult.text = buildString {
                append("HEX Data written successfully:\n")
                append("${binding.etSle4442Data.text}\n")
                append("from Index = 0 - [0, 31]\n")
                append("Data size: ${data.size}")
            }
        }.onFailure {
            binding.tvResult.text = it.message
            it.printStackTrace()
        }
    }


    private fun onReadSle4442ProtectedButtonClicked() {
        runCatching {
            val data = mIccManager.sle4442_readProtectionMemory(INDEX_0, 4)
            if (data == null || data.isEmpty()) throw Exception("Read Data from SLE4442 failed")
            return@runCatching data
        }.onSuccess { data ->
            binding.tvResult.text = buildString {
                append("HEX Data read successfully:\n")
                append("${BytesUtil.bytes2HexString(data)}\n")
                append("from Index = 0 - [0, 31]\n")
                append("Data size: 4 Bytes (You can only read data from protected area by the unit of 4 Bytes")
            }
        }.onFailure {
            binding.tvResult.text = it.message
            it.printStackTrace()
        }
    }



}