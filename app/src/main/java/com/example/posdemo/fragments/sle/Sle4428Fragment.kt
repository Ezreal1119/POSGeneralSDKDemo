package com.example.posdemo.fragments.sle

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.posdemo.databinding.FragmentSle4428Binding
import com.example.posdemo.iccard.ICC_SLOT
import com.example.posdemo.iccard.INDEX_900
import com.example.posdemo.iccard.PASSWORD_ALL_Fs
import com.example.posdemo.iccard.SLE_CARD_TYPE
import com.example.posdemo.iccard.SleCardActivity
import com.example.posdemo.iccard.VOLTAGE_5V
import com.example.posdemo.utils.DataUtil
import com.urovo.sdk.utils.BytesUtil
import kotlin.collections.isEmpty

class Sle4428Fragment : Fragment() {

    private var _binding: FragmentSle4428Binding? = null
    private val binding get() = _binding!!

    private val mIccManager
        get() = (requireActivity() as SleCardActivity).mIccManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSle4428Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            btnPowerUpSle4428.setOnClickListener { onPowerUpSle4428ButtonClicked() }
            btnPowerDownSle4428.setOnClickListener { onPowerDownSle4428ButtonClicked() }
            btnResetSle4428.setOnClickListener { onResetSle4428ButtonClicked() }
            btnDeactivateSle4428.setOnClickListener { onDeactivateSle4428ButtonClicked() }
            btnVerifyPasswordFFFFFF.setOnClickListener { onVerifyPasswordFFFFFFButtonClicked() }
            btnWriteSle4428.setOnClickListener { onWriteSle4428ButtonClicked() }
            btnReadSle4428.setOnClickListener { onReadSle4428ButtonClicked() }
            etSle4428Data.setText(DataUtil.randomHex(10))
            btnPowerUpSle4428.isEnabled = true
            btnPowerDownSle4428.isEnabled = false
            btnResetSle4428.isEnabled = false
            btnDeactivateSle4428.isEnabled = false
            btnReadSle4428.isEnabled = false
            btnWriteSle4428.isEnabled = false
            btnVerifyPasswordFFFFFF.isEnabled = false
            tvResult.text = buildString {
                append("Understanding SLE4428 ICCard:\n\n")
                append(" - It's type of ICCard, but less sophisticated than EMV ICCard\n")
                append(" - It can only be written and read data\n")
                append(" - You can only Read Data before verification\n")
                append(" - You can Write Data & Change Password ONLY after verification\n")
                append(" - After 3 times of unsuccessful verifications, the Card will be disabled permanently\n")
                append(" - No have protection area, all [0,1023] are free to write data\n")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        runCatching {
            mIccManager.deactivate()
            mIccManager.close()
        }
        _binding = null
    }

    private fun onPowerUpSle4428ButtonClicked() {
        runCatching {
            val ret = mIccManager.open(ICC_SLOT.toByte(), SLE_CARD_TYPE.toByte(), VOLTAGE_5V.toByte())
            if (ret < 0) throw Exception("SLE4428 powered up failed")
        }.onSuccess {
            Toast.makeText(requireContext(), "Powered up SLE4428 successfully", Toast.LENGTH_SHORT).show()
            binding.tvResult.text = "Powered up SLE4428 successfully"
            binding.btnResetSle4428.isEnabled = true
            binding.btnPowerUpSle4428.isEnabled = false
        }.onFailure {
            binding.tvResult.text = it.message
            it.printStackTrace()
        }
    }


    private fun onPowerDownSle4428ButtonClicked() {
        runCatching {
            val ret = mIccManager.close()
            if (ret < 0) throw Exception("SLE4428 powered down failed")
        }.onSuccess {
            Toast.makeText(requireContext(), "Powered down SLE4428 successfully", Toast.LENGTH_SHORT).show()
            binding.tvResult.text = "Powered down SLE4428 successfully"
            binding.btnPowerUpSle4428.isEnabled = true
            binding.btnPowerDownSle4428.isEnabled = false
        }.onFailure {
            binding.tvResult.text = it.message
            it.printStackTrace()
        }
    }


