package com.example.posgeneralsdkdemo.fragments.sle

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.posgeneralsdkdemo.databinding.FragmentSle4442Binding
import com.example.posgeneralsdkdemo.iccard.ICC_SLOT
import com.example.posgeneralsdkdemo.iccard.INDEX_100
import com.example.posgeneralsdkdemo.iccard.INDEX_8
import com.example.posgeneralsdkdemo.iccard.PASSWORD_ALL_Fs
import com.example.posgeneralsdkdemo.iccard.SLE_CARD_TYPE
import com.example.posgeneralsdkdemo.iccard.SleCardActivity
import com.example.posgeneralsdkdemo.iccard.VOLTAGE_5V
import com.example.posgeneralsdkdemo.utils.DataUtil
import com.urovo.sdk.utils.BytesUtil
import kotlin.collections.isEmpty

class Sle4442Fragment : Fragment() {

    private var _binding: FragmentSle4442Binding? = null
    private val binding get() = _binding!!

    private val mIccManager
        get() = (requireActivity() as SleCardActivity).mIccManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSle4442Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            btnPowerUpSle4442.setOnClickListener { onPowerUpSle4442ButtonClicked() }
            btnPowerDownSle4442.setOnClickListener { onPowerDownSle4442ButtonClicked() }
            btnResetSle4442.setOnClickListener { onResetSle4442ButtonClicked() }
            btnDeactivateSle4442.setOnClickListener { onDeactivateSle4442ButtonClicked() }
            btnVerifyPasswordFFFFFF.setOnClickListener { onVerifyPasswordFFFFFFButtonClicked() }
            btnWriteSle4442.setOnClickListener { onWriteSle4442ButtonClicked() }
            btnReadSle4442.setOnClickListener { onReadSle4442ButtonClicked() }
            btnWriteSle4442Protected.setOnClickListener { onWriteSle4442ProtectedButtonClicked() }
            btnReadSle4442Protected.setOnClickListener { onReadSle4442ProtectedButtonClicked() }
            etSle4442Data.setText(DataUtil.randomHex(10))
            btnPowerUpSle4442.isEnabled = true
            btnPowerDownSle4442.isEnabled = false
            btnResetSle4442.isEnabled = false
            btnDeactivateSle4442.isEnabled = false
            btnReadSle4442.isEnabled = false
            btnWriteSle4442.isEnabled = false
            btnVerifyPasswordFFFFFF.isEnabled = false
            btnReadSle4442Protected.isEnabled = false
            btnWriteSle4442Protected.isEnabled = false
            tvResult.text = buildString {
                append("Understanding SLE4442 ICCard:\n\n")
                append(" - It's type of ICCard, but less sophisticated than EMV ICCard\n")
                append(" - It can only be written and read data\n")
                append(" - You can only Read Data(both non-protected & protected areas) before verification\n")
                append(" - You can Write Data & Change Password ONLY after verification\n")
                append(" - After 3 times of unsuccessful verifications, the Card will be disabled permanently\n")
                append(" - [0, 31] is protected area, can only be written once\n")
                append(" - [32, 255] is non-protected area, only written and read data arbitrarily")
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

    private fun onPowerUpSle4442ButtonClicked() {
        runCatching {
            val ret = mIccManager.open(ICC_SLOT.toByte(), SLE_CARD_TYPE.toByte(), VOLTAGE_5V.toByte())
            if (ret < 0) throw Exception("SLE4442 powered up failed")
        }.onSuccess {
            Toast.makeText(requireContext(), "Powered up SLE4442 successfully", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(requireContext(), "Powered down SLE4442 successfully", Toast.LENGTH_SHORT).show()
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
            return@runCatching outputLen
        }.onSuccess { outputLen -> // outputLen
            binding.etSle4442Data.setText(DataUtil.randomHex(10))
            Toast.makeText(requireContext(), "SLE4442 activated successfully", Toast.LENGTH_SHORT).show()
            val data = atrBuffer.copyOf(outputLen)
            binding.tvResult.text = buildString {
                append("Power on successfully! ATR: \n")
                append(BytesUtil.bytes2HexString(data))
                append("\n\n")
                append("Note: if any ATR(Answer to Reset) returns, then means SLE4442 is powered on successfully.\n\n")
                append("Verify Password successfully: $PASSWORD_ALL_Fs")
            }
            binding.apply {
                btnResetSle4442.isEnabled = false
                btnDeactivateSle4442.isEnabled = true
                btnReadSle4442.isEnabled = true
                btnWriteSle4442.isEnabled = true
                btnVerifyPasswordFFFFFF.isEnabled = true
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
            Toast.makeText(requireContext(), "SLE4442 deactivation successfully", Toast.LENGTH_SHORT).show()
            binding.apply {
                tvResult.text = "SLE4442 deactivation successfully"
                btnDeactivateSle4442.isEnabled = false
                btnPowerDownSle4442.isEnabled = true
                btnReadSle4442.isEnabled = false
                btnWriteSle4442.isEnabled = false
                btnVerifyPasswordFFFFFF.isEnabled = false
                btnWriteSle4442Protected.isEnabled = false
                btnReadSle4442Protected.isEnabled = false
            }
        }.onFailure {
            binding.tvResult.text = it.message
            it.printStackTrace()
        }
    }


    private fun onVerifyPasswordFFFFFFButtonClicked() {
        runCatching {
            val ret = mIccManager.sle4442_verifyPassword(BytesUtil.hexString2Bytes(PASSWORD_ALL_Fs))
            if (ret != 0x00) throw Exception("Verify Password failed: $PASSWORD_ALL_Fs")
        }.onSuccess {
            Toast.makeText(requireContext(), "Verify Password successfully", Toast.LENGTH_SHORT).show()
            binding.tvResult.text = buildString {
                append("Verify Password $PASSWORD_ALL_Fs successfully!\n\n")
                append("Now you can write data (before you can only read data)\n\n")
                append("Please note:\n")
                append(" - The default password is FFFFFF\n")
                append(" - If entered 3 wrong passwords in a row, the ICC will be disabled permanently\n")
                append(" - You can change the password using 'sle4442_changePassword(byte[] passwd)'\n")
                append(" - Password will stay effective until the ICC is taken out")
            }
            binding.btnVerifyPasswordFFFFFF.isEnabled = false
        }.onFailure {
            Toast.makeText(requireContext(), "If 3 times failed, the Card will be permanently disabled!", Toast.LENGTH_SHORT).show()
            binding.tvResult.text = it.message
            it.printStackTrace()
        }
    }

    private fun onWriteSle4442ButtonClicked() {
        val data = BytesUtil.hexString2Bytes(binding.etSle4442Data.text.toString())
        runCatching {
            val ret = mIccManager.sle4442_writeMainMemory(INDEX_100, data, data.size)
//            if (ret != 0) throw Exception("Wrote Data to SLE4442 failed! Please verify password first!")
            val dataReadForVerify = mIccManager.sle4442_readMainMemory(INDEX_100, BytesUtil.hexString2Bytes(binding.etSle4442Data.text.toString()).size)
            if (dataReadForVerify == null || dataReadForVerify.isEmpty()) throw Exception("Read Data for verification from SLE4442 failed")
            if (BytesUtil.bytes2HexString(dataReadForVerify) != binding.etSle4442Data.text.toString()) throw Exception("Wrote Data to SLE4442 failed! Please verify password first!")
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
            val ret = mIccManager.sle4442_writeProtectionMemory(INDEX_8, BytesUtil.hexString2Bytes("12"), 1)
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
            val data = mIccManager.sle4442_readProtectionMemory(INDEX_8, 4)
            if (data == null || data.isEmpty()) throw Exception("Read Data from SLE4442 failed")
            return@runCatching data
        }.onSuccess { data ->
            binding.tvResult.text = buildString {
                append("HEX Data read successfully:\n")
                append("${BytesUtil.bytes2HexString(data)}\n")
                append("from Index = 0 - [0, 31]\n")
                append("Data size: 4 Bytes \n")
                append("(You can only read data from protected area by the unit of 4 Bytes)")
            }
        }.onFailure {
            binding.tvResult.text = it.message
            it.printStackTrace()
        }
    }

}