    private fun onResetSle4428ButtonClicked() {
        val atrBuffer = ByteArray(64)
        runCatching {
            val outputLen = mIccManager.sle4428_reset(atrBuffer)
            if (outputLen <= 0) throw Exception("SLE4428 activated(Reset) failed")
            return@runCatching outputLen
        }.onSuccess { outputLen -> // outputLen
            binding.etSle4428Data.setText(DataUtil.randomHex(10))
            Toast.makeText(requireContext(), "SLE4428 activated successfully", Toast.LENGTH_SHORT).show()
            val data = atrBuffer.copyOf(outputLen)
            binding.tvResult.text = buildString {
                append("Power on successfully! ATR: \n")
                append(BytesUtil.bytes2HexString(data))
                append("\n\n")
                append("Note: if any ATR(Answer to Reset) returns, then means SLE4428 is powered on successfully.\n\n")
                append("Verify Password successfully: $PASSWORD_ALL_Fs")
            }
            binding.apply {
                btnResetSle4428.isEnabled = false
                btnDeactivateSle4428.isEnabled = true
                btnReadSle4428.isEnabled = true
                btnWriteSle4428.isEnabled = true
                btnVerifyPasswordFFFFFF.isEnabled = true

            }
        }.onFailure {
            binding.tvResult.text = it.message
            it.printStackTrace()
        }
    }

    private fun onDeactivateSle4428ButtonClicked() {
        runCatching {
            val ret = mIccManager.deactivate()
            if (ret != 0) throw Exception("SLE4428 deactivation failed")
        }.onSuccess {
            Toast.makeText(requireContext(), "SLE4428 deactivation successfully", Toast.LENGTH_SHORT).show()
            binding.apply {
                tvResult.text = "SLE4428 deactivation successfully"
                btnDeactivateSle4428.isEnabled = false
                btnPowerDownSle4428.isEnabled = true
                btnReadSle4428.isEnabled = false
                btnWriteSle4428.isEnabled = false
                btnVerifyPasswordFFFFFF.isEnabled = false
            }
        }.onFailure {
            binding.tvResult.text = it.message
            it.printStackTrace()
        }
    }


    private fun onVerifyPasswordFFFFFFButtonClicked() {
        runCatching {
            val ret = mIccManager.sle4428_password(0, BytesUtil.hexString2Bytes(PASSWORD_ALL_Fs))
            if (ret != 0x00) throw Exception("Verify Password failed: $PASSWORD_ALL_Fs")
        }.onSuccess {
            Toast.makeText(requireContext(), "Verify Password successfully", Toast.LENGTH_SHORT).show()
            binding.tvResult.text = buildString {
                append("Verify Password $PASSWORD_ALL_Fs successfully!\n\n")
                append("Now you can write data (before you can only read data)\n\n")
                append("Please note:\n")
                append(" - The default password is FFFFFF\n")
                append(" - If entered 3 wrong passwords in a row, the ICC will be disabled permanently\n")
                append(" - You can change the password using 'mIccManager.sle4428_password(1, byte[] data)'\n")
                append(" - Password will stay effective until the ICC is taken out")
            }
            binding.btnVerifyPasswordFFFFFF.isEnabled = false
        }.onFailure {
            Toast.makeText(requireContext(), "If 3 times failed, the Card will be permanently disabled!", Toast.LENGTH_SHORT).show()
            binding.tvResult.text = it.message
            it.printStackTrace()
        }
    }

    private fun onWriteSle4428ButtonClicked() {
        val data = BytesUtil.hexString2Bytes(binding.etSle4428Data.text.toString())
        runCatching {
            val ret = mIccManager.sle4428_writeMemory(INDEX_900, data, data.size)
//            if (ret != 0) throw Exception("Wrote Data to SLE4428 failed! Please verify password first! ret=$ret")
            val dataReadForVerify = mIccManager.sle4428_readMemory(INDEX_900, BytesUtil.hexString2Bytes(binding.etSle4428Data.text.toString()).size)
            if (dataReadForVerify == null || dataReadForVerify.isEmpty()) throw Exception("Read Data for verification from SLE4428 failed")
            if (BytesUtil.bytes2HexString(dataReadForVerify) != binding.etSle4428Data.text.toString()) throw Exception("Wrote Data to SLE4428 failed! Please verify password first!")
        }.onSuccess {
            binding.tvResult.text = buildString {
                append("HEX Data written successfully:\n")
                append("${binding.etSle4428Data.text}\n")
                append("from Index = 900 - [0, 1023]\n")
                append("Data size: ${data.size}")
            }
        }.onFailure {
            binding.tvResult.text = it.message
            it.printStackTrace()
        }
    }

    private fun onReadSle4428ButtonClicked() {
        val dataLen = BytesUtil.hexString2Bytes(binding.etSle4428Data.text.toString()).size
        runCatching {
            val data = mIccManager.sle4428_readMemory(INDEX_900, dataLen)
            if (data == null || data.isEmpty()) throw Exception("Read Data from SLE4428 failed")
            return@runCatching data
        }.onSuccess { data ->
            binding.tvResult.text = buildString {
                append("HEX Data read successfully:\n")
                append("${BytesUtil.bytes2HexString(data)}\n")
                append("from Index = 900 - [0, 1023]\n")
                append("Data size: ${data.size}")
            }
        }.onFailure {
            binding.tvResult.text = it.message
            it.printStackTrace()
        }
    }

